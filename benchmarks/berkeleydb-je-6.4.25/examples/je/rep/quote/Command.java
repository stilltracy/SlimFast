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

import java.util.StringTokenizer;

/**
 * An enumeration of the commands used by the stock quotes example.
 */
enum Command {

    PRINT(true), /* Prints all the stocks currently in the database */
    UPDATE,      /* Update the info associated with the stock */
    QUIT(true),  /* Quit the application */
    NONE;        /* An internal pseudo command indicating no command */

    /* Indicates whether the command is manifest, that is its the enum name
       itself.  */
    final private boolean manifest;

    Command(boolean manifest) {
        this.manifest = manifest;
    }

    /**
     * A non-manifest command
     */
    Command() {
        this(false);
    }

    /**
     * Determines the command denoted by the line.
     *
     * @param line the text as typed in at the console.
     *
     * @return the command represented by the line, or NONE if the line is
     * empty.
     *
     * @throws InvalidCommandException if no recognizable command was found on
     * a non-empty line.
     */
    static Command getCommand(String line) throws InvalidCommandException {
        StringTokenizer tokenizer = new StringTokenizer(line);
        if (!tokenizer.hasMoreTokens()) {
            return NONE;
        }
        String command = tokenizer.nextToken();

        /* Check for a manifest command */
        for (Command c : Command.values()) {
            if (c.manifest && c.name().equalsIgnoreCase(command)) {
                if (!tokenizer.hasMoreTokens()) {
                    return c;
                }
                /* Extra token. */
                throw new InvalidCommandException(
                        "Unexpected argument: "  + tokenizer.nextToken() +
                        " for command: " + command);
            }
        }
        /* A stock update command, token following arg must be a price*/
        if (!tokenizer.hasMoreTokens()) {
            throw new InvalidCommandException("Unknown command: " + command +
                                              "\n" + StockQuotes.usage());
        }
        String price = tokenizer.nextToken();

        try {
            Float.parseFloat(price);
            if (tokenizer.hasMoreTokens()) {
                throw new InvalidCommandException
                    ("Extraneous argument: " + tokenizer.nextToken());
            }
        } catch (NumberFormatException e) {
            throw new InvalidCommandException
                ("Stock price must be a numeric value, not: " + price);
        }

        return UPDATE;
    }

    @SuppressWarnings("serial")
    static class InvalidCommandException extends Exception {
        InvalidCommandException(String error) {
            super(error);
        }
    }
}
