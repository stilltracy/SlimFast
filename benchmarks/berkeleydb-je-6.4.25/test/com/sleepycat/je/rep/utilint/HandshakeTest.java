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

package com.sleepycat.je.rep.utilint;

import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Test;

import com.sleepycat.je.rep.ReplicationNetworkConfig;
import com.sleepycat.je.rep.net.DataChannel;
import com.sleepycat.je.rep.net.DataChannelFactory;
import com.sleepycat.je.rep.net.DataChannelFactory.ConnectOptions;
import com.sleepycat.je.rep.net.PasswordSource;
import com.sleepycat.je.rep.utilint.FreePortLocator;
import com.sleepycat.je.rep.utilint.ServiceDispatcher;
import com.sleepycat.je.rep.utilint.ServiceDispatcher.ServiceConnectFailedException;
import com.sleepycat.je.rep.utilint.ServiceHandshake.AuthenticationMethod;
import com.sleepycat.je.rep.utilint.net.DataChannelFactoryBuilder;
import com.sleepycat.util.test.TestBase;

/**
 * Check various service handshake cases
 */
public class HandshakeTest extends TestBase {

    final static String SERVICE_NAME = "testing";

    InetSocketAddress dispAddr;
    ServiceDispatcher dispatcher;
    BlockingQueue<DataChannel> serviceQueue;

    @Override
    @After
    public void tearDown()
        throws Exception {

        if (dispatcher != null) {
            dispatcher.shutdown();
            dispatcher = null;
        }
    }

    /**
     * Sanity check that no authentication works.
     */
    @Test
    public void testBasicConfig()
        throws Exception {

        DataChannelFactory dcFactory =
            DataChannelFactoryBuilder.construct(
                ReplicationNetworkConfig.createDefault());

        initDispatcher(dcFactory, null);

        DataChannel channel =
            dcFactory.connect(dispAddr, new ConnectOptions());

        ServiceDispatcher.doServiceHandshake(channel, SERVICE_NAME);
    }

    /**
     * Try authentication with our test password authentication implemenation.
     */
    @Test
    public void testPwAuth()
        throws Exception {

        AuthenticationMethod[] authInfo =
            new AuthenticationMethod[] { new TestPasswordAuthentication(
                new TestPasswordSource("hello")) };

        DataChannelFactory dcFactory =
            DataChannelFactoryBuilder.construct(
                ReplicationNetworkConfig.createDefault());

        initDispatcher(dcFactory, authInfo);

        DataChannel channel = dcFactory.connect(dispAddr, new ConnectOptions());

        ServiceDispatcher.doServiceHandshake(channel, SERVICE_NAME, authInfo);
    }

    /**
     * Test no authentication provided when authentication is required.
     */
    @Test
    public void testNoAuthProvided()
        throws Exception {

        ReplicationNetworkConfig repNetConfig =
            ReplicationNetworkConfig.createDefault();

        AuthenticationMethod[] authInfo =
            new AuthenticationMethod[] { new TestPasswordAuthentication(
                new TestPasswordSource("hello")) };

        DataChannelFactory dcFactory =
            DataChannelFactoryBuilder.construct(repNetConfig);

        initDispatcher(dcFactory, authInfo);

        DataChannel channel = dcFactory.connect(dispAddr, new ConnectOptions());

        try {
            ServiceDispatcher.doServiceHandshake(channel, SERVICE_NAME);
            fail("expected exception");
        } catch (ServiceConnectFailedException e) {
        }
    }

    /**
     * Test no common authentication provided when authentication is required.
     */
    @Test
    public void testNoCommonAuth()
        throws Exception {

        ReplicationNetworkConfig repNetConfig =
            ReplicationNetworkConfig.createDefault();

        AuthenticationMethod[] authInfo =
            new AuthenticationMethod[] { new TestPasswordAuthentication(
                new TestPasswordSource("hello")) };

        AuthenticationMethod privateAuth =
            new TestPasswordAuthentication(new TestPasswordSource("hello")) {

                @Override
                public String getMechanismName() {
                    return "private";
                }
            };

        AuthenticationMethod[] privateAuthInfo =
            new AuthenticationMethod[] { privateAuth };

        DataChannelFactory dcFactory =
            DataChannelFactoryBuilder.construct(repNetConfig);

        initDispatcher(dcFactory, authInfo);

        DataChannel channel = dcFactory.connect(dispAddr, new ConnectOptions());

        try {
            ServiceDispatcher.doServiceHandshake(channel, SERVICE_NAME,
                                                 privateAuthInfo);
            fail("expected exception");
        } catch (ServiceConnectFailedException e) {
        }
    }

    /**
     * Test failed authentication
     */
    @Test
    public void testfailedAuth()
        throws Exception {

        ReplicationNetworkConfig repNetConfig =
            ReplicationNetworkConfig.createDefault();

        AuthenticationMethod[] authInfo =
            new AuthenticationMethod[] { new TestPasswordAuthentication(
                new TestPasswordSource("hello")) };

        AuthenticationMethod[] badAuthInfo =
            new AuthenticationMethod[] { new TestPasswordAuthentication(
                new TestPasswordSource("xhello")) };

        DataChannelFactory dcFactory =
            DataChannelFactoryBuilder.construct(repNetConfig);

        initDispatcher(dcFactory, authInfo);

        DataChannel channel = dcFactory.connect(dispAddr, new ConnectOptions());

        try {
            ServiceDispatcher.doServiceHandshake(channel, SERVICE_NAME,
                                                 badAuthInfo);
            fail("expected exception");
        } catch (ServiceConnectFailedException e) {
        }
    }

    private void initDispatcher(DataChannelFactory channelFactory,
                                AuthenticationMethod[] authInfo)
        throws Exception {

        FreePortLocator locator = new FreePortLocator("localhost", 5000, 6000);
        int freePort = locator.next();

        dispAddr = new InetSocketAddress("localhost", freePort);
        dispatcher = new ServiceDispatcher(dispAddr, channelFactory);
        dispatcher.addTestAuthentication(authInfo);
        serviceQueue = new LinkedBlockingQueue<DataChannel>();
        dispatcher.register(SERVICE_NAME, serviceQueue);
        dispatcher.start();
    }

    private class TestPasswordSource implements PasswordSource {
        private final String password;

        private TestPasswordSource(String password) {
            this.password = password;
        }

        @Override
        public char[] getPassword() {
            return password.toCharArray();
        }
    }
}
