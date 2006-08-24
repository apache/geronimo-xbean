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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Help implements Command {
    public static void register() {
        CommandRegistry.register("help", Help.class);
    }

    public int main(String[] args, InputStream in, PrintStream out) {
        Map hash = CommandRegistry.getCommandMap();;
        Set set = hash.keySet();
        Iterator cmds = set.iterator();
        while (cmds.hasNext()) {
            out.print(" " + cmds.next());
            out.println("");
        }
        return 0;
    }
}
