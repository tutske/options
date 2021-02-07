package org.tutske.lib.options.sources;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;
import org.tutske.lib.options.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class PropertyFileOptionSourceTest {

	private OptionConsumer consumer = mock (OptionConsumer.class);
	private PropertyFileOptionSource source = new PropertyFileOptionSource ();

	@Test
	public void it_should_recognize_options_from_the_environment () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");

		source.subscribe (Utils.options (firstname), consumer);
		source.consume (stream ("FIRST_NAME = John"));

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (eq (firstname), captor.capture ());

		assertThat (captor.getValue ().get (0), is ("John"));
	}

	@Test
	public void it_should_recognize_options_specified_in_lower_case () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");

		source.subscribe (Utils.options (firstname), consumer);
		source.consume (stream ("first_name = John"));

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (eq (firstname), captor.capture ());

		assertThat (captor.getValue ().get (0), is ("John"));
	}

	@Test
	public void it_should_notify_multiple_listeners_of_multiple_values () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");

		OptionConsumer first = mock (OptionConsumer.class);
		OptionConsumer second = mock (OptionConsumer.class);

		source.subscribe (Utils.options (firstname, lastname), first);
		source.subscribe (Utils.options (firstname, lastname), second);

		source.consume (stream (
			"FIRST_NAME = John",
			"LAST_NAME = Doe"
		));

		verify (first).accept (eq (firstname), any ());
		verify (first).accept (eq (lastname), any ());
		verify (second).accept (eq (firstname), any ());
		verify (second).accept (eq (lastname), any ());
	}

	@Test
	public void it_should_not_notify_of_options_not_in_the_bag () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");

		source.subscribe (Utils.options (firstname, lastname), consumer);
		source.consume (stream ("FIRST_NAME = John"));

		verify (consumer, times (0)).accept (eq (lastname), any ());
	}

	@Test
	public void it_should_notify_a_listeners_multiple_times_when_consuming_multiple_environments () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");
		source.subscribe (Utils.options (firstname, lastname), consumer);

		source.consume (stream ("FIRST_NAME = John"));
		source.consume (stream ("FIRST_NAME = Jane"));

		verify (consumer, times (2)).accept (eq (firstname), any ());
	}

	@Test
	public void it_should_not_notify_when_values_in_the_environment_dont_match_options () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");
		source.subscribe (Utils.options (firstname, lastname), consumer);

		source.consume (stream (
			"NON_EXISTING = value",
			"IGNORED_OPTION = value"
		));

		verify (consumer, times (0)).accept (any (), any ());
	}

	@Test
	public void it_should_not_notify_when_the_listener_has_unsubscribed () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");
		source.subscribe (Utils.options (firstname, lastname), consumer);
		source.unsubscribe (Utils.options (firstname, lastname), consumer);

		source.consume (stream (
			"FIRST_NAME = John",
			"LAST_NAME = Doe"
		));

		verify (consumer, times (0)).accept (any (), any ());
	}

	@Test
	public void it_should_still_notify_when_unsubscribing_non_existent_subscription () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");
		source.subscribe (Utils.options (firstname, lastname), consumer);
		source.unsubscribe (Utils.options (firstname, lastname), mock (OptionConsumer.class));

		source.consume (stream (
			"FIRST_NAME = John",
			"LAST_NAME = Doe"
		));

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (eq (firstname), captor.capture ());

		assertThat (captor.getValue ().get (0), is ("John"));
	}

	@Test
	public void it_should_not_notify_only_of_still_subscribed_options () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");
		source.subscribe (Utils.options (firstname, lastname), consumer);
		source.unsubscribe (Utils.options (lastname), consumer);

		source.consume (stream (
			"FIRST_NAME = John",
			"LAST_NAME = Doe"
		));

		verify (consumer, times (0)).accept (eq (lastname), any ());
		verify (consumer, times (1)).accept (eq (firstname), any ());
	}

	@Test
	public void it_should_not_do_anything_on_null_inputs () throws Exception {
		source.subscribe (Utils.options (new Option.StringOption ("first name")), consumer);
		source.consume ((InputStream) null);
		verify (consumer, times (0)).accept (any (), any ());
	}

	@Test
	public void it_should_not_do_anything_on_non_existing_paths () throws Exception {
		source.subscribe (Utils.options (new Option.StringOption ("first name")), consumer);
		source.consume (Paths.get ("does/not/exist.properties"));
		verify (consumer, times (0)).accept (any (), any ());
	}

	@Test
	public void it_should_propagate_exceptions_from_properties_parsing () {
		source.subscribe (Utils.options (new Option.StringOption ("first name")), consumer);
		assertThrows (Exception.class, () -> {
			source.consume (new InputStream () {
				@Override public int read () throws IOException {
					throw new IOException ("Fail intentionally");
				}
			});
		});
	}

	@Test
	public void it_should_propagate_exceptions_from_options () {
		source.subscribe (asList (new Option.StringOption ("name")), new OptionConsumer () {
			@Override public <T> void accept (Option<T> option, List<T> values) throws Exception {
				throw new Exception ("Intentional Falure");
			}
		});
		assertThrows (Exception.class, () -> {
			source.consume (stream ("NAME = jhon"));
		});
	}

	@Test
	public void it_should_read_from_actual_file_paths () throws Exception {
		Path path = Files.createTempFile ("example", ".properties");
		path.toFile ().deleteOnExit ();

		try ( OutputStream out = Files.newOutputStream (path) ) {
			stream ("FIRST_NAME = john").transferTo (out);
		}

		Option<String> firstname = new Option.StringOption ("first name");
		source.subscribe (Utils.options (firstname), consumer);
		source.consume (path);
		verify (consumer, times (1)).accept (eq (firstname), any ());
	}

	@Test
	public void it_should_read_from_actual_files () throws Exception {
		Path path = Files.createTempFile ("example", ".properties");
		path.toFile ().deleteOnExit ();

		try ( OutputStream out = Files.newOutputStream (path) ) {
			stream ("FIRST_NAME = john").transferTo (out);
		}

		Option<String> firstname = new Option.StringOption ("first name");
		source.subscribe (Utils.options (firstname), consumer);
		source.consume (path.toFile ());
		verify (consumer, times (1)).accept (eq (firstname), any ());
	}

	@Test
	public void it_should_read_from_actual_files_names () throws Exception {
		Path path = Files.createTempFile ("example", ".properties");
		path.toFile ().deleteOnExit ();

		try ( OutputStream out = Files.newOutputStream (path) ) {
			stream ("FIRST_NAME = john").transferTo (out);
		}

		Option<String> firstname = new Option.StringOption ("first name");
		source.subscribe (Utils.options (firstname), consumer);
		source.consume (path.toString ());
		verify (consumer, times (1)).accept (eq (firstname), any ());
	}

	@Test
	public void it_should_read_from_actual_resources () throws Exception {
		Path path = Files.createTempFile ("example", ".properties");
		path.toFile ().deleteOnExit ();

		try ( OutputStream out = Files.newOutputStream (path) ) {
			stream ("FIRST_NAME = john").transferTo (out);
		}

		Option<String> firstname = new Option.StringOption ("first name");
		source.subscribe (Utils.options (firstname), consumer);
		source.consume ("file://" + path.toString ());
		verify (consumer, times (1)).accept (eq (firstname), any ());
	}

	@Test
	public void it_should_propagate_exceptions_from_failed_resource_loading () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		source.subscribe (Utils.options (firstname), consumer);
		assertThrows (Exception.class, () -> {
			source.consume ("file://does-not-exist.properties");
		});
	}

	@Test
	public void it_should_propagate_exceptions_from_the_consumer_when_consuming_paths () throws Exception {
		Path path = Files.createTempFile ("example", ".properties");
		path.toFile ().deleteOnExit ();
		try ( OutputStream out = Files.newOutputStream (path) ) {
			stream ("FIRST_NAME = john").transferTo (out);
		}

		source.subscribe (Utils.options (new Option.StringOption ("first name")), new OptionConsumer () {
			@Override public <T> void accept (Option<T> option, List<T> values) throws Exception {
				throw new Exception ("Fail intentionally");
			}
		});

		assertThrows (Exception.class, () -> {
			source.consume (path);
		});
	}

	private InputStream stream (String ... content) {
		return new ByteArrayInputStream (String.join ("\n", content).getBytes ());
	}

}
