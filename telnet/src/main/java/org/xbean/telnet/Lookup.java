/**
 *
 * Copyright 2005 David Blevins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbean.telnet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class Lookup extends Command {

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
            Command.register("lookup", cmd);
        } catch (Exception e) {
        }
    }

    private static String PWD = "";

    // execute jndi lookups
    public void exec(String[] args, InputStream in, PrintStream out) throws IOException {
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
                return;
            } catch (Throwable e) {
                out.print("lookup: error: ");
                e.printStackTrace(new PrintStream(out));
                return;
            }
            if (obj instanceof Context) {
                list(name, in, out);
                return;
            }
            // TODO:1: Output the different data types differently
            out.println("" + obj);
        } catch (Exception e) {
            e.printStackTrace(new PrintStream(out));
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
