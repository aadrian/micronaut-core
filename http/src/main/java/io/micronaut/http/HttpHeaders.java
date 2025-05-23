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
package io.micronaut.http;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.type.Headers;
import io.micronaut.http.util.HttpHeadersUtil;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Constants for common HTTP headers. See https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface HttpHeaders extends Headers {

    /**
     * {@code "Accept"}.
     */
    String ACCEPT = "Accept";

    /**
     * {@code "Accept-CH"}.
     */
    String ACCEPT_CH = "Accept-CH";

    /**
     * {@code "Accept-CH"}.
     */
    String ACCEPT_CH_LIFETIME = "Accept-CH-Lifetime";

    /**
     * {@code "Accept-Charset"}.
     */
    String ACCEPT_CHARSET = "Accept-Charset";

    /**
     * {@code "Accept-Encoding"}.
     */
    String ACCEPT_ENCODING = "Accept-Encoding";

    /**
     * {@code "Accept-Language"}.
     */
    String ACCEPT_LANGUAGE = "Accept-Language";

    /**
     * {@code "Accept-Ranges"}.
     */
    String ACCEPT_RANGES = "Accept-Ranges";

    /**
     * {@code "Accept-Patch"}.
     */
    String ACCEPT_PATCH = "Accept-Patch";

    /**
     * {@code "Access-Control-Allow-Credentials"}.
     */
    String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    /**
     * {@code "Access-Control-Allow-Headers"}.
     */
    String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    /**
     * {@code "Access-Control-Allow-Methods"}.
     */
    String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    /**
     * {@code "Access-Control-Allow-Origin"}.
     */
    String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /**
     * {@code "Access-Control-Allow-Private-Network"}.
     * @see <a href="https://developer.chrome.com/blog/private-network-access-preflight">Private Network Access</a>
     * @since 4.3.0
     */
    String ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK = "Access-Control-Allow-Private-Network";

    /**
     * {@code "Access-Control-Expose-Headers"}.
     */
    String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    /**
     * {@code "Access-Control-Max-Age"}.
     */
    String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    /**
     * {@code "Access-Control-Request-Headers"}.
     */
    String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    /**
     * {@code "Access-Control-Request-Method"}.
     */
    String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

    /**
     * {@code "Access-Control-Request-Private-Network"}.
     * @see <a href="https://developer.chrome.com/blog/private-network-access-preflight">Private Network Access</a>
     * @since 4.3.0
     */
    String ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK = "Access-Control-Request-Private-Network";

    /**
     * {@code "Age"}.
     */
    String AGE = "Age";

    /**
     * {@code "Allow"}.
     */
    String ALLOW = "Allow";

    /**
     * {@code "Authorization"}.
     */
    String AUTHORIZATION = "Authorization";

    /**
     * {@code "Authorization"}.
     */
    String AUTHORIZATION_INFO = "Authorization-Info";

    /**
     * {@code "Cache-Control"}.
     */
    String CACHE_CONTROL = "Cache-Control";

    /**
     * {@code "Connection"}.
     */
    String CONNECTION = "Connection";

    /**
     * {@code "Content-Base"}.
     */
    String CONTENT_BASE = "Content-Base";

    /**
     * {@code "Content-Disposition"}.
     */
    String CONTENT_DISPOSITION = "Content-Disposition";

    /**
     * {@code "Content-DPR"}.
     */
    String CONTENT_DPR = "Content-DPR";

    /**
     * {@code "Content-Encoding"}.
     */
    String CONTENT_ENCODING = "Content-Encoding";

    /**
     * {@code "Content-Language"}.
     */
    String CONTENT_LANGUAGE = "Content-Language";

    /**
     * {@code "Content-Length"}.
     */
    String CONTENT_LENGTH = "Content-Length";

    /**
     * {@code "Content-Location"}.
     */
    String CONTENT_LOCATION = "Content-Location";

    /**
     * {@code "Content-Transfer-Encoding"}.
     */
    String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

    /**
     * {@code "Content-MD5"}.
     */
    String CONTENT_MD5 = "Content-MD5";

    /**
     * {@code "Content-Range"}.
     */
    String CONTENT_RANGE = "Content-Range";

    /**
     * {@code "Content-Type"}.
     */
    String CONTENT_TYPE = "Content-Type";

    /**
     * {@code "Cookie"}.
     */
    String COOKIE = "Cookie";

    /**
     * {@code "Cross-Origin-Resource-Policy"}.
     */
    String CROSS_ORIGIN_RESOURCE_POLICY = "Cross-Origin-Resource-Policy";

    /**
     * {@code "Date"}.
     */
    String DATE = "Date";

    /**
     * {@code "Device-Memory"}.
     */
    String DEVICE_MEMORY = "Device-Memory";

    /**
     * {@code "Downlink"}.
     */
    String DOWNLINK = "Downlink";

    /**
     * {@code "DPR"}.
     */
    String DPR = "DPR";

    /**
     * {@code "ECT"}.
     */
    String ECT = "ECT";

    /**
     * {@code "ETag"}.
     */
    String ETAG = "ETag";

    /**
     * {@code "Expect"}.
     */
    String EXPECT = "Expect";

    /**
     * {@code "Expires"}.
     */
    String EXPIRES = "Expires";

    /**
     * {@code "Feature-Policy"}.
     */
    String FEATURE_POLICY = "Feature-Policy";

    /**
     * {@code "Forwarded"}.
     */
    String FORWARDED = "Forwarded";

    /**
     * {@code "From"}.
     */
    String FROM = "From";

    /**
     * {@code "Host"}.
     */
    String HOST = "Host";

    /**
     * {@code "If-Match"}.
     */
    String IF_MATCH = "If-Match";

    /**
     * {@code "If-Modified-Since"}.
     */
    String IF_MODIFIED_SINCE = "If-Modified-Since";

    /**
     * {@code "If-None-Match"}.
     */
    String IF_NONE_MATCH = "If-None-Match";

    /**
     * {@code "If-Range"}.
     */
    String IF_RANGE = "If-Range";

    /**
     * {@code "If-Unmodified-Since"}.
     */
    String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    /**
     * {@code "Last-Modified"}.
     */
    String LAST_MODIFIED = "Last-Modified";

    /**
     * {@code "Link"}.
     */
    String LINK = "Link";

    /**
     * {@code "Location"}.
     */
    String LOCATION = "Location";

    /**
     * {@code "Max-Forwards"}.
     */
    String MAX_FORWARDS = "Max-Forwards";

    /**
     * {@code "Origin"}.
     */
    String ORIGIN = "Origin";

    /**
     * {@code "Pragma"}.
     */
    String PRAGMA = "Pragma";

    /**
     * {@code "Proxy-Authenticate"}.
     */
    String PROXY_AUTHENTICATE = "Proxy-Authenticate";

    /**
     * {@code "Proxy-Authorization"}.
     */
    String PROXY_AUTHORIZATION = "Proxy-Authorization";

    /**
     * {@code "Range"}.
     */
    String RANGE = "Range";

    /**
     * {@code "Referer"}.
     */
    String REFERER = "Referer";

    /**
     * {@code "Referrer-Policy"}.
     */
    String REFERRER_POLICY = "Referrer-Policy";

    /**
     * {@code "Retry-After"}.
     */
    String RETRY_AFTER = "Retry-After";

    /**
     * {@code "RTT"}.
     */
    String RTT = "RTT";

    /**
     * {@code "Save-Data"}.
     */
    String SAVE_DATA = "Save-Data";

    /**
     * {@code "Sec-WebSocket-Key1"}.
     */
    String SEC_WEBSOCKET_KEY1 = "Sec-WebSocket-Key1";

    /**
     * {@code "Sec-WebSocket-Key2"}.
     */
    String SEC_WEBSOCKET_KEY2 = "Sec-WebSocket-Key2";

    /**
     * {@code "Sec-WebSocket-Location"}.
     */
    String SEC_WEBSOCKET_LOCATION = "Sec-WebSocket-Location";

    /**
     * {@code "Sec-WebSocket-Origin"}.
     */
    String SEC_WEBSOCKET_ORIGIN = "Sec-WebSocket-Origin";

    /**
     * {@code "Sec-WebSocket-Protocol"}.
     */
    String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

    /**
     * {@code "Sec-WebSocket-Version"}.
     */
    String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

    /**
     * {@code "Sec-WebSocket-Key"}.
     */
    String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

    /**
     * {@code "Sec-WebSocket-Accept"}.
     */
    String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

    /**
     * {@code "Server"}.
     */
    String SERVER = "Server";

    /**
     * {@code "Set-Cookie"}.
     */
    String SET_COOKIE = "Set-Cookie";

    /**
     * {@code "Set-Cookie2"}.
     */
    String SET_COOKIE2 = "Set-Cookie2";

    /**
     * {@code "Source-Map"}.
     */
    String SOURCE_MAP = "SourceMap";

    /**
     * {@code "TE"}.
     */
    String TE = "TE";

    /**
     * {@code "Trailer"}.
     */
    String TRAILER = "Trailer";

    /**
     * {@code "Transfer-Encoding"}.
     */
    String TRANSFER_ENCODING = "Transfer-Encoding";

    /**
     * {@code "Upgrade"}.
     */
    String UPGRADE = "Upgrade";

    /**
     * {@code "User-Agent"}.
     */
    String USER_AGENT = "User-Agent";

    /**
     * {@code "Vary"}.
     */
    String VARY = "Vary";

    /**
     * {@code "Via"}.
     */
    String VIA = "Via";

    /**
     * {@code "Viewport-Width"}.
     */
    String VIEWPORT_WIDTH = "Viewport-Width";

    /**
     * {@code "Warning"}.
     */
    String WARNING = "Warning";

    /**
     * {@code "WebSocket-Location"}.
     */
    String WEBSOCKET_LOCATION = "WebSocket-Location";

    /**
     * {@code "WebSocket-Origin"}.
     */
    String WEBSOCKET_ORIGIN = "WebSocket-Origin";

    /**
     * {@code "WebSocket-Protocol"}.
     */
    String WEBSOCKET_PROTOCOL = "WebSocket-Protocol";

    /**
     * {@code "Width"}.
     */
    String WIDTH = "Width";

    /**
     * {@code "WWW-Authenticate"}.
     */
    String WWW_AUTHENTICATE = "WWW-Authenticate";

    /**
     * {@code "X-Auth-Token"}.
     */
    String X_AUTH_TOKEN = "X-Auth-Token";

    /**
     * Unmodifiable List of every header constant defined in {@link HttpHeaders}.
     */
    List<String> STANDARD_HEADERS = Collections.unmodifiableList(Arrays.asList(
        ACCEPT,
        ACCEPT,
        ACCEPT_CH,
        ACCEPT_CH_LIFETIME,
        ACCEPT_CHARSET,
        ACCEPT_ENCODING,
        ACCEPT_LANGUAGE,
        ACCEPT_RANGES,
        ACCEPT_PATCH,
        ACCESS_CONTROL_ALLOW_CREDENTIALS,
        ACCESS_CONTROL_ALLOW_HEADERS,
        ACCESS_CONTROL_ALLOW_METHODS,
        ACCESS_CONTROL_ALLOW_ORIGIN,
        ACCESS_CONTROL_EXPOSE_HEADERS,
        ACCESS_CONTROL_MAX_AGE,
        ACCESS_CONTROL_REQUEST_HEADERS,
        ACCESS_CONTROL_REQUEST_METHOD,
        ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK,
        AGE,
        ALLOW,
        AUTHORIZATION,
        AUTHORIZATION_INFO,
        CACHE_CONTROL,
        CONNECTION,
        CONTENT_BASE,
        CONTENT_DISPOSITION,
        CONTENT_DPR,
        CONTENT_ENCODING,
        CONTENT_LANGUAGE,
        CONTENT_LENGTH,
        CONTENT_LOCATION,
        CONTENT_TRANSFER_ENCODING,
        CONTENT_MD5,
        CONTENT_RANGE,
        CONTENT_TYPE,
        COOKIE,
        CROSS_ORIGIN_RESOURCE_POLICY,
        DATE,
        DEVICE_MEMORY,
        DOWNLINK,
        DPR,
        ECT,
        ETAG,
        EXPECT,
        EXPIRES,
        FEATURE_POLICY,
        FORWARDED,
        FROM,
        HOST,
        IF_MATCH,
        IF_MODIFIED_SINCE,
        IF_NONE_MATCH,
        IF_RANGE,
        IF_UNMODIFIED_SINCE,
        LAST_MODIFIED,
        LINK,
        LOCATION,
        MAX_FORWARDS,
        ORIGIN,
        PRAGMA,
        PROXY_AUTHENTICATE,
        PROXY_AUTHORIZATION,
        RANGE,
        REFERER,
        REFERRER_POLICY,
        RETRY_AFTER,
        RTT,
        SAVE_DATA,
        SEC_WEBSOCKET_KEY1,
        SEC_WEBSOCKET_KEY2,
        SEC_WEBSOCKET_LOCATION,
        SEC_WEBSOCKET_ORIGIN,
        SEC_WEBSOCKET_PROTOCOL,
        SEC_WEBSOCKET_VERSION,
        SEC_WEBSOCKET_KEY,
        SEC_WEBSOCKET_ACCEPT,
        SERVER,
        SET_COOKIE,
        SET_COOKIE2,
        SOURCE_MAP,
        TE,
        TRAILER,
        TRANSFER_ENCODING,
        UPGRADE,
        USER_AGENT,
        VARY,
        VIA,
        VIEWPORT_WIDTH,
        WARNING,
        WEBSOCKET_LOCATION,
        WEBSOCKET_ORIGIN,
        WEBSOCKET_PROTOCOL,
        WIDTH,
        WWW_AUTHENTICATE,
        X_AUTH_TOKEN
    ));

    /**
     * Whether the given key is contained within these values.
     *
     * @param name The key name
     * @return True if it is
     * @since 4.8.0
     */
    default boolean contains(CharSequence name) {
        return contains(name.toString());
    }

    /**
     * Obtain the date header.
     *
     * @param name The header name
     * @return The date header as a {@link ZonedDateTime} otherwise if it is not present or cannot be parsed
     * {@link Optional#empty()}
     */
    default Optional<ZonedDateTime> findDate(CharSequence name) {
        try {
            return findFirst(name).map(str -> {
                    LocalDateTime localDateTime = LocalDateTime.parse(str, DateTimeFormatter.RFC_1123_DATE_TIME);
                    return ZonedDateTime.of(localDateTime, ZoneId.of("GMT"));
                }

            );
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /**
     * Obtain the date header.
     *
     * @param name The header name
     * @return The date header as a {@link ZonedDateTime} otherwise if it is not present or cannot be parsed null
     */
    default ZonedDateTime getDate(CharSequence name) {
        return findDate(name).orElse(null);
    }

    /**
     * Obtain an integer header.
     *
     * @param name The header name
     * @return The date header as a {@link ZonedDateTime} otherwise if it is not present or cannot be parsed null
     */
    default Integer getInt(CharSequence name) {
        return findInt(name).orElse(null);
    }

    /**
     * Find an integer header.
     *
     * @param name The name of the header
     * @return An {@link Optional} of {@link Integer}
     */
    default Optional<Integer> findInt(CharSequence name) {
        return get(name, ConversionContext.INT);
    }

    /**
     * Get the first value of the given header.
     *
     * @param name The header name
     * @return The first value or null if it is present
     */
    default Optional<String> findFirst(CharSequence name) {
        return getFirst(name, ConversionContext.STRING);
    }

    /**
     * The request or response content type.
     *
     * @return The content type
     */
    default Optional<MediaType> contentType() {
        return getFirst(HttpHeaders.CONTENT_TYPE, MediaType.CONVERSION_CONTEXT);
    }

    /**
     * The request or response content type.
     *
     * @return The content type
     */
    default OptionalLong contentLength() {
        final Long aLong = getFirst(HttpHeaders.CONTENT_LENGTH, ConversionContext.LONG).orElse(null);
        if (aLong != null) {
            return OptionalLong.of(aLong);
        } else {
            return OptionalLong.empty();
        }
    }

    /**
     * A list of accepted {@link MediaType} instances.
     *
     * @return A list of zero or many {@link MediaType} instances
     */
    default List<MediaType> accept() {
        return MediaType.orderedOf(getAll(HttpHeaders.ACCEPT));
    }

    /**
     * The {@code Accept-Charset} header, or {@code null} if unset.
     *
     * @return The {@code Accept-Charset} header
     * @since 4.0.0
     */
    @Nullable
    default Charset acceptCharset() {
        return findAcceptCharset().orElse(null);
    }

    /**
     * The {@code Accept-Charset} header, or empty if unset.
     *
     * @return The {@code Accept-Charset} header
     * @since 4.3.0
     */
    default Optional<Charset> findAcceptCharset() {
        return findFirst(HttpHeaders.ACCEPT_CHARSET)
            .map(HttpHeadersUtil::parseAcceptCharset);
    }

    /**
     * The {@code Accept-Language} header, or {@code null} if unset.
     *
     * @return The {@code Accept-Language} header
     * @since 4.0.0
     */
    @Nullable
    default Locale acceptLanguage() {
        return findAcceptLanguage().orElse(null);
    }

    /**
     * The {@code Accept-Language} header, or empty if unset.
     *
     * @return The {@code Accept-Language} header
     * @since 4.3.0
     */
    default Optional<Locale> findAcceptLanguage() {
        return findFirst(HttpHeaders.ACCEPT_LANGUAGE)
            .map(text -> {
                String part = HttpHeadersUtil.splitAcceptHeader(text);
                return part == null ? Locale.getDefault() : Locale.forLanguageTag(part);
            });
    }

    /**
     * @return Whether the {@link HttpHeaders#CONNECTION} header is set to Keep-Alive
     */
    default boolean isKeepAlive() {
        return findFirst(CONNECTION)
                 .map(val -> val.equalsIgnoreCase(HttpHeaderValues.CONNECTION_KEEP_ALIVE)).orElse(false);
    }

    /**
     * @return The {@link #ORIGIN} header
     */
    default Optional<String> getOrigin() {
        return findFirst(ORIGIN);
    }

    /**
     * @return The {@link #AUTHORIZATION} header
     */
    default Optional<String> getAuthorization() {
        return findFirst(AUTHORIZATION);
    }

    /**
     * @return The {@link #CONTENT_TYPE} header
     */
    default Optional<String> getContentType() {
        return findFirst(CONTENT_TYPE);
    }
}
