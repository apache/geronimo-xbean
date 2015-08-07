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

import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class GroovySh implements Command {
    public static void register() {
        CommandRegistry.register("groovysh", GroovySh.class);
    }

    public int main(String[] args, InputStream in, PrintStream out) {
        GroovyShell shell = new GroovyShell();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String version = GroovySystem.getVersion();
        out.println("Lets get Groovy!");
        out.println("================");
        out.println("Version: " + version + " JVM: " + System.getProperty("java.vm.version"));
        out.println("Hit carriage return twice to execute a command");
        out.println("The command 'quit' will terminate the shell");
        int counter = 1;
        while (true) {
            StringBuffer buffer = new StringBuffer();
            while (true) {
                out.print("groovy> ");
                String line;
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    out.println("Caught: " + e);
                    e.printStackTrace();
                    return -1;
                }
                if (line != null) {
                    buffer.append(line);
                    buffer.append('\n');
                }
                if (line == null || line.trim().length() == 0) {
                    break;
                }
            }
            String command = buffer.toString().trim();
            if (command == null || command.equals("quit")) {
                break;
            }
            try {
                Object answer = shell.evaluate(command, "CommandLine" + counter++ + ".groovy");
                out.println(InvokerHelper.inspect(answer));
            } catch (Exception e) {
                out.println("Caught: " + e);
                e.printStackTrace();
            }
        }
        return 0;
    }
}
