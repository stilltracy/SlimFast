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

public class TruncateTest extends com.sleepycat.je.TruncateTest {

    // TODO: relies on exact standalone LN counts. Rep introduces additional
    // LNs.
    @Override
    public void testEnvTruncateAbort() {
    }

    @Override
    public void testEnvTruncateCommit() {
    }

    @Override
    public void testEnvTruncateAutocommit() {
    }

    @Override
    public void testEnvTruncateNoFirstInsert() {
    }

    // Skip since it's non-transactional
    @Override
    public void testNoTxnEnvTruncateCommit() {
    }

    @Override
    public void testTruncateCommit() {
    }

    @Override
    public void testTruncateCommitAutoTxn() {
    }

    @Override
    public void testTruncateEmptyDeferredWriteDatabase() {
    }

    // TODO: Complex setup -- replicators not shutdown resulting in an
    // attempt to rebind to an already bound socket address
    @Override
    public void testTruncateAfterRecovery() {
    }

    /* Non-transactional access is not supported by HA. */
    @Override
    public void testTruncateNoLocking() {
    }

    /* Calls EnvironmentImpl.abnormalShutdown. */
    @Override
    public void testTruncateRecoveryWithoutMapLNDeletion()
        throws Throwable {
    }

    /* Calls EnvironmentImpl.abnormalShutdown. */
    @Override
    public void testTruncateRecoveryWithoutMapLNDeletionNonTxnal()
        throws Throwable {
    }

    /* Calls EnvironmentImpl.abnormalShutdown. */
    @Override
    public void testRemoveRecoveryWithoutMapLNDeletion()
        throws Throwable {
    }

    /* Calls EnvironmentImpl.abnormalShutdown. */
    @Override
    public void testRemoveRecoveryWithoutMapLNDeletionNonTxnal()
        throws Throwable {
    }

    /* Calls EnvironmentImpl.abnormalShutdown. */
    @Override
    public void testRecoveryRenameMapLNDeletion()
        throws Throwable {
    }
}
