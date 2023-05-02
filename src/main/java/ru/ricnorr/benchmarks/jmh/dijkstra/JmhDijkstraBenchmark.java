package ru.ricnorr.benchmarks.jmh.dijkstra;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.Affinity;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhDijkstraBenchmark {

  static final Comparator<NodeIdAndDistance> COMPARATOR =
      Comparator.comparing((NodeIdAndDistance it) -> it.distance).thenComparing((NodeIdAndDistance it) -> it.nodeId);

  public record NodeIdAndDistance(
      int nodeId,
      long distance
  ) {
  }

  @Param("0")
  public int nodesCnt;
  @Param("0")
  public int threads;
  @Param("")
  public String lockType;
  @Param("1")
  public double probabilityOfEdge;
  NumaLock lock;

  List<Thread> threadList = new ArrayList<>();

  RandomGraph randomGraph;

  Phaser onFinish;


  @Setup(Level.Trial)
  public void init() {
    System.out.println("Get system property jdk.virtualThreadScheduler.parallelism=" +
        System.getProperty("jdk.virtualThreadScheduler.parallelism"));
    System.out.println("Get system property jdk.virtualThreadScheduler.maxPoolSize=" +
        System.getProperty("jdk.virtualThreadScheduler.maxPoolSize"));
    if (!lockType.equals(LockType.SYNCHRONIZED.toString())) {
      lock = Utils.initLock(LockType.valueOf(lockType), threads);
    }
  }

  @Setup(Level.Invocation)
  public void prepare() {
    threadList = new ArrayList<>();
    onFinish = new Phaser(threads + 1);
    ThreadFactory threadFactory = Thread.ofVirtual().factory();
    var priorityQueue = new MultiQueue(2 * Math.min(threads, Utils.CORES_CNT), LockType.valueOf(lockType));
    randomGraph = new RandomGraph(nodesCnt, probabilityOfEdge);
    var startNode = randomGraph.nodes.get(0);
    startNode.parallelDistance.set(0);
    priorityQueue.addElement(new NodeIdAndDistance(startNode.id, 0));

    System.out.println("create threads");
    AtomicInteger customBarrier = new AtomicInteger();
    AtomicBoolean locked = new AtomicBoolean(false);
    var carrierThreads = new HashSet<>();
    for (int i = 0; i < threads; i++) {
      var thread = threadFactory.newThread(
          () -> {
            customBarrier.incrementAndGet();
            int cores = Math.min(threads, Utils.CORES_CNT);
            while (customBarrier.get() < cores) {
              // do nothing
            }
            while (!locked.compareAndSet(false, true)) {
              // do nothing
            }
            Thread currentCarrier = Utils.getCurrentCarrierThread();
            if (!carrierThreads.contains(currentCarrier)) {
              Affinity.affinityLib.pinToCore(carrierThreads.size());
              carrierThreads.add(currentCarrier);
            }
            locked.compareAndSet(true, false);


            while (true) {
              System.out.printf("activeNodes %d\n", priorityQueue.valueCounter());
              var curMin = priorityQueue.getElement();
              if (curMin == null && priorityQueue.valueCounter() == 0) {
                break;
              }
              if (curMin == null) {
                continue;
              }
              var curNode = randomGraph.nodes.get(curMin.nodeId);
              var curDistance = curNode.parallelDistance.get();
              for (RandomGraph.Edge e : curNode.outgoingEdges) {
                while (true) {
                  var oldDistance = e.to().parallelDistance.get();
                  var newDistance = curDistance + e.weight();
                  if (oldDistance == Long.MAX_VALUE || oldDistance > newDistance) {
                    if (e.to().parallelDistance.compareAndSet(oldDistance, newDistance)) {
                      priorityQueue.addElement(new NodeIdAndDistance(e.to().id, newDistance));
                    } else {
                      continue;
                    }
                  }
                  break;
                }
              }
              priorityQueue.decCounter();
              Thread.yield();
            }
            onFinish.arrive();
          }
      );
      thread.setName("vt-thread-" + i);
      threadList.add(thread);
    }
  }

  void shortestPathSequential(RandomGraph.GraphNode start) {
    System.out.println("Shortest path seq started");
    start.seqDistance = 0;
    var q = new PriorityQueue<>(COMPARATOR);
    q.add(new NodeIdAndDistance(start.id, start.seqDistance));
    while (!q.isEmpty()) {
      var min = q.poll();
      var cur = randomGraph.nodes.get(min.nodeId);
      for (RandomGraph.Edge e : cur.outgoingEdges) {
        if (e.to().seqDistance > cur.seqDistance + e.weight()) {
          e.to().seqDistance = cur.seqDistance + e.weight();
          q.removeIf(it -> it.nodeId == e.to().id);
          q.add(new NodeIdAndDistance(e.to().id, e.to().seqDistance));
        }
      }
    }
    System.out.println("Shortest path seq ended");
  }

  //@TearDown(Level.Invocation)
  public void assertResult() {
    shortestPathSequential(randomGraph.nodes.get(0));
    for (RandomGraph.GraphNode node : randomGraph.nodes) {
      if (node.seqDistance != node.parallelDistance.get()) {
        throw new RuntimeException(String.format("Par and seq are different, expected=%d, got=%d", node.seqDistance,
            node.parallelDistance.get()));
      }
    }
  }

  @org.openjdk.jmh.annotations.Benchmark
  @Fork(1)
  @Warmup(iterations = 20)
  @Measurement(iterations = 20)
  @BenchmarkMode({Mode.SingleShotTime})
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void bench() {
    for (int i = 0; i < threads; i++) {
      threadList.get(i).start();
    }
    onFinish.arriveAndAwaitAdvance();
  }
}
