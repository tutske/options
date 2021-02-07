package org.tutske.lib.options.sources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;
import org.tutske.lib.options.Utils;

import java.util.List;


public class PropertyLineOptionSourceTest {

	private OptionConsumer consumer = mock (OptionConsumer.class);
	private PropertyLineOptionSource source = new PropertyLineOptionSource ();

	@Test
	public void it_should_recognize_a_single_option_in_all_caps () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");

		source.subscribe (Utils.options (firstname), consumer);
		source.consume ("FIRST_NAME=John");

		assertThat (findValue (firstname), is ("John"));
	}

	@Test
	public void it_should_recognize_a_single_option_in_all_lowercase () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");

		source.subscribe (Utils.options (firstname), consumer);
		source.consume ("first_name=John");

		assertThat (findValue (firstname), is ("John"));
	}

	@Test
	public void it_should_recognize_a_single_option_in_snake_case () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");

		source.subscribe (Utils.options (firstname), consumer);
		source.consume ("first-name=John");

		assertThat (findValue (firstname), is ("John"));
	}

	@Test
	public void it_should_recognize_multiple_options_in_snake_case () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");

		source.subscribe (Utils.options (firstname, lastname), consumer);
		source.consume ("first-name=John last-name=Doe");

		assertThat (findValue (firstname), is ("John"));
		assertThat (findValue (lastname), is ("Doe"));
	}

	@Test
	public void it_should_handle_truthy_boolean_options () throws Exception {
		Option<Boolean> first = new Option.BooleanOption ("first");
		Option<Boolean> second = new Option.BooleanOption ("second");
		Option<Boolean> third = new Option.BooleanOption ("third");

		source.subscribe (Utils.options (first, second, third), consumer);
		source.consume ("first=yes second=true third");

		assertThat (findValue (first), is (true));
		assertThat (findValue (second), is (true));
		assertThat (findValue (third), is (true));
	}

	@Test
	public void it_should_handle_falsy_boolean_options () throws Exception {
		Option<Boolean> first = new Option.BooleanOption ("first");
		Option<Boolean> second = new Option.BooleanOption ("second");
		Option<Boolean> third = new Option.BooleanOption ("third");

		source.subscribe (Utils.options (first, second, third), consumer);
		source.consume ("first=no second=false no-third");

		assertThat (findValue (first), is (false));
		assertThat (findValue (second), is (false));
		assertThat (findValue (third), is (false));
	}

	@Test
	public void it_should_handle_all_prefixed_falsy_boolean_options () throws Exception {
		Option<Boolean> first = new Option.BooleanOption ("first");
		Option<Boolean> second = new Option.BooleanOption ("second");
		Option<Boolean> third = new Option.BooleanOption ("third");

		source.subscribe (Utils.options (first, second, third), consumer);
		source.consume ("no-first not-second non-third");

		assertThat (findValue (first), is (false));
		assertThat (findValue (second), is (false));
		assertThat (findValue (third), is (false));
	}

	@Test
	public void it_should_handle_all_prefixed_boolean_options_with_explicit_value () throws Exception {
		Option<Boolean> first = new Option.BooleanOption ("first");
		Option<Boolean> second = new Option.BooleanOption ("second");
		Option<Boolean> third = new Option.BooleanOption ("third");

		source.subscribe (Utils.options (first, second, third), consumer);
		source.consume ("no-first=yes not-second=no non-third=false");

		assertThat (findValue (first), is (false));
		assertThat (findValue (second), is (true));
		assertThat (findValue (third), is (true));
	}

	@Test
	public void it_should_not_notify_about_boolean_options_that_are_not_specified () throws Exception {
		Option<Boolean> first = new Option.BooleanOption ("first");
		source.subscribe (Utils.options (first), consumer);
		source.consume ("");
		verify (consumer, times (0)).accept (eq (first), any ());
	}

	@Test
	public void it_should_complain_when_the_option_is_not_known () {
		Option<Boolean> first = new Option.BooleanOption ("first");
		source.subscribe (Utils.options (first), consumer);
		assertThrows (Exception.class, () -> {
			source.consume ("no-first does-not-exist");
		});
	}

	@Test
	public void it_should_propagate_exceptions_from_the_consumer () throws Exception {
		source.subscribe (Utils.options (new Option.BooleanOption ("first")), new OptionConsumer () {
			@Override public <T> void accept (Option<T> option, List<T> values) throws Exception {
				throw new Exception ("Fail intentionally");
			}
		});
		assertThrows (Exception.class, () -> {
			source.consume ("first");
		});
	}

	@Test
	public void it_should_use_arbitrary_separators () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");

		PropertyLineOptionSource source = PropertyLineOptionSource.withSeparator (",");
		source.subscribe (Utils.options (firstname, lastname), consumer);
		source.consume ("first-name = John, last-name = Doe");

		assertThat (findValue (firstname), is ("John"));
	}

	private <T> T findValue (Option<T> option) throws Exception {
		return findValue (consumer, option);
	}

	private <T> T findValue (OptionConsumer consumer, Option<T> option) throws Exception {
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (eq (option), captor.capture ());
		return (T) captor.getValue ().get (0);
	}

}
