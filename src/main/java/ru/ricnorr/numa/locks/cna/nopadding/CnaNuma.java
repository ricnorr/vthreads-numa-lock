package ru.ricnorr.numa.locks.cna.nopadding;

import ru.ricnorr.numa.locks.Utils;


public class CnaNuma extends AbstractCna {

    public CnaNuma(boolean useLightThreads) {
        super(useLightThreads, Utils::getClusterID);
    }
}


//  && (ThreadLocalRandom.current().nextInt() & 0xff) != 0 // probability = 1 - (1 / 2**8) == 0.996


// keep_lock_local() &&


//        private CNANode successor_found(CNANode me, CNANode cur, CNANode secHead, CNANode secTail) {
//            if (me.spin.get() != trueValue) {
//                me.spin.get().secTail.get().next.set(secHead);
//            } else {
//                me.spin.set(secHead);
//            }
//            //me.spin.getValue().next.set(null);
//            secTail.next.set(null);
//            me.spin.get().secTail.set(secTail);
//            return cur;
//        }

//        private boolean isFromOneBigNuma(int curSocket, int mySocket) {
//            return (curSocket == 0 && mySocket == 1) || (curSocket == 1 && mySocket == 0)
//                    || (curSocket == 2 && mySocket == 3) || (curSocket == 3 && mySocket == 2);
//        }
//
//        private boolean keep_lock_local() { // probability 0.9999
//            return (ThreadLocalRandom.current().nextInt() & 0xffff) != 0;
//        }
