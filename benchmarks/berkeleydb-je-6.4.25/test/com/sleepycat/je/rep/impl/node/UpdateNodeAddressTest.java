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
import static org.junit.Assert.fail;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashSet;

import org.junit.Test;

import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.dbi.EnvironmentFailureReason;
import com.sleepycat.je.rep.MasterStateException;
import com.sleepycat.je.rep.MemberNotFoundException;
import com.sleepycat.je.rep.ReplicaStateException;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.util.ReplicationGroupAdmin;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.RepTestUtils.RepEnvInfo;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * Tests for the {@code updateAddress()} feature.
 */
public class UpdateNodeAddressTest extends TestBase {
    /* Replication tests use multiple environments. */
    private final File envRoot;
    private RepEnvInfo[] repEnvInfo;

    public UpdateNodeAddressTest() {
        envRoot = SharedTestUtils.getTestDir();
    }

    /**
     * Test that incorrect operations will throw correct exceptions.
     */
    @Test
    public void testUpdateExceptions()
        throws Throwable {

        try {
            /* Set up the replication group. */
            repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, 3);
            ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);

            /* Create the ReplicationGroupAdmin. */
            HashSet<InetSocketAddress> helperSets =
                new HashSet<InetSocketAddress>();
            helperSets.add(master.getRepConfig().getNodeSocketAddress());
            ReplicationGroupAdmin groupAdmin =
                new ReplicationGroupAdmin(RepTestUtils.TEST_REP_GROUP_NAME,
                                          helperSets,
                                          RepTestUtils.readRepNetConfig());

            /*
             * Try to update an unexisted node, expect a
             * MemberNotFoundException.
             */
            try {
                groupAdmin.updateAddress("node4", "localhost", 5004);
                fail("Expect exceptions here");
            } catch (MemberNotFoundException e) {
                /* Expected exception. */
            } catch (Exception e) {
                fail("Unexpected exception: " + e);
            }

            /* Try to update the master, expect a MasterStateException. */
            try {
                groupAdmin.updateAddress
                    (master.getNodeName(), "localhost", 5004);
                fail("Expect exceptions here");
            } catch (MasterStateException e) {
                /* Expected exception. */
            } catch (Exception e) {
                fail("Unexpected exception: " + e);
            }

            /*
             * Try to update a node that is still alive, expect
             * ReplicaStateException.
             */
            try {
                groupAdmin.updateAddress
                    (repEnvInfo[2].getEnv().getNodeName(),
                     "localhost", 5004);
                fail("Expect exceptions here");
            } catch (ReplicaStateException e) {
                /* Expected exception. */
            } catch (Exception e) {
                fail("Unexpected exception: " + e);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            RepTestUtils.shutdownRepEnvs(repEnvInfo);
        }
    }

    /*
     * Test that the node after updating its address can work after removing
     * all its old log files.
     */
    @Test
    public void testUpdateAddressWithNoFormerLogs()
        throws Throwable {

        doTest(true);
    }

    /*
     * Do the test, if deleteLogFiles is true, the former environment home for
     * the node whose address updated will be deleted.
     */
    private void doTest(boolean deleteLogFiles)
        throws Throwable {

        try {

            /*
             * Disable the LocalCBVLSN changes so that no
             * InsufficientLogException will be thrown when restart the node
             * whose address has been updated.
             */
            LocalCBVLSNUpdater.setSuppressGroupDBUpdates(true);

            /* Create the replication group. */
            repEnvInfo = RepTestUtils.setupEnvInfos(envRoot, 3);
            ReplicatedEnvironment master = RepTestUtils.joinGroup(repEnvInfo);

            /* Create the ReplicationGroupAdmin. */
            HashSet<InetSocketAddress> helperSets =
                new HashSet<InetSocketAddress>();
            helperSets.add(master.getRepConfig().getNodeSocketAddress());
            ReplicationGroupAdmin groupAdmin =
                new ReplicationGroupAdmin(RepTestUtils.TEST_REP_GROUP_NAME,
                                          helperSets,
                                          RepTestUtils.readRepNetConfig());

            /* Shutdown node3. */
            InetSocketAddress nodeAddress =
                repEnvInfo[2].getRepConfig().getNodeSocketAddress();
            String nodeName = repEnvInfo[2].getEnv().getNodeName();
            File envHome = repEnvInfo[2].getEnv().getHome();
            repEnvInfo[2].closeEnv();

            /* Update the address for node3. */
            try {
                groupAdmin.updateAddress(nodeName,
                                         nodeAddress.getHostName(),
                                         nodeAddress.getPort() + 1);
            } catch (Exception e) {
                fail("Unexpected exception: " + e);
            }

            /* Restart node3 will get an EnvironmentFailureException. */
            try {
                repEnvInfo[2].openEnv();
                fail("Expect exceptions here.");
            } catch (EnvironmentFailureException e) {
                /* Expected exception. */
                assertEquals(e.getReason(),
                             EnvironmentFailureReason.HANDSHAKE_ERROR);
            } catch (Exception e) {
                fail("Unexpected excpetion: " + e);
            }

            /*
             * Delete all files in node3's environment home so that node3 can
             * restart as a fresh new node.
             */
            assertTrue(envHome.exists());

            /* Delete the former log files if we'd like to. */
            if (deleteLogFiles) {
                for (File file : envHome.listFiles()) {
                    /* Don't delete the je.properties. */
                    if (file.getName().contains("properties")) {
                        continue;
                    }

                    assertTrue(file.isFile());
                    assertTrue(file.delete());
                }
            }

            /* Reset the ReplicationConfig and restart again. */
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            envConfig.setTransactional(true);

            ReplicationConfig repConfig = new ReplicationConfig();
            repConfig.setNodeName(nodeName);
            repConfig.setGroupName(RepTestUtils.TEST_REP_GROUP_NAME);
            repConfig.setNodeHostPort(nodeAddress.getHostName() + ":" +
                                      (nodeAddress.getPort() + 1));
            repConfig.setHelperHosts(master.getRepConfig().getNodeHostPort());

            ReplicatedEnvironment replica = null;
            try {
                replica =
                    new ReplicatedEnvironment(envHome, repConfig, envConfig);
            } catch (Exception e) {
                fail("Unexpected exception: " + e);
            } finally {
                if (replica != null) {
                    replica.close();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            RepTestUtils.shutdownRepEnvs(repEnvInfo);
        }
    }

    /*
     * Test that address updates does work even the node reuses its old log
     * files.
     */
    @Test
    public void testUpdateAddressWithFormerLogs()
        throws Throwable {

        doTest(false);
    }
}
