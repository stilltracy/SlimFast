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

package com.sleepycat.je.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;

import org.junit.Test;

import com.sleepycat.je.utilint.Timestamp;
import com.sleepycat.util.test.TestBase;

/**
 *  Test basic marshalling utilities
 */
public class LogUtilsTest extends TestBase {

    @Test
    public void testMarshalling() {
        ByteBuffer dest = ByteBuffer.allocate(100);

        // unsigned ints
        long unsignedData = 10;
        dest.clear();
        LogUtils.writeUnsignedInt(dest, unsignedData);
        assertEquals(LogUtils.UNSIGNED_INT_BYTES, dest.position());
        dest.flip();
        assertEquals(unsignedData, LogUtils.readUnsignedInt(dest));

        unsignedData = 49249249L;
        dest.clear();
        LogUtils.writeUnsignedInt(dest, unsignedData);
        assertEquals(LogUtils.UNSIGNED_INT_BYTES, dest.position());
        dest.flip();
        assertEquals(unsignedData, LogUtils.readUnsignedInt(dest));

        // ints
        int intData = -1021;
        dest.clear();
        LogUtils.writeInt(dest, intData);
        assertEquals(LogUtils.INT_BYTES, dest.position());
        dest.flip();
        assertEquals(intData, LogUtils.readInt(dest));

        intData = 257;
        dest.clear();
        LogUtils.writeInt(dest, intData);
        assertEquals(LogUtils.INT_BYTES, dest.position());
        dest.flip();
        assertEquals(intData, LogUtils.readInt(dest));

        // longs
        long longData = -1021;
        dest.clear();
        LogUtils.writeLong(dest, longData);
        assertEquals(LogUtils.LONG_BYTES, dest.position());
        dest.flip();
        assertEquals(longData, LogUtils.readLong(dest));

        // byte arrays
        byte[] byteData = new byte[] {1,2,3,4,5,6,7,8,9,10,11,12};
        dest.clear();
        LogUtils.writeByteArray(dest, byteData);
        assertEquals(LogUtils.getPackedIntLogSize(12) + 12, dest.position());
        dest.flip();
        assertTrue(Arrays.equals(byteData,
                                 LogUtils.readByteArray(dest,
                                                        false/*unpacked*/)));

        // Strings
        String stringData = "Hello world!";
        dest.clear();
        LogUtils.writeString(dest, stringData);
        assertEquals(LogUtils.getPackedIntLogSize(12) + 12, dest.position());
        dest.flip();
        assertEquals(stringData,
                     LogUtils.readString
                     (dest, false/*unpacked*/, LogEntryType.LOG_VERSION));

        // String with multi-byte char, a Euro sign represented as 3 UTF bytes
        String multiByteData = "Hello Euro!\u20ac";
        dest.clear();
        LogUtils.writeString(dest, multiByteData);
        assertEquals(LogUtils.getPackedIntLogSize(14) + 14, dest.position());
        dest.flip();
        assertEquals(multiByteData,
            LogUtils.readString(
                dest, false/*unpacked*/, LogEntryType.LOG_VERSION));

        // Timestamps
        Timestamp timestampData =
            new Timestamp(Calendar.getInstance().getTimeInMillis());
        dest.clear();
        LogUtils.writeTimestamp(dest, timestampData);
        assertEquals(LogUtils.getTimestampLogSize(timestampData),
                     dest.position());
        dest.flip();
        assertEquals(timestampData, LogUtils.readTimestamp(dest, false));

        // Booleans
        boolean boolData = true;
        dest.clear();
        LogUtils.writeBoolean(dest, boolData);
        assertEquals(1, dest.position());
        dest.flip();
        assertEquals(boolData, LogUtils.readBoolean(dest));

        testPacked(dest);
    }

    private void testPacked(ByteBuffer dest) {

        // packed ints
        int intValue = 119;
        dest.clear();
        LogUtils.writePackedInt(dest, intValue);
        assertEquals(1, dest.position());
        dest.flip();
        assertEquals(intValue, LogUtils.readPackedInt(dest));

        intValue = 0xFFFF + 119;
        dest.clear();
        LogUtils.writePackedInt(dest, intValue);
        assertEquals(3, dest.position());
        dest.flip();
        assertEquals(intValue, LogUtils.readPackedInt(dest));

        intValue = Integer.MAX_VALUE;
        dest.clear();
        LogUtils.writePackedInt(dest, intValue);
        assertEquals(5, dest.position());
        dest.flip();
        assertEquals(intValue, LogUtils.readPackedInt(dest));

        // packed longs
        long longValue = 119;
        dest.clear();
        LogUtils.writePackedLong(dest, longValue);
        assertEquals(1, dest.position());
        dest.flip();
        assertEquals(longValue, LogUtils.readPackedLong(dest));

        longValue = 0xFFFFFFFFL + 119;
        dest.clear();
        LogUtils.writePackedLong(dest, longValue);
        assertEquals(5, dest.position());
        dest.flip();
        assertEquals(longValue, LogUtils.readPackedLong(dest));

        longValue = Long.MAX_VALUE;
        dest.clear();
        LogUtils.writePackedLong(dest, longValue);
        assertEquals(9, dest.position());
        dest.flip();
        assertEquals(longValue, LogUtils.readPackedLong(dest));
    }
}
