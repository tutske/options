package org.tutske.lib.options.sources;

import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;
import org.tutske.lib.options.OptionSource;
import org.tutske.lib.options.impl.BaseOptionSource;
import org.tutske.lib.utils.Exceptions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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
		Map<String, Option> lookup = BaseOptionSource.gatherOptions (options);
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

		BaseOptionSource.notifyOptions (consumer, options, gathered);

		tail.remove ("--");
		return tail.toArray (new String [] {});
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
