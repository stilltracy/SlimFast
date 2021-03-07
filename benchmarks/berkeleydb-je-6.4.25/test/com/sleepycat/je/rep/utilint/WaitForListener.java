/*-
 *
 *  This file is part of Oracle Berkeley DB Java Edition
 *  Copyright (C) 2002, 2015 Oracle and/or its affiliates.  All rights reserved.
 *
 *  Oracle Berkeley DB Java Edition is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation, version 3.
 *
 *  Oracle Berkeley DB Java Edition is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License in
 *  the LICENSE file along with Oracle Berkeley DB Java Edition.  If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 *  An active Oracle commercial licensing agreement for this product
 *  supercedes this license.
 *
 *  For more information please contact:
 *
 *  Vice President Legal, Development
 *  Oracle America, Inc.
 *  5OP-10
 *  500 Oracle Parkway
 *  Redwood Shores, CA 94065
 *
 *  or
 *
 *  berkeleydb-info_us@oracle.com
 *
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  EOF
 *
 */

package com.sleepycat.je.rep.utilint;

import static com.sleepycat.je.rep.ReplicatedEnvironment.State.MASTER;
import static com.sleepycat.je.rep.ReplicatedEnvironment.State.REPLICA;
import static com.sleepycat.je.rep.ReplicatedEnvironment.State.UNKNOWN;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.sleepycat.je.rep.ReplicatedEnvironment.State;
import com.sleepycat.je.rep.StateChangeEvent;
import com.sleepycat.je.rep.StateChangeListener;

/**
 * Utility class to wait for one of a set of state change events.
 *
 * This is the preferred class to use as an alternative to the WaitForXXX
 * sequence of classes in this package.
 */
public class WaitForListener implements StateChangeListener {

    final CountDownLatch latch = new CountDownLatch(1);
    final Set<State>  waitStates;

    private boolean success = true;

    public WaitForListener(State... states) {
        waitStates = new HashSet<State>(Arrays.asList(states));
    }

    public void stateChange(StateChangeEvent stateChangeEvent) {

        if (waitStates.contains(stateChangeEvent.getState())) {
            latch.countDown();
            return;
        }

        if (stateChangeEvent.getState().isDetached()) {
            /* It will never transition out of this state. */
            success = false;
            latch.countDown();
            return;
        }

        /* Some other intermediate state, not of interest; ignore it. */
    }

    public boolean await()
        throws InterruptedException {

        latch.await();
        return success;
    }

    /**
     * Specialized listener for Active states
     */
    public static class Active extends WaitForListener {
        public Active() {
            super(MASTER, REPLICA);
        }
    }

    /**
     * Specialized listener for transition to UNKNOWN
     */
    public static class Unknown extends WaitForListener {
        public Unknown() {
            super(UNKNOWN);
        }
    }
}
