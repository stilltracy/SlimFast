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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.config.DurationConfigParam;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.config.IntConfigParam;
import com.sleepycat.je.dbi.DatabaseId;
import com.sleepycat.je.dbi.DatabaseImpl;
import com.sleepycat.je.dbi.DbConfigManager;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.ReplicationNetworkConfig;
import com.sleepycat.je.rep.impl.RepImpl;
import com.sleepycat.je.rep.impl.RepParams;
import com.sleepycat.je.rep.stream.FeederFilter;
import com.sleepycat.je.rep.stream.OutputWireRecord;
import com.sleepycat.je.rep.utilint.HostPortPair;
import com.sleepycat.je.utilint.DatabaseUtil;
import com.sleepycat.je.utilint.PropUtil;
import com.sleepycat.je.utilint.VLSN;

/**
 * Object to represent parameters to configure a subscription.
 */
public class SubscriptionConfig implements Cloneable {

    /*-----------------------------------*/
    /*-      Constant Parameters        -*/
    /*-----------------------------------*/

    /* queue poll interval in millisecond, 1 second */
    public final static long QUEUE_POLL_INTERVAL_MS = 1000l;

    /* for quick response, no Nagle's algorithm */
    public final boolean TCP_NO_DELAY = true;
    /* always blocking mode socket channel */
    public final boolean BLOCKING_MODE_CHANNEL = true;
    /* always validate parameters */
    private final boolean validateParams = true;

    /*-----------------------------------*/
    /*-    User-defined Parameters      -*/
    /*-----------------------------------*/

    /* local directory of subscriber */
    private final String subHome;

    /*
     * identity of a subscription node.
     *
     * Subscription client need to create a globally unique node name, e.g.,
     * subscription-<uuid> because the feeder maintains the identity of
     * each connection, and would reject request from a client with a
     * duplicate identity.
     */
    private final String subNodeName;

    /* subscriber host and port */
    private final String subHostPortPair;
    /* host where the feeder is running */
    private final String feederHostPortPair;
    /* name of replication group */
    private final String groupName;

    /*
     * uuid of feeder replication group.
     *
     * This parameter is optional. It subscription client does not provide a
     * group UUID, subscription would subscribe a feeder as long as the
     * subscription group name matches that of the feeder. However, if
     * subscription client does provide a valid group UUID, it has to match
     * that of feeder, otherwise subscription request will be rejected.
     */
    private UUID groupUUID;

    /* callback used in subscription */
    private SubscriptionCallback callBack;
    /* filter passed to feeder */
    private FeederFilter feederFilter;

    /* home of a set of connection parameters */
    private Properties props;

    /* message queue size */
    private int inputMessageQueueSize;
    private int outputMessageQueueSize;

    /**
     * Create a subscription configuration
     *
     * @param subNodeName        id of the subscription
     * @param subHome            home directory of subscriber
     * @param subHostPortPair    subscriber host and port
     * @param feederHostPortPair feeder host and port
     * @param groupName          name of replication group feeder belong to
     */
    public SubscriptionConfig(String subNodeName,
                              String subHome,
                              String subHostPortPair,
                              String feederHostPortPair,
                              String groupName) throws UnknownHostException {
       this(subNodeName, subHome, subHostPortPair, feederHostPortPair,
            groupName, null);
    }

    /**
     * Create a subscription configuration with group UUID.
     *
     * @param subNodeName        id of the subscription
     * @param subHome            home directory of subscriber
     * @param subHostPortPair    subscriber host and port
     * @param feederHostPortPair feeder host and port
     * @param groupName          name of replication group feeder belong to
     * @param groupUUID          id of replication group feeder belong to
     */
    public SubscriptionConfig(String subNodeName,
                              String subHome,
                              String subHostPortPair,
                              String feederHostPortPair,
                              String groupName,
                              UUID   groupUUID) throws UnknownHostException {
        /* subscriber */
        this.subNodeName = subNodeName;
        this.subHome = subHome;
        this.subHostPortPair = subHostPortPair;

        /* feeder */
        this.feederHostPortPair = feederHostPortPair;

        /* replication group */
        this.groupName = groupName;
        this.groupUUID = groupUUID;

        /* other parameters */
        props = new Properties();
        inputMessageQueueSize = getDefaultMsgQueueSize();
        outputMessageQueueSize = getDefaultMsgQueueSize();

        /* default callback and filter */
        callBack = new DefaultCallback();
        feederFilter = new DefaultFeederFilter();

        verifyParameters();
    }

