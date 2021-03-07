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

import java.util.Arrays;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;

import com.sleepycat.je.JEVersion;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.rep.elections.Proposer.Proposal;
import com.sleepycat.je.rep.elections.Protocol.StringValue;
import com.sleepycat.je.rep.elections.Protocol.Value;
import com.sleepycat.je.rep.elections.Protocol.ValueParser;
import com.sleepycat.je.rep.impl.TextProtocol;
import com.sleepycat.je.rep.impl.TextProtocol.Message;
import com.sleepycat.je.rep.impl.TextProtocolTestBase;
import com.sleepycat.je.rep.impl.node.NameIdPair;
import com.sleepycat.je.rep.net.DataChannelFactory;
import com.sleepycat.je.rep.utilint.RepTestUtils;
import com.sleepycat.je.rep.utilint.net.DataChannelFactoryBuilder;

public class ProtocolTest extends TextProtocolTestBase {

    private Protocol protocol = null;
    private DataChannelFactory channelFactory;

    @Before
    public void setUp()
        throws Exception {

        channelFactory =
            DataChannelFactoryBuilder.construct(
                RepTestUtils.readRepNetConfig(), GROUP_NAME);

        protocol = new Protocol(TimebasedProposalGenerator.getParser(),
                                new ValueParser() {
                                    public Value parse(String wireFormat) {
                                        if ("".equals(wireFormat)) {
                                            return null;
                                        }
                                        return new StringValue(wireFormat);

                                    }
                                },
                                GROUP_NAME,
                                new NameIdPair(NODE_NAME, 1),
                                null,
                                channelFactory);
        protocol.updateNodeIds(new HashSet<Integer>
                               (Arrays.asList(new Integer(1))));
    }

    @After
    public void tearDown() {
        protocol = null;
    }

    @Override
    protected Message[] createMessages() {
        TimebasedProposalGenerator proposalGenerator =
            new TimebasedProposalGenerator();
        Proposal proposal = proposalGenerator.nextProposal();
        Value value = new Protocol.StringValue("test1");
        Value svalue = new Protocol.StringValue("test2");
        Message[] messages = new Message[] {
                protocol.new Propose(proposal),
                protocol.new Accept(proposal, value),
                protocol.new Result(proposal, value),
                protocol.new Shutdown(),
                protocol.new MasterQuery(),

                protocol.new Reject(proposal),
                protocol.new Promise(proposal, value, svalue, 100, 1,
                                     LogEntryType.LOG_VERSION,
                                     JEVersion.CURRENT_VERSION),
                protocol.new Accepted(proposal, value),
                protocol.new MasterQueryResponse(proposal, value)
        };

        return messages;
    }

    @Override
    protected TextProtocol getProtocol() {
        return protocol;
    }
}
