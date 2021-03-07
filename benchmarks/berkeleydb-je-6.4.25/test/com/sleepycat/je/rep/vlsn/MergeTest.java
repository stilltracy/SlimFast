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

package com.sleepycat.je.rep.vlsn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.log.LogEntryHeader;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.log.LogItem;
import com.sleepycat.je.log.Provisional;
import com.sleepycat.je.log.ReplicationContext;
import com.sleepycat.je.log.entry.LogEntry;
import com.sleepycat.je.log.entry.SingleItemEntry;
import com.sleepycat.je.recovery.RecoveryInfo;
import com.sleepycat.je.rep.impl.node.NameIdPair;
import com.sleepycat.je.rep.vlsn.VLSNIndex.ForwardVLSNScanner;
import com.sleepycat.je.txn.RollbackStart;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.je.utilint.VLSN;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class MergeTest extends TestBase {

    private final String testMapDb = "TEST_MAP_DB";
    private final boolean verbose = Boolean.getBoolean("verbose");
    private final File envHome;
    private final byte lnType = 
        LogEntryType.LOG_INS_LN_TRANSACTIONAL.getTypeNum();

    private Environment env;
    private EnvironmentImpl envImpl;
    private int bucketStride = 4;
    private int bucketMaxMappings = 3;
    private int recoveryStride = 3;
    private int recoveryMaxMappings = 4;


    public MergeTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    private Environment makeEnvironment() {

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);
        return new Environment(envHome, envConfig);
    }

    /**
     * Test the tricky business of recovering the VLSNIndex. See VLSNIndex(),
     * and how the vlsnIndex is initialized from what's persistent on disk, and
     * another tracker is filled with vlsn->lsn mappings gleaned from reading
     * the log during recovery. The recovery tracker's contents are used to
     * override what is on the on-disk tracker.
     */
    @Test
    public void testMerge()
        throws Throwable {
        env = makeEnvironment();
        envImpl = DbInternal.getEnvironmentImpl(env);

        /* 
         * VLSN ranges for the test:
         * start->initialSize are mapped before recovery
         * secondStart ->recoverySize are mapped in the recovery tracker.
         */
        long start = 1;
        long initialSize = 40;
        long secondStart = start + initialSize - 20;
        long recoverySize = 30;

        RecoveryTrackerGenerator generator = 
            new NoRollbackGenerator(secondStart, recoverySize);
        try {
            doMerge(generator, initialSize);
        } finally {
            env.close();
        }
    }

    @Test
    public void testSingleRBMerge()
        throws Throwable {
        env = makeEnvironment();
        envImpl = DbInternal.getEnvironmentImpl(env);

        long initialSize = 40;

        RecoveryTrackerGenerator generator = new RollbackGenerator
            (new RollbackInfo(new VLSN(20), DbLsn.makeLsn(5, 20 * 10)));

        try {
            doMerge(generator, initialSize);
        } finally {
            env.close();
        }
    }

    @Test
    public void testMultiRBMerge()
        throws Throwable {
        env = makeEnvironment();
        envImpl = DbInternal.getEnvironmentImpl(env);

        long initialSize = 50;

        RecoveryTrackerGenerator generator = new RollbackGenerator
            (new RollbackInfo(new VLSN(30), DbLsn.makeLsn(5, 30 * 10)),
             new TestInfo(31, DbLsn.makeLsn(6, (31 * 30))),
             new TestInfo(32, DbLsn.makeLsn(6, (32 * 30))),
             new TestInfo(33, DbLsn.makeLsn(6, (33 * 30))),
             new TestInfo(34, DbLsn.makeLsn(6, (34 * 30))),
             new TestInfo(35, DbLsn.makeLsn(6, (35 * 30))),
             new TestInfo(36, DbLsn.makeLsn(6, (36 * 30))),
             new TestInfo(37, DbLsn.makeLsn(6, (37 * 30))),
             new RollbackInfo(new VLSN(33), DbLsn.makeLsn(6, (33 * 30))),
             new TestInfo(34, DbLsn.makeLsn(7, (34 * 40))),
             new TestInfo(35, DbLsn.makeLsn(7, (35 * 40))),
             new TestInfo(36, DbLsn.makeLsn(7, (36 * 40))),
             new TestInfo(37, DbLsn.makeLsn(7, (37 * 40))));

        try {
            doMerge(generator, initialSize);
        } finally {
            env.close();
        }
    }

    @Test
    public void testRBInRecoveryLogMerge()
        throws Throwable {
        env = makeEnvironment();
        envImpl = DbInternal.getEnvironmentImpl(env);

        long initialSize = 50;

        RecoveryTrackerGenerator generator = new RollbackGenerator
            (new TestInfo(51, DbLsn.makeLsn(6, (51 * 30))),
             new TestInfo(52, DbLsn.makeLsn(6, (52 * 30))),
             new TestInfo(53, DbLsn.makeLsn(6, (53 * 30))),
             new TestInfo(54, DbLsn.makeLsn(6, (54 * 30))),
             new TestInfo(55, DbLsn.makeLsn(6, (55 * 30))),
             new TestInfo(56, DbLsn.makeLsn(6, (56 * 30))),
             new TestInfo(57, DbLsn.makeLsn(6, (57 * 30))),
             new TestInfo(58, DbLsn.makeLsn(6, (58 * 30))),
             new TestInfo(59, DbLsn.makeLsn(6, (59 * 30))),
             new TestInfo(60, DbLsn.makeLsn(6, (60 * 30))),
             new TestInfo(61, DbLsn.makeLsn(6, (61 * 30))),
             new RollbackInfo(new VLSN(55), DbLsn.makeLsn(6, (55 * 30))),
             new TestInfo(56, DbLsn.makeLsn(7, (56 * 40))),
             new TestInfo(57, DbLsn.makeLsn(7, (57 * 40))));

        try {
            doMerge(generator, initialSize);
        } finally {
            env.close();
        }
    }

    private void doMerge(RecoveryTrackerGenerator generator,
                         long initialSize) 
        throws Throwable { 
        
        for (int flushPoint = 1; flushPoint <= initialSize; flushPoint++) {
            if (verbose) {
                System.out.println("flush=" + flushPoint +
                                   " initSize = " + initialSize);
            }

            VLSNIndex vlsnIndex = new VLSNIndex(envImpl, testMapDb,
                                                new NameIdPair("node1", 1),
                                                bucketStride, bucketMaxMappings,
                                                10000, new RecoveryInfo());
            try {

                List<TestInfo> expected = new ArrayList<TestInfo>();

                populate(flushPoint, vlsnIndex, initialSize, expected);
                vlsnIndex.merge(generator.makeRecoveryTracker(expected));

                assertTrue(vlsnIndex.verify(verbose));
                checkMerge(vlsnIndex, expected);

            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            } finally {
                vlsnIndex.close();
                env.removeDatabase(null, testMapDb);
            }
        }
    }

    /**
     * Fill up an initial VLSNIndex, flushing at different spots to create a 
     * different tracker/on-disk mix.
     */
    private void populate(int flushPoint,
                          VLSNIndex vlsnIndex,
                          long initialSize,
                          List<TestInfo> expected) {


        for (long i = 1; i <= initialSize; i++) {
            TestInfo info = new TestInfo(i, DbLsn.makeLsn(5, i * 10));

            /* populate vlsn index */
            vlsnIndex.put(makeLogItem(info));

            /* populate expected list */
            expected.add(info);

            if (i == flushPoint) {
                vlsnIndex.flushToDatabase(Durability.COMMIT_NO_SYNC);
            }
        }
    }

    private LogItem makeLogItem(TestInfo info) {
        LogItem item = new LogItem();
        item.header = info.header;
        item.lsn = info.lsn;
        return item;
    }

    private void checkMerge(VLSNIndex vlsnIndex, List<TestInfo> expected) {

        /* The new tracker should have the right range. */
        VLSNRange range = vlsnIndex.getRange();
        assertEquals(new VLSN(1), range.getFirst());
        VLSN lastVLSN = expected.get(expected.size() - 1).vlsn;
        assertEquals(lastVLSN, range.getLast());

        // TODO: test that the sync and commit fields in the tracker are
        // correct.

        ForwardVLSNScanner scanner = new ForwardVLSNScanner(vlsnIndex);
        long firstLsn = scanner.getStartingLsn(expected.get(0).vlsn);
        assertEquals(DbLsn.getNoFormatString(expected.get(0).lsn) +
                     " saw first VLSN " + DbLsn.getNoFormatString(firstLsn), 
                     expected.get(0).lsn, firstLsn);         

        boolean vlsnForLastInRange = false;

        int validMappings = 0;
        for (TestInfo info : expected) {
            long lsn = scanner.getPreciseLsn(info.vlsn);
            if (lsn != DbLsn.NULL_LSN) {
                if (verbose) {
                    System.out.println(info);
                }

                assertEquals(DbLsn.getNoFormatString(info.lsn), info.lsn, lsn);
                validMappings++;

                if (info.vlsn.equals(lastVLSN)) {
                    vlsnForLastInRange = true;
                }
            }
        }

        /* Should see a lsn value for the last VLSN in the range. */
        assertTrue(vlsnForLastInRange);

        /* Some portion of the expected set should be mapped. */
        assertTrue(validMappings > (expected.size()/bucketStride) - 1);
    }

    interface RecoveryTrackerGenerator {
        public VLSNRecoveryTracker makeRecoveryTracker(List<TestInfo> expected);
    }

    private class NoRollbackGenerator implements RecoveryTrackerGenerator {

        private long secondStart;
        private long recoverySize;
        
        NoRollbackGenerator(long secondStart,
                            long recoverySize) {
            this.secondStart = secondStart;
            this.recoverySize = recoverySize;
        }

        public VLSNRecoveryTracker makeRecoveryTracker
            (List<TestInfo> expected) {

            VLSNRecoveryTracker recoveryTracker = 
                new VLSNRecoveryTracker(envImpl,
                                        recoveryStride,
                                        recoveryMaxMappings,
                                        100000);

            /* Truncate the expected mappings list. */
            Iterator<TestInfo> iter = expected.iterator();
            while (iter.hasNext()) {
                TestInfo ti = iter.next();
                if (ti.vlsn.getSequence() >= secondStart) {
                    iter.remove();
                }
            }
            
            for (long i = secondStart; i < secondStart + recoverySize; i ++) {
                TestInfo info = new TestInfo(i, DbLsn.makeLsn(6,i * 20));
                recoveryTracker.trackMapping(info.lsn, 
                                             info.header,
                                             info.entry);
                expected.add(info);
            }

            return recoveryTracker;
        }
    }

    private class RollbackGenerator implements RecoveryTrackerGenerator {
        private final Object[] recoveryLog;
        
        RollbackGenerator(Object ... recoveryLog) {
            this.recoveryLog = recoveryLog;
        }

        public VLSNRecoveryTracker 
            makeRecoveryTracker(List<TestInfo> expected) {

            VLSNRecoveryTracker recoveryTracker = 
                new VLSNRecoveryTracker(envImpl,
                                        recoveryStride,
                                        recoveryMaxMappings,
                                        100000);
            
            for (Object info : recoveryLog) {
                if (info instanceof TestInfo) {
                    TestInfo t = (TestInfo) info;
                    recoveryTracker.trackMapping(t.lsn, t.header, t.entry);
                    expected.add(t);
                } else if (info instanceof RollbackInfo) {
                    RollbackInfo r = (RollbackInfo) info;

                    /* Register the pseudo rollback with the tracker. */
                    recoveryTracker.trackMapping(0 /* lsn */,
                                                 r.header, r.rollbackEntry);

                    /* Truncate the expected mappings list. */
                    Iterator<TestInfo> iter = expected.iterator();
                    while (iter.hasNext()) {
                        TestInfo ti = iter.next();
                        if (ti.vlsn.compareTo(r.matchpointVLSN) > 0) {
                            iter.remove();
                        }
                    }
                }
            }

            return recoveryTracker;
        }
    }

    private class TestInfo {
        final long lsn;
        final VLSN vlsn;
        final LogEntryHeader header;
        final LogEntry entry;

        TestInfo(long vlsnVal, long lsn, LogEntry entry) {
            this.lsn = lsn;
            this.vlsn = new VLSN(vlsnVal);
            this.header = new LogEntryHeader(entry.getLogType().getTypeNum(),
                                             0, 0, vlsn);
            this.entry = entry;
        }

        TestInfo(long vlsnVal, long lsn) {
            this.lsn = lsn;
            this.vlsn = new VLSN(vlsnVal);
            this.header = new LogEntryHeader(lnType, 0, 0, vlsn);
            this.entry = null;
        }

        @Override 
        public String toString() {
            return "vlsn=" + vlsn + " lsn=" + DbLsn.getNoFormatString(lsn) +
                " entryType="  + header.getType();
        }
    }

    private class RollbackInfo {
        final VLSN matchpointVLSN;
        final LogEntryHeader header;
        final LogEntry rollbackEntry;

        RollbackInfo(VLSN matchpointVLSN, long matchpointLsn) {

            this.matchpointVLSN = matchpointVLSN;
            Set<Long> noActiveTxns = Collections.emptySet();
            rollbackEntry = 
                SingleItemEntry.create(LogEntryType.LOG_ROLLBACK_START,
                                       new RollbackStart(matchpointVLSN,
                                                         matchpointLsn,
                                                         noActiveTxns));

             header = new LogEntryHeader(rollbackEntry, Provisional.NO,
                                         ReplicationContext.NO_REPLICATE);
        }        
    }
}
