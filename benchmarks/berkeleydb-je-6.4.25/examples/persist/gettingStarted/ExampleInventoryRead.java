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

package persist.gettingStarted;

import java.io.File;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;

public class ExampleInventoryRead {

    private static File myDbEnvPath =
        new File("/tmp/JEDB");

    private DataAccessor da;

    // Encapsulates the database environment.
    private static MyDbEnv myDbEnv = new MyDbEnv();

    // The item to locate if the -s switch is used
    private static String locateItem;

    private static void usage() {
        System.out.println("ExampleInventoryRead [-h <env directory>]" +
                           "[-s <item to locate>]");
        System.exit(-1);
    }

    public static void main(String args[]) {
        ExampleInventoryRead eir = new ExampleInventoryRead();
        try {
            eir.run(args);
        } catch (DatabaseException dbe) {
            System.err.println("ExampleInventoryRead: " + dbe.toString());
            dbe.printStackTrace();
        } finally {
            myDbEnv.close();
        }
        System.out.println("All done.");
    }

    private void run(String args[])
        throws DatabaseException {
        // Parse the arguments list
        parseArgs(args);

        myDbEnv.setup(myDbEnvPath, // path to the environment home
                      true);       // is this environment read-only?

        // Open the data accessor. This is used to retrieve
        // persistent objects.
        da = new DataAccessor(myDbEnv.getEntityStore());

        // If a item to locate is provided on the command line,
        // show just the inventory items using the provided name.
        // Otherwise, show everything in the inventory.
        if (locateItem != null) {
            showItem();
        } else {
            showAllInventory();
        }
    }

    // Shows all the inventory items that exist for a given
    // inventory name.
    private void showItem() throws DatabaseException {

        // Use the inventory name secondary key to retrieve
        // these objects.
        EntityCursor<Inventory> items =
            da.inventoryByName.subIndex(locateItem).entities();
        try {
            for (Inventory item : items) {
                displayInventoryRecord(item);
            }
        } finally {
            items.close();
        }
    }

    // Displays all the inventory items in the store
    private void showAllInventory()
        throws DatabaseException {

        // Get a cursor that will walk every
        // inventory object in the store.
        EntityCursor<Inventory> items =
            da.inventoryBySku.entities();

        try {
            for (Inventory item : items) {
                displayInventoryRecord(item);
            }
        } finally {
            items.close();
        }
    }

    private void displayInventoryRecord(Inventory theInventory)
            throws DatabaseException {

            System.out.println(theInventory.getSku() + ":");
            System.out.println("\t " + theInventory.getItemName());
            System.out.println("\t " + theInventory.getCategory());
            System.out.println("\t " + theInventory.getVendor());
            System.out.println("\t\tNumber in stock: " +
                theInventory.getVendorInventory());
            System.out.println("\t\tPrice per unit:  " +
                theInventory.getVendorPrice());
            System.out.println("\t\tContact: ");

            Vendor theVendor =
                    da.vendorByName.get(theInventory.getVendor());
            assert theVendor != null;

            System.out.println("\t\t " + theVendor.getAddress());
            System.out.println("\t\t " + theVendor.getCity() + ", " +
                theVendor.getState() + " " + theVendor.getZipcode());
            System.out.println("\t\t Business Phone: " +
                theVendor.getBusinessPhoneNumber());
            System.out.println("\t\t Sales Rep: " +
                                theVendor.getRepName());
            System.out.println("\t\t            " +
                theVendor.getRepPhoneNumber());
    }

    protected ExampleInventoryRead() {}

    private static void parseArgs(String args[]) {
        for(int i = 0; i < args.length; ++i) {
            if (args[i].startsWith("-")) {
                switch(args[i].charAt(1)) {
                    case 'h':
                        myDbEnvPath = new File(args[++i]);
                        break;
                    case 's':
                        locateItem = args[++i];
                        break;
                    default:
                        usage();
                }
            }
        }
    }
}
