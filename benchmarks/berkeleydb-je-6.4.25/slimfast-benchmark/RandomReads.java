
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.*;

/**
 * A simple benchmark that reads a given number of random keys from the database.
 *
 * Can also insert the given number of keys into a database, with values of the given size.
 *
 * Based on Oracle's example code in SimpleExample.java and MeasureInsertSize.java
 */
class RandomReads {
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 1;

    private enum Operation { INSERT, RANDOMREAD };

    private static final String OUTPUT_TAG = "[randomreads] ";

    final private Operation op; // the operation to perform
    final private int numRecords;   // num records to insert or retrieve
    final private int valueSizeBytes; // size of values, in bytes
    final private File envDir;
    final private int maxKey; // the max key value used when generating random reads

    public RandomReads(Operation op, File envDir, int numRecords, int valueSizeBytes, int maxKey) {
        this.numRecords = numRecords;
        this.op = op;
        this.envDir = envDir;
        this.valueSizeBytes = valueSizeBytes;
        this.maxKey = maxKey;
    }

    /**
     * Usage string
     */
    public static void usage() {
        System.out.println("usage is one of:");
        System.out.println(" java " + RandomReads.class.getName() + " insert <dbEnvHomeDirectory> <numKeysToInsert> <valueSizeInBytes>");
        System.out.println(" java " + RandomReads.class.getName() + " randomread <dbEnvHomeDirectory> <numKeysToRead> <maxKeyValue>");
        System.exit(EXIT_FAILURE);
    }

    /**
     * Main
     */
    public static void main(String argv[]) {
        if (argv.length != 4) {
            usage();
            return;
        }

        File envHomeDirectory = new File(argv[1]);
        int numKeysToInsert = Integer.parseInt(argv[2]);
        int maxKeyValue = 0;
        int valueSizeInBytes = 0;

        final Operation op;
        if (argv[0].equals("insert")) {
            op = Operation.INSERT;
            valueSizeInBytes = Integer.parseInt(argv[3]);

        } else if (argv[0].equals("randomread")) {
            op = Operation.RANDOMREAD;
            maxKeyValue = Integer.parseInt(argv[3]);

        } else {
            op = null;
            System.out.println("Invalid operation: "+argv[0]);
            System.out.print("Must be one of ");
            for (Operation o : Operation.values()) {
                System.out.print(o + " ");
            }
            System.out.println("");
            System.exit(EXIT_FAILURE);
        }

        try {
            RandomReads app = new RandomReads(op, envHomeDirectory, numKeysToInsert, valueSizeInBytes, maxKeyValue);
            app.run();

        } catch (DatabaseException e) {
            e.printStackTrace();
            System.exit(EXIT_FAILURE);
        }

        System.exit(EXIT_SUCCESS);
    }

    /**
     * Insert or retrieve data
     */
    public void run() throws DatabaseException {
        /* Create a new, transactional database environment */
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);

        envConfig.setCachePercent(90 /*percent*/);

        Environment exampleEnv = new Environment(envDir, envConfig);

        /*
         * Make a database within that environment
         *
         * Notice that we use an explicit transaction to
         * perform this database open, and that we
         * immediately commit the transaction once the
         * database is opened. This is required if we
         * want transactional support for the database.
         * However, we could have used autocommit to
         * perform the same thing by simply passing a
         * null txn handle to openDatabase().
         */
        Transaction txn = null; //exampleEnv.beginTransaction(null, null);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(true);
        dbConfig.setDeferredWrite(false);
        Database exampleDb = exampleEnv.openDatabase(null, "simpleDb", dbConfig);
        //txn.commit();

        System.out.println(OUTPUT_TAG+"starting up!");

        /*
         * Insert or retrieve data. In our example, database records are
         * integer pairs.
         */

        /* DatabaseEntry represents the key and data of each record */
        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = new DatabaseEntry(new byte[valueSizeBytes]);

        if (Operation.INSERT == op) {

            txn = null;
            /* put some data in */
            for (int i = 0; i < numRecords; i++) {
                /*
                 * Note that autocommit mode, described in the Getting
                 * Started Guide, is an alternative to explicitly
                 * creating the transaction object.
                 */
                //txn = exampleEnv.beginTransaction(null, null);

                /* Use a binding to convert the int into a DatabaseEntry. */

                IntegerBinding.intToEntry(i, keyEntry);
                OperationStatus status = exampleDb.put(txn, keyEntry, dataEntry);

                /*
                 * Note that put will throw a DatabaseException when
                 * error conditions are found such as deadlock.
                 * However, the status return conveys a variety of
                 * information. For example, the put might succeed,
                 * or it might not succeed if the record alread exists
                 * and the database was not configured for duplicate
                 * records.
                 */
                if (status != OperationStatus.SUCCESS) {
                    throw new RuntimeException("Data insertion got status " +
                                               status);
                }
                //txn.commit();
                
                if ( i > 0 && 0 == i % 1000 ) System.out.println(OUTPUT_TAG+"Inserted datum "+i);
            }

            System.out.println(OUTPUT_TAG+"Completed insertion of "+numRecords+" records");

        } else if (Operation.RANDOMREAD == op) {
            /* perform random reads */
            System.out.println(OUTPUT_TAG+"Opening cursor...");
            Cursor cursor = exampleDb.openCursor(null, null);
            System.out.println(OUTPUT_TAG+"Retrieving data...");

            final long startTime = System.currentTimeMillis();
            long prevIntervalStart = startTime;
            final int INTERVAL_SIZE = 100;

            for (int i = 0; i < numRecords; i++) {
                int r = i % maxKey; //ThreadLocalRandom.current().nextInt(0, maxKey);
                IntegerBinding.intToEntry(r, keyEntry);
                OperationStatus status = cursor.getSearchKey(keyEntry, dataEntry, null);

                if (status != OperationStatus.SUCCESS) {
                    /*
                    System.out.println("key=" +
                                       IntegerBinding.entryToInt(keyEntry) +
                                       " data=" +
                                       IntegerBinding.entryToInt(dataEntry));
                    */
                    System.out.println(OUTPUT_TAG+"Read error: " + status + " key:" + r);
                }

                if ( i > 0 && 0 == i % INTERVAL_SIZE ) {
                    final long now = System.currentTimeMillis();
                    final double total_s = (now - startTime) / 1000.0; 
                    final double interval_s = (now - prevIntervalStart) / 1000.0; 
                    StatsConfig sc = new StatsConfig();
                    sc.setClear(true);
                    sc.setFast(true);
                    EnvironmentStats stats = exampleEnv.getStats(sc);
                    double cacheSizeGB = stats.getCacheTotalBytes() / 1000000000.0;
                    long cacheMisses = stats.getNCacheMiss();
                    long randReads = stats.getNRandomReads();
                    String statsStr = String.format(" %.2f read/s (%.2f overall), cache using %.3f GB, %d cache misses, %d random reads", 
                                                    INTERVAL_SIZE / interval_s, i / total_s, cacheSizeGB, cacheMisses, randReads);
                    System.out.println(OUTPUT_TAG+"Completed read "+i+ statsStr);
                    prevIntervalStart = System.currentTimeMillis();
                }
            }
            cursor.close();
        }

        exampleDb.close();
        exampleEnv.close();

    }
}
