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
package io.micronaut.context.env;

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.Toggleable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An abstract implementation of the {@link PropertySourceLoader} interface.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractPropertySourceLoader implements PropertySourceLoader, Toggleable, Ordered {

    /**
     * Default position for the property source loader.
     */
    public static final int DEFAULT_POSITION = EnvironmentPropertySource.POSITION - 100;

    protected Logger log;

    protected AbstractPropertySourceLoader() {
        this(true);
    }

    protected AbstractPropertySourceLoader(boolean logEnabled) {
        log = logEnabled ? LoggerFactory.getLogger(getClass()) : NOPLogger.NOP_LOGGER;
    }

    @Override
    public int getOrder() {
        return DEFAULT_POSITION;
    }

    @Override
    public Optional<PropertySource> load(String resourceName, ResourceLoader resourceLoader) {
        return load(resourceLoader, resourceName, getOrder());
    }

    @Override
    public Optional<PropertySource> loadEnv(String resourceName, ResourceLoader resourceLoader, ActiveEnvironment activeEnvironment) {
        return load(resourceLoader, resourceName + "-" + activeEnvironment.getName(), this.getOrder() + 1 + activeEnvironment.getPriority());
    }

    private Optional<PropertySource> load(ResourceLoader resourceLoader, String fileName, int order) {
        if (isEnabled()) {
            Set<String> extensions = getExtensions();
            for (String ext : extensions) {
                String fileExt = fileName +  "." + ext;
                Map<String, Object> finalMap = loadProperties(resourceLoader, fileName, fileExt);

                if (!finalMap.isEmpty()) {
                    return Optional.of(
                        createPropertySource(fileName, finalMap, order, PropertySource.Origin.of(fileExt))
                    );
                }
            }
        }

        return Optional.empty();
    }

    /**
     *
     * @param name The name of the property source
     * @param map  The map
     * @param order The order of the property source
     * @return property source
     */
    protected MapPropertySource createPropertySource(String name, Map<String, Object> map, int order, PropertySource.Origin origin) {
        return new MapPropertySource(name, map) {
            @Override
            public int getOrder() {
                return order;
            }

            @Override
            public Origin getOrigin() {
                return origin != null ? origin : super.getOrigin();
            }
        };
    }


    /**
     *
     * @param name The name of the property source
     * @param map  The map
     * @param order The order of the property source
     * @return property source
     * @deprecated Use {@link #createPropertySource(String, Map, int, PropertySource.Origin)}
     */
    @Deprecated(forRemoval = true)
    protected MapPropertySource createPropertySource(String name, Map<String, Object> map, int order) {
        return new MapPropertySource(name, map) {
                            @Override
                            public int getOrder() {
                                return order;
                            }
                        };
    }

    private Map<String, Object> loadProperties(ResourceLoader resourceLoader, String qualifiedName, String fileName) {
        Optional<InputStream> config = readInput(resourceLoader, fileName);
        if (config.isPresent()) {
            log.debug("Found PropertySource for file name: {}", fileName);
            try (InputStream input = config.get()) {
                return read(qualifiedName, input);
            } catch (IOException e) {
                throw new ConfigurationException("I/O exception occurred reading [" + fileName + "]: " + e.getMessage(), e);
            }
        } else {
            log.debug("No PropertySource found for file name: {}", fileName);
        }
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> read(String name, InputStream input) throws IOException {
        Map<String, Object> finalMap = new LinkedHashMap<>();
        processInput(name, input, finalMap);
        return finalMap;
    }

    /**
     * @param resourceLoader The resource loader
     * @param fileName       The file name
     * @return An input stream wrapped inside an {@link Optional}
     */
    protected Optional<InputStream> readInput(ResourceLoader resourceLoader, String fileName) {
        return resourceLoader.getResourceAsStream(fileName);
    }

    /**
     * @param name     The name
     * @param input    The input stream
     * @param finalMap The map with all the properties processed
     * @throws IOException If the input stream doesn't exist
     */
    protected abstract void processInput(String name, InputStream input, Map<String, Object> finalMap) throws IOException;

    /**
     * @param finalMap The map with all the properties processed
     * @param map      The map to process
     * @param prefix   The prefix for the keys
     */
    protected void processMap(Map<String, Object> finalMap, Map map, String prefix) {
        for (Object o : map.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            if (value instanceof Map map1 && !map1.isEmpty()) {
                processMap(finalMap, map1, prefix + key + '.');
            } else {
                finalMap.put(prefix + key, value);
            }
        }
    }

    /**
     * Return logEnabled value.
     *
     * @return is log enabled
     * @deprecated don't need to have this method
     *
     * @since 3.9.0
     */
    @Deprecated
    public boolean isLogEnabled() {
        return !(log instanceof NOPLogger);
    }

    /**
     * Setter for logEnabled.
     *
     * @param logEnabled is log enabled
     *
     * @deprecated set logEnabled value by constructor
     *
     * @since 3.9.0
     */
    @Deprecated
    public void setLogEnabled(boolean logEnabled) {
        if (logEnabled) {
            log = LoggerFactory.getLogger(getClass());
        } else {
            log = NOPLogger.NOP_LOGGER;
        }
    }

}
