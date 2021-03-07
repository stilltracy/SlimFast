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

package com.sleepycat.je.config;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.util.test.TestBase;

public class EnvironmentParamsTest extends TestBase {

    private IntConfigParam intParam =
        new IntConfigParam("param.int",
                           new Integer(2),
                           new Integer(10),
                           new Integer(5),
                           false, // mutable
                           false);// for replication

    private LongConfigParam longParam =
        new LongConfigParam("param.long",
                            new Long(2),
                            new Long(10),
                            new Long(5),
                            false, // mutable
                            false);// for replication

    private ConfigParam mvParam =
        new ConfigParam("some.mv.param.#", null, true /* mutable */,
                        false /* for replication */);

    /**
     * Test param validation.
     */
    @Test
    public void testValidation() {
        assertTrue(mvParam.isMultiValueParam());

        try {
            new ConfigParam(null, "foo", false /* mutable */,
                            false /* for replication */);
            fail("should disallow null name");
        } catch (EnvironmentFailureException e) {
            // expected.
        }

        /* Test bounds. These are all invalid and should fail */
        checkValidateParam(intParam, "1");
        checkValidateParam(intParam, "11");
        checkValidateParam(longParam, "1");
        checkValidateParam(longParam, "11");
    }

    /**
     * Check that an invalid parameter isn't mistaken for a multivalue
     * param.
     */
    @Test
    public void testInvalidVsMultiValue() {
        try {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setConfigParam("je.maxMemory.stuff", "true");
            fail("Should throw exception");
        } catch (IllegalArgumentException IAE) {
            // expected
        }
    }

    /* Helper to catch expected exceptions */
    private void checkValidateParam(ConfigParam param, String value) {
        try {
            param.validateValue(value);
            fail("Should throw exception");
        } catch (IllegalArgumentException e) {
            // expect this exception
        }
    }
}
