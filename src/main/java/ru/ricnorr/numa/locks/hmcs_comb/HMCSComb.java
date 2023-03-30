package ru.ricnorr.numa.locks.hmcs_comb;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jdk.internal.vm.annotation.Contended;
import ru.ricnorr.numa.locks.AbstractNumaLock;
import ru.ricnorr.numa.locks.Utils;

import static ru.ricnorr.numa.locks.hmcs.HMCSQNodeInterface.ACQUIRE_PARENT;
import static ru.ricnorr.numa.locks.hmcs.HMCSQNodeInterface.COHORT_START;
import static ru.ricnorr.numa.locks.hmcs.HMCSQNodeInterface.LOCKED;
import static ru.ricnorr.numa.locks.hmcs.HMCSQNodeInterface.UNLOCKED;
import static ru.ricnorr.numa.locks.hmcs.HMCSQNodeInterface.WAIT;
import static ru.ricnorr.numa.locks.hmcs_comb.HMCSComb.NumaStatus.HAVE_LOCK;
import static ru.ricnorr.numa.locks.hmcs_comb.HMCSComb.NumaStatus.TRY_TO_GIVE_LOCK;
import static ru.ricnorr.numa.locks.hmcs_comb.HMCSComb.NumaStatus.WAITING_LOCK;


public class HMCSComb extends AbstractNumaLock {

  private final HNodeComb[] leafs;

  private final HNodeComb[] numaHNodes = new HNodeComb[Utils.NUMA_NODES_CNT];

  ThreadLocal<Integer> cclThreadLocal = ThreadLocal.withInitial(Utils::getKunpengCCLId);

  ThreadLocal<Integer> numaThreadLocal = ThreadLocal.withInitial(Utils::getNumaNodeId);

  private static int THRESHOLD = 256;

  enum NumaStatus {
    HAVE_LOCK,

    TRY_TO_GIVE_LOCK,

    WAITING_LOCK,

  }

  class Triple {
    int numa;

    int ccl;

    HMCSQNodeComb hmcsqNode;

    public Triple(int numa, int ccl, HMCSQNodeComb hmcsqNode) {
      this.numa = numa;
      this.ccl = ccl;
      this.hmcsqNode = hmcsqNode;
    }
  }

