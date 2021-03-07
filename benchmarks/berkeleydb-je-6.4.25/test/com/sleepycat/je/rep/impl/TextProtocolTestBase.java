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
package com.sleepycat.je.rep.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.sleepycat.je.rep.impl.TextProtocol.InvalidMessageException;
import com.sleepycat.je.rep.impl.TextProtocol.Message;
import com.sleepycat.util.test.TestBase;

/**
 * The superclass for all tests of protocols that inherit from TextProtocol.
 *
 * All subclasses need to create the messages belongs to each sub-protocol and 
 * return an instance of sub-protocol.
 */
public abstract class TextProtocolTestBase extends TestBase {

    private TextProtocol protocol;
    protected static final String GROUP_NAME = "TestGroup";
    protected static final String NODE_NAME = "Node 1";

    /**
     * Verify that all Protocol messages are idempotent under the 
     * serialization/de-serialization sequence.
     * @throws InvalidMessageException
     */
    @Test
    public void testAllMessages()
        throws InvalidMessageException {

        Message[] messages = createMessages();

        protocol = getProtocol();

        /* Ensure that we are testing all of them */
        assertEquals(messages.length, protocol.messageCount());
        /* Now test them. */
        for (Message m : messages) {
            check(m);
            if (!getClass().equals(RepGroupProtocolTest.class) &&
                !getClass().equals(NodeStateProtocolTest.class)) {
                checkMismatch(m);
            }
        }
    }

    /* Create messages for test. */
    protected abstract Message[] createMessages();

    /* Return the concrete protocol. */
    protected abstract TextProtocol getProtocol();

    private void check(Message m1) 
        throws InvalidMessageException {

        String wireFormat = m1.wireFormat();
        Message m2 = protocol.parse(wireFormat);
        assertEquals(m1, m2);
    }

    /* Replaces a specific token vale with the one supplied. */
    private String hackToken(String wireFormat, 
                             TextProtocol.TOKENS tokenType,
                             String hackValue) {
        String[] tokens = wireFormat.split(TextProtocol.SEPARATOR_REGEXP);
        tokens[tokenType.ordinal()] = hackValue;
        String line = "";
        for (String token : tokens) {
            line += (token + TextProtocol.SEPARATOR);
        }

        return line.substring(0, line.length()-1);
    }

    /* Tests consistency checks on message headers. */
    private void checkMismatch(Message m1){
        String[] wireFormats = new String[] {
                hackToken(m1.wireFormat(), TextProtocol.TOKENS.VERSION_TOKEN,
                          "9999999"),
                hackToken(m1.wireFormat(), TextProtocol.TOKENS.NAME_TOKEN,
                          "BADGROUPNAME"),
                hackToken(m1.wireFormat(), TextProtocol.TOKENS.ID_TOKEN, 
                          "0") };

        for (String wireFormat : wireFormats) {
            try {
                protocol.parse(wireFormat);
                fail("Expected Illegal Arg Exception");
            } catch (InvalidMessageException e) {
                assertTrue(true);
            }
        }
    }
}
