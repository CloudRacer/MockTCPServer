package io.cloudracer;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class RegexMatcher extends TypeSafeMatcher<DataStream> {

	private final String regex;

	public RegexMatcher(final String regex) {
		this.regex = regex;
	}

	@Override
	public void describeTo(final Description description) {
		description.appendText("matches regular expression=`" + regex + "`");
	}

	@Override
	public boolean matchesSafely(final DataStream data) {
		return data.toString().matches(regex);
	}

	// matcher method you can call on this matcher class
	public static RegexMatcher matchesRegex(final String regex) {
		return new RegexMatcher(regex);
	}
}