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


public class TxnTimeoutTest extends com.sleepycat.je.txn.TxnTimeoutTest {

    /* 
     * The following unit tests are excluded because they intentionally 
     * provoked to exceptions and handles those accordingly. The special 
     * handing is not available on the replica side, and would cause a replica
     * failure.
     */
    @Override
    public void testTxnTimeout() {
    }
    
    @Override
    public void testPerTxnTimeout() {
    }

    @Override
    public void testEnvTxnTimeout() {
    }

    @Override
    public void testEnvNoLockTimeout() {
    }

    @Override
    public void testPerLockTimeout() {
    }

    @Override
    public void testEnvLockTimeout() {
    }

    @Override
    public void testPerLockerTimeout() {
    }

    @Override
    public void testReadCommittedTxnTimeout() {
    }

    @Override
    public void testReadCommittedLockTimeout() {
    }

    @Override
    public void testSerializableTxnTimeout() {
    }

    @Override
    public void testSerializableLockTimeout() {
    }
}
