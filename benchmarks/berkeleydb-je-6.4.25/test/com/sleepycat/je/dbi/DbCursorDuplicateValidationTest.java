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

import static org.junit.Assert.assertFalse;

import java.util.Enumeration;
import java.util.Hashtable;

import org.junit.Test;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbTestProxy;
import com.sleepycat.je.tree.BIN;

public class DbCursorDuplicateValidationTest extends DbCursorTestBase {

    public DbCursorDuplicateValidationTest() {
        super();
    }

    @Test
    public void testValidateCursors()
        throws Throwable {

        initEnv(true);
        Hashtable dataMap = new Hashtable();
        createRandomDuplicateData(10, 1000, dataMap, false, false);

        Hashtable bins = new Hashtable();

        DataWalker dw = new DataWalker(bins) {
                void perData(String foundKey, String foundData)
                    throws DatabaseException {
                    CursorImpl cursorImpl =
                        DbTestProxy.dbcGetCursorImpl(cursor);
                    BIN lastBin = cursorImpl.getBIN();
                    if (rnd.nextInt(10) < 8) {
                        cursor.delete();
                    }
                    dataMap.put(lastBin, lastBin);
                }
            };
        dw.setIgnoreDataMap(true);
        dw.walkData();
        dw.close();
        Enumeration e = bins.keys();
        while (e.hasMoreElements()) {
            BIN b = (BIN) e.nextElement();
            assertFalse(b.getCursorSet().size() > 0);
        }
    }
}
