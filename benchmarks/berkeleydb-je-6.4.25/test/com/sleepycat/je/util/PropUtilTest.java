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

package com.sleepycat.je.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.sleepycat.je.utilint.PropUtil;
import com.sleepycat.util.test.TestBase;

public class PropUtilTest extends TestBase {

    private static long NS_TO_MICRO = 1000;
    private static long NS_TO_MILLI = 1000000;
    private static long NS_TO_SECOND = 1000000000;
    private static long NS_TO_MINUTE = 60 * NS_TO_SECOND;
    private static long NS_TO_HOUR = 60 * NS_TO_MINUTE;
    private static long MS_TO_SECOND = 1000;
    private static long MS_TO_MINUTE = 60 * MS_TO_SECOND;
    private static long MS_TO_HOUR = 60 * MS_TO_MINUTE;


    @Test
    public void testDurationToMillis() {

        /* Disallow negative values. */
        try {
            PropUtil.durationToMillis(-1, TimeUnit.SECONDS);
        } catch (IllegalArgumentException expected) {
        }

        /* Disallow millis > Integer.MAX_VALUE. */
        try {
            PropUtil.durationToMillis(((long) Integer.MAX_VALUE) + 1,
                                      TimeUnit.MILLISECONDS);
        } catch (IllegalArgumentException expected) {
        }

        /* Disallow null unit with non-zero time. */
        try {
            PropUtil.durationToMillis(1, null);
        } catch (IllegalArgumentException expected) {
        }

        /* Allow null unit with zero time. */
        assertEquals(0, PropUtil.durationToMillis(0, null));

        /* Positive input should result in at least 1 ms. */
        assertEquals(1, PropUtil.durationToMillis(1, TimeUnit.MICROSECONDS));
        assertEquals(1, PropUtil.durationToMillis(1, TimeUnit.NANOSECONDS));

        /* Misc conversions. */
        assertEquals(0, PropUtil.durationToMillis(0, TimeUnit.SECONDS));
        assertEquals(1, PropUtil.durationToMillis(1, TimeUnit.MILLISECONDS));
        assertEquals(1, PropUtil.durationToMillis(999, TimeUnit.MICROSECONDS));
        assertEquals(1, PropUtil.durationToMillis(1000, TimeUnit.MICROSECONDS));
        assertEquals(1, PropUtil.durationToMillis(1001, TimeUnit.MICROSECONDS));
        assertEquals(1, PropUtil.durationToMillis(1999, TimeUnit.MICROSECONDS));
        assertEquals(2, PropUtil.durationToMillis(2000000,
                                                  TimeUnit.NANOSECONDS));
    }

    @Test
    public void testMillisToDuration() {

        /* Disallow null unit. */
        try {
            PropUtil.millisToDuration(0, null);
        } catch (IllegalArgumentException expected) {
        }

        /* Misc conversions. */
        assertEquals(0, PropUtil.millisToDuration(0, TimeUnit.SECONDS));
        assertEquals(1, PropUtil.millisToDuration(1000, TimeUnit.SECONDS));
    }

