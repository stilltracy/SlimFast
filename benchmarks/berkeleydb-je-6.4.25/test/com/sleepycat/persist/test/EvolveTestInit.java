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
package com.sleepycat.persist.test;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.sleepycat.util.test.SharedTestUtils;

/**
 * Runs part one of the EvolveTest.  This part is run with the old/original
 * version of EvolveClasses in the classpath.  It creates a fresh environment
 * and store containing instances of the original class.  When EvolveTest is
 * run, it will read/write/evolve these objects from the store created here.
 *
 * @author Mark Hayes
 */
public class EvolveTestInit extends EvolveTestBase {

    public EvolveTestInit(String originalClsName, String evolvedClsName)
            throws Exception {
        super(originalClsName, evolvedClsName);
    }

    @Override
    boolean useEvolvedClass() {
        return false;
    }

    @Before
    public void setUp() {

        envHome = getTestInitHome(false /*evolved*/);
        envHome.mkdirs();
        SharedTestUtils.emptyDir(envHome);
    }

    @Test
    public void testInit()
        throws Exception {

        openEnv();
        if (!openStoreReadWrite()) {
            fail();
        }
        caseObj.writeObjects(store);
        caseObj.checkUnevolvedModel(store.getModel(), env);
        closeAll();
    }
}
