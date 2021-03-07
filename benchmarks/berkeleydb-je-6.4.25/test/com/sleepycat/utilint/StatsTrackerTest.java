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

package com.sleepycat.utilint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.Test;

import com.sleepycat.util.test.TestBase;

public class StatsTrackerTest extends TestBase {

    enum TestType {GET, PUT, DELETE} ;

    /*
     * Test thread stack trace dumping
     */
    @Test
    public void testActivityCounter() 
        throws InterruptedException {
     
        Integer maxNumThreadDumps = 3;

        Logger logger = Logger.getLogger("test");
        StatsTracker<TestType> tracker = 
            new StatsTracker<TestType>(TestType.values(),
                                       logger,
                                       2,
                                       1,
                                       maxNumThreadDumps,
                                       100);

        /* 
         * If there is only one concurrent thread, there should be no thread 
         * dumps.
         */
        for (int i = 0; i < 20; i++) {
            long startA = tracker.markStart();
            Thread.sleep(10);
            tracker.markFinish(TestType.GET, startA);
        }

        /* Did we see some thread dumps? */
        assertEquals(0, tracker.getNumCompletedDumps() );

        /*
         * Simulate three concurrent threads. There should be automatic thread
         * dumping, because the tracker is configured to dump when there are
         * more than two concurrent threads with operations of > 1 ms.
         */
        for (int i = 0; i < 20; i++) {
            long startA = tracker.markStart();
            long startB = tracker.markStart();
            long startC = tracker.markStart();
            Thread.sleep(10);
            tracker.markFinish(TestType.GET, startA);
            tracker.markFinish(TestType.GET, startB);
            tracker.markFinish(TestType.GET, startC);
        }

        long expectedMaxDumps = maxNumThreadDumps;

        /* Did we see some thread dumps? */
        assertTrue(tracker.getNumCompletedDumps() > 1);
        assertTrue(tracker.getNumCompletedDumps() <= expectedMaxDumps);
    }
}
