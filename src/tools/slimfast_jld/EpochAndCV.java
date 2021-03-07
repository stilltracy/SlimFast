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

import rr.event.AccessEvent;
import rr.state.AbstractArrayState;
import rr.state.ShadowThread;
import rr.state.update.AbstractArrayUpdater;
import tools.util.CV;

import java.io.Serializable;

public class EpochAndCV extends EpochPair implements Serializable {

	/* array holding the clock values*/
	protected int[] a;
	private static final int FAST = 8;

	EpochAndCV[] nextECVs;

	public EpochAndCV(EpochAndCV ecv) {
                int[] ca = ecv.a;
                if (ca != null) {
                    makeCV(ca.length);
                    assign(ecv);
                }
                this.lastWriteClock=ecv.lastWriteClock;
                this.lastReadClock=ecv.lastReadClock;
		this.lastReadTid=ecv.lastReadTid;
		this.lastWriteTid=ecv.lastWriteTid;
        }

        @Override
        public int hashCode()
        {
                return lastWriteClock*3+lastWriteTid;
        }
        @Override
        public boolean equals(Object obj)
        {
                EpochAndCV ecv=(EpochAndCV)obj;
                if(ecv.lastWriteClock!=lastWriteClock||ecv.lastWriteTid!=lastWriteTid||a.length!=ecv.a.length)
                        return false;
                assert(a!=null&&ecv.a!=null);
                for(int i=0;i<a.length;i++)
                {
                        if(a[i]!=ecv.a[i])
                                return false;
                }
                return true;
        }

	public void initCV(int size) {
		if (size > 0) {
			makeCV(size);
		}
	}

	// check that a.length < len before calling.
	final public synchronized void assign(EpochAndCV cv) {
		int[] ca = cv.a;
		for(int i=0; i<a.length;i++) a[i]=ca[i];
	}

	public void makeCV(int i) {
		if (a == null) {
			a = new int[i];
			nextECVs=new EpochAndCV[i];
		}
	}

	final private synchronized void resize(int len) {
		if (len < a.length) return;
		int[] b = new int[len];
		EpochAndCV[] e=new EpochAndCV[len];
		for(int i=0;i<a.length; i++)
		{
			b[i]=a[i];
			e[i]=nextECVs[i];
		}
		a=b;
		nextECVs=e;
	}

	final public synchronized void max(CV c) {
		int[] ca = c.a;
		if (a.length<ca.length) this.resize(ca.length);
		if (a.length>ca.length) c.resize(a.length);
		int[] thisa = this.a;
		switch (ca.length) {
			default: slowMax(c);
			case 8: if (thisa[7]<ca[7]) thisa[7]=ca[7];
			case 7: if (thisa[6]<ca[6]) thisa[6]=ca[6];
			case 6: if (thisa[5]<ca[5]) thisa[5]=ca[5];
			case 5: if (thisa[4]<ca[4]) thisa[4]=ca[4];
			case 4: if (thisa[3]<ca[3]) thisa[3]=ca[3];
			case 3: if (thisa[2]<ca[2]) thisa[2]=ca[2];
			case 2: if (thisa[1]<ca[1]) thisa[1]=ca[1];
			case 1: if (thisa[0]<ca[0]) thisa[0]=ca[0];
			case 0:
		}
	}

	/* Requires this.a.length <= c.a.length */
	final private void slowMax(CV c) {
		int[] ca = c.a;
		int[] thisa = this.a;
		// iterate until thisa.length since someone may have extended ca since
		// we verified it was long enough.
		for(int i = FAST; i < thisa.length; i++) {
			if (thisa[i] < ca[i]) thisa[i] = ca[i];
		}
	}

	/* Return true if any entry in c1 is greater than in c2. */
	final public boolean anyGt(CV other) {
		if (other.a.length< this.a.length) other.resize(this.a.length);
		synchronized(this) {
			int ca1[] = this.a;
			int ca2[] = other.a;
			switch (ca1.length) {
				default: if (slowAnyGt(this,other)) return true;
				case 8:  if (ca1[7]>ca2[7]) return true;
				case 7:  if (ca1[6]>ca2[6]) return true;
				case 6:  if (ca1[5]>ca2[5]) return true;
				case 5:  if (ca1[4]>ca2[4]) return true;
				case 4:  if (ca1[3]>ca2[3]) return true;
				case 3:  if (ca1[2]>ca2[2]) return true;
				case 2:  if (ca1[1]>ca2[1]) return true;
				case 1:  if (ca1[0]>ca2[0]) return true;
				case 0:
			}
			return false;
		}
	}

	/*
	 * Return true if any entry in c1 is greater than in c2.
	 * Requires c2.a.length >= c1.a.length
	 */
	final private static boolean slowAnyGt(EpochAndCV c1, CV c2) {
		int ca1[] = c1.a;
		int ca2[] = c2.a;
		for(int i=FAST; i < ca1.length; i++) {
			if (ca1[i]>ca2[i]) return true;
		}
		return false;
	}

	/*
	 * Returns next index i>=start such that c1.a[i]>c2.a[i],
	 * or -1 if no such.
	 */
	final public int nextGt(CV other, int start) {
		if (other.a.length<this.a.length) other.resize(this.a.length);
		synchronized(this) {
			for(int i=start; i<this.a.length; i++) {
				if(this.a[i]>other.a[i]) return i;
			}
		}
		return -1;
	}

