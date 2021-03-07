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
import java.lang.reflect.Field;
import sun.misc.Unsafe;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.Map;
import rr.state.update.AbstractFieldUpdater;
import rr.org.objectweb.asm.Opcodes;
import rr.state.update.AbstractArrayUpdater;
import rr.state.AbstractArrayState;
import gnu.trove.map.hash.TIntObjectHashMap;

import rr.event.ClassAccessedEvent;
import rr.annotations.Abbrev;
import rr.barrier.BarrierEvent;
import rr.barrier.BarrierListener;
import rr.barrier.BarrierMonitor;
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.ClassInitializedEvent;
import rr.event.Event;
import rr.event.FieldAccessEvent;
import rr.event.JoinEvent;
import rr.event.NewThreadEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.event.AccessEvent.Kind;
import rr.instrument.classes.ArrayAllocSiteTracker;
import rr.meta.ArrayAccessInfo;
import rr.meta.ClassInfo;
import rr.meta.FieldInfo;
import rr.meta.MetaDataInfoMaps;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.state.ShadowVolatile;
import rr.tool.Tool;
import tools.util.CV;
import tools.util.MemoryUsageRecorder;
import acme.util.Assert;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.decorations.NullDefault;
import acme.util.decorations.DecorationFactory.Type;
import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;
import rr.meta.FieldAccessInfo;
import acme.util.option.CommandLineOption;

@Abbrev("SF")
public class SlimFastFTTool extends Tool implements BarrierListener<FastTrackBarrierState>, Opcodes {
	

	public static final int INIT_CV_SIZE = 32;
	public final ErrorMessage<FieldInfo> fieldErrors = ErrorMessages.makeFieldErrorMessage("SlimFast");
	public final ErrorMessage<ArrayAccessInfo> arrayErrors = ErrorMessages.makeArrayErrorMessage("SlimFast");

	public static final Decoration<ClassInfo,CV> classInitTime = MetaDataInfoMaps.getClasses().makeDecoration("SlimFast:InitTime", Type.MULTIPLE, 
			new DefaultValue<ClassInfo,CV>() {
		public CV get(ClassInfo t) {
			return new CV(INIT_CV_SIZE);
		}
	});

	public SlimFastFTTool(final String name, final Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		new BarrierMonitor<FastTrackBarrierState>(this, new DefaultValue<Object,FastTrackBarrierState>() {
			public FastTrackBarrierState get(Object k) {
				return new FastTrackBarrierState(ShadowLock.get(k));
			}
		});
	}

	static TLSObject ts_get_tls_obj(ShadowThread ts){Assert.panic("Bad"); return null;}
	static void ts_set_tls_obj(ShadowThread ts, TLSObject tls_obj){Assert.panic("Bad"); }

	static final Decoration<ShadowLock,FastTrackLockData> ftLockData = ShadowLock.makeDecoration("SlimFast:ShadowLock", DecorationFactory.Type.MULTIPLE,
			new DefaultValue<ShadowLock,FastTrackLockData>() { public FastTrackLockData get(final ShadowLock ld) { return new FastTrackLockData(ld); }});

	static final FastTrackLockData get(final ShadowLock ld) {
		return ftLockData.get(ld);
	}

	static final Decoration<ShadowVolatile,FastTrackVolatileData> ftVolatileData = ShadowVolatile.makeDecoration("SlimFast:shadowVolatile", DecorationFactory.Type.MULTIPLE,
			new DefaultValue<ShadowVolatile,FastTrackVolatileData>() { public FastTrackVolatileData get(final ShadowVolatile ld) { return new FastTrackVolatileData(ld); }});

	static final FastTrackVolatileData get(final ShadowVolatile ld) {
		return ftVolatileData.get(ld);
	}

