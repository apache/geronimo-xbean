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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class Lookup implements Command {

    private final javax.naming.Context ctx;

    public Lookup() throws Exception {
        this(new InitialContext());
    }

    public Lookup(Context ctx) {
        this.ctx = ctx;
    }

    public static void register() {
        try {
            Lookup cmd = new Lookup();
            CommandRegistry.register("lookup", cmd);
        } catch (Exception e) {
        }
    }

    private static String PWD = "";

    // execute jndi lookups
    public int main(String[] args, InputStream in, PrintStream out) {
        try {
            String name = "";
            if (args == null || args.length == 0) {
                name = PWD;
            } else {
                name = args[0];
            }
            Object obj = null;
            try {
                obj = ctx.lookup(name);
            } catch (NameNotFoundException e) {
                out.print("lookup: ");
                out.print(name);
                out.println(": No such object or subcontext");
                return -1;
            } catch (Throwable e) {
                out.print("lookup: error: ");
                e.printStackTrace(new PrintStream(out));
                return -1;
            }
            if (obj instanceof Context) {
                list(name, in, out);
                return 0;
            }
            // TODO:1: Output the different data types differently
            out.println("" + obj);
            return 0;
            
        } catch (Exception e) {
            e.printStackTrace(new PrintStream(out));
            return -2;
        }
    }

    public void list(String name, InputStream in, PrintStream out) throws IOException {
        try {
            NamingEnumeration names = null;
            try {
                names = ctx.list(name);
            } catch (NameNotFoundException e) {
                out.print("lookup: ");
                out.print(name);
                out.println(": No such object or subcontext");
                return;
            } catch (Throwable e) {
                out.print("lookup: error: ");
                e.printStackTrace(new PrintStream(out));
                return;
            }
            if (names == null) {
                return;
            }
            while (names.hasMore()) {
                NameClassPair entry = (NameClassPair) names.next();
                out.println(entry.getName());
            }
        } catch (Exception e) {
            e.printStackTrace(new PrintStream(out));
        }
    }
}