	final public void inc(int tid) {
		if (a.length <= tid) resize(tid +1);
		a[tid]++;
	}

	final public void inc(int tid, int amount) {
		if (a.length <= tid) resize(tid + 1);
		a[tid]+=amount;
	}

	@Override
	public String toString() {
		String r = "[";
		if(a!=null)
		for(int i=0; i<a.length; i++) r += (i > 0 ? " " : "") + String.format("%08X", a[i]);
		return r+"]";
	}


	final public int get(int tid) {
		if (tid < a.length) {
			return a[tid];
		} else {
			return 0;
		}
	}

	final public int size() {
		return a.length;
	}

	final synchronized public int gets(int tid) {
		if (tid < a.length) {
			return a[tid];
		} else {
			return 0;
		}
	}
	public EpochAndCV getNextECV(int tid, int tdClock, ShadowThread st, TLSObject tls_obj)
	{
		if(a.length<=tid)
		{
			this.resize(tid+1);
		}
		EpochAndCV nextECV=nextECVs[tid];
		if(nextECV!=null&&nextECV.get(tid)==tdClock)
		{
			return nextECV;
		}

		/* we could have looked up the ecv_pool here, but experimental results told us that
		   the desired meta-object is already in ecv_pool. */
		//nextECV=tls_obj.tryGetEpochAndCVFromPool(lastReadTid,lastReadClock, (byte)tid,tdClock,lastWriteTid, lastWriteClock);
		if(nextECV==null)
		{
			nextECV=new EpochAndCV(this);
		}

		nextECV.set(tid,tdClock);

		this.nextECVs[tid]=nextECV;
		return nextECV;
	}

	final synchronized public void set(int tid, int v) {
		if (a.length<=tid) {
			resize(tid+1);
		}
		a[tid] = v;
	}

	final public void clear() {
		for(int i=0; i<a.length;i++) a[i]=0;
	}
	public EpochAndCV(byte lastT, int lastW)
	{
		this.lastWriteTid=lastT;
		this.lastWriteClock=lastW;
	}
	@Override
	public boolean checkAndUpdate(SlimFastFTTool tool, AccessEvent ae, ShadowThread td, byte tid, TLSObject tls_obj, int tdClock, CV tdCV)
	{
		if(ae.isWrite())
		{
			if (lastWriteClock == tdClock&&
			    lastWriteTid==tid) {
				return true;
			}

			if (lastWriteClock > tdCV.get(lastWriteTid)) {
				tool.error(ae, 1, "write-by-thread-", lastWriteTid, "write-by-thread-", tid);
			}
				if (this.anyGt(tdCV))
				{
					for (int prevReader = this.nextGt(tdCV, 0); prevReader > -1; prevReader = this.nextGt(tdCV, prevReader + 1)) {
						if (prevReader != td.getTid()) {
							tool.error(ae, 3, "read-by-thread-", prevReader, "write-by-thread-", tid);
						}
					}
				}
				boolean success=ae.putShadow(tls_obj.wr_epochpair_of_epoch);
				return success;
		}
		else
		{
			if (lastWriteClock > tdCV.get(lastWriteTid)) {
				tool.error(ae, 4, "write-by-thread-", lastWriteTid, "read-by-thread-", tid);
			}
			if (this.get(tid) == tdClock) {
				return true;
			}
		       EpochAndCV newEcv=this.getNextECV(tid,tdClock,td,tls_obj);
		       return ae.putShadow(newEcv);

		}
	}
	@Override
	public boolean arrayReadFastPath(ShadowThread td, final int tid, final TLSObject tls_obj, AbstractArrayState arrayState, AbstractArrayUpdater updater, int index)
	{
		final int tdClock=tls_obj.clock;
		final CV fhbCV=tls_obj.cv;
		if ( get(tid) !=tdClock)
		{
			EpochAndCV newEcv=this.getNextECV(tid,tdClock,td, tls_obj);
			return  arrayPutShadow(updater,arrayState,index,newEcv);
		}
		return true;
	}
	@Override
	public boolean arrayWriteFastPath(ShadowThread td, final int tid, final TLSObject tls_obj, AbstractArrayState arrayState, AbstractArrayUpdater updater, int index)
	{
		final int tdClock=tls_obj.clock;
		final CV tdCV=tls_obj.cv;
		if(this.anyGt(tdCV))
		{
			return false;
		}
		boolean success = arrayPutShadow(updater, arrayState, index, tls_obj.wr_epochpair_of_epoch);
		return success;
	}
	@Override
	public boolean fieldReadFastPath(ShadowThread td, final int tid, final TLSObject tls_obj, int fadId, Object target)
	{
		final int tdClock=tls_obj.clock;
		if(this.get(tid)!=tdClock)
		{
			EpochAndCV newEcv=this.getNextECV(tid,tdClock,td, tls_obj);
			return fieldPutShadow(tls_obj,fadId,newEcv,target);
		}
		return true;
	}
	@Override
	public boolean fieldWriteFastPath(ShadowThread td, final int tid, final TLSObject tls_obj, int fadId, Object target)
	{
		final int tdClock=tls_obj.clock;
		final CV tdCV=tls_obj.cv;

		if(this.anyGt(tdCV))
		{
			return false;
		}
		return fieldPutShadow(tls_obj,fadId, tls_obj.wr_epochpair_of_epoch, target);
	}

}

