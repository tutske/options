package org.tutske.lib.cmds;

import org.tutske.lib.options.DynamicOption;
import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionSource;
import org.tutske.lib.options.OptionStore;
import org.tutske.lib.options.StoreChangeConsumer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class CommandStore implements OptionStore {

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

	@Override
	public void bind (OptionSource source) {
		stores.get (main).bind (source);
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

	@Override
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

	@Override
	public boolean knows (Option<?> option) {
		for ( OptionStore store : stores.values () ) {
			if ( store.knows (option) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean has (Option<?> option) {
		for ( OptionStore store : stores.values () ) {
			if ( store.has (option) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <T> void onChange (Option<T> option, StoreChangeConsumer<T> consumer) {
		findStore (option).onChange (option, consumer);
	}

	@Override
	public <T> void onChanges (Option<T> option, StoreChangeConsumer.Multi<T> consumer) {
		findStore (option).onChanges (option, consumer);
	}

	@Override
	public <T> DynamicOption<T> dynamic (Option<T> option) {
		return findStore (option).dynamic (option);
	}

	@Override
	public <T> DynamicOption.Value<T> dynamicValue (Option<T> option) {
		return findStore (option).dynamicValue (option);
	}

	private OptionStore findStore (Option option) {
		for ( OptionStore store : stores.values () ) {
			if ( store.knows (option) ) {
				return store;
			}
		}
		throw new RuntimeException (
			"Option " + option + " can not be found in any of the associated stores"
		);
	}

}
