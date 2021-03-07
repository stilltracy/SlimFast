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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.CommitToken;
import com.sleepycat.je.ReplicaConsistencyPolicy;
import com.sleepycat.je.rep.impl.PointConsistencyPolicy;
import com.sleepycat.je.rep.impl.RepParams;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.je.utilint.VLSN;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class ReplicationConfigTest extends TestBase {

    ReplicationConfig repConfig;

    @Override
    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        repConfig = new ReplicationConfig();
    }

    // TODO: need tests for every entrypoint

    @Test
    public void testConsistency() {

        ReplicaConsistencyPolicy policy =
            new TimeConsistencyPolicy(100, TimeUnit.MILLISECONDS,
                                      1, TimeUnit.SECONDS);
        repConfig.setConsistencyPolicy(policy);
        assertEquals(policy, repConfig.getConsistencyPolicy());

        policy = NoConsistencyRequiredPolicy.NO_CONSISTENCY;
        repConfig.setConsistencyPolicy(policy);
        assertEquals(policy, repConfig.getConsistencyPolicy());

        try {
            policy =
                new CommitPointConsistencyPolicy
                    (new CommitToken(new UUID(0, 0), 0), 0, null);
            repConfig.setConsistencyPolicy(policy);
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            policy =  new PointConsistencyPolicy(VLSN.NULL_VLSN);
            repConfig.setConsistencyPolicy(policy);
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            repConfig.setConfigParam
            (RepParams.CONSISTENCY_POLICY.getName(),
             "badPolicy");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testHelperHosts() {
        /* Correct configs */
        repConfig.setHelperHosts("localhost");
        Set<InetSocketAddress> helperSockets = repConfig.getHelperSockets();
        assertEquals(1, helperSockets.size());
        assertEquals(Integer.parseInt(RepParams.DEFAULT_PORT.getDefault()),
                     helperSockets.iterator().next().getPort());

        repConfig.setHelperHosts("localhost:6000");
        helperSockets = repConfig.getHelperSockets();
        assertEquals(1, helperSockets.size());
        assertEquals(6000, helperSockets.iterator().next().getPort());

        repConfig.setHelperHosts("localhost:6000,localhost:6001");
        helperSockets = repConfig.getHelperSockets();
        assertEquals(2, helperSockets.size());

        /* Incorrect configs */
        /*
         * It would be nice if this were an effective test, but because various
         * ISPs will not actually let their DNS servers return an unknown
         * host, we can't rely on this failing.
        try {
            repConfig.setHelperHosts("unknownhost");
            fail("expected exception");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
         */
        try {
            repConfig.setHelperHosts("localhost:80");
            fail("expected exception");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
        try {
            repConfig.setHelperHosts("localhost:xyz");
            fail("expected exception");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        try {
            repConfig.setHelperHosts(":6000");
            fail("expected exception");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    @Test
    public void testMinRetainedVLSNs() {

        /* Boundary conditions */
        repConfig.setConfigParam(RepParams.MIN_RETAINED_VLSNS.getName(), "0");

        repConfig.setConfigParam(RepParams.MIN_RETAINED_VLSNS.getName(),
                                 Integer.toString(Integer.MAX_VALUE));

        /* Routine */
        repConfig.setConfigParam(RepParams.MIN_RETAINED_VLSNS.getName(),
                                 "100");

        try {
            repConfig.setConfigParam(RepParams.MIN_RETAINED_VLSNS.getName(),
                "-1");
            fail("expected exception");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    @Test
    public void testSerialize()
        throws Throwable {

        ReplicationConfig repConfig = new ReplicationConfig();
        /* Test the serialized fields in RepliationMutableConfig, props. */
        repConfig.setNodeName("node1");
        repConfig.setGroupName("group");
        repConfig.setNodeHostPort("localhost:5001");
        /* Test the serialized fields in RepliationConfig. */
        repConfig.setAllowConvert(true);

        File envHome = SharedTestUtils.getTestDir();
        ReplicationConfig newConfig = (ReplicationConfig)
            TestUtils.serializeAndReadObject(envHome, repConfig);

        assertTrue(newConfig != repConfig);
        assertEquals(newConfig.getNodeName(), "node1");
        assertEquals(newConfig.getGroupName(), "group");
        assertEquals(newConfig.getNodeHostPort(), "localhost:5001");
        assertFalse
            (newConfig.getValidateParams() == repConfig.getValidateParams());
        assertTrue
            (newConfig.getAllowConvert() == repConfig.getAllowConvert());
    }
}
