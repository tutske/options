package org.tutske.lib.options.cmd;

import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionStore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class CommandStore {

	private final Map<Command, OptionStore> stores = new LinkedHashMap<> ();
	private Command main;

	void addStore (Command command, OptionStore store) {
		if ( stores.containsKey (command) ) throw new RuntimeException (
			"Command " + command + " is already associated with a store."
		);

		stores.put (command, store);
		if ( main == null ) { main = command; }
	}

	void setMain (Command command) {
		this.main = command;
	}

	public OptionStore optionStore (Command command) {
		if ( ! stores.containsKey (command) ) throw new RuntimeException (
			"Command " + command + " does not have a store associated."
		);

		return stores.get (command);
	}

	public List<Command> commands () {
		return new LinkedList<> (stores.keySet ());
	}

	public List<Option<?>> options () {
		List<Option<?>> options = new LinkedList<> ();
		for ( OptionStore store : stores.values () ) {
			options.addAll (store.options ());
		}
		return options;
	}

	public List<Option<?>> options (Command command) {
		return stores.containsKey (command) ? stores.get (command).options () : Collections.emptyList ();
	}

	public <T> T get (Option<T> option) {
		return get (main, option);
	}

	public <T> T get (Command command, Option<T> option) {
		return optionStore (command).get (option);
	}

	public <T> T find (Option<T> option) {
		return findStore (option).get (option);
	}

	public <T> List<T> getAll (Option<T> option) {
		return getAll (main, option);
	}

	public <T> List<T> getAll (Command command, Option<T> option) {
		return optionStore (command).getAll (option);
	}

	public <T> List<T> findAll (Option<T> option) {
		return findStore (option).getAll (option);
	}

	public boolean has (Option<?> option) {
		for ( OptionStore store : stores.values () ) {
			if ( store.has (option) ) {
				return true;
			}
		}
		return false;
	}

	private OptionStore findStore (Option option) {
		for ( OptionStore store : stores.values () ) {
			if ( store.has (option) ) {
				return store;
			}
		}
		throw new RuntimeException (
			"Option " + option + " can not be found in any of the associated stores"
		);
	}

}
