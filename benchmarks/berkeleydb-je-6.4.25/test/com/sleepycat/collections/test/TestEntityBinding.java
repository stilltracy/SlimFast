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

package com.sleepycat.collections.test;

import com.sleepycat.bind.EntityBinding;
import com.sleepycat.bind.RecordNumberBinding;
import com.sleepycat.je.DatabaseEntry;

/**
 * @author Mark Hayes
 */
class TestEntityBinding implements EntityBinding {

    private boolean isRecNum;

    TestEntityBinding(boolean isRecNum) {

        this.isRecNum = isRecNum;
    }

    public Object entryToObject(DatabaseEntry key, DatabaseEntry value) {

        byte keyByte;
        if (isRecNum) {
            if (key.getSize() != 4) {
                throw new IllegalStateException();
            }
            keyByte = (byte) RecordNumberBinding.entryToRecordNumber(key);
        } else {
            if (key.getSize() != 1) {
                throw new IllegalStateException();
            }
            keyByte = key.getData()[key.getOffset()];
        }
        if (value.getSize() != 1) {
            throw new IllegalStateException();
        }
        byte valByte = value.getData()[value.getOffset()];
        return new TestEntity(keyByte, valByte);
    }

    public void objectToKey(Object object, DatabaseEntry key) {

        byte val = (byte) ((TestEntity) object).key;
        if (isRecNum) {
            RecordNumberBinding.recordNumberToEntry(val, key);
        } else {
            key.setData(new byte[] { val }, 0, 1);
        }
    }

    public void objectToData(Object object, DatabaseEntry value) {

        byte val = (byte) ((TestEntity) object).value;
        value.setData(new byte[] { val }, 0, 1);
    }
}
