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

import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;
import static com.sleepycat.persist.model.Relationship.ONE_TO_ONE;

import com.sleepycat.je.CursorConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
class RepTestData {
    @SuppressWarnings("unused")
    @PrimaryKey(sequence="KEY")
    private int key;

    @SuppressWarnings("unused")
    @SecondaryKey(relate=ONE_TO_ONE)
    private int data;

    @SuppressWarnings("unused")
    @SecondaryKey(relate=MANY_TO_ONE)
    private String name;

    public void setKey(int key) {
        this.key = key;
    }

    public void setData(int data) {
        this.data = data;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getKey() {
        return key;
    }

    public int getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public boolean logicEquals(RepTestData object, int distance) {
        if (object == null) {
            return false;
        }

        if (key == (object.getKey() + distance) &&
            data == (object.getData() + distance) &&
            name.equals(object.getName())) {
            return true;
        }
        
        return false;
    }

    public String toString() {
        return "Instance: key = " + key + ", data = " + data +
               ", name = " + name;
    }

    /* Insert dbSize records to the specified EntityStore. */
    public static void insertData(EntityStore dbStore, int dbSize) 
        throws Exception {

        insertData(dbStore, dbSize, false);
    }

    /* 
     * Insert dbSize reocrds to the specified EntityStore. 
     *
     * If useFixedName is true, the name field will be assigned a fixed string,
     * to make the assertion in ReplicaReading works corrrectly.
     */
    public static void insertData(EntityStore dbStore, 
                                  int dbSize, 
                                  boolean useFixedName) 
        throws Exception {

        PrimaryIndex<Integer, RepTestData> primaryIndex =
            dbStore.getPrimaryIndex(Integer.class, RepTestData.class);
        for (int i = 1; i <= dbSize; i++) {
            RepTestData data = new RepTestData();
            data.setData(i);
            data.setName(useFixedName ? "test" : generateNameField(i));
            primaryIndex.put(data);
        }
        System.out.println("num unique keys in names field = " +
                           countUniqueNames(dbStore, primaryIndex));
        dbStore.close();
    }


    /*
     * Vary the name field has a many-one secondary key. We will manipulate
     * this data in order to create a duplicates data base with both single
     * keys, and keys that have duplicate trees, to create more stress.
     */
    public static String generateNameField(int index) {
        if ((index % 5) == 0) {
            return "testSingle_" + index;  // singleton keys
        } else if ((index % 7) == 0) {
            return "test_7";  // dup tree of key=test7
        } else if ((index % 4) == 0) {
            return "test_4"; // dup tree of key=test4
        } else if ((index % 3) == 0) {
            return "test_3"; // dup tree of key=test3
        } else if ((index % 2) == 0) {
            return "test_2"; // dup tree of key=test2
        } else {
            return "testSingle_" + index;  // singleton keys
        }
    }

    /**
     * Count the unique number of names in a entity store of RepTestData, in
     * order to validate that there is a reasonable distribution of duplicate
     * values.
     */
    public static int 
        countUniqueNames(EntityStore store,
                         PrimaryIndex<Integer, RepTestData> primary) {
        
        SecondaryIndex<String, Integer, RepTestData> recordByName =
            store.getSecondaryIndex(primary, String.class, "name");
        EntityCursor<String> nameVals = 
            recordByName.keys(null, CursorConfig.READ_UNCOMMITTED);
        int numUnique = 0;
        while (nameVals.nextNoDup() != null) {
            numUnique++;
        
        }
        nameVals.close();
        return numUnique;
    }
}
