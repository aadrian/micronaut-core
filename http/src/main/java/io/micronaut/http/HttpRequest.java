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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.cookie.Cookies;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>Common interface for HTTP request implementations.</p>
 *
 * @param <B> The Http message body
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("MethodName")
public interface HttpRequest<B> extends HttpMessage<B> {

    /**
     * Constant for HTTP scheme.
     */
    String SCHEME_HTTP = "http";

    /**
     * Constant for HTTPS scheme.
     */
    String SCHEME_HTTPS = "https";

    /**
     * @return The {@link Cookies} instance
     */
    @NonNull Cookies getCookies();

    /**
     * @return The HTTP parameters contained with the URI query string
     */
    @NonNull HttpParameters getParameters();

    /**
     * @return The request method
     */
    @NonNull HttpMethod getMethod();

    /**
     * @return The full request URI
     */
    @NonNull URI getUri();

    /**
     * Returns a new request object that allows mutation.
     * @return The mutable request
     * @since 2.0.0
     */
    default MutableHttpRequest<B> mutate() {
        throw new UnsupportedOperationException("Request is immutable");
    }

    /**
     * @return The http version of the request.
     */
    default HttpVersion getHttpVersion() {
        return HttpVersion.HTTP_1_1;
    }

    /**
     * A list of accepted {@link MediaType} instances sorted by their quality rating.
     *
     * @return A list of zero or many {@link MediaType} instances
     */
    default Collection<MediaType> accept() {
        return getHeaders().accept();
    }

    /**
     *
     * @return The name of the method (same as {@link HttpMethod} value for standard http methods).
     */
    default @NonNull String getMethodName() {
        return getMethod().name();
    }

    /**
     * The user principal stored within the request.
     *
     * @return The principal
     * @since 1.0.4
     */
    default @NonNull Optional<Principal> getUserPrincipal() {
        return getAttribute(HttpAttributes.PRINCIPAL, Principal.class);
    }

    /**
     * The user principal stored within the request.
     *
     * @param principalType The principal type
     * @return The principal
     * @param <T> The principal type
     * @since 1.0.4
     */
    default @NonNull <T extends Principal> Optional<T> getUserPrincipal(Class<T> principalType) {
        return getAttribute(HttpAttributes.PRINCIPAL, principalType);
    }

    /**
     * Set the user principal.
     *
     * @param principal The principal
     * @since 4.8.0
     */
    default void setUserPrincipal(@Nullable Principal principal) {
        if (principal != null) {
            setAttribute(HttpAttributes.PRINCIPAL, principal);
        } else {
            removeAttribute(HttpAttributes.PRINCIPAL, Principal.class);
        }
    }

    /**
     * @return Get the raw, percent-encoded path without any parameters
     */
    default @NonNull String getPath() {
        return getUri().getRawPath();
    }

    /**
     * @return Obtain the remote address
     */
    default @NonNull InetSocketAddress getRemoteAddress() {
        return getServerAddress();
    }

    /**
     * @return Obtain the server address
     */
    default @NonNull InetSocketAddress getServerAddress() {
        String host = getUri().getHost();
        int port = getUri().getPort();
        return new InetSocketAddress(host != null ? host : "localhost", port > -1 ? port : 80);
    }

    /**
     * @return The server host name
     */
    default @Nullable
    String getServerName() {
        return getUri().getHost();
    }

    /**
     * @return Is the request an HTTPS request
     */
    default boolean isSecure() {
        String scheme = getUri().getScheme();
        return scheme != null && scheme.equals("https");
    }

    @Override
    default HttpRequest<B> setAttribute(CharSequence name, Object value) {
        return (HttpRequest<B>) HttpMessage.super.setAttribute(name, value);
    }

    @Override
    default Optional<Locale> getLocale() {
        return getHeaders().findAcceptLanguage();
    }

    /**
     * Retrieves the Certificate used for mutual authentication.
     *
     * @return A certificate used for authentication, if applicable.
     */
    @SuppressWarnings("deprecation")
    default Optional<Certificate> getCertificate() {
        Optional<Certificate> attribute = this.getAttribute(HttpAttributes.X509_CERTIFICATE, Certificate.class);
        if (attribute.isPresent()) {
            return attribute;
        }
        Optional<SSLSession> session = getSslSession();
        if (session.isPresent()) {
            try {
                return Optional.of(session.get().getPeerCertificates()[0]);
            } catch (SSLPeerUnverifiedException e) {
                // won't return unverified cert
            }
        }
        return Optional.empty();
    }

    /**
     * Get the SSL session used for the connection to the client, if available.
     *
     * @return The session
     */
    default Optional<SSLSession> getSslSession() {
        return Optional.empty();
    }

    /**
     * Get the origin header.
     *
     * @return The origin header
     * @see HttpHeaders#getOrigin()
     */
    default Optional<String> getOrigin() {
        return getHeaders().getOrigin();
    }

