/*-
 *
 *  This file is part of Oracle NoSQL Database
 *  Copyright (C) 2011, 2014 Oracle and/or its affiliates.  All rights reserved.
 *
 *  Oracle NoSQL Database is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation, version 3.
 *
 *  Oracle NoSQL Database is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public
 *  License in the LICENSE file along with Oracle NoSQL Database.  If not,
 *  see <http://www.gnu.org/licenses/>.
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

package com.sleepycat.je.rep.stream;

import com.sleepycat.je.rep.impl.RepImpl;

/**
 * The FeederFilter is used by the Feeder to determine whether a record should
 * be sent to the Replica. The filter object is created at the replica and is
 * transmitted to the Feeder as part of the syncup process. The filter thus
 * represents replica code that is running inside the Feeder, that is, the
 * computation has been moved closer to the data and can be used to eliminate
 * unnecessary network communication overheads.
 */
public interface FeederFilter {

    /**
     * The execute method that invoked before a record is sent to the replica.
     * If the filter returns null, the feeder will not send the record to the
     * replica as part of the replication stream, since it's not of interest
     * to the replica. It can for example be used to filter out tables that
     * are not of interest to the replica.
     *
     * @param record the record to be filtered
     * @param repImpl repImpl of the RN where the filter is executed
     *
     * @return the original input record if it is to be sent to the replica.
     * null if it's to be skipped.
     */
    OutputWireRecord execute(final OutputWireRecord record,
                             final RepImpl repImpl);
}
