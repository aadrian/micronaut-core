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
package io.micronaut.inject.ast;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.type.DefaultArgument;
import io.micronaut.core.util.ArgumentUtils;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ast.annotation.MutableAnnotationMetadataDelegate;
import io.micronaut.inject.ast.beans.BeanElementBuilder;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.micronaut.inject.writer.BeanDefinitionVisitor.PROXY_SUFFIX;

/**
 * Stores data about an element that references a class.
 *
 * @author James Kleeh
 * @author graemerocher
 * @since 1.0
 */
public interface ClassElement extends TypedElement {
    /**
     * Constant for an empty class element array.
     *
     * @since 3.1.0
     */
    ClassElement[] ZERO_CLASS_ELEMENTS = new ClassElement[0];

    /**
     * Returns the type annotations.
     * Added by:
     * - The declaration of the type variable {@link java.lang.annotation.ElementType#TYPE_PARAMETER}
     * - The use of the type {@link java.lang.annotation.ElementType#TYPE}
     * @return the type annotations
     * @since 4.0.0
     */
    @Experimental
    @NonNull
    default MutableAnnotationMetadataDelegate<AnnotationMetadata> getTypeAnnotationMetadata() {
        return (MutableAnnotationMetadataDelegate<AnnotationMetadata>) MutableAnnotationMetadataDelegate.EMPTY;
    }

    /**
     * Tests whether one type is assignable to another.
     *
     * @param type The type to check
     * @return {@code true} if and only if this type is assignable to the second
     */
    boolean isAssignable(String type);

    /**
     * In this case of calling {@link #getTypeArguments()} a returned {@link ClassElement} may represent a type variable
     * in which case this method will return {@code true}.
     *
     * @return Is this type a type variable.
     * @since 3.0.0
     */
    default boolean isTypeVariable() {
        return false;
    }

    /**
     *
     * @param kind The kind of error
     * @return Whether errors are present in the type element.
     * @since 4.7.18
     */
    @Experimental
    default boolean hasUnresolvedTypes(UnresolvedTypeKind... kind) {
        return false;
    }

    /**
     * @return Whether this is a generic placeholder.
     * @see GenericPlaceholderElement
     * @since 3.1.0
     */
    @Experimental
    default boolean isGenericPlaceholder() {
        return this instanceof GenericPlaceholderElement;
    }

    /**
     * @return Whether this is a wildcard.
     * @see WildcardElement
     */
    @Experimental
    default boolean isWildcard() {
        return this instanceof WildcardElement;
    }

    /**
     * Is raw type.
     * @return true if the type is raw
     * @since 4.0.0
     */
    @Experimental
    default boolean isRawType() {
        return false;
    }

    /**
     * Tests whether one type is assignable to another.
     *
     * @param type The type to check
     * @return {@code true} if and only if this type is assignable to the second
     * @since 2.3.0
     */
    default boolean isAssignable(ClassElement type) {
        return isAssignable(type.getName());
    }

    /**
     * Whether this element is an {@link Optional}.
     *
     * @return Is this element an optional
     * @since 2.3.0
     */
    default boolean isOptional() {
        return isAssignable(Optional.class) || isAssignable(OptionalLong.class) || isAssignable(OptionalDouble.class) || isAssignable(OptionalInt.class);
    }

    /**
     * Checks whether the bean type is a container type.
     *
     * @return Whether the type is a container type like {@link Iterable}.
     * @since 4.0.0
     */
    default boolean isContainerType() {
        return DefaultArgument.CONTAINER_TYPES.contains(getName());
    }

    /**
     * Gets optional value type.
     *
     * @return the value type
     * @since 4.0.0
     */
    default Optional<ClassElement> getOptionalValueType() {
        return Optional.empty();
    }

    /**
     * This method will return the name of the underlying type automatically unwrapping in the case of an optional
     * or wrapped representation of the type.
     *
     * @return Returns the canonical name of the type.
     * @since 2.3.0
     */
    default String getCanonicalName() {
        if (isOptional()) {
            return getFirstTypeArgument().map(ClassElement::getName).orElse(Object.class.getName());
        } else {
            return getName();
        }
    }

    /**
     * @return Whether this element is a record
     * @since 2.1.0
     */
    default boolean isRecord() {
        return false;
    }

    /**
     * Is this type an inner class.
     *
     * @return True if it is an inner class
     * @since 2.1.2
     */
    default boolean isInner() {
        return false;
    }

