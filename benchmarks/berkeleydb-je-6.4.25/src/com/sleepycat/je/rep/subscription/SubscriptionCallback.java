/*-
 *
 *  This file is part of Oracle NoSQL Database
 *  Copyright (C) 2011, 2015 Oracle and/or its affiliates.  All rights reserved.
 *
 *  Oracle NoSQL Database is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation, version 3.
 *
 *  Oracle NoSQL Database is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public
 *  License in the LICENSE file along with Oracle NoSQL Database.  If not,
 *  see <http://www.gnu.org/licenses/>.
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

package com.sleepycat.je.rep.subscription;

import com.sleepycat.je.utilint.VLSN;

/**
 * Interface of subscription callback function, to be implemented by clients to
 * process each received subscription message.
 */
public interface SubscriptionCallback {

    /**
     * Process a put (insert or update) entry from stream
     *
     * @param vlsn   VLSN of the insert entry
     * @param key    key of the insert entry
     * @param value  value of the insert entry
     * @param txnId  id of txn the entry belongs to
     */
    void processPut(VLSN vlsn, byte[] key, byte[] value, long txnId);

    /**
     * Process a delete entry from stream
     *
     * @param vlsn   VLSN of the delete entry
     * @param key    key of the delete entry
     * @param txnId  id of txn the entry belongs to
     */
    void processDel(VLSN vlsn, byte[] key, long txnId);

    /**
     * Process a commit entry from stream
     *
     * @param vlsn  VLSN of commit entry
     * @param txnId id of txn to commit
     */
    void processCommit(VLSN vlsn, long txnId);

    /**
     * Process an abort entry from stream
     *
     * @param vlsn  VLSN of abort entry
     * @param txnId id of txn to abort
     */
    void processAbort(VLSN vlsn, long txnId);

    /**
     * Process the exception from stream.
     *
     * @param exp  exception raised in service and to be processed by
     *             client
     */
    void processException(final Exception exp);
}
