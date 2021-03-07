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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.EnvironmentFailureReason;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.junit.JUnitThread;
import com.sleepycat.je.log.entry.LogEntry;
import com.sleepycat.je.log.entry.TraceLogEntry;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.utilint.StringUtils;

/**
 * Test basic log management.
 */
public class LogManagerTest extends TestBase {

    static private final boolean DEBUG = false;

    private FileManager fileManager;
    private LogManager logManager;
    private final File envHome;
    private Environment env;

    public LogManagerTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    /**
     * Log and retrieve objects, with log in memory
     */
    @Test
    public void testBasicInMemory()
        throws IOException, ChecksumException, DatabaseException {

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        DbInternal.disableParameterValidation(envConfig);
        envConfig.setConfigParam(EnvironmentParams.NODE_MAX.getName(), "6");
        envConfig.setConfigParam
            (EnvironmentParams.LOG_FILE_MAX.getName(), "1000");
        turnOffDaemons(envConfig);
        envConfig.setAllowCreate(true);
        env = new Environment(envHome, envConfig);
        EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
        fileManager = envImpl.getFileManager();
        logManager = envImpl.getLogManager();
        logAndRetrieve(envImpl);
        env.close();
    }

    @Test
    public void testFlushItemLargerThanBufferSize()
        throws Throwable {

        JUnitThread junitThread = null;
        try {
            /* Open env with small buffer size. */
            final int bufSize = 1024;
            final String bufSizeStr = String.valueOf(bufSize);
            final EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_CLEANER,
                                     "false");
            envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_EVICTOR,
                                     "false");
            envConfig.setConfigParam(EnvironmentConfig.ENV_RUN_CHECKPOINTER,
                                     "false");
            envConfig.setConfigParam(EnvironmentConfig.LOG_BUFFER_SIZE,
                                     bufSizeStr);
            envConfig.setAllowCreate(true);
            env = new Environment(envHome, envConfig);
            final EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
            fileManager = envImpl.getFileManager();
            logManager = envImpl.getLogManager();
            assertEquals(bufSizeStr,
                         env.getConfig().getConfigParam
                         (EnvironmentConfig.LOG_BUFFER_SIZE));

            /* Write a Trace to initialize FileManager.endOfLogRWFile. */
            final Trace smTrace = new Trace("small");
            Trace.trace(envImpl, smTrace);
            logManager.flushWriteNoSync();

            /* Make a Trace entry that is larger than buffer size. */
            final StringBuilder bigString = new StringBuilder(bufSize * 2);
            while (bigString.length() < bufSize) {
                bigString.append
                    ("12345679890123456798901234567989012345679890");
            }
            final Trace trace = new Trace(bigString.toString());

            /* Use a separate thread to simulate an fsync. */
            final CountDownLatch step1 = new CountDownLatch(1);
            junitThread = new JUnitThread
                ("LogManagerTest.testFlushItemLargerThanBufferSize") {
                @Override
                public void testBody()
                    throws Throwable {

                    fileManager.testWriteQueueLock();
                    step1.countDown();

                    /*
                     * Sleep long enough to let logForceFlush() get to the
                     * point that it checks the fsync lock.  But we have to
                     * release the lock or it will deadlock.
                     */
                    Thread.sleep(1000);
                    fileManager.testWriteQueueUnlock();
                }
            };

