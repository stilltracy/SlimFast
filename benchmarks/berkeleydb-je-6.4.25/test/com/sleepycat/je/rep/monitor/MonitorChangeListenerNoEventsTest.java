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

package com.sleepycat.je.rep.monitor;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.sleepycat.je.rep.MemberNotFoundException;
import com.sleepycat.je.rep.ReplicationNode;
import com.sleepycat.je.utilint.TestHookAdapter;

/**
 * Perform the tests from MonitorChangeListenerTest, but with event delivery
 * disabled, to test in the presence of network failures.
 */
public class MonitorChangeListenerNoEventsTest
        extends MonitorChangeListenerTest {

    @BeforeClass
    public static void classSetup() {
        MonitorService.processGroupChangeHook =
            new TestHookAdapter<GroupChangeEvent>() {
            @Override
            public void doHook(GroupChangeEvent event) {
                ReplicationNode node;
                try {
                    node = event.getRepGroup().getMember(event.getNodeName());
                } catch (MemberNotFoundException e) {
                    node = null;
                }
                /*
                 * Deliver monitor events because they are only generated
                 * locally and are not simulated by pings.
                 */
                if ((node != null) && !node.getType().isMonitor()) {
                    throw new IllegalStateException("don't deliver");
                }
            }
        };
        MonitorService.processJoinGroupHook =
            new TestHookAdapter<JoinGroupEvent>() {
            @Override
            public void doHook(JoinGroupEvent event) {
                throw new IllegalStateException("don't deliver");
            }
        };
        MonitorService.processLeaveGroupHook =
            new TestHookAdapter<LeaveGroupEvent>() {
            @Override
            public void doHook(LeaveGroupEvent event) {
                throw new IllegalStateException("don't deliver");
            }
        };
    }

    @AfterClass
    public static void classCleanup() {
        MonitorService.processGroupChangeHook = null;
        MonitorService.processJoinGroupHook = null;
        MonitorService.processLeaveGroupHook = null;
    }

    /**
     * When event delivery is disabled, the ping produces the leave event, but
     * it uses a different LeaveReason, so disable this check.
     */
    void checkShutdownReplicaLeaveReason(final LeaveGroupEvent event) {
    }
}
