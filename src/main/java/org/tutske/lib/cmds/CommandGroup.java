package org.tutske.lib.cmds;

import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionSource;
import org.tutske.lib.options.sources.ArgumentOptionSource;
import org.tutske.lib.options.sources.DefaultsOptionSource;
import org.tutske.lib.options.OptionStore;
import org.tutske.lib.options.OptionStoreFactory;
import org.tutske.lib.utils.Exceptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


public class CommandGroup {

	public class CommandConfig {
		private final List<Option> options = new LinkedList<> ();
		private final Set<Command> subs = new HashSet<> ();
		private final Command current;
		private Command parent;
		private Consumer<OptionStore> storeConfig;
		private CmdFunction fn;
		private CmdConsumer before;
		private CmdConsumer after;
		private Boolean fullScan;

		public CommandConfig (Command current) {
			this.current = current;
		}

		public CommandConfig registerOptions (Option ... options) {
			Collections.addAll (this.options, options);
			return this;
		}

		public CommandConfig options (Option ... options) {
			return registerOptions (options);
		}

		public CommandConfig fn (CmdFunction fn) {
			this.fn = fn;
			return this;
		}

		public CommandConfig handle (CmdConsumer fn) {
			return fn ((cmd, opts, tail) -> {
				fn.run (cmd, opts, tail);
				return null;
			});
		}

		public CommandConfig before (CmdConsumer fn) {
			this.before = fn;
			return this;
		}

		public CommandConfig after (CmdConsumer fn) {
			this.after = fn;
			return this;
		}

		public CommandConfig configureStore (Consumer<OptionStore> store) {
			this.storeConfig = store;
			return this;
		}

		public CommandConfig subCommand (Command command) {
			CommandConfig sub = configure (command);

			if ( sub.parent != null ) {
				throw new RuntimeException (String.format (
					"Command %s already is a sub command of %s, cannot add to %s",
					command, sub.parent, this.current
				));
			}

			sub.parent = this.current;
			this.subs.add (command);

			return this;
		}

		public CommandConfig fullScan (boolean fullScan) {
			this.fullScan = fullScan;
			return this;
		}
	}

	private final Map<Command, CommandConfig> configs = new HashMap<> ();

	public CommandGroup register (String command) {
		return register (Command.get (command));
	}

	public CommandGroup register (Command command) {
		if ( ! configs.containsKey (command) ) { configs.put (command, new CommandConfig (command)); }
		return this;
	}

	public CommandGroup register (String command, Consumer<CommandConfig> config) {
		return register (Command.get (command), config);
	}

	public CommandGroup register (Command command, Consumer<CommandConfig> config) {
		register (command);
		config.accept (configs.get (command));
		return this;
	}

	public CommandGroup registerHandle (String command, CmdConsumer fn, Consumer<CommandConfig> config) {
		return registerHandle (Command.get (command), fn, config);
	}

	public CommandGroup register (String command, CmdFunction<?> fn, Consumer<CommandConfig> config) {
		return register (Command.get (command), fn, config);
	}

	public CommandGroup registerHandle (Command command, CmdConsumer fn, Consumer<CommandConfig> config) {
		register (command);
		configs.get (command).handle (fn);
		config.accept (configs.get (command));
		return this;
	}

	public CommandGroup register (Command command, CmdFunction<?> fn, Consumer<CommandConfig> config) {
		register (command);
		configs.get (command).fn (fn);
		config.accept (configs.get (command));
		return this;
	}

	public CommandConfig configure (String command) {
		return configure (Command.get (command));
	}

	public CommandConfig configure (Command command) {
		register (command);
		return configs.get (command);
	}

	public <T> T run (String [] args) {
		return run (Command.GLOBAL, args);
	}

	public <T> T run (Command command, String [] args) {
		return initialize ().internalRun (command, new CommandStore (), args);
	}

	private <T> T internalRun (Command command, CommandStore cmds, String [] args) {
		CommandConfig config = configs.get (command);
		if ( config == null ) { return null; }

		ArgumentOptionSource source = new ArgumentOptionSource ();
		OptionStore store = createStore (config, source);
		cmds.addStore (command, store);

		String [] tail = source.consumeTailed (args,
			config.fullScan == null ? config.subs.isEmpty () : config.fullScan
		);

		String cmd = tail.length > 0 ? tail[0] : "--";
		String [] remaining = tail.length > 1 ? new String [tail.length - 1] : new String [] {};
		if ( tail.length > 1 ) { System.arraycopy (tail, 1, remaining, 0, remaining.length); }

		Command sub = findSub (config.subs, cmd);
		if ( sub == null ) { return execute (command, cmds, tail); }
		else { return internalRun (sub, cmds, remaining); }
	}

	private <T> T execute (Command command, CommandStore cmds, String [] tail) {
		CommandConfig cfg = configs.get (command);
		while ( cfg.fn == null && cfg.parent != null ) {
			cfg = configs.get (cfg.parent);
		}

		if ( cfg.fn == null ) {
			throw new RuntimeException ("Failed to find a command function for: " + command);
		}

		cmds.setMain (command);

		try { return runMethods (command, cfg, cmds, tail); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	private <T> T runMethods (Command command, CommandConfig cfg, CommandStore opts, String [] tail) throws Exception {
		runPreMethods (command, cfg, opts, tail);
		T value = (T) cfg.fn.run (command, opts, tail);
		runPostMethods (command, cfg, opts, tail);
		return value;
	}

	private void runPreMethods (Command command, CommandConfig cfg, CommandStore opts, String [] tail) throws Exception {
		if ( cfg.parent != null ) { runPreMethods (command, configs.get (cfg.parent), opts, tail); }
		if ( cfg.before != null ) { cfg.before.run (command, opts, tail); }
	}

	private void runPostMethods (Command command, CommandConfig cfg, CommandStore opts, String [] tail) throws Exception {
		if ( cfg.after != null ) { cfg.after.run (command, opts, tail); }
		if ( cfg.parent != null ) { runPostMethods (command, configs.get (cfg.parent), opts, tail); }
	}

	private CommandGroup initialize () {
		if ( ! configs.containsKey (Command.GLOBAL) ) {
			register (Command.GLOBAL);
		}

		if ( configs.get (Command.GLOBAL).subs.isEmpty () ) {
			register (Command.GLOBAL, config -> configs.values ().stream ()
				.filter (cfg -> cfg.parent == null && cfg.current != Command.GLOBAL)
				.forEach (cfg -> config.subCommand (cfg.current))
			);
		}

		return this;
	}

	private OptionStore createStore (CommandConfig config, OptionSource source) {
		OptionStore store = OptionStoreFactory.createNew (
			config.options.toArray (new Option [] {}),
			new DefaultsOptionSource (), source
		);
		if ( config.storeConfig != null ) { config.storeConfig.accept (store); }
		return store;
	}

	private Command findSub (Set<Command> commands, String command) {
		for ( Command sub : commands ) {
			if ( sub.matches (command) ) { return sub; }
		}
		return null;
	}

}
