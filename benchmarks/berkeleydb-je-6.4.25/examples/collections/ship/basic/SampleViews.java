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

package collections.ship.basic;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.collections.StoredEntrySet;
import com.sleepycat.collections.StoredMap;

/**
 * SampleViews defines the data bindings and collection views for the sample
 * database.
 *
 * @author Mark Hayes
 */
public class SampleViews {

    private StoredMap partMap;
    private StoredMap supplierMap;
    private StoredMap shipmentMap;

    /**
     * Create the data bindings and collection views.
     */
    public SampleViews(SampleDatabase db) {

        // In this sample, the stored key and data entries are used directly
        // rather than mapping them to separate objects. Therefore, no binding
        // classes are defined here and the SerialBinding class is used.
        //
        ClassCatalog catalog = db.getClassCatalog();
        EntryBinding partKeyBinding =
            new SerialBinding(catalog, PartKey.class);
        EntryBinding partDataBinding =
            new SerialBinding(catalog, PartData.class);
        EntryBinding supplierKeyBinding =
            new SerialBinding(catalog, SupplierKey.class);
        EntryBinding supplierDataBinding =
            new SerialBinding(catalog, SupplierData.class);
        EntryBinding shipmentKeyBinding =
            new SerialBinding(catalog, ShipmentKey.class);
        EntryBinding shipmentDataBinding =
            new SerialBinding(catalog, ShipmentData.class);

        // Create map views for all stores and indices.
        // StoredSortedMap is not used since the stores and indices are
        // ordered by serialized key objects, which do not provide a very
        // useful ordering.
        //
        partMap =
            new StoredMap(db.getPartDatabase(),
                          partKeyBinding, partDataBinding, true);
        supplierMap =
            new StoredMap(db.getSupplierDatabase(),
                          supplierKeyBinding, supplierDataBinding, true);
        shipmentMap =
            new StoredMap(db.getShipmentDatabase(),
                          shipmentKeyBinding, shipmentDataBinding, true);
    }

    // The views returned below can be accessed using the java.util.Map or
    // java.util.Set interfaces, or using the StoredMap and StoredEntrySet
    // classes, which provide additional methods.  The entry sets could be
    // obtained directly from the Map.entrySet() method, but convenience
    // methods are provided here to return them in order to avoid down-casting
    // elsewhere.

    /**
     * Return a map view of the part storage container.
     */
    public final StoredMap getPartMap() {

        return partMap;
    }

    /**
     * Return a map view of the supplier storage container.
     */
    public final StoredMap getSupplierMap() {

        return supplierMap;
    }

    /**
     * Return a map view of the shipment storage container.
     */
    public final StoredMap getShipmentMap() {

        return shipmentMap;
    }

    /**
     * Return an entry set view of the part storage container.
     */
    public final StoredEntrySet getPartEntrySet() {

        return (StoredEntrySet) partMap.entrySet();
    }

    /**
     * Return an entry set view of the supplier storage container.
     */
    public final StoredEntrySet getSupplierEntrySet() {

        return (StoredEntrySet) supplierMap.entrySet();
    }

    /**
     * Return an entry set view of the shipment storage container.
     */
    public final StoredEntrySet getShipmentEntrySet() {

        return (StoredEntrySet) shipmentMap.entrySet();
    }
}
