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

package collections.ship.factory;

import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.collections.StoredSortedValueSet;
import com.sleepycat.collections.TupleSerialFactory;

/**
 * SampleViews defines the data bindings and collection views for the sample
 * database.
 *
 * @author Mark Hayes
 */
public class SampleViews {

    private StoredSortedMap partMap;
    private StoredSortedMap supplierMap;
    private StoredSortedMap shipmentMap;
    private StoredSortedMap shipmentByPartMap;
    private StoredSortedMap shipmentBySupplierMap;
    private StoredSortedMap supplierByCityMap;

    /**
     * Create the data bindings and collection views.
     */
    public SampleViews(SampleDatabase db) {

        // Use the TupleSerialFactory for a Serial/Tuple-based database
        // where marshalling interfaces are used.
        //
        TupleSerialFactory factory = db.getFactory();

        // Create map views for all stores and indices.
        // StoredSortedMap is used since the stores and indices are ordered
        // (they use the DB_BTREE access method).
        //
        partMap =
            factory.newSortedMap(db.getPartDatabase(),
                                 PartKey.class, Part.class, true);
        supplierMap =
            factory.newSortedMap(db.getSupplierDatabase(),
                                 SupplierKey.class, Supplier.class, true);
        shipmentMap =
            factory.newSortedMap(db.getShipmentDatabase(),
                                 ShipmentKey.class, Shipment.class, true);
        shipmentByPartMap =
            factory.newSortedMap(db.getShipmentByPartDatabase(),
                                 PartKey.class, Shipment.class, true);
        shipmentBySupplierMap =
            factory.newSortedMap(db.getShipmentBySupplierDatabase(),
                                 SupplierKey.class, Shipment.class, true);
        supplierByCityMap =
            factory.newSortedMap(db.getSupplierByCityDatabase(),
                                 String.class, Supplier.class, true);
    }

    // The views returned below can be accessed using the java.util.Map or
    // java.util.Set interfaces, or using the StoredMap and StoredValueSet
    // classes, which provide additional methods.  The entity sets could be
    // obtained directly from the Map.values() method but convenience methods
    // are provided here to return them in order to avoid down-casting
    // elsewhere.

    /**
     * Return a map view of the part storage container.
     */
    public StoredSortedMap getPartMap() {

        return partMap;
    }

    /**
     * Return a map view of the supplier storage container.
     */
    public StoredSortedMap getSupplierMap() {

        return supplierMap;
    }

    /**
     * Return a map view of the shipment storage container.
     */
    public StoredSortedMap getShipmentMap() {

        return shipmentMap;
    }

    /**
     * Return an entity set view of the part storage container.
     */
    public StoredSortedValueSet getPartSet() {

        return (StoredSortedValueSet) partMap.values();
    }

    /**
     * Return an entity set view of the supplier storage container.
     */
    public StoredSortedValueSet getSupplierSet() {

        return (StoredSortedValueSet) supplierMap.values();
    }

    /**
     * Return an entity set view of the shipment storage container.
     */
    public StoredSortedValueSet getShipmentSet() {

        return (StoredSortedValueSet) shipmentMap.values();
    }

    /**
     * Return a map view of the shipment-by-part index.
     */
    public StoredSortedMap getShipmentByPartMap() {

        return shipmentByPartMap;
    }

    /**
     * Return a map view of the shipment-by-supplier index.
     */
    public StoredSortedMap getShipmentBySupplierMap() {

        return shipmentBySupplierMap;
    }

    /**
     * Return a map view of the supplier-by-city index.
     */
    public StoredSortedMap getSupplierByCityMap() {

        return supplierByCityMap;
    }
}
