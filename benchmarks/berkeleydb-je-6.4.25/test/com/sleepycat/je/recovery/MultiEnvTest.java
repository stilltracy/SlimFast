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

package com.sleepycat.je.recovery;

import java.io.File;

import org.junit.Test;

import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

public class MultiEnvTest extends TestBase {

    private final File envHome1;
    private final File envHome2;

    public MultiEnvTest() {
        envHome1 = SharedTestUtils.getTestDir();
        envHome2 = new File(envHome1,
                            "propTest");
    }

    @Test
    public void testNodeIdsAfterRecovery() {

            /*
             * TODO: replace this test which previously checked that the node
             * id sequence shared among environments was correct with a test
             * that checks all sequences, including replicated ones. This
             * change is appropriate because the node id sequence is no longer
             * a static field.
             */
    }
}
