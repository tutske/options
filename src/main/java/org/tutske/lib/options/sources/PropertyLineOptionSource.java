package org.tutske.lib.options.sources;

import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;
import org.tutske.lib.options.OptionSource;
import org.tutske.lib.options.UnknownOptionException;
import org.tutske.lib.options.impl.BaseOptionSource;
import org.tutske.lib.utils.Exceptions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class PropertyLineOptionSource extends BaseOptionSource implements OptionSource {

	public static PropertyLineOptionSource withSeparator (String sep) {
		return new PropertyLineOptionSource (Pattern.compile ("\\s*" + sep + "\\s*"));
	}

	private final Pattern pattern;

	public PropertyLineOptionSource () {
		this (Pattern.compile ("\\s+"));
	}

	public PropertyLineOptionSource (Pattern pattern) {
		this.pattern = pattern;
	}

	public void consume (String value) {
		String [] parts = pattern.split (value);
		for ( Map.Entry<OptionConsumer, List<Option>> entry : listeners.entrySet () ) {
			try { processOptions (entry.getKey (), entry.getValue (), parts); }
			catch ( Exception e ) { throw Exceptions.wrap (e); }
		}
	}

	private void processOptions (OptionConsumer consumer, List<Option> options, String [] args) {
		Map<String, Option> lookup = BaseOptionSource.gatherOptions (options);
		Map<String, List<String>> gathered = new HashMap<> ();

		for ( String arg : args ) {
			if ( arg.length () == 0 ) { continue; }

			String name = normalize (extractName (arg));
			String value = extractValue (arg);

			if ( ! lookup.containsKey (name) ) {
				throw new UnknownOptionException ("Option `" + name + "` is not known.");
			}

			gathered.computeIfAbsent (name, key -> new LinkedList<> ()).add (value);
		}

		BaseOptionSource.notifyOptions (consumer, options, gathered);
	}

	private String extractName (String arg) {
		int index = arg.indexOf ("=");
		return index < 0 ? arg.trim () : arg.substring (0, index).trim ();
	}

	private String extractValue (String arg) {
		int index = arg.indexOf ("=");
		return index < 0 ? "" : arg.substring (index + 1).trim ();
	}

	private String normalize (String name) {
		return name
			.toLowerCase ()
			.replace ("_", " ")
			.replaceAll ("-", " ");
	}

}