            /*
             * Flush log buffer and write queue, then log the big trace entry.
             * Before the bug fix [#20717], the write queue was not flushed
             * when the trace was logged.
             */
            logManager.flushWriteNoSync();
            assertFalse(fileManager.hasQueuedWrites());
            junitThread.start();
            step1.await();
            logManager.logForceFlush(new TraceLogEntry(trace),
                                     false /*fsyncRequired*/,
                                     ReplicationContext.NO_REPLICATE);
            assertFalse(fileManager.hasQueuedWrites());
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            if (junitThread != null) {
                junitThread.shutdown();
                junitThread = null;
            }
            env.close();
        }
    }

    /**
     * Log and retrieve objects, with log completely flushed to disk
     */
    @Test
    public void testBasicOnDisk()
        throws Throwable {

        try {

            /*
             * Force the buffers and files to be small. The log buffer is
             * actually too small, will have to grow dynamically. Each file
             * only holds one test item (each test item is 50 bytes).
             */
            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            DbInternal.disableParameterValidation(envConfig);
            envConfig.setConfigParam(
                            EnvironmentParams.LOG_MEM_SIZE.getName(),
                            EnvironmentParams.LOG_MEM_SIZE_MIN_STRING);
            envConfig.setConfigParam(
                            EnvironmentParams.NUM_LOG_BUFFERS.getName(), "2");
            envConfig.setConfigParam(
                            EnvironmentParams.LOG_FILE_MAX.getName(), "79");
            envConfig.setConfigParam(
                            EnvironmentParams.NODE_MAX.getName(), "6");

            /* Disable noisy UtilizationProfile database creation. */
            DbInternal.setCreateUP(envConfig, false);
            /* Don't checkpoint utilization info for this test. */
            DbInternal.setCheckpointUP(envConfig, false);
            /* Don't run the cleaner without a UtilizationProfile. */
            envConfig.setConfigParam
                (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");

            /*
             * Don't run any daemons, those emit trace messages and other log
             * entries and mess up our accounting.
             */
            turnOffDaemons(envConfig);
            envConfig.setAllowCreate(true);

            /*
             * Recreate the file manager and log manager w/different configs.
             */
            env = new Environment(envHome, envConfig);
            EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
            fileManager = envImpl.getFileManager();
            logManager = envImpl.getLogManager();

            logAndRetrieve(envImpl);

            /*
             * Expect 10 je files, 7 to hold logged records, 1 to hold root, no
             * recovery messages, 2 for checkpoint records
             */
            String[] names = fileManager.listFileNames(FileManager.JE_SUFFIXES);
            assertEquals("Should be 10 files on disk", 10, names.length);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            env.close();
        }
    }

    /**
     * Log and retrieve objects, with some of log flushed to disk, some of log
     * in memory.
     */
    @Test
    public void testComboDiskMemory()
        throws Throwable {

        try {

            /*
             * Force the buffers and files to be small. The log buffer is
             * actually too small, will have to grow dynamically. Each file
             * only holds one test item (each test item is 50 bytes)
             */
            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            DbInternal.disableParameterValidation(envConfig);
            envConfig.setConfigParam
                (EnvironmentParams.LOG_MEM_SIZE.getName(),
                 EnvironmentParams.LOG_MEM_SIZE_MIN_STRING);
            envConfig.setConfigParam
                (EnvironmentParams.NUM_LOG_BUFFERS.getName(), "2");
            envConfig.setConfigParam(EnvironmentParams.LOG_FILE_MAX.getName(),
                                     "64");
            envConfig.setConfigParam(EnvironmentParams.NODE_MAX.getName(),
                                     "6");

            /* Disable noisy UtilizationProfile database creation. */
            DbInternal.setCreateUP(envConfig, false);
            /* Don't checkpoint utilization info for this test. */
            DbInternal.setCheckpointUP(envConfig, false);
            /* Don't run the cleaner without a UtilizationProfile. */
            envConfig.setConfigParam
                (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");

            /*
             * Don't run the cleaner or the checkpointer daemons, those create
             * more log entries and mess up our accounting
             */
            turnOffDaemons(envConfig);
            envConfig.setAllowCreate(true);

            env = new Environment(envHome, envConfig);
            EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
            fileManager = envImpl.getFileManager();
            logManager = envImpl.getLogManager();

            logAndRetrieve(envImpl);

            /*
             * Expect 10 JE files:
             * root
             * ckptstart
             * ckptend
             * trace trace
             * trace trace
             * trace trace
             * trace trace
             * trace trace
             * trace trace
             * trace trace
             *
             * This is based on a manual perusal of the log files and their
             * contents. Changes in the sizes of log entries can throw this
             * test off, and require that a check and a change to the assertion
             * value.
             */
            String[] names = fileManager.listFileNames(FileManager.JE_SUFFIXES);
            assertEquals("Should be 10 files on disk", 10, names.length);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            env.close();
        }
    }

    /**
     * Log and retrieve objects, with some of log flushed to disk, some
     * of log in memory. Force the read buffer to be very small
     */
    @Test
    public void testFaultingIn()
        throws Throwable {

        try {

            /*
             * Force the buffers and files to be small. The log buffer is
             * actually too small, will have to grow dynamically. We read in 32
             * byte chunks, will have to re-read only holds one test item (each
             * test item is 50 bytes)
             */
            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            DbInternal.disableParameterValidation(envConfig);
            envConfig.setConfigParam
                (EnvironmentParams.LOG_MEM_SIZE.getName(),
                 EnvironmentParams.LOG_MEM_SIZE_MIN_STRING);
            envConfig.setConfigParam
                (EnvironmentParams.NUM_LOG_BUFFERS.getName(), "2");
            envConfig.setConfigParam
                (EnvironmentParams.LOG_FILE_MAX.getName(), "200");
            envConfig.setConfigParam
                (EnvironmentParams.LOG_FAULT_READ_SIZE.getName(), "32");
            envConfig.setConfigParam
                (EnvironmentParams.NODE_MAX.getName(), "6");
            envConfig.setAllowCreate(true);
            env = new Environment(envHome, envConfig);
            EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
            fileManager = envImpl.getFileManager();
            logManager = envImpl.getLogManager();
            logAndRetrieve(envImpl);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            env.close();
        }
    }

    /**
     * Log several objects, retrieve them.
     */
    private void logAndRetrieve(EnvironmentImpl envImpl)
        throws IOException, ChecksumException, DatabaseException {

        /* Make test loggable objects. */
        List<Trace> testRecs = new ArrayList<Trace>();
        for (int i = 0; i < 10; i++) {
            testRecs.add(new Trace("Hello there, rec " + (i+1)));
        }

        /* Log three of them, remember their LSNs. */
        List<Long> testLsns = new ArrayList<Long>();

        for (int i = 0; i < 3; i++) {
            long lsn = Trace.trace(envImpl, testRecs.get(i));
            if (DEBUG) {
                System.out.println("i = " + i + " test LSN: file = " +
                                   DbLsn.getFileNumber(lsn) +
                                   " offset = " +
                                   DbLsn.getFileOffset(lsn));
            }
            testLsns.add(new Long(lsn));
        }

        /* Ask for them back, out of order. */
        assertEquals(testRecs.get(2),
                     logManager.getEntry
                     (DbLsn.longToLsn(testLsns.get(2))));
        assertEquals(testRecs.get(0),
                     logManager.getEntry
                     (DbLsn.longToLsn(testLsns.get(0))));
        assertEquals(testRecs.get(1),
                     logManager.getEntry
                     (DbLsn.longToLsn(testLsns.get(1))));

        /* Intersperse logging and getting. */
        testLsns.add
            (new Long(Trace.trace(envImpl, testRecs.get(3))));
        testLsns.add
            (new Long(Trace.trace(envImpl, testRecs.get(4))));

        assertEquals(testRecs.get(2),
                     logManager.getEntry
                     (DbLsn.longToLsn(testLsns.get(2))));
        assertEquals(testRecs.get(4),
                     logManager.getEntry
                     (DbLsn.longToLsn(testLsns.get(4))));

        /* Intersperse logging and getting. */
        testLsns.add
            (new Long(Trace.trace(envImpl, testRecs.get(5))));
        testLsns.add
            (new Long(Trace.trace(envImpl, testRecs.get(6))));
        testLsns.add
            (new Long(Trace.trace(envImpl, testRecs.get(7))));

        assertEquals(testRecs.get(7),
                     logManager.getEntry
                     (DbLsn.longToLsn(testLsns.get(7))));
        assertEquals(testRecs.get(0),
                     logManager.getEntry
                     (DbLsn.longToLsn(testLsns.get(0))));
        assertEquals(testRecs.get(6),
                     logManager.getEntry
                     (DbLsn.longToLsn(testLsns.get(6))));

        /*
         * Check that we can retrieve log entries as byte buffers, and get the
         * correct object back. Used by replication.
         */
        long lsn = testLsns.get(7).longValue();
        ByteBuffer buffer = logManager.getByteBufferFromLog(lsn);

        HeaderAndEntry contents = readHeaderAndEntry(buffer, null /*envImpl*/);

        assertEquals(testRecs.get(7),
                     contents.entry.getMainItem());
        assertEquals(LogEntryType.LOG_TRACE.getTypeNum(),
                     contents.header.getType());
        assertEquals(LogEntryType.LOG_VERSION,
                     contents.header.getVersion());
    }

    private void turnOffDaemons(EnvironmentConfig envConfig) {
        envConfig.setConfigParam(
                       EnvironmentParams.ENV_RUN_CLEANER.getName(),
                      "false");
        envConfig.setConfigParam(
                       EnvironmentParams.ENV_RUN_CHECKPOINTER.getName(),
                       "false");
        envConfig.setConfigParam(
                       EnvironmentParams.ENV_RUN_EVICTOR.getName(),
                       "false");
        envConfig.setConfigParam(
                       EnvironmentParams.ENV_RUN_INCOMPRESSOR.getName(),
                       "false");
    }

    /**
     * Log a few items, then hit exceptions. Make sure LSN state is correctly
     * maintained and that items logged after the exceptions are at the correct
     * locations on disk.
     */
    @Test
    public void testExceptions()
        throws Throwable {

        int logBufferSize = ((int) EnvironmentParams.LOG_MEM_SIZE_MIN) / 3;
        int numLogBuffers = 5;
        int logBufferMemSize = logBufferSize * numLogBuffers;
        int logFileMax = 1000;
        int okCounter = 0;

        try {
            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            DbInternal.disableParameterValidation(envConfig);
            envConfig.setConfigParam(EnvironmentParams.LOG_MEM_SIZE.getName(),
                                     new Integer(logBufferMemSize).toString());
            envConfig.setConfigParam
                (EnvironmentParams.NUM_LOG_BUFFERS.getName(),
                 new Integer(numLogBuffers).toString());
            envConfig.setConfigParam
                (EnvironmentParams.LOG_FILE_MAX.getName(),
                 new Integer(logFileMax).toString());
            envConfig.setConfigParam(
                            EnvironmentParams.NODE_MAX.getName(), "6");

            /* Disable noisy UtilizationProfile database creation. */
            DbInternal.setCreateUP(envConfig, false);
            /* Don't checkpoint utilization info for this test. */
            DbInternal.setCheckpointUP(envConfig, false);
            /* Don't run the cleaner without a UtilizationProfile. */
            envConfig.setConfigParam
                (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");

            /*
             * Don't run any daemons, those emit trace messages and other log
             * entries and mess up our accounting.
             */
            turnOffDaemons(envConfig);
            envConfig.setAllowCreate(true);
            env = new Environment(envHome, envConfig);
            EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
            fileManager = envImpl.getFileManager();
            logManager = envImpl.getLogManager();

            /* Keep track of items logged and their LSNs. */
            ArrayList<Trace> testRecs = new ArrayList<Trace>();
            ArrayList<Long> testLsns = new ArrayList<Long>();

            /*
             * Intersperse:
             * - log successfully
             * - log w/failure because the item doesn't fit in the log buffer
             * - have I/O failures writing out the log
             * Verify that all expected items can be read. Some will come
             * from the log buffer pool.
             * Then close and re-open the environment, to verify that
             * all log items are faulted from disk
             */

            /* Successful log. */
            addOkayItem(envImpl, okCounter++,
                        testRecs, testLsns, logBufferSize);

            /* Item that's too big for the log buffers. */
            attemptTooBigItem(envImpl, logBufferSize, testRecs, testLsns);

            /* Successful log. */
            addOkayItem(envImpl, okCounter++,
                        testRecs, testLsns, logBufferSize);

            /*
             * This verify read the items from the log buffers. Note before SR
             * #12638 existed (LSN state not restored properly after exception
             * because of too-small log buffer), this verify hung.
             */
            verifyOkayItems(logManager, testRecs, testLsns, true);

            /* More successful logs, along with a few too-big items. */
            for (;okCounter < 23; okCounter++) {
                addOkayItem(envImpl, okCounter, testRecs,
                            testLsns, logBufferSize);

                if ((okCounter % 4) == 0) {
                    attemptTooBigItem(envImpl, logBufferSize,
                                      testRecs, testLsns);
                }
                /*
                 * If we verify in the loop, sometimes we'll read from disk and
                 * sometimes from the log buffer pool.
                 */
                verifyOkayItems(logManager, testRecs, testLsns, true);
            }

            /*
             * Test the case where we flip files and write the old write buffer
             * out before we try getting a log buffer for the new item. We need
             * to
             *
             * - hit a log-too-small exceptin
             * - right after, we need to log an item that is small enough
             *   to fit in the log buffer but big enough to require that
             *   we flip to a new file.
             */
            long nextLsn = fileManager.getNextLsn();
            long fileOffset = DbLsn.getFileOffset(nextLsn);

            assertTrue((logFileMax - fileOffset ) < logBufferSize);
            attemptTooBigItem(envImpl, logBufferSize, testRecs, testLsns);
            addOkayItem(envImpl, okCounter++,
                        testRecs, testLsns, logBufferSize,
                        ((int)(logFileMax - fileOffset)));
            verifyOkayItems(logManager, testRecs, testLsns, true);

            /*
             * Finally, close this environment and re-open, and read all
             * expected items from disk.
             */
            env.close();
            envConfig.setAllowCreate(false);
            env = new Environment(envHome, envConfig);
            envImpl  = DbInternal.getEnvironmentImpl(env);
            fileManager = envImpl.getFileManager();
            logManager = envImpl.getLogManager();
            verifyOkayItems(logManager, testRecs, testLsns, false);

            /* Check that we read these items off disk. */
            EnvironmentStats stats = env.getStats(null);
            assertTrue(stats.getEndOfLog() > 0);
            assertTrue("nNotResident=" + stats.getNNotResident() +
                       " nRecs=" + testRecs.size(),
                       stats.getNNotResident() >= testRecs.size());

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            env.close();
        }
    }

    private void addOkayItem(EnvironmentImpl envImpl,
                             int tag,
                             List<Trace> testRecs,
                             List<Long> testLsns,
                             int logBufferSize,
                             int fillerLen)
        throws DatabaseException {

        String filler = StringUtils.fromUTF8(new byte[fillerLen]);
        Trace t = new Trace("okay" + filler + tag );
        assertTrue(logBufferSize > t.getLogSize());
        testRecs.add(t);
        long lsn = Trace.trace(envImpl, t);
        testLsns.add(new Long(lsn));
    }

    private void addOkayItem(EnvironmentImpl envImpl,
                             int tag,
                             List<Trace> testRecs,
                             List<Long> testLsns,
                             int logBufferSize)
        throws DatabaseException {

        addOkayItem(envImpl, tag, testRecs, testLsns, logBufferSize, 0);
    }

    private void attemptTooBigItem(EnvironmentImpl envImpl,
                                   int logBufferSize,
                                   Trace big,
                                   List<Trace> testRecs,
                                   List<Long> testLsns) {
        assertTrue(big.getLogSize() > logBufferSize);

        try {
            long lsn = Trace.trace(envImpl, big);
            testLsns.add(new Long(lsn));
            testRecs.add(big);
        } catch (DatabaseException expected) {
            fail("Should not have hit exception.");
        }
    }

    private void attemptTooBigItem(EnvironmentImpl envImpl,
                                   int logBufferSize,
                                   List<Trace> testRecs,
                                   List<Long> testLsns) {
        String stuff = "12345679890123456798901234567989012345679890";
        while (stuff.length() < EnvironmentParams.LOG_MEM_SIZE_MIN) {
            stuff += stuff;
        }
        Trace t = new Trace(stuff);
        attemptTooBigItem(envImpl, logBufferSize, t, testRecs, testLsns);
    }

    @SuppressWarnings("hiding")
    private void verifyOkayItems(LogManager logManager,
                                 ArrayList<Trace> testRecs,
                                 ArrayList<Long> testLsns,
                                 boolean checkOrder)
        throws IOException, DatabaseException {

        /* read forwards. */
        for (int i = 0; i < testRecs.size(); i++) {
            assertEquals(testRecs.get(i),
                         logManager.getEntry
                         (DbLsn.longToLsn(testLsns.get(i))));

        }

        /* Make sure LSNs are adjacent */
        assertEquals(testLsns.size(), testRecs.size());

        if (checkOrder) {

            /*
             * TODO: sometimes an ioexception entry will make it into the write
             * buffer, and sometimes it won't. It depends on whether the IO
             * exception was thrown when before or after the logabble item was
             * written into the buffer.  I haven't figure out yet how to tell
             * the difference, so for now, we don't check order in the portion
             * of the test that issues IO exceptions.
             */
            for (int i = 1; i < testLsns.size(); i++) {

                long lsn = testLsns.get(i).longValue();
                long lsnFile = DbLsn.getFileNumber(lsn);
                long lsnOffset = DbLsn.getFileOffset(lsn);
                long prevLsn = testLsns.get(i-1).longValue();
                long prevFile = DbLsn.getFileNumber(prevLsn);
                long prevOffset = DbLsn.getFileOffset(prevLsn);
                if (prevFile == lsnFile) {
                    assertEquals("item " + i + "prev = " +
                                 DbLsn.toString(prevLsn) +
                                 " current=" + DbLsn.toString(lsn),
                                 (testRecs.get(i-1).getLogSize() +
                                  LogEntryHeader.MIN_HEADER_SIZE),
                                 lsnOffset - prevOffset);
                } else {
                    assertEquals(prevFile+1, lsnFile);
                    assertEquals(FileManager.firstLogEntryOffset(),
                                 lsnOffset);
                }
            }
        }

        /* read backwards. */
        for (int i = testRecs.size() - 1; i > -1; i--) {
            assertEquals(testRecs.get(i),
                         logManager.getEntry
                         (DbLsn.longToLsn(testLsns.get(i))));

        }
    }

    /**
     * Convenience method for marshalling a header and log entry
     * out of a byte buffer read directly out of the log.
     * @throws DatabaseException
     */
    private static HeaderAndEntry readHeaderAndEntry(ByteBuffer bytesFromLog,
                                                     EnvironmentImpl envImpl)
        throws ChecksumException {

        HeaderAndEntry ret = new HeaderAndEntry();
        ret.header = new LogEntryHeader(bytesFromLog,
                                        LogEntryType.LOG_VERSION);
        ret.header.readVariablePortion(bytesFromLog);

        ret.entry =
            LogEntryType.findType(ret.header.getType()).getNewLogEntry();

        ret.entry.readEntry(envImpl, ret.header, bytesFromLog);
        return ret;
    }

    private static class HeaderAndEntry {
        public LogEntryHeader header;
        public LogEntry entry;

        /* Get an HeaderAndEntry from readHeaderAndEntry */
        private HeaderAndEntry() {
        }

        public boolean logicalEquals(HeaderAndEntry other) {
            return (header.logicalEqualsIgnoreVersion(other.header) &&
                    entry.logicalEquals(other.entry));
        }

        @Override
        public String toString() {
            return header.toString() + ' ' + entry;
        }
    }

    /**
     * Ensure that a ChecksumException is thrown when any portion of a log
     * entry is corrupted, e.g., by a disk failure.  This tests ensures that
     * no other exception is thrown when validating the entry, e.g., via an
     * assertion.
     *
     * There is one exception of a corrupted entry that is intentionally
     * allowed:  If the corruption does nothing more than toggle the invisible
     * bit, then we do not consider this a checksum error.  This is extremely
     * unlikely to occur, and is tolerated to allow toggling the invisible bit
     * in the log entry with a single atomic write of a single byte.
     */
    @Test
    public void testEntryChecksum() {

        /* Open env. */
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        turnOffDaemons(envConfig);
        envConfig.setAllowCreate(true);
        env = new Environment(envHome, envConfig);
        EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(env);
        logManager = envImpl.getLogManager();

        /* Check file header, then next entry. */
        int len = doChecksumTest(envImpl, DbLsn.makeLsn(0, 0));
        doChecksumTest(envImpl, DbLsn.makeLsn(0, len));

        env.close();
    }

    private int doChecksumTest(final EnvironmentImpl envImpl, final long lsn) {

        final ByteBuffer byteBuf = logManager.getByteBufferFromLog(lsn);
        final int byteLen = byteBuf.capacity();

        /* Expect no exception with unmodified buffer. */
        try {
            verifyChecksum(envImpl, lsn, byteBuf);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }

        /* Expect failure. */
        for (int i = 0; i < byteLen; i += 1) {
            int oldVal = byteBuf.get(i) & 0xFF;
            int newVal;
            /* Replace byte with 0. */
            newVal = (oldVal == 0) ? 1 : 0;
            expectChecksumFailure(envImpl, lsn, byteBuf, i, oldVal, newVal);
            /* Replace byte with 0xFF. */
            newVal = (oldVal == 0xFF) ? 0xF7 : 0xFF;
            expectChecksumFailure(envImpl, lsn, byteBuf, i, oldVal, newVal);
            /* Increment byte value. */
            newVal = oldVal + 1;
            expectChecksumFailure(envImpl, lsn, byteBuf, i, oldVal, newVal);
            /* Decrement byte value. */
            newVal = oldVal - 1;
            expectChecksumFailure(envImpl, lsn, byteBuf, i, oldVal, newVal);
            /* Set individual bits. */
            for (int j = 0; j < 8; j += 1) {
                int flag = 1 << j;
                if ((oldVal & flag) == 0) {
                    newVal = oldVal | flag;
                    expectChecksumFailure(envImpl, lsn, byteBuf, i, oldVal,
                                          newVal);
                }
            }
            /* Clear individual bits. */
            for (int j = 0; j < 8; j += 1) {
                int flag = 1 << j;
                if ((oldVal & flag) != 0) {
                    newVal = oldVal & ~flag;
                    expectChecksumFailure(envImpl, lsn, byteBuf, i, oldVal,
                                          newVal);
                }
            }
        }

        return byteBuf.remaining();
    }

    private void expectChecksumFailure(final EnvironmentImpl envImpl,
                                       final long lsn,
                                       final ByteBuffer byteBuf,
                                       final int bufIndex,
                                       final int oldValue,
                                       final int newValue) {
        /* Replace buffer value. */
        byteBuf.put(bufIndex, (byte) newValue);

        /* Expect checksum exception. */
        try {
            verifyChecksum(envImpl, lsn, byteBuf);
            fail("Expected ChecksumException");
        } catch (ChecksumException e) {
            /* Expected. */
        } catch (EnvironmentFailureException e) {
            if (bufIndex == LogEntryHeader.FLAGS_OFFSET &&
                newValue == LogEntryHeader.makeInvisible((byte) oldValue)) {
                /* Expected when only the invisible bit is toggled. */
                assertEquals(EnvironmentFailureReason.LOG_INTEGRITY,
                             e.getReason());
            } else {
                /* Not expected. */
                throw e;
            }
        }

        /* Restore buffer value. */
        byteBuf.put(bufIndex, (byte) oldValue);
    }

    private void verifyChecksum(final EnvironmentImpl envImpl,
                                final long lsn,
                                final ByteBuffer byteBuf)
        throws ChecksumException {

        LogBuffer logBuf = new LogBuffer(byteBuf.capacity(), envImpl);
        logBuf.getDataBuffer().put(byteBuf);
        byteBuf.rewind();
        logBuf.getDataBuffer().rewind();
        logBuf.registerLsn(lsn);
        logBuf.latchForWrite();
        logManager.getLogEntryFromLogSource
            (lsn, logBuf, false /*invisibleReadAllowed*/);
    }
}
