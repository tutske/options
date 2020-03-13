package org.tutske.lib.options;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;


public class OptionTest {

	@Test
	public void it_should_parse_durations_with_full_hours () {
		Duration duration = new Option.DurationOption ("").parseValue ("2hours");
		assertThat (duration.toMillis (), is (TimeUnit.HOURS.toMillis (2)));
	}

	@Test
	public void it_should_give_duration_fallbacks () {
		Duration otherwise = new Option.DurationOption ("", "2hours").getDefault ();
		assertThat (otherwise.toMillis (), is (TimeUnit.HOURS.toMillis (2)));
	}

	@Test
	public void it_should_parse_colon_duration_notations () {
		Duration duration = new Option.DurationOption ("").parseValue ("2:00:00");
		assertThat (duration.toMillis (), is (TimeUnit.HOURS.toMillis (2)));
	}

	@Test
	public void it_should_parse_colon_duration_notations_with_two_parts () {
		Duration duration = new Option.DurationOption ("").parseValue ("2:25");
		assertThat (duration.toMillis (), is (
			TimeUnit.HOURS.toMillis (2) + TimeUnit.MINUTES.toMillis (25)
		));
	}

	@Test
	public void it_should_parse_unit_suffixed_notations () {
		Option<Duration> option = new Option.DurationOption ("");
		assertThat (option.parseValue ("25000000ns").toMillis (), is (25L));
		assertThat (option.parseValue ("25ms").toMillis (), is (25L));

		assertThat (option.parseValue ("25s").toMillis (), is (TimeUnit.SECONDS.toMillis (25)));
		assertThat (option.parseValue ("1second").toMillis (), is (TimeUnit.SECONDS.toMillis (1)));
		assertThat (option.parseValue ("25seconds").toMillis (), is (TimeUnit.SECONDS.toMillis (25)));

		assertThat (option.parseValue ("3m").toMillis (), is (TimeUnit.MINUTES.toMillis (3)));
		assertThat (option.parseValue ("1minute").toMillis (), is (TimeUnit.MINUTES.toMillis (1)));
		assertThat (option.parseValue ("2minutes").toMillis (), is (TimeUnit.MINUTES.toMillis (2)));

		assertThat (option.parseValue ("3h").toMillis (), is (TimeUnit.HOURS.toMillis (3)));
		assertThat (option.parseValue ("1hour").toMillis (), is (TimeUnit.HOURS.toMillis (1)));
		assertThat (option.parseValue ("2hours").toMillis (), is (TimeUnit.HOURS.toMillis (2)));

		assertThat (option.parseValue ("3d").toMillis (), is (TimeUnit.DAYS.toMillis (3)));
		assertThat (option.parseValue ("1day").toMillis (), is (TimeUnit.DAYS.toMillis (1)));
		assertThat (option.parseValue ("2days").toMillis (), is (TimeUnit.DAYS.toMillis (2)));

		assertThat (option.parseValue ("3w").toMillis (), is (TimeUnit.DAYS.toMillis (3 * 7)));
		assertThat (option.parseValue ("1week").toMillis (), is (TimeUnit.DAYS.toMillis (1 * 7)));
		assertThat (option.parseValue ("2weeks").toMillis (), is (TimeUnit.DAYS.toMillis (2 * 7)));

		assertThat (option.parseValue ("3y").toMillis (), is (TimeUnit.DAYS.toMillis (3 * 365)));
		assertThat (option.parseValue ("1year").toMillis (), is (TimeUnit.DAYS.toMillis (1 * 365)));
		assertThat (option.parseValue ("2years").toMillis (), is (TimeUnit.DAYS.toMillis (2 * 365)));
	}

	@Test
	public void it_should_combine_multple_parsed_duration_notations () {
		Option<Duration> option = new Option.DurationOption ("");
		Duration duration = option.parseValue ("1week 3days 23:17:12");

		assertThat (duration.toMillis (), is (
			TimeUnit.DAYS.toMillis (1 * 7) +
			TimeUnit.DAYS.toMillis (3) +
			TimeUnit.HOURS.toMillis (23) +
			TimeUnit.MINUTES.toMillis (17) +
			TimeUnit.SECONDS.toMillis (12)
		));
	}

	@Test (expected = Exception.class)
	public void it_should__complain_when_providing_too_many_duration_parts () {
		new Option.DurationOption ("").parseValue ("2:00:00:00");
	}

	@Test (expected = Exception.class)
	public void it_should__complain_when_providing_too_few_duration_parts () {
		new Option.DurationOption ("").parseValue ("2");
	}

	@Test
	public void it_should_parse_positive_boolean_values () {
		Option<Boolean> option = new Option.BooleanOption ("");
		assertThat (option.parseValue (""), is (true));
		assertThat (option.parseValue ("yes"), is (true));
		assertThat (option.parseValue ("on"), is (true));
	}

	@Test
	public void it_should_parse_negative_boolean_values () {
		Option<Boolean> option = new Option.BooleanOption ("");
		assertThat (option.parseValue ("no"), is (false));
		assertThat (option.parseValue ("off"), is (false));
	}

	@Test
	public void it_should_parse_integer_option_values () {
		assertThat (new Option.IntegerOption ("").parseValue ("6"), is (6));
	}

	@Test
	public void it_should_parse_long_option_values () {
		assertThat (new Option.LongOption ("").parseValue ("6"), is (6L));
	}

	@Test
	public void it_should_parse_float_option_values () {
		assertThat (new Option.FloatOption ("").parseValue ("6.3"), is (6.3F));
	}

	@Test
	public void it_should_parse_double_option_values () {
		assertThat (new Option.DoubleOption ("").parseValue ("6.3"), is (6.3D));
	}

	@Test
	public void it_should_parse_path_option_values () {
		assertThat (
			new Option.PathOption ("").parseValue ("/etc/hosts"),
			is (Paths.get ("/etc/hosts"))
		);
	}

	@Test
	public void it_should_give_path_fallbacks () {
		Path otherwise = new Option.PathOption ("", "/etc/hosts").getDefault ();
		assertThat (otherwise, is (Paths.get ("/etc/hosts")));
	}

	@Test
	public void it_should_parse_uri_optiosn_values () throws Exception {
		assertThat (
			new Option.UriOption ("").parseValue ("http;//user:pass@host.domain/path"),
			is (new URI ("http;//user:pass@host.domain/path"))
		);
	}

	@Test
	public void it_should_give_uri_fallbacks () throws Exception {
		URI otherwise = new Option.UriOption ("", "http;//user:pass@host.domain/path").getDefault ();
		assertThat (otherwise, is (new URI ("http;//user:pass@host.domain/path")));
	}

	@Test
	public void it_should_parse_enum_options () {
		Option<TestOption> option = new Option.EnumOption<> ("test", TestOption.values ());
		assertThat (option.parseValue ("First"), is (TestOption.First));
	}

	@Test
	public void it_should_give_enum_fallbacks() {
		Option<TestOption> option = new Option.EnumOption<> ("test", TestOption.values (), TestOption.Second);
		assertThat (option.getDefault (), is (TestOption.Second));
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_not_representing_a_value_of_the_enum () throws Exception {
		new Option.EnumOption<> ("test", TestOption.values ()).parseValue ("DOES_NOT_EXIST");
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_parsing_nulls () {
		new Option.EnumOption<> ("test", TestOption.values ()).parseValue (null);
	}

	public static enum TestOption {
		First, Second, Third;
	}

}
