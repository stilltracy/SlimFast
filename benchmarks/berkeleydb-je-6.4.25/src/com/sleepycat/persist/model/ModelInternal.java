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

package com.sleepycat.persist.model;

import com.sleepycat.persist.impl.PersistCatalog;

/**
 * <!-- begin JE only -->
 * @hidden
 * <!-- end JE only -->
 * Internal access class that should not be used by applications.
 *
 * @author Mark Hayes
 */
public class ModelInternal {

    /**
     * Internal access method that should not be used by applications.
     *
     * @param model the EntityModel.
     * @param catalog the PersistCatalog.
     */
    public static void setCatalog(EntityModel model, PersistCatalog catalog) {
        model.setCatalog(catalog);
    }

    /**
     * Internal access method that should not be used by applications.
     *
     * @param model the EntityModel.
     * @param loader the ClassLoader.
     */
    public static void setClassLoader(EntityModel model, ClassLoader loader) {
        /* Do not overwrite loader with null value. */
        if (loader != null) {
            model.setClassLoader(loader);
        }
    }

    /**
     * Internal access method that should not be used by applications.
     *
     * @param model the EntityModel.
     * @return the ClassLoader.
     */
    public static ClassLoader getClassLoader(EntityModel model) {
        return model.getClassLoader();
    }
}
