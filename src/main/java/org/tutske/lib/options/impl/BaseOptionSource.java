package org.tutske.lib.options.impl;

import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;
import org.tutske.lib.options.OptionSource;
import org.tutske.lib.utils.Exceptions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public abstract class BaseOptionSource implements OptionSource {

	protected final Map<OptionConsumer, List<Option>> listeners = new HashMap<> ();

	@Override
	public void subscribe (List<Option> options, OptionConsumer consumer) {
		List<Option> copy = new LinkedList<> (options);
		listeners.put (consumer, copy);
	}

	@Override
	public void unsubscribe (List<Option> options, OptionConsumer consumer) {
		if ( ! listeners.containsKey (consumer) ) { return; }

		List<Option> current = listeners.get (consumer);
		current.removeAll (options);

		if ( current.isEmpty () ) { listeners.remove (consumer); }
	}

	public static  Map<String, Option> gatherOptions (List<Option> options) {
		Map<String, Option> gathered = new HashMap<> ();

		for ( Option option : options ) {
			String name = option.getName ().toLowerCase ();
			gathered.put (name, option);

			if ( ! (option instanceof Option.BooleanOption) ) { continue; }

			gathered.putIfAbsent ("no " + name, option);
			gathered.putIfAbsent ("not " + name, option);
			gathered.putIfAbsent ("non " + name, option);
		}

		return gathered;
	}

	public static void notifyOptions (OptionConsumer consumer, List<Option> options, Map<String, List<String>> gathered) {
		for ( Option option : options ) {
			if ( option instanceof Option.BooleanOption ) { notifyBooleanOption (option, consumer, gathered); }
			else { notifyNormalOption (option, consumer, gathered); }
		}
	}

	public static void notifyBooleanOption (Option option, OptionConsumer consumer, Map<String, List<String>> gathered) {
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

	public static void notifyNormalOption (Option option, OptionConsumer consumer, Map<String, List<String>> gathered) {
		List<String> values = gathered.get (option.getName ());
		if ( values == null ) { return; }
		notify (consumer, option, values.stream ().map (val -> option.parseValue (val)).collect(Collectors.toList()));
	}

	public static void notify (OptionConsumer consumer, Option option, List values) {
		try { consumer.accept (option, values); }
		catch ( Exception exception ) { throw Exceptions.wrap (exception); }
	}

}