	protected ShadowVar createHelper(AccessEvent e) {
		
		ShadowThread st=e.getThread();
		TLSObject tls_obj=ts_get_tls_obj(st);
		EpochPair epochpair = null;
		if(e.isWrite()){
		    epochpair = tls_obj.getEpochPairFromPool(tls_obj.clock, tls_obj.tid, tls_obj.clock);
		}else{
		    epochpair = tls_obj.getEpochPairFromPool(tls_obj.clock, tls_obj.tid, 0);
		}
		return epochpair;
	}


	@Override
	final public ShadowVar makeShadowVar(final AccessEvent fae) {
		if (fae.getKind() == Kind.VOLATILE) {
			FastTrackVolatileData vd = get(((VolatileAccessEvent)fae).getShadowVolatile());
			ShadowThread currentThread = fae.getThread();
			TLSObject tls_obj=ts_get_tls_obj(currentThread);
			vd.cv.max(tls_obj.cv);
			return super.makeShadowVar(fae);
		} else {
			return createHelper(fae);
		}
	}


	protected void maxAndIncEpochAndCV(ShadowThread currentThread, CV other, Event e) {
		TLSObject tls_obj=ts_get_tls_obj(currentThread);
		CV cv = tls_obj.cv;
		cv.max(other);
		cv.inc(currentThread.getTid());
		tls_obj.clock=cv.get(currentThread.getTid());
		tls_obj.reset();
	}


	protected void maxEpochAndCV(ShadowThread currentThread, CV other, Event e) {
		TLSObject tls_obj=ts_get_tls_obj(currentThread);
		CV cv = tls_obj.cv;
		cv.max(other);
		tls_obj.clock=cv.get(currentThread.getTid());
	}


	protected void incEpochAndCV(ShadowThread currentThread, Event e) {
		TLSObject tls_obj=ts_get_tls_obj(currentThread);
		CV cv = tls_obj.cv;
		cv.inc(currentThread.getTid());
		tls_obj.clock=cv.get(currentThread.getTid());
		tls_obj.reset();
	
	}

	
	static volatile boolean singleThreaded=true;
	@Override
	public void create(NewThreadEvent e) {
		ShadowThread currentThread = e.getThread();
		TLSObject tls_obj=new TLSObject();
		ts_set_tls_obj(currentThread, tls_obj);
		CV cv = tls_obj.cv;

		if (cv == null) {
			cv = new CV(INIT_CV_SIZE);
			tls_obj.cv=cv;
			cv.set(currentThread.getTid(),  0);
			this.incEpochAndCV(currentThread, null);
		}
		int tid=currentThread.getTid();
		tls_obj.tid=(byte)tid;
		if(tid!=0)
		{
			singleThreaded=false;
		}
		super.create(e);

	}


	@Override
	public void stop(ShadowThread td) {
		super.stop(td);
	}

	@Override
	public void acquire(final AcquireEvent ae) {
		final ShadowThread td = ae.getThread();
		final ShadowLock shadowLock = ae.getLock();
		final FastTrackLockData fhbLockData = get(shadowLock);

		this.maxEpochAndCV(td, fhbLockData.cv, ae);

		super.acquire(ae);
	}



	@Override
	public void release(final ReleaseEvent re) {
		final ShadowThread td = re.getThread();
		final ShadowLock shadowLock = re.getLock();
		final FastTrackLockData fhbLockData = get(shadowLock);
		TLSObject tls_obj=ts_get_tls_obj(td);
		CV cv = tls_obj.cv;
		fhbLockData.cv.max(cv);
		this.incEpochAndCV(td, re);

		super.release(re);

	}

	@Override
	public void access(final AccessEvent ae) {

		if(singleThreaded)
			return;

		final ShadowThread td = ae.getThread();
		final TLSObject tls_obj=ts_get_tls_obj(td);
		final int tdClock = tls_obj.clock;
		final CV tdCV = tls_obj.cv;
		final int tid=td.getTid();

		Object target = ae.getTarget();
		if (target == null) {
			CV initTime = classInitTime.get(((FieldAccessEvent)ae).getInfo().getField().getOwner());
			this.maxEpochAndCV(td, initTime, ae);

		}
		ShadowVar orig = ae.getOriginalShadow();
		EpochPair epochpair=null;
		
		while(true)
		{
			orig = ae.getOriginalShadow();

			if (orig instanceof EpochPair){
				
				EpochPair x = (EpochPair)orig;
				boolean success=x.checkAndUpdate(this,ae,td,(byte)tid,tls_obj,tdClock,tdCV);
				if(success)
				{
					return;
				}
				else
				{
					ae.putOriginalShadow(ae.getShadow());//get the most recent version of the shadow field
				}
			} else {
				super.access(ae);
				return ;
			}
		}	
	}

