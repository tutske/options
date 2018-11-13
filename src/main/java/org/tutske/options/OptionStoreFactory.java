package org.tutske.options;

import org.tutske.options.impl.ReplacingOptionStore;
import org.tutske.options.sources.ArgumentOptionSource;
import org.tutske.options.sources.DefaultsOptionSource;
import org.tutske.options.sources.EnvironmentOptionSource;
import org.tutske.options.sources.PropertyFileOptionSource;

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

	public static OptionStore createServiceOptionStore (Option [] options, String [] args, OptionSource ... sources) {
		return createServiceOptionStore (options, null, "", args, sources);
	}

	public static OptionStore createServiceOptionStore (Option [] options, Option<String> config, String [] args, OptionSource ... sources) {
		return createServiceOptionStore (options, config, "", args, sources);
	}

	public static OptionStore createServiceOptionStore (Option [] options, String prefix, String [] args, OptionSource ... sources) {
		return createServiceOptionStore (options, null, prefix, args, sources);
	}

	public static OptionStore createServiceOptionStore (Option [] options, Option<String> config, String prefix, String [] args, OptionSource ... sources) {
		DefaultsOptionSource defaults = new DefaultsOptionSource ();
		PropertyFileOptionSource properties = new PropertyFileOptionSource ();
		ArgumentOptionSource arguments = new ArgumentOptionSource ();
		EnvironmentOptionSource environment = new EnvironmentOptionSource (prefix, "_");

		OptionStore store = createNew (options, defaults, properties, environment, arguments);

		environment.consume (System.getenv ());
		arguments.consume (args);

		if ( config != null && store.has (config) ) {
			String name = store.get (config);

			properties.consume ("resource://" + name);
			properties.consume (name);

			environment.consume (System.getenv ());
			arguments.consume (args);
		}

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
