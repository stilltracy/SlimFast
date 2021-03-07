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

package com.sleepycat.je.rep.elections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.rep.elections.Proposer.Proposal;
import com.sleepycat.je.rep.impl.node.CommitFreezeLatch;

public class VLSNFreezeLatchTest {

    private CommitFreezeLatch latch = new CommitFreezeLatch();
    /* A sequential series of proposals */
    private Proposal p1, p2, p3;

    @Before
    public void setUp() 
        throws Exception {
        
        latch = new CommitFreezeLatch();
        latch.setTimeOut(10 /* ms */);
        TimebasedProposalGenerator pg = new TimebasedProposalGenerator(1);
        p1 = pg.nextProposal();
        p2 = pg.nextProposal();
        p3 = pg.nextProposal();
    }

    @Test
    public void testTimeout()
        throws InterruptedException {

        latch.freeze(p2);
        // Earlier event does not release waiters
        latch.vlsnEvent(p1);

        assertFalse(latch.awaitThaw());
        assertEquals(1, latch.getAwaitTimeoutCount());
    }

    @Test
    public void testElection()
        throws InterruptedException {

        latch.freeze(p2);
        latch.vlsnEvent(p2);
        assertTrue(latch.awaitThaw());
        assertEquals(1, latch.getAwaitElectionCount());
    }

    @Test
    public void testNewerElection()
        throws InterruptedException {

        latch.freeze(p2);
        latch.vlsnEvent(p3);
        assertTrue(latch.awaitThaw());
        assertEquals(1, latch.getAwaitElectionCount());
    }

    @Test
    public void testNoFreeze()
        throws InterruptedException {

        latch.vlsnEvent(p1);

        assertFalse(latch.awaitThaw());
        assertEquals(0, latch.getAwaitTimeoutCount());
    }
}
