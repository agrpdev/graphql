package cz.atlascon.graphql.schemas.decorators;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.schemas.CommonSchemaGenerator;
import cz.atlascon.graphql.schemas.pojo.PojoInputSchemaGenerator;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition.Builder;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ArgumentsDecorator implements OutputDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentsDecorator.class);
    private final PojoInputSchemaGenerator inputSchemaGenerator;

    public ArgumentsDecorator(final PojoInputSchemaGenerator inputSchemaGenerator) {
        Preconditions.checkNotNull(inputSchemaGenerator);
        this.inputSchemaGenerator = inputSchemaGenerator;
    }

    @Override
    public void decorate(final Builder fieldBuilder, final Method method) {
        final Map<String, Parameter> namedParams = mapParams(method);
        final Map<String, Annotation[]> paramAnnotations = getAnnotations(namedParams);
        final Map<String, Type> paramTypes = getTypes(namedParams);
        for (String paramName : namedParams.keySet()) {
            final Annotation[] annotations = paramAnnotations.get(paramName);
            final Type type = paramTypes.get(paramName);
            Nonnull nonnull = find(annotations, Nonnull.class);
            final GraphQLInputType gqlType = inputSchemaGenerator.createInputType(
                    annotations == null ? List.of() : Arrays.asList(annotations), type);
            final GraphQLInputType resulting;
            if (nonnull != null && !(gqlType instanceof GraphQLNonNull)) {
                resulting = CommonSchemaGenerator.wrapNonNull(gqlType);
            } else {
                resulting = gqlType;
            }
            GraphQLParam param = find(annotations, GraphQLParam.class);
            final GraphQLArgument arg = GraphQLArgument.newArgument().name(param.value())
                    .description(param.description().isBlank() ? null : param.description().strip()).type(resulting)
                    .build();
            fieldBuilder.argument(arg);
        }
        return;
    }

    private static LinkedHashMap<String, Type> getTypes(final Map<String, Parameter> namedParams) {
        return namedParams.entrySet().stream().map(e -> Map.entry(e.getKey(), e.getValue().getParameterizedType()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (m, mm) -> m, LinkedHashMap::new));
    }

    private static LinkedHashMap<String, Annotation[]> getAnnotations(final Map<String, Parameter> namedParams) {
        return namedParams.entrySet().stream().map(e -> Map.entry(e.getKey(), e.getValue().getAnnotations()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (m, mm) -> m, LinkedHashMap::new));
    }

    private static LinkedHashMap<String, Parameter> mapParams(final Method method) {
        final LinkedHashMap<String, Parameter> named = Maps.newLinkedHashMap();
        for (int i = 0; i < method.getParameters().length; i++) {
            final Parameter parameter = method.getParameters()[i];
            final GraphQLParam param = find(parameter.getAnnotations(), GraphQLParam.class);
            if (param == null) {
                continue;
            } else {
                final String argName = param.value();
                if (named.containsKey(argName)) {
                    throw new IllegalArgumentException("Duplicate argument name " + argName + " in " + method);
                }
                named.put(argName, parameter);
            }
        }
        return named;
    }


    private static <E extends Annotation> E find(final Annotation[] ans, final Class<E> anClz) {
        for (int i = 0; i < ans.length; i++) {
            if (ans[i].annotationType().isAssignableFrom(anClz)) {
                return (E) ans[i];
            }
        }
        return null;
    }
}
