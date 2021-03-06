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

package com.sleepycat.je.txn;

import java.util.Set;
import java.util.List;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockConflictException;
import com.sleepycat.je.dbi.DatabaseImpl;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.dbi.MemoryBudget;
import com.sleepycat.je.utilint.StatGroup;

/**
 * DummyLockManager performs no locking for DS mode.
 */
public class DummyLockManager extends LockManager {

    /*
     * Even though a user may specify isNoLocking for performance reasons, JE
     * will sometimes still need to use locking internally (e.g. handle locks,
     * and transactional access to internal db's).  So we can not completely
     * eliminate the Lock Manager. Instead, when isNoLocking is specified, we
     * keep a standard Lock Manager around for use by cursors that access
     * internal databases.  Delegate to that as needed.
     * [#16453]
     */
    private final LockManager superiorLockManager;

    public DummyLockManager(EnvironmentImpl envImpl,
                            LockManager superiorLockManager) {
        super(envImpl);
        this.superiorLockManager = superiorLockManager;
    }

    @Override
    public Set<LockInfo> getOwners(Long lsn) {
        return superiorLockManager.getOwners(lsn);
    }

    @Override
    public List<LockInfo> getWaiters(Long lsn) {
        return superiorLockManager.getWaiters(lsn);
    }
    
    @Override
    public LockType getOwnedLockType(Long lsn, Locker locker) {
        return superiorLockManager.getOwnedLockType(lsn, locker);
    }
    
    @Override
    public boolean isLockUncontended(Long lsn) {
        return superiorLockManager.isLockUncontended(lsn);
    }

    /**
     * @see LockManager#lookupLock
     */
    @Override
    Lock lookupLock(Long lsn)
        throws DatabaseException {

        Lock ret = superiorLockManager.lookupLock(lsn);
        return ret;
    }

    /**
     * @see LockManager#attemptLock
     */
    @Override
    LockAttemptResult attemptLock(Long lsn,
                                  Locker locker,
                                  LockType type,
                                  boolean nonBlockingRequest,
                                  boolean jumpAheadOfWaiters)
        throws DatabaseException {

        if (locker.lockingRequired()) {
            return superiorLockManager.attemptLock
                (lsn, locker, type, nonBlockingRequest, jumpAheadOfWaiters);
        }
        return new LockAttemptResult(null, LockGrantType.NEW, true);
    }

    /**
     * @see LockManager#getTimeoutInfo
     */
    @Override
    TimeoutInfo getTimeoutInfo(
        boolean isLockNotTxnTimeout,
        Locker locker,
        long lsn,
        LockType type,
        LockGrantType grantType,
        Lock useLock,
        long timeout,
        long start,
        long now,
        DatabaseImpl database,
        Set<LockInfo> owners,
        List<LockInfo> waiters)
        throws DatabaseException {

        if (locker.lockingRequired()) {
            return superiorLockManager.getTimeoutInfo(
                isLockNotTxnTimeout, locker, lsn, type, grantType, useLock,
                timeout, start, now, database, owners, waiters);
        }
        return null;
    }

    /**
     * @see LockManager#releaseAndFindNotifyTargets
     */
    @Override
    Set<Locker> releaseAndFindNotifyTargets(long lsn, Locker locker)
        throws DatabaseException {

        /*
         * Unconditionally release the lock.  This does not detract from the
         * performance benefit of disabled locking, since this method is only
         * called if a lock was previously acquired, i.e., it is held by a
         * Locker.
         *
         * The comment below is now obsolete because handle locks are no longer
         * transferred.
         *   If the release of the lock were conditional, a lock transferred
         *   between Lockers (as we do with Database handle locks) would never
         *   be released, since the destination Locker's lockingRequired
         *   property is not set to true.  In general, it is safer to
         *   unconditionally release locks than to rely on the lockingRequired
         *   property. [#17985]
         */
        return superiorLockManager.releaseAndFindNotifyTargets(lsn, locker);
    }

    /**
     * @see LockManager#demote
     */
    @Override
    void demote(long lsn, Locker locker)
        throws DatabaseException {

        if (locker.lockingRequired()) {
            superiorLockManager.demote(lsn, locker);
        } else {
            return;
        }
    }

    /**
     * @see LockManager#isLocked
     */
    @Override
    boolean isLocked(Long lsn)
        throws DatabaseException {

        return superiorLockManager.isLocked(lsn);
    }

    /**
     * @see LockManager#isOwner
     */
    @Override
    boolean isOwner(Long lsn, Locker locker, LockType type)
        throws DatabaseException {

        return superiorLockManager.isOwner(lsn, locker, type);
    }

    /**
     * @see LockManager#isWaiter
     */
    @Override
    boolean isWaiter(Long lsn, Locker locker)
        throws DatabaseException {

        return superiorLockManager.isWaiter(lsn, locker);
    }

    /**
     * @see LockManager#nWaiters
     */
    @Override
    int nWaiters(Long lsn)
        throws DatabaseException {

        return superiorLockManager.nWaiters(lsn);
    }

    /**
     * @see LockManager#nOwners
     */
    @Override
    int nOwners(Long lsn)
        throws DatabaseException {

        return superiorLockManager.nOwners(lsn);
    }

    /**
     * @see LockManager#getWriterOwnerLocker
     */
    @Override
    Locker getWriteOwnerLocker(Long lsn)
        throws DatabaseException {

        return superiorLockManager.getWriteOwnerLocker(lsn);
    }

    /**
     * @see LockManager#validateOwnership
     */
    @Override
    boolean validateOwnership(Long lsn,
                              Locker locker,
                              LockType type,
                              boolean flushFromWaiters,
                              MemoryBudget mb,
                              Set<LockInfo> owners,
                              List<LockInfo> waiters)
        throws DatabaseException {

        if (locker.lockingRequired()) {
            return superiorLockManager.validateOwnership
                (lsn, locker, type, flushFromWaiters, mb, owners, waiters);
        }
        return true;
    }

    /**
     * @see LockManager#stealLock
     */
    @Override
    public LockAttemptResult stealLock(Long lsn,
                                          Locker locker,
                                          LockType lockType)
        throws DatabaseException {

        if (locker.lockingRequired()) {
            return superiorLockManager.stealLock
                (lsn, locker, lockType);
        }
        return null;
    }

    /**
     * @see LockManager#dumpLockTable
     */
    @Override
    void dumpLockTable(StatGroup stats, boolean clear)
        throws DatabaseException {

        superiorLockManager.dumpLockTable(stats, clear);
    }
}
