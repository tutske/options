package org.tutske.options.sources;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.tutske.options.Utils.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.tutske.options.Option;
import org.tutske.options.OptionConsumer;

import java.util.HashMap;
import java.util.List;


public class EnvironmentOptionSourceTest {

	private OptionConsumer consumer = mock (OptionConsumer.class);
	private EnvironmentOptionSource source = new EnvironmentOptionSource ("L", "_");

	@Test
	public void it_should_recognize_options_from_the_environment () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");

		source.subscribe (options (firstname), consumer);
		source.consume (new HashMap<String, String> () {{
			put ("L_FIRST_NAME", "John");
		}});

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

		source.subscribe (options (firstname, lastname), first);
		source.subscribe (options (firstname, lastname), second);

		source.consume (new HashMap<String, String> () {{
			put ("L_FIRST_NAME", "John");
			put ("L_LAST_NAME", "DOE");
		}});

		verify (first).accept (eq (firstname), any ());
		verify (first).accept (eq (lastname), any ());
		verify (second).accept (eq (firstname), any ());
		verify (second).accept (eq (lastname), any ());
	}

	@Test
	public void it_should_not_notify_of_options_not_in_the_bag () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");

		source.subscribe (options (firstname, lastname), consumer);
		source.consume (new HashMap<String, String> () {{
			put ("L_FIRST_NAME", "John");
		}});

		verify (consumer, times (0)).accept (eq (lastname), any ());
	}

	@Test
	public void it_should_notify_a_listeners_multiple_times_when_consuming_multiple_environments () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");
		source.subscribe (options (firstname, lastname), consumer);

		source.consume (new HashMap<String, String> () {{
			put ("L_FIRST_NAME", "John");
		}});
		source.consume (new HashMap<String, String> () {{
			put ("L_FIRST_NAME", "Jane");
		}});

		verify (consumer, times (2)).accept (eq (firstname), any ());
	}

	@Test
	public void it_should_not_notify_when_values_in_the_environment_dont_match_options () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");
		source.subscribe (options (firstname, lastname), consumer);

		source.consume (new HashMap<String, String> () {{
			put ("L_NON_EXISTING", "value");
			put ("L_IGNORED_OPTION", "value");
		}});

		verify (consumer, times (0)).accept (any (), any ());
	}

	@Test
	public void it_should_not_notify_when_the_listener_has_unsubscribed () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");
		source.subscribe (options (firstname, lastname), consumer);
		source.unsubscribe (options (firstname, lastname), consumer);

		source.consume (new HashMap<String, String> () {{
			put ("L_FIRST_NAME", "John");
			put ("L_LAST_NAME", "DOE");
		}});

		verify (consumer, times (0)).accept (any (), any ());
	}

	@Test
	public void it_should_not_notify_only_of_still_subscribed_options () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		Option<String> lastname = new Option.StringOption ("last name");
		source.subscribe (options (firstname, lastname), consumer);
		source.unsubscribe (options (lastname), consumer);

		source.consume (new HashMap<String, String> () {{
			put ("L_FIRST_NAME", "John");
			put ("L_LAST_NAME", "DOE");
		}});

		verify (consumer, times (0)).accept (eq (lastname), any ());
		verify (consumer, times (1)).accept (eq (firstname), any ());
	}

}
