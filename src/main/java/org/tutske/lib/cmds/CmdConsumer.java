package org.tutske.lib.cmds;

@FunctionalInterface
public interface CmdConsumer {

	public void run (Command command, CommandStore store, String [] tail) throws Exception;

}
