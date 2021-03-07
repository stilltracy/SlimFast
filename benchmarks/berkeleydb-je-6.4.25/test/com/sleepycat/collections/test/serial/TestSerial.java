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
package com.sleepycat.collections.test.serial;

/**
 * @see StoredClassCatalogTest
 * @author Mark Hayes
 */
class TestSerial implements java.io.Serializable {

    static final long serialVersionUID = -3738980000390384920L;

    private int i = 123;
    private TestSerial other;

    // The following field 's' was added after this class was compiled and
    // serialized instances were saved in resource files.  This allows testing
    // that the original stored instances can be deserialized after changing
    // the class.  The serialVersionUID is needed for this according to Java
    // serialization rules, and was generated with the serialver tool.
    //
    private String s = "string";

    TestSerial(TestSerial other) {

        this.other = other;
    }

    TestSerial getOther() {

        return other;
    }

    int getIntField() {

        return i;
    }

    String getStringField() {

        return s; // this returned null before field 's' was added.
    }

    public boolean equals(Object object) {

        try {
            TestSerial o = (TestSerial) object;
            if ((o.other == null) ? (this.other != null)
                                  : (!o.other.equals(this.other))) {
                return false;
            }
            if (this.i != o.i) {
                return false;
            }
            // the following test was not done before field 's' was added
            if ((o.s == null) ? (this.s != null)
                              : (!o.s.equals(this.s))) {
                return false;
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }
}