    /**
     * Whether this element is an enum.
     *
     * @return True if it is an enum
     */
    default boolean isEnum() {
        return this instanceof EnumElement;
    }

    /**
     * @return True if the class represents a proxy
     */
    default boolean isProxy() {
        return getSimpleName().endsWith(PROXY_SUFFIX);
    }

    /**
     * Find and return a single primary constructor. If more than constructor candidate exists, then return empty unless a
     * constructor is found that is annotated with either {@link io.micronaut.core.annotation.Creator} or {@link AnnotationUtil#INJECT}.
     *
     * @return The primary constructor if one is present
     */
    default @NonNull Optional<MethodElement> getPrimaryConstructor() {
        Optional<MethodElement> staticCreator = findStaticCreator();
        if (staticCreator.isPresent()) {
            return staticCreator;
        }
        if (isInner() && !isStatic()) {
            // only static inner classes can be constructed
            return Optional.empty();
        }
        List<ConstructorElement> constructors = getAccessibleConstructors();
        if (constructors.isEmpty()) {
            return Optional.empty();
        }
        if (constructors.size() == 1) {
            return Optional.of(constructors.get(0));
        }
        Optional<ConstructorElement> annotatedConstructor = constructors.stream()
                .filter(c -> c.hasStereotype(AnnotationUtil.INJECT) || c.hasStereotype(Creator.class))
                .findFirst();
        if (annotatedConstructor.isPresent()) {
            return annotatedConstructor.map(c -> c);
        }
        return constructors.stream()
                .filter(io.micronaut.inject.ast.Element::isPublic)
                .<MethodElement>map(c -> c)
                .findFirst();
    }

    /**
     * Find and return a single default constructor. A default constructor is one
     * without arguments that is accessible.
     *
     * @return The default constructor if one is present
     */
    default Optional<MethodElement> getDefaultConstructor() {
        Optional<MethodElement> staticCreator = findDefaultStaticCreator();
        if (staticCreator.isPresent()) {
            return staticCreator;
        }
        if (isInner() && !isStatic()) {
            // only static inner classes can be constructed
            return Optional.empty();
        }
        List<ConstructorElement> constructors = getAccessibleConstructors()
                .stream()
                .filter(ctor -> ctor.getParameters().length == 0).toList();
        if (constructors.isEmpty()) {
            return Optional.empty();
        }
        if (constructors.size() == 1) {
            return Optional.of(constructors.get(0));
        }
        return constructors.stream()
                .filter(Element::isPublic)
                .<MethodElement>map(c -> c)
                .findFirst();

    }

    /**
     * Find and return a single primary static creator. If more than creator candidate exists, then return empty unless a static
     * creator is found that is annotated with {@link io.micronaut.core.annotation.Creator}.
     *
     * @return The primary creator if one is present
     */
    default Optional<MethodElement> findStaticCreator() {
        List<MethodElement> staticCreators = getAccessibleStaticCreators();
        if (staticCreators.isEmpty()) {
            return Optional.empty();
        }
        if (staticCreators.size() == 1) {
            return Optional.of(staticCreators.get(0));
        }
        //Can be multiple static @Creator methods. Prefer one with args here. The no arg method (if present) will
        //be picked up by findDefaultStaticCreator
        List<MethodElement> withArgs = staticCreators.stream().filter(method -> method.getParameters().length > 0).toList();
        if (withArgs.size() == 1) {
            return Optional.of(withArgs.get(0));
        } else {
            staticCreators = withArgs;
        }
        return staticCreators.stream().filter(Element::isPublic).findFirst();
    }

    /**
     * Find and return a single default static creator. A default static creator is one
     * without arguments that is accessible.
     * *
     *
     * @return a static creator
     * @since 4.0.0
     */
    default Optional<MethodElement> findDefaultStaticCreator() {
        List<MethodElement> staticCreators = getAccessibleStaticCreators()
                .stream()
                .filter(c -> c.getParameters().length == 0).toList();
        if (staticCreators.isEmpty()) {
            return Optional.empty();
        }
        if (staticCreators.size() == 1) {
            return Optional.of(staticCreators.get(0));
        }
        return staticCreators.stream().filter(Element::isPublic).findFirst();
    }

    /**
     * Find accessible constructors.
     *
     * @return accessible constructors
     * @since 4.0.0
     */
    @NonNull
    default List<ConstructorElement> getAccessibleConstructors() {
        return getEnclosedElements(ElementQuery.CONSTRUCTORS)
                .stream()
                .filter(ctor -> !ctor.isPrivate())
                .toList();
    }

