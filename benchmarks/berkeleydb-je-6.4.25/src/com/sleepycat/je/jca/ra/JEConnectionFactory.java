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

import java.io.Serializable;

import javax.resource.Referenceable;

import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.TransactionConfig;

/**
 * An application may obtain a {@link JEConnection} in this manner:
 * <pre>
 *    InitialContext iniCtx = new InitialContext();
 *    Context enc = (Context) iniCtx.lookup("java:comp/env");
 *    Object ref = enc.lookup("ra/JEConnectionFactory");
 *    JEConnectionFactory dcf = (JEConnectionFactory) ref;
 *    JEConnection dc = dcf.getConnection(envDir, envConfig);
 * </pre>
 *
 * See &lt;JEHOME&gt;/examples/jca/HOWTO-**.txt and
 * &lt;JEHOME&gt;/examples/jca/simple/SimpleBean.java for more information
 * on how to build the resource adapter and use a JEConnection.
 */
public interface JEConnectionFactory
    extends Referenceable, Serializable {

    public JEConnection getConnection(String jeRootDir,
                                      EnvironmentConfig envConfig)
        throws JEException;

    public JEConnection getConnection(String jeRootDir,
                                      EnvironmentConfig envConfig,
                                      TransactionConfig transConfig)
        throws JEException;
}
