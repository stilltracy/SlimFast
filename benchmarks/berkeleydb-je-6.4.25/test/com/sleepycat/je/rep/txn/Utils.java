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
package com.sleepycat.je.rep.txn;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Test data for unit tests in this package.
 *
 * TestData has an id key, populated by a sequence, and a random value data
 * field. Testdata subclasses know whether they are meant to survive the syncup
 * or not, and put themselves in the appropriate test confirmation set. These
 * classes are static because the DPL does not support persistent inner
 * classes.  
 */
class Utils {

    @Entity
    abstract static class TestData {
        @PrimaryKey(sequence="ID")
        private long id;
        private int payload;

        TestData(int payload) {
            this.payload = payload;
        }
        
        TestData() {} // for deserialization

        @Override
        public String toString() {
            return "id=" + id + " payload=" + payload +
                " rollback=" + getRollback();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof TestData) {
                TestData t = (TestData) other;
                return ((t.id == id) && (t.payload==payload));
            }

            return false;
        }

        @Override
        public int hashCode() {
            return (int)(id + payload);
        }

        abstract boolean getRollback();
    }

    /* SavedData was committed, and should persist past rollbacks. */
    @Persistent
    static class SavedData extends TestData {

        SavedData(int payload) {
            super(payload);
        }
        
        @SuppressWarnings("unused")
        private SavedData() {super();} // for deserialization

        boolean getRollback() {
            return false;
        }
    }

    /* RollbackData was uncommitted, and should disappear after rollbacks. */
    @Persistent
    static class RollbackData extends TestData {

        RollbackData(int payload) {
            super(payload);
        }
        
        @SuppressWarnings("unused")
        private RollbackData() {super();} // for deserialization

        boolean getRollback() {
            return true;
        }     
    }
}
