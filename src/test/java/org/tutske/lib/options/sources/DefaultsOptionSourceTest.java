package org.tutske.lib.options.sources;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.tutske.lib.options.Utils.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;

import java.util.List;


public class DefaultsOptionSourceTest {

	private OptionConsumer consumer = mock (OptionConsumer.class);
	private DefaultsOptionSource source = new DefaultsOptionSource ();

	@Test
	public void it_should_notify_of_the_options_default_value () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name", "John");
		source.subscribe (options (firstname), consumer);

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (eq (firstname), captor.capture ());

		assertThat (captor.getValue ().get (0), is ("John"));
	}

	@Test
	public void it_should_notify_of_primitive_times () throws Exception {
		Option<Integer> age = new Option.IntegerOption ("age", 23);
		source.subscribe (options (age), consumer);

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass (List.class);
		verify (consumer).accept (eq (age), captor.capture ());

		assertThat (captor.getValue ().get (0), is (23));
	}

	@Test
	public void it_should_not_do_anything_when_the_option_has_no_default_value () throws Exception {
		Option<String> firstname = new Option.StringOption ("first name");
		source.subscribe (options (firstname), consumer);
		verify (consumer, times (0)).accept (any (), any ());
	}

}
