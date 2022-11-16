package cz.atlascon.graphql.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import cz.atlascon.graphql.ng.GraphQLDto;
import cz.atlascon.graphql.ng.GraphQLReturnTypes;
import cz.atlascon.graphql.ng.GraphQLTypeName;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by tomas on 13.6.17.
 */
public class Common {

    public static final String INPUT_SUFFIX = "_input";

    private Common() {

    }

    public static String getInputName(final Class clz) {
        Preconditions.checkArgument(!clz.isInterface(), "No interface allowed in input - " + clz.getName());
        if (clz.isAnnotationPresent(GraphQLTypeName.class)) {
            final String val = ((GraphQLTypeName) clz.getAnnotation(GraphQLTypeName.class)).inputName();
            if (!val.isBlank()) {
                return val;
            }
        }
        if (clz.isAnnotationPresent(GraphQLDto.class)) {
            final String val = ((GraphQLDto) clz.getAnnotation(GraphQLDto.class)).inputName();
            if (!val.isBlank()) {
                return val;
            }
        }
        final String pckgName = clz.getPackageName();
        Preconditions.checkArgument(!pckgName.isBlank(), "Expecting package for " + clz);
        final String name = (clz.getPackageName() + "." + clz.getSimpleName()).replace('.', '_');
        return name + INPUT_SUFFIX;
    }

    public static String getName(final Class clz) {
        if (clz.isAnnotationPresent(GraphQLTypeName.class)) {
            final String val = ((GraphQLTypeName) clz.getAnnotation(GraphQLTypeName.class)).value();
            if (!val.isBlank()) {
                return val;
            }
        }
        if (clz.isAnnotationPresent(GraphQLDto.class)) {
            final String val = ((GraphQLDto) clz.getAnnotation(GraphQLDto.class)).value();
            if (!val.isBlank()) {
                return val;
            }
        }
        final String pckgName = clz.getPackageName();
        Preconditions.checkArgument(!pckgName.isBlank(), "Expecting package for " + clz);
        final String name = (clz.getPackageName() + "." + clz.getSimpleName()).replace('.', '_');
        if (clz.isInterface()) {
            return "I_" + name;
        } else {
            return name;
        }
    }


    public static Set<Type> getPossibleOutputs(final Class<?> aClass) {
        Set<Type> set = Sets.newHashSet();
        final GraphQLReturnTypes[] retTypes = aClass.getAnnotationsByType(GraphQLReturnTypes.class);
        for (GraphQLReturnTypes returnTypes : retTypes) {
            set.addAll(Arrays.asList(returnTypes.values()));
        }
        return set;

    }

}