	enum ReturnCode{
		RETURN_TRUE, RETURN_FALSE, FALLTHROUGH
	}
	public static final ReturnCode fastReadCheck( ShadowThread td, TLSObject tls_obj, ShadowVar orig)
	{
		int tdClock=tls_obj.clock;
		EpochPair x=(EpochPair)orig;
		final int tid=td.getTid();
		if(x.lastReadClock==tdClock&&
		   x.lastReadTid==tid)
		{
			return ReturnCode.RETURN_TRUE;
		}
		final CV fhbCV=tls_obj.cv;
		if(x.lastWriteTid != tid && x.lastWriteClock > fhbCV.get(x.lastWriteTid))
		{
			return ReturnCode.RETURN_FALSE;
		}
		return ReturnCode.FALLTHROUGH;

	}
	public static boolean arrayReadFastPath(int index, AbstractArrayState arrayState, ShadowThread td) 
	{
		if(singleThreaded)
			return true;
		TLSObject tls_obj=ts_get_tls_obj(td);
		ShadowVar orig=arrayState.getState(index);
		final int tid=td.getTid();
		if(orig instanceof EpochPair)
		{
			switch(fastReadCheck(td,tls_obj,orig))
			{
				case FALLTHROUGH:
					break;
				case RETURN_TRUE:
					return true;
				case RETURN_FALSE:
					return false;
				default:
			}

			EpochPair x=(EpochPair)orig;
			boolean success= x.arrayReadFastPath(td,tid,tls_obj, arrayState, td.arrayUpdater,index);
			if(success)
			{
				return true;
			}
		}
		return false;
	}

	public static boolean arrayWriteFastPath(int index, AbstractArrayState arrayState, ShadowThread td) {
		if(singleThreaded)
			return true;
		TLSObject tls_obj=ts_get_tls_obj(td);
		ShadowVar orig=arrayState.getState(index);
		if(orig instanceof EpochPair)
		{
			final int tdClock=tls_obj.clock;
			EpochPair x=(EpochPair) orig;
			final int tid = td.getTid();

			if(x.lastWriteClock ==tdClock&&
			   x.lastWriteTid==tid)
			{
				return true;
			}
			final CV tdCV=tls_obj.cv;
			if(x.lastWriteTid!=tid && x.lastWriteClock > tdCV.get(x.lastWriteTid))
			{
				return false;
			}
			boolean success= x.arrayWriteFastPath(td,tid, tls_obj, arrayState, td.arrayUpdater, index);
			if(success)
			{
				return true;
			}
		}
		return false;
	}
	
