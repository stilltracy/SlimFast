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

package com.sleepycat.je.rep.dual;

public class EnvironmentTest extends com.sleepycat.je.EnvironmentTest {

    /* In following test cases, Environment is set non-transactional. */
    @Override
    public void testReadOnly() {
    }

    @Override
    public void testTransactional() {
    }

    @Override
    public void testOpenWithoutCheckpoint() {
    }

    @Override
    public void testMutableConfig() {
    }

    @Override
    public void testTxnDeadlockStackTrace() {
    }

    @Override
    public void testParamLoading() {
    }

    @Override
    public void testConfig() {
    }

    @Override
    public void testGetDatabaseNames() {
    }

    @Override
    public void testDaemonRunPause() {
    }

    /* In following test cases, Database is set non-transactional. */
    @Override
    public void testDbRenameCommit() {
    }

    @Override
    public void testDbRenameAbort() {
    }

    @Override
    public void testDbRemove() {
    }

    @Override
    public void testDbRemoveReadCommitted() {
    }

    @Override
    public void testDbRemoveNonTxnl() {
    }

    @Override
    public void testDbRemoveCommit() {
    }

    /*
     * In following two test cases, they try to reclose a same environment or
     * database. This behavior would fail, override it for now,
     * may be fixed in the future.
    */
    @Override
    public void testClose() {
    }

    @Override
    public void testExceptions() {
    }

    /*
     * This test case tries to open two environments on a same directory,
     * it would fail, override it for now, may be fixed in the future.
     */
    @Override
    public void testReferenceCounting() {
    }

    /*
     * This test opens an environment read-only so this is not applicable
     * to ReplicatedEnvironments (which can not be opened read-only).
     */
    @Override
    public void testReadOnlyDbNameOps() {
    }

    /* HA doesn't support in memory only environment. */
    @Override
    public void testMemOnly() {
    }
}
