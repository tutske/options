package org.tutske.options.sources;

import org.tutske.options.Option;
import org.tutske.options.Option.*;
import org.tutske.options.OptionConsumer;
import org.tutske.options.OptionSource;
import org.tutske.lib.utils.Exceptions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ArgumentOptionSource extends BaseOptionSource implements OptionSource {

	public void consume (String [] args) {
		for ( Map.Entry<OptionConsumer, List<Option>> entry : listeners.entrySet () ) {
			try { processOptions (entry.getKey (), entry.getValue (), args, true); }
			catch ( Exception e ) { throw Exceptions.wrap (e); }
		}
	}

	public String [] consumeTailed (String [] args) {
		return consumeTailed (args, false);
	}

	public String [] consumeTailed (String [] args, boolean skipUnknown) {
		if ( listeners.size () > 1 ) { throw new RuntimeException ("Can only give a tail with single listener"); }
		Map.Entry<OptionConsumer, List<Option>> entry = listeners.entrySet ().iterator ().next ();
		return processOptions (entry.getKey (), entry.getValue (), args, skipUnknown);
	}

	private String [] processOptions (OptionConsumer consumer, List<Option> options, String [] args, boolean skipUnknown) {
		List<String> tail = new LinkedList<> ();
		Map<String, Option> lookup = gatherOptions (options);
		Map<String, List<String>> gathered = new HashMap<> ();

		for ( String arg : args ) {
			if ( (! tail.isEmpty () && ! skipUnknown) || tail.contains ("--") ) {
				tail.add (arg);
				continue;
			}

			if ( "--".equals (arg) || ! arg.startsWith ("--") ) {
				tail.add (arg);
				continue;
			}

			String name = normalize (extractName (arg));
			String value = extractValue (arg);
			Option option = lookup.get (name);

			if ( option != null ) { gathered.computeIfAbsent (name, key -> new LinkedList<> ()).add (value); }
			else { tail.add (arg); }
		}

		notifyOptions (consumer, options, gathered);

		tail.remove ("--");
		return tail.toArray (new String [] {});
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

			if ( ! (option instanceof BooleanOption) ) { continue; }

			gathered.putIfAbsent ("no " + option.getName (), option);
			gathered.putIfAbsent ("not " + option.getName (), option);
			gathered.putIfAbsent ("non " + option.getName (), option);
		}

		return gathered;
	}

	private String extractName (String arg) {
		int index = arg.indexOf ("=");
		return index < 0 ? arg.substring (2) : arg.substring (2, index);
	}

	private String extractValue (String arg) {
		int index = arg.indexOf ("=");
		return index < 0 ? "" : arg.substring (index + 1);
	}

	private String normalize (String name) {
		return name.replaceAll ("-", " ");
	}

}
