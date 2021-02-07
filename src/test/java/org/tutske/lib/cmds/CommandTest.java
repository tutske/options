package org.tutske.lib.cmds;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;


public class CommandTest {

	@Test
	public void it_should_give_commands_a_representation () {
		Command command = Command.get ("run");
		assertThat (command.representation (), is ("run"));
	}

	@Test
	public void it_should_match_exact_representations () {
		assertThat (Command.get ("run").matches ("run"), is (true));
	}

	@Test
	public void it_should_not_match_other_representations () {
		assertThat (Command.get ("run").matches ("other"), is (false));
	}

	@Test
	public void it_should_have_a_global_command () {
		assertThat (Command.GLOBAL.representation (), notNullValue ());
	}

	@Test
	public void it_shnould_not_match_anything_for_the_global_command () {
		assertThat (Command.GLOBAL.matches ("anything"), is (false));
	}

	@Test
	public void it_should_contain_the_representation_in_the_string_form () {
		assertThat (Command.get ("run").toString (), containsString ("run"));
	}

	@Test
	public void it_should_have_a_string_representation_of_the_global_command () {
		assertThat (Command.GLOBAL.toString ().toLowerCase (), containsString ("global"));
	}

}
