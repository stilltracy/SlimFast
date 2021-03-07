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

package com.sleepycat.je.latch;

import static com.sleepycat.je.latch.LatchStatDefinition.LATCH_CONTENTION;
import static com.sleepycat.je.latch.LatchStatDefinition.LATCH_NOWAIT_SUCCESS;
import static com.sleepycat.je.latch.LatchStatDefinition.LATCH_NOWAIT_UNSUCCESS;
import static com.sleepycat.je.latch.LatchStatDefinition.LATCH_NO_WAITERS;
import static com.sleepycat.je.latch.LatchStatDefinition.LATCH_RELEASES;
import static com.sleepycat.je.latch.LatchStatDefinition.LATCH_SELF_OWNED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Test;

import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.dbi.EnvironmentFailureReason;
import com.sleepycat.je.junit.JUnitThread;
import com.sleepycat.je.utilint.StatGroup;
import com.sleepycat.util.test.TestBase;

public class LatchTest extends TestBase {
    private Latch latch1 = null;
    private Latch latch2 = null;
    private JUnitThread tester1 = null;
    private JUnitThread tester2 = null;

    static private final boolean DEBUG = false;

    private void debugMsg(String message) {
        if (DEBUG) {
            System.out.println(Thread.currentThread().toString()
                               + " " +  message);
        }
    }

    private void createExclusiveLatches() {
        latch1 = LatchFactory.createExclusiveLatch(
            LatchFactory.createTestLatchContext("LatchTest-latch1"),
            true /*collectStats*/);
        latch2 = LatchFactory.createExclusiveLatch(
            LatchFactory.createTestLatchContext("LatchTest-latch2"),
            true /*collectStats*/);
    }

    @After
    public void tearDown() {
        latch1 = null;
        latch2 = null;
    }

    @Test
    public void testDebugOutput() {

        /* Stupid test solely for the sake of code coverage. */
        createExclusiveLatches();

        /* Acquire a latch. */
        latch1.acquireExclusive();

        LatchSupport.btreeLatchesHeldToString();
    }

    @Test
    public void testAcquireAndReacquire()
        throws Throwable {

        createExclusiveLatches();
        JUnitThread tester =
            new JUnitThread("testAcquireAndReacquire") {
                @Override
                public void testBody() {

                    /* Acquire a latch. */
                    latch1.acquireExclusive();

                    /* Try to acquire it again -- should fail. */
                    try {
                        latch1.acquireExclusive();
                        fail("didn't catch UNEXPECTED_STATE");
                    } catch (EnvironmentFailureException expected) {
                        assertSame(EnvironmentFailureReason.
                                   UNEXPECTED_STATE, expected.getReason());
                        assertTrue
                            (latch1.getStats().getInt
                             (LATCH_SELF_OWNED) == 1);
                    }

                    /* Release it. */
                    latch1.release();

                    /* Release it again -- should fail. */
                    try {
                        latch1.release();
                        fail("didn't catch UNEXPECTED_STATE");
                    } catch (EnvironmentFailureException expected) {
                        assertSame(EnvironmentFailureReason.
                                   UNEXPECTED_STATE, expected.getReason());
                    }
                }
            };

        tester.doTest();
    }

    @Test
    public void testAcquireAndReacquireShared()
        throws Throwable {

        final SharedLatch latch = LatchFactory.createSharedLatch(
            LatchFactory.createTestLatchContext("LatchTest-latch2"), false);

        JUnitThread tester =
            new JUnitThread("testAcquireAndReacquireShared") {
                @Override
                public void testBody() {

                    /* Acquire a shared latch. */
                    latch.acquireShared();
                    assert latch.isOwner();

                    /* Acquire it again -- should fail. */
                    try {
                        latch.acquireShared();
                        fail("didn't catch UNEXPECTED_STATE");
                    } catch (EnvironmentFailureException expected) {
                        assertSame(EnvironmentFailureReason.UNEXPECTED_STATE,
                                   expected.getReason());
                    }
                    assert latch.isOwner();

                    /* Release it. */
                    latch.release();

                    /* Release it again -- should fail. */
                    try {
                        latch.release();
                        fail("didn't catch UNEXPECTED_STATE");
                    } catch (EnvironmentFailureException e) {
                        assertSame(EnvironmentFailureReason.
                                   UNEXPECTED_STATE, e.getReason());
                    }
                }
            };

        tester.doTest();
    }

