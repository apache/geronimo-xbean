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
package org.apache.xbean.telnet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

import org.apache.xbean.command.Command;
import org.apache.xbean.command.CommandRegistry;
import org.apache.xbean.terminal.telnet.TelnetShell;

public class TelnetShellTest extends TestCase {

    public void testService() throws Exception {
        TestCommand.register();

        TelnetShell telnetShell = new TelnetShell("acme");
        ByteArrayInputStream in = new ByteArrayInputStream("test hello world\r\n".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        telnetShell.service(in, out);

        // TODO test case still needs work
        System.out.println(new String(out.toByteArray()));
    }

    public static class TestCommand implements Command {
        public static void register() {
            CommandRegistry.register("test", TestCommand.class);
        }
        public int main(String[] args, InputStream in, PrintStream out) {
            out.print(args[0].length());
            out.print(args[1].length());
            return 0;
        }
    }
}
