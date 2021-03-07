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

import java.io.ObjectStreamClass;
import java.math.BigInteger;

import com.sleepycat.bind.serial.ClassCatalog;

/**
 * NullCatalog is a dummy Catalog implementation that simply
 * returns large (8 byte) class IDs so that ObjectOutput
 * can be simulated when computing a serialized size.
 *
 * @author Mark Hayes
 */
class NullClassCatalog implements ClassCatalog {

    private long id = Long.MAX_VALUE;

    public void close() {
    }

    public byte[] getClassID(ObjectStreamClass classFormat) {
        return BigInteger.valueOf(id--).toByteArray();
    }

    public ObjectStreamClass getClassFormat(byte[] classID) {
        return null; // ObjectInput not supported
    }

    public ClassLoader getClassLoader() {
        return null;
    }
}
