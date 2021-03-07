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

package com.sleepycat.je.rep.persist.test;

import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.AnnotationModel;
import com.sleepycat.persist.model.EntityModel;
import com.sleepycat.persist.evolve.Mutations;

public abstract class AppBaseImpl implements AppInterface {

    protected int version;
    protected ReplicatedEnvironment env;
    protected EntityStore store;
    private boolean doInitDuringOpen;

    public void setVersion(final int version) {
        this.version = version;
    }

    public void setInitDuringOpen(final boolean doInit) {
        doInitDuringOpen = doInit;
    }

    public void open(final ReplicatedEnvironment env) {
        this.env = env;
        StoreConfig config =
            new StoreConfig().setAllowCreate(true).setTransactional(true);
        Mutations mutations = new Mutations();
        EntityModel model = new AnnotationModel();
        if (doInitDuringOpen) {
            setupConfig(mutations, model);
        }
        config.setMutations(mutations);
        config.setModel(model);
        store = new EntityStore(env, "foo", config);
        if (doInitDuringOpen) {
            init();
        }
    }

    protected abstract void setupConfig(final Mutations mutations,
                                        final EntityModel model);

    protected abstract void init();

    public void close() {
        store.close();
    }

    public void adopt(AppInterface other) {
        version = other.getVersion();
        env = other.getEnv();
        store = other.getStore();
        if (doInitDuringOpen) {
            init();
        }
    }

    public ReplicatedEnvironment getEnv() {
        return env;
    }

    public EntityStore getStore() {
        return store;
    }

    public int getVersion() {
        return version;
    }
}
