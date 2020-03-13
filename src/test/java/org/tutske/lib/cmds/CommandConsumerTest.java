package org.tutske.lib.cmds;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.tutske.lib.options.Option;


public class CommandConsumerTest {

	private Option<Boolean> verbose = new Option.BooleanOption ("verbose");

	CommandGroup store = new CommandGroup ();
	CmdConsumer fn = mock (CmdConsumer.class);

	@Test
	public void it_should_run_command_consumers () throws Exception {
		store.configure (Command.GLOBAL).handle (fn);
		assertThat (store.run (new String [] {}), nullValue ());
		verify (fn).run (eq (Command.GLOBAL), any (), any ());
	}

	@Test
	public void it_should_run_command_consumer_configured_through_register_with_config () throws Exception {
		store.registerHandle (Command.GLOBAL, fn, cfg -> cfg.options (verbose));
		assertThat (store.run (new String [] {}), nullValue ());
		verify (fn).run (eq (Command.GLOBAL), any (), any ());
	}

	@Test
	public void it_should_run_command_consumer_configured_from_string_commands () throws Exception {
		store.registerHandle ("run", fn, cfg -> cfg.options (verbose));
		assertThat (store.run (new String [] { "run" }), nullValue ());
		verify (fn).run (eq (Command.get ("run")), any (), any ());
	}

}
