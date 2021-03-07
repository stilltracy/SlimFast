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

package com.sleepycat.je.utilint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.sleepycat.util.test.TestBase;

public class BitMapTest extends TestBase {

    @Test
    public void testSegments() {

        BitMap bmap = new BitMap();
        int startBit = 15;
        int endBit = 62;
        assertEquals(0, bmap.cardinality());
        assertEquals(0, bmap.getNumSegments());

        assertFalse(bmap.get(1001L));
        assertEquals(0, bmap.getNumSegments());

        /* set a bit in different segments. */
        for (int i = startBit; i <= endBit; i++) {
            long index = 1L << i;
            index += 17;
            bmap.set(index);
        }

        assertEquals((endBit - startBit +1), bmap.cardinality());
        assertEquals((endBit - startBit + 1), bmap.getNumSegments());

        /* should be set. */
        for (int i = startBit; i <= endBit; i++) {
            long index = 1L << i;
            index += 17;
            assertTrue(bmap.get(index));
        }

        /* should be clear. */
        for (int i = startBit; i <= endBit; i++) {
            long index = 7 + (1L << i);
            assertFalse(bmap.get(index));
        }

        /* checking for non-set bits should not create more segments. */
        assertEquals((endBit - startBit +1), bmap.cardinality());
        assertEquals((endBit - startBit + 1), bmap.getNumSegments());
    }

    @Test
    public void testNegative() {
        BitMap bMap = new BitMap();

        try {
            bMap.set(-300);
            fail("should have thrown exception");
        } catch (IndexOutOfBoundsException expected) {
        }
    }
}
