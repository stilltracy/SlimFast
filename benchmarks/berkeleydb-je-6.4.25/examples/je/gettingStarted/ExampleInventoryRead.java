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

// file ExampleInventoryRead

package je.gettingStarted;

import java.io.File;
import java.io.IOException;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryCursor;

public class ExampleInventoryRead {

    private static File myDbEnvPath =
        new File("/tmp/JEDB");

    // Encapsulates the database environment and databases.
    private static MyDbEnv myDbEnv = new MyDbEnv();

    private static TupleBinding inventoryBinding;
    private static EntryBinding vendorBinding;

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
                      true);      // is this environment read-only?

        // Setup our bindings.
        inventoryBinding = new InventoryBinding();
        vendorBinding =
             new SerialBinding(myDbEnv.getClassCatalog(),
                               Vendor.class);

        if (locateItem != null) {
            showItem();
        } else {
            showAllInventory();
        }
    }

    private void showItem() throws DatabaseException {

        SecondaryCursor secCursor = null;
        try {
            // searchKey is the key that we want to find in the
            // secondary db.
            DatabaseEntry searchKey =
                new DatabaseEntry(locateItem.getBytes("UTF-8"));

            // foundKey and foundData are populated from the primary
            // entry that is associated with the secondary db key.
            DatabaseEntry foundKey = new DatabaseEntry();
            DatabaseEntry foundData = new DatabaseEntry();

            // open a secondary cursor
            secCursor =
                myDbEnv.getNameIndexDB().openSecondaryCursor(null, null);

            // Search for the secondary database entry.
            OperationStatus retVal =
                secCursor.getSearchKey(searchKey, foundKey,
                    foundData, LockMode.DEFAULT);

            // Display the entry, if one is found. Repeat until no more
            // secondary duplicate entries are found
            while(retVal == OperationStatus.SUCCESS) {
                Inventory theInventory =
                    (Inventory)inventoryBinding.entryToObject(foundData);
                displayInventoryRecord(foundKey, theInventory);
                retVal = secCursor.getNextDup(searchKey, foundKey,
                    foundData, LockMode.DEFAULT);
            }
        } catch (Exception e) {
            System.err.println("Error on inventory secondary cursor:");
            System.err.println(e.toString());
            e.printStackTrace();
        } finally {
            if (secCursor != null) {
                secCursor.close();
            }
        }
    }

    private void showAllInventory()
        throws DatabaseException {
        // Get a cursor
        Cursor cursor = myDbEnv.getInventoryDB().openCursor(null, null);

        // DatabaseEntry objects used for reading records
        DatabaseEntry foundKey = new DatabaseEntry();
        DatabaseEntry foundData = new DatabaseEntry();

        try { // always want to make sure the cursor gets closed
            while (cursor.getNext(foundKey, foundData,
                        LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                Inventory theInventory =
                    (Inventory)inventoryBinding.entryToObject(foundData);
                displayInventoryRecord(foundKey, theInventory);
            }
        } catch (Exception e) {
            System.err.println("Error on inventory cursor:");
            System.err.println(e.toString());
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    private void displayInventoryRecord(DatabaseEntry theKey,
                                        Inventory theInventory)
        throws DatabaseException {

        DatabaseEntry searchKey = null;
        try {
            String theSKU = new String(theKey.getData(), "UTF-8");
            System.out.println(theSKU + ":");
            System.out.println("\t " + theInventory.getItemName());
            System.out.println("\t " + theInventory.getCategory());
            System.out.println("\t " + theInventory.getVendor());
            System.out.println("\t\tNumber in stock: " +
                theInventory.getVendorInventory());
            System.out.println("\t\tPrice per unit:  " +
                theInventory.getVendorPrice());
            System.out.println("\t\tContact: ");

            searchKey =
                new DatabaseEntry(theInventory.getVendor().getBytes("UTF-8"));
        } catch (IOException willNeverOccur) {}
        DatabaseEntry foundVendor = new DatabaseEntry();

        if (myDbEnv.getVendorDB().get(null, searchKey, foundVendor,
                LockMode.DEFAULT) != OperationStatus.SUCCESS) {
            System.out.println("Could not find vendor: " +
                theInventory.getVendor() + ".");
            System.exit(-1);
        } else {
            Vendor theVendor =
                (Vendor)vendorBinding.entryToObject(foundVendor);
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
                        locateItem = new String(args[++i]);
                        break;
                    default:
                        usage();
                }
            }
        }
    }
}
