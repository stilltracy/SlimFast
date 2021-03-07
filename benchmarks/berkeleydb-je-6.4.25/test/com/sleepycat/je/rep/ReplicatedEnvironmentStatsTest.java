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

import org.junit.Test;

import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;

public class ReplicatedEnvironmentStatsTest extends RepTestBase {

    // TODO: more detailed tests check for expected stat return values under
    // simulated conditions.

    /**
     * Exercise every public entry point on master and replica stats.
     */
    @Test
    public void testBasic() {
        createGroup();

        for (RepEnvInfo ri : repEnvInfo) {
            ReplicatedEnvironment rep = ri.getEnv();
            final ReplicatedEnvironmentStats repStats = rep.getRepStats(null);
            invokeAllAccessors(repStats);
        }
    }

    /**
     * Simply exercise the code path
     */
    private void invokeAllAccessors(ReplicatedEnvironmentStats stats) {
        stats.getAckWaitMs();
        stats.getLastCommitTimestamp();
        stats.getLastCommitVLSN();
        stats.getNFeedersCreated();
        stats.getNFeedersShutdown();
        stats.getNMaxReplicaLag();
        stats.getNMaxReplicaLagName();
        stats.getNProtocolBytesRead();
        stats.getNProtocolBytesWritten();
        stats.getNProtocolMessagesRead();
        stats.getNProtocolMessagesWritten();
        stats.getNReplayAborts();
        stats.getNReplayCommitAcks();
        stats.getNReplayCommitNoSyncs();
        stats.getNReplayCommitSyncs();
        stats.getNReplayCommitWriteNoSyncs();
        stats.getNReplayCommits();
        stats.getNReplayGroupCommitMaxExceeded();
        stats.getNReplayGroupCommitTimeouts();
        stats.getNReplayGroupCommits();
        stats.getNReplayLNs();
        stats.getNReplayNameLNs();
        stats.getNTxnsAcked();
        stats.getNTxnsNotAcked();
        stats.getProtocolBytesReadRate();
        stats.getProtocolBytesWriteRate();
        stats.getProtocolMessageReadRate();
        stats.getProtocolMessageWriteRate();
        stats.getProtocolReadNanos();
        stats.getProtocolWriteNanos();
        stats.getReplayElapsedTxnTime();
        stats.getReplayLatestCommitLagMs();
        stats.getReplayMaxCommitProcessingNanos();
        stats.getReplayMinCommitProcessingNanos();
        stats.getReplayTotalCommitLagMs();
        stats.getReplayTotalCommitProcessingNanos();
        stats.getReplicaDelayMap();
        stats.getReplicaLastCommitTimestampMap();
        stats.getReplicaLastCommitVLSNMap();
        stats.getReplicaVLSNLagMap();
        stats.getReplicaVLSNRateMap();
        stats.getStatGroups();
        stats.getTips();
        stats.getTotalTxnMs();
        stats.getTrackerLagConsistencyWaitMs();
        stats.getTrackerLagConsistencyWaits();
        stats.getTrackerVLSNConsistencyWaitMs();
        stats.getTrackerVLSNConsistencyWaits();
        stats.getVLSNRate();
    }
}