	public static boolean fieldReadFastPath(int fadId, Object target, ShadowVar orig, ShadowThread td) 
	{
		if(singleThreaded)
			return true;

		TLSObject tls_obj=ts_get_tls_obj(td);
		final int tid=td.getTid();
		if(orig instanceof EpochPair)
		{
			switch(fastReadCheck(td,tls_obj,orig))
			{
				case FALLTHROUGH:
					break;
				case RETURN_TRUE:
					return true;
				case RETURN_FALSE:
					return false;
				default:
					
			}
			EpochPair x=(EpochPair)orig;
			boolean success= x.fieldReadFastPath(td,tid,tls_obj,fadId, target);
			if(success)
				return true;
		}
		return false;


	}
	public static boolean fieldWriteFastPath(int fadId, Object target, ShadowVar orig, ShadowThread td) 
	{
		if(singleThreaded)
			return true;
		TLSObject tls_obj=ts_get_tls_obj(td);
		if(orig instanceof EpochPair)
		{
			EpochPair x=(EpochPair) orig;
			final int tid = td.getTid();
			final int tdClock=tls_obj.clock;
			if(x.lastWriteClock ==tdClock&&
			   x.lastWriteTid==tid)
			{
				return true;
			}
			final CV tdCV=tls_obj.cv;
			if(x.lastWriteTid!=tid && x.lastWriteClock > tdCV.get(x.lastWriteTid))
			{
				return false;
			}
			if( x.lastReadTid != tid && x.lastReadClock > tdCV.get(x.lastReadTid))
			{
				return false;
			}
			boolean success= x.fieldWriteFastPath(td,tid, tls_obj,fadId, target);
			if(success)
				return true;
			
		}
		return false;

	}
		@Override
	public void volatileAccess(final VolatileAccessEvent fae) {
		final ShadowVar orig = fae.getOriginalShadow();
		final ShadowThread td = fae.getThread();

		FastTrackVolatileData vd = get((fae).getShadowVolatile());
		TLSObject tls_obj=ts_get_tls_obj(td);
		final CV cv = tls_obj.cv;
		if (fae.isWrite()) {
			vd.cv.max(cv);
			this.incEpochAndCV(td, fae); 		
		} else {
			cv.max(vd.cv);
		}
		super.volatileAccess(fae);
	}