  @SuppressWarnings("unchecked")
  public HMCSComb() {
    super(Utils::getKunpengCCLId);
    int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;

    leafs = (HNodeComb[]) Array.newInstance(HNodeComb.class, Utils.CCL_CNT);
    if (Runtime.getRuntime().availableProcessors() == 128 || Runtime.getRuntime().availableProcessors() == 96) {
      var root = new HNodeComb(null, new HMCSQNodeComb());
      List<HNodeComb> superNumaNodes = new ArrayList<>();
      for (int i = 0; i < Utils.NUMA_NODES_CNT / 2; i++) {
        superNumaNodes.add(new HNodeComb(root, new HMCSQNodeComb()));
      }
      List<HNodeComb> numaNodes = new ArrayList<>();
      for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
        numaNodes.add(new HNodeComb(superNumaNodes.get(i / 2), new HMCSQNodeComb()));
        numaHNodes[i] = numaNodes.get(numaNodes.size() - 1);
      }
      for (int i = 0; i < Utils.CCL_CNT; i++) {
        leafs[i] = new HNodeComb(numaNodes.get(i / cclPerNuma), new HMCSQNodeComb());
      }
    } else {
      var root = new HNodeComb(null, new HMCSQNodeComb());
      List<HNodeComb> numaNodes = new ArrayList<>();
      for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
        numaNodes.add(new HNodeComb(root, new HMCSQNodeComb()));
        numaHNodes[i] = numaNodes.get(numaNodes.size() - 1);
      }
      for (int i = 0; i < Utils.CCL_CNT; i++) {
        leafs[i] = new HNodeComb(numaNodes.get(i / cclPerNuma), new HMCSQNodeComb());
      }
    }

  }

  @Override
  @SuppressWarnings("unchecked")
  public Object lock(Object obj) {
    HMCSQNodeComb node = new HMCSQNodeComb();
    Triple res;
    int cclId;
    int numaId;
    while (true) {
      cclId = Utils.getByThreadFromThreadLocal(cclThreadLocal, Utils.getCurrentCarrierThread());
      numaId = Utils.getByThreadFromThreadLocal(numaThreadLocal, Utils.getCurrentCarrierThread());
      HNodeComb numaHNode = numaHNodes[numaId];
      if (numaHNode.numaStatus == NumaStatus.TRY_TO_GIVE_LOCK) {
        Thread.yield();
        continue;
      }
      if (numaHNode.numaStatus == NumaStatus.HAVE_LOCK) {
        numaHNode.threads.incrementAndGet();
        lockH(node, leafs[cclId], 0);
        res = new Triple(numaId, cclId, node);
        break;
      }
      if (numaHNode.numaStatus == WAITING_LOCK) {
        int onNumaNode = numaHNode.threads.get();
        if (onNumaNode < Utils.CCL_PER_NUMA) {
          if (numaHNode.threads.compareAndSet(onNumaNode, onNumaNode + 1)) {
            lockH(node, leafs[cclId], 0);
            res = new Triple(numaId, cclId, node);
            break;
          }
        } else {
          Thread.yield();
          continue;
        }
      }
    }
    if (numaHNodes[numaId].numaStatus == WAITING_LOCK) {
      numaHNodes[numaId].numaStatus = HAVE_LOCK;
    }
    return res;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void unlock(Object obj) {
    Triple triple = (Triple) obj;
    unlockH(leafs[triple.ccl], triple.hmcsqNode, 0);
    numaHNodes[triple.numa].threads.decrementAndGet();
  }


  @Override
  public boolean hasNext(Object obj) {
    throw new IllegalStateException("Not implemented");
  }

  private void lockH(HMCSQNodeComb qNode, HNodeComb hNode, int lvl) {
    if (hNode.parent == null) {
      qNode.setNextAtomically(null);
      qNode.setStatusAtomically(LOCKED);
      HMCSQNodeComb pred = hNode.tail.getAndSet(qNode);
      if (pred == null) {
        qNode.setStatusAtomically(UNLOCKED);
      } else {
        pred.setNextAtomically(qNode);
        while (qNode.getStatus() == LOCKED) {
          Thread.onSpinWait();
        } // spin
      }
    } else {
      qNode.setNextAtomically(null);
      qNode.setStatusAtomically(WAIT);
      HMCSQNodeComb pred = hNode.tail.getAndSet(qNode);
      if (pred != null) {
        pred.setNextAtomically(qNode);
        while (qNode.getStatus() == WAIT) {
          Thread.onSpinWait();
        } // spin
        if (qNode.getStatus() < ACQUIRE_PARENT) {
          return;
        }
      }
      qNode.setStatusAtomically(COHORT_START);
      lockH(hNode.node, hNode.parent, lvl + 1);
    }
  }

  private void unlockH(HNodeComb hNode, HMCSQNodeComb qNode, int lvl) {
    if (hNode.parent == null) { // top hierarchy
      releaseHelper(hNode, qNode, UNLOCKED);
      return;
    }
    int curCount = qNode.getStatus();
    boolean skipThresholdCheck = false;
    if (curCount >= THRESHOLD && hNode.node.getStatus() >= THRESHOLD - 1 && lvl == 0) {
      if (hNode.parent.numaStatus != TRY_TO_GIVE_LOCK) {
        hNode.parent.numaStatus = TRY_TO_GIVE_LOCK;
      }
      skipThresholdCheck = true;
    }
    if (lvl == 1 &&
        (curCount >= THRESHOLD || hNode.numaStatus == TRY_TO_GIVE_LOCK)) { // проставил CCL либо еще не проставил
      if (hNode.numaStatus != TRY_TO_GIVE_LOCK) {
        hNode.numaStatus = TRY_TO_GIVE_LOCK;
      }
      skipThresholdCheck = true;
    }
    if (!skipThresholdCheck && curCount >= THRESHOLD) {
      unlockH(hNode.parent, hNode.node, lvl + 1);
      releaseHelper(hNode, qNode, ACQUIRE_PARENT);
      return;
    }
    HMCSQNodeComb succ = qNode.getNext();
    if (succ != null) {
      succ.setStatusAtomically(curCount + 1);
      return;
    }
    if (lvl == 1) {
      hNode.numaStatus = WAITING_LOCK;
    }
    unlockH(hNode.parent, hNode.node, lvl + 1);
    releaseHelper(hNode, qNode, ACQUIRE_PARENT);
  }

  private void releaseHelper(HNodeComb l, HMCSQNodeComb i, int val) {
    HMCSQNodeComb succ = i.getNext();
    if (succ != null) {
      succ.setStatusAtomically(val);
    } else {
      if (l.tail.compareAndSet(i, null)) {
        return;
      }
      do {
        succ = i.getNext();
      } while (succ == null);
      succ.setStatusAtomically(val);
    }
  }

  public class HNodeComb {

    @Contended("gr1")
    private final AtomicReference<HMCSQNodeComb> tail;

    @Contended("gr1")
    private final HNodeComb parent;

    @Contended("gr1")
    HMCSQNodeComb node;

    @Contended("gr2")
    private final AtomicInteger threads = new AtomicInteger();

    @Contended("gr3")
    private volatile NumaStatus numaStatus = WAITING_LOCK;


    public HNodeComb(HNodeComb parent, HMCSQNodeComb qNode) {
      this.parent = parent;
      this.tail = new AtomicReference<>(null);
      this.node = qNode;
    }
  }
}

