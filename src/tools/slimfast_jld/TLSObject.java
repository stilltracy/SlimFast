/******************************************************************************

 Copyright (c) 2016, Yuanfeng Peng (University of Pennsylvania)
 All rights reserved.  

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:

 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above
 copyright notice, this list of conditions and the following
 disclaimer in the documentation and/or other materials provided
 with the distribution.

 * Neither the names of the University of Pennsylvania 
 nor the names of its contributors may be
 used to endorse or promote products derived from this software
 without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 ******************************************************************************/


package tools.slimfast_jld;

import acme.util.Assert;
import rr.meta.FieldAccessInfo;
import rr.meta.MetaDataInfoMaps;
import rr.state.update.AbstractFieldUpdater;
import sun.misc.Unsafe;
import tools.util.CV;

final class TLSObject {
    /* maximum sizes of S_t & Q_t*/
    private static final int EP_POOL_SIZE = 20;
    private static final int ECV_POOL_SIZE = 20;
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    /** constants used for unsafe array indexing */
    final private static int EP_POOL_BASE, EP_POOL_SHIFT, LW_CLOCK_BASE, LW_CLOCK_SHIFT, LW_TID_BASE;

    static {
        int scale;

        EP_POOL_BASE = UNSAFE.arrayBaseOffset(Object[].class);
        scale = UNSAFE.arrayIndexScale(Object[].class);
        if (scale != 8) {
            Assert.panic("Object[] scale != 8: " + scale);
        }
        //EP_POOL_SHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        EP_POOL_SHIFT = 3;

        LW_CLOCK_BASE = UNSAFE.arrayBaseOffset(int[].class);
        scale = UNSAFE.arrayIndexScale(int[].class);
        if (scale != 4) {
            Assert.panic("int[] scale != 4: " + scale);
        }
        //LW_CLOCK_SHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        LW_CLOCK_SHIFT = 2;

        LW_TID_BASE = UNSAFE.arrayBaseOffset(byte[].class);
        scale = UNSAFE.arrayIndexScale(byte[].class);
        if (scale != 1) {
            Assert.panic("byte[] should have a scale of 1: " + scale);
        }
    }

    /* local caches for reusing metadata objects (S_t, Q_t, W_t)*/
    final private EpochPair[] epochpair_pool = new EpochPair[EP_POOL_SIZE];
    /** cache lastWriteClock fields from epochpair_pool for faster, unsafe access */
    final private int[] epochpair_lastWriteClock = new int[EP_POOL_SIZE];
    /** cache lastWriteTid fields from epochpair_pool for faster, unsafe access */
    final private byte[] epochpair_lastWriteTid = new byte[EP_POOL_SIZE];
    final private EpochAndCV[] ecv_pool = new EpochAndCV[ECV_POOL_SIZE];
    public int clock;
    public CV cv;
    /* per-thread information needed by the vector-clock algorithm */
    byte tid;
    EpochPair wr_epochpair_of_epoch;
    private int actualEPCount = 0;
    private int actualECVCount = 0;

    static AbstractFieldUpdater getCurrentFieldUpdater(int fadId) {
        AbstractFieldUpdater current_field_updater;
        FieldAccessInfo fad = MetaDataInfoMaps.getFieldAccesses().get(fadId);
        current_field_updater = fad.getField().getUpdater();
        return current_field_updater;
    }

    /* called upon each release(end of a release-free region) */
    public void reset() {
        for (int i = 0; i < actualEPCount; i++) {
            epochpair_pool[i] = null;
            epochpair_lastWriteClock[i] = 0;
            epochpair_lastWriteTid[i] = 0;
        }
        for (int i = 0; i < actualECVCount; i++) {
            ecv_pool[i] = null;
        }
        wr_epochpair_of_epoch = new EpochPair(tid, clock, tid, clock);
        actualEPCount = 0;
        actualECVCount = 0;
    }

    /* if no entry exists in the pool, create one*/
    EpochAndCV getEpochAndCVFromPool(byte tid1, int lastReadClock1, byte tid2, int lastReadClock2, byte lastWriteTid, int lastWriteClock) {
        for (int i = 0; i < actualECVCount; i++) {
            if (ecv_pool[i].lastWriteTid == lastWriteTid &&
                    ecv_pool[i].lastWriteClock == lastWriteClock &&
                    ecv_pool[i].get(tid1) == lastReadClock1) {
                return ecv_pool[i];
            }
        }
        EpochAndCV ecv = new EpochAndCV(lastWriteTid, lastWriteClock);
        ecv.makeCV(SlimFastFTTool.INIT_CV_SIZE);
        ecv.set(tid1, lastReadClock1);
        ecv.set(tid2, lastReadClock2);
        if (actualECVCount < ECV_POOL_SIZE) {
            actualECVCount++;
        }
        ecv_pool[actualECVCount - 1] = ecv;
        return ecv;

    }

    /* does not create an entry upon misses; instead, just returns null*/
    public EpochAndCV tryGetEpochAndCVFromPool(byte tid1, int lastReadClock1, byte tid2, int lastReadClock2, byte lastWriteTid, int lastWriteClock) {
        for (int i = 0; i < actualECVCount; i++) {
            if (ecv_pool[i].lastWriteTid == lastWriteTid &&
                    ecv_pool[i].lastWriteClock == lastWriteClock &&
                    ecv_pool[i].get(tid1) == lastReadClock1) {
                return ecv_pool[i];
            }
        }
        return null;
    }

    /* if no entry exists in the pool, create one*/
    EpochPair getEpochPairFromPool(int lastReadClock, byte lastWriteTid, int lastWriteClock) {
        for (int i = 0; i < actualEPCount; i++) {
            int lwc = UNSAFE.getInt(epochpair_lastWriteClock, LW_CLOCK_BASE + (i << LW_CLOCK_SHIFT));
            int lwtid = UNSAFE.getByte(epochpair_lastWriteTid, LW_TID_BASE + i);
            if (lwtid == lastWriteTid && lwc == lastWriteClock) {
                // NB: the cast is ~1% cheaper than the bounds check on SOR
                return (EpochPair) UNSAFE.getObject(epochpair_pool, EP_POOL_BASE + (i << EP_POOL_SHIFT));
                //return epochpair_pool[i];
            }
        }
        EpochPair ep = new EpochPair(tid, lastReadClock, lastWriteTid, lastWriteClock);
        if (actualEPCount < EP_POOL_SIZE) {
            actualEPCount++;
        }
        epochpair_pool[actualEPCount - 1] = ep;
        epochpair_lastWriteTid[actualEPCount - 1] = lastWriteTid;
        epochpair_lastWriteClock[actualEPCount - 1] = lastWriteClock;
        return ep;
    }

}
