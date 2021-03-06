/*-
 *
 *  This file is part of Oracle NoSQL Database
 *  Copyright (C) 2011, 2015 Oracle and/or its affiliates.  All rights reserved.
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
package com.sleepycat.je.rep.subscription;

import com.sleepycat.je.rep.GroupShutdownException;
import com.sleepycat.je.rep.InsufficientLogException;
import com.sleepycat.je.rep.RepInternal;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.utilint.InternalException;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.je.utilint.PollCondition;
import com.sleepycat.je.utilint.TestHook;
import com.sleepycat.je.utilint.VLSN;

import java.io.File;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Object to represent a subscription to receive and process replication
 * streams from Feeder. It defines the public subscription APIs which can
 * be called by clients.
 */
public class Subscription {

    /* configuration parameters */
    private final SubscriptionConfig configuration;
    /* logger */
    private final Logger logger;
    /* subscription dummy environment */
    private final ReplicatedEnvironment dummyRepEnv;
    /* subscription statistics */
    private final SubscriptionStat statistics;

    /* main subscription thread */
    private SubscriptionThread subscriptionThread;

    /**
     * Create an instance of subscription from configuration
     *
     * @param configuration configuration parameters
     * @param logger        logging handler
     *
     * @throws IllegalArgumentException  if env directory does not exist
     */
    public Subscription(SubscriptionConfig configuration, Logger logger)
        throws IllegalArgumentException {

        this.configuration = configuration;
        this.logger = logger;

        /* init environment and parameters */
        dummyRepEnv = createDummyRepEnv(configuration);
        subscriptionThread = null;
        statistics = new SubscriptionStat();
    }

    /**
     * Start subscription main thread, subscribe from the very first VLSN
     * from the feeder. The subscriber will stay alive and consume all entries
     * until it shuts down.
     *
     * @throws InsufficientLogException if feeder is unable to stream from
     *                                  start VLSN
     * @throws GroupShutdownException   if subscription receives group shutdown
     * @throws InternalException        if internal exception
     * @throws TimeoutException         if subscription initialization timeout
    */
    public void start()
        throws IllegalArgumentException, InsufficientLogException,
        GroupShutdownException, InternalException, TimeoutException {

        start(VLSN.FIRST_VLSN);
    }

    /**
     * Start subscription main thread, subscribe from a specific VLSN
     * from the feeder. The subscriber will stay alive and consume all entries
     * until it shuts down.
     *
     * @param vlsn the start VLSN of subscription. It cannot be NULL_VLSN
     *             otherwise an IllegalArgumentException will be raised.
     *
     * @throws InsufficientLogException if feeder is unable to stream from
     *                                  start VLSN
     * @throws GroupShutdownException   if subscription receives group shutdown
     * @throws InternalException        if internal exception
     * @throws TimeoutException         if subscription initialization timeout
     */
    public void start(VLSN vlsn)
        throws IllegalArgumentException, InsufficientLogException,
        GroupShutdownException, InternalException, TimeoutException {

        if (vlsn.equals(VLSN.NULL_VLSN)) {
            throw new IllegalArgumentException("Start VLSN cannot be null");
        }

        subscriptionThread =
            new SubscriptionThread(dummyRepEnv, vlsn,
                                   configuration, statistics,
                                   logger);
        /* fire the subscription thread */
        subscriptionThread.start();

        if (!waitForSubscriptionInitDone(subscriptionThread)) {
            LoggerUtils.warning(logger, RepInternal.getRepImpl(dummyRepEnv),
                                "Timeout in initialization, shut down " +
                                "subscription.");
            shutdown();
            throw new TimeoutException("Subscription initialization timeout " +
                                       "after " +
                                       configuration.getPollTimeoutMs() +
                                       " ms");
        }

        /* if not success, throw exception to caller */
        final Exception exp = subscriptionThread.getStoredException();
        switch (subscriptionThread.getStatus()) {
            case SUCCESS:
                break;

            case VLSN_NOT_AVAILABLE:
                throw (InsufficientLogException) exp;

            case GRP_SHUTDOWN:
                throw (GroupShutdownException) exp;

            case UNKNOWN_ERROR:
            case CONNECTION_ERROR:
            default:
                throw new InternalException("internal exception from " +
                                            "subscription thread", exp);
        }
    }

    /**
     * Shutdown a subscription completely
     */
    public void shutdown() {
        if (subscriptionThread != null && subscriptionThread.isAlive()) {
            subscriptionThread.shutdown();
        }
        subscriptionThread = null;

        if (dummyRepEnv != null) {
            dummyRepEnv.close();
        }
    }

    /**
     * Get subscription thread status, if thread does not exit,
     * return subscription not yet started.
     *
     * @return status of subscription
     */
    public SubscriptionStatus getSubscriptionStatus() {
        if (subscriptionThread == null) {
            return SubscriptionStatus.INIT;
        } else {
            return subscriptionThread.getStatus();
        }
    }

    /**
     * Get subscription statistics
     *
     * @return  statistics
     */
    public SubscriptionStat getStatistics() {
        return statistics;
    }

    /**
     * For unit test only
     *
     * @param testHook test hook
     */
    void setExceptionHandlingTestHook(TestHook<SubscriptionThread> testHook) {
        if (subscriptionThread != null) {
            subscriptionThread.setExceptionHandlingTestHook(testHook);
        }
    }

    /**
     * Create a dummy replicated env used by subscription. The dummy env will
     * be used in the SubscriptionThread, SubscriptionProcessMessageThread and
     * SubscriptionOutputThread to connect to feeder.
     *
     * @param config subscription configuration
     * @return a replicated environment
     * @throws IllegalArgumentException if env directory does not exist
     */
    private ReplicatedEnvironment createDummyRepEnv(SubscriptionConfig config)
        throws IllegalArgumentException {

        File envHome = new File(config.getSubscriberHome());
        if (!envHome.exists()) {
            throw new IllegalArgumentException("Env directory " +
                                               envHome.getAbsolutePath() +
                                               " does not exist.");
        }

        return RepInternal.createInternalEnvHandle(envHome, configuration
            .createReplicationConfig(), configuration.createEnvConfig());
    }

    /**
     * Wait for subscription thread to finish initialization
     *
     * @param t thread of subscription
     * @return true if init done successfully, false if timeout
     */
    private boolean waitForSubscriptionInitDone(final SubscriptionThread t) {
        return new PollCondition(configuration.getPollIntervalMs(),
                                 configuration.getPollTimeoutMs()) {
            @Override
            protected boolean condition() {
                return t.getStatus() != SubscriptionStatus.INIT;
            }

        }.await();
    }
}
