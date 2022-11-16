package cz.atlascon.graphql.schemas.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import cz.atlascon.graphql.common.Common;
import cz.atlascon.graphql.ng.AllowNullElements;
import cz.atlascon.graphql.schemas.CommonSchemaGenerator;
import cz.atlascon.graphql.schemas.types.GraphQLTypeFactory;
import graphql.schema.*;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class PojoInputSchemaGenerator {

    private final GraphQLTypeFactory graphQLTypeFactory;
    private final ConcurrentMap<Type, GraphQLInputType> inputMap = Maps.newConcurrentMap();
    private final ConcurrentMap<Map.Entry<Type, Boolean>, GraphQLInputType> listInputs = Maps.newConcurrentMap();
    private final ConcurrentMap<Type, GraphQLTypeReference> references = Maps.newConcurrentMap();
    private final CommonSchemaGenerator commonSchemaGenerator;

    public PojoInputSchemaGenerator(final GraphQLTypeFactory graphQLTypeFactory,
                                    final CommonSchemaGenerator commonSchemaGenerator) {
        Preconditions.checkNotNull(commonSchemaGenerator);
        this.commonSchemaGenerator = commonSchemaGenerator;
        Preconditions.checkNotNull(graphQLTypeFactory);
        this.graphQLTypeFactory = graphQLTypeFactory;
    }

    public GraphQLInputType createInputType(final Type type) {
        return createInputType(List.of(), type);
    }

    public GraphQLInputType createInputType(final List<Annotation> argAnnotations,
                                            final Type type) {
        GraphQLInputType created = doCreateInputType(argAnnotations, type);
        if (created == null) {
            return null;
        }
        // TODO generalize to annotation filters
        final Set<Class> aClasses = argAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toSet());
        if (aClasses.contains(Nonnull.class)) {
            return CommonSchemaGenerator.wrapNonNull(created);
        }
        return created;
    }

    private GraphQLInputType doCreateInputType(List<Annotation> argAnnotations, Type type) {
        if (type == void.class || type == Void.class) {
            return null;
        }
        try {
            // is it simple common type?
            final Optional<GraphQLType> commonType = graphQLTypeFactory.createType(type);
            if (commonType.isPresent()) {
                return (GraphQLInputType) commonType.get();
            }
            // is type a class?
            if (type instanceof Class) {
                if (inputMap.containsKey(type)) {
                    return inputMap.get(type);
                }

                final Class clz = (Class) type;
                if (references.containsKey(clz)) {
                    return references.get(clz);
                }

                final String name = Common.getInputName(clz);
                references.putIfAbsent(clz, GraphQLTypeReference.typeRef(name));

                final GraphQLInputObjectType oit = buildObjectSchema(clz);
                inputMap.putIfAbsent(type, oit);
                return oit;
            } else if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType() instanceof Class && Map.class.isAssignableFrom((Class) pt.getRawType())) {
                    if (inputMap.containsKey(type)) {
                        return inputMap.get(type);
                    }
                    final GraphQLInputType map = commonSchemaGenerator.createMapInputSchema(
                            createInputType(pt.getActualTypeArguments()[0]),
                            createInputType(pt.getActualTypeArguments()[1])
                    );
                    inputMap.putIfAbsent(type, map);
                    return map;
                } else if (pt.getRawType() instanceof Class && Collection.class
                        .isAssignableFrom((Class) pt.getRawType())) {
                    final boolean allowNullElements = argAnnotations.stream().anyMatch(a -> a.annotationType() == AllowNullElements.class);
                    if (listInputs.containsKey(Map.entry(type, allowNullElements))) {
                        return listInputs.get(Map.entry(type, allowNullElements));
                    }
                    final GraphQLInputType memberType = createInputType(pt.getActualTypeArguments()[0]);
                    final GraphQLInputType memberWrapped;
                    if (argAnnotations.stream().anyMatch(a -> a.annotationType() == AllowNullElements.class)) {
                        memberWrapped = memberType;
                    } else {
                        memberWrapped = CommonSchemaGenerator.wrapNonNull(memberType);
                    }
                    final GraphQLInputType list = GraphQLList.list(memberWrapped);
                    listInputs.putIfAbsent(Map.entry(type, allowNullElements), list);
                    return list;
                } else {
                    if (pt.getRawType() instanceof Class && ((Class) pt.getRawType()).isInterface()) {
                        throw new IllegalArgumentException("Input type can not be interface - " + type);
                    } else {
                        throw new IllegalArgumentException("Unknown parameterized type" + type);
                    }
                }
            }
            throw new IllegalArgumentException("Unknown type " + type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GraphQLInputObjectType buildObjectSchema(final Class clz) {
        Preconditions
                .checkArgument(PojoDeserializer.isDeserializable(clz), "Input class " + clz + " is not deserializable");
        final String typeName = Common.getInputName(clz);
        GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject()
                .name(typeName);
        final Optional<Executable> exOpt = PojoDeserializer.findConstructingMethod(clz);
        final Executable ex = exOpt.orElseThrow(() -> new RuntimeException("Unable to find suitable constructor/factory method (with @JsonCreator annotations) for class " + clz));
        final Type[] params = ex.getGenericParameterTypes();
        final Annotation[][] annotations = ex.getParameterAnnotations();
        for (int i = 0; i < ex.getParameterCount(); i++) {
            final List<Annotation> argAnnotations = Arrays.asList(annotations[i]);
            final GraphQLInputType fieldType = createInputType(argAnnotations, params[i]);
            final GraphQLInputType resulting;
            if (argAnnotations.stream().anyMatch(a -> a.annotationType() == Nonnull.class)) {
                resulting = CommonSchemaGenerator.wrapNonNull(fieldType);
            } else {
                resulting = fieldType;
            }
            final Annotation an = argAnnotations.stream().filter(a -> a.annotationType() == JsonProperty.class).findAny().orElseThrow();
            final String argName = ((JsonProperty) an).value();
            final GraphQLInputObjectField inField = GraphQLInputObjectField.newInputObjectField()
                    .name(argName)
                    .type(resulting)
                    .build();
            builder.field(inField);
        }
        return builder.build();
    }


}
