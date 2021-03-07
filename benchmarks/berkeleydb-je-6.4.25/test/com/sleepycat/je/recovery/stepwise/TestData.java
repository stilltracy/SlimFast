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

import java.util.Arrays;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.DatabaseEntry;

/**
 * Wrapper class that encapsulates a record in a database used for recovery
 * testing.
 */
public class TestData implements Comparable<TestData> {
    private DatabaseEntry key;
    private DatabaseEntry data;

    public TestData(DatabaseEntry key, DatabaseEntry data) {
        this.key = new DatabaseEntry(key.getData());
        this.data = new DatabaseEntry(data.getData());
    }

    public boolean equals(Object o ) {
        if (this == o)
            return true;
        if (!(o instanceof TestData))
            return false;

        TestData other = (TestData) o;
        if (Arrays.equals(key.getData(), other.key.getData()) &&
            Arrays.equals(data.getData(), other.data.getData())) {
            return true;
        } else
            return false;
    }

    public String toString() {
        return  " k=" + IntegerBinding.entryToInt(key) +
                " d=" + IntegerBinding.entryToInt(data);
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public DatabaseEntry getKey() {
        return key;
    }

    public DatabaseEntry getData() {
        return data;
    }

    /** TODO: do any recovery tests use a custom comparator? */
    @Override
    public int compareTo(TestData o) {
        final int key1 = IntegerBinding.entryToInt(key);
        final int key2 = IntegerBinding.entryToInt(o.key);
        final int keyCmp = Integer.compare(key1, key2);
        if (keyCmp != 0) {
            return keyCmp;
        }
        final int data1 = IntegerBinding.entryToInt(data);
        final int data2 = IntegerBinding.entryToInt(o.data);
        return Integer.compare(data1, data2);
    }
}
