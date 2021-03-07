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


package tools.slimfast;
import java.util.HashMap;
import java.lang.ref.WeakReference;
import tools.util.CV;
import rr.meta.MetaDataInfoMaps;
import rr.meta.FieldAccessInfo;
import rr.state.update.AbstractFieldUpdater;
import sun.misc.Unsafe;
public class TLSObject{
	/* per-thread information needed by the vector-clock algorithm */
	byte tid;
	public int clock;
	public CV cv;

	/* maximum sizes of S_t & Q_t*/
	public static final int EP_POOL_SIZE=20;
	public static final int ECV_POOL_SIZE=20;

	/* local caches for reusing metadata objects (S_t, Q_t, W_t)*/
	final EpochPair[] epochpair_pool=new EpochPair[EP_POOL_SIZE];
	int actualEPCount=0;
	EpochAndCV[] ecv_pool=new EpochAndCV[ECV_POOL_SIZE];
	int actualECVCount=0;
	public EpochPair wr_epochpair_of_epoch;

	public static final AbstractFieldUpdater getCurrentFieldUpdater(int fadId)
	{
		AbstractFieldUpdater current_field_updater=null;
		FieldAccessInfo fad = MetaDataInfoMaps.getFieldAccesses().get(fadId);
		current_field_updater = fad.getField().getUpdater(); 
		return current_field_updater;
	}

	/* called upon each release(end of a release-free region) */
	public void reset()
	{
		for(int i=0;i<actualEPCount;i++)
			epochpair_pool[i]=null;
		for(int i=0;i<actualECVCount;i++)
			ecv_pool[i]=null;
		wr_epochpair_of_epoch=new EpochPair(tid,clock,tid,clock);
		actualEPCount=0;
		actualECVCount=0;	
	}
	/* if no entry exists in the pool, create one*/
	public EpochAndCV getEpochAndCVFromPool(byte tid1, int lastReadClock1, byte tid2, int lastReadClock2, byte lastWriteTid, int lastWriteClock)
	{
		EpochAndCV ecv=null;
		for(int i=0;i<actualECVCount;i++)
		{
			if(ecv_pool[i].lastWriteTid==lastWriteTid&&
			   ecv_pool[i].lastWriteClock==lastWriteClock&&
			   ecv_pool[i].get(tid1)==lastReadClock1)
			{
				return ecv_pool[i];	
			}
		}
		ecv=new EpochAndCV(lastWriteTid,lastWriteClock);
		ecv.makeCV(SlimFastFTTool.INIT_CV_SIZE);
		ecv.set(tid1,lastReadClock1);
		ecv.set(tid2,lastReadClock2);
		if(actualECVCount<ECV_POOL_SIZE)
			actualECVCount++;
		ecv_pool[actualECVCount-1]=ecv;
		return ecv;	
		
	}
	/* does not create an entry upon misses; instead, just returns null*/
	public EpochAndCV tryGetEpochAndCVFromPool(byte tid1, int lastReadClock1, byte tid2, int lastReadClock2, byte lastWriteTid, int lastWriteClock)
	{
		EpochAndCV ecv=null;
		for(int i=0;i<actualECVCount;i++)
		{
			if(ecv_pool[i].lastWriteTid==lastWriteTid&&
			   ecv_pool[i].lastWriteClock==lastWriteClock&&
			   ecv_pool[i].get(tid1)==lastReadClock1)
			{
				return ecv_pool[i];	
			}
		}
		return null;

	}
	/* if no entry exists in the pool, create one*/
	public EpochPair getEpochPairFromPool( int lastReadClock, byte lastWriteTid, int lastWriteClock)
	{
		EpochPair ep=null;
		for(int i=0;i<actualEPCount;i++)
		{
			if(epochpair_pool[i].lastWriteTid==lastWriteTid&&
			   epochpair_pool[i].lastWriteClock==lastWriteClock)
			{
				return epochpair_pool[i];
			}
		}
		ep= new EpochPair(tid,lastReadClock,lastWriteTid,lastWriteClock);
		if(actualEPCount<EP_POOL_SIZE)
			actualEPCount++;
		epochpair_pool[actualEPCount-1]=ep;
		return ep;
	}

}
