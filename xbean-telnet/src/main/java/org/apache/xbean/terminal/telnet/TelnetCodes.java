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

public interface TelnetCodes {
    /**
     * End of subnegotiation parameters.
     * <p/>
     * Name: SE
     * Code: 240
     */
    public static final int SE = 240;
    /**
     * No operation.
     * <p/>
     * Name: NOP
     * Code: 241
     */
    public static final int NOP = 241;
    /**
     * The data stream portion of a Synch.
     * This should always be accompanied
     * by a TCP Urgent notification.
     * <p/>
     * Name: Data Mark
     * Code: 242
     */
    public static final int Data_Mark = 242;
    /**
     * NVT character BRK.
     * <p/>
     * Name: Break
     * Code: 243
     */
    public static final int Break = 243;
    /**
     * The function IP.
     * <p/>
     * Name: Interrupt Process
     * Code: 244
     */
    public static final int Interrupt_Process = 244;
    /**
     * The function AO.
     * <p/>
     * Name: Abort output
     * Code: 245
     */
    public static final int Abort_output = 245;
    /**
     * The function AYT.
     * <p/>
     * Name: Are You There
     * Code: 246
     */
    public static final int Are_You_There = 246;
    /**
     * The function EC.
     * <p/>
     * Name: Erase character
     * Code: 247
     */
    public static final int Erase_character = 247;
    /**
     * The function EL.
     * <p/>
     * Name: Erase Line
     * Code: 248
     */
    public static final int Erase_Line = 248;
    /**
     * The GA signal.
     * <p/>
     * Name: Go ahead
     * Code: 249
     */
    public static final int Go_ahead = 249;
    /**
     * Indicates that what follows is
     * subnegotiation of the indicated
     * option.
     * <p/>
     * Name: SB
     * Code: 250
     */
    public static final int SB = 250;
    /**
     * Indicates the desire to begin
     * performing, or confirmation that
     * you are now performing, the
     * indicated option.
     * <p/>
     * Name: WILL (option code)
     * Code: 251
     */
    public static final int WILL = 251;
    /**
     * Indicates the refusal to perform,
     * or continue performing, the
     * indicated option.
     * <p/>
     * Name: WON'T (option code)
     * Code: 252
     */
    public static final int WONT = 252;
    /**
     * Indicates the request that the
     * other party perform, or
     * confirmation that you are expecting
     * he other party to perform, the
     * ndicated option.
     * <p/>
     * Name: DO (option code)
     * Code: 253
     */
    public static final int DO = 253;
    /**
     * Indicates the demand that the
     * other party stop performing,
     * or confirmation that you are no
     * longer expecting the other party
     * to perform, the indicated option.
     * <p/>
     * Name: DON'T (option code)
     * Code: 254
     */
    public static final int DONT = 254;
    /**
     * Interpret as command
     * aka Data Byte
     * <p/>
     * Name: IAC
     * Code: 255
     */
    public static final int IAC = 255;
}