    /**
     * Create an environment configuration for subscription
     *
     * @return an environment configuration
     */
    public EnvironmentConfig createEnvConfig() {
        /* Populate env. configuration parameters */
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setReadOnly(true);
        envConfig.setTransactional(true);
        envConfig.setConfigParam(
            EnvironmentParams.ENV_RECOVERY.getName(), "false");
        envConfig.setConfigParam(
            EnvironmentParams.ENV_SETUP_LOGGER.getName(), "true");

        return envConfig;
    }

    /**
     * Create a replication configuration for subscription
     *
     * @return a replication configuration
     */
    public ReplicationConfig createReplicationConfig() {
        /* Populate rep. configuration parameters */
        ReplicationConfig repConfig =
            new ReplicationConfig(getGroupName(),
                                  getSubNodeName(),
                                  getSubNodeHostPort());

        repConfig.setConfigParam(RepParams.SUBSCRIBER_USE.getName(), "true");

        ReplicationNetworkConfig defaultNetConfig =
            ReplicationNetworkConfig.createDefault();
        repConfig.setRepNetConfig(defaultNetConfig);

        repConfig.setConfigParam(RepParams.REPLICA_MESSAGE_QUEUE_SIZE.getName(),
                                 Integer.toString(getDefaultMsgQueueSize()));

        repConfig.setConfigParam(
            RepParams.REPLICA_TIMEOUT.getName(),
            String.valueOf(getChannelTimeout(TimeUnit.MILLISECONDS)) +
            " ms");

        repConfig.setConfigParam(
            RepParams.PRE_HEARTBEAT_TIMEOUT.getName(),
            String.valueOf(getPreHeartbeatTimeout(TimeUnit.MILLISECONDS)) +
            " ms");

        repConfig.setConfigParam(
            RepParams.REPSTREAM_OPEN_TIMEOUT.getName(),
            String.valueOf(getStreamOpenTimeout(TimeUnit.MILLISECONDS)) +
            " ms");

        repConfig.setConfigParam(RepParams.HEARTBEAT_INTERVAL.getName(),
                                 Integer.toString(getHeartbeatIntervalMs()));

        repConfig.setConfigParam(
            RepParams.REPLICA_RECEIVE_BUFFER_SIZE.getName(),
            Integer.toString(getReceiveBufferSize()));

        return repConfig;
    }
    
    /*--------------*/
    /*-  Getters   -*/
    /*--------------*/

    public FeederFilter getFeederFilter() {
        return feederFilter;
    }

    public SubscriptionCallback getCallBack() {
        return callBack;
    }

    public String getSubscriberHome() {
        return subHome;
    }

    public String getFeederHost() {
        return HostPortPair.getHostname(feederHostPortPair);
    }

    public int getFeederPort() {
        return HostPortPair.getPort(feederHostPortPair);
    }

    public InetAddress getFeederHostAddr() throws UnknownHostException {
        return InetAddress.getByName(HostPortPair
                                         .getHostname(feederHostPortPair));
    }

    public String getSubNodeName() {
        return subNodeName;
    }

    public String getSubNodeHostPort() {
        return subHostPortPair;
    }

    public String getGroupName() {
        return groupName;
    }

    public UUID getGroupUUID() {
        return groupUUID;
    }

    public int getMaxConnectRetries() {
        return DbConfigManager.getIntVal(props,
                                         RepParams
                                             .SUBSCRIPTION_MAX_CONNECT_RETRIES);
    }

    public long getSleepBeforeRetryMs() {
        return
            DbConfigManager.getDurationVal(props,
                                           RepParams
                                               .SUBSCRIPTION_SLEEP_BEFORE_RETRY,
                                           TimeUnit.MILLISECONDS);
    }

    public long getChannelTimeout(TimeUnit unit) {
        DurationConfigParam param = RepParams.REPLICA_TIMEOUT;
        if (props.containsKey(param.getName())) {
            return DbConfigManager.getDurationVal(props,
                                                  RepParams.REPLICA_TIMEOUT,
                                                  unit);
        } else {
            long ms = PropUtil.parseDuration(param.getDefault());
            return unit.convert(ms, TimeUnit.MILLISECONDS);
        }
    }

