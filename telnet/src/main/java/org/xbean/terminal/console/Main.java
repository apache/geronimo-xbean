package org.xbean.terminal.console;

import org.xbean.command.CommandShell;

public class Main {

    public static void main(String[] args) {
        CommandShell shell = new CommandShell("localhost");
        System.exit(shell.main(new String[]{}, System.in, System.out));
    }
}
