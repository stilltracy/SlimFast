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

package com.sleepycat.je.recovery.stepwise;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.DatabaseEntry;

/*
 * A Transactional log entry should add put itself
 * into the not-yet-committed set.
 */

public class TxnalDeletedEntry extends LogEntryInfo {
    private long txnId;

    TxnalDeletedEntry(long lsn,
                      int key,
                      int data,
                      long txnId) {
        super(lsn, key, data);
        this.txnId = txnId;
    }

    /* Implement this accordingly. For example, a LogEntryInfo which
     * represents a non-txnal LN record would add that key/data to the
     * expected set. A txnal delete LN record would delete the record
     * from the expecte set at commit time.
     */
    @Override
    public void updateExpectedSet
        (Set<TestData> useExpected,
         Map<Long, Set<TestData>> newUncommittedRecords,
         Map<Long, Set<TestData>> deletedUncommittedRecords) {

        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry();

        IntegerBinding.intToEntry(key, keyEntry);
        IntegerBinding.intToEntry(data, dataEntry);

        Long mapKey = new Long(txnId);
        Set<TestData> records = deletedUncommittedRecords.get(mapKey);
        if (records == null) {
            records = new HashSet<TestData>();
           deletedUncommittedRecords.put(mapKey, records);
        }
        records.add(new TestData(keyEntry, dataEntry));
    }
}
