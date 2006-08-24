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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CommandRegistry {
    
    protected static final HashMap commands = new HashMap();

    static {
        loadCommandList();
    }

    protected static final CommandRegistry unknownCommand = new CommandRegistry();

    public static Map getCommandMap() {
        HashMap rc = new HashMap();
        for (Iterator iter = commands.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            Command command = getCommand(name);
            rc.put(name, command);
        }
        return rc;
    }

    public static void register(String name, Command cmd) {
        commands.put(name, cmd);
    }

    public static void register(String name, Class cmd) {
        commands.put(name, cmd);
    }

    public static Command getCommand(String name) {
        Object cmd = commands.get(name);
        if (cmd instanceof Class) {
            cmd = loadCommand((Class) cmd);
            register(name, (Command) cmd);
        }
        return (Command) cmd;
    }


    // - Protected methods - //
    protected static Command loadCommand(Class commandClass) {
        Command cmd = null;
        try {
            cmd = (Command) commandClass.newInstance();
        } catch (Exception e) {
            //throw new IOException("Cannot instantiate command class "+commandClass+"\n"+e.getClass().getName()+":\n"+e.getMessage());
        }
        return cmd;
    }

    /**
     * TODO: Replace this part with the classpath magic
     */
    protected static void loadCommandList() {
        Exit.register();
        Help.register();
        Lookup.register();
        Version.register();
        GroovySh.register();
    }
}
