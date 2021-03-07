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

package je.rep.quote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.StringTokenizer;

import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

/**
 * Common utility methods for the StockQuote examples.
 */
class QuoteUtil {

    /**
     * Opens a transactional EntityStore in the given replicated environment.
     */
    static EntityStore openEntityStore(ReplicatedEnvironment env,
                                       String storeName) {

        final StoreConfig storeConfig = new StoreConfig();

        /* An Entity Store in a replicated environment must be transactional.*/
        storeConfig.setTransactional(true);

        /* Note that both Master and Replica open the store for write. */
        storeConfig.setReadOnly(false);
        storeConfig.setAllowCreate(true);

        return new EntityStore(env, storeName, storeConfig);
    }

    /**
     * Display a prompt for this node. If this node accepts input, read and
     * return the input.
     *
     * @param name a descriptive string for the prompt
     *
     * @param nodeName the name or null, if the prompt is not from a rep node
     *
     * @param isMaster true if the node is currently the master
     *
     * @param stdin   the Reader providing command input
     *
     * @return the string that was typed in, in response to the prompt.
     *
     * @throws IOException
     */
    static String promptAndRead(String name,
                                String nodeName,
                                boolean isMaster,
                                PrintStream promptStream,
                                BufferedReader stdin)
        throws IOException {

        if (promptStream != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            if (nodeName != null) {
                sb.append("-").append(nodeName).append(" ");
                if (isMaster) {
                    sb.append("(master)");
                } else {
                    sb.append("(replica)");
                }
            }
            promptStream.print(sb.toString());
            promptStream.print("> ");
        }
        return stdin.readLine();
    }

    /**
     * Forwards the request line to the target and prints out the results
     * of the command at the current console.
     *
     * @param target the socket on which the application is listening
     *
     * @param commandLine the command to be executed on the remote target
     *
     * @param printStream the stream used to capture the output from the
     * forwarded request
     */
    static void forwardRequest(InetSocketAddress target,
                               String commandLine,
                               PrintStream printStream)
        throws IOException {

        /* Open a connection to the current master. */
        Socket socket = new Socket();
        PrintStream out = null;
        BufferedReader in = null;
        try {
            socket.connect(target);
            out = new PrintStream(socket.getOutputStream(), true);
            out.println(commandLine);
            in = new BufferedReader(new InputStreamReader(socket
                    .getInputStream()));
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                printStream.println(line);
            }
        } finally {
            QuoteUtil.closeSocketAndStreams(socket, in, out);
        }
    }

    /**
     * Utility to close socket and its streams.
     *
     * @param socket to be closed
     * @param in input reader to be closed
     * @param out output stream to be closed
     */
    static void closeSocketAndStreams(Socket socket,
                                      BufferedReader in,
                                      PrintStream out) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            // Ignore exceptions during cleanup
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (RuntimeException e) {
            // Ignore exceptions during cleanup
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore exceptions during cleanup
        }
    }

    /**
     * Parses a line to return a new Quote.
     *
     * @param line the line containing the quote
     *
     * @return a Quote
     */
    static Quote parseQuote(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);
        String stockSymbol = tokenizer.nextToken();
        float stockValue = Float.parseFloat(tokenizer.nextToken());

        return new Quote(stockSymbol, stockValue);
    }
}
