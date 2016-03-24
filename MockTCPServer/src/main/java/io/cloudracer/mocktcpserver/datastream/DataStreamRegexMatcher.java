package io.cloudracer.mocktcpserver.datastream;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class DataStreamRegexMatcher extends TypeSafeMatcher<DataStream> {

    private final String regex;

    public DataStreamRegexMatcher(final String regex) {
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
    public static DataStreamRegexMatcher matchesRegex(final String regex) {
        return new DataStreamRegexMatcher(regex);
    }
}