    /**
     * Get accessible static creators.
     * A static creator is a static method annotated with {@link Creator} that can be used to create the class.
     * For enums "valueOf" is picked as a static creator.
     *
     * @return static creators
     * @since 4.0.0
     */
    @NonNull
    default List<MethodElement> getAccessibleStaticCreators() {
        List<MethodElement> creators = getEnclosedElements(ElementQuery.ALL_METHODS
                .onlyDeclared()
                .onlyStatic()
                .onlyAccessible()
                .annotated(annotationMetadata -> annotationMetadata.hasStereotype(Creator.class))
        )
                .stream()
                .filter(method -> method.getReturnType().isAssignable(this))
                .toList();
        if (creators.isEmpty() && isEnum()) {
            return getEnclosedElements(ElementQuery.ALL_METHODS
                    .named("valueOf")
                    .onlyStatic()
                    .onlyAccessible()
            )
                    .stream()
                    .filter(method -> method.getReturnType().isAssignable(this))
                    .toList();
        }
        return creators;
    }

    /**
     * Returns the super type of this element or empty if the element has no super type.
     *
     * @return An optional of the super type
     */
    default Optional<ClassElement> getSuperType() {
        return Optional.empty();
    }

    /**
     * @return The interfaces implemented by this class element
     */
    default Collection<ClassElement> getInterfaces() {
        return Collections.emptyList();
    }

    @NonNull
    @Override
    default ClassElement getType() {
        return this;
    }

    /**
     * The simple name without the package name.
     *
     * @return The simple name
     */
    @Override
    default String getSimpleName() {
        return NameUtils.getSimpleName(getName());
    }

    /**
     * The package name.
     *
     * @return The package name
     */
    default String getPackageName() {
        return NameUtils.getPackageName(getName());
    }

    /**
     * The package name.
     *
     * @return The package name
     * @since 3.0.0
     */
    default PackageElement getPackage() {
        return PackageElement.of(getPackageName());
    }

    /**
     * Returns the bean properties (getters and setters) for this class element.
     *
     * @return The bean properties for this class element
     */
    @NonNull
    default List<PropertyElement> getBeanProperties() {
        return Collections.emptyList();
    }

    /**
     * Returns the synthetic bean properties. The properties where one of the methods (getter or setter)
     * is synthetic - not user defined but created by the compiler.
     *
     * @return The bean properties for this class element
     * @since 4.0.0
     */
    @NonNull
    default List<PropertyElement> getSyntheticBeanProperties() {
        return Collections.emptyList();
    }

    /**
     * Returns the bean properties (getters and setters) for this class element based on custom configuration.
     *
     * @param propertyElementQuery The configuration
     * @return The bean properties for this class element
     * @since 4.0.0
     */
    @NonNull
    default List<PropertyElement> getBeanProperties(@NonNull PropertyElementQuery propertyElementQuery) {
        return Collections.emptyList();
    }

    /**
     * Return all the fields of this class element.
     *
     * @return The fields
     */
    @NonNull
    default List<FieldElement> getFields() {
        return getEnclosedElements(ElementQuery.ALL_FIELDS);
    }

    /**
     * Find an instance/static field with a name in this class, super class or an interface.
     *
     * @param name The field name
     * @return The field
     * @since 4.0.0
     */
    @Experimental
    @NonNull
    default Optional<FieldElement> findField(String name) {
        return getEnclosedElement(ElementQuery.ALL_FIELDS.named(name));
    }

    /**
     * Find an instance/static method with a name in this class, super class or an interface.
     *
     * @return The methods
     * @since 4.0.0
     */
    @NonNull
    default List<MethodElement> getMethods() {
        return getEnclosedElements(ElementQuery.ALL_METHODS);
    }

    /**
     * Find a method with a name.
     *
     * @param name The method name
     * @return The method
     * @since 4.0.0
     */
    @NonNull
    @Experimental
    default Optional<MethodElement> findMethod(String name) {
        return getEnclosedElement(ElementQuery.ALL_METHODS.named(name));
    }

    /**
     * Return the elements that match the given query.
     *
     * @param query The query to use.
     * @param <T>   The element type
     * @return The fields
     * @since 2.3.0
     */
    @NonNull
    default <T extends Element> List<T> getEnclosedElements(@NonNull ElementQuery<T> query) {
        return Collections.emptyList();
    }

    /**
     * Returns the enclosing type if {@link #isInner()} return {@code true}.
     *
     * @return The enclosing type if any
     * @since 3.0.0
     */
    default Optional<ClassElement> getEnclosingType() {
        return Optional.empty();
    }