	public void error(final AccessEvent ae, final int errorCase, final String prevOp, final int prevTid, final String curOp, final int curTid) {

	
		try {		
			if (ae instanceof FieldAccessEvent) {
			
				FieldAccessEvent fae = (FieldAccessEvent)ae;
				final FieldInfo fd = fae.getInfo().getField();
				final ShadowThread currentThread = fae.getThread();
				final Object target = fae.getTarget();

				fieldErrors.error(currentThread,
						fd,
						"Guard State", 					fae.getOriginalShadow(),
						"Current Thread",				toString(currentThread), 
						"Class",						target==null?fd.getOwner():target.getClass(),
						"Field",						Util.objectToIdentityString(target) + "." + fd, 
						"Prev Op",						prevOp + prevTid,
						"Cur Op",						curOp + curTid, 
						"Case", 						"#" + errorCase,
						"Stack",						ShadowThread.stackDumpForErrorMessage(currentThread) 
				);
				if (!fieldErrors.stillLooking(fd)) {
					advance(ae);
					return;
				}
			} else {
				
				ArrayAccessEvent aae = (ArrayAccessEvent)ae;
				final ShadowThread currentThread = aae.getThread();
				final Object target = aae.getTarget();
				if(arrayErrors==null)
					return;
				arrayErrors.error(currentThread,
						aae.getInfo(),
						"Alloc Site", 					ArrayAllocSiteTracker.allocSites.get(aae.getTarget()),
						"Guard State", 					aae.getOriginalShadow(),
						"Current Thread",				toString(currentThread), 
						"Array",						Util.objectToIdentityString(target) + "[" + aae.getIndex() + "]",
						"Prev Op",						prevOp + prevTid /*+ ("name = " + ShadowThread.get(prevTid).getThread().getName())*/, // would crash if the previous thread was null
						"Cur Op",						curOp + curTid + ("name = " + ShadowThread.get(curTid).getThread().getName()), 
						"Case", 						"#" + errorCase,
						"Stack",						ShadowThread.stackDumpForErrorMessage(currentThread) 
				);

				aae.getArrayState().specialize();

				if (!arrayErrors.stillLooking(aae.getInfo())) {
					advance(aae);
					return;
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			Assert.panic(e);
		}
			
	}

	@Override
	public void preStart(final StartEvent se) {

		final ShadowThread td = se.getThread();
		final ShadowThread forked = se.getNewThread();
		TLSObject tls_obj=ts_get_tls_obj(td);
		final CV curCV = tls_obj.cv;
		TLSObject forked_tls_obj=ts_get_tls_obj(forked);
		CV forkedCV = forked_tls_obj.cv;
		this.maxAndIncEpochAndCV(forked, curCV, se);
		this.incEpochAndCV(td, se);
		super.preStart(se);
	}


	@Override
	public void postJoin(final JoinEvent je) {
		final ShadowThread td = je.getThread();
		final ShadowThread joining = je.getJoiningThread();

		// this test tells use whether the tid has been reused already or not.  Necessary
		// to still account for stopped thread, even if that thread's tid has been reused,
		// but good to know if this is happening alot...
		TLSObject tls_obj=ts_get_tls_obj(joining);
		if (joining.getTid() != -1) {
			this.incEpochAndCV(joining, je);
			this.maxEpochAndCV(td, tls_obj.cv, je);
		} else {
			Yikes.yikes("Joined after tid got reused --- don't touch anything related to tid here!");
			this.maxEpochAndCV(td, tls_obj.cv, je);
		}

		super.postJoin(je);	
	}
	@Override
	public void preNotify(NotifyEvent we) {
		super.preNotify(we);
	}
	

	@Override
	public void preWait(WaitEvent we) {
		FastTrackLockData lockData = get(we.getLock());
		ShadowThread weTd=we.getThread();
		TLSObject tls_obj=ts_get_tls_obj(weTd);
		synchronized(lockData) {
			lockData.cv.max(tls_obj.cv);
		}
		this.incEpochAndCV(weTd, we);
	
		super.preWait(we);
	}
	@Override
	public void postWait(WaitEvent we) { 
		FastTrackLockData lockData = get(we.getLock());
		this.maxEpochAndCV(we.getThread(), lockData.cv, we);
		super.postWait(we);
	}

	public static String toString(final ShadowThread td) {
		TLSObject tls_obj=ts_get_tls_obj(td);
		return String.format("[tid=%-2d   cv=%s   epoch=%s]", td.getTid(), tls_obj.cv, Integer.toHexString(tls_obj.clock));
	}


	private final Decoration<ShadowThread, CV> cvForExit = 
		ShadowThread.makeDecoration("SF:barrier", DecorationFactory.Type.MULTIPLE, new NullDefault<ShadowThread, CV>());

	public void preDoBarrier(BarrierEvent<FastTrackBarrierState> be) {
		FastTrackBarrierState ftbe = be.getBarrier();
		ShadowThread currentThread = be.getThread();
		CV entering = ftbe.getEntering();
		TLSObject tls_obj=ts_get_tls_obj(currentThread);
		entering.max(tls_obj.cv);
		cvForExit.set(currentThread, entering);
	}

	public void postDoBarrier(BarrierEvent<FastTrackBarrierState> be) {
		FastTrackBarrierState ftbe = be.getBarrier();
		ShadowThread currentThread = be.getThread();
		CV old = cvForExit.get(currentThread);
		ftbe.reset(old);
		this.maxAndIncEpochAndCV(currentThread, old, be);
		TLSObject tls_obj=ts_get_tls_obj(currentThread);
	}

	@Override
	public void classInitialized(ClassInitializedEvent e) {
		final ShadowThread currentThread = e.getThread();
		TLSObject tls_obj=ts_get_tls_obj(currentThread);
		final CV cv = tls_obj.cv;
		Util.log("Class Init for " + e + " -- " + cv);
		classInitTime.get(e.getRRClass()).max(cv);
		this.incEpochAndCV(currentThread, e);
		super.classInitialized(e);
	}
	@Override
	public void classAccessed(ClassAccessedEvent e) {
		CV initTime = classInitTime.get(e.getRRClass());
		ShadowThread currentThread = e.getThread();
		this.maxEpochAndCV(currentThread, initTime, e); 
	}
	@Override
	public void printXML(XMLWriter xml) {
		super.printXML(xml);
		for (ShadowThread td : ShadowThread.getThreads()) {
			xml.print("thread", toString(td));		
		}
	}

}
