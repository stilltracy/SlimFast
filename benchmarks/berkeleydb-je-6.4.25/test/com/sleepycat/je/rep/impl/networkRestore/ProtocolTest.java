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

package com.sleepycat.je.rep.impl.networkRestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.log.LogUtils;
import com.sleepycat.je.rep.impl.networkRestore.Protocol.FileStart;
import com.sleepycat.je.rep.impl.node.NameIdPair;
import com.sleepycat.je.rep.util.TestChannel;
import com.sleepycat.je.rep.utilint.BinaryProtocol.Message;
import com.sleepycat.je.utilint.Adler32;
import com.sleepycat.je.utilint.VLSN;
import com.sleepycat.util.test.TestBase;

public class ProtocolTest  extends TestBase {

    Protocol protocol;
    private Message[] messages;

    @Before
    public void setUp() 
        throws Exception {
        
        protocol = new Protocol(new NameIdPair("n1", (short)1),
                                Protocol.VERSION,
                                null);

        messages = new Message[] {
                protocol.new FeederInfoReq(),
                protocol.new FeederInfoResp(1, new VLSN(100), new VLSN(200)),
                protocol.new FileListReq(),
                protocol.new FileListResp(new String[]{"f1","f2"}),
                protocol.new FileReq("f1"),
                protocol.new FileStart("f1",100, System.currentTimeMillis()),
                protocol.new FileEnd("f1", 100, System.currentTimeMillis(),
                                      new byte[100]),
                protocol.new FileInfoReq("f1", true),
                protocol.new FileInfoResp("f1", 100, System.currentTimeMillis(),
                                      new byte[100]),
                protocol.new Done(),
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

    @Test
    public void testFileReqResp()
        throws IOException, Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
        WritableByteChannel oc = Channels.newChannel(baos);
        oc.write(protocol.new FileStart("f1", 100, System.currentTimeMillis()).
                 wireFormat().duplicate());

        Adler32 ochecksum = new Adler32();
        CheckedOutputStream cos = new CheckedOutputStream(baos, ochecksum);

        // Simulate a file payload.
        for (int i=0; i < 100; i++)  {
            cos.write(i);
        }
        ByteBuffer csum = ByteBuffer.allocate(8);
        LogUtils.writeLong(csum, ochecksum.getValue());
        baos.write(csum.array());

        byte[] o = baos.toByteArray();

        TestChannel ch =
            new TestChannel((ByteBuffer)ByteBuffer.allocate(o.length).
                            put(o).flip());

        FileStart m = (FileStart) protocol.read(ch);
        long length = m.getFileLength();
        Adler32 ichecksum = new Adler32();
        CheckedInputStream cis =
            new CheckedInputStream(Channels.newInputStream(ch), ichecksum);
        for (int i=0; i < length; i++) {
            assertEquals(i, cis.read());
        }

        csum = ByteBuffer.allocate(8);
        ch.read(csum);
        csum.flip();
        assertEquals(ochecksum.getValue(), LogUtils.readLong(csum));
        assertEquals(ochecksum.getValue(), ichecksum.getValue());
    }
}
