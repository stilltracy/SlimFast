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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;

import com.sleepycat.je.rep.impl.RepTestBase;

public class MonitorTestBase extends RepTestBase {

    /* The monitor being tested. */
    protected Monitor monitor;

    @Override
    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        monitor = createMonitor(100, "mon10000");
    }

    @Override
    @After
    public void tearDown()
        throws Exception {

        super.tearDown();
        monitor.shutdown();
    }

    protected class TestChangeListener implements MonitorChangeListener {
        String masterNodeName;

        volatile NewMasterEvent masterEvent;
        volatile GroupChangeEvent groupEvent;
        volatile JoinGroupEvent joinEvent;
        volatile LeaveGroupEvent leaveEvent;

        /* Statistics records how may events happen. */
        private final AtomicInteger masterEvents = new AtomicInteger(0);
        private final AtomicInteger groupAddEvents = new AtomicInteger(0);
        private final AtomicInteger groupRemoveEvents = new AtomicInteger(0);
        private final AtomicInteger joinGroupEvents = new AtomicInteger(0);
        private final AtomicInteger leaveGroupEvents = new AtomicInteger(0);

        /* Barrier to test whether event happens. */
        volatile CountDownLatch masterBarrier;
        volatile CountDownLatch groupBarrier;
        volatile CountDownLatch joinGroupBarrier;
        volatile CountDownLatch leaveGroupBarrier;

        public TestChangeListener() {}

        public void notify(NewMasterEvent newMasterEvent) {
            logger.info("notify " + newMasterEvent);
            masterEvents.incrementAndGet();
            masterNodeName = newMasterEvent.getNodeName();
            masterEvent = newMasterEvent;
            countDownBarrier(masterBarrier);
        }

        public void notify(GroupChangeEvent groupChangeEvent) {
            logger.info("notify " + groupChangeEvent);
            switch (groupChangeEvent.getChangeType()) {
                case ADD:
                    groupAddEvents.incrementAndGet();
                    break;
                case REMOVE:
                    groupRemoveEvents.incrementAndGet();
                    break;
                default:
                    throw new IllegalStateException("Unexpected change type.");
            }
            groupEvent = groupChangeEvent;
            countDownBarrier(groupBarrier);
        }

        public void notify(JoinGroupEvent joinGroupEvent) {
            logger.info("notify " + joinGroupEvent);
            joinGroupEvents.incrementAndGet();
            joinEvent = joinGroupEvent;
            countDownBarrier(joinGroupBarrier);
        }

        public void notify(LeaveGroupEvent leaveGroupEvent) {
            logger.info("notify " + leaveGroupEvent);
            leaveGroupEvents.incrementAndGet();
            leaveEvent = leaveGroupEvent;
            countDownBarrier(leaveGroupBarrier);
        }

        void awaitEvent(CountDownLatch latch)
            throws InterruptedException {

            awaitEvent(null, latch);
        }

        void awaitEvent(String message, CountDownLatch latch)
            throws InterruptedException {

            latch.await(30, TimeUnit.SECONDS);
            assertEquals(((message != null) ? (message + ": ") : "") +
                         "Events not received after timeout:",
                         0, latch.getCount());
        }

        private void countDownBarrier(CountDownLatch barrier) {
            if (barrier != null && barrier.getCount() > 0) {
                barrier.countDown();
            }
        }

        int getMasterEvents() {
            return masterEvents.get();
        }

        void clearMasterEvents() {
            masterEvents.set(0);
        }

        int getGroupAddEvents() {
            return groupAddEvents.get();
        }

        void clearGroupAddEvents() {
            groupAddEvents.set(0);
        }

        int getGroupRemoveEvents() {
            return groupRemoveEvents.get();
        }

        void clearGroupRemoveEvents() {
            groupRemoveEvents.set(0);
        }

        int getJoinGroupEvents() {
            return joinGroupEvents.get();
        }

        void clearJoinGroupEvents() {
            joinGroupEvents.set(0);
        }

        int getLeaveGroupEvents() {
            return leaveGroupEvents.get();
        }

        void clearLeaveGroupEvents() {
            leaveGroupEvents.set(0);
        }

    }
}
