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

import com.sleepycat.je.DbInternal;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.util.StringDbt;

/**
 * Various unit tests for CursorImpl to enhance code coverage.
 */
public class CodeCoverageTest extends DbCursorTestBase {

    public CodeCoverageTest() {
        super();
    }

    /**
     * Test the internal CursorImpl.delete() deleted LN code..
     */
    @Test
    public void testDeleteDeleted()
        throws Throwable {

        try {
            initEnv(false);
            doSimpleCursorPuts();

            StringDbt foundKey = new StringDbt();
            StringDbt foundData = new StringDbt();

            OperationStatus status = cursor.getFirst(foundKey, foundData,
                                                     LockMode.DEFAULT);
            assertEquals(OperationStatus.SUCCESS, status);

            cursor.delete();
            cursor.delete();

            /*
             * While we've got a cursor in hand, call CursorImpl.dumpToString()
             */
            DbInternal.getCursorImpl(cursor).dumpToString(true);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }
}
