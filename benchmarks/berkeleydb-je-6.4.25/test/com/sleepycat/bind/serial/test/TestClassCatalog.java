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
import java.util.HashMap;
import java.util.Map;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;

/**
 * @author Mark Hayes
 */
public class TestClassCatalog implements ClassCatalog {

    private final Map<Integer, ObjectStreamClass> idToDescMap =
        new HashMap<Integer, ObjectStreamClass>();
    private final Map<String, Integer> nameToIdMap =
        new HashMap<String, Integer>();
    private int nextId = 1;

    public TestClassCatalog() {
    }

    public void close() {
    }

    public synchronized byte[] getClassID(ObjectStreamClass desc) {
        String className = desc.getName();
        Integer intId = nameToIdMap.get(className);
        if (intId == null) {
            intId = nextId;
            nextId += 1;

            idToDescMap.put(intId, desc);
            nameToIdMap.put(className, intId);
        }
        DatabaseEntry entry = new DatabaseEntry();
        IntegerBinding.intToEntry(intId, entry);
        return entry.getData();
    }

    public synchronized ObjectStreamClass getClassFormat(byte[] byteId)
        throws DatabaseException {

        DatabaseEntry entry = new DatabaseEntry();
        entry.setData(byteId);
        int intId = IntegerBinding.entryToInt(entry);

        ObjectStreamClass desc = (ObjectStreamClass) idToDescMap.get(intId);
        if (desc == null) {
            throw new RuntimeException("classID not found");
        }
        return desc;
    }

    public ClassLoader getClassLoader() {
        return null;
    }
}
