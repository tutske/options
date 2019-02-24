package org.tutske.lib.options.sources;

import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;
import org.tutske.lib.options.OptionSource;
import org.tutske.lib.utils.Exceptions;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public class EnvironmentOptionSource extends BaseOptionSource implements OptionSource {

	private final String leading;
	private final String sep;

	public EnvironmentOptionSource (String leading, String sep) {
		this.leading = leading;
		this.sep = sep;
	}

	public void consume (Map<String, String> environment) {
		for ( Map.Entry<OptionConsumer, List<Option>> entry : listeners.entrySet () ) {
			for ( Option option : entry.getValue () ) {
				String value = environment.get (canonicalName (option));
				if ( value == null ) { continue; }

				try { entry.getKey ().accept (option, values (option, value)); }
				catch ( Exception e ) { throw Exceptions.wrap (e); }
			}
		}
	}

	private List values (Option option, String value) {
		return Collections.singletonList (option.parseValue (value));
	}

	private String canonicalName (Option option) {
		return leading + sep + option.getName ().toUpperCase ().replaceAll (" ", sep);
	}

}
