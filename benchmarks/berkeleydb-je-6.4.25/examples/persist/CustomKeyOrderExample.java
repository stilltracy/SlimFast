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

package persist;

import java.io.File;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;

public class CustomKeyOrderExample {

    @Entity
    static class Person {

        @PrimaryKey
        ReverseOrder name;

        Person(String name) {
            this.name = new ReverseOrder(name);
        }

        private Person() {} // For deserialization

        @Override
        public String toString() {
            return name.value;
        }
    }

    @Persistent
    static class ReverseOrder implements Comparable<ReverseOrder> {

        @KeyField(1)
        String value;

        ReverseOrder(String value) {
            this.value = value;
        }

        private ReverseOrder() {} // For deserialization

        public int compareTo(ReverseOrder o) {
            return o.value.compareTo(value);
        }
    }

    public static void main(String[] args)
        throws DatabaseException {

        if (args.length != 2 || !"-h".equals(args[0])) {
            System.err.println
                ("Usage: java " + CustomKeyOrderExample.class.getName() +
                 " -h <envHome>");
            System.exit(2);
        }
        CustomKeyOrderExample example =
            new CustomKeyOrderExample(new File(args[1]));
        example.run();
        example.close();
    }

    private Environment env;
    private EntityStore store;

    private CustomKeyOrderExample(File envHome)
        throws DatabaseException {

        /* Open a transactional Berkeley DB engine environment. */
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        env = new Environment(envHome, envConfig);

        /* Open a transactional entity store. */
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        store = new EntityStore(env, "TestStore", storeConfig);
    }

    private void run()
        throws DatabaseException {

        PrimaryIndex<ReverseOrder,Person> index =
            store.getPrimaryIndex(ReverseOrder.class, Person.class);

        index.put(new Person("Andy"));
        index.put(new Person("Lisa"));
        index.put(new Person("Zola"));

        /* Print the entities in key order. */
        EntityCursor<Person> people = index.entities();
        try {
            for (Person person : people) {
                System.out.println(person);
            }
        } finally {
            people.close();
        }
    }

    private void close()
        throws DatabaseException {

        store.close();
        env.close();
    }
}
