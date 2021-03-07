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
import tools.util.CV;
import rr.state.ShadowThread;
import rr.event.AccessEvent;
import rr.state.AbstractArrayState;
import rr.state.update.AbstractArrayUpdater;
import rr.state.update.AbstractFieldUpdater;
import rr.state.ShadowVar;
import acme.util.Yikes;
import rr.tool.Tool;
import rr.meta.FieldAccessInfo;
import rr.meta.MetaDataInfoMaps;
public class EpochPair implements ShadowVar{
	public volatile int lastReadClock=0;
	public volatile int lastWriteClock=0;
	public volatile byte lastReadTid=0;
	public volatile byte lastWriteTid=0;
	
	public EpochPair()
	{
	}
	public EpochPair(byte lastReadTid, int lastReadClock, byte lastWriteTid, int lastWriteClock)
	{
		this.lastReadClock=lastReadClock;
		this.lastWriteClock=lastWriteClock;
		this.lastReadTid=lastReadTid;
		this.lastWriteTid=lastWriteTid;
	}
	public boolean checkAndUpdate(SlimFastFTTool tool, AccessEvent ae,ShadowThread td, byte tid, TLSObject tls_obj,int tdClock, CV tdCV)
	{
		if (ae.isWrite()) //EO ==> EO
		{
			if (lastWriteClock == tdClock&&
			    lastWriteTid==tid) {
				return true;
			}

			if (lastWriteClock > tdCV.get(lastWriteTid)) {
				tool.error(ae, 1, "write-by-thread-", lastWriteTid, "write-by-thread-", tid);
			}

			if (lastReadTid != td.getTid() && lastReadClock > tdCV.get(lastReadTid)) {
				tool.error(ae, 2, "read-by-thread-", lastReadTid, "write-by-thread-", tid);
			}
			//Tool.incCounter(td,"WRITE_EO_EO");
			boolean success=ae.putShadow(tls_obj.wr_epochpair_of_epoch);
			return success;
		} else //EO ==> EO || EO ==> CV
		{
			if (lastReadClock == tdClock&&
			    lastReadTid==tid) {
				return true;
			} 
			if (lastWriteClock > tdCV.get(lastWriteTid)) {
				tool.error(ae, 4, "write-by-thread-", lastWriteTid, "read-by-thread-",tid);
			}

			if (lastReadClock <= tdCV.get(lastReadTid)) 
			{ //EO ==> EO
				EpochPair epochpair = tls_obj.getEpochPairFromPool(tdClock, lastWriteTid,lastWriteClock);
				boolean success=ae.putShadow(epochpair);
				return success;
			} else 
			{//EO ==> CV
			
				EpochAndCV ecv=tls_obj.getEpochAndCVFromPool(lastReadTid,lastReadClock, tid,tdClock,lastWriteTid, lastWriteClock);
				boolean success=ae.putShadow(ecv);
				return success;
			}
			
		}
	}

	public final boolean fieldPutShadow(TLSObject tls_obj,int fadId, EpochPair newGS, Object target) {
		AbstractFieldUpdater updater=tls_obj.getCurrentFieldUpdater(fadId);
		return updater.putState(target, this, newGS);
		
	}

	public final boolean arrayPutShadow(AbstractArrayUpdater updater, AbstractArrayState arrayState,int index, EpochPair epochpair)
	{
		return updater.putState(arrayState, index, this, epochpair);
	}

	final EpochPair getNewEpochPair(ShadowThread td, final int tid, final TLSObject tls_obj)
	{
		final int tdClock=tls_obj.clock;
		final CV fhbCV=tls_obj.cv;				
		
		EpochPair epochpair=null;
		if(lastReadTid == tid || lastReadClock <=fhbCV.get(lastReadTid))
		{
			epochpair=tls_obj.getEpochPairFromPool(tdClock,lastWriteTid, lastWriteClock);
		}
		else
		{
			EpochAndCV ecv=tls_obj.getEpochAndCVFromPool(lastReadTid,lastReadClock, (byte)tid,tdClock,lastWriteTid, lastWriteClock);
			epochpair=ecv;
		}
		return epochpair;
	}
	public boolean fieldReadFastPath(ShadowThread td,final int tid, final TLSObject tls_obj, int fadId, Object target)
	{
		EpochPair epochpair=getNewEpochPair(td,tid,tls_obj);
		boolean success=fieldPutShadow(tls_obj,fadId, epochpair, target);
		return success;
		
	}
	public boolean fieldWriteFastPath(ShadowThread td,final int tid, final TLSObject tls_obj, int fadId, Object target)
	{
		final int tdClock=tls_obj.clock;
		final CV tdCV=tls_obj.cv;
		if( lastReadTid != tid && lastReadClock > tdCV.get(lastReadTid))
		{
			return false;
		}
		return fieldPutShadow(tls_obj,fadId, tls_obj.wr_epochpair_of_epoch, target);
	}


	public boolean arrayReadFastPath(ShadowThread td,final int tid, final TLSObject tls_obj, AbstractArrayState arrayState, AbstractArrayUpdater updater, int index)
	{
		EpochPair epochpair=getNewEpochPair(td, tid,tls_obj);
		boolean success=arrayPutShadow(updater,arrayState,index, epochpair);
		return success;
	}
	public boolean arrayWriteFastPath(ShadowThread td,final int tid, final TLSObject tls_obj, AbstractArrayState arrayState, AbstractArrayUpdater updater, int index)
	{
		final int tdClock=tls_obj.clock;
		final CV tdCV=tls_obj.cv;
		if( lastReadTid != tid && lastReadClock > tdCV.get(lastReadTid))
		{
			return false;
		}
		return arrayPutShadow(updater,arrayState, index, tls_obj.wr_epochpair_of_epoch);
	}
	
}

