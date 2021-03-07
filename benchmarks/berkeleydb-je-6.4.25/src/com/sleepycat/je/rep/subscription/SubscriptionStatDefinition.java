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
package com.sleepycat.je.rep.subscription;

import com.sleepycat.je.utilint.StatDefinition;

/**
 * Object to represent subscription statistics
 */
class SubscriptionStatDefinition {

    public static final String GROUP_NAME = "Subscription";
    public static final String GROUP_DESC = "Subscription statistics";

    public static final StatDefinition SUB_N_REPLAY_QUEUE_OVERFLOW =
            new StatDefinition(
                "nReplayQueueOverflow",
                "The number inserts into the replay queue that failed " +
                "because the queue was full.");

    public static final StatDefinition SUB_MSG_RECEIVED =
            new StatDefinition(
                "msg_received",
                "The number of messages received from feeder");

    public static final StatDefinition SUB_MSG_RESPONDED =
            new StatDefinition(
                "msg_responded",
                "The number of messages responded to feeder");

    public static final StatDefinition SUB_OPS_PROCESSED =
            new StatDefinition(
                "ops_processed",
                "The number of data operations processed by subscriber");

    public static final StatDefinition SUB_TXN_COMMITTED =
            new StatDefinition(
                "txn_committed",
                "The number of committed transactions received from feeder ");

    public static final StatDefinition SUB_TXN_ABORTED =
            new StatDefinition(
                "txn_aborted",
                "The number of aborted transactions received from feeder ");

    public static final StatDefinition SUB_MAX_PENDING_INPUT =
            new StatDefinition(
                "max_pending_input",
                "The max number of pending items in the input queue");
}