    /*
     * Do a million acquire/release pairs.  The junit output will tell us how
     * long it took.
     */
    @Test
    public void testAcquireReleasePerformance()
        throws Throwable {

        createExclusiveLatches();
        JUnitThread tester =
            new JUnitThread("testAcquireReleasePerformance") {
                @Override
                public void testBody() {
                    final int N_PERF_TESTS = 1000000;
                    for (int i = 0; i < N_PERF_TESTS; i++) {
                        /* Acquire a latch */
                        latch1.acquireExclusive();
                        /* Release it. */
                        latch1.release();
                    }
                    StatGroup stats = latch1.getStats();
                    stats.toString();
                    assertTrue(stats.getInt(LATCH_NO_WAITERS) == N_PERF_TESTS);
                    assertTrue(stats.getInt(LATCH_RELEASES) == N_PERF_TESTS);
                }
            };

        tester.doTest();
    }

    /* Test latch waiting. */

    @Test
    public void testWait()
        throws Throwable {

        createExclusiveLatches();
        for (int i = 0; i < 10; i++) {
            doTestWait();
        }
    }

    private int nAcquiresWithContention = 0;

    public void doTestWait()
        throws Throwable {

        final AtomicBoolean t1Acquired = new AtomicBoolean(false);

        tester1 =
            new JUnitThread("testWait-Thread1") {
                @Override
                public void testBody() {
                    /* Acquire a latch. */
                    latch1.acquireExclusive();
                    t1Acquired.set(true);

                    /* Wait for tester2 to try to acquire the latch. */
                    while (latch1.getNWaiters() == 0) {
                        Thread.yield();
                    }

                    latch1.release();
                }
            };

        tester2 =
            new JUnitThread("testWait-Thread2") {
                @Override
                public void testBody() {
                    /* Wait for tester1 to start. */
                    while (!t1Acquired.get()) {
                        Thread.yield();
                    }

                    /* Acquire a latch. */
                    latch1.acquireExclusive();

                    assertTrue(latch1.getStats().getInt(LATCH_CONTENTION)
                               == ++nAcquiresWithContention);

                    /* Release it. */
                    latch1.release();
                }
            };

        tester1.start();
        tester2.start();
        tester1.finishTest();
        tester2.finishTest();
    }

