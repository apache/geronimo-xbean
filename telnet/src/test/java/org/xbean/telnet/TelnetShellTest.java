package org.xbean.telnet;

import junit.framework.*;
import org.xbean.telnet.TelnetShell;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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

    public static class TestCommand extends Command {
        public static void register() {
            Command.register("test", TestCommand.class);
        }
        public void exec(String[] args, InputStream in, PrintStream out) throws IOException {
            out.print(args[0].length());
            out.print(args[1].length());
        }
    }
}