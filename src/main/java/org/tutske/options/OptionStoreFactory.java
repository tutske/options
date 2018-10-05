package org.tutske.options;

import org.tutske.options.impl.ReplacingOptionStore;

import java.util.HashMap;
import java.util.Map;


public class OptionStoreFactory {

	public static OptionStore createNew (Option ... options) {
		validateOptions (options);
		return new ReplacingOptionStore (options);
	}

	public static OptionStore createNew (Option [] options, OptionSource ... sources) {
		OptionStore store = createNew (options);
		for ( OptionSource source : sources ) { store.bind (source); }
		return store;
	}

	private static void validateOptions (Option [] options) {
		Map<String, Option> mapped = new HashMap<> ();
		for ( Option option : options ) {
			mapped.putIfAbsent (option.getName (), option);
			if ( option != mapped.get (option.getName ()) ) {
				throw new DuplicateOptionException (option);
			}
		}
	}

}