    @Test
    public void testAcquireNoWait()
        throws Throwable {

        final AtomicBoolean t1Acquired = new AtomicBoolean(false);
        final AtomicBoolean t2TryAcquire = new AtomicBoolean(false);
        final AtomicBoolean t1Released = new AtomicBoolean(false);

        createExclusiveLatches();
        tester1 =
            new JUnitThread("testWait-Thread1") {
                @Override
                public void testBody() {
                    /* Acquire a latch. */
                    debugMsg("Acquiring Latch");
                    latch1.acquireExclusive();
                    t1Acquired.set(true);

                    /* Wait for tester2 to try to acquire the latch. */
                    debugMsg("Waiting for other thread");
                    while (!t2TryAcquire.get()) {
                        Thread.yield();
                    }

                    debugMsg("Releasing the latch");
                    latch1.release();
                    t1Released.set(true);
                }
            };

        tester2 =
            new JUnitThread("testWait-Thread2") {
                @Override
                public void testBody() {
                    /* Wait for tester1 to start. */
                    debugMsg("Waiting for T1 to acquire latch");
                    while (!t1Acquired.get()) {
                        Thread.yield();
                    }

                    /*
                     * Attempt Acquire with no wait -- should fail since
                     * tester1 has it.
                     */
                    debugMsg("Acquiring no wait");
                    assertFalse(latch1.acquireExclusiveNoWait());
                    assertTrue(latch1.getStats().getInt
                               (LATCH_NOWAIT_UNSUCCESS) == 1);
                    t2TryAcquire.set(true);

                    debugMsg("Waiting for T1 to release latch");
                    while (!t1Released.get()) {
                        Thread.yield();
                    }

                    /*
                     * Attempt Acquire with no wait -- should succeed now that
                     * tester1 is done.
                     */
                    debugMsg("Acquiring no wait - 2");
                    assertTrue(latch1.acquireExclusiveNoWait());
                    assertTrue(latch1.getStats().getInt
                               (LATCH_NOWAIT_SUCCESS) == 1);

                    /*
                     * Attempt Acquire with no wait again -- should throw
                     * exception since we already have it.
                     */
                    debugMsg("Acquiring no wait - 3");
                    try {
                        latch1.acquireExclusiveNoWait();
                        fail("didn't throw UNEXPECTED_STATE");
                    } catch (EnvironmentFailureException expected) {
                        assertSame(EnvironmentFailureReason.
                                   UNEXPECTED_STATE, expected.getReason());
                    }

                    /* Release it. */
                    debugMsg("releasing the latch");
                    latch1.release();
                }
            };

        tester1.start();
        tester2.start();
        tester1.finishTest();
        tester2.finishTest();
    }

    /* State for testMultipleWaiters. */
    private final int N_WAITERS = 5;

    /* A JUnitThread that holds the waiter number. */
    private class MultiWaiterTestThread extends JUnitThread {
        private final int waiterNumber;
        public MultiWaiterTestThread(String name, int waiterNumber) {
            super(name);
            this.waiterNumber = waiterNumber;
        }
    }

    @Test
    public void testMultipleWaiters()
        throws Throwable {

        createExclusiveLatches();

        JUnitThread[] waiterThreads = new JUnitThread[N_WAITERS];

        final AtomicBoolean t1Acquired = new AtomicBoolean(false);

        tester1 =
            new JUnitThread("testWait-Thread1") {
                @Override
                public void testBody() {

                    /* Acquire a latch. */
                    debugMsg("About to acquire latch");
                    latch1.acquireExclusive();
                    debugMsg("acquired latch");
                    t1Acquired.set(true);

                    /*
                     * Wait for all other testers to be waiting on the latch.
                     */
                    while (latch1.getNWaiters() < N_WAITERS) {
                        Thread.yield();
                    }

                    debugMsg("About to release latch");
                    latch1.release();
                }
            };

        for (int i = 0; i < N_WAITERS; i++) {
            waiterThreads[i] =
                new MultiWaiterTestThread("testWait-Waiter" + i, i) {
                    @Override
                    public void testBody() {

                        int waiterNumber =
                            ((MultiWaiterTestThread)
                             Thread.currentThread()).waiterNumber;

                        /* Wait for tester1 to start. */
                        debugMsg("Waiting for main to acquire latch");
                        while (!t1Acquired.get()) {
                            Thread.yield();
                        }

                        /*
                         * Wait until it's our turn to try to acquire the
                         * latch.
                         */
                        debugMsg("Waiting for our turn to acquire latch");
                        while (latch1.getNWaiters() < waiterNumber) {
                            Thread.yield();
                        }

                        /* Try to acquire the latch */
                        debugMsg("About to acquire latch");
                        latch1.acquireExclusive();

                        debugMsg("getNWaiters: " + latch1.getNWaiters());
                        assertTrue(latch1.getNWaiters() ==
                                   (N_WAITERS - waiterNumber - 1));

                        /* Release it. */
                        debugMsg("About to release latch");
                        latch1.release();
                    }
                };
        }

        tester1.start();

        for (int i = 0; i < N_WAITERS; i++) {
            waiterThreads[i].start();
        }

        tester1.finishTest();
        for (int i = 0; i < N_WAITERS; i++) {
            waiterThreads[i].finishTest();
        }
    }
}
