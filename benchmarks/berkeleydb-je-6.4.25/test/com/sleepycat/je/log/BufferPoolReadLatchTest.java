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

package com.sleepycat.je.log;

import static com.sleepycat.je.latch.LatchStatDefinition.LATCH_RELEASES;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.util.DualTestCase;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.StatGroup;
import com.sleepycat.util.test.SharedTestUtils;

/**
 * Checks that we don't have to latch the buffer pool to check for LSNs that
 * are outside the pool's range. This is important to avoid read contention.
 * [#19642]
 */
public class BufferPoolReadLatchTest extends DualTestCase {
    private File envHome;
    private Environment env;

    public BufferPoolReadLatchTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @Test
    public void testBufferPoolReadLatch()
        throws Throwable {

        /* Open env with tiny cache to cause cache misses. */
        final EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        envConfig.setCacheSize(1 << 20);
        envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_CLEANER,
                                 "false");
        envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_EVICTOR,
                                 "false");
        envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_CHECKPOINTER,
                                 "false");
        envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_IN_COMPRESSOR,
                                 "false");
        env = create(envHome, envConfig);

        /* Open db. */
        final DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        final Database db = env.openDatabase(null, "foo", dbConfig);

        /* Write enough data to cycle twice through log buffers. */
        final DatabaseEntry key = new DatabaseEntry();
        final DatabaseEntry data = new DatabaseEntry(new byte[1024]);
        final int logBufMemSize = (1 << 20) * 3; // 3 MB
        final int nRecords = (logBufMemSize * 2) / 1024;
        for (int i = 0; i < nRecords; i += 1) {
            IntegerBinding.intToEntry(i, key);
            final OperationStatus status = db.putNoOverwrite(null, key, data);
            assertSame(OperationStatus.SUCCESS, status);
        }

        /* Get and clear buffer pool latch stats. */
        final StatGroup latchStats = DbInternal.getEnvironmentImpl(env).
                                                getLogManager().
                                                getBufferPoolLatchStats();
        latchStats.clear();

        /* Read first half of records, which should not be in log buffers. */
        for (int i = 0; i < (nRecords / 2); i += 1) {
            IntegerBinding.intToEntry(i, key);
            final OperationStatus status = db.get(null, key, data, null);
            assertSame(OperationStatus.SUCCESS, status);
        }

        /*
         * Check for a small number of latches.  Check release count because it
         * is simple (there are many acquire counts).  Before the enhancement
         * [#19642], there were around 3,000 latches.  After the enhancement
         * there were only 2 latches, but this number could be variable.
         */
        final int nLatches = latchStats.getInt(LATCH_RELEASES);
        assertTrue(String.valueOf(nLatches), nLatches < 10);

        /* Close all. */
        db.close();
        close(env);
    }
}
