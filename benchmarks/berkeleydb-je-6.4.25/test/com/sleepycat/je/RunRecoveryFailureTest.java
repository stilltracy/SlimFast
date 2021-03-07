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

package com.sleepycat.je;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.log.FileManager;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * @excludeDualMode
 * This test does not run in Replication Dual Mode. There are several
 * logistical issues.
 *
 * -It assumes that all log files are in the <envHome> directory, whereas
 * dual mode environments are in <envHome>/rep*
 * -It attempts to set the log file size to 1024, which is overridden by the
 * dual mode framework.
 *
 * Since the test doesn't add any unique coverage to dual mode testing, it's
 * not worth overcoming the logistical issues.
 */
public class RunRecoveryFailureTest extends TestBase {

    private Environment env;
    private final File envHome;

    public RunRecoveryFailureTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        openEnv();
    }

    @After
    public void tearDown() {

        /*
         * Close down environments in case the unit test failed so that the log
         * files can be removed.
         */
        try {
            if (env != null) {
                env.close();
                env = null;
            }
        } catch (RunRecoveryException e) {
            /* ok, the test hosed it. */
            return;
        } catch (DatabaseException e) {
            /* ok, the test closed it */
        }
    }

    private void openEnv()
        throws DatabaseException {

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setTransactional(true);

        /*
         * Run with tiny log buffers, so we can go to disk more (and see the
         * checksum errors)
         */
        DbInternal.disableParameterValidation(envConfig);
        envConfig.setConfigParam
            (EnvironmentParams.NUM_LOG_BUFFERS.getName(), "2");
        envConfig.setConfigParam
            (EnvironmentParams.LOG_MEM_SIZE.getName(),
             EnvironmentParams.LOG_MEM_SIZE_MIN_STRING);
        envConfig.setConfigParam
            (EnvironmentParams.LOG_FILE_MAX.getName(), "1024");
        envConfig.setAllowCreate(true);
        env = new Environment(envHome, envConfig);
    }

    /*
     * Corrupt an environment while open, make sure we get a
     * RunRecoveryException.
     */
    @Test
    public void testInvalidateEnvMidStream()
        throws Throwable {

        try {
            /* Make a new db in this env and flush the file. */
            Transaction txn =
                env.beginTransaction(null, TransactionConfig.DEFAULT);
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(true);
            dbConfig.setAllowCreate(true);
            Database db = env.openDatabase(txn, "foo", dbConfig);
            DatabaseEntry key = new DatabaseEntry(new byte[1000]);
            DatabaseEntry data = new DatabaseEntry(new byte[1000]);
            for (int i = 0; i < 100; i += 1) {
                db.put(txn, key, data);
            }

            env.getEnvironmentImpl().getLogManager().flush();
            env.getEnvironmentImpl().getFileManager().clear();

            /*
             * Corrupt each log file, then abort the txn. Aborting the txn
             * results in an undo of each insert, which will provoke JE into
             * reading the log a lot, and noticing the file corruption.  Should
             * get a checksum error, which should invalidate the environment.
             */
            long currentFile = DbInternal.getEnvironmentImpl(env)
                                         .getFileManager()
                                         .getCurrentFileNum();
            for (int fileNum = 0; fileNum <= currentFile; fileNum += 1) {
                String logFileName =
                    FileManager.getFileName(fileNum, FileManager.JE_SUFFIX);
                File file = new File(envHome, logFileName);
                RandomAccessFile starterFile =
                    new RandomAccessFile(file, "rw");
                FileChannel channel = starterFile.getChannel();
                long fileSize = channel.size();
                if (fileSize > FileManager.firstLogEntryOffset()) {
                    ByteBuffer junkBuffer = ByteBuffer.allocate
                        ((int) fileSize - FileManager.firstLogEntryOffset());
                    int written = channel.write
                        (junkBuffer, FileManager.firstLogEntryOffset());
                    assertTrue(written > 0);
                    starterFile.close();
                }
            }

            try {
                txn.abort();
                fail("Should see a run recovery exception");
            } catch (RunRecoveryException e) {
            }

            try {
                env.getDatabaseNames();
                fail("Should see a run recovery exception again");
            } catch (RunRecoveryException e) {
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }
}
