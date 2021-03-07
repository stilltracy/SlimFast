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

package com.sleepycat.je.evictor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import com.sleepycat.util.test.TestBase;
import org.junit.Test;

/**
 */
public class OffHeapAllocatorTest extends TestBase {

    @Test
    public void testBasic() throws Exception {

        final OffHeapAllocatorFactory factory = new OffHeapAllocatorFactory();
        final OffHeapAllocator allocator = factory.getDefaultAllocator();

        long memId = allocator.allocate(100);
        assertTrue(memId != 0);
        assertEquals(100, allocator.size(memId));

        byte[] buf = new byte[100];
        byte[] buf2 = new byte[100];

        Arrays.fill(buf, (byte) 1);
        allocator.copy(memId, 0, buf, 0, 100);
        Arrays.fill(buf2, (byte) 0);
        assertTrue(Arrays.equals(buf, buf2));

        Arrays.fill(buf, (byte) 1);
        allocator.copy(buf, 0, memId, 0, 100);
        Arrays.fill(buf2, (byte) 0);
        allocator.copy(memId, 0, buf2, 0, 100);
        assertTrue(Arrays.equals(buf, buf2));

        allocator.free(memId);
    }
}
