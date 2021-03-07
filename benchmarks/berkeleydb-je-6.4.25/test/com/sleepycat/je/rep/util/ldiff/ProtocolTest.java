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

package com.sleepycat.je.rep.util.ldiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.rep.impl.node.NameIdPair;
import com.sleepycat.je.rep.util.TestChannel;
import com.sleepycat.je.rep.utilint.BinaryProtocol.Message;
import com.sleepycat.je.utilint.VLSN;
import com.sleepycat.util.test.TestBase;

public class ProtocolTest  extends TestBase {

    Protocol protocol;
    private Message[] messages;
    private Block testBlock;

    @Before
    public void setUp() 
        throws Exception {
        
        super.setUp();
        protocol = new Protocol(new NameIdPair("n1", (short)1),
                                null);
        testBlock = new Block(5);
        byte[] beginKey = {0, 1, 2, 3};
        testBlock.setBeginKey(beginKey);
        byte[] beginData = {(byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef};
        testBlock.setBeginData(beginData);
        byte[] md5Hash = {(byte)0xdb, (byte)0xcd, (byte)0xdb, (byte)0xcd};
        testBlock.setMd5Hash(md5Hash);
        testBlock.setNumRecords(1 << 13);
        testBlock.setRollingChksum(123456789L);

        MismatchedRegion region = new MismatchedRegion();
        region.setLocalBeginKey(beginKey);
        region.setLocalBeginData(beginData);
        region.setLocalDiffSize(10);
        region.setRemoteBeginKey(beginKey);
        region.setRemoteBeginData(beginData);
        region.setRemoteDiffSize(10);

        Record record = new Record(beginKey, beginData, new VLSN(5));

        messages = new Message[] {
                protocol.new DbBlocks("test.db", 1 << 13),
                protocol.new DbMismatch("test.db does not exist"),
                protocol.new BlockListStart(),
                protocol.new BlockListEnd(),
                protocol.new BlockInfo(testBlock),
                protocol.new EnvDiff(),
                protocol.new EnvInfo(4),
                protocol.new RemoteDiffRequest(region),
                protocol.new RemoteRecord(record),
                protocol.new DiffAreaStart(),
                protocol.new DiffAreaEnd(),
                protocol.new Done(),
                protocol.new Error("An LDiff Error")
        };
    }

    @Test
    public void testBasic()
        throws IOException {

        assertEquals(protocol.messageCount() -
                     protocol.getPredefinedMessageCount(),
                     messages.length);
        for (Message m : messages) {
            ByteBuffer testWireFormat = m.wireFormat().duplicate();
            Message newMessage =
                protocol.read(new TestChannel(testWireFormat));
            assertTrue(newMessage.getOp() + " " +
                       Arrays.toString(testWireFormat.array()) + "!=" +
                       Arrays.toString(newMessage.wireFormat().array()),
                       Arrays.equals(testWireFormat.array().clone(),
                                     newMessage.wireFormat().array().clone()));
        }
    }
}
