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

package com.sleepycat.util.test;

import static org.junit.Assert.fail;

import java.io.DataOutputStream;
import java.util.Arrays;

import org.junit.Test;

import com.sleepycat.util.FastOutputStream;
import com.sleepycat.util.UtfOps;

/**
 * @author Mark Hayes
 */
public class UtfTest extends TestBase {

    /**
     * Compares the UtfOps implementation to the java.util.DataOutputStream
     * (and by implication DataInputStream) implementation, character for
     * character in the full Unicode set.
     */
    @Test
    public void testMultibyte()
        throws Exception {

        char c = 0;
        byte[] buf = new byte[10];
        byte[] javaBuf = new byte[10];
        char[] cArray = new char[1];
        FastOutputStream javaBufStream = new FastOutputStream(javaBuf);
        DataOutputStream javaOutStream = new DataOutputStream(javaBufStream);

        try {
            for (int cInt = Character.MIN_VALUE; cInt <= Character.MAX_VALUE;
                 cInt += 1) {
                c = (char) cInt;
                cArray[0] = c;
                int byteLen = UtfOps.getByteLength(cArray);

                javaBufStream.reset();
                javaOutStream.writeUTF(new String(cArray));
                int javaByteLen = javaBufStream.size() - 2;

                if (byteLen != javaByteLen) {
                    fail("Character 0x" + Integer.toHexString(c) +
                         " UtfOps size " + byteLen +
                         " != JavaIO size " + javaByteLen);
                }

                Arrays.fill(buf, (byte) 0);
                UtfOps.charsToBytes(cArray, 0, buf, 0, 1);

                if (byteLen == 1 && buf[0] == (byte) 0xff) {
                    fail("Character 0x" + Integer.toHexString(c) +
                         " was encoded as FF, which is reserved for null");
                }

                for (int i = 0; i < byteLen; i += 1) {
                    if (buf[i] != javaBuf[i + 2]) {
                        fail("Character 0x" + Integer.toHexString(c) +
                             " byte offset " + i +
                             " UtfOps byte " + Integer.toHexString(buf[i]) +
                             " != JavaIO byte " +
                             Integer.toHexString(javaBuf[i + 2]));
                    }
                }

                int charLen = UtfOps.getCharLength(buf, 0, byteLen);
                if (charLen != 1) {
                    fail("Character 0x" + Integer.toHexString(c) +
                         " UtfOps char len " + charLen +
                         " but should be one");
                }

                cArray[0] = (char) 0;
                int len = UtfOps.bytesToChars(buf, 0, cArray, 0, byteLen,
                                              true);
                if (len != byteLen) {
                    fail("Character 0x" + Integer.toHexString(c) +
                         " UtfOps bytesToChars(w/byteLen) len " + len +
                         " but should be " + byteLen);
                }

                if (cArray[0] != c) {
                    fail("Character 0x" + Integer.toHexString(c) +
                         " UtfOps bytesToChars(w/byteLen) char " +
                         Integer.toHexString(cArray[0]));
                }

                cArray[0] = (char) 0;
                len = UtfOps.bytesToChars(buf, 0, cArray, 0, 1, false);
                if (len != byteLen) {
                    fail("Character 0x" + Integer.toHexString(c) +
                         " UtfOps bytesToChars(w/charLen) len " + len +
                         " but should be " + byteLen);
                }

                if (cArray[0] != c) {
                    fail("Character 0x" + Integer.toHexString(c) +
                         " UtfOps bytesToChars(w/charLen) char " +
                         Integer.toHexString(cArray[0]));
                }

                String s = new String(cArray, 0, 1);
                byte[] sBytes = UtfOps.stringToBytes(s);
                if (sBytes.length != byteLen) {
                    fail("Character 0x" + Integer.toHexString(c) +
                         " UtfOps stringToBytes() len " + sBytes.length +
                         " but should be " + byteLen);
                }

                for (int i = 0; i < byteLen; i += 1) {
                    if (sBytes[i] != javaBuf[i + 2]) {
                        fail("Character 0x" + Integer.toHexString(c) +
                             " byte offset " + i +
                             " UtfOps byte " + Integer.toHexString(sBytes[i]) +
                             " != JavaIO byte " +
                             Integer.toHexString(javaBuf[i + 2]));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Character 0x" + Integer.toHexString(c) +
                               " exception occurred");
            throw e;
        }
    }
}
