package org.xbean.telnet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

import org.xbean.command.Command;
import org.xbean.command.CommandRegistry;
import org.xbean.terminal.telnet.TelnetShell;

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