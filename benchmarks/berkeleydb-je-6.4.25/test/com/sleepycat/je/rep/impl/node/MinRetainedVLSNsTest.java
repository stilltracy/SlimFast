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

package com.sleepycat.je.rep.impl.node;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.rep.InsufficientLogException;
import com.sleepycat.je.rep.NetworkRestore;
import com.sleepycat.je.rep.NetworkRestoreConfig;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.impl.RepParams;
import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.rep.vlsn.VLSNRange;
import com.sleepycat.je.utilint.VLSN;

public class MinRetainedVLSNsTest extends RepTestBase {

    @Override
    @Before
    public void setUp() throws Exception {
        groupSize = 4;
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test old behavior with no retained VLSNS
     */
    @Test
    public void testNoneRetained() {
        retainedInternalTest(0);
    }

    @Test
    public void testRetained() {
        retainedInternalTest(1000);
    }

    /**
     * Test to ensure that at least the ninimum number of configured VLSNs
     * is maintained.
     */
    public void retainedInternalTest(int minRetainedVLSNs) {
        setRepConfigParam(RepParams.MIN_RETAINED_VLSNS,
                          Integer.toString(minRetainedVLSNs));

        /*
         * For rapid updates of the global cbvlsn as new log files are created
         */
        setEnvConfigParam(EnvironmentParams.LOG_FILE_MAX, "4000");
        createGroup(3);

        final ReplicatedEnvironment master = repEnvInfo[0].getEnv();
        populateDB(master, 1000);

        /* Create garbage by overwriting */
        populateDB(master, 1000);

        /* Sync group. */
        RepTestUtils.syncGroup(repEnvInfo);

        checkGlobalCBVLSN();

        /*
         * Open a new environment. It must be able to syncup or network
         * restore.
         */
        try {
            repEnvInfo[repEnvInfo.length - 1].openEnv();
        } catch (InsufficientLogException ile) {
            new NetworkRestore().execute(ile, new NetworkRestoreConfig());
            repEnvInfo[repEnvInfo.length - 1].openEnv();
        }

        checkGlobalCBVLSN();
    }

    private void checkGlobalCBVLSN() {
        for (RepEnvInfo info : repEnvInfo) {
            if (info.getEnv() == null) {
                continue;
            }
            final int minRetainedVLSNs = Integer.parseInt(info.getRepConfig().
               getConfigParam(RepParams.MIN_RETAINED_VLSNS.getName()));
            final VLSN groupCBVLSN = info.getRepNode().getGroupCBVLSN();
            final VLSNRange range = info.getRepImpl().getVLSNIndex().getRange();
            final long retainedVLSNs = range.getLast().getSequence() -
                        groupCBVLSN.getSequence();

            assertTrue(retainedVLSNs >= minRetainedVLSNs);
        }
    }
}
