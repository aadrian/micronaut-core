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
package io.micronaut.http.server;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.format.ReadableBytes;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.Toggleable;
import io.micronaut.http.HttpVersion;
import io.micronaut.http.context.ServerContextPathProvider;
import io.micronaut.http.server.cors.CorsOriginConfiguration;
import io.micronaut.http.server.util.locale.HttpLocaleResolutionConfiguration;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.scheduling.executor.ThreadSelection;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * <p>A base {@link ConfigurationProperties} for servers.</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@ConfigurationProperties(value = HttpServerConfiguration.PREFIX, cliPrefix = "")
public class HttpServerConfiguration implements ServerContextPathProvider {

    /**
     * The default port value.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int DEFAULT_PORT = 8080;

    /**
     * The prefix used for configuration.
     */
    public static final String PREFIX = "micronaut.server";

    /**
     * The default value random port.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int DEFAULT_RANDOM_PORT = -1;

    /**
     * The default max request size.
     */
    @SuppressWarnings("WeakerAccess")
    public static final long DEFAULT_MAX_REQUEST_SIZE = 1024 * 1024 * 10L; // 10MB

    /**
     * The default max buffer size.
     */
    @SuppressWarnings("WeakerAccess")
    public static final long DEFAULT_MAX_REQUEST_BUFFER_SIZE = 1024 * 1024 * 10L; // 10MB

    /**
     * The default read idle time in minutes.
     */
    @SuppressWarnings("WeakerAccess")
    public static final long DEFAULT_READ_IDLE_TIME_MINUTES = 5L;

    /**
     * The default write idle time in minutes.
     */
    @SuppressWarnings("WeakerAccess")
    public static final long DEFAULT_WRITE_IDLE_TIME_MINUTES = 5L;

    /**
     * The default date header.
     */
    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_DATEHEADER = true;

    /**
     * The default idle time.
     */
    @SuppressWarnings("WeakerAccess")
    public static final long DEFAULT_IDLE_TIME_MINUTES = 5;

    /**
     * The default value for log handled exceptions.
     */
    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_LOG_HANDLED_EXCEPTIONS = false;

    /**
     * The default value for enabling dual protocol (http/https).
     */
    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_DUAL_PROTOCOL = false;

    /**
     * The default value for redirect HTTP to HTTPS when using dual protocal.
     */
    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_HTTP_TO_HTTPS_REDIRECT = false;

    /**
     * The default value whether to dispatch OPTIONS Requests.
     */
    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_DISPATCH_OPTIONS_REQUESTS = false;

    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_SEMICOLON_IS_NORMAL_CHAR = false;

