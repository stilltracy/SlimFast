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

package com.sleepycat.je.rep.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.rep.NodeState;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationNetworkConfig;
import com.sleepycat.je.rep.ReplicationNode;
import com.sleepycat.je.rep.net.DataChannelFactory;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.je.rep.utilint.net.DataChannelFactoryBuilder;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/*
 * A unit test which tests the DbPing utility.
 */
public class DbPingTest extends TestBase {
    private final File envRoot;
    private RepEnvInfo[] repEnvInfo;

    public DbPingTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    @Override
    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, 1);
    }

    @Override
    @After
    public void tearDown() {
        RepTestUtils.shutdownRepEnvs(repEnvInfo);
    }

    /*
     * Test the function of DbPing when using network properties
     */
    @Test
    public void testDbPingNetProps()
        throws Exception {

        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);

        File propertyFile = new File(repEnvInfo[0].getEnvHome().getPath(),
                                     "je.properties");
        ReplicationNetworkConfig repNetConfig =
            RepTestUtils.readRepNetConfig();

        DataChannelFactory channelFactory =
            DataChannelFactoryBuilder.construct(repNetConfig);

        ReplicationGroupAdmin groupAdmin = new ReplicationGroupAdmin
            (RepTestUtils.TEST_REP_GROUP_NAME,
             master.getRepConfig().getHelperSockets(),
             channelFactory);

        String groupName = groupAdmin.getGroupName();

        Set<ReplicationNode> replicationNodes =
            groupAdmin.getGroup().getElectableNodes();
        assertTrue(replicationNodes.size() > 0);
        for (ReplicationNode replicationNode : replicationNodes) {
            /*
             * Test DbPing with network properties set via a configuration 
             * object
             */
            DbPing propsPing = new DbPing(
                replicationNode, groupName, 10000, repNetConfig);
            NodeState propsNodeState = propsPing.getNodeState();
            assertEquals(propsNodeState.getGroupName(), groupName);

            /* Test DbPing with network properties set via a property file */
            DbPing filePing = new DbPing(
                replicationNode, groupName, 10000, propertyFile);
            NodeState fileNodeState = filePing.getNodeState();
            assertEquals(fileNodeState.getGroupName(), groupName);

            /* Test DbPing with an explicit channel factory */
            DbPing factoryPing = new DbPing(
                replicationNode, groupName, 10000, channelFactory);
            NodeState factoryNodeState = factoryPing.getNodeState();
            assertEquals(factoryNodeState.getGroupName(), groupName);
        }
    }

    /*
     * Test the -netProps command-line argument
     */
    @Test
    public void testDbPingNetPropsCommandLine()
        throws Exception {

        ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);

        File propertyFile = new File(repEnvInfo[0].getEnvHome().getPath(),
                                     "je.properties");

        ReplicationGroupAdmin groupAdmin = new ReplicationGroupAdmin
            (RepTestUtils.TEST_REP_GROUP_NAME,
             master.getRepConfig().getHelperSockets(),
             RepTestUtils.readRepNetConfig());

        Set<ReplicationNode> replicationNodes =
            groupAdmin.getGroup().getElectableNodes();
        assertTrue(replicationNodes.size() > 0);
        for (ReplicationNode replicationNode : replicationNodes) {

            String[] args = new String[] {
                "-groupName", RepTestUtils.TEST_REP_GROUP_NAME,
                "-nodeName", replicationNode.getName(),
                "-nodeHost", master.getRepConfig().getNodeHostPort(),
                "-netProps", propertyFile.getPath(),
                "-socketTimeout", "5000" };

            /* Ping the node. */
            PrintStream original = System.out;
            try {
                /* Avoid polluting the test output. */
                System.setOut(new PrintStream(new ByteArrayOutputStream()));

                DbPing.main(args);

            } catch (Exception e) {
                fail("Unexpected exception: " + LoggerUtils.getStackTrace(e));
            } finally {
                System.setOut(original);
            }
        }
    }
}
