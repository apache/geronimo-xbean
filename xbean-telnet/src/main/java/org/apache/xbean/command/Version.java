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

import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;

public class Version implements Command {
    
    public static void register() {
        CommandRegistry.register("version", Version.class);
    }

    public int main(String[] args, InputStream in, PrintStream out) {
        /*
         * Output startup message
         */
        Properties versionInfo = new Properties();
        try {
            versionInfo.load(new URL("resource:/openejb-version.properties").openConnection().getInputStream());
        } catch (java.io.IOException e) {
        }
        out.print("OpenEJB Remote Server ");
        out.print(versionInfo.getProperty("version"));
        out.print("    build: ");
        out.print(versionInfo.getProperty("date"));
        out.print("-");
        out.println(versionInfo.getProperty("time"));
        out.println(versionInfo.getProperty("url"));
        return 0;
    }
}