    /**
     * Return the first enclosed element matching the given query.
     *
     * @param query The query to use.
     * @param <T>   The element type
     * @return The fields
     * @since 2.3.0
     */
    default <T extends Element> Optional<T> getEnclosedElement(@NonNull ElementQuery<T> query) {
        List<T> enclosedElements = getEnclosedElements(query);
        if (!enclosedElements.isEmpty()) {
            return Optional.of(enclosedElements.iterator().next());
        }
        return Optional.empty();
    }

    /**
     * @return Whether the class element is an interface
     */
    default boolean isInterface() {
        return false;
    }

    /**
     * @return Whether the type is iterable (either an array or an {@link Iterable})
     */
    default boolean isIterable() {
        return isArray() || isAssignable(Iterable.class);
    }

    /**
     * The list of type arguments bound to this type, or an empty list if there are no type arguments or this is a raw
     * type.
     * <p>
     * Note that for compatibility reasons, this method is inconsistent with {@link #getTypeArguments()}. In particular,
     * this method reflects the <i>declaration</i> type: If there is a {@code class Test<T> { T field; }}, this method
     * will return {@code T} as the field type, even if the field type was obtained through a {@code Test<String>}.
     *
     * @return The list of type arguments, in the same order as {@link #getDeclaredGenericPlaceholders()}. Must be empty or
     * of the same length as {@link #getDeclaredGenericPlaceholders()}.
     * @since 3.1.0
     */
    @NonNull
    @Experimental
    default List<? extends ClassElement> getBoundGenericTypes() {
        return new ArrayList<>(getTypeArguments().values());
    }

    /**
     * The type arguments declared on the raw class. Independent of the actual
     * {@link #getBoundGenericTypes() bound type arguments}.
     *
     * <p>This method will resolve the generic placeholders defined of the declaring class, if any.
     * </p>
     *
     * <p>For example {@code List<String>} will result a single placeholder called {@code E} of type {@link Object}.</p>
     *
     * @return The type arguments declared on this class.
     * @since 3.1.0
     */
    @NonNull
    @Experimental
    default List<? extends GenericPlaceholderElement> getDeclaredGenericPlaceholders() {
        return Collections.emptyList();
    }

    /**
     * Get a {@link ClassElement} instance corresponding to this type, but without any type arguments bound. For
     * {@code List<String>}, this returns {@code List}.
     *
     * @return The raw class of this potentially parameterized type.
     * @since 3.1.0
     */
    @NonNull
    @Experimental
    default ClassElement getRawClassElement() {
        return withTypeArguments(Collections.emptyList());
    }

    /**
     * Get a {@link ClassElement} instance corresponding to this type, but with the given type arguments. This is the best
     * effort – implementations may only support {@link ClassElement}s that come from the same visitor context, and
     * other {@link ClassElement}s only to a limited degree.
     *
     * @param typeArguments The new type arguments.
     * @return A {@link ClassElement} of the same raw class with the new type arguments.
     * @throws UnsupportedOperationException If any of the given type arguments are unsupported.
     * @deprecated replaced with {@link #withTypeArguments(Collection)} for consistent API.
     */
    @NonNull
    @Experimental
    @Deprecated(since = "4", forRemoval = true)
    default ClassElement withBoundGenericTypes(@NonNull List<? extends ClassElement> typeArguments) {
        return withTypeArguments((Collection<ClassElement>) typeArguments);
    }

    /**
     * Perform a fold operation on the type arguments (type arguments, wildcard bounds, resolved via {@link #getBoundGenericTypes()}), and then on this
     * type. For {@code List<? extends String>}, this returns {@code f(List<f(? extends f(String))>)}. The bounds of
     * type variables are not folded.
     *
     * <p>
     * {@code null} has special meaning here. Returning {@code null} from a fold operation will try to make the
     * surrounding type a raw type. For example, for {@code Map<String, Object>}, returning {@code null} for the fold
     * on {@code Object} will lead to the parameterized {@code Map<String, null>} type being replaced by {@code Map}.
     * </p>
     *
     * <p>This also means that this method may return {@code null} if the top-level fold operation returned {@code null}.</p>
     *
     * @param fold The fold operation to apply recursively to all component types.
     * @return The folded type.
     * @since 3.1.0
     */
    @Experimental
    @Nullable
    default ClassElement foldBoundGenericTypes(@NonNull Function<ClassElement, ClassElement> fold) {
        List<ClassElement> typeArgs = getBoundGenericTypes().stream().map(arg -> arg.foldBoundGenericTypes(fold)).toList();
        if (typeArgs.contains(null)) {
            typeArgs = Collections.emptyList();
        }
        return fold.apply(withTypeArguments(typeArgs));
    }

