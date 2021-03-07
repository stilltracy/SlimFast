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

import java.io.File;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.TransactionConfig;

public class JEConnectionFactoryImpl implements JEConnectionFactory {

    private static final long serialVersionUID = 410682596L;

    /*
     * These are not transient because SJSAS seems to need to serialize
     * them when leaving them in JNDI.
     */
    private final /* transient */ ConnectionManager manager;
    private final /* transient */ ManagedConnectionFactory factory;
    private Reference reference;

    /* Make the constructor public for serializability testing. */
    public JEConnectionFactoryImpl(ConnectionManager manager,
                            ManagedConnectionFactory factory) {
        this.manager = manager;
        this.factory = factory;
    }

    public JEConnection getConnection(String jeRootDir,
                                      EnvironmentConfig envConfig)
        throws JEException {

        return getConnection(jeRootDir, envConfig, null);
    }

    public JEConnection getConnection(String jeRootDir,
                                      EnvironmentConfig envConfig,
                                      TransactionConfig transConfig)
        throws JEException {

        JEConnection dc = null;
         JERequestInfo jeInfo =
             new JERequestInfo(new File(jeRootDir), envConfig, transConfig);
        try {
            dc = (JEConnection) manager.allocateConnection(factory, jeInfo);
        } catch (ResourceException e) {
            throw new JEException("Unable to get Connection: " + e);
        }

        return dc;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public Reference getReference() {
        return reference;
    }
}