    @Test
    public void testParseDuration() {

        /* parse value, value in ns, value in ms */
        String[][][] testvals =
            {{{"1 ns", "1", "1"}},
             {{"1" , Long.toString(NS_TO_MICRO), "1"}},
             {{"1 us" , "1000", "1"}},
             {{"1 ms", "1000000", "1"}},
             {{"1 nanoseconds", "1", "1"}},
             {{"1 microseconds", "1000", "1"}},
             /* TimeUnitNames */
             {{"3000000 nanoseconds", "3000000", "3"}},
             {{"3000 microseconds", Long.toString(NS_TO_MICRO * 3000), "3"}},
             {{"3 milliseconds", Long.toString(NS_TO_MILLI * 3), "3"}},
             {{"3 seconds", Long.toString(NS_TO_SECOND * 3), "3000"}},
             /* IEEE abbreviations */
             {{"3000000 NS", "3000000", "3"}},
             {{"3000 US", Long.toString(NS_TO_MICRO * 3000), "3"}},
             {{"3 MS", Long.toString(NS_TO_MILLI * 3), "3"}},
             {{"3 S", Long.toString(NS_TO_SECOND * 3), Long.toString(MS_TO_SECOND * 3)}},
             {{"3 MIN", Long.toString(NS_TO_MINUTE * 3), Long.toString(MS_TO_MINUTE * 3)}},
             {{"3 H", Long.toString(NS_TO_HOUR * 3), Long.toString(MS_TO_HOUR * 3)}},
             {{"1 s", Long.toString(NS_TO_SECOND),
               Long.toString(MS_TO_SECOND)}},
             {{"1 min", Long.toString(NS_TO_MINUTE),
               Long.toString(MS_TO_MINUTE)}},
             {{"1 h", Long.toString(NS_TO_HOUR), Long.toString(MS_TO_HOUR)}},
             /* maximum 32 bit for ms*/
             {{"2147483647 ms", Long.toString(2147483647L * NS_TO_MILLI),
               "2147483647"}},
               /* maximum 32 bit ns*/
             {{"596 h", Long.toString(596 * NS_TO_HOUR),
               Long.toString(596 * MS_TO_HOUR)}}};

        String[][][] exceeds32BitMillis =
            {{{"2147483648 ms", Long.toString(2147483648L * NS_TO_MILLI),
               "2147483648"}},
             {{"597 h", Long.toString(597 * NS_TO_HOUR),
               Long.toString(597 * MS_TO_HOUR)}}
            };


        /* Disallow empty string. */
        try {
            PropUtil.parseDuration("");
        } catch (IllegalArgumentException expected) {
        }

        /* Disallow whitespace. */
        try {
            PropUtil.parseDuration(" \t");
        } catch (IllegalArgumentException expected) {
        }

        /* Disallow bad number. */
        try {
            PropUtil.parseDuration("X");
        } catch (IllegalArgumentException expected) {
        }

        /* Disallow bad number with unit. */
        try {
            PropUtil.parseDuration("X ms");
        } catch (IllegalArgumentException expected) {
        }

        /* Disallow bad unit. */
        try {
            PropUtil.parseDuration("3 X");
        } catch (IllegalArgumentException expected) {
        }

        /* Disallow extra stuff after unit. */
        try {
            PropUtil.parseDuration("3 ms X");
        } catch (IllegalArgumentException expected) {
        }

        /* Disallow negative number. */
        try {
            PropUtil.parseDuration("-1");
        } catch (IllegalArgumentException expected) {
        }

        /* Disallow negative number with unit. */
        try {
            PropUtil.parseDuration("-1 ms");
        } catch (IllegalArgumentException expected) {
        }

        for (String[][] val : testvals) {
            long valueNano = PropUtil.parseDurationNS(val[0][0]);
            assertTrue("expected " + val[0][1] + " got "+ val[0][0],
                       valueNano == Long.valueOf(val[0][1]));

            int valueMillis = PropUtil.parseDuration(val[0][0]);
            assertTrue("expected " + val[0][2] + " got "+ val[0][0],
                    valueMillis == Long.valueOf(val[0][2]));
        }

        for (String[][] val : exceeds32BitMillis) {
            try {
                long valueNano = PropUtil.parseDurationNS(val[0][0]);
                assertTrue("expected " + val[0][1] + " got "+ val[0][0],
                           valueNano == Long.valueOf(val[0][1]));

                int valueMillis = PropUtil.parseDuration(val[0][0]);
                fail("Exception not generated for value exceeding maximum.");
            } catch (Exception e) {
                // ignore expected exception.
            }
        }
    }

    @Test
    public void testFormatDuration() {
        assertEquals("30 NANOSECONDS",
                     PropUtil.formatDuration(30, TimeUnit.NANOSECONDS));
        assertEquals("30 MICROSECONDS",
                     PropUtil.formatDuration(30, TimeUnit.MICROSECONDS));
        assertEquals("30 MILLISECONDS",
                     PropUtil.formatDuration(30, TimeUnit.MILLISECONDS));
        assertEquals("30 SECONDS",
                     PropUtil.formatDuration(30, TimeUnit.SECONDS));
    }
}
