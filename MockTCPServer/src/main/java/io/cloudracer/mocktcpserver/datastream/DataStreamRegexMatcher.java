package io.cloudracer.mocktcpserver.datastream;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Typesafe comparison between a Regular Expression and the content of a {@link DataStream}.
 *
 * @author John McDonnell
 */
public class DataStreamRegexMatcher extends TypeSafeMatcher<DataStream> {

    private final String regex;

    /**
     * Specify the Regular Expression to compares.
     *
     * @param regex {@link DataStreamRegexMatcher#matchesSafely(DataStream)} will parse this Regular Expression against the content of a provided {@link DataStream}.
     */
    public DataStreamRegexMatcher(final String regex) {
        this.regex = regex;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(String.format("Match the regular expression: %s.", this.regex));
    }

    /**
     * @param data the contents of this {@link DataStream} will be matched against the {@link DataStreamRegexMatcher#DataStreamRegexMatcher(String) specified Regular Expression}.
     * @return True if the {@link DataStreamRegexMatcher#DataStreamRegexMatcher(String) specified Regular Expression} matches against the content of the provided {@link DataStream}; otherwise false.
     */
    @Override
    public boolean matchesSafely(final DataStream data) {
        return data.toString().matches(this.regex);
    }
}