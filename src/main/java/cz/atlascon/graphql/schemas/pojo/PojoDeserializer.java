package cz.atlascon.graphql.schemas.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PojoDeserializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PojoDeserializer.class);

    public static Object convert(final Object argument,
                                 final Type targetType) {
        if (argument == null) {
            return null;
        }
        if (targetType == Void.class || targetType == void.class) {
            return null;
        }
        if (Map.class.isAssignableFrom(argument.getClass())) {
            return deserializeObject((Map) argument, targetType);
        } else if (Collection.class.isAssignableFrom(argument.getClass())) {
            Preconditions.checkArgument(targetType instanceof ParameterizedType);
            ParameterizedType pt = (ParameterizedType) targetType;
            final Type collectionType = pt.getActualTypeArguments()[0];
            return ((Collection) argument).stream().map(member -> convert(member, collectionType))
                    .collect(Collectors.toList());
        } else {
            return argument;
        }
    }


    private static Object deserializeObject(final Map<String, Object> arguments, final Type targetType) {
        try {
            final Class clz =
                    targetType instanceof ParameterizedType ? ((Class) ((ParameterizedType) targetType).getRawType())
                            : (Class) targetType;
            final Optional<Executable> exOpt = findConstructingMethod(clz);
            Preconditions.checkArgument(exOpt.isPresent(),
                    "Class " + clz + " is not deserializable - missing @JsonCreator constructor or factory method");
            return useCreator(clz, exOpt.get(), arguments);
            // ?????????????????????????????????? important
            // =======================================
            // still nothing? maybe just pass value through?
//            Preconditions.checkArgument(arguments.size() == 1);
//            return Iterables.getOnlyElement(arguments.values());
            // =======================================
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception creating " + targetType, e);
        }
    }

    private static Object useCreator(final Class clz,
                                     final Executable ex,
                                     final Map<String, Object> arguments) {
        final Parameter[] params = ex.getParameters();
        final Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            final Parameter param = params[i];
            final JsonProperty property = param.getAnnotation(JsonProperty.class);
            if (property == null) {
                throw new IllegalArgumentException("Missing @JsonProperty annotation on param " + param + " in " + clz);
            }
            final String propName = property.value();
            final Type argType = getArgType(param);
            final Object arg = convert(arguments.get(propName), argType);
            if (param.getAnnotation(Nonnull.class) != null && arg == null) {
                throw new IllegalArgumentException("Null parameter for non null property " + propName + " in " + clz);
            }
            args[i] = arg;
        }
        try {
            if (ex instanceof Constructor) {
                return ((Constructor) ex).newInstance(args);
            } else if (ex instanceof Method) {
                return ((Method) ex).invoke(null, args);
            } else {
                throw new RuntimeException("Executable must be Method/Constructor, was " + ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to deserialize " + clz + " from arguments " + arguments, e);
        }
    }

    private static Type getArgType(final Parameter param) {
        if (param.getParameterizedType() != null) {
            return param.getParameterizedType();
        } else {
            return param.getType();
        }
    }

    private static Executable findSuitableFactoryMethod(final Class<?> clz) {
        for (Method m : clz.getMethods()) {
            // static factory method
            // with same return type
            // and suitable signature
            if (Modifier.isStatic(m.getModifiers()) && (m.getReturnType() == clz) && isSuitable(m)) {
                return m;
            }
        }
        return null;
    }

    private static Executable findSuitableConstructor(final Class<?> clz) {
        for (Constructor c : clz.getConstructors()) {
            if (isSuitable(c)) {
                return c;
            }
        }
        return null;
    }

    public static Optional<Executable> findConstructingMethod(final Class<?> clz) {
        final Executable c = findSuitableConstructor(clz);
        if (c == null) {
            return Optional.ofNullable(findSuitableFactoryMethod(clz));
        } else {
            return Optional.of(c);
        }
    }

    private static boolean isSuitable(final Executable ex) {
        if (ex.isAnnotationPresent(JsonCreator.class)) {
            for (Parameter p : ex.getParameters()) {
                final JsonProperty an = p.getAnnotation(JsonProperty.class);
                if (an == null || an.value().isBlank()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isDeserializable(final Class clz) {
        return findConstructingMethod(clz) != null;
    }
}
