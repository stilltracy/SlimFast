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

package com.sleepycat.je.dbi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.config.BooleanConfigParam;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.util.TestUtils;
import com.sleepycat.util.test.TestBase;

public class DbConfigManagerTest extends TestBase {

    /**
     * Test that parameter defaults work, that we can add and get
     * parameters
     */
    @Test
    public void testBasicParams() {
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setCacheSize(2000);
        DbConfigManager configManager = new DbConfigManager(envConfig);

        /**
         * Longs: The config manager should return the value for an
         * explicitly set param and the default for one not set.
         *
         */
        assertEquals(2000,
                     configManager.getLong(EnvironmentParams.MAX_MEMORY));
        assertEquals(EnvironmentParams.ENV_RECOVERY.getDefault(),
                     configManager.get(EnvironmentParams.ENV_RECOVERY));
    }

    /**
     * Checks that leading and trailing whitespace is ignored when parsing a
     * boolean.  [#22212]
     */
    @Test
    public void testBooleanWhitespace() {
        String val = " TruE "; // has leading and trailing space
        String name = EnvironmentConfig.SHARED_CACHE; // any boolean will do
        BooleanConfigParam param =
            (BooleanConfigParam) EnvironmentParams.SUPPORTED_PARAMS.get(name);
        param.validateValue(val);
        EnvironmentConfig envConfig = TestUtils.initEnvConfig();
        envConfig.setConfigParam(name, val);
        DbConfigManager configManager = new DbConfigManager(envConfig);
        assertEquals(true, configManager.getBoolean(param));
    }
}
