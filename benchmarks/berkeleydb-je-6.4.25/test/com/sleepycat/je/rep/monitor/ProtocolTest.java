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
package com.sleepycat.je.rep.monitor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.JEVersion;
import com.sleepycat.je.rep.NodeType;
import com.sleepycat.je.rep.impl.RepGroupImpl;
import com.sleepycat.je.rep.impl.RepNodeImpl;
import com.sleepycat.je.rep.impl.TextProtocol;
import com.sleepycat.je.rep.impl.TextProtocol.InvalidMessageException;
import com.sleepycat.je.rep.impl.TextProtocol.Message;
import com.sleepycat.je.rep.impl.TextProtocolTestBase;
import com.sleepycat.je.rep.impl.node.NameIdPair;
import com.sleepycat.je.rep.net.DataChannelFactory;
import com.sleepycat.je.rep.monitor.GroupChangeEvent.GroupChangeType;
import com.sleepycat.je.rep.monitor.LeaveGroupEvent.LeaveReason;
import com.sleepycat.je.rep.monitor.Protocol.GroupChange;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.net.DataChannelFactoryBuilder;

public class ProtocolTest extends TextProtocolTestBase {

    private Protocol protocol;
    private DataChannelFactory channelFactory;

    @Override
    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        channelFactory =
            DataChannelFactoryBuilder.construct(
                RepTestUtils.readRepNetConfig());
        protocol =
            new Protocol(GROUP_NAME, new NameIdPair(NODE_NAME, 1), null,
                         channelFactory);
        protocol.updateNodeIds(new HashSet<Integer>
                               (Arrays.asList(new Integer(1))));
    }

    @Override
    @After
    public void tearDown() {
        protocol = null;
    }

    @Override
    protected Message[] createMessages() {
        Message[] messages = new Message [] {
                protocol.new GroupChange(
                    new RepGroupImpl(GROUP_NAME, null), NODE_NAME,
                    GroupChangeType.ADD),
                protocol.new JoinGroup(NODE_NAME,
                                       null,
                                       System.currentTimeMillis()),
                protocol.new LeaveGroup(NODE_NAME, null,
                                        LeaveReason.ABNORMAL_TERMINATION,
                                        System.currentTimeMillis(),
                                        System.currentTimeMillis())
        };

        return messages;
    }

    @Override
    protected TextProtocol getProtocol() {
        return protocol;
    }

    /**
     * Test parsing messages with version differences between the message and
     * the protocol for the change to add the jeVersion field to the
     * RepNodeImpl class.
     */
    @Test
    public void testJEVersionVersioning()
        throws InvalidMessageException {

        /* New group format with JE version and new node types */
        final RepNodeImpl newNode = new RepNodeImpl(
            new NameIdPair("m1", 1), NodeType.MONITOR, "localhost", 5000,
            JEVersion.CURRENT_VERSION);
        final RepNodeImpl secondaryNode = new RepNodeImpl(
            new NameIdPair("s1", 2), NodeType.SECONDARY, "localhost", 5001,
            JEVersion.CURRENT_VERSION);
        final RepGroupImpl newGroup = new RepGroupImpl(GROUP_NAME, null);
        final Map<Integer, RepNodeImpl> nodeMap =
            new HashMap<Integer, RepNodeImpl>();
        nodeMap.put(1, newNode);
        nodeMap.put(2, secondaryNode);
        newGroup.setNodes(nodeMap);

        /* Old protocol using RepGroupImpl version 2 */
        final Protocol oldProtocol =
            new Protocol(Protocol.REP_GROUP_V2_VERSION, GROUP_NAME,
                         new NameIdPair(NODE_NAME, 1), null,
                         channelFactory);

        /* Old group format with no JE version or new node types */
        final RepNodeImpl oldNode = new RepNodeImpl(
            new NameIdPair("m1", 1), NodeType.MONITOR, "localhost", 5000,
            null);
        final RepGroupImpl oldGroup =
            new RepGroupImpl(GROUP_NAME, newGroup.getUUID(),
                             RepGroupImpl.FORMAT_VERSION_2);
        oldGroup.setNodes(Collections.singletonMap(1, oldNode));

        /* Old message format, using new group format, to check conversion */
        final GroupChange oldGroupChange = oldProtocol.new GroupChange(
            newGroup, NODE_NAME, GroupChangeType.ADD);

        /* Receive old format with old protocol */
        final GroupChange oldGroupChangeViaOld =
            (GroupChange) oldProtocol.parse(oldGroupChange.wireFormat());
        assertEquals("Old message format via old protocol should use old" +
                     " group format",
                     oldGroup, oldGroupChangeViaOld.getGroup());

        /* Receive old format with new protocol */
        final GroupChange oldGroupChangeViaNew =
            (GroupChange) protocol.parse(oldGroupChange.wireFormat());
        assertEquals("Old message format via new protocol should use old" +
                     " group format",
                     oldGroup, oldGroupChangeViaNew.getGroup());

        /* Receive new format with old protocol */
        final GroupChange newGroupChange = protocol.new GroupChange(
                newGroup, NODE_NAME, GroupChangeType.ADD);
        try {
            oldProtocol.parse(newGroupChange.wireFormat());
            fail("Expected InvalidMessageException when old protocol" +
                 " receives new format message");
        } catch (InvalidMessageException e) {
            assertEquals("New message format via old protocol should produce" +
                         " a version mismatch",
                         TextProtocol.MessageError.VERSION_MISMATCH,
                         e.getErrorType());
        }
    }
}
