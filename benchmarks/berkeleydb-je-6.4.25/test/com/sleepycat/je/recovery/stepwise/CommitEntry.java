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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/*
 * A Commit entry signals that some records should be moved from the
 * not-yet-committed sets to the expected set.
 */
public class CommitEntry extends LogEntryInfo {
    private long txnId;

    CommitEntry(long lsn, long txnId) {
        super(lsn, 0, 0);
        this.txnId = txnId;
    }

    @Override
    public void updateExpectedSet
        (Set<TestData>  useExpected,
         Map<Long, Set<TestData>> newUncommittedRecords,
         Map<Long, Set<TestData>> deletedUncommittedRecords) {

        Long mapKey = new Long(txnId);

        /* Add any new records to the expected set. */
        Set<TestData> records = newUncommittedRecords.get(mapKey);
        if (records != null) {
            Iterator<TestData> iter = records.iterator();
            while (iter.hasNext()) {
                useExpected.add(iter.next());
            }
        }

        /* Remove any deleted records from expected set. */
        records = deletedUncommittedRecords.get(mapKey);
        if (records != null) {
            Iterator<TestData> iter = records.iterator();
            while (iter.hasNext()) {
                useExpected.remove(iter.next());
            }
        }
    }
}
