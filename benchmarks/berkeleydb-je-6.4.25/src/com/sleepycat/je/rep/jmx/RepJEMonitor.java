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

package com.sleepycat.je.rep.jmx;

import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.jmx.JEMonitor;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.RepInternal;

/**
 * A concrete MBean for monitoring a replicated open JE Environment.
 *
 * It not only has the same attributes and operations as the standalone 
 * JEMonitor, but also has some special replicated related operations.
 */
public class RepJEMonitor extends JEMonitor {

    /**
     * @hidden
     *
     *  Name for dumping rep stats operation. 
     */
    public static final String OP_DUMP_REPSTATS = "getReplicationStats";

    /** 
     * @hidden
     *
     * Name for getting rep stats tips. 
     */
    public static final String OP_GET_REP_TIPS = "getRepTips";

    /* Name for getting RepImpl state. */
    static final String OP_DUMP_STATE = "dumpReplicationState";

    /* Define the dumping rep stats operation. */
    private static final MBeanOperationInfo OP_DUMP_REPSTATS_INFO =
        new MBeanOperationInfo
        (OP_DUMP_REPSTATS,
         "Dump environment's replicated stats.",
         statParams, "java.lang.String", MBeanOperationInfo.INFO);

    private static final MBeanOperationInfo OP_DUMP_STATE_INFO =
        new MBeanOperationInfo
        (OP_DUMP_STATE,
         "Dump replicated environment state, including current position in " +
         "replication stream and replication group database.", 
         new MBeanParameterInfo[0],
         "java.lang.String", MBeanOperationInfo.INFO);

    protected RepJEMonitor(Environment env) {
        super(env);
    }

    public RepJEMonitor() {
        super();
    }

    @Override
    protected void initClassFields() {
        currentClass = RepJEMonitor.class;
        className = "RepJEMonitor";
        DESCRIPTION = "Monitor an open replicated Berkeley DB, " +
                      "Java Edition environment.";
    }

    @Override
    public Object invoke(String actionName,
                         Object[] params,
                         String[] signature)
        throws MBeanException {

        if (actionName == null) {
            throw new IllegalArgumentException("ActionName can't be null.");
        }

        try {
            if (actionName.equals(OP_DUMP_REPSTATS)) {
                return ((ReplicatedEnvironment) env).
                    getRepStats(getStatsConfig(params)).toString();
            } else if (actionName.equals(OP_GET_REP_TIPS)) {
                return ((ReplicatedEnvironment) env).getRepStats
                    (getStatsConfig(new Object[] {false, true})).getTips();
            } else if (actionName.equals(OP_DUMP_STATE)) {
                return RepInternal.getRepImpl
                    ((ReplicatedEnvironment) env).dumpState();
            }
        } catch (DatabaseException e) {
            throw new MBeanException(new RuntimeException(e.getMessage()));
        }

        return super.invoke(actionName, params, signature);
    }

    @Override
    protected void doRegisterMBean(Environment useEnv) 
        throws Exception {

        server.registerMBean(new RepJEMonitor(useEnv), jeName);
    }

    @Override
    protected void addOperations() {
        super.addOperations();
        operationList.add(OP_DUMP_REPSTATS_INFO);
        operationList.add(OP_DUMP_STATE_INFO);
    }
}
