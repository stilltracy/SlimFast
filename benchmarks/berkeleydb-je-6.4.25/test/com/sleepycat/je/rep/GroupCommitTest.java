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

package com.sleepycat.je.rep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.sleepycat.je.Database;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.rep.ReplicatedEnvironment.State;
import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils;

/**
 * test for group commit functionality
 */
public class GroupCommitTest extends RepTestBase {

    @Override
    public void setUp()
        throws Exception {

        /* need just one replica for this test. */
        groupSize = 2;

        super.setUp();
    }

    /**
     * Verify that group commits can be initiated by either exceeding the
     * time interval, or the group commit size.
     */
    @Test
    public void testBasic()
        throws InterruptedException {

        /* Use a very generous full second for the group commit interval. */
        final long intervalNs = TimeUnit.SECONDS.toNanos(1);
        final int maxGroupCommit = 4;

        initGroupCommitConfig(intervalNs, maxGroupCommit);

        createGroup();
        State state = repEnvInfo[0].getEnv().getState();
        assertEquals(State.MASTER, state);
        ReplicatedEnvironment menv = repEnvInfo[0].getEnv();
        ReplicatedEnvironment renv = repEnvInfo[1].getEnv();

        long startNs = System.nanoTime();
        final StatsConfig statsConfig = new StatsConfig().setClear(true);

        /* Clear and discard stats. */
        renv.getRepStats(statsConfig);

        /* Just a single write. */
        doWrites(menv, 1);

        ReplicatedEnvironmentStats rstats = renv.getRepStats(statsConfig);

        /* Verify that the group commit was the result of a timeout. */
        assertTrue((System.nanoTime() - startNs) > intervalNs);

        assertEquals(1, rstats.getNReplayGroupCommitTxns());
        assertEquals(1, rstats.getNReplayGroupCommits());
        assertEquals(1, rstats.getNReplayGroupCommitTimeouts());
        assertEquals(0, rstats.getNReplayCommitSyncs());
        assertEquals(1, rstats.getNReplayCommitNoSyncs());

        /* Now force an exact group commit size overflow. */
        doWrites(menv, maxGroupCommit);
        rstats = renv.getRepStats(statsConfig);

        assertEquals(maxGroupCommit, rstats.getNReplayGroupCommitTxns());
        assertEquals(1, rstats.getNReplayGroupCommits());
        assertEquals(0, rstats.getNReplayGroupCommitTimeouts());
        assertEquals(0, rstats.getNReplayCommitSyncs());
        assertEquals(maxGroupCommit, rstats.getNReplayCommitNoSyncs());

        /* Group commit size + 1 timeout txn */
        doWrites(menv, maxGroupCommit + 1);
        rstats = renv.getRepStats(statsConfig);

        assertEquals(maxGroupCommit + 1, rstats.getNReplayGroupCommitTxns());
        assertEquals(2, rstats.getNReplayGroupCommits());
        assertEquals(1, rstats.getNReplayGroupCommitTimeouts());
        assertEquals(0, rstats.getNReplayCommitSyncs());
        assertEquals(maxGroupCommit + 1, rstats.getNReplayCommitNoSyncs());
    }

    private void initGroupCommitConfig(final long intervalMs,
                                       final int maxGroupCommit)
        throws IllegalArgumentException {

        for (int i=0; i < groupSize; i++) {
            repEnvInfo[i].getRepConfig().
                setConfigParam(ReplicationConfig.REPLICA_GROUP_COMMIT_INTERVAL,
                               intervalMs + " ns");
            repEnvInfo[i].getRepConfig().
                setConfigParam(ReplicationConfig.REPLICA_MAX_GROUP_COMMIT,
                               Integer.toString(maxGroupCommit));
        }
    }

    /**
     * Verify that group commits can be turned off.
     */
    @Test
    public void testGroupCommitOff()
        throws InterruptedException {

        /* Now turn off group commits on the replica */
        initGroupCommitConfig(Integer.MAX_VALUE, 0);

        createGroup();
        /* Already joined, rejoin master. */
        State state = repEnvInfo[0].getEnv().getState();
        assertEquals(State.MASTER, state);
        ReplicatedEnvironment menv = repEnvInfo[0].getEnv();
        ReplicatedEnvironment renv = repEnvInfo[1].getEnv();

        final StatsConfig statsConfig = new StatsConfig().setClear(true);

        /* Clear and discard stats. */
        renv.getRepStats(statsConfig);

        /* Just a single write. */
        doWrites(menv, 1);

        ReplicatedEnvironmentStats rstats = renv.getRepStats(statsConfig);

        assertEquals(0, rstats.getNReplayGroupCommitTxns());
        assertEquals(0, rstats.getNReplayGroupCommits());
        assertEquals(0, rstats.getNReplayGroupCommitTimeouts());
        assertEquals(1, rstats.getNReplayCommitSyncs());
        assertEquals(0, rstats.getNReplayCommitNoSyncs());
    }

    void doWrites(ReplicatedEnvironment menv, int count)
        throws InterruptedException {

        final WriteThread wt[] = new WriteThread[count];

        for (int i=0; i < count; i++) {
            wt[i] = new WriteThread(menv);
            wt[i].start();
        }

        for (int i=0; i < count; i++) {
            wt[i].join(60000);
        }
    }

    /* Used as the basis for producing unique db names. */
    private static AtomicInteger dbId = new AtomicInteger(0);

    /**
     * Thread used to create concurrent updates amenable to group commits.
     */
    private class WriteThread extends Thread {
        ReplicatedEnvironment menv;

        WriteThread(ReplicatedEnvironment menv) {
            super();
            this.menv = menv;
        }

        @Override
        public void run() {
            final TransactionConfig mtc = new TransactionConfig();
            mtc.setDurability(RepTestUtils.SYNC_SYNC_ALL_DURABILITY);
            Transaction mt = menv.beginTransaction(null, mtc);
            Database db = menv.openDatabase(mt,
                                            "testDB" + dbId.incrementAndGet(),
                                            dbconfig);
            mt.commit();
            db.close();
        }
    }
}
