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

package com.sleepycat.persist.evolve;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

/**
 * A collection of mutations for configuring class evolution.
 *
 * <p>Mutations are configured when a store is opened via {@link
 * StoreConfig#setMutations StoreConfig.setMutations}.  For example:</p>
 *
 * <pre class="code">
 *  Mutations mutations = new Mutations();
 *  // Add mutations...
 *  StoreConfig config = new StoreConfig();
 *  config.setMutations(mutations);
 *  EntityStore store = new EntityStore(env, "myStore", config);</pre>
 *
 * <p>Mutations cause data conversion to occur lazily as instances are read
 * from the store.  The {@link EntityStore#evolve EntityStore.evolve} method
 * may also be used to perform eager conversion.</p>
 *
 * <p>Not all incompatible class changes can be handled via mutations.  For
 * example, complex refactoring may require a transformation that manipulates
 * multiple entity instances at once.  Such changes are not possible with
 * mutations but can made by performing a <a
 * href="package-summary.html#storeConversion">store conversion</a>.</p>
 *
 * @see com.sleepycat.persist.evolve Class Evolution
 * @author Mark Hayes
 */
public class Mutations implements Serializable {

    private static final long serialVersionUID = -1744401530444812916L;

    private Map<Mutation, Renamer> renamers;
    private Map<Mutation, Deleter> deleters;
    private Map<Mutation, Converter> converters;

    /**
     * Creates an empty set of mutations.
     */
    public Mutations() {
        renamers = new HashMap<Mutation, Renamer>();
        deleters = new HashMap<Mutation, Deleter>();
        converters = new HashMap<Mutation, Converter>();
    }

    /**
     * Returns true if no mutations are present.
     *
     * @return true if no mutations are present.
     */
    public boolean isEmpty() {
        return renamers.isEmpty() &&
               deleters.isEmpty() &&
               converters.isEmpty();
    }

    /**
     * Adds a renamer mutation.
     *
     * @param renamer the Renamer.
     */
    public void addRenamer(Renamer renamer) {
        renamers.put(new Key(renamer), renamer);
    }

    /**
     * Returns the renamer mutation for the given class, version and field, or
     * null if none exists.  A null field name should be specified to get a
     * class renamer.
     *
     * @param className the class name.
     *
     * @param classVersion the class version.
     *
     * @param fieldName the field name in the given class version.
     *
     * @return the Renamer, or null.
     */
    public Renamer getRenamer(String className,
                              int classVersion,
                              String fieldName) {
        return renamers.get(new Key(className, classVersion, fieldName));
    }

    /**
     * Returns an unmodifiable collection of all renamer mutations.
     *
     * @return the renamers.
     */
    public Collection<Renamer> getRenamers() {
        return renamers.values();
    }

    /**
     * Adds a deleter mutation.
     *
     * @param deleter the Deleter.
     */
    public void addDeleter(Deleter deleter) {
        deleters.put(new Key(deleter), deleter);
    }

    /**
     * Returns the deleter mutation for the given class, version and field, or
     * null if none exists.  A null field name should be specified to get a
     * class deleter.
     *
     * @param className the class name.
     *
     * @param classVersion the class version.
     *
     * @param fieldName the field name.
     *
     * @return the Deleter, or null.
     */
    public Deleter getDeleter(String className,
                              int classVersion,
                              String fieldName) {
        return deleters.get(new Key(className, classVersion, fieldName));
    }

    /**
     * Returns an unmodifiable collection of all deleter mutations.
     *
     * @return the deleters.
     */
    public Collection<Deleter> getDeleters() {
        return deleters.values();
    }

    /**
     * Adds a converter mutation.
     *
     * @param converter the Converter.
     */
    public void addConverter(Converter converter) {
        converters.put(new Key(converter), converter);
    }

    /**
     * Returns the converter mutation for the given class, version and field,
     * or null if none exists.  A null field name should be specified to get a
     * class converter.
     *
     * @param className the class name.
     *
     * @param classVersion the class version.
     *
     * @param fieldName the field name.
     *
     * @return the Converter, or null.
     */
    public Converter getConverter(String className,
                                  int classVersion,
                                  String fieldName) {
        return converters.get(new Key(className, classVersion, fieldName));
    }

    /**
     * Returns an unmodifiable collection of all converter mutations.
     *
     * @return the converters.
     */
    public Collection<Converter> getConverters() {
        return converters.values();
    }

    private static class Key extends Mutation {
        static final long serialVersionUID = 2793516787097085621L;

        Key(String className, int classVersion, String fieldName) {
            super(className, classVersion, fieldName);
        }

        Key(Mutation mutation) {
            super(mutation.getClassName(),
                  mutation.getClassVersion(),
                  mutation.getFieldName());
        }
    }

    /**
     * Returns true if this collection has the same set of mutations as the
     * given collection and all mutations are equal.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Mutations) {
            Mutations o = (Mutations) other;
            return renamers.equals(o.renamers) &&
                   deleters.equals(o.deleters) &&
                   converters.equals(o.converters);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return renamers.hashCode() +
               deleters.hashCode() +
               converters.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (renamers.size() > 0) {
            buf.append(renamers.values());
        }
        if (deleters.size() > 0) {
            buf.append(deleters.values());
        }
        if (converters.size() > 0) {
            buf.append(converters.values());
        }
        if (buf.length() > 0) {
            return buf.toString();
        } else {
            return "[Empty Mutations]";
        }
    }
}
