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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.rep.utilint.SizeAwaitMap.Predicate;
import com.sleepycat.util.test.TestBase;

public class SizeAwaitMapTest extends TestBase {

    SizeAwaitMap<Integer, Integer> smap = null;
    SizeWaitThread testThreads[];
    AtomicInteger doneThreads;

    CountDownLatch startLatch = null;

    /* Large number to help expose concurrency issues, if any. */
    static final int threadCount = 200;

    /**
     * Set up the test, creating the SizeAwaitMap with the specified predicate.
     */
    private void setUp(final Predicate<Integer> predicate)
        throws Exception {

        smap = new SizeAwaitMap<Integer,Integer>(null, predicate);
        testThreads = new SizeWaitThread[threadCount];
        doneThreads = new AtomicInteger(0);
        startLatch = new CountDownLatch(threadCount);
        for (int i=0; i < threadCount; i++) {
            testThreads[i] =
                new SizeWaitThread(i, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            testThreads[i].start();
        }
        // Wait for threads to start up
        startLatch.await();
    }

    private void checkLiveThreads(int checkStart) {
        for (int j=checkStart; j < threadCount; j++) {
            assertTrue(testThreads[j].isAlive());
        }
        assertEquals(checkStart, doneThreads.intValue());
    }

    /**
     * Tests basic put/remove operations
     */
    @Test
    public void testBasic() throws Exception {
        setUp(null);
        joinThread(0);
        assertEquals(1, doneThreads.intValue());
        for (int i=1; i < threadCount; i++) {
            assertTrue(testThreads[i].isAlive());
            smap.put(i, i);
            joinThread(i);
            assertTrue(testThreads[i].success);
            // All subsequent threads continue to live
            checkLiveThreads(i+1);

            // Remove should have no impact
            smap.remove(i);
            checkLiveThreads(i+1);

            // Re-adding should have no impact
            smap.put(i, i);
            checkLiveThreads(i+1);
        }
    }

    /*
     * Tests clear operation.
     */
    @Test
    public void testClear() throws Exception {
        setUp(null);
        joinThread(0);
        assertEquals(1, doneThreads.intValue());
        /* Wait for the threads */
        while (smap.latchCount()!= (threadCount-1)) {
            Thread.sleep(10);
        }

        smap.clear(new MyTestException());
        assertTrue(smap.size() == 0);
        for (int i=1; i < threadCount; i++) {
            joinThread(i);
            assertTrue(testThreads[i].cleared);
            assertFalse(testThreads[i].interrupted);
        }
        assertEquals(threadCount, doneThreads.intValue());
    }

    /**
     * Tests put and remove operations with a predicate.
     */
    @Test
    public void testPredicate() throws Exception {
        /* Only count even values */
        setUp(new Predicate<Integer>() {
                @Override
                public boolean match(final Integer i) {
                    return ((i % 2) == 0);
                }
            });
        joinThread(0);
        assertEquals(1, doneThreads.intValue());
        for (int i = 1; i < threadCount; i++) {
            assertTrue(testThreads[i].isAlive());

            // Odd value
            int value = (2 * i) - 1;
            smap.put(value, value);

            // No change
            checkLiveThreads(i);

            // Remove should have no impact
            smap.remove(value);
            checkLiveThreads(i);

            // Re-adding should have no impact
            smap.put(value, value);
            checkLiveThreads(i);

            // Even value
            value++;
            smap.put(value, value);
            joinThread(i);
            assertTrue(testThreads[i].success);
            // All subsequent threads continue to live
            checkLiveThreads(i+1);

            // Remove should have no impact
            smap.remove(value);
            checkLiveThreads(i+1);

            // Re-adding should have no impact
            smap.put(value, value);
            checkLiveThreads(i+1);
        }
    }

    /**
     * Threads which wait for specific map sizes.
     */
    private class SizeWaitThread extends Thread {

        /* The size to wait for. */
        final int size;
        final long timeout;
        final TimeUnit unit;
        boolean interrupted = false;
        boolean cleared = false;
        boolean success = false;

        SizeWaitThread(int size, long timeout, TimeUnit unit) {
            this.size = size;
            this.timeout = timeout;
            this.unit = unit;
        }

        public void run() {
            startLatch.countDown();
            try {
                success = smap.sizeAwait(size, timeout, unit);
            } catch (MyTestException mte) {
                cleared = true;
            } catch (InterruptedException e) {
                interrupted = true;
            } finally {
                doneThreads.incrementAndGet();
            }

        }
    }

    @SuppressWarnings("serial")
    private class MyTestException extends DatabaseException {
        MyTestException() {
            super("testing");
        }
    }

    /**
     * Wait no more than 5 seconds to join the specified thread and check that
     * it died.
     */
    private void joinThread(final int threadNum) throws InterruptedException {
        testThreads[threadNum].join(5000);
        assertFalse("thread alive", testThreads[threadNum].isAlive());
    }
}
