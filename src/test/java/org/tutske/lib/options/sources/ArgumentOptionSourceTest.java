package org.tutske.lib.options.sources;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;


public class ArgumentOptionSourceTest {

	private Option<Integer> count = new Option.IntegerOption ("count");
	private OptionConsumer consumer = mock (OptionConsumer.class);
	private ArgumentOptionSource source = new ArgumentOptionSource ();

	@Test
	public void it_should_notify_listeners_when_they_are_attached () throws Exception {
		source.subscribe (asList (count), consumer);
		source.consume (new String [] { "--count=9" });

		verify (consumer).accept (eq (count), (List) argThat (hasItem (9)));
	}

	@Test
	public void it_should_notify_listeners_when_passing_new_options () throws Exception {
		source.subscribe (asList (count), consumer);
		source.unsubscribe (asList (count), consumer);

		source.consume (new String [] { "--count=9" });

		verify (consumer, times (0)).accept (any (), any ());
	}

	@Test
	public void it_should_parse_path_options () throws Exception {
		source.subscribe (asList (new Option.PathOption ("path")), consumer);

		source.consume (new String [] { "--path=/tmp/example.txt" });

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (any (), captor.capture ());

		assertThat (captor.getValue ().get (0), is (Paths.get ("/tmp/example.txt")));
	}

	@Test
	public void it_should_understand_boolean_options () throws Exception {
		source.subscribe (asList (new Option.BooleanOption ("verbose")), consumer);
		source.consume (new String [] { "--verbose" });

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (any (), captor.capture ());

		assertThat (captor.getValue ().get (0), is (true));
	}

	@Test
	public void it_should_understand_boolean_options_with_explicit_true () throws Exception {
		source.subscribe (asList (new Option.BooleanOption ("verbose")), consumer);
		source.consume (new String [] { "--verbose=true" });

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (any (), captor.capture ());

		assertThat (captor.getValue ().get (0), is (true));
	}

	@Test
	public void it_should_understand_boolean_options_with_explicit_false () throws Exception {
		source.subscribe (asList (new Option.BooleanOption ("verbose")), consumer);
		source.consume (new String [] { "--verbose=false" });

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (any (), captor.capture ());

		assertThat (captor.getValue ().get (0), is (false));
	}

	@Test
	public void it_should_understand_negated_boolean_options () throws Exception {
		source.subscribe (asList (new Option.BooleanOption ("verbose")), consumer);
		source.consume (new String [] { "--no-verbose" });

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (any (), captor.capture ());

		assertThat (captor.getValue ().get (0), is (false));
	}

	@Test
	public void it_should_understand_negated_explicit_true_boolean_options () throws Exception {
		source.subscribe (asList (new Option.BooleanOption ("verbose")), consumer);
		source.consume (new String [] { "--no-verbose=true" });

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (any (), captor.capture ());

		assertThat (captor.getValue ().get (0), is (false));
	}

	@Test
	public void it_should_understand_negated_explicit_false_boolean_options () throws Exception {
		source.subscribe (asList (new Option.BooleanOption ("verbose")), consumer);
		source.consume (new String [] { "--no-verbose=false" });

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (any (), captor.capture ());

		assertThat (captor.getValue ().get (0), is (true));
	}


	@Test
	public void it_should_notify_of_multiple_values () throws Exception {
		source.subscribe (asList (new Option.StringOption ("name")), consumer);
		source.consume (new String [] { "--name=jhon", "--name=jane" });

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (any (), captor.capture ());

		assertThat (captor.getValue (), (Matcher) contains ("jhon", "jane"));
	}

	@Test
	public void it_should_skip_things_that_dont_have_a_matching_option () throws Exception {
		source.subscribe (Collections.emptyList (), consumer);
		source.consume (new String [] { "--name=jhon", "--count=9", "--verbose" });

		verify (consumer, times (0)).accept (any (), any ());
	}

	@Test
	public void it_should_skip_over_non_options () throws Exception {
		Option<String> name = new Option.StringOption ("name");

		source.subscribe (asList (name), consumer);
		source.consume (new String [] { "start", "", "--name=jhon" });

		verify (consumer).accept (eq (name), any ());
	}

	@Test
	public void it_should_stop_after_double_dash () throws Exception {
		Option<String> name = new Option.StringOption ("name");

		source.subscribe (asList (name), consumer);
		source.consume (new String [] { "start", "--", "--name=jhon" });

		verify (consumer, times (0)).accept (any (), any ());
	}

	@Test
	public void it_should_propagate_exceptions_from_parsing () {
		source.subscribe (asList (new Option.LongOption ("age")), consumer);
		assertThrows (NumberFormatException.class, () -> {
			source.consume (new String [] { "--age=abc" });
		});
	}

	@Test
	public void it_should_propagate_exceptions_from_consumers () {
		Option<String> name = new Option.StringOption ("name");
		source.subscribe (asList (name), new OptionConsumer () {
			@Override public <T> void accept (Option<T> option, List<T> values) throws Exception {
				throw new Exception ("Intentional Falure");
			}
		});
		assertThrows (Exception.class, () -> {
			source.consume (new String[] { "--name=john" });
		});
	}

}