    /**
     * Get the type arguments for the given type name.
     *
     * @param type The type to retrieve type arguments for
     * @return The type arguments for this class element
     * @since 1.1.1
     */
    @NonNull
    default Map<String, ClassElement> getTypeArguments(@NonNull String type) {
        ArgumentUtils.requireNonNull("type", type);
        return getAllTypeArguments().getOrDefault(type, Collections.emptyMap());
    }

    /**
     * Get the type arguments for the given type name.
     *
     * @param type The type to retrieve type arguments for
     * @return The type arguments for this class element
     */
    @NonNull
    default Map<String, ClassElement> getTypeArguments(@NonNull Class<?> type) {
        ArgumentUtils.requireNonNull("type", type);
        return getTypeArguments(type.getName());
    }

    /**
     * @return The type arguments for this class element
     */
    @NonNull
    default Map<String, ClassElement> getTypeArguments() {
        return Collections.emptyMap();
    }

    /**
     * Builds a map of all the type parameters for a class, its super classes and interfaces.
     * The resulting map contains the name of the class to the map of the resolved generic types.
     *
     * @return The type arguments for this class element
     */
    @NonNull
    default Map<String, Map<String, ClassElement>> getAllTypeArguments() {
        Map<String, Map<String, ClassElement>> result = new LinkedHashMap<>();
        Stream.concat(
                getInterfaces().stream(),
                getSuperType().stream()
        ).map(ClassElement::getAllTypeArguments).forEach(result::putAll);
        result.put(getName(), getTypeArguments());
        return result;
    }

    /**
     * @return The first type argument
     */
    default Optional<ClassElement> getFirstTypeArgument() {
        return getTypeArguments().values().stream().findFirst();
    }

    /**
     * Tests whether one type is assignable to another.
     *
     * @param type The type to check
     * @return {@code true} if and only if the type is assignable to the second
     */
    default boolean isAssignable(Class<?> type) {
        return isAssignable(type.getName());
    }

    /**
     * Convert the class element to an element for the same type, but representing an array.
     * Do not mutate the existing instance. Create a new instance instead.
     *
     * @return A new class element
     */
    @NonNull
    ClassElement toArray();

    /**
     * Dereference a class element denoting an array type by converting it to its element type.
     * Do not mutate the existing instance. Create a new instance instead.
     *
     * @return A new class element
     * @throws IllegalStateException if this class element doesn't denote an array type
     */
    @NonNull
    ClassElement fromArray();

    /**
     * This method adds an associated bean using this class element as the originating element.
     *
     * <p>Note that this method can only be called on classes being directly compiled by Micronaut. If the ClassElement is
     * loaded from pre-compiled code an {@link UnsupportedOperationException} will be thrown.</p>
     *
     * @param type The type of the bean
     * @return A bean builder
     */
    @NonNull
    default BeanElementBuilder addAssociatedBean(@NonNull ClassElement type) {
        throw new UnsupportedOperationException("Element of type [" + getClass() + "] does not support adding associated beans at compilation time");
    }

    @Override
    default ClassElement withAnnotationMetadata(AnnotationMetadata annotationMetadata) {
        return (ClassElement) TypedElement.super.withAnnotationMetadata(annotationMetadata);
    }

    /**
     * Copies this element and overrides its type arguments.
     *
     * @param typeArguments The type arguments
     * @return A new element
     * @since 4.0.0
     */
    @NonNull
    default ClassElement withTypeArguments(Map<String, ClassElement> typeArguments) {
        throw new UnsupportedOperationException("Element of type [" + getClass() + "] does not support copy constructor");
    }

    /**
     * Copies this element and overrides its type arguments.
     * Variation of {@link #withTypeArguments(Map)} that doesn't require type argument names.
     *
     * @param typeArguments The type arguments
     * @return A new element
     * @since 4.0.0
     */
    @NonNull
    default ClassElement withTypeArguments(@NonNull Collection<ClassElement> typeArguments) {
        if (typeArguments.isEmpty()) {
            // Allow to eliminate all arguments
            return withTypeArguments(Collections.emptyMap());
        }
        Set<String> genericNames = getTypeArguments().keySet();
        if (genericNames.size() != typeArguments.size()) {
            throw new IllegalStateException("Expected to have: " + genericNames.size() + " type arguments! Got: " + typeArguments.size());
        }
        Map<String, ClassElement> boundByName = CollectionUtils.newLinkedHashMap(typeArguments.size());
        Iterator<String> keys = genericNames.iterator();
        Iterator<? extends ClassElement> args = typeArguments.iterator();
        while (keys.hasNext() && args.hasNext()) {
            boundByName.put(keys.next(), args.next());
        }
        return withTypeArguments(boundByName);
    }