    public long getPollIntervalMs() {
        return DbConfigManager.getDurationVal(props,
                                              RepParams
                                                  .SUBSCRIPTION_POLL_INTERVAL,
                                              TimeUnit.MILLISECONDS);
    }

    public long getPollTimeoutMs() {
        return DbConfigManager.getDurationVal(props,
                                              RepParams
                                                  .SUBSCRIPTION_POLL_TIMEOUT,
                                              TimeUnit.MILLISECONDS);
    }

    public long getPreHeartbeatTimeout(TimeUnit unit) {
        DurationConfigParam param = RepParams.PRE_HEARTBEAT_TIMEOUT;
        if (props.containsKey(param.getName())) {
            return DbConfigManager.getDurationVal(props, param, unit);
        } else {
            long ms = PropUtil.parseDuration(param.getDefault());
            return unit.convert(ms, TimeUnit.MILLISECONDS);
        }
    }

    public long getStreamOpenTimeout(TimeUnit unit) {
        DurationConfigParam param = RepParams.REPSTREAM_OPEN_TIMEOUT;
        if (props.containsKey(param.getName())) {
            return DbConfigManager.getDurationVal(props, param, unit);
        } else {
            long ms = PropUtil.parseDuration(param.getDefault());
            return unit.convert(ms, TimeUnit.MILLISECONDS);
        }
    }

    public int getHeartbeatIntervalMs() {
        IntConfigParam param = RepParams.HEARTBEAT_INTERVAL;
        if (props.containsKey(param.getName())) {
            return DbConfigManager.getIntVal(props, param);
        } else {
            return Integer.parseInt(param.getDefault());
        }
    }

    public int getReceiveBufferSize() {
        IntConfigParam param = RepParams.REPLICA_RECEIVE_BUFFER_SIZE;
        if (props.containsKey(param.getName())) {
            return DbConfigManager.getIntVal(props, param);
        } else {
            return Integer.parseInt(param.getDefault());
        }
    }

    public int getInputMessageQueueSize() {
        return inputMessageQueueSize;
    }

    public int getOutputMessageQueueSize() {
        return outputMessageQueueSize;
    }

    public InetSocketAddress getInetSocketAddress()
        throws UnknownHostException {
        return new InetSocketAddress(getFeederHostAddr(), getFeederPort());
    }


    /*--------------*/
    /*-  Setters   -*/
    /*--------------*/

    public void setGroupUUID(UUID gID) {
        groupUUID = gID;
    }

    public void setCallback(SubscriptionCallback cbk) {
        if (cbk == null) {
            throw new IllegalArgumentException("Subscription callback cannot " +
                                               "be null.");
        }
        callBack = cbk;
    }

    public void setChannelTimeout(long timeout, TimeUnit unit)
            throws IllegalArgumentException {
        DbConfigManager.setDurationVal(props, RepParams.REPLICA_TIMEOUT,
                                       timeout, unit, validateParams);
    }

    public void setPreHeartbeatTimeout(long timeout, TimeUnit unit)
            throws IllegalArgumentException {
        DbConfigManager.setDurationVal(props, RepParams.PRE_HEARTBEAT_TIMEOUT,
                                       timeout, unit, validateParams);
    }

    public void setHeartbeatInterval(int ms)
            throws IllegalArgumentException {
        DbConfigManager.setIntVal(props, RepParams.HEARTBEAT_INTERVAL, ms,
                                  validateParams);
    }

    public void setStreamOpenTimeout(long timeout, TimeUnit unit)
            throws IllegalArgumentException {
        DbConfigManager.setDurationVal(props, RepParams.REPSTREAM_OPEN_TIMEOUT,
                                       timeout, unit, validateParams);
    }

    public void setReceiveBufferSize(int val) {
        DbConfigManager.setIntVal(props, RepParams.REPLICA_RECEIVE_BUFFER_SIZE,
                                  val, validateParams);
    }

    public void setInputMessageQueueSize(int size) {
        inputMessageQueueSize = size;
    }

    public void setOutputMessageQueueSize(int size) {
        outputMessageQueueSize = size;
    }

    public SubscriptionConfig clone() {
        try {
            SubscriptionConfig ret = (SubscriptionConfig) super.clone();
            ret.setProps(this.props);
            return ret;
        } catch (CloneNotSupportedException willNeverOccur) {
            return null;
        }
    }

