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

package com.sleepycat.je.rep.subscription;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sleepycat.je.rep.impl.RepImpl;
import com.sleepycat.je.rep.stream.FeederFilter;
import com.sleepycat.je.rep.stream.OutputWireRecord;
import com.sleepycat.je.rep.utilint.HostPortPair;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.je.utilint.VLSN;
import com.sleepycat.util.test.TestBase;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test that SubscriptionConfig can be initialized, set, and read correctly.
 */
public class SubscriptionConfigTest extends TestBase {

    private final String home = "./test/subscription/";
    private final String feederHostPortPair = "localhost:6000";
    private final String subNodeName = "test-subscriber";
    private final String subHostPortPair = "localhost:6000";
    private final String groupName = "rg1";
    private final UUID   groupUUID =
        UUID.fromString("cb675927-433a-4ed6-8382-0403e9861619");
    private SubscriptionConfig config;

    private final Logger logger =
            LoggerUtils.getLoggerFixedPrefix(getClass(), "Test");

    @Override
    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        config = new SubscriptionConfig(subNodeName, home, subHostPortPair,
                                        feederHostPortPair, groupName,
                                        groupUUID);
    }

    @Test
    public void testInitialziedParameters() throws Exception {
        assertEquals(home, config.getSubscriberHome());
        assertEquals(subNodeName, config.getSubNodeName());
        assertEquals(subHostPortPair, config.getSubNodeHostPort());

        assertEquals(HostPortPair.getHostname(feederHostPortPair),
                     config.getFeederHost());
        assertEquals(HostPortPair.getPort(feederHostPortPair),
                     config.getFeederPort());

        assertEquals(groupName, config.getGroupName());
        assertEquals(groupUUID, config.getGroupUUID());
    }

    @Test
    public void testSetParameters() {
        long timeout = 10000;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        config.setChannelTimeout(timeout, unit);
        assertEquals(timeout, config.getChannelTimeout(unit));
        config.setPreHeartbeatTimeout(2 * timeout, unit);
        assertEquals(2 * timeout, config.getPreHeartbeatTimeout(unit));
        config.setStreamOpenTimeout(3 * timeout, unit);
        assertEquals(3 * timeout, config.getStreamOpenTimeout(unit));

        int interval = 2000;
        config.setHeartbeatInterval(interval);
        assertEquals(interval, config.getHeartbeatIntervalMs());

        int sz = 10240;
        config.setInputMessageQueueSize(sz);
        assertEquals(sz, config.getInputMessageQueueSize());
        config.setOutputMessageQueueSize(2 * sz);
        assertEquals(2 * sz, config.getOutputMessageQueueSize());

        config.setReceiveBufferSize(3 * sz);
        assertEquals(3 * sz, config.getReceiveBufferSize());
    }

    @Test
    public void testFeederFilter() throws Exception {

        /* filter cannot be null */
        try {
            config = new SubscriptionConfig(subNodeName,
                                            "./",
                                            subHostPortPair,
                                            feederHostPortPair,
                                            groupName);


            config.setFeederFilter(null);
        } catch (IllegalArgumentException e) {
            /* expected exception due to null feeder filter */
            logger.info("Expected IllegalArgumentException " + e.getMessage());
        }

        /* filter is set correctly */
        config = new SubscriptionConfig(subNodeName,
                                        "./",
                                        subHostPortPair,
                                        feederHostPortPair,
                                        groupName);

        final String token = "test filter";
        config.setFeederFilter(new TestFeederFilter(token));
        assert(config.getFeederFilter().toString().equals(token));
    }

    @Test
    public void testCallback() throws Exception {

        /* callback cannot be null */
        try {
            config = new SubscriptionConfig(subNodeName,
                                            "./",
                                            subHostPortPair,
                                            feederHostPortPair,
                                            groupName);


            config.setCallback(null);
        } catch (IllegalArgumentException e) {
            /* expected exception due to null feeder filter */
            logger.info("Expected IllegalArgumentException " + e.getMessage());
        }

        /* callback is set correctly */
        config = new SubscriptionConfig(subNodeName,
                                        "./",
                                        subHostPortPair,
                                        feederHostPortPair,
                                        groupName);

        final String token = "TestCallback";
        config.setCallback(new TestCallback(token));
        assert(config.getCallBack().toString().equals(token));
    }


    @Test
    public void testMissingParameters() throws Exception {

        /* making missing parameters */
        try {
            config = new SubscriptionConfig(null,
                                            "./",
                                            subHostPortPair,
                                            feederHostPortPair,
                                            groupName);

            /* should not be able to create a config with missing parameters */
            fail("Expect IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            /* expected exception */
            logger.info("Expected IllegalArgumentException " + e.getMessage());
        }

        try {
            config = new SubscriptionConfig(subNodeName,
                                            null,
                                            subHostPortPair,
                                            feederHostPortPair,
                                            groupName);

            /* should not be able to create a config with missing parameters */
            fail("Expect IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            /* expected exception */
            logger.info("Expected IllegalArgumentException " + e.getMessage());
        }

        try {
            config = new SubscriptionConfig(subNodeName,
                                            "./",
                                            null,
                                            feederHostPortPair,
                                            groupName);

            /* should not be able to create a config with missing parameters */
            fail("Expect IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            /* expected exception */
            logger.info("Expected IllegalArgumentException " + e.getMessage());
        }

        try {
            config = new SubscriptionConfig(subNodeName,
                                            "./",
                                            subHostPortPair,
                                            null,
                                            groupName);

            /* should not be able to create a config with missing parameters */
            fail("Expect IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            /* expected exception */
            logger.info("Expected IllegalArgumentException " + e.getMessage());
        }

        try {
            config = new SubscriptionConfig(subNodeName,
                                            "./",
                                            subHostPortPair,
                                            feederHostPortPair,
                                            null);

            /* should not be able to create a config with missing parameters */
            fail("Expect IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            /* expected exception */
            logger.info("Expected IllegalArgumentException " + e.getMessage());
        }
    }

    /* no-op test callback */
    private class TestCallback implements SubscriptionCallback {

        private final String id;

        TestCallback(String id) {
            this.id = id;
        }

        @Override
        public void processPut(VLSN vlsn, byte[] key, byte[] value,
                               long txnId) {

        }

        @Override
        public void processDel(VLSN vlsn, byte[] key, long txnId) {

        }

        @Override
        public void processCommit(VLSN vlsn, long txnid) {

        }

        @Override
        public void processAbort(VLSN vlsn, long txnid) {

        }

        @Override
        public void processException(final Exception exception) {

        }

        @Override
        public String toString() {
            return id;
        }
    }

    /* no-op test feeder filter */
    private class TestFeederFilter implements FeederFilter, Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;

        TestFeederFilter(String id) {
            this.id = id;
        }

        @Override
        public OutputWireRecord execute(final OutputWireRecord record,
                                        final RepImpl repImpl) {
            return record;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
