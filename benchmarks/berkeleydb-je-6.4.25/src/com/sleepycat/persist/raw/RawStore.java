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

package com.sleepycat.persist.raw;

import java.io.Closeable;

import com.sleepycat.compat.DbCompat;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.StoreExistsException;
import com.sleepycat.persist.StoreNotFoundException;
import com.sleepycat.persist.evolve.IncompatibleClassException;
import com.sleepycat.persist.evolve.Mutations;
import com.sleepycat.persist.impl.Store;
import com.sleepycat.persist.model.EntityModel;

/**
 * Provides access to the raw data in a store for use by general purpose tools.
 * A <code>RawStore</code> provides access to stored entities without using
 * entity classes or key classes.  Keys are represented as simple type objects
 * or, for composite keys, as {@link RawObject} instances, and entities are
 * represented as {@link RawObject} instances.
 *
 * <p>{@code RawStore} objects are thread-safe.  Multiple threads may safely
 * call the methods of a shared {@code RawStore} object.</p>
 *
 * <p>When using a {@code RawStore}, the current persistent class definitions
 * are not used.  Instead, the previously stored metadata and class definitions
 * are used.  This has several implications:</p>
 * <ol>
 * <li>An {@code EntityModel} may not be specified using {@link
 * StoreConfig#setModel}.  In other words, the configured model must be
 * null (the default).</li>
 * <li>When storing entities, their format will not automatically be evolved
 * to the current class definition, even if the current class definition has
 * changed.</li>
 * </ol>
 *
 * @author Mark Hayes
 */
public class RawStore
    /* <!-- begin JE only --> */
    implements Closeable
    /* <!-- end JE only --> */
    {

    private Store store;

    /**
     * Opens an entity store for raw data access.
     *
     * @param env an open Berkeley DB environment.
     *
     * @param storeName the name of the entity store within the given
     * environment.
     *
     * @param config the store configuration, or null to use default
     * configuration properties.
     *
     * @throws StoreNotFoundException when the {@link
     * StoreConfig#setAllowCreate AllowCreate} configuration parameter is false
     * and the store's internal catalog database does not exist.
     *
     * @throws IllegalArgumentException if the <code>Environment</code> is
     * read-only and the <code>config ReadOnly</code> property is false.
     *
     * @throws DatabaseException the base class for all BDB exceptions.
     */
    public RawStore(Environment env, String storeName, StoreConfig config)
        throws StoreNotFoundException, DatabaseException {

        try {
            store = new Store(env, storeName, config, true /*rawAccess*/);
        } catch (StoreExistsException e) {
            /* Should never happen, ExclusiveCreate not used. */
            throw DbCompat.unexpectedException(e);
        } catch (IncompatibleClassException e) {
            /* Should never happen, evolution is not performed. */
            throw DbCompat.unexpectedException(e);
        }
    }

    /**
     * Opens the primary index for a given entity class.
     *
     * @param entityClass the name of the entity class.
     *
     * @return the PrimaryIndex.
     *
     * @throws DatabaseException the base class for all BDB exceptions.
     */
    public PrimaryIndex<Object, RawObject> getPrimaryIndex(String entityClass)
        throws DatabaseException {

        return store.getPrimaryIndex
            (Object.class, null, RawObject.class, entityClass);
    }

    /**
     * Opens the secondary index for a given entity class and secondary key
     * name.
     *
     * @param entityClass the name of the entity class.
     *
     * @param keyName the secondary key name.
     *
     * @return the SecondaryIndex.
     *
     * @throws DatabaseException the base class for all BDB exceptions.
     */
    public SecondaryIndex<Object, Object, RawObject>
        getSecondaryIndex(String entityClass, String keyName)
        throws DatabaseException {

        return store.getSecondaryIndex
            (getPrimaryIndex(entityClass), RawObject.class, entityClass,
             Object.class, null, keyName);
    }

    /**
     * Returns the environment associated with this store.
     *
     * @return the Environment.
     */
    public Environment getEnvironment() {
        return store.getEnvironment();
    }

    /**
     * Returns a copy of the entity store configuration.
     *
     * @return the StoreConfig.
     */
    public StoreConfig getConfig() {
        return store.getConfig();
    }

    /**
     * Returns the name of this store.
     *
     * @return the store name.
     */
    public String getStoreName() {
        return store.getStoreName();
    }

    /**
     * Returns the last configured and stored entity model for this store.
     *
     * @return the EntityModel.
     */
    public EntityModel getModel() {
        return store.getModel();
    }

    /**
     * Returns the set of mutations that were configured and stored previously.
     *
     * @return the Mutations.
     */
    public Mutations getMutations() {
        return store.getMutations();
    }

    /**
     * Closes all databases and sequences that were opened by this model.  No
     * databases opened via this store may be in use.
     *
     * <p>WARNING: To guard against memory leaks, the application should
     * discard all references to the closed handle.  While BDB makes an effort
     * to discard references from closed objects to the allocated memory for an
     * environment, this behavior is not guaranteed.  The safe course of action
     * for an application is to discard all references to closed BDB
     * objects.</p>
     *
     * @throws DatabaseException the base class for all BDB exceptions.
     */
    public void close()
        throws DatabaseException {

        store.close();
    }
}
