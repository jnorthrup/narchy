/* JRdpLoader.java
 * Component: ProperJavaRDP
 *
 * Revision: $Revision: #2 $
 * Author: $Author: tvkelley $
 * Date: $Date: 2009/09/15 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Launch ProperJavaRDP with settings from a config file
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 *
 * (See gpl.txt for details of the GNU General Public License.)
 *
 */
package net.propero.rdp.loader;

import net.propero.rdp.Rdesktop;
import net.propero.rdp.Utilities_Localised;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;

public class JRdpLoader {

    
    private static final String[] identifiers = {"--user", "--password", "--domain",
            "--fullscreen", "--geometry", "--use_rdp5"};

    
    private static final String[] pairs = {"-u", "-p", "-d", "-f", "-g",
            "--use_rdp5"};

    public static void main(String[] args) {

        if (args.length <= 0) {
            System.err.println("Expected usage: JRdpLoader launchFile");
            System.exit(-1);
        }

        var launchFile = args[0];

        try {
            var outArgs = "";


            var fstream = new FileInputStream(launchFile);
            var in = new DataInputStream(fstream);
            var port = "";
            var server = "";
            while (in.available() != 0) {
                var line = in.readLine();
                var stok = new StringTokenizer(line);
                if (stok.hasMoreTokens()) {
                    var identifier = stok.nextToken();
                    var value = "";
                    while (stok.hasMoreTokens()) {
                        value += stok.nextToken();
                        if (stok.hasMoreTokens())
                            value += " ";
                    }

                    switch (identifier) {
                        case "--server":
                            server = value;
                            break;
                        case "--port":
                            port = value;
                            break;
                        default:
                            var p = getParam(identifier);
                            if (p != null)
                                outArgs += p + ' ' + value + ' ';
                            break;
                    }
                }
            }

            if (!server.isEmpty()) {
                outArgs += server;
                if (!port.isEmpty())
                    outArgs += ':' + port;


                var finArgs = Utilities_Localised.split(outArgs, " ");

                Rdesktop.main(finArgs);
                in.close();
            } else {
                System.err.println("No server name provided");
                System.exit(0);
            }

        } catch (IOException ioe) {
            System.err.println("Launch file could not be read: "
                    + ioe.getMessage());
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static String getParam(String identifier) {
        for (var i = 0; i < identifiers.length; i++) {
            if (identifier.equals(identifiers[i])) {
                return pairs[i];
            }
        }

        return null;
    }

}
