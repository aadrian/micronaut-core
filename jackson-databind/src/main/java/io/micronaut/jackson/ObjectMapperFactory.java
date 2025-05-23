/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TSFBuilder;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Type;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.reflect.GenericTypeUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.jackson.serialize.MicronautDeserializers;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Factory bean for creating the Jackson {@link com.fasterxml.jackson.databind.ObjectMapper}.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Factory
@BootstrapContextCompatible
public class ObjectMapperFactory {

    /**
     * Name for Micronaut module.
     */
    public static final String MICRONAUT_MODULE = "micronaut";

    @Inject
    protected ConversionService conversionService;

    @Inject
    protected BeanContext beanContext;

    @Inject
    // have to be fully qualified due to JDK Module type
    protected com.fasterxml.jackson.databind.Module[] jacksonModules = new com.fasterxml.jackson.databind.Module[0];

    @Inject
    protected JsonSerializer[] serializers = new JsonSerializer[0];

    @Inject
    protected JsonDeserializer[] deserializers = new JsonDeserializer[0];

    @Inject
    protected BeanSerializerModifier[] beanSerializerModifiers = new BeanSerializerModifier[0];

    @Inject
    protected BeanDeserializerModifier[] beanDeserializerModifiers = new BeanDeserializerModifier[0];

    @Inject
    protected KeyDeserializer[] keyDeserializers = new KeyDeserializer[0];

    /**
     * Builds default Factory {@link JsonFactory} using properties from {@link JacksonConfiguration}.
     *
     * @param jacksonConfiguration The configuration
     * @return The {@link JsonFactory}
     */
    @Requires(beans = JacksonConfiguration.class)
    @Singleton
    @BootstrapContextCompatible
    public JsonFactory jsonFactory(JacksonConfiguration jacksonConfiguration) {
        final TSFBuilder<?, ?> jsonFactoryBuilder = JsonFactory.builder();
        jacksonConfiguration.getFactorySettings().forEach(jsonFactoryBuilder::configure);
        return jsonFactoryBuilder.build();
    }

    /**
     * Set additional serializers.
     *
     * @param serializers The serializers
     * @since 4.0
     */
    public void setSerializers(JsonSerializer... serializers) {
        this.serializers = serializers;
    }

    /**
     * Set additional deserializers.
     *
     * @param deserializers The deserializers
     * @since 4.0
     */
    public void setDeserializers(JsonDeserializer... deserializers) {
        this.deserializers = deserializers;
    }

    /**
     * Builds the core Jackson {@link ObjectMapper} from the optional configuration and {@link JsonFactory}.
     *
     * @param jacksonConfiguration The configuration
     * @param jsonFactory The JSON factory
     * @return The {@link ObjectMapper}
     */
    @Singleton
    @Primary
    @Named("json")
    @BootstrapContextCompatible
    public ObjectMapper objectMapper(@Nullable JacksonConfiguration jacksonConfiguration,
                                     @Nullable JsonFactory jsonFactory) {
        ObjectMapper objectMapper = jsonFactory != null ? new ObjectMapper(jsonFactory) : new ObjectMapper();

        final boolean hasConfiguration = jacksonConfiguration != null;
        if (!hasConfiguration || jacksonConfiguration.isModuleScan()) {
            objectMapper.findAndRegisterModules();
        }
        objectMapper.registerModules(jacksonModules);
        if (beanContext != null) {
            objectMapper.setTypeFactory(objectMapper.getTypeFactory().withClassLoader(beanContext.getClassLoader()));
        }

        SimpleModule module = new SimpleModule(MICRONAUT_MODULE);
        module.setDeserializers(new MicronautDeserializers(conversionService));

        for (JsonSerializer serializer : serializers) {
            Class<? extends JsonSerializer> type = serializer.getClass();
            Type annotation = type.getAnnotation(Type.class);
            if (annotation != null) {
                Class<?>[] value = annotation.value();
                for (Class<?> aClass : value) {
                    module.addSerializer(aClass, serializer);
                }
            } else {
                Optional<Class<?>> targetType = GenericTypeUtils.resolveSuperGenericTypeArgument(type);
                if (targetType.isPresent()) {
                    module.addSerializer(targetType.get(), serializer);
                } else {
                    module.addSerializer(serializer);
                }
            }
        }

        for (JsonDeserializer deserializer : deserializers) {
            Class<? extends JsonDeserializer> type = deserializer.getClass();
            Type annotation = type.getAnnotation(Type.class);
            if (annotation != null) {
                Class<?>[] value = annotation.value();
                for (Class<?> aClass : value) {
                    module.addDeserializer(aClass, deserializer);
                }
            } else {
                Optional<Class<?>> targetType = GenericTypeUtils.resolveSuperGenericTypeArgument(type);
                targetType.ifPresent(aClass -> module.addDeserializer(aClass, deserializer));
            }
        }

        if (hasConfiguration && jacksonConfiguration.isTrimStrings()) {
            module.addDeserializer(String.class, new StringDeserializer() {
                @Override
                public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String value = super.deserialize(p, ctxt);
                    return StringUtils.trimToNull(value);
                }
            });
        }

        for (KeyDeserializer keyDeserializer : keyDeserializers) {
            Class<? extends KeyDeserializer> type = keyDeserializer.getClass();
            Type annotation = type.getAnnotation(Type.class);
            if (annotation != null) {
                Class<?>[] value = annotation.value();
                for (Class<?> clazz : value) {
                    module.addKeyDeserializer(clazz, keyDeserializer);
                }
            }
        }
        objectMapper.registerModule(module);

        for (BeanSerializerModifier beanSerializerModifier : beanSerializerModifiers) {
            objectMapper.setSerializerFactory(
                objectMapper.getSerializerFactory().withSerializerModifier(
                    beanSerializerModifier
                ));
        }

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);

        if (hasConfiguration) {

            ObjectMapper.DefaultTyping defaultTyping = jacksonConfiguration.getDefaultTyping();
            if (defaultTyping != null) {
                objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), defaultTyping);
            }

            JsonInclude.Include include = jacksonConfiguration.getSerializationInclusion();
            if (include != null) {
                objectMapper.setSerializationInclusion(include);
            }
            String dateFormat = jacksonConfiguration.getDateFormat();
            if (dateFormat != null) {
                objectMapper.setDateFormat(new SimpleDateFormat(dateFormat));
            }
            Locale locale = jacksonConfiguration.getLocale();
            if (locale != null) {
                objectMapper.setLocale(locale);
            }
            TimeZone timeZone = jacksonConfiguration.getTimeZone();
            if (timeZone != null) {
                objectMapper.setTimeZone(timeZone);
            }
            PropertyNamingStrategy propertyNamingStrategy = jacksonConfiguration.getPropertyNamingStrategy();
            if (propertyNamingStrategy != null) {
                objectMapper.setPropertyNamingStrategy(propertyNamingStrategy);
            }

            jacksonConfiguration.getSerializationSettings().forEach(objectMapper::configure);
            jacksonConfiguration.getDeserializationSettings().forEach(objectMapper::configure);
            jacksonConfiguration.getMapperSettings().forEach(objectMapper::configure);
            jacksonConfiguration.getParserSettings().forEach(objectMapper::configure);
            jacksonConfiguration.getGeneratorSettings().forEach(objectMapper::configure);
        }
        return objectMapper;
    }
}