    /**
     * Create a class element for the given simple type.
     *
     * @param type The type
     * @return The class element
     */
    @NonNull
    static ClassElement of(@NonNull Class<?> type) {
        return new ReflectClassElement(
                Objects.requireNonNull(type, "Type cannot be null")
        );
    }

    /**
     * Create a class element for the given complex type.
     *
     * @param type The type
     * @return The class element
     */
    @Experimental
    @NonNull
    static ClassElement of(@NonNull Type type) {
        Objects.requireNonNull(type, "Type cannot be null");
        if (type instanceof Class<?> aClass) {
            return new ReflectClassElement(aClass);
        } else if (type instanceof TypeVariable<?> typeVariable) {
            return new ReflectGenericPlaceholderElement(typeVariable, 0);
        } else if (type instanceof WildcardType wildcardType) {
            return new ReflectWildcardElement(wildcardType);
        } else if (type instanceof ParameterizedType pType) {
            if (pType.getOwnerType() != null) {
                throw new UnsupportedOperationException("Owner types are not supported");
            }
            return new ReflectClassElement(ReflectTypeElement.getErasure(type)) {
                @NonNull
                @Override
                public List<? extends ClassElement> getBoundGenericTypes() {
                    return Arrays.stream(pType.getActualTypeArguments())
                            .map(ClassElement::of)
                            .toList();
                }
            };
        } else if (type instanceof GenericArrayType genericArrayType) {
            return of(genericArrayType.getGenericComponentType()).toArray();
        } else {
            throw new IllegalArgumentException("Bad type: " + type.getClass().getName());
        }
    }

    /**
     * Create a class element for the given simple type.
     *
     * @param type               The type
     * @param annotationMetadata The annotation metadata
     * @param typeArguments      The type arguments
     * @return The class element
     * @since 2.4.0
     */
    @NonNull
    static ClassElement of(@NonNull Class<?> type,
                           @NonNull AnnotationMetadata annotationMetadata,
                           @NonNull Map<String, ClassElement> typeArguments) {
        Objects.requireNonNull(annotationMetadata, "Annotation metadata cannot be null");
        Objects.requireNonNull(typeArguments, "Type arguments cannot be null");
        return new ReflectClassElement(
                Objects.requireNonNull(type, "Type cannot be null")
        ) {
            @Override
            public AnnotationMetadata getAnnotationMetadata() {
                return annotationMetadata;
            }

            @Override
            public Map<String, ClassElement> getTypeArguments() {
                return Collections.unmodifiableMap(typeArguments);
            }

            @NonNull
            @Override
            public List<? extends ClassElement> getBoundGenericTypes() {
                return getDeclaredGenericPlaceholders().stream()
                        .map(tv -> typeArguments.get(tv.getVariableName()))
                        .toList();
            }
        };
    }

    /**
     * Create a class element for the given simple type.
     *
     * @param typeName The type
     * @return The class element
     */
    @Internal
    @NonNull
    static ClassElement of(@NonNull String typeName) {
        return new SimpleClassElement(typeName);
    }

    /**
     * Create a class element for the given simple type.
     *
     * @param typeName           The type
     * @param isInterface        Is the type an interface
     * @param annotationMetadata The annotation metadata
     * @return The class element
     */
    @Internal
    @NonNull
    static ClassElement of(@NonNull String typeName, boolean isInterface, @Nullable AnnotationMetadata annotationMetadata) {
        return new SimpleClassElement(typeName, isInterface, annotationMetadata);
    }

    /**
     * Create a class element for the given simple type.
     *
     * @param typeName           The type
     * @param isInterface        Is the type an interface
     * @param annotationMetadata The annotation metadata
     * @param typeArguments      The type arguments
     * @return The class element
     */
    @Internal
    @NonNull
    static ClassElement of(@NonNull String typeName, boolean isInterface, @Nullable AnnotationMetadata annotationMetadata, Map<String, ClassElement> typeArguments) {
        return new SimpleClassElement(typeName, isInterface, annotationMetadata);
    }
}
