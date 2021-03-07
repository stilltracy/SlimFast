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

import java.util.Map;
import java.util.Set;

import com.sleepycat.je.utilint.DbLsn;

/*
 * A LogEntryInfo supports stepwise recovery testing, where the log is
 * systematically truncated and recovery is executed. At each point in a log,
 * there is a set of records that we expect to see. The LogEntryInfo
 * encapsulates enough information about the current log entry so we can
 * update the expected set accordingly.
 */

public class LogEntryInfo {
    private long lsn;
    int key;
    int data;

    LogEntryInfo(long lsn,
              int key,
              int data) {
        this.lsn = lsn;
        this.key = key;
        this.data = data;
    }

    /*
     * Implement this accordingly. For example, a LogEntryInfo which
     * represents a non-txnal LN record would add that key/data to the
     * expected set. A txnal delete LN record would delete the record
     * from the expecte set at commit.
     *
     * The default action is that the expected set is not changed.
     */
    public void updateExpectedSet
        (Set<TestData> expectedSet, 
         Map<Long, Set<TestData>> newUncommittedRecords, 
         Map<Long, Set<TestData>> deletedUncommittedRecords) {}

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(this.getClass().getName());
        sb.append("lsn=").append(DbLsn.getNoFormatString(lsn));
        sb.append(" key=").append(key);
        sb.append(" data=").append(data);
        return sb.toString();
    }

    public long getLsn() {
        return lsn;
    }
}
