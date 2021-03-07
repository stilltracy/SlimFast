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

package com.sleepycat.bind.serial.test;

import java.io.Serializable;

import com.sleepycat.bind.tuple.MarshalledTupleKeyEntity;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * @author Mark Hayes
 */
@SuppressWarnings("serial")
public class MarshalledObject
    implements Serializable, MarshalledTupleKeyEntity {

    private String data;
    private transient String primaryKey;
    private String indexKey1;
    private String indexKey2;

    public MarshalledObject(String data, String primaryKey,
                            String indexKey1, String indexKey2) {
        this.data = data;
        this.primaryKey = primaryKey;
        this.indexKey1 = indexKey1;
        this.indexKey2 = indexKey2;
    }

    public boolean equals(Object o) {

        try {
            MarshalledObject other = (MarshalledObject) o;

            return this.data.equals(other.data) &&
                   this.primaryKey.equals(other.primaryKey) &&
                   this.indexKey1.equals(other.indexKey1) &&
                   this.indexKey2.equals(other.indexKey2);
        } catch (Exception e) {
            return false;
        }
    }

    public String getData() {

        return data;
    }

    public String getPrimaryKey() {

        return primaryKey;
    }

    public String getIndexKey1() {

        return indexKey1;
    }

    public String getIndexKey2() {

        return indexKey2;
    }

    public int expectedKeyLength() {

        return primaryKey.length() + 1;
    }

    public void marshalPrimaryKey(TupleOutput keyOutput) {

        keyOutput.writeString(primaryKey);
    }

    public void unmarshalPrimaryKey(TupleInput keyInput) {

        primaryKey = keyInput.readString();
    }

    public boolean marshalSecondaryKey(String keyName, TupleOutput keyOutput) {

        if ("1".equals(keyName)) {
            if (indexKey1.length() > 0) {
                keyOutput.writeString(indexKey1);
                return true;
            } else {
                return false;
            }
        } else if ("2".equals(keyName)) {
            if (indexKey2.length() > 0) {
                keyOutput.writeString(indexKey2);
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException("Unknown keyName: " + keyName);
        }
    }

    public boolean nullifyForeignKey(String keyName) {

        if ("1".equals(keyName)) {
            if (indexKey1.length() > 0) {
                indexKey1 = "";
                return true;
            } else {
                return false;
            }
        } else if ("2".equals(keyName)) {
            if (indexKey2.length() > 0) {
                indexKey2 = "";
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException("Unknown keyName: " + keyName);
        }
    }
}
