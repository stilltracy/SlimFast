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

package com.sleepycat.je.rep.dual;

public class DatabaseTest extends com.sleepycat.je.DatabaseTest {

    /* Non-transactional tests. */

    @Override
    public void testOpenCursor() {
    }

    @Override
    public void testOpenCursorAfterEnvInvalidation() {
    }

    @Override
    public void testPreloadBytesExceedsCache() {
    }

    @Override
    public void testPreloadEntireDatabase() {
    }

    @Override
    public void testPutNoOverwriteInADupDbNoTxn() {
    }

    @Override
    public void testDeferredWriteDatabaseCount() {
    }

    @Override
    public void testDeferredWriteDatabaseCountDups() {
    }

    /*
     * Replication disturbs the cache due to the presence of the feeder and as
     * a consequence invalidates the assumptions underlying the test.
     */
    @Override
    public void testPreloadAllInCache() {
    }

    @Override
    public void testPreloadAllInCacheOffHeap() {
    }

    @Override
    public void testPreloadCacheMemoryLimit() {
    }

    @Override
    public void testPreloadMultipleDatabases() {
    }

    @Override
    public void testPreloadTimeLimit() {
    }

    @Override
    public void testPreloadInternalMemoryLimit() {
    }

    @Override
    public void testPreloadWithProgress() {
    }

    @Override
    public void testDbPreempted() {
    }
}
