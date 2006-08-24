/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.command;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Vector;


public class CommandShell implements Command {

    private final String prompt;

    public CommandShell(String serverName) {
        serverName = serverName.toLowerCase();
        prompt = TTY_Reset + TTY_Bright + "["+serverName+"]$ " + TTY_Reset;
    }

    private boolean stop = false;
    private int rc = 0;

    public static final char ESC = (char) 27;
    public static final String TTY_Reset = ESC + "[0m";
    public static final String TTY_Bright = ESC + "[1m";
    public static final String TTY_Dim = ESC + "[2m";
    public static final String TTY_Underscore = ESC + "[4m";
    public static final String TTY_Blink = ESC + "[5m";
    public static final String TTY_Reverse = ESC + "[7m";
    public static final String TTY_Hidden = ESC + "[8m";
    /* Foreground Colors */
    public static final String TTY_FG_Black = ESC + "[30m";
    public static final String TTY_FG_Red = ESC + "[31m";
    public static final String TTY_FG_Green = ESC + "[32m";
    public static final String TTY_FG_Yellow = ESC + "[33m";
    public static final String TTY_FG_Blue = ESC + "[34m";
    public static final String TTY_FG_Magenta = ESC + "[35m";
    public static final String TTY_FG_Cyan = ESC + "[36m";
    public static final String TTY_FG_White = ESC + "[37m";
    /* Background Colors */
    public static final String TTY_BG_Black = ESC + "[40m";
    public static final String TTY_BG_Red = ESC + "[41m";
    public static final String TTY_BG_Green = ESC + "[42m";
    public static final String TTY_BG_Yellow = ESC + "[43m";
    public static final String TTY_BG_Blue = ESC + "[44m";
    public static final String TTY_BG_Magenta = ESC + "[45m";
    public static final String TTY_BG_Cyan = ESC + "[46m";
    public static final String TTY_BG_White = ESC + "[47m";

    public int main(String[] args, InputStream input, PrintStream out) {

        DataInputStream in = new DataInputStream(input);
        while (!stop) {
            prompt(in, out);
        }
        return rc;
    }

    protected void prompt(DataInputStream in, PrintStream out) {
        try {
            out.print(prompt);
            out.flush();

            String commandline = in.readLine();
            if( commandline == null ) {
                this.stop = true;
                return;
            }
            commandline = commandline.trim();
            if (commandline.length() < 1) {
                return;
            }

            String command = commandline;

            StringTokenizer cmdstr = new StringTokenizer(command);
            command = cmdstr.nextToken();

            // Get parameters
            Vector p = new Vector();
            while ( cmdstr.hasMoreTokens() ) {
                p.add(cmdstr.nextToken());
            }
            String[] args = new String[p.size()];
            p.copyInto(args);

            Command cmd = CommandRegistry.getCommand(command);

            if (cmd == null) {
                out.print(command);
                out.println(": command not found");
            } else {
                cmd.main(args, in, out);
            }
        } catch (UnsupportedOperationException e) {
            this.rc=-1;
            this.stop = true;
        } catch (Throwable e) {
            e.printStackTrace(out);
            this.rc=-1;
            this.stop = true;
        }
    }

    protected void badCommand(DataInputStream in, PrintStream out) throws IOException {
        //asdf: command not found
    }

}
