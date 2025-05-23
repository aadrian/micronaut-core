/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.core.util;

import io.micronaut.core.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Utility methods for Strings.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public final class StringUtils {

    /**
     * Constant for the value true.
     */
    public static final String TRUE = "true";
    /**
     * Constant for the value false.
     */
    public static final String FALSE = "false";

    /**
     * Constant for an empty String array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Constant for an empty String.
     */
    public static final String EMPTY_STRING = "";

    /**
     * a space.
     */
    public static final char SPACE = 0x20;

    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    /**
     * Return whether the given string is empty.
     *
     * @param str The string
     * @return True if str is empty or null
     */
    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * Return whether the given string is not empty.
     *
     * @param str The string
     * @return True if str is not null and not empty
     */
    public static boolean isNotEmpty(@Nullable CharSequence str) {
        return !isEmpty(str);
    }

    /**
     * Return whether the given string has non whitespace characters.
     *
     * @param str The string
     * @return True if str contains any non whitespace characters
     */
    public static boolean hasText(@Nullable CharSequence str) {
        if (isEmpty(str)) {
            return false;
        }

        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts the given objects into a set of interned strings contained within an internal pool of sets. See {@link String#intern()}.
     *
     * @param objects The objects
     * @return An unmodifiable, pooled set of strings
     */
    @SuppressWarnings("unused")
    public static List<String> internListOf(Object... objects) {
        if (objects == null || objects.length == 0) {
            return Collections.emptyList();
        }
        List<String> strings = new ArrayList<>(objects.length);
        for (Object object : objects) {
            strings.add(object.toString());
        }
        return Collections.unmodifiableList(strings);
    }

    /**
     * Converts the given objects into a map of interned strings. See {@link String#intern()}.
     *
     * @param values The objects
     * @return An unmodifiable set of strings
     * @see CollectionUtils#mapOf(Object...)
     */
    @SuppressWarnings("unused")
    public static Map<String, Object> internMapOf(Object... values) {
        if (values == null) {
            return Collections.emptyMap();
        }
        int len = values.length;
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Number of arguments should be an even number representing the keys and values");
        }

        Map<String, Object> answer = new HashMap<>((int) (len / 2 / 0.75));
        int i = 0;
        while (i < values.length - 1) {
            String key = values[i++].toString();
            Object val = values[i++];
            answer.put(key, val);
        }
        return answer;
    }

    /**
     * Is the given string a series of digits.
     *
     * @param str The string
     * @return True if it is a series of digits
     */
    public static boolean isDigits(String str) {
        return isNotEmpty(str) && DIGIT_PATTERN.matcher(str).matches();
    }

    /**
     * Parse the given {@code String} value into a {@link Locale}, accepting
     * the {@link Locale#toString} format as well as BCP 47 language tags.
     *
     * @param localeValue the locale value: following either {@code Locale's}
     *                    {@code toString()} format ("en", "en_UK", etc.), also accepting spaces as
     *                    separators (as an alternative to underscores), or BCP 47 (e.g. "en-UK")
     *                    as specified by {@link Locale#forLanguageTag} on Java 7+
     *                    <p>Copied from the Spring Framework while retaining all license, copyright and author information.</p>
     * @return a corresponding {@code Locale} instance, or {@code null} if none
     * @throws IllegalArgumentException in case of an invalid locale specification
     * @see Locale#forLanguageTag
     * @since 2.3.0
     */
    @Nullable
    public static Locale parseLocale(String localeValue) {
        String[] tokens = tokenizeToStringArray(localeValue, "_ ", false, false);
        if (tokens.length == 1) {
            validateLocalePart(localeValue);
            Locale resolved = Locale.forLanguageTag(localeValue);
            if (resolved.getLanguage().length() > 0) {
                return resolved;
            }
        }
        String language = (tokens.length > 0 ? tokens[0] : "");
        String country = (tokens.length > 1 ? tokens[1] : "");
        validateLocalePart(language);
        validateLocalePart(country);

        String variant = "";
        if (tokens.length > 2) {
            // There is definitely a variant, and it is everything after the country
            // code sans the separator between the country code and the variant.
            int endIndexOfCountryCode = localeValue.indexOf(country, language.length()) + country.length();
            // Strip off any leading '_' and whitespace, what's left is the variant.
            variant = trimLeadingWhitespace(localeValue.substring(endIndexOfCountryCode));
            if (variant.startsWith("_")) {
                variant = trimLeadingCharacter(variant, '_');
            }
        }

        if (variant.isEmpty() && country.startsWith("#")) {
            variant = country;
            country = "";
        }

        return (language.length() > 0 ? new Locale(language, country, variant) : null);
    }

    /**
     * <p>Copied from the Spring Framework while retaining all license, copyright and author information.</p>
     */
    private static void validateLocalePart(String localePart) {
        for (int i = 0; i < localePart.length(); i++) {
            char ch = localePart.charAt(i);
            if (ch != ' ' && ch != '_' && ch != '-' && ch != '#' && !Character.isLetterOrDigit(ch)) {
                throw new IllegalArgumentException(
                        "Locale part \"" + localePart + "\" contains invalid characters");
            }
        }
    }

    /**
     * Returns a new string without any leading whitespace.
     *
     * @param str The string
     * @return The string without leading whitespace
     * @since 2.3.0
     */
    public static String trimLeadingWhitespace(String str) {
        return trimLeading(str, Character::isWhitespace);
    }

    /**
     * Returns a new string without any leading characters that match the supplied character.
     *
     * @param str The string
     * @param c   The character to remove
     * @return The string without leading characters matching the supplied character.
     * @since 2.3.0
     */
    public static String trimLeadingCharacter(String str, char c) {
        return trimLeading(str, character -> character == c);
    }

    /**
     * Returns a new string without any leading characters that match the supplied predicate.
     *
     * @param str       The string
     * @param predicate The predicate to test characters against
     * @return The string without leading characters matching the supplied predicate.
     * @since 2.3.0
     */
    public static String trimLeading(String str, Predicate<Character> predicate) {
        if (isEmpty(str)) {
            return str;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!predicate.test(str.charAt(i))) {
                return str.substring(i);
            }
        }
        return "";
    }

    /**
     * Tokenize the given String into a String array via a StringTokenizer.
     * Trims tokens and omits empty tokens.
     * <p>The given delimiters string is supposed to consist of any number of
     * delimiter characters. Each of those characters can be used to separate
     * tokens. A delimiter is always a single character; for multi-character
     * delimiters, consider using {@code delimitedListToStringArray}
     *
     * <p>Copied from the Spring Framework while retaining all license, copyright and author information.
     *
     * @param str        the String to tokenize
     * @param delimiters the delimiter characters, assembled as String
     *                   (each of those characters is individually considered as delimiter).
     * @return an array of the tokens
     * @see java.util.StringTokenizer
     * @see java.lang.String#trim()
     */
    public static String[] tokenizeToStringArray(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    /**
     * Tokenize the given String into a String array via a StringTokenizer.
     * <p>The given delimiters string is supposed to consist of any number of
     * delimiter characters. Each of those characters can be used to separate
     * tokens. A delimiter is always a single character; for multi-character
     * delimiters, consider using {@code delimitedListToStringArray}
     *
     * <p>Copied from the Spring Framework while retaining all license, copyright and author information.
     *
     * @param str               the String to tokenize
     * @param delimiters        the delimiter characters, assembled as String
     *                          (each of those characters is individually considered as delimiter)
     * @param trimTokens        trim the tokens via String's {@code trim}
     * @param ignoreEmptyTokens omit empty tokens from the result array
     *                          (only applies to tokens that are empty after trimming; StringTokenizer
     *                          will not consider subsequent delimiters as token in the first place).
     * @return an array of the tokens ({@code null} if the input String
     * was {@code null})
     * @see java.util.StringTokenizer
     * @see java.lang.String#trim()
     */
    public static String[] tokenizeToStringArray(
            String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

        if (str == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(str, delimiters);
        List<String> tokens = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (!ignoreEmptyTokens || !token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * Replace the dots in the property with underscore and
     * transform to uppercase.
     *
     * @param dottedProperty The property with dots, example - a.b.c
     * @return The converted value
     */
    public static String convertDotToUnderscore(String dottedProperty) {
        return convertDotToUnderscore(dottedProperty, true);
    }

    /**
     * Replace the dots in the property with underscore and
     * transform to uppercase based on given flag.
     *
     * @param dottedProperty The property with dots, example - a.b.c
     * @param uppercase      To transform to uppercase string
     * @return The converted value
     */
    public static String convertDotToUnderscore(String dottedProperty, boolean uppercase) {
        if (dottedProperty == null) {
            return dottedProperty;
        }
        dottedProperty = dottedProperty.replace('.', '_');
        return uppercase ? dottedProperty.toUpperCase() : dottedProperty;
    }

    /**
     * Prepends a partial uri and normalizes / characters.
     * For example, if the base uri is "/foo/" and the uri
     * is "/bar/", the output will be "/foo/bar/". Similarly,
     * if the base uri is "/foo" and the uri is "bar", the
     * output will be "/foo/bar"
     *
     * @param baseUri The uri to prepend. E.g. /foo
     * @param uri     The uri to combine with the baseUri. E.g. /bar
     * @return A combined uri string
     */
    public static String prependUri(String baseUri, String uri) {
        StringBuilder builder = new StringBuilder(baseUri);
        if (!uri.isEmpty() && (uri.length() != 1 || uri.charAt(0) != '/')) {
            if (!uri.startsWith("/") && !uri.startsWith("?")) {
                builder.append('/');
            }
            builder.append(uri);
        }
        if (builder.isEmpty()) {
            return "";
        }

        int i = 0;
        if (builder.charAt(0) != '/' && builder.indexOf("://") != -1) {
            // skip until after scheme
            while (i < builder.length() && builder.charAt(i) != ':') {
                i++;
            }
            i += 2;
        }
        // replace double slashes
        for (; i < builder.length() - 1; i++) {
            if (builder.charAt(i) == '/' && builder.charAt(i + 1) == '/') {
                builder.deleteCharAt(i);
                i--;
            } else if (builder.charAt(i) == '?') {
                break;
            }
        }
        return builder.toString();
    }

    /**
     * Capitalizes the first character of the provided string.
     *
     * @param str The string to capitalize
     * @return The capitalized string
     */
    public static String capitalize(String str) {
        char[] array = str.toCharArray();
        if (array.length > 0) {
            array[0] = Character.toUpperCase(array[0]);
        }
        return new String(array);
    }

    /**
     * Trims the supplied string. If the string is empty or null before or after
     * trimming, null is returned.
     *
     * @param string the string to trim
     * @return the trimmed string or null
     */
    @Nullable
    public static String trimToNull(@Nullable String string) {
        return Optional.ofNullable(string)
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .orElse(null);
    }

    /**
     * Is the boolean string true. Values that represent true are: yes, y, on, and true.
     *
     * @param booleanString The boolean string
     * @return True if it is a valid value
     */
    public static boolean isTrue(String booleanString) {
        if (booleanString == null) {
            return false;
        }
        switch (booleanString) {
            case "yes":
            case "y":
            case "on":
            case "true":
                return true;
            default:
                return false;
        }
    }

    /**
     * Fast split by one character iterable. Implementation omits empty strings.
     * This should be a fast alternative to {@link String#split(String)} where it's used with one character.
     *
     * @param sequence The sequence to split
     * @param splitCharacter The split character
     * @since 2.5.0
     * @return The iterable of possible sequences
     */
    public static Iterable<String> splitOmitEmptyStrings(final CharSequence sequence, final char splitCharacter) {
        Objects.requireNonNull(sequence);
        return new SplitOmitEmptyIterator(sequence, splitCharacter);
    }

    /**
     * Fast split by one character iterable. Implementation omits empty strings.
     * This should be a fast alternative to {@link String#split(String)} where it's used with one character.
     *
     * @param sequence The sequence to split
     * @param splitCharacter The split character
     * @since 2.5.0
     * @return The list of possible sequences
     */
    public static List<String> splitOmitEmptyStringsList(final CharSequence sequence, final char splitCharacter) {
        Objects.requireNonNull(sequence);
        int count = 0;
        for (int i = 0; i < sequence.length(); i++) {
            if (sequence.charAt(i) == splitCharacter) {
                count++;
            }
        }
        List<String> result = new ArrayList<>(count + 1);
        SplitOmitEmptyIterator iterator = new SplitOmitEmptyIterator(sequence, splitCharacter);
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    /**
     * Fast split by one character iterator. Implementation omits empty strings.
     * This should be a fast alternative to {@link String#split(String)} where it's used with one character.
     *
     * @param sequence The sequence to split
     * @param splitCharacter The split character
     * @since 2.5.0
     * @return The iterator of possible sequences
     */
    public static Iterator<String> splitOmitEmptyStringsIterator(final CharSequence sequence, final char splitCharacter) {
        Objects.requireNonNull(sequence);
        return new SplitOmitEmptyIterator(sequence, splitCharacter);
    }

    /**
     * Fast split by one character iterator.
     *
     * @author Denis Stepanov
     * @since 2.5.0
     */
    private static final class SplitOmitEmptyIterator implements Iterator<String>, Iterable<String> {

        private final CharSequence sequence;
        private final char splitCharacter;
        private final int length;
        private int index = 0;
        private int fromIndex = 0;
        private int toIndex = 0;
        private boolean end = false;
        private boolean hasNext = true;
        private boolean adjust = true;

        private SplitOmitEmptyIterator(CharSequence sequence, char splitCharacter) {
            this.sequence = sequence;
            this.splitCharacter = splitCharacter;
            this.length = sequence.length();
        }

        @Override
        public boolean hasNext() {
            if (adjust) {
                adjust();
            }
            return hasNext;
        }

        @Override
        public String next() {
            if (adjust) {
                adjust();
            }
            if (!hasNext) {
                throw new NoSuchElementException();
            }
            hasNext = false;
            adjust = !end;
            if (fromIndex == 0 && toIndex == length) {
                return sequence.toString();
            }
            return sequence.subSequence(fromIndex, toIndex).toString();
        }

        private void adjust() {
            adjust = false;
            hasNext = false;
            fromIndex = index;
            while (index < length) {
                if (sequence.charAt(index) == splitCharacter) {
                    if (fromIndex == index) {
                        index++;
                        fromIndex = index;
                        continue; // Empty string
                    }
                    hasNext = true;
                    toIndex = index;
                    index++;
                    return;
                } else {
                    index++;
                }
            }
            if (fromIndex != index) {
                toIndex = length;
                hasNext = true;
            }
            end = true;
        }

        @Override
        public Iterator<String> iterator() {
            if (index == 0) {
                return this;
            }
            return new SplitOmitEmptyIterator(sequence, splitCharacter);
        }
    }
}
