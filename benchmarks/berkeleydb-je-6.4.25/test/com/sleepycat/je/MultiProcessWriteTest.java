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

package com.sleepycat.je;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;

import org.junit.Test;

import com.sleepycat.je.junit.JUnitProcessThread;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;

/**
 * [#16348] JE file handle leak when multi-process writing a same environment.
 *
 * This test would create two process through two threads, one process open
 * the environment in r/w mode, then the other thread tries to get the write
 * lock of the je.lck, to check whether there exists file handle leak.
 */
public class MultiProcessWriteTest extends TestBase {
    private final File envHome;

    public MultiProcessWriteTest() {
        envHome = SharedTestUtils.getTestDir();
    }

    @Test
    public void testMultiEnvWrite() {
        /* Initiate the environment. */
        MainWrite.main
            (new String[]{"-envHome", SharedTestUtils.getTestDir().getAbsolutePath(),
                          "-init", "-initSize", "1000"});

        /* Command for process 1. */
        String[] command1 = new String[8];
        command1[0] = "com.sleepycat.je.MultiProcessWriteTest$MainWrite";
        command1[1] = "-envHome";
        command1[2] = SharedTestUtils.getTestDir().getPath();
        command1[3] = "-write";
        command1[4] = "-numOps";
        command1[5] = "100000";
        command1[6] = "-procNum";
        command1[7] = "1";

        /* Command for process 2. */
        String[] jvmParams = { "-Xmx40m" };
        String[] command2 = new String[8];
        command2[0] = command1[0];
        command2[1] = command1[1];
        command2[2] = command1[2];
        command2[3] = command1[3];
        command2[4] = command1[4];
        command2[5] = "200000";
        command2[6] = command1[6];
        command2[7] = "2";

        /* Create and start the two threads. */
        JUnitProcessThread thread1 =
            new JUnitProcessThread("process1", command1);
        JUnitProcessThread thread2 =
            new JUnitProcessThread("process2", jvmParams, command2);

        thread1.start();

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        thread2.start();

        /* End these two threads. */
        try {
            thread1.finishTest();
            thread2.finishTest();
        } catch (Throwable t) {
            System.err.println(t.toString());
        }

        /* Check whether the process throws out unexpected exceptions. */
        assertEquals(thread1.getExitVal(), 0);
        assertEquals(thread2.getExitVal(), 0);
    }

    /**
     * Write records into the environment.
     *
     * It can run initialization and run in process 1 mode, process 2 mode
     * as specified.
     */
    static class MainWrite {
        private static final int CACHE_LIMIT = 50000;
        private int procNum;

        private final File envHome;
        private ArrayList<TestObject> objectList = new ArrayList<TestObject>();

        private PrimaryIndex<String, TestObject> objectBySid;
        private Environment env;
        private EntityStore store;

        public MainWrite(File envHome) {
            this.envHome = envHome;
        }

        public void setProcNum(int procNum) {
            this.procNum = procNum;
        }

        public boolean setup(boolean readOnly) {
            boolean open = false;
            try {
                EnvironmentConfig envConfig = new EnvironmentConfig();
                envConfig.setReadOnly(readOnly);
                envConfig.setAllowCreate(!readOnly);

                env = new Environment(envHome, envConfig);

                StoreConfig storeConfig = new StoreConfig();
                storeConfig.setReadOnly(readOnly);
                storeConfig.setAllowCreate(!readOnly);

                store = new EntityStore(env, "EntityStore", storeConfig);

                objectBySid =
                    store.getPrimaryIndex(String.class, TestObject.class);

                open = true;
            } catch (EnvironmentLockedException e) {
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }

            return open;
        }

        public void init(int initSize) {
            putNoTry(initSize);
        }

        public void putNoTry(int numOps) {
            setup(false);
            try {
                for (int i = 0; i < numOps; i++) {
                    TestObject object = new TestObject();
                    String sId = new Integer(i).toString();
                    object.setSid(sId);
                    object.setName("hero" + sId);
                    object.setCountry("China");
                    objectBySid.putNoReturn(object);
                }
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
        }

        public void putWithTry(int numOps) {
            try {
                setup(true);
                for (int i = 0; i < numOps; i++) {
                    TestObject object = new TestObject();
                    String s = new Integer(i).toString();
                    object.setSid(s);
                    object.setName("hero" + s);
                    object.setCountry("China");

                    objectList.add(object);

                    if (objectList.size() >= CACHE_LIMIT) {
                        close();

                        boolean success = false;
                        while (!success) {
                            success = setup(false);
                        }

                        for (int j = 0; j < objectList.size(); j++) {
                            objectBySid.putNoReturn(objectList.get(j));
                        }

                        close();
                        setup(true);
                        objectList = new ArrayList<TestObject>();
                    }
                }
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            if (store != null) {
                try {
                    store.close();
                    store = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (env != null) {
                try {
                    env.close();
                    env = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public static void usage() {
            System.out.println("java MainWrite -envHome <home> " +
                               "-init|-write -numOps <Integer> -procNum <1|2>");
            System.exit(-1);
        }

        public static void main(String args[]) {
            if (!args[0].equals("-envHome")) {
                usage();
            }

            MainWrite test = new MainWrite(new File(args[1]));

            if (args[2].equals("-init")) {
                if (!args[3].equals("-initSize")) {
                    usage();
                } else {
                    test.init(new Integer(args[4]));
                    test.close();
                }
            } else if (args[2].equals("-write")) {
                if (!args[3].equals("-numOps")) {
                    usage();
                } else {
                    if (!(args[6].equals("1") ||
                        args[6].equals("2"))) {
                        usage();
                    }

                    test.setProcNum(new Integer(args[6]));

                    if (args[6].equals("1"))
                        test.putNoTry(new Integer(args[4]));
                    else
                        test.putWithTry(new Integer(args[4]));

                    test.close();
                }
            } else {
                usage();
            }
        }
    }

    @Entity
    static class TestObject {
        @PrimaryKey
        private String sid;

        private String name;

        private String country;

        public void setSid(String sid) {
            this.sid = sid;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getSid() {
            return sid;
        }

        public String getName() {
            return name;
        }

        public String getCountry() {
            return country;
        }
    }
}
