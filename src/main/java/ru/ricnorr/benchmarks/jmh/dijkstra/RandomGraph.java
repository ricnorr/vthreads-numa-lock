package ru.ricnorr.benchmarks.jmh.dijkstra;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

class RandomGraph {

  List<GraphNode> nodes;

  public RandomGraph(int vertexes, double edgeProbability) {
    nodes = new ArrayList<>();
    for (int i = 0; i < vertexes; i++) {
      nodes.add(new GraphNode(i));
    }

    var random = new Random();
    for (int i = 0; i < vertexes; i++) {
      for (int j = i + 1; j < vertexes; j++) {
        if (random.nextDouble() < edgeProbability) {
          nodes.get(i).outgoingEdges.add(new Edge(nodes.get(j), random.nextInt(1, 10000)));
        }
      }
    }
  }

  record Edge(
      GraphNode to,
      int weight
  ) {
  }

  public class GraphNode {
    int id;

    List<Edge> outgoingEdges = new ArrayList<>();

    AtomicLong parallelDistance = new AtomicLong(Long.MAX_VALUE);

    long seqDistance = Long.MAX_VALUE;

    public GraphNode(int id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof GraphNode)) {
        return false;
      }
      return ((GraphNode) obj).id == id;
    }
  }
}