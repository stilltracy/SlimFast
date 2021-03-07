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

package com.sleepycat.je.test;

import java.util.Set;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import com.sleepycat.je.SecondaryMultiKeyCreator;
import com.sleepycat.util.test.TxnTestCase;

/**
 * Permutes a TxnTestCase over a boolean property for using multiple secondary
 * keys.
 */
public abstract class MultiKeyTxnTestCase extends TxnTestCase {

    boolean useMultiKey = false;

    /**
     * Wraps a single key creator to exercise the multi-key code for tests that
     * only create a single secondary key.
     */
    static class SimpleMultiKeyCreator
        implements SecondaryMultiKeyCreator {

        private SecondaryKeyCreator keyCreator;

        SimpleMultiKeyCreator(SecondaryKeyCreator keyCreator) {
            this.keyCreator = keyCreator;
        }

        public void createSecondaryKeys(SecondaryDatabase secondary,
                                        DatabaseEntry key,
                                        DatabaseEntry data,
                                        Set results)
            throws DatabaseException {

            DatabaseEntry result = new DatabaseEntry();
            if (keyCreator.createSecondaryKey(secondary, key, data, result)) {
                results.add(result);
            }
        }
    }
}
