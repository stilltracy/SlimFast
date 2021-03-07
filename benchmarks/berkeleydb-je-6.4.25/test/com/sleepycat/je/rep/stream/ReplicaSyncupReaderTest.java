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

package com.sleepycat.je.rep.stream;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.log.LogManager;
import com.sleepycat.je.log.ReplicationContext;
import com.sleepycat.je.log.entry.CommitLogEntry;
import com.sleepycat.je.log.entry.SingleItemEntry;
import com.sleepycat.je.recovery.CheckpointEnd;
import com.sleepycat.je.rep.RepInternal;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.impl.node.NameIdPair;
import com.sleepycat.je.rep.stream.MatchpointSearchResults.PassedTxnInfo;
import com.sleepycat.je.rep.stream.VLSNTestUtils.CheckReader;
import com.sleepycat.je.rep.stream.VLSNTestUtils.CheckWireRecord;
import com.sleepycat.je.rep.stream.VLSNTestUtils.LogPopulator;
import com.sleepycat.je.txn.TxnCommit;
import com.sleepycat.je.utilint.Timestamp;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Ensure that ReplicaSyncupReader tracks checkpoints and commits properly, so
 * that rollback conditions are obeyed.
 */

public class ReplicaSyncupReaderTest extends TestBase {

    private File envHome;

    public ReplicaSyncupReaderTest() {
        envHome = SharedTestUtils.getTestDir();
    }
 
    @Test
    public void testRepAndNonRepCommits()
        throws DatabaseException, InterruptedException {
        runTest(new RepAndNonRepCommits());
    }

    @Test
    public void testMultipleCkpts()
        throws DatabaseException, InterruptedException {
        runTest(new MultipleCkpts());
    }

    private void runTest(CommitsAndCkpts populator)
        throws DatabaseException, InterruptedException {

        ReplicatedEnvironment rep =
            VLSNTestUtils.setupLog(envHome,
                                   5,
                                   3,
                                   populator);

        EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(rep);
        List<CheckWireRecord> expected =
            VLSNTestUtils.collectExpectedData(new CheckReader(envImpl));
        long lastLsn =  envImpl.getFileManager().getLastUsedLsn();

        try {

            MatchpointSearchResults searchResults =
                new MatchpointSearchResults(envImpl);
            int lastIndex = expected.size() - 1;

            ReplicaSyncupReader replicaSyncupReader =
                new ReplicaSyncupReader
                (envImpl,
                 RepInternal.getRepImpl(rep).getVLSNIndex(),
                 lastLsn,
                 10000,
                 new NameIdPair("n0",0),
                 expected.get(lastIndex).getVLSN(), // startVLSN
                 populator.lsnBeforePopulate,       // finishLSN
                 searchResults);

            for (int i = lastIndex; i >=0; i-- ) {
                replicaSyncupReader.scanBackwards(expected.get(i).getVLSN());
            }

            assertEquals(populator.nExpectedCommits,
                         searchResults.getNumPassedCommits());
            assertEquals(populator.passedCheckpointEnd,
                         searchResults.getPassedCheckpointEnd());

            PassedTxnInfo earliest = searchResults.getEarliestPassedTxn();
            assertEquals(populator.earliestTxnId, earliest.id);
            assertEquals(populator.earliestPassedTime, earliest.time);
            assertEquals(populator.earliestTxnLsn, earliest.lsn);
        } finally {
            if (rep != null) {
                rep.close();
            }
        }
    }

    private abstract class CommitsAndCkpts implements LogPopulator {

        long nExpectedCommits = 1;
        boolean passedCheckpointEnd = true;
        long earliestTxnId = 20;
        Timestamp earliestPassedTime;
        long earliestTxnLsn;
        long lsnBeforePopulate;

        protected LogManager logManager;

        @Override
        public void populateLog(ReplicatedEnvironment rep) {
            EnvironmentImpl envImpl = DbInternal.getEnvironmentImpl(rep);
            logManager = envImpl.getLogManager();

            /*
             * Remember the lsn before we begin adding new entries to the log.
             * We want to limit the reading of the log by the
             * ReplicaSyncupReader in order to skip the parts of the log written
             * before this phase, so the test can be sure what to check for.
             */
            lsnBeforePopulate = envImpl.getFileManager().getNextLsn();

            writeLogEntries(rep);
        }

        protected abstract void writeLogEntries(ReplicatedEnvironment rep);
    }

    private class RepAndNonRepCommits extends CommitsAndCkpts {

        RepAndNonRepCommits() {
            nExpectedCommits = 1;
            passedCheckpointEnd = true;
            earliestTxnId = 20;
        }

        @Override
        public void writeLogEntries(ReplicatedEnvironment rep) {

            SingleItemEntry<CheckpointEnd> endEntry =
                SingleItemEntry.create(LogEntryType.LOG_CKPT_END,
                                       new CheckpointEnd
                                       ("test", 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                                        true /*cleanedFilesToDelete*/));
            logManager.log(endEntry, ReplicationContext.NO_REPLICATE);

            /*
             * Only replicated commits should be noted by the sync reader.
             */
            TxnCommit commit = new TxnCommit(10, 0, 1);

            CommitLogEntry commitEntry = new CommitLogEntry(commit);
            logManager.log(commitEntry, ReplicationContext.NO_REPLICATE);

            commit = new TxnCommit(20, 0, 1);
            commitEntry = new CommitLogEntry(commit);
            earliestPassedTime = commit.getTime();
            earliestTxnLsn =
                logManager.log(commitEntry, ReplicationContext.MASTER);
            logManager.flushNoSync();
        }
    }

    private class MultipleCkpts extends CommitsAndCkpts {

        MultipleCkpts() {
             nExpectedCommits = 2;
             passedCheckpointEnd = false;
             earliestTxnId = 10;
        }

        @Override
        public void writeLogEntries(ReplicatedEnvironment rep) {

            /* Ckpt A */
            SingleItemEntry<CheckpointEnd> endEntry =
                SingleItemEntry.create(LogEntryType.LOG_CKPT_END,
                                       new CheckpointEnd
                                       ("test", 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                                        false /*cleanedFilesToDelete*/));
            logManager.log(endEntry, ReplicationContext.NO_REPLICATE);

            /* Commit A */
            TxnCommit commit = new TxnCommit(10, 0, 1);
            earliestPassedTime = commit.getTime();
            CommitLogEntry commitEntry = new CommitLogEntry(commit);
            earliestTxnLsn =
                logManager.log(commitEntry, ReplicationContext.MASTER);

            /* Commit B */
            commitEntry = new CommitLogEntry(new TxnCommit(20, 0, 1));
            logManager.log(commitEntry, ReplicationContext.MASTER);


            /* Ckpt B */
            endEntry =
                SingleItemEntry.create(LogEntryType.LOG_CKPT_END,
                                       new CheckpointEnd
                                       ("test", 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                                        false /*cleanedFilesToDelete*/));
            logManager.log(endEntry, ReplicationContext.NO_REPLICATE);

            logManager.flushNoSync();
        }
    }
}
