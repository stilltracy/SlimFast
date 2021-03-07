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

package com.sleepycat.je.rep.util.ldiff;

import org.junit.Test;

import com.sleepycat.util.test.TestBase;
import com.sleepycat.utilint.StringUtils;

public class LDiffUtilTest extends TestBase {
    byte[][] al = new byte[][] {
            StringUtils.toUTF8("key1|Value1"),
            StringUtils.toUTF8("key2|Value2"),
            StringUtils.toUTF8("key3|Value3"),
            StringUtils.toUTF8("key4|Value4"),
            StringUtils.toUTF8("key5|Value5"),
            StringUtils.toUTF8("key6|Value6"),
            StringUtils.toUTF8("key7|Value7"),
            StringUtils.toUTF8("key8|Value8"),
            StringUtils.toUTF8("key9|Value9"),
            StringUtils.toUTF8("key10|Value10") };

    @Test
    public void testPlaceHolder() {
        /* 
         * A Junit test will fail if there are no tests cases at all, so
         * here is a placeholder test.
         */
    }

    /* Verifies the basics of the rolling checksum computation. */
    /*
     * public void testgetRollingChksum() { List<byte[]> tlist =
     * Arrays.asList(al); int blockSize = 5; long rsum =
     * LDiffUtil.getRollingChksum(tlist.subList(0, blockSize)); for (int i = 1;
     * (i + blockSize) <= tlist.size(); i++) { int removeIndex = i - 1; int
     * addIndex = removeIndex + blockSize; List<byte[]> list =
     * tlist.subList(removeIndex + 1, addIndex + 1); // The reference value.
     * long ref = LDiffUtil.getRollingChksum(list); // The incrementally
     * computed chksum rsum = LDiffUtil.rollChecksum(rsum, blockSize,
     * LDiffUtil.getXi(al[removeIndex]), LDiffUtil.getXi(al[addIndex]));
     * assertEquals(ref, rsum); // System.err.printf("ref:%x, rsum:%x\n", ref,
     * rsum); } }
     */
}
