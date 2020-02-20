package org.tutske.lib.options.sources;

import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;
import org.tutske.lib.options.OptionSource;
import org.tutske.lib.options.impl.BaseOptionSource;
import org.tutske.lib.utils.Exceptions;
import org.tutske.lib.utils.Resource;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class PropertyFileOptionSource extends BaseOptionSource implements OptionSource {

	public void consume (String filename) {
		if ( ! filename.contains ("://")) {
			consume (Paths.get (filename));
			return;
		}

		try ( InputStream in = Resource.getResource (filename) ) { consume (in); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	public void consume (File file) {
		consume (file.toPath ());
	}

	public void consume (Path path) {
		if ( ! Files.exists (path) ) { return; }
		try ( InputStream in = Files.newInputStream (path) ) { consume (in); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	public void consume (InputStream in) {
		if ( in == null ) { return; }

		Properties properties = new Properties ();

		try { properties.load (in); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }

		for ( Map.Entry<OptionConsumer, List<Option>> entry : listeners.entrySet () ) {
			for ( Option option : entry.getValue () ) {
				String value = properties.getProperty (canonicalName (option));
				if ( value == null ) { value = properties.getProperty (option.getName ().replace (" ", "_")); }
				if ( value == null ) { value = properties.getProperty (option.getName ().replace (" ", "-")); }
				if ( value == null ) { value = properties.getProperty (option.getName ().replace (" ", ".")); }
				if ( value == null ) { value = properties.getProperty (option.getName ()); }

				if ( value == null ) { continue; }

				try { entry.getKey ().accept (option, values (option, value)); }
				catch ( Exception e ) { throw Exceptions.wrap (e); }
			}
		}
	}

	private List values (Option option, String value) {
		return Collections.singletonList (option.parseValue (value));
	}

	private String canonicalName (Option<?> option) {
		return option.getName ().toUpperCase ().replace (" ", "_");
	}

}
