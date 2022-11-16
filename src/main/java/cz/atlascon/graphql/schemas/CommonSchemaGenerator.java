package cz.atlascon.graphql.schemas;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cz.atlascon.graphql.ng.GraphQLDto;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLIgnore;
import graphql.schema.*;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class CommonSchemaGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonSchemaGenerator.class);
    public static final String UNNAMED_ARGUMENT_NAME = "arg";
    public static final String KEY = "key";
    public static final String VAL = "val";
    private final Map<String, GraphQLInputType> inputMaps = Maps.newConcurrentMap();
    private final Map<String, GraphQLOutputType> outputMaps = Maps.newConcurrentMap();

    public static String getTypeName(final GraphQLType type) {
        Preconditions.checkNotNull(type);
        if (!(type instanceof GraphQLNamedType)) {
            StringBuilder name = new StringBuilder();
            GraphQLType current = type;

            while (!(current instanceof GraphQLNamedType)) {
                Preconditions.checkArgument(current instanceof GraphQLModifiedType,
                        "Expecting GraphQLModifiedType, got " + current.getClass());

                if (current instanceof GraphQLList) {
                    name.append("list_");
                }

                current = ((GraphQLModifiedType) current).getWrappedType();
            }
            return name.append(((GraphQLNamedType) current).getName()).toString();
        } else {
            return ((GraphQLNamedType) type).getName();
        }
    }

    public GraphQLInputType createMapInputSchema(final GraphQLInputType key, final GraphQLInputType val) {
        final String entryName = CommonSchemaGenerator.createMapTypeName(key, val);
        return inputMaps.computeIfAbsent(entryName, n -> {
            GraphQLInputObjectField keyField = createInputField(KEY, key);
            GraphQLInputObjectField valField = createInputField(VAL, val);
            final GraphQLInputObjectType entryType = GraphQLInputObjectType.newInputObject()
                    .name(entryName)
                    .field(keyField)
                    .field(valField)
                    .build();
            return GraphQLList.list(entryType);
        });
    }

    private static GraphQLInputObjectField createInputField(final String fieldName,
                                                            final GraphQLInputType inputType) {
        return GraphQLInputObjectField.newInputObjectField()
                .name(fieldName)
                .type(inputType)
                .build();
    }

    private static String createMapTypeName(final GraphQLInputType key, final GraphQLInputType val) {
        return "input_map_entry_"
                + CommonSchemaGenerator.getTypeName(key)
                + "_"
                + CommonSchemaGenerator.getTypeName(val);
    }

    public static String getFieldName(final Method m) {
        final GraphQLField field = m.getAnnotation(GraphQLField.class);
        if (field != null && field.value() != null && !field.value().isBlank()) {
            return field.value().strip();
        } else {
            final String methodName = m.getName();
            Preconditions.checkArgument(methodName.startsWith("is") || methodName.startsWith("get"),
                    "expecting method isXXX or getXXX, got " + methodName);
            return Introspector.decapitalize(methodName.substring(methodName.startsWith("is") ? 2 : 3));
        }
    }

    public static <E extends GraphQLOutputType> GraphQLOutputType wrapNonNull(final E type) {
        if (!(type instanceof GraphQLNonNull)) {
            return GraphQLNonNull.nonNull(type);
        } else {
            return type;
        }
    }

    public static <E extends GraphQLInputType> GraphQLInputType wrapNonNull(final E type) {
        if (!(type instanceof GraphQLNonNull)) {
            return GraphQLNonNull.nonNull(type);
        } else {
            return type;
        }
    }

    public static GraphQLInputType unwrap(final GraphQLInputType type) {
        GraphQLType actual = type;
        while (actual instanceof GraphQLModifiedType) {
            actual = ((GraphQLModifiedType) actual).getWrappedType();
        }
        Preconditions.checkArgument(actual instanceof GraphQLInputType);
        return (GraphQLInputType) actual;
    }

    public GraphQLOutputType createMapOutputSchema(final GraphQLOutputType key,
                                                   final GraphQLOutputType val) {
        String entryName =
                "map_entry_" + CommonSchemaGenerator.getTypeName(key) + "_" + CommonSchemaGenerator.getTypeName(val);

        return outputMaps.computeIfAbsent(entryName, n -> {

            GraphQLFieldDefinition keyField = createMapField(CommonSchemaGenerator.KEY, key);
            GraphQLFieldDefinition valField = createMapField(CommonSchemaGenerator.VAL, val);

            final GraphQLObjectType entryType = GraphQLObjectType
                    .newObject()
                    .field(keyField)
                    .field(valField)
                    .name(entryName)
                    .build();
            return GraphQLNonNull.nonNull(GraphQLList.list(wrapNonNull(entryType)));
        });
    }

    private static GraphQLFieldDefinition createMapField(String fieldName, GraphQLOutputType type) {
        Preconditions.checkNotNull(type, "Got null type");
        Preconditions.checkNotNull(fieldName, "Got null fieldName");
        return GraphQLFieldDefinition.newFieldDefinition()
                .type(CommonSchemaGenerator.wrapNonNull(type))
                .dataFetcher(new DataFetcher<Object>() {
                    @Override
                    public Object get(final DataFetchingEnvironment environment) throws Exception {
                        final String fieldName = environment.getField().getName();
                        if (fieldName.equals(CommonSchemaGenerator.KEY)) {
                            return ((Map.Entry) environment.getSource()).getKey();
                        } else if (fieldName.equals(CommonSchemaGenerator.VAL)) {
                            return ((Map.Entry) environment.getSource()).getValue();
                        } else {
                            throw new IllegalArgumentException("Unknown field for map entry - " + fieldName);
                        }
                    }
                })
                .name(fieldName)
                .build();
    }

    public static List<Method> getGraphQLMethods(final Class clz) {
        if (!clz.isAnnotationPresent(GraphQLDto.class)) {
            // just annotated methods
            final List<Method> withAnnotation = MethodUtils
                    .getMethodsListWithAnnotation(clz, GraphQLField.class, true, false);
            return withAnnotation;
        } else {
            // is graphql DTO class -> all methods except for ignored
            try {
                final List<Method> all = Lists.newArrayList();
                for (Method method : getDtoMethods(clz)) {
                    // ignore ignored methods
                    if (method.getAnnotation(GraphQLIgnore.class) == null) {
                        all.add(method);
                    }
                }
                return all;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static List<Method> getDtoMethods(final Class clz) {
        final List<Method> res = Lists.newArrayList();
        // interface
        if (clz.isInterface()) {
            for (Method m : clz.getMethods()) {
                if (isDtoMethod(m)) {
                    res.add(m);
                }
            }
        } else {
            // get methods from super classes
            Class current = clz;
            while (current != null && current != Object.class) {
                for (Method m : current.getDeclaredMethods()) {
                    if (isDtoMethod(m)) {
                        res.add(m);
                    }
                }
                current = current.getSuperclass();
            }
            // get default methods from interfaces
            final Set<Class<?>> interfaces = getAllImplementedTypesRecursively(clz);
            for (Class<?> c : interfaces) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.isDefault() && isDtoMethod(m)) {
                        res.add(m);
                    }
                }
            }
        }
        return res;
    }

    /*
     *  https://stackoverflow.com/questions/22031207/find-all-classes-and-interfaces-a-class-extends-or-implements-recursively
     */
    private static Set<Class<?>> getAllImplementedTypesRecursively(Class<?> clazz) {
        List<Class<?>> res = new ArrayList<>();
        do {
            // First, add all the interfaces implemented by this class
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length > 0) {
                res.addAll(Arrays.asList(interfaces));
                for (Class<?> interfaze : interfaces) {
                    res.addAll(getAllImplementedTypesRecursively(interfaze));
                }
            }
            // Add the super class
            Class<?> superClass = clazz.getSuperclass();
            // Interfaces does not have java,lang.Object as superclass, they have null, so break the cycle and return
            if (superClass == null) {
                break;
            }
            // Now inspect the superclass
            clazz = superClass;
        } while (!"java.lang.Object".equals(clazz.getCanonicalName()));

        return new HashSet<Class<?>>(res);
    }

    public static boolean isDtoMethod(final Method m) {
        if (!Modifier.isPublic(m.getModifiers())) {
            return false;
        }
//        if (!Modifier.isPublic(m.getModifiers()) || Modifier.isAbstract(m.getModifiers())) {
//            return false;
//        }
        final String name = m.getName();
        final Class<?> returnType = m.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            return false;
        }

        if (!name.startsWith("is") && !name.startsWith("get")) {
            return false;
        }
        if (name.startsWith("is")) {
            if (returnType == boolean.class
                    || returnType == Boolean.class) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }
}
