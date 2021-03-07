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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import com.sleepycat.je.DbInternal;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.dbi.EnvironmentImpl;

public class JEManagedConnectionFactory
    implements ManagedConnectionFactory, Serializable {

    private static final long serialVersionUID = 658705244L;

    public JEManagedConnectionFactory() {
    }

    public Object createConnectionFactory(ConnectionManager cxManager) {
        return new JEConnectionFactoryImpl(cxManager, this);
    }

    public Object createConnectionFactory() {
        throw EnvironmentFailureException.unexpectedState
            ("must supply a connMgr");
    }

    public ManagedConnection
        createManagedConnection(Subject subject,
                                ConnectionRequestInfo info)
        throws ResourceException {

        JERequestInfo jeInfo = (JERequestInfo) info;
        return new JEManagedConnection(subject, jeInfo);
    }

    public ManagedConnection
        matchManagedConnections(Set connectionSet,
                                Subject subject,
                                ConnectionRequestInfo info) {
        JERequestInfo jeInfo = (JERequestInfo) info;
        Iterator iter = connectionSet.iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if (next instanceof JEManagedConnection) {
                JEManagedConnection mc = (JEManagedConnection) next;
                EnvironmentImpl nextEnvImpl =
                    DbInternal.getEnvironmentImpl(mc.getEnvironment());
                /* Do we need to match on more than root dir and r/o? */
                if (nextEnvImpl.getEnvironmentHome().
                    equals(jeInfo.getJERootDir()) &&
                    nextEnvImpl.isReadOnly() ==
                    jeInfo.getEnvConfig().getReadOnly()) {
                    return mc;
                }
            }
        }
        return null;
    }

    public void setLogWriter(PrintWriter out) {
    }

    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof JEManagedConnectionFactory) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
