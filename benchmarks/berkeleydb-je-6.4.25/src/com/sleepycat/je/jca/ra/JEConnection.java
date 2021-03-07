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

package com.sleepycat.je.jca.ra;

import java.io.Closeable;
import javax.resource.ResourceException;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Transaction;

/**
 * A JEConnection provides access to JE services. See
 * &lt;JEHOME&gt;/examples/jca/HOWTO-**.txt and
 * &lt;JEHOME&gt;/examples/jca/simple/SimpleBean.java for more information on
 * how to build the resource adaptor and use a JEConnection.
 */
public class JEConnection implements Closeable {

    private JEManagedConnection mc;
    private JELocalTransaction txn;

    public JEConnection(JEManagedConnection mc) {
        this.mc = mc;
    }

    protected void setManagedConnection(JEManagedConnection mc,
                                        JELocalTransaction lt) {
        this.mc = mc;
        if (txn == null) {
            txn = lt;
        }
    }

    JELocalTransaction getLocalTransaction() {
        return txn;
    }

    void setLocalTransaction(JELocalTransaction txn) {
        this.txn = txn;
    }

    public Environment getEnvironment() {
        return mc.getEnvironment();
    }

    public Database openDatabase(String name, DatabaseConfig config)
        throws DatabaseException {

        return mc.openDatabase(name, config);
    }

    public SecondaryDatabase openSecondaryDatabase(String name,
                                                   Database primaryDatabase,
                                                   SecondaryConfig config)
        throws DatabaseException {

        return mc.openSecondaryDatabase(name, primaryDatabase, config);
    }

    public void removeDatabase(String databaseName)
        throws DatabaseException {

        mc.removeDatabase(databaseName);
    }

    public long truncateDatabase(String databaseName, boolean returnCount)
        throws DatabaseException {

        return mc.truncateDatabase(databaseName, returnCount);
    }

    public Transaction getTransaction()
        throws ResourceException {

        if (txn == null) {
            return null;
        }

        try {
            return txn.getTransaction();
        } catch (DatabaseException DE) {
            ResourceException ret = new ResourceException(DE.toString());
            ret.initCause(DE);
            throw ret;
        }
    }

    public void close() {
        mc.close();
    }
}
