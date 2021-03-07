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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Test;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.utilint.StringUtils;

public class IOExceptionTest extends TestBase {

    private Environment env;
    private Database db;
    private final File envHome;

    public IOExceptionTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @After
    public void tearDown()
        throws DatabaseException {

        if (db != null) {
            db.close();
        }

        if (env != null) {
            env.close();
        }
    }

    @Test
    public void testLogBufferOverflowAbortNoDupes() {
        doLogBufferOverflowTest(false, false);
    }

    @Test
    public void testLogBufferOverflowCommitNoDupes() {
        doLogBufferOverflowTest(true, false);
    }

    @Test
    public void testLogBufferOverflowAbortDupes() {
        doLogBufferOverflowTest(false, true);
    }

    @Test
    public void testLogBufferOverflowCommitDupes() {
        doLogBufferOverflowTest(true, true);
    }

    private void doLogBufferOverflowTest(boolean abort, boolean dupes) {
        try {
            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            envConfig.setTransactional(true);
            envConfig.setAllowCreate(true);
            envConfig.setCacheSize(100000);
            env = new Environment(envHome, envConfig);

            String databaseName = "ioexceptiondb";
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            dbConfig.setSortedDuplicates(true);
            dbConfig.setTransactional(true);
            db = env.openDatabase(null, databaseName, dbConfig);

            Transaction txn = env.beginTransaction(null, null);
            DatabaseEntry oneKey =
                (dupes ?
                 new DatabaseEntry(StringUtils.toUTF8("2")) :
                 new DatabaseEntry(StringUtils.toUTF8("1")));
            DatabaseEntry oneData =
                new DatabaseEntry(new byte[10]);
            DatabaseEntry twoKey =
                new DatabaseEntry(StringUtils.toUTF8("2"));
            DatabaseEntry twoData =
                new DatabaseEntry(new byte[100000]);
            if (dupes) {
                DatabaseEntry temp = oneKey;
                oneKey = oneData;
                oneData = temp;
                temp = twoKey;
                twoKey = twoData;
                twoData = temp;
            }

            try {
                assertTrue(db.put(txn, oneKey, oneData) ==
                           OperationStatus.SUCCESS);
                db.put(txn, twoKey, twoData);
            } catch (DatabaseException DE) {
                fail("unexpected DatabaseException");
            }

            /* Read back the data and make sure it all looks ok. */
            try {
                assertTrue(db.get(txn, oneKey, oneData, null) ==
                           OperationStatus.SUCCESS);
                assertTrue(oneData.getData().length == (dupes ? 1 : 10));
            } catch (DatabaseException DE) {
                fail("unexpected DatabaseException");
            }

            try {
                assertTrue(db.get(txn, twoKey, twoData, null) ==
                           OperationStatus.SUCCESS);
            } catch (DatabaseException DE) {
                fail("unexpected DatabaseException");
            }

            try {
                if (abort) {
                    txn.abort();
                } else {
                    txn.commit();
                }
            } catch (DatabaseException DE) {
                fail("unexpected DatabaseException");
            }

            /* Read back the data and make sure it all looks ok. */
            try {
                assertTrue(db.get(null, oneKey, oneData, null) ==
                           (abort ?
                            OperationStatus.NOTFOUND :
                            OperationStatus.SUCCESS));
                assertTrue(oneData.getData().length == (dupes ? 1 : 10));
            } catch (DatabaseException DE) {
                fail("unexpected DatabaseException");
            }

            try {
                assertTrue(db.get(null, twoKey, twoData, null) ==
                           (abort ?
                            OperationStatus.NOTFOUND :
                            OperationStatus.SUCCESS));
            } catch (DatabaseException DE) {
                fail("unexpected DatabaseException");
            }

        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    @Test
    public void testIOExceptionDuringFileFlippingWrite() {
        doIOExceptionDuringFileFlippingWrite(8, 33, 2);
    }

    private void doIOExceptionDuringFileFlippingWrite(int numIterations,
                                                      int exceptionStartWrite,
                                                      int exceptionWriteCount) {
        try {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            DbInternal.disableParameterValidation(envConfig);
            envConfig.setTransactional(true);
            envConfig.setAllowCreate(true);
            envConfig.setConfigParam("je.log.fileMax", "1000");
            envConfig.setConfigParam("je.log.bufferSize", "1025");
            envConfig.setConfigParam("je.env.runCheckpointer", "false");
            envConfig.setConfigParam("je.env.runCleaner", "false");
            env = new Environment(envHome, envConfig);

            EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(true);
            dbConfig.setAllowCreate(true);
            db = env.openDatabase(null, "foo", dbConfig);

            /*
             * Put one record into the database so it gets populated w/INs and
             * LNs, and we can fake out the RMW commits used below.
             */
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            IntegerBinding.intToEntry(5, key);
            IntegerBinding.intToEntry(5, data);
            db.put(null, key, data);

            /*
             * Now generate trace and commit log entries. The trace records
             * aren't forced out, but the commit records are forced.
             */
            FileManager.WRITE_COUNT = 0;
            FileManager.THROW_ON_WRITE = true;
            FileManager.STOP_ON_WRITE_COUNT = exceptionStartWrite;
            FileManager.N_BAD_WRITES = exceptionWriteCount;
            for (int i = 0; i < numIterations; i++) {

                try {
                    /* Generate a non-forced record. */
                    if (i == (numIterations - 1)) {

                        /*
                         * On the last iteration, write a record that is large
                         * enough to force a file flip (i.e. an fsync which
                         * succeeds) followed by the large write (which doesn't
                         * succeed due to an IOException).  In [#15754] the
                         * large write fails on Out Of Disk Space, rolling back
                         * the savedLSN to the previous file, even though the
                         * file has flipped.  The subsequent write ends up in
                         * the flipped file, but at the offset of the older
                         * file (leaving a hole in the new flipped file).
                         */
                        Trace.trace(envImpl,
                                    i + "/" + FileManager.WRITE_COUNT +
                                    " " + new String(new byte[2000]));
                    } else {
                        Trace.trace(envImpl,
                                        i + "/" + FileManager.WRITE_COUNT +
                                        " " + "xx");
                    }
                } catch (IllegalStateException ISE) {
                    /* Eat exception thrown by TraceLogHandler. */
                }

                /*
                 * Generate a forced record by calling commit. Since RMW
                 * transactions that didn't actually do a write won't log a
                 * commit record, do an addLogInfo to trick the txn into
                 * logging a commit.
                 */
                Transaction txn = env.beginTransaction(null, null);
                db.get(txn, key, data, LockMode.RMW);
                DbInternal.getTxn(txn).addLogInfo(DbLsn.makeLsn(3, 3));
                txn.commit();
            }
            db.close();

            /*
             * Verify that the log files are ok and have no checksum errors.
             */
            FileReader reader =
                new FileReader(DbInternal.getEnvironmentImpl(env),
                               4096, true, 0, null, DbLsn.NULL_LSN,
                               DbLsn.NULL_LSN) {
                    @Override
            protected boolean processEntry(ByteBuffer entryBuffer) {
                        entryBuffer.position(entryBuffer.position() +
                                             currentEntryHeader.getItemSize());
                        return true;
                    }
                };

            DbInternal.getEnvironmentImpl(env).getLogManager().flush();

            while (reader.readNextEntry()) {
            }

            /* Make sure the reader really did scan the files. */
            assert (DbLsn.getFileNumber(reader.getLastLsn()) == 3) :
                DbLsn.toString(reader.getLastLsn());

            env.close();
            env = null;
            db = null;
        } catch (Throwable T) {
            T.printStackTrace();
        } finally {
            FileManager.STOP_ON_WRITE_COUNT = Long.MAX_VALUE;
            FileManager.N_BAD_WRITES = Long.MAX_VALUE;
        }
    }
}
