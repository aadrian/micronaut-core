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
package io.micronaut.aop.chain;

import io.micronaut.aop.Adapter;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.ExecutionHandle;
import io.micronaut.inject.qualifiers.Qualifiers;

import static io.micronaut.aop.Adapter.InternalAttributes.ADAPTED_ARGUMENT_TYPES;
import static io.micronaut.aop.Adapter.InternalAttributes.ADAPTED_BEAN;
import static io.micronaut.aop.Adapter.InternalAttributes.ADAPTED_METHOD;
import static io.micronaut.aop.Adapter.InternalAttributes.ADAPTED_QUALIFIER;

/**
 * Internal class that implements introduction advice for the {@link io.micronaut.aop.Adapter} annotation.
 *
 * @author graemerocher
 * @since 1.0
 */
@Internal
final class AdapterIntroduction implements MethodInterceptor<Object, Object> {

    private final ExecutionHandle<?, ?> executionHandle;

    /**
     * Default constructor.
     *
     * @param beanContext The bean context
     * @param method The target method
     */
    AdapterIntroduction(BeanContext beanContext, ExecutableMethod<?, ?> method) {
        AnnotationValue<Adapter> adapterAnnotationValue = method.getAnnotation(Adapter.class);
        if (adapterAnnotationValue == null) {
            throw new IllegalStateException("Adapter method must have @Adapter annotation");
        }
        Class<?> beanType = adapterAnnotationValue.classValue(ADAPTED_BEAN).orElse(null);
        if (beanType == null) {
            throw new IllegalStateException("No bean type to adapt found in Adapter configuration for method: " + method);
        }

        String beanMethod = adapterAnnotationValue.stringValue(ADAPTED_METHOD).orElse(null);
        if (StringUtils.isEmpty(beanMethod)) {
            throw new IllegalStateException("No bean method to adapt found in Adapter configuration for method: " + method);
        }

        String beanQualifier = adapterAnnotationValue.stringValue(ADAPTED_QUALIFIER).orElse(null);
        Class<?>[] argumentTypes = adapterAnnotationValue.classValues(ADAPTED_ARGUMENT_TYPES);
        Class<?>[] methodArgumentTypes = method.getArgumentTypes();
        Class<?>[] arguments = argumentTypes.length == methodArgumentTypes.length ? argumentTypes : methodArgumentTypes;
        if (StringUtils.isNotEmpty(beanQualifier)) {
            this.executionHandle = beanContext.findExecutionHandle(
                beanType,
                Qualifiers.byName(beanQualifier),
                beanMethod,
                arguments
            ).orElseThrow(() -> new IllegalStateException("Cannot adapt method [" + method + "]. Target method [" + beanMethod + "] not found on bean " + beanType));

        } else {
            this.executionHandle = beanContext.findExecutionHandle(
                beanType,
                beanMethod,
                arguments
            ).orElseThrow(() -> new IllegalStateException("Cannot adapt method [" + method + "]. Target method [" + beanMethod + "] not found on bean " + beanType));
        }
    }

    @Nullable
    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        return executionHandle.invoke(context.getParameterValues());
    }
}
