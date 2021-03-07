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

package com.sleepycat.je.jmx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import junit.framework.TestCase;

import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.util.TestUtils;

/**
 * A utility class for testing JE MBeans.
 */
public class MBeanTestUtils extends TestCase {
    public static void validateGetters(DynamicMBean mbean,
                                       int numExpectedAttributes,
                                       boolean DEBUG)
        throws Throwable {

        MBeanInfo info = mbean.getMBeanInfo();

        MBeanAttributeInfo[] attrs = info.getAttributes();

        /* test getters. */
        int attributesWithValues = 0;
        for (int i = 0; i < attrs.length; i++) {
            String name = attrs[i].getName();
            Object result = mbean.getAttribute(name);
            if (DEBUG) {
                System.out.println("Attribute " + i +
                                   " name=" + name +
                                   " result=" + result);
            }
            if (result != null) {
                attributesWithValues++;
                checkObjectType
                    ("Attribute", name, attrs[i].getType(), result);
            }
        }

        assertEquals(numExpectedAttributes, attributesWithValues);
    }

    /*
     * Check that there are the expected number of operations.
     */
    public static void checkOpNum(DynamicMBean mbean,
                                  int numExpectedOperations,
                                  boolean DEBUG)
        throws Throwable {

        MBeanInfo info = mbean.getMBeanInfo();

        MBeanOperationInfo[] ops = info.getOperations();
        if (DEBUG) {
            for (int i = 0; i < ops.length; i++) {
                System.out.println("op: " + ops[i].getName());
            }
        }
        assertEquals(numExpectedOperations, ops.length);
    }

    /**
     * Checks that all types for the given mbean are serializable.
     */
    public static void doTestSerializable(DynamicMBean mbean) {

        MBeanInfo info = mbean.getMBeanInfo();

        MBeanAttributeInfo[] attrs = info.getAttributes();
        for (int i = 0; i < attrs.length; i++) {
            checkSerializable
                ("Attribute", attrs[i].getName(), attrs[i].getType());
        }

        MBeanOperationInfo[] ops = info.getOperations();
        for (int i = 0; i < ops.length; i += 1) {
            checkSerializable
                ("Operation",
                 ops[i].getName() + " return type",
                 ops[i].getReturnType());
            MBeanParameterInfo[] params = ops[i].getSignature();
            for (int j = 0; j < params.length; j += 1) {
                checkSerializable
                    ("Operation",
                     ops[i].getName() + " parameter " + j,
                     params[j].getType());
            }
        }

        MBeanConstructorInfo[] ctors = info.getConstructors();
        for (int i = 0; i < ctors.length; i++) {
            MBeanParameterInfo[] params = ctors[i].getSignature();
            for (int j = 0; j < params.length; j += 1) {
                checkSerializable
                    ("Constructor",
                     ctors[i].getName() + " parameter " + j,
                     params[j].getType());
            }
        }

        MBeanNotificationInfo[] notifs = info.getNotifications();
        for (int i = 0; i < notifs.length; i++) {
            String[] types = notifs[i].getNotifTypes();
            for (int j = 0; j < types.length; j += 1) {
                checkSerializable
                    ("Notification", notifs[i].getName(), types[j]);
            }
        }
    }

    /**
     * Checks that a given type is serializable.
     */
    private static void checkSerializable(String identifier,
                                          String name,
                                          String type) {

        if ("void".equals(type)) {
            return;
        }

        String msg = identifier + ' ' + name + " is type " + type;
        try {
            Class cls = Class.forName(type);
            if (!Serializable.class.isAssignableFrom(cls)) {
                fail(msg + " -- not Serializable");
            }
        } catch (Exception e) {
            fail(msg + " -- " + e);
        }
    }

    /**
     * Checks that an object (parameter or return value) is of the type
     * specified in the BeanInfo.
     */
    public static void checkObjectType(String identifier,
                                       String name,
                                       String type,
                                       Object object) {
        String msg = identifier + ' ' + name + " is type " + type;
        if ("void".equals(type)) {
            assertNull(msg + "-- should be null", object);
            return;
        }
        try {
            Class cls = Class.forName(type);
            assertTrue
                (msg + " -- object class is " + object.getClass().getName(),
                 cls.isAssignableFrom(object.getClass()));
        } catch (Exception e) {
            fail(msg + " -- " + e);
        }

        /*
         * The true test of serializable is to serialize.  This checks the a
         * elements of a list, for example.
         */
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
        } catch (Exception e) {
            fail(msg + " -- " + e);
        }
    }

    public static void checkForNoOpenHandles(String environmentDir) {
        File envFile = new File(environmentDir);
        Environment testEnv = DbInternal.getEnvironmentShell(envFile);
        assertTrue(testEnv == null);
    }

    public static Environment openTxnalEnv(boolean isTransactional, 
                                           File envHome) 
        throws Exception {

        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(isTransactional);

        return new Environment(envHome, envConfig);
    }

    /*
     * Check that there are the expected number of operations. If specified, 
     * invoke and check the operations of JEMonitor and JEApplicationMBean.
     */
    public static void validateMBeanOperations(DynamicMBean mbean,
                                              int numExpectedOperations,
                                              boolean tryInvoke,
                                              String databaseName,
                                              String[] expectedDatabases,
                                              boolean DEBUG)
        throws Throwable {

        checkOpNum(mbean, numExpectedOperations, DEBUG);

        MBeanInfo info = mbean.getMBeanInfo();
        MBeanOperationInfo[] ops = info.getOperations();

        if (tryInvoke) {
            for (int i = 0; i < ops.length; i++) {
                String opName = ops[i].getName();

                /* Try the per-database operations if specified. */
                if ((databaseName != null) &&
                    opName.equals(JEMBeanHelper.OP_DB_STAT)) {
                    /* Invoke with the name of the database. */
                    Object result = mbean.invoke
                        (opName,
                         new Object[] {null, null, databaseName},
                         null);
                    assertTrue(result instanceof String);
                    checkObjectType
                        ("Operation", opName, ops[i].getReturnType(), result);
                }

                if ((expectedDatabases != null) &&
                    opName.equals(JEMBeanHelper.OP_DB_NAMES)) {
                    Object result = mbean.invoke(opName, null, null);
                    List names = (List) result;
                    assertTrue(Arrays.equals(expectedDatabases,
                                             names.toArray()));
                    checkObjectType
                        ("Operation", opName, ops[i].getReturnType(), result);
                }

                /*
                 * Also invoke all operations with null params, to sanity
                 * check.
                 */
                Object result = mbean.invoke(opName, null, null);
                if (result != null) {
                    checkObjectType
                        ("Operation", opName, ops[i].getReturnType(), result);
                }
            }
        }
    }
}
