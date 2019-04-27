package org.tutske.lib.cmds;


@FunctionalInterface
public interface CmdFunction<T> {

	public T run (Command command, CommandStore store, String [] tail) throws Exception;

}
