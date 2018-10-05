package org.tutske.options;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


public class OptionTest {

	@Test
	public void it_should_parse_durations_with_full_hours () {
		Duration duration = new Option.DurationOption ("").parseValue ("2hours");
		assertThat (duration.toMillis (), is (TimeUnit.HOURS.toMillis (2)));
	}

}
