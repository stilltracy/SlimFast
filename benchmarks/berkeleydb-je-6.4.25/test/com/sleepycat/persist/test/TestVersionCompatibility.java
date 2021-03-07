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

import org.junit.Before;
import org.junit.Test;

/**
 * Test that the catalog and data records created with a different version of
 * the DPL are compatible with this version. This test is actually called by 
 * TestVersionCompatibilitySuite, since it contains two parts, firstly to run
 * TestVersionCompatibility tests check previously evolved data without 
 * changing it, then run EvolveTest to try evolving it.
 * 
 * @author Mark Hayes
 */
public class TestVersionCompatibility extends EvolveTestBase {

       
    public TestVersionCompatibility(String originalClsName,
            String evolvedClsName) throws Exception {
        super(originalClsName, evolvedClsName);
    }

    @Override
    boolean useEvolvedClass() {
        return true;
    }

    @Before
    public void setUp() {
        envHome = getTestInitHome(true /*evolved*/);
    }

    @Test
    public void testPreviouslyEvolved()
        throws Exception {

        /* If the store cannot be opened, this test is not appropriate. */
        if (caseObj.getStoreOpenException() != null) {
            return;
        }

        /* The update occurred previously. */
        caseObj.updated = true;

        openEnv();

        /* Open read-only and double check that everything is OK. */
        openStoreReadOnly();
        caseObj.checkEvolvedModel
            (store.getModel(), env, true /*oldTypesExist*/);
        caseObj.readObjects(store, false /*doUpdate*/);
        caseObj.checkEvolvedModel
            (store.getModel(), env, true /*oldTypesExist*/);
        closeStore();

        /* Check raw objects. */
        openRawStore();
        caseObj.checkEvolvedModel
            (rawStore.getModel(), env, true /*oldTypesExist*/);
        caseObj.readRawObjects
            (rawStore, true /*expectEvolved*/, true /*expectUpdated*/);
        closeRawStore();

        closeAll();
    }
}
