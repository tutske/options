package org.tutske.lib.cmds;

import java.util.HashMap;
import java.util.Map;


public interface Command {

	public static Command create (String name) {
		return SimpleCommand.cache.compute (name, (k, v) -> new SimpleCommand (k));
	}

	public static Command get (String name) {
		return SimpleCommand.cache.computeIfAbsent (name, SimpleCommand::new);
	}

	public boolean matches (String name);

	static final Command GLOBAL = new Command () {
		@Override public String toString () { return "<GlobalCommand />"; }
		@Override public boolean matches (String name) { return false; }
	};

	static class SimpleCommand implements Command {
		private static final Map<String, Command> cache = new HashMap<> ();

		private final String name;

		private SimpleCommand (String name) { this.name = name; }

		@Override public String toString () { return String.format ("<SimpleCommand: %s />", name); }
		@Override public boolean matches (String name) { return this.name.equals (name); }
	}

}
