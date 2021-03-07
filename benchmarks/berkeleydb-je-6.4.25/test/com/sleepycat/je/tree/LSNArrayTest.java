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

package com.sleepycat.je.tree;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.util.test.TestBase;

public class LSNArrayTest extends TestBase {
    private static final int N_ELTS = 128;

    private IN theIN;

    @Before
    public void setUp() 
        throws Exception {
        
        super.setUp();
        theIN = new IN();
    }

    @Test
    public void testPutGetElement() {
        doTest(N_ELTS);
    }

    @Test
    public void testOverflow() {
        doTest(N_ELTS << 2);
    }

    @Test
    public void testFileOffsetGreaterThan3Bytes() {
        theIN.initEntryLsn(10);
        theIN.setLsnInternal(0, 0xfffffe);
        assertTrue(theIN.getLsn(0) == 0xfffffe);
        assertTrue(theIN.getEntryLsnByteArray() != null);
        assertTrue(theIN.getEntryLsnLongArray() == null);
        theIN.setLsnInternal(1, 0xffffff);
        assertTrue(theIN.getLsn(1) == 0xffffff);
        assertTrue(theIN.getEntryLsnLongArray() != null);
        assertTrue(theIN.getEntryLsnByteArray() == null);

        theIN.initEntryLsn(10);
        theIN.setLsnInternal(0, 0xfffffe);
        assertTrue(theIN.getLsn(0) == 0xfffffe);
        assertTrue(theIN.getEntryLsnByteArray() != null);
        assertTrue(theIN.getEntryLsnLongArray() == null);
        theIN.setLsnInternal(1, 0xffffff + 1);
        assertTrue(theIN.getLsn(1) == 0xffffff + 1);
        assertTrue(theIN.getEntryLsnLongArray() != null);
        assertTrue(theIN.getEntryLsnByteArray() == null);
    }

    private void doTest(int nElts) {
        theIN.initEntryLsn(nElts);
        for (int i = nElts - 1; i >= 0; i--) {
            long thisLsn = DbLsn.makeLsn(i, i);
            theIN.setLsnInternal(i, thisLsn);
            if (theIN.getLsn(i) != thisLsn) {
                System.out.println(i + " found: " +
                                   DbLsn.toString(theIN.getLsn(i)) +
                                   " expected: " +
                                   DbLsn.toString(thisLsn));
            }
            assertTrue(theIN.getLsn(i) == thisLsn);
        }

        for (int i = 0; i < nElts; i++) {
            long thisLsn = DbLsn.makeLsn(i, i);
            theIN.setLsn(i, thisLsn);
            assertTrue(theIN.getLsn(i) == thisLsn);
        }
    }
}
