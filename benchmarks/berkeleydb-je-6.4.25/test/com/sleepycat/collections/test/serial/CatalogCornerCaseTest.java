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

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.compat.DbCompat;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.util.test.SharedTestUtils;
import com.sleepycat.util.test.TestBase;
import com.sleepycat.util.test.TestEnv;

/**
 * @author Mark Hayes
 */
public class CatalogCornerCaseTest extends TestBase {

    private Environment env;

    public CatalogCornerCaseTest() {

        customName = "CatalogCornerCaseTest";
    }

    @Before
    public void setUp()
        throws Exception {

        super.setUp();
        SharedTestUtils.printTestName(customName);
        env = TestEnv.BDB.open(customName);
    }

    @After
    public void tearDown() {

        try {
            if (env != null) {
                env.close();
            }
        } catch (Exception e) {
            System.out.println("Ignored exception during tearDown: " + e);
        } finally {
            /* Ensure that GC can cleanup. */
            env = null;
        }
    }

    @Test
    public void testReadOnlyEmptyCatalog()
        throws Exception {

        String file = "catalog.db";

        /* Create an empty database. */
        DatabaseConfig config = new DatabaseConfig();
        config.setAllowCreate(true);
        DbCompat.setTypeBtree(config);
        Database db =
            DbCompat.testOpenDatabase(env, null, file, null, config);
        db.close();

        /* Open the empty database read-only. */
        config.setAllowCreate(false);
        config.setReadOnly(true);
        db = DbCompat.testOpenDatabase(env, null, file, null, config);

        /* Expect exception when creating the catalog. */
        try {
            new StoredClassCatalog(db);
            fail();
        } catch (RuntimeException e) { }
        db.close();
    }
}
