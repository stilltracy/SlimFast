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
package com.sleepycat.je.rep.impl.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.sleepycat.je.rep.impl.node.RepNode.SecondaryNodeIds;

public class RepNodeTest {

    /** Test the SecondaryNodeIds class. */
    @Test
    public void testSecondaryNodeIds() {
        final int size = 100;
        final SecondaryNodeIds ids = new SecondaryNodeIds(size);
        final Set<Integer> set = new HashSet<Integer>();
        for (int i = 0; i < size; i++) {
            final int id = ids.allocateId();
            assertTrue("Unexpected ID value: " + id,
                       id >= (Integer.MAX_VALUE - size));
            set.add(id);
        }
        assertEquals("Number of unique IDs", 100, set.size());
        try {
            ids.allocateId();
            fail("Expected IllegalStateException when no more IDs");
        } catch (IllegalStateException e) {
        }
        try {
            ids.deallocateId(-1);
            fail("Expected IllegalArgumentException for negative ID");
        } catch (IllegalArgumentException e) {
        }
        try {
            ids.deallocateId(Integer.MAX_VALUE - size - 1);
            fail("Expected IllegalArgumentException for invalid ID");
        } catch (IllegalArgumentException e) {
        }
        for (final int id : new HashSet<Integer>(set)) {
            ids.deallocateId(id);
            final int id2 = ids.allocateId();
            assertEquals("Reallocate same ID", id, id2);
        }
        for (final int id : set) {
            ids.deallocateId(id);
        }
        try {
            ids.deallocateId(Integer.MAX_VALUE);
            fail("Expected IllegalArgumentException for not present ID");
        } catch (IllegalArgumentException e) {
        }
    }
}
