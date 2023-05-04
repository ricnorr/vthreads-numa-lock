package ru.ricnorr.benchmarks.jmh.dijkstra;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
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
public class TextStatBenchmark {

  static final Comparator<NodeIdAndDistance> COMPARATOR =
      Comparator.comparing((NodeIdAndDistance it) -> it.distance).thenComparing((NodeIdAndDistance it) -> it.nodeId);

  public record NodeIdAndDistance(
      int nodeId,
      long distance
  ) {
  }
  
  @Param("0")
  public int threads;
  @Param("")
  public String lockType;
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
//    var priorityQueue = new MultiQueue(Math.min(Utils.CORES_CNT, threads), LockType.valueOf(lockType));
//    randomGraph = new RandomGraph(nodesCnt, probabilityOfEdge);
//    var startNode = randomGraph.nodes.get(0);
//    startNode.parallelDistance.set(0);
//    priorityQueue.addElement(startNode);
    byte[] array = new byte[256];
    int wordsCnt = 1_000_00;
    var words = new String[wordsCnt];
    for (int i = 0; i < wordsCnt; i++) {
      new Random().nextBytes(array);
      words[i] = new String(array, StandardCharsets.UTF_8);
    }
    System.out.println("create threads");
    AtomicInteger customBarrier = new AtomicInteger();
    AtomicBoolean locked = new AtomicBoolean(false);
    var carrierThreads = new HashSet<>();
    var map = new HashMap<String, Integer>();
    for (int i = 0; i < threads; i++) {
      int finalI = i;
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

            int wordsPerThread = wordsCnt / threads;
            for (int j = 0; j < wordsPerThread; j++) {
              for (String x : words[finalI * wordsPerThread + j].split(" ")) {
                var obj = lock.lock(null);
                map.put(x, map.getOrDefault(x, 0) + 1);
                lock.unlock(obj);
              }
              Thread.yield();
            }
//            while (true) {
//              var cur = priorityQueue.getElement();
//              if (cur == null && priorityQueue.valueCounter() == 0) {
//                break;
//              }
//              if (cur == null) {
//                continue;
//              }
//              var curDistance = cur.parallelDistance.get();
//              for (RandomGraph.Edge e : cur.outgoingEdges) {
//                while (true) {
//                  var oldDistance = e.to().parallelDistance.get();
//                  var newDistance = curDistance + e.weight();
//                  if (oldDistance == Long.MAX_VALUE || oldDistance > newDistance) {
//                    if (e.to().parallelDistance.compareAndSet(oldDistance, newDistance)) {
//                      priorityQueue.addElement(cur);
//                    } else {
//                      continue;
//                    }
//                  }
//                  break;
//                }
//              }
//              priorityQueue.decCounter();
//              Thread.yield();
//            }
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
