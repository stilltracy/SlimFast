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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.impl.RepTestBase;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;

/**
 * A master going up and down should result in an election being held even if
 * all the replicas still agree that it's the master. This is because the
 * other replicas may have more up to date logs than the master, which may
 * have lost transactions when it went down.
 *
 * SR17911 has more detail.
 */
public class MasterBounceTest extends RepTestBase {

    /* (non-Javadoc)
     * @see com.sleepycat.je.rep.impl.RepTestBase#setUp()
     */
    @Before
    public void setUp() 
        throws Exception {

        /*
         * A rep group of two effectively prevents another election from being
         * held across the bounce, since there is no election quorum.
         */
        groupSize = 2;
        super.setUp();
    }

    @Test
    public void testBounce() {
        createGroup();
        final RepEnvInfo masterInfo = repEnvInfo[0];
        ReplicatedEnvironment master = masterInfo.getEnv();
        assertTrue(master.getState().isMaster());

        /* No elections since the group grew around the first node. */
        assertEquals(0, masterInfo.getRepNode().getElections().
                        getElectionCount());

        masterInfo.closeEnv();
        masterInfo.openEnv();

        /* Verify that an election was held to select a new master. */
        assertEquals(1, masterInfo.getRepNode().getElections().
                        getElectionCount());
    }
}
