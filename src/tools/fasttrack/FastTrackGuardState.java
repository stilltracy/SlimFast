/******************************************************************************

Copyright (c) 2010, Cormac Flanagan (University of California, Santa Cruz)
                    and Stephen Freund (Williams College) 

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

 * Neither the names of the University of California, Santa Cruz
      and Williams College nor the names of its contributors may be
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

package tools.fasttrack;

import rr.state.ShadowVar;
import tools.util.CV;
//import tools.util.Epoch;
import rr.tool.Tool;
import rr.state.ShadowThread;

public class FastTrackGuardState extends CV implements ShadowVar {
	
	public volatile int/*clock*/ lastWriteClock;
	public volatile int/*clock*/ lastReadClock;
	public volatile byte lastReadTid=0;
	public volatile byte lastWriteTid=0;
	
	public FastTrackGuardState() {
		super(0);
	}
	
	public FastTrackGuardState(boolean isWrite, byte tid, int clock) {
		super(0);
		init(isWrite, tid, clock);
	} 
	
	public void init(boolean isWrite, byte tid, int clock) {
		if (isWrite) {
			lastReadClock = 0;
			lastReadTid=tid;
			lastWriteTid=tid;
			lastWriteClock = clock; 
		} else {
			lastReadTid=tid;
			lastWriteTid=tid;
			lastWriteClock = 0;
			lastReadClock = clock; 
		}		
	}

	@Override
	public void makeCV(int i) {
		super.makeCV(i);
	}

	@Override
	public String toString() {
		return String.format("[W=%s R=%s CV=%s]", lastWriteClock, lastReadClock, a == null ? "null" : super.toString());
	}
}
