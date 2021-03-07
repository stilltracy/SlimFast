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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import com.sleepycat.je.rep.net.DataChannel;
import com.sleepycat.je.rep.net.PasswordSource;
import com.sleepycat.je.rep.utilint.ServiceDispatcher.Response;
import com.sleepycat.je.rep.utilint.ServiceHandshake.AuthenticationMethod;
import com.sleepycat.je.rep.utilint.ServiceHandshake.ClientHandshake;
import com.sleepycat.je.rep.utilint.ServiceHandshake.ClientInitOp;
import com.sleepycat.je.rep.utilint.ServiceHandshake.ServerHandshake;
import com.sleepycat.je.rep.utilint.ServiceHandshake.ServerInitOp;
import com.sleepycat.je.rep.utilint.ServiceHandshake.InitResult;
import com.sleepycat.je.rep.utilint.ServiceHandshake.IOAdapter;
import com.sleepycat.utilint.StringUtils;

/**
 * This class provides a sample implementation of an authentication method
 * that uses clear text passwords.
 */
class TestPasswordAuthentication implements AuthenticationMethod {

    /* The indicator for the clear password authentication method. */
    static final String MECHANISM = "TestPassword";

    protected PasswordSource passwordSource;

    TestPasswordAuthentication(PasswordSource passwordSource) {
        this.passwordSource = passwordSource;
    }

    @Override
    public String getMechanismName() {
        return MECHANISM;
    }

    @Override
    public ClientInitOp getClientOp(ClientHandshake initState,
                                    String ignoredParams) {
        return new ClientPasswordOp(initState, passwordSource);
    }

    @Override
    public ServerInitOp getServerOp(ServerHandshake initState) {
        return new ServerPasswordOp(initState, passwordSource);
    }

    @Override
    public String getServerParams() {
        return "";
    }

    /**
     * Server-side implementation. Reads the password and compares
     * to the expected password.
     * Password format is:
     *    Length:   1 byte, range 0-127
     *    Password: <Length> bytes ASCII encoded string
     */
    static class ServerPasswordOp extends ServerInitOp {

        private final static int INITIAL_BUFFER_SIZE = 1;
        private final PasswordSource passwordSource;
        private ByteBuffer buffer;
        private boolean sizeRead = false;

        ServerPasswordOp(ServerHandshake initState,
                         PasswordSource passwordSource) {
            super(initState);
            this.passwordSource = passwordSource;
            this.buffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
        }

        @Override
        InitResult processOp(DataChannel channel) throws IOException {
            InitResult readResult = fillBuffer(channel, buffer);
            if (readResult != InitResult.DONE) {
                return readResult;
            }

            if (sizeRead == false) {
                /* We've just read the size into the buffer */
                sizeRead = true;
                buffer.flip();
                final int passwordLen = buffer.get();
                if (passwordLen <= 0) {
                    initState.logMsg(Level.WARNING,
                                     true, // noteError
                                     "Bad password length: " + passwordLen);
                    sendBuffer(channel, Response.FORMAT_ERROR.byteBuffer());
                    closeChannel(channel);
                    return InitResult.FAIL;
                }
                buffer = ByteBuffer.allocate(passwordLen);

                readResult = fillBuffer(channel, buffer);
                if (readResult != InitResult.DONE) {
                    return readResult;
                }
            }

            buffer.flip();
            /* Now get the password itself */
            final CharBuffer passwordBuf = StringUtils.fromASCII(buffer);

            /* Erase the password info since we no longer need it */
            zero(buffer);
            final char[] passwordChars = passwordBuf.array();

            try {
                if (Arrays.equals(passwordSource.getPassword(),
                                  passwordChars)) {
                    return InitResult.DONE;
                }
            } finally {
                /* Again, clean up after ourselves */
                zero(passwordChars);
            }

            initState.logMsg(Level.WARNING,
                             true, // noteError
                             "Bad password received - length: " +
                             buffer.capacity());
            /*
             * Don't immediately communicate the failure to the client
             * in case this is a password guessing attack.  Make them
             * wait for a response.
             */
            return InitResult.REJECT;
        }
    }

    /**
     * Client-side: authenticate via password.
     */
    static class ClientPasswordOp extends ClientInitOp {

        private final PasswordSource passwordSource;

        ClientPasswordOp(ClientHandshake initState,
                         PasswordSource passwordSource) {
            super(initState);
            this.passwordSource = passwordSource;
        }

        @Override
        InitResult processOp(IOAdapter ioAdapter) throws IOException {
            final char[] password = passwordSource.getPassword();
            final byte[] passwordMessage = servicePasswordMessage(password);
            zero(password);
            ioAdapter.write(passwordMessage);
            zero(passwordMessage);

            final byte[] responseByte = new byte[1];
            final int result = ioAdapter.read(responseByte);
            if (result < 0) {
                throw new IOException(
                    "No service authenticate response byte: " + result);
            }
            final Response response = Response.get(responseByte[0]);
            if (response == null) {
                throw new IOException("Unexpected read response byte: " +
                                      responseByte[0]);
            }
            setResponse(response);
            return InitResult.DONE;
        }

        /**
         * Builds a password component of the authentication message
         */
        private byte[] servicePasswordMessage(char[] password) {
            final CharBuffer passwordCharBuf = CharBuffer.wrap(password);
            final ByteBuffer passwordByteBuf =
                StringUtils.toASCII(passwordCharBuf);
            final int length = 1 + passwordByteBuf.limit();
            final ByteBuffer buffer = ByteBuffer.allocate(length);
            buffer.put((byte)passwordByteBuf.limit()).put(passwordByteBuf);
            zero(passwordByteBuf);
            return buffer.array();
        }
    }

    /**
     * Zero out the buffer.
     */
    private static void zero(ByteBuffer buf) {
        buf.clear();
        for (int i = 0; i < buf.limit(); i++) {
            buf.put((byte)0);
        }
    }

    private static void zero(byte[] buf) {
        Arrays.fill(buf, (byte)0);
    }

    private static void zero(char[] buf) {
        Arrays.fill(buf, ' ');
    }
}
