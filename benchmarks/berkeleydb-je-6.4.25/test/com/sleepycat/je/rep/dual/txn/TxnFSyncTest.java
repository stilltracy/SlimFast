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
package com.sleepycat.je.rep.dual.txn;

import java.util.List;

import org.junit.runners.Parameterized.Parameters;

import com.sleepycat.util.test.TxnTestCase;

public class TxnFSyncTest extends com.sleepycat.je.txn.TxnFSyncTest {

    // TODO: Low level environment manipulation. Env not being closed. Multiple
    // active environment handles to the same environment.

    public TxnFSyncTest(String type) {
        super(type);
    }
    
    @Parameters
    public static List<Object[]> genParams() {
        return getTxnParams(
            new String[] {TxnTestCase.TXN_USER, TxnTestCase.TXN_AUTO}, true);
    }

    /* junit.framework.AssertionFailedError: Address already in use
        at junit.framework.Assert.fail(Assert.java:47)
        at com.sleepycat.je.rep.RepEnvWrapper.create(RepEnvWrapper.java:60)
        at com.sleepycat.je.DualTestCase.create(DualTestCase.java:63)
        at com.sleepycat.je.txn.TxnFSyncTest.testFSyncButNoClose(TxnFSyncTest.java:105)
        ...

        */
    @Override
    public void testFSyncButNoClose() {
    }
}
