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
import java.util.HashMap;
import java.util.LinkedList;
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
			try { processOptions (entry.getKey (), entry.getValue (), properties); }
			catch ( Exception e ) { throw Exceptions.wrap (e); }
		}
	}

	private void processOptions (OptionConsumer consumer, List<Option> options, Properties properties) {
		Map<String, List<String>> gathered = new HashMap<> ();
		properties.forEach ((k, v) -> gathered
			.computeIfAbsent (normalize (String.valueOf (k)), ignore -> new LinkedList<> ())
			.add (String.valueOf (v))
		);
		BaseOptionSource.notifyOptions (consumer, options, gathered);
	}

	private String normalize (String name) {
		return name
			.toLowerCase ()
			.replace ("_", " ")
			.replaceAll ("-", " ");
	}

}
