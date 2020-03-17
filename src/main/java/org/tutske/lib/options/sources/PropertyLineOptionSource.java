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
import java.util.stream.Collectors;


public class PropertyLineOptionSource extends BaseOptionSource implements OptionSource {

	public void consume (String value) {
		String [] parts = value.split ("\\s+");
		for ( Map.Entry<OptionConsumer, List<Option>> entry : listeners.entrySet () ) {
			try { processOptions (entry.getKey (), entry.getValue (), parts); }
			catch ( Exception e ) { throw Exceptions.wrap (e); }
		}
	}

	private void processOptions (OptionConsumer consumer, List<Option> options, String [] args) {
		Map<String, Option> lookup = gatherOptions (options);
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

		notifyOptions (consumer, options, gathered);
	}

	private void notifyOptions (OptionConsumer consumer, List<Option> options, Map<String, List<String>> gathered) {
		for ( Option option : options ) {
			if ( option instanceof Option.BooleanOption ) { notifyBooleanOption (option, consumer, gathered); }
			else { notifyNormalOption (option, consumer, gathered); }
		}
	}

	private void notifyBooleanOption (Option option, OptionConsumer consumer, Map<String, List<String>> gathered) {
		List<String> values = gathered.get (option.getName ());

		boolean negated = values == null;

		if ( values == null ) { values = gathered.get ("no " + option.getName ()); }
		if ( values == null ) { values = gathered.get ("not " + option.getName ()); }
		if ( values == null ) { values = gathered.get ("non " + option.getName ()); }

		if ( values == null ) { return; }

		notify (consumer, option, values.stream ()
			.map (val -> negated ? (! (Boolean) option.parseValue (val)) : option.parseValue (val))
			.collect(Collectors.toList ())
		);
	}

	private void notifyNormalOption (Option option, OptionConsumer consumer, Map<String, List<String>> gathered) {
		List<String> values = gathered.get (option.getName ());
		if ( values == null ) { return; }
		notify (consumer, option, values.stream ().map (val -> option.parseValue (val)).collect(Collectors.toList()));
	}

	private void notify (OptionConsumer consumer, Option option, List values) {
		try { consumer.accept (option, values); }
		catch ( Exception exception ) { throw Exceptions.wrap (exception); }
	}

	private Map<String, Option> gatherOptions (List<Option> options) {
		Map<String, Option> gathered = new HashMap<> ();

		for ( Option option : options ) {
			gathered.put (option.getName (), option);

			if ( ! (option instanceof Option.BooleanOption) ) { continue; }

			gathered.putIfAbsent ("no " + option.getName (), option);
			gathered.putIfAbsent ("not " + option.getName (), option);
			gathered.putIfAbsent ("non " + option.getName (), option);
		}

		return gathered;
	}

	private String extractName (String arg) {
		int index = arg.indexOf ("=");
		return index < 0 ? arg : arg.substring (0, index);
	}

	private String extractValue (String arg) {
		int index = arg.indexOf ("=");
		return index < 0 ? "" : arg.substring (index + 1);
	}

	private String normalize (String name) {
		return name
			.toLowerCase ()
			.replace ("_", " ")
			.replaceAll ("-", " ");
	}

}
