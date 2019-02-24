package org.tutske.lib.options.sources;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.tutske.lib.options.Utils.*;

import org.junit.Test;
import org.tutske.lib.options.Option;
import org.tutske.lib.options.Option.*;
import org.tutske.lib.options.OptionConsumer;


public class ArgumentOptionSourceTailTest {

	private OptionConsumer consumer = mock (OptionConsumer.class);
	private ArgumentOptionSource source = new ArgumentOptionSource ();

	@Test (expected = RuntimeException.class)
	public void it_should_complain_on_consume_with_tail_with_multiple_listeners () {
		source.subscribe (asList (new StringOption ("name")), createConsumer ());
		source.subscribe (asList (new StringOption ("name")), createConsumer ());

		source.consumeTailed (new String [] { "--", "--name=john" });
	}

	@Test
	public void it_should_consume_with_tail () {
		source.subscribe (asList (new StringOption ("name")), consumer);
		String tail [] = source.consumeTailed (new String [] {});
		assertThat (tail.length, is (0));
	}

	@Test
	public void it_should_call_the_listener_when_consuming_with_tail () throws Exception {
		Option<String> name = new StringOption ("name");

		source.subscribe (asList (name), consumer);
		source.consumeTailed (new String [] { "--name=jhon" });

		verify (consumer).accept (eq (name), any ());
	}

	@Test
	public void it_should_put_everything_after_double_dash_in_the_tail () {
		source.subscribe (options ("name"), consumer);
		String tail [] = source.consumeTailed (new String [] { "--", "--name=john" });
		assertThat (tail, arrayContaining ("--name=john"));
	}

	@Test
	public void it_should_stop_at_the_first_unknown_thing () {
		source.subscribe (options ("name", "age"), consumer);

		String tail [] = source.consumeTailed (new String [] { "--name=john", "first", "--age=23", "last" });
		assertThat (tail, arrayContaining ("first", "--age=23", "last"));
	}

	@Test
	public void it_should_put_all_unknown_things_in_the_tail_when_asked () {
		source.subscribe (options ("name", "age"), consumer);

		String tail [] = source.consumeTailed (new String [] { "--name=john", "first", "--age=23", "last" }, true);
		assertThat (tail, arrayContaining ("first", "last"));
	}

	@Test
	public void it_should_stop_at_double_dash_even_when_continuing () {
		source.subscribe (options ("name", "age", "verbose"), consumer);

		String tail [] = source.consumeTailed (new String [] { "--name=john", "first", "--verbose", "--", "--age=23", "last" }, true);
		assertThat (tail, arrayContaining ("first", "--age=23", "last"));
	}

	@Test
	public void it_should_not_notify_of_options_that_are_skipped () throws Exception {
		Option<String> name = new StringOption ("name");
		Option<Integer> age = new IntegerOption ("age");
		Option<Boolean> verbose = new BooleanOption ("verbose");

		source.subscribe (asList (name, age, verbose), consumer);
		source.consumeTailed (new String [] { "--name=john", "first", "--verbose", "--age=23" });

		verify (consumer, times (0)).accept (eq (verbose), any ());
		verify (consumer, times (0)).accept (eq (age), any ());
	}

	@Test
	public void it_should_notify_of_options_after_an_unknown_argument () throws Exception {
		Option<String> name = new StringOption ("name");
		Option<Integer> age = new IntegerOption ("age");
		Option<Boolean> verbose = new BooleanOption ("verbose");

		source.subscribe (asList (name, age, verbose), consumer);
		source.consumeTailed (new String [] { "--name=john", "first", "--verbose", "--age=23" }, true);

		verify (consumer).accept (eq (verbose), any ());
		verify (consumer).accept (eq (age), any ());
	}

}