    /**
     * For internal test only.
     *
     * Set the feeder filter which will be transmitted to Feeder. Due to the
     * dup db issues, client is not allowed to set non-default feeder filter.
     *
     * @param filter  the non-null feeder filter
     */
    void setFeederFilter(FeederFilter filter) {

        if (filter == null) {
            throw new IllegalArgumentException("Feeder filter cannot be null.");
        }
        feederFilter = filter;
    }

    private void setProps(Properties p) {
        props = p;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("subscription configuration: ").append("\n");
        sb.append("subscription name: ").append(subNodeName).append("\n");
        sb.append("home directory: ").append(subHome).append("\n");
        sb.append("home host and port: ").append(subHostPortPair).append("\n");

        sb.append("feeder host and port: ").append(feederHostPortPair)
          .append("\n");

        try {
            sb.append("feeder address: ")
              .append(getFeederHostAddr()).append("\n");
        } catch (UnknownHostException e) {
            sb.append("feeder address: unknown host ")
              .append(feederHostPortPair).append("\n");
        }
        sb.append("feeder filter: ").append(feederFilter).append("\n");

        sb.append("rep group: ").append(groupName).append("\n");
        sb.append("rep group id: ").append(groupUUID).append("\n");

        return sb.toString();
    }

    /*
     * Verify all required parameters are available and valid
     *
     * must-have parameters:
     * - non-null home directory
     * - non-null feeder host port pair
     * - non-null feeder host name
     * - non-null feeder host port
     * - non-null subscriber node host port pair
     * - non-null subscriber node name
     * - non-null subscriber node host port
     * - non-null replication group name
     *
     * @throws IllegalArgumentException
     */
    private void verifyParameters() throws IllegalArgumentException {

        DatabaseUtil.checkForNullParam(getSubscriberHome(),
                "subscription home directory");

        DatabaseUtil.checkForNullParam(feederHostPortPair,
                                       "feeder host port pair");

        DatabaseUtil.checkForNullParam(getFeederHost(), "feeder host name");

        DatabaseUtil.checkForNullParam(getFeederPort(), "feeder host port");

        DatabaseUtil.checkForNullParam(subHostPortPair,
                                       "subscriber host port pair");

        DatabaseUtil.checkForNullParam(getSubNodeName(),
                                       "subscriber node name");

        DatabaseUtil.checkForNullParam(getSubNodeHostPort(),
                                       "subscriber node host port");

        DatabaseUtil.checkForNullParam(getGroupName(), "replication group");
    }

    /* a default no-op callback */
    private class DefaultCallback implements SubscriptionCallback {

        DefaultCallback() {
        }

        @Override
        public void processPut(VLSN vlsn, byte[] key, byte[] value,
                               long txnId) {

        }

        @Override
        public void processDel(VLSN vlsn, byte[] key, long txnId) {

        }

        @Override
        public void processCommit(VLSN vlsn, long txnid) {

        }

        @Override
        public void processAbort(VLSN vlsn, long txnid) {

        }

        @Override
        public void processException(final Exception exception) {

        }
    }

    private int getDefaultMsgQueueSize() {
        IntConfigParam param = RepParams.REPLICA_MESSAGE_QUEUE_SIZE;
        if (props.containsKey(param.getName())) {
            return DbConfigManager.getIntVal(props, param);
        } else {
            return Integer.parseInt(param.getDefault());
        }
    }

    /*
     * a default filter that filters out entries from internal db and db
     * supporting duplicates.
     */
    private static class DefaultFeederFilter
            implements FeederFilter, Serializable {
        private static final long serialVersionUID = 1L;

        DefaultFeederFilter() {
            super();
        }

        @Override
        public OutputWireRecord execute(final OutputWireRecord record,
                                        final RepImpl repImpl) {

            /* keep record if db id is null */
            final DatabaseId dbId = record.getReplicableDBId();
            if (dbId == null) {
                return record;
            }

            final DatabaseImpl impl = repImpl.getDbTree().getDb(dbId);
            /* keep record if db impl is not available */
            if (impl == null) {
                return record;
            }

            /* filter out if from an db supporting duplicates */
            if (impl.getSortedDuplicates()) {
                return null;
            }

            /* filter out if from an internal db */
            if (impl.isInternalDb()) {
                return null;
            }

            return record;
        }
    }
}