    @SuppressWarnings("WeakerAccess")
    public static final int DEFAULT_MAX_PARAMS = 1024;

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerConfiguration.class);

    private Integer port;
    private String host;
    private Integer readTimeout;
    private long maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;
    private long maxRequestBufferSize = DEFAULT_MAX_REQUEST_BUFFER_SIZE;
    private Duration readIdleTimeout = null;
    private Duration writeIdleTimeout = null;
    private Duration idleTimeout = Duration.ofMinutes(DEFAULT_IDLE_TIME_MINUTES);
    private MultipartConfiguration multipart = new MultipartConfiguration();
    private CorsConfiguration cors = new CorsConfiguration();
    private String serverHeader;
    private boolean dateHeader = DEFAULT_DATEHEADER;
    private boolean logHandledExceptions = DEFAULT_LOG_HANDLED_EXCEPTIONS;
    private HostResolutionConfiguration hostResolution;
    private HttpLocaleResolutionConfigurationProperties localeResolution;
    private String clientAddressHeader;
    private String contextPath;
    private boolean dualProtocol = DEFAULT_DUAL_PROTOCOL;
    private boolean httpToHttpsRedirect = DEFAULT_HTTP_TO_HTTPS_REDIRECT;

    private boolean dispatchOptionsRequests = DEFAULT_DISPATCH_OPTIONS_REQUESTS;

    private HttpVersion httpVersion = HttpVersion.HTTP_1_1;
    private final ApplicationConfiguration applicationConfiguration;
    private Charset defaultCharset;
    private ThreadSelection threadSelection = ThreadSelection.MANUAL;
    private boolean validateUrl = true;
    private boolean notFoundOnMissingBody = true;
    private boolean semicolonIsNormalChar = DEFAULT_SEMICOLON_IS_NORMAL_CHAR;
    private int maxParams = DEFAULT_MAX_PARAMS;

    /**
     * Default constructor.
     */
    public HttpServerConfiguration() {
        this.applicationConfiguration = new ApplicationConfiguration();
    }

    /**
     * @param applicationConfiguration The application configuration
     */
    @Inject
    public HttpServerConfiguration(ApplicationConfiguration applicationConfiguration) {
        if (applicationConfiguration != null) {
            this.defaultCharset = applicationConfiguration.getDefaultCharset();
        }

        this.applicationConfiguration = applicationConfiguration != null ? applicationConfiguration : new ApplicationConfiguration();
    }

    /**
     * The HTTP version to use. Defaults to {@link HttpVersion#HTTP_1_1}.
     * @return The http version
     */
    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    /**
     * Sets the HTTP version to use. Defaults to {@link HttpVersion#HTTP_1_1}.
     * @param httpVersion The http version
     */
    public void setHttpVersion(HttpVersion httpVersion) {
        if (httpVersion != null) {
            this.httpVersion = httpVersion;
        }
    }

    /**
     * @return The {@link ThreadSelection} model to use for the server.
     */
    public @NonNull ThreadSelection getThreadSelection() {
        return threadSelection;
    }

    /**
     * Sets the {@link io.micronaut.scheduling.executor.ThreadSelection} model to use for the server. Default value MANUAL.
     * @param threadSelection The thread selection model
     */
    public void setThreadSelection(ThreadSelection threadSelection) {
        if (threadSelection != null) {
            this.threadSelection = threadSelection;
        }
    }

    /**
     * @return The application configuration instance
     */
    public ApplicationConfiguration getApplicationConfiguration() {
        return applicationConfiguration;
    }

    /**
     * @return The default charset to use
     */
    public Charset getDefaultCharset() {
        return defaultCharset;
    }

    /**
     * @return The default server port
     */
    public Optional<Integer> getPort() {
        return Optional.ofNullable(port);
    }

    /**
     * @return The default host
     */
    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    /**
     * @return The read timeout setting for the server
     * @deprecated This setting does nothing
     */
    @Deprecated
    public Optional<Integer> getReadTimeout() {
        return Optional.ofNullable(readTimeout);
    }

    /**
     * @return Configuration for multipart / file uploads
     */
    public MultipartConfiguration getMultipart() {
        return multipart;
    }

    /**
     * @return Configuration for CORS
     */
    public CorsConfiguration getCors() {
        return cors;
    }

    /**
     * @return The maximum request body size
     */
    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    /**
     * @return The maximum number of bytes from the request that may be buffered if the application requests buffering
     */
    public long getMaxRequestBufferSize() {
        return maxRequestBufferSize;
    }

    /**
     * @return The default amount of time to allow read operation connections  to remain idle
     */
    public Duration getReadIdleTimeout() {
        return Optional.ofNullable(readIdleTimeout).orElse(Duration.ofMinutes(DEFAULT_READ_IDLE_TIME_MINUTES));
    }

    /**
     * @return The default amount of time to allow to write operation connections to remain idle
     */
    public Duration getWriteIdleTimeout() {
        return Optional.ofNullable(writeIdleTimeout).orElse(Duration.ofMinutes(DEFAULT_WRITE_IDLE_TIME_MINUTES));
    }

    /**
     * @return The time to allow an idle connection for
     */
    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * @return The optional server header value
     */
    public Optional<String> getServerHeader() {
        return Optional.ofNullable(serverHeader);
    }

    /**
     * @return True if the date header should be set
     */
    public boolean isDateHeader() {
        return dateHeader;
    }

    /**
     * @return True if exceptions handled by either an error
     * route or exception handler should be logged
     */
    public boolean isLogHandledExceptions() {
        return logHandledExceptions;
    }

    /**
     * @return The host resolution configuration
     */
    @Nullable
    public HostResolutionConfiguration getHostResolution() {
        return hostResolution;
    }

    /**
     * @return The host resolution configuration
     */
    @Nullable
    public HttpLocaleResolutionConfiguration getLocaleResolution() {
        return localeResolution;
    }

    /**
     * @return Which header stores the original client
     */
    public String getClientAddressHeader() {
        return clientAddressHeader;
    }

    /**
     * @return the context path for the web server
     */
    @Override
    public String getContextPath() {
        return contextPath;
    }

    /**
     * @return if dual protocol has been enabled or not
     */
    public boolean isDualProtocol() {
        return dualProtocol;
    }

    /**
     * @return if redirection from HTTP to HTTPS is enabled or not
     */
    public boolean isHttpToHttpsRedirect() {
        return httpToHttpsRedirect;
    }

    /**
     * Set to true to dispatch OPTIONS requests. Default value ({@value #DEFAULT_DISPATCH_OPTIONS_REQUESTS}.
     * @return Whether OPTIONS requests should be dispatched.
     * @since 4.2.0
     */
    public boolean isDispatchOptionsRequests() {
        return dispatchOptionsRequests;
    }

    /**
     * @param defaultCharset The default charset to use
     */
    public void setDefaultCharset(Charset defaultCharset) {
        this.defaultCharset = defaultCharset;
    }

    /**
     * Sets the port to bind to. Default value ({@value #DEFAULT_RANDOM_PORT})
     *
     * @param port The port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Sets the host to bind to.
     * @param host The host
     */
    public void setHost(String host) {
        if (StringUtils.isNotEmpty(host)) {
            this.host = host;
        }
    }

    /**
     * Sets the default read timeout.
     *
     * @param readTimeout The read timeout
     * @deprecated This setting does nothing
     */
    @Deprecated
    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Sets the name of the server header.
     *
     * @param serverHeader The server header
     */
    public void setServerHeader(String serverHeader) {
        this.serverHeader = serverHeader;
    }

    /**
     * Sets the maximum request size. Default value ({@value #DEFAULT_MAX_REQUEST_SIZE} =&gt; // 10MB)
     *
     * @param maxRequestSize The max request size
     */
    public void setMaxRequestSize(@ReadableBytes long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    /**
     * Sets the maximum number of request bytes that will be buffered. Fully streamed requests can
     * still exceed this value. Default value ({@value #DEFAULT_MAX_REQUEST_BUFFER_SIZE} =&gt; // 10MB).
     * Currently limited to {@code 2^31}, if you need longer request bodies, stream them.<br>
     * Note that there is always some internal buffering, so a very low value ({@code < ~64K}) will
     * essentially act like a request size limit.
     *
     * @param maxRequestBufferSize The maximum number of bytes from the request that may be buffered if the application requests buffering
     */
    public void setMaxRequestBufferSize(@ReadableBytes long maxRequestBufferSize) {
        if (maxRequestBufferSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("max-request-buffer-size must be < " + Integer.MAX_VALUE);
        }
        this.maxRequestBufferSize = maxRequestBufferSize;
    }

    /**
     * Sets the amount of time a connection can remain idle without any reads occurring. Default value ({@value #DEFAULT_READ_IDLE_TIME_MINUTES} minutes).
     *
     * @param readIdleTimeout The read idle time
     */
    public void setReadIdleTimeout(Duration readIdleTimeout) {
        this.readIdleTimeout = readIdleTimeout;
    }

    /**
     * Sets the amount of time a connection can remain idle without any writes occurring. Default value ({@value #DEFAULT_WRITE_IDLE_TIME_MINUTES} minutes).
     *
     * @param writeIdleTimeout The write idle time
     */
    public void setWriteIdleTimeout(Duration writeIdleTimeout) {
        this.writeIdleTimeout = writeIdleTimeout;
    }

    /**
     * Sets the idle time of connections for the server. Default value ({@value #DEFAULT_IDLE_TIME_MINUTES} minutes).
     *
     * @param idleTimeout The idle time
     */
    public void setIdleTimeout(Duration idleTimeout) {
        if (idleTimeout != null) {
            this.idleTimeout = idleTimeout;
        }
    }

    /**
     * Sets the multipart configuration.
     *
     * @param multipart The multipart configuration
     */
    public void setMultipart(MultipartConfiguration multipart) {
        this.multipart = multipart;
    }

    /**
     * Sets the cors configuration.
     * @param cors The cors configuration
     */
    public void setCors(CorsConfiguration cors) {
        this.cors = cors;
    }

    /**
     * Sets whether a date header should be sent back. Default value ({@value #DEFAULT_DATEHEADER}).
     *
     * @param dateHeader True if a date header should be sent.
     */
    public void setDateHeader(boolean dateHeader) {
        this.dateHeader = dateHeader;
    }

    /**
     * Sets whether exceptions handled by either an error route or exception handler
     * should still be logged. Default value ({@value #DEFAULT_LOG_HANDLED_EXCEPTIONS }).
     *
     * @param logHandledExceptions True if exceptions should be logged
     */
    public void setLogHandledExceptions(boolean logHandledExceptions) {
        this.logHandledExceptions = logHandledExceptions;
    }

    /**
     * @param hostResolution The host resolution configuration
     */
    public void setHostResolution(HostResolutionConfiguration hostResolution) {
        this.hostResolution = hostResolution;
    }

    /**
     * @param localeResolution The locale resolution configuration
     */
    public void setLocaleResolution(HttpLocaleResolutionConfigurationProperties localeResolution) {
        this.localeResolution = localeResolution;
    }

    /**
     * @param clientAddressHeader The header that stores the original client address
     */
    public void setClientAddressHeader(String clientAddressHeader) {
        this.clientAddressHeader = clientAddressHeader;
    }

    /**
     * Sets the context path for the web server.
     *
     * @param contextPath the context path for the web server
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * @param dualProtocol the dual protocol (http/https) configuration. Default value ({@value #DEFAULT_DUAL_PROTOCOL}).
     */
    public void setDualProtocol(boolean dualProtocol) {
        this.dualProtocol = dualProtocol;
    }

    /**
     * @param httpToHttpsRedirect Set to true to enable redirecting all http requests to the same URL but with
                                  https instead. This should only be used when {@code dualProtocol} is enabled.
                                  Default value ({@value #DEFAULT_HTTP_TO_HTTPS_REDIRECT}). This feature uses
                                  the host resolution capabilities to determine the host to redirect to.
     */
    public void setHttpToHttpsRedirect(boolean httpToHttpsRedirect) {
        this.httpToHttpsRedirect = httpToHttpsRedirect;
    }

    /**
     * Set to true to dispatch OPTIONS requests. Default value ({@value #DEFAULT_DISPATCH_OPTIONS_REQUESTS}.
     * @param dispatchOptionsRequests Set to true to dispatch OPTIONS requests.
     * @since 4.2.0
     */
    public void setDispatchOptionsRequests(boolean dispatchOptionsRequests) {
        this.dispatchOptionsRequests = dispatchOptionsRequests;
    }

    /**
     * If the url should be validated by converting it to {@link java.net.URI}.
     *
     * @param validateUrl The validate URL value
     * @since 4.3.0
     */
    public void setValidateUrl(boolean validateUrl) {
        this.validateUrl = validateUrl;
    }

    /**
     * @return True if the url should be validated
     * @since 4.3.0
     */
    public boolean isValidateUrl() {
        return validateUrl;
    }

    /**
     * @return True if not-found should be returned on missing body. False to return an empty body.
     * @since 4.6
     */
    public boolean isNotFoundOnMissingBody() {
        return notFoundOnMissingBody;
    }

    /**
     * @param notFoundOnMissingBody True if no found should be returned on missing body. False to return an empty body.
     * @since 4.6
     */
    public void setNotFoundOnMissingBody(boolean notFoundOnMissingBody) {
        this.notFoundOnMissingBody = notFoundOnMissingBody;
    }

    /**
     * Returns whether the semicolon is considered a normal character in the query.
     * A "normal" semicolon is one that is not used as a parameter separator.
     *
     * @return {@code true} if the semicolon is a normal character, {@code false} otherwise.
     * @since 4.8
     */
    public boolean isSemicolonIsNormalChar() {
        return semicolonIsNormalChar;
    }

    /**
     * Sets whether the semicolon should be considered a normal character in the query.
     * A "normal" semicolon is one that is not used as a parameter separator.
     *
     * @param semicolonIsNormalChar {@code true} if the semicolon should be a normal character, {@code false} otherwise.
     * @since 4.8
     */
    public void setSemicolonIsNormalChar(boolean semicolonIsNormalChar) {
        this.semicolonIsNormalChar = semicolonIsNormalChar;
    }

    /**
     * @return the maximum parameter count.
     * @since 4.8
     */
    public int getMaxParams() {
        return maxParams;
    }

    /**
     * @param maxParams the maximum parameter count.
     * @since 4.8
     */
    public void setMaxParams(int maxParams) {
        this.maxParams = maxParams;
    }


    /**
     * Configuration for multipart handling.
     */
    @ConfigurationProperties("multipart")
    public static class MultipartConfiguration implements Toggleable {

        /**
         * The default enable value.
         */
        @SuppressWarnings("WeakerAccess")
        public static final boolean DEFAULT_ENABLED = false;

        /**
         * The default max file size.
         */
        @SuppressWarnings("WeakerAccess")
        public static final long DEFAULT_MAX_FILE_SIZE = 1024L * 1024; // 1MB

        /**
         * The default disk value.
         */
        @SuppressWarnings("WeakerAccess")
        public static final boolean DEFAULT_DISK = false;

        /**
         * The default mixed value.
         */
        @SuppressWarnings("WeakerAccess")
        public static final boolean DEFAULT_MIXED = false;

        /**
         * The default threshold value.
         */
        @SuppressWarnings("WeakerAccess")
        public static final long DEFAULT_THRESHOLD = 1024L * 1024 * 10; // 10MB

        private File location;
        private long maxFileSize = DEFAULT_MAX_FILE_SIZE;
        private Boolean enabled;
        private boolean disk = DEFAULT_DISK;
        private boolean mixed = DEFAULT_MIXED;
        private long threshold = DEFAULT_THRESHOLD;

        /**
         * @return The location to store temporary files
         */
        public Optional<File> getLocation() {
            return Optional.ofNullable(location);
        }

        /**
         * @return The max file size. Defaults to 1MB
         */
        public long getMaxFileSize() {
            return maxFileSize;
        }

        /**
         * @return Whether file uploads are enabled. Defaults to true.
         */
        @Override
        public boolean isEnabled() {
            return enabled == null ? DEFAULT_ENABLED : enabled;
        }

        /**
         * The default multipart enabled setting is false for servlets, but effectively
         * true for Netty. This method returns the value only if set by the user.
         *
         * @return The enabled setting.
         */
        @Internal
        public Optional<Boolean> getEnabled() {
            return Optional.ofNullable(enabled);
        }

        /**
         * @return Whether to use disk. Defaults to false.
         */
        public boolean isDisk() {
            return disk;
        }

        /**
         * @return Whether to use a mixed upload
         */
        public boolean isMixed() {
            return mixed;
        }

        /**
         * @return The threshold to trigger storage to disk
         */
        public long getThreshold() {
            return threshold;
        }

        /**
         * Sets the location to store files.
         * @param location The location
         */
        public void setLocation(File location) {
            this.location = location;
        }

        /**
         * Sets the max file size. Default value ({@value #DEFAULT_MAX_FILE_SIZE} =&gt; 1MB).
         * @param maxFileSize The max file size
         */
        public void setMaxFileSize(@ReadableBytes long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        /**
         * Sets whether multipart processing is enabled. Default value ({@value #DEFAULT_ENABLED}).
         * @param enabled True if it is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Sets whether to buffer data to disk or not. Default value ({@value #DEFAULT_DISK}).
         * @param disk True if data should be written to disk
         */
        public void setDisk(boolean disk) {
            this.disk = disk;
        }

        /**
         * Sets whether to buffer data to disk if the size is greater than the
         * threshold. Default value ({@value #DEFAULT_MIXED}).
         *
         * @param mixed True if data should be written to disk after a threshold.
         */
        public void setMixed(boolean mixed) {
            this.mixed = mixed;
        }

        /**
         * Sets the amount of data that should be received that will trigger
         * the data to be stored to disk. Default value ({@value #DEFAULT_THRESHOLD}).
         *
         * @param threshold The threshold
         */
        public void setThreshold(@ReadableBytes long threshold) {
            this.threshold = threshold;
        }
    }

    /**
     * Configuration for CORS.
     */
    @ConfigurationProperties("cors")
    public static class CorsConfiguration implements Toggleable {

        public static final boolean DEFAULT_ENABLED = false;
        public static final boolean DEFAULT_SINGLE_HEADER = false;
        public static final boolean DEFAULT_LOCALHOST_PASS_THROUGH = false;

        private boolean enabled = DEFAULT_ENABLED;
        private boolean singleHeader = DEFAULT_SINGLE_HEADER;
        private boolean localhostPassThrough = DEFAULT_LOCALHOST_PASS_THROUGH;

        private Map<String, CorsOriginConfiguration> configurations = Collections.emptyMap();

        private Map<String, CorsOriginConfiguration> defaultConfiguration = new LinkedHashMap<>(1);

        /**
         * @return Whether cors is enabled. Defaults to false.
         */
        @Override
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * @return Whether localhost pass-through is enabled. Defaults to {@value #DEFAULT_LOCALHOST_PASS_THROUGH}.
         * @since 3.8.5
         */
        public boolean isLocalhostPassThrough() {
            return localhostPassThrough;
        }

        /**
         * @return The cors configurations
         */
        public Map<String, CorsOriginConfiguration> getConfigurations() {
            if (enabled && configurations.isEmpty()) {
                if (defaultConfiguration.isEmpty()) {
                    defaultConfiguration.put("default", new CorsOriginConfiguration());
                }
                return defaultConfiguration;
            }
            return configurations;
        }

        /**
         * @return Whether headers should be combined into a single header
         */
        public boolean isSingleHeader() {
            return singleHeader;
        }

        /**
         * Sets whether CORS is enabled. Default value ({@value #DEFAULT_ENABLED})
         * @param enabled True if CORS is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Sets whether localhost pass-through is enabled. Default value {@value #DEFAULT_LOCALHOST_PASS_THROUGH}.
         * Setting this to true will allow requests to be made to localhost from any origin.
         *
         * @param localhostPassThrough True if localhost pass-through is enabled
         * @since 3.8.5
         */
        public void setLocalhostPassThrough(boolean localhostPassThrough) {
            this.localhostPassThrough = localhostPassThrough;
        }

        /**
         * Sets the CORS configurations.
         * @param configurations The CORS configurations
         */
        public void setConfigurations(Map<String, CorsOriginConfiguration> configurations) {
            this.configurations = configurations;
        }

        /**
         * Sets whether CORS header values should be joined into a single header. Default value ({@value #DEFAULT_SINGLE_HEADER}).
         *
         * @param singleHeader The single header flag
         */
        public void setSingleHeader(boolean singleHeader) {
            this.singleHeader = singleHeader;
        }
    }

    /**
     * Configuration for host resolution with the {@link io.micronaut.http.server.util.HttpHostResolver}.
     */
    @ConfigurationProperties("host-resolution")
    public static class HostResolutionConfiguration {

        private static final Boolean DEFAULT_PORT_IN_HOST = false;

        private String hostHeader;
        private String protocolHeader;
        private String portHeader;
        private boolean portInHost = DEFAULT_PORT_IN_HOST;
        private List<Pattern> allowedHosts = Collections.emptyList();

        /**
         * @return The host header name
         */
        public String getHostHeader() {
            return hostHeader;
        }

        /**
         * @param hostHeader The header name that stores the host
         */
        public void setHostHeader(String hostHeader) {
            this.hostHeader = hostHeader;
        }

        /**
         * @return The protocol header name
         */
        public String getProtocolHeader() {
            return protocolHeader;
        }

        /**
         * @param protocolHeader The header name that stores the protocol
         */
        public void setProtocolHeader(String protocolHeader) {
            this.protocolHeader = protocolHeader;
        }

        /**
         * @return The port header name
         */
        public String getPortHeader() {
            return portHeader;
        }

        /**
         * @param portHeader The header name that stores the port
         */
        public void setPortHeader(String portHeader) {
            this.portHeader = portHeader;
        }

        /**
         * @return If the host header supports a port
         */
        public boolean isPortInHost() {
            return portInHost;
        }

        /**
         * @param portInHost True if the host header supports a port
         *                   appended with {@code :}. Default value ({@link #DEFAULT_PORT_IN_HOST}).
         */
        public void setPortInHost(boolean portInHost) {
            this.portInHost = portInHost;
        }

        /**
         * @return The list of hosts to validate the resolved host against.
         */
        public List<Pattern> getAllowedHosts() {
            return allowedHosts;
        }

        /**
         * @param allowedHosts The list of allowed host regex patterns. Any resolved
         *                     host must match one of the configured hosts if the
         *                     list is supplied. Each host is passed through
         *                     {@link Pattern#compile(String)}.
         */
        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = new ArrayList<>(allowedHosts.size());
            for (String s: allowedHosts) {
                this.allowedHosts.add(Pattern.compile(s));
            }
        }

        /**
         * @return True if any host headers have been configured
         */
        public boolean headersConfigured() {
            return hostHeader != null || protocolHeader != null || portHeader != null;
        }
    }

    /**
     * Configuration for locale resolution used by {@link io.micronaut.http.server.util.locale.HttpLocaleResolver}.
     */
    @ConfigurationProperties("locale-resolution")
    public static class HttpLocaleResolutionConfigurationProperties implements HttpLocaleResolutionConfiguration {

        public static final String PREFIX = HttpServerConfiguration.PREFIX + ".locale-resolution";
        private static final boolean DEFAULT_HEADER_RESOLUTION = true;

        private Locale fixed;
        private String sessionAttribute;
        private String cookieName;
        private boolean header = DEFAULT_HEADER_RESOLUTION;
        private Locale defaultLocale = Locale.getDefault();

        /**
         * @return The fixed locale
         */
        @Override
        @NonNull
        public Optional<Locale> getFixed() {
            return Optional.ofNullable(fixed);
        }

        /**
         * Set the language tag for the locale. Supports BCP 47 language
         * tags (e.g. "en-US") and ISO standard (e.g "en_US").
         *
         * @param fixed The fixed locale
         */
        public void setFixed(@Nullable Locale fixed) {
            this.fixed = fixed;
        }

        /**
         * @return The key in the session that stores the locale
         */
        @Override
        @NonNull
        public Optional<String> getSessionAttribute() {
            return Optional.ofNullable(sessionAttribute);
        }

        /**
         * Sets the key in the session to look for the locale.
         *
         * @param sessionAttribute The session attribute key
         */
        public void setSessionAttribute(@Nullable String sessionAttribute) {
            this.sessionAttribute = sessionAttribute;
        }

        /**
         * @return The locale to be used if one cannot be resolved.
         */
        @Override
        @NonNull
        public Locale getDefaultLocale() {
            return defaultLocale;
        }

        /**
         * Sets the locale that will be used if the locale cannot be
         * resolved through any means. Defaults to the system default.
         *
         * @param defaultLocale The default locale.
         */
        public void setDefaultLocale(@NonNull Locale defaultLocale) {
            this.defaultLocale = defaultLocale;
        }

        /**
         * @return The name of the cookie that contains the locale.
         */
        @Override
        @NonNull
        public Optional<String> getCookieName() {
            return Optional.ofNullable(cookieName);
        }

        /**
         * Sets the name of the cookie that is used to store the locale.
         *
         * @param cookieName The name of the cookie used to store the locale
         */
        public void setCookieName(@Nullable String cookieName) {
            this.cookieName = cookieName;
        }

        /**
         * @return True if the accept header should be searched for the locale.
         */
        @Override
        public boolean isHeader() {
            return header;
        }

        /**
         * Set to true if the locale should be resolved from the `Accept-Language` header.
         * Default value ({@value #DEFAULT_HEADER_RESOLUTION}).
         *
         * @param header Header resolution
         */
        public void setHeader(boolean header) {
            this.header = header;
        }
    }

    /**
     * Allows configuration of properties for the {@link io.micronaut.http.server.netty.body.AbstractFileBodyWriter}.
     *
     * @author James Kleeh
     * @author graemerocher
     * @since 3.1.0
     */
    @ConfigurationProperties("responses.file")
    public static class FileTypeHandlerConfiguration {

        /**
         * The default cache seconds.
         */
        @SuppressWarnings("WeakerAccess")
        public static final int DEFAULT_CACHESECONDS = 60;

        private int cacheSeconds = DEFAULT_CACHESECONDS;
        private CacheControlConfiguration cacheControl = new CacheControlConfiguration();

        /**
         * Default constructor.
         */
        public FileTypeHandlerConfiguration() {
        }

        @Inject
        FileTypeHandlerConfiguration(
            @Nullable @Property(name = "netty.responses.file.cache-seconds") Integer cacheSecondsOld,
            @Nullable @Property(name = "netty.responses.file.cache-control.public") Boolean isPublicOld,
            @Nullable @Property(name = "micronaut.server.netty.responses.file.cache-seconds") Integer cacheSeconds,
            @Nullable @Property(name = "micronaut.server.netty.responses.file.cache-control.public") Boolean isPublic

        ) {

            if (cacheSecondsOld != null) {
                this.cacheSeconds = cacheSecondsOld;
                LOG.warn("The configuration `netty.responses.file.cache-seconds` is deprecated and will be removed in a future release. Use `micronaut.server.responses.file.cache-seconds` instead.");
            }
            if (isPublicOld != null) {
                this.cacheControl.setPublic(isPublicOld);
                LOG.warn("The configuration `netty.responses.file.cache-control.public` is deprecated and will be removed in a future release. Use `micronaut.server.responses.file.cache-control.public` instead.");
            }
            if (cacheSeconds != null) {
                this.cacheSeconds = cacheSeconds;
                LOG.warn("The configuration `micronaut.server.netty.responses.file.cache-seconds` is deprecated and will be removed in a future release. Use `micronaut.server.responses.file.cache-seconds` instead.");
            }
            if (isPublic != null) {
                this.cacheControl.setPublic(isPublic);
                LOG.warn("The configuration `micronaut.server.netty.responses.file.cache-control.public` is deprecated and will be removed in a future release. Use `micronaut.server.responses.file.cache-control.public` instead.");
            }
        }

        /**
         * @return the cache seconds
         */
        public int getCacheSeconds() {
            return cacheSeconds;
        }

        /**
         * Cache Seconds. Default value ({@value #DEFAULT_CACHESECONDS}).
         * @param cacheSeconds cache seconds
         */
        public void setCacheSeconds(int cacheSeconds) {
            this.cacheSeconds = cacheSeconds;
        }

        /**
         * @return The cache control configuration
         */
        public CacheControlConfiguration getCacheControl() {
            return cacheControl;
        }

        /**
         * Sets the cache control configuration.
         *
         * @param cacheControl The cache control configuration
         */
        public void setCacheControl(CacheControlConfiguration cacheControl) {
            this.cacheControl = cacheControl;
        }

        /**
         * Configuration for the Cache-Control header.
         */
        @ConfigurationProperties("cache-control")
        public static class CacheControlConfiguration {

            private static final boolean DEFAULT_PUBLIC_CACHE = false;

            private boolean publicCache = DEFAULT_PUBLIC_CACHE;

            /**
             * Sets whether the cache control is public. Default value ({@value #DEFAULT_PUBLIC_CACHE})
             *
             * @param publicCache Public cache value
             */
            public void setPublic(boolean publicCache) {
                this.publicCache = publicCache;
            }

            /**
             * @return True if the cache control should be public
             */
            @NonNull
            public boolean getPublic() {
                return publicCache;
            }
        }
    }
}
