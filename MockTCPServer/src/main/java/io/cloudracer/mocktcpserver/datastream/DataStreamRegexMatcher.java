package io.cloudracer.mocktcpserver.datastream;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Type-safe comparison between a Regular Expression and the content of a {@link DataStream}.
 *
 * @author John McDonnell
 */
public class DataStreamRegexMatcher extends TypeSafeMatcher<DataStream> {

    private String regEx;

    /**
     * Specify the Regular Expression to compares.
     *
     * @param regEx {@link DataStreamRegexMatcher#matchesSafely(DataStream)} will parse this Regular Expression against the content of a provided {@link DataStream}.
     */
    public DataStreamRegexMatcher(final String regEx) {
        setRegEx(regEx);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(String.format("Match the regular expression: %s.", getRegEx()));
    }

    /**
     * @param data the contents of this {@link DataStream} will be matched against the {@link DataStreamRegexMatcher#DataStreamRegexMatcher(String) specified Regular Expression}.
     * @return True if the {@link DataStreamRegexMatcher#DataStreamRegexMatcher(String) specified Regular Expression} matches against the content of the provided {@link DataStream}; otherwise false.
     */
    @Override
    public boolean matchesSafely(final DataStream data) {
        return data.toString().matches(getRegEx());
    }

    /**
     * Regular Expression used by this comparison.
     *
     * @return the Regular Expression used by this comparison
     */
    public String getRegEx() {
        return new String(regEx);
    }

    private void setRegEx(String regex) {
        this.regEx = regex;
    }
}