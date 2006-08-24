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
package org.apache.xbean.terminal.telnet;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TelnetInputStream extends FilterInputStream implements TelnetCodes {
    // state table for what options have been negotiated
    private TelnetOption[] options = new TelnetOption[256];
    private OutputStream out = null;

    /**
     * We haven yet implemented any Telnet options, so we just explicitly
     * disable some common options for safety sake.
     * <p/>
     * Certain Telnet clients (MS Windows Telnet) are enabling options without
     * asking first. Shame, shame, shame.
     *
     * @throws IOException
     */
    public TelnetInputStream(InputStream in, OutputStream out) throws IOException {
        super(in);
        this.out = out;
        negotiateOption(DONT, 1);
        negotiateOption(DONT, 6);
        negotiateOption(DONT, 24);
        negotiateOption(DONT, 33);
        negotiateOption(DONT, 34);
    }

    public int read() throws IOException {
        int b = super.read();
        if (b == IAC) {
            // The cosole has a reference
            // to this input stream
            processCommand();
            // Call read recursively as
            // the next character could
            // also be a command
            b = this.read();
        }
        //System.out.println("B="+b);
        return b;
    }

    /**
     * This is only called by TelnetInputStream
     * it is assumed that the IAC byte has already been read from the stream.
     *
     * @throws IOException
     */
    private void processCommand() throws IOException {
        // Debug statement
        print("C: IAC ");
        int command = super.read();
        switch (command) {
            case WILL:
                senderWillEnableOption(super.read());
                break;
            case DO:
                pleaseDoEnableOption(super.read());
                break;
            case WONT:
                senderWontEnableOption(super.read());
                break;
            case DONT:
                pleaseDontEnableOption(super.read());
                break;
            default:
                unimplementedCommand(command);
                break;
        }
    }

    private void unimplementedCommand(int command) {
        println(command + ": command not found");
    }

    /**
     * Client says: I will enable OptionX
     * <p/>
     * If the sender initiated the negotiation of the
     * option, we must send a reply. Replies can be DO or DON'T.
     *
     * @param optionID
     * @throws IOException
     */
    private void senderWillEnableOption(int optionID) throws IOException {
        // Debug statement
        println("WILL " + optionID);
        TelnetOption option = getOption(optionID);
        if (option.hasBeenNegotiated()) return;
        if (option.isInNegotiation()) {
            option.enable();
        } else if (!option.isInNegotiation() && option.isSupported()) {
            negotiateOption(DO, optionID);
            option.enable();
        } else if (!option.isInNegotiation() && !option.isSupported()) {
            negotiateOption(DONT, optionID);
            option.disable();
        }
    }

    /**
     * Client says: Please, do enable OptionX
     * <p/>
     * If the sender initiated the negotiation of the
     * option, we must send a reply.
     * <p/>
     * Replies can be WILL or WON'T.
     *
     * @param optionID
     * @throws IOException
     */
    private void pleaseDoEnableOption(int optionID) throws IOException {
        // Debug statement
        println("DO " + optionID);
        TelnetOption option = getOption(optionID);
        if (option.hasBeenNegotiated()) return;
        if (option.isInNegotiation()) {
            option.enable();
        } else if (!option.isInNegotiation() && option.isSupported()) {
            negotiateOption(WILL, optionID);
            option.enable();
        } else if (!option.isInNegotiation() && !option.isSupported()) {
            negotiateOption(WONT, optionID);
            option.disable();
        }
    }

    /**
     * Client says: I won't enable OptionX
     * <p/>
     * <p/>
     * If the sender initiated the negotiation of the
     * option, we must send a reply.
     * <p/>
     * Replies can only be DON'T.
     *
     * @param optionID
     * @throws IOException
     */
    private void senderWontEnableOption(int optionID) throws IOException {
        println("WONT " + optionID);
        TelnetOption option = getOption(optionID);
        if (option.hasBeenNegotiated()) return;
        if (!option.isInNegotiation()) {
            negotiateOption(DONT, optionID);
        }
        option.disable();
    }

    /**
     * Client says: Please, don't enable OptionX
     * <p/>
     * If the sender initiated the negotiation of the
     * option, we must send a reply.
     * <p/>
     * Replies can only be WON'T.
     *
     * @param optionID
     * @throws IOException
     */
    private void pleaseDontEnableOption(int optionID) throws IOException {
        // Debug statement
        println("DONT " + optionID);
        TelnetOption option = getOption(optionID);
        if (option.hasBeenNegotiated()) return;
        if (!option.isInNegotiation()) {
            negotiateOption(WONT, optionID);
        }
        option.disable();
    }

    // TODO:0: Replace with actual logging
    private void println(String s) {
        // System.out.println(s);
    }

    // TODO:0: Replace with actual logging
    private void print(String s) {
        // System.out.print(s);
    }

    /**
     * Send an option negitiation command to the client
     *
     * @param negotiate
     * @param optionID
     * @throws IOException
     */
    private void negotiateOption(int negotiate, int optionID)
            throws IOException {
        TelnetOption option = getOption(optionID);
        option.isInNegotiation(true);
        String n = null;
        switch (negotiate) {
            case WILL:
                n = "WILL ";
                break;
            case DO:
                n = "DO ";
                break;
            case WONT:
                n = "WONT ";
                break;
            case DONT:
                n = "DONT ";
                break;
        }
        // Debug statement
        println("S: IAC " + n + optionID);
        synchronized (out) {
            out.write(IAC);
            out.write(negotiate);
            out.write(optionID);
        }
    }

    private TelnetOption getOption(int optionID) {
        TelnetOption opt = options[optionID];
        if (opt == null) {
            opt = new TelnetOption(optionID);
            options[optionID] = opt;
        }
        return opt;
    }
}