    /**
     * Return a {@link MutableHttpRequest} for a {@link HttpMethod#GET} request for the given URI.
     *
     * @param uri The URI
     * @param <T> The Http request type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> GET(URI uri) {
        return GET(uri.toString());
    }

    /**
     * Return a {@link MutableHttpRequest} for a {@link HttpMethod#GET} request for the given URI.
     *
     * @param uri The URI
     * @param <T> The Http request type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> GET(String uri) {
        return HttpRequestFactory.INSTANCE.get(uri);
    }

    /**
     * Return a {@link MutableHttpRequest} for a {@link HttpMethod#OPTIONS} request for the given URI.
     *
     * @param uri The URI
     * @param <T> The Http request type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> OPTIONS(URI uri) {
        return OPTIONS(uri.toString());
    }

    /**
     * Return a {@link MutableHttpRequest} for a {@link HttpMethod#OPTIONS} request for the given URI.
     *
     * @param uri The URI
     * @param <T> The Http request type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> OPTIONS(String uri) {
        return HttpRequestFactory.INSTANCE.options(uri);
    }

    /**
     * Return a {@link MutableHttpRequest} for a {@link HttpMethod#HEAD} request for the given URI.
     *
     * @param uri The URI
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static MutableHttpRequest<?> HEAD(URI uri) {
        return HEAD(uri.toString());
    }

    /**
     * Return a {@link MutableHttpRequest} for a {@link HttpMethod#HEAD} request for the given URI.
     *
     * @param uri The URI
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static MutableHttpRequest<?> HEAD(String uri) {
        return HttpRequestFactory.INSTANCE.head(uri);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#POST} request for the given URI.
     *
     * @param uri  The URI
     * @param body The body of the request (content type defaults to {@link MediaType#APPLICATION_JSON}
     * @param <T>  The body type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> POST(URI uri, T body) {
        return POST(uri.toString(), body);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#POST} request for the given URI.
     *
     * @param uri  The URI
     * @param body The body of the request (content type defaults to {@link MediaType#APPLICATION_JSON}
     * @param <T>  The body type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> POST(String uri, T body) {
        Objects.requireNonNull(uri, "Argument [uri] is required");
        return HttpRequestFactory.INSTANCE.post(uri, body);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#PUT} request for the given URI.
     *
     * @param uri  The URI
     * @param body The body of the request (content type defaults to {@link MediaType#APPLICATION_JSON}
     * @param <T>  The body type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> PUT(URI uri, T body) {
        return PUT(uri.toString(), body);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#PUT} request for the given URI.
     *
     * @param uri  The URI
     * @param body The body of the request (content type defaults to {@link MediaType#APPLICATION_JSON}
     * @param <T>  The body type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> PUT(String uri, T body) {
        Objects.requireNonNull(uri, "Argument [uri] is required");
        return HttpRequestFactory.INSTANCE.put(uri, body);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#PATCH} request for the given URI.
     *
     * @param uri  The URI
     * @param body The body of the request (content type defaults to {@link MediaType#APPLICATION_JSON}
     * @param <T>  The body type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> PATCH(URI uri, T body) {
        return PATCH(uri.toString(), body);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#PATCH} request for the given URI.
     *
     * @param uri  The URI
     * @param body The body of the request (content type defaults to {@link MediaType#APPLICATION_JSON}
     * @param <T>  The body type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> PATCH(String uri, T body) {
        Objects.requireNonNull(uri, "Argument [uri] is required");
        return HttpRequestFactory.INSTANCE.patch(uri, body);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#DELETE} request for the given URI.
     *
     * @param uri  The URI
     * @param body The body of the request (content type defaults to {@link MediaType#APPLICATION_JSON}
     * @param <T>  The body type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> DELETE(URI uri, T body) {
        return DELETE(uri.toString(), body);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#DELETE} request for the given URI.
     *
     * @param uri  The URI
     * @param body The body of the request (content type defaults to {@link MediaType#APPLICATION_JSON}
     * @param <T>  The body type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> DELETE(String uri, T body) {
        Objects.requireNonNull(uri, "Argument [uri] is required");
        return HttpRequestFactory.INSTANCE.delete(uri, body);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#DELETE} request for the given URI.
     *
     * @param uri The URI
     * @param <T> The Http request type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> DELETE(String uri) {
        return DELETE(uri, null);
    }

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#DELETE} request for the given URI.
     *
     * @param uri The URI
     * @param <T> The Http request type
     * @return The {@link MutableHttpRequest} instance
     * @see HttpRequestFactory
     */
    static <T> MutableHttpRequest<T> DELETE(URI uri) {
        return DELETE(uri.toString(), null);
    }

    /**
     * Create a new {@link MutableHttpRequest} for the given method and URI.
     *
     * @param httpMethod The method
     * @param uri        The URI
     * @param <T>        The Http request type
     * @return The request
     */
    static <T> MutableHttpRequest<T> create(HttpMethod httpMethod, String uri) {
        Objects.requireNonNull(httpMethod, "Argument [httpMethod] is required");
        return create(httpMethod, uri, httpMethod.name());
    }

    /**
     * Create a new {@link MutableHttpRequest} for the given method and URI.
     *
     * @param httpMethod The method
     * @param uri        The URI
     * @param <T>        The Http request type
     * @param httpMethodName Method name - for standard http methods is equal to {@link HttpMethod#name()}
     * @return The request
     */
    static <T> MutableHttpRequest<T> create(HttpMethod httpMethod, String uri, String httpMethodName) {
        Objects.requireNonNull(httpMethod, "Argument [httpMethod] is required");
        Objects.requireNonNull(uri, "Argument [uri] is required");
        Objects.requireNonNull(httpMethodName, "Argument [httpMethodName] is required");
        return HttpRequestFactory.INSTANCE.create(httpMethod, uri, httpMethodName);
    }

    /**
     * Returns a mutable request based on this request.
     * @return the mutable request
     * @since 4.7
     */
    default MutableHttpRequest<B> toMutableRequest() {
        if (this instanceof MutableHttpRequest<B> mutableHttpRequest) {
            return mutableHttpRequest;
        }
        MutableHttpRequest<B> mutableHttpRequest = HttpRequest.create(getMethod(), getUri().toString());
        getBody().ifPresent(mutableHttpRequest::body);
        getHeaders().forEach((name, value) -> {
            for (String val : value) {
                mutableHttpRequest.header(name, val);
            }
        });
        mutableHttpRequest.getAttributes().putAll(getAttributes());
        return mutableHttpRequest;
    }
}
