package cz.atlascon.graphql.ng.resources;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import cz.atlascon.graphql.GraphQlMutation;
import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.GraphQlSubscription;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLFields;
import cz.atlascon.graphql.ng.GraphQLParam;
import graphql.schema.FieldCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceMethod {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceMethod.class);
    private final Method method;
    private final Object resourceInstance;
    private final Map<Integer, String> inputs;

    public ResourceMethod(Method method, Object resourceInstance) {
        Preconditions.checkNotNull(method);
        this.method = method;
        this.method.setAccessible(true);
        this.resourceInstance = resourceInstance;
        this.inputs = createInputsMap();
    }

    public Map<Integer, String> getInputs() {
        return inputs;
    }

    private Map<Integer, String> createInputsMap() {
        final Type[] genericParameterTypes = method.getGenericParameterTypes();
        final Annotation[][] annotations = method.getParameterAnnotations();
        final Map<Integer, String> inputs = Maps.newHashMap();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            final List<Annotation> anots = Arrays.asList(annotations[i] == null ? new Annotation[0] : annotations[i]);
            final Optional<Annotation> argAnnot = anots.stream().filter(a -> a instanceof GraphQLParam).findAny();
            if (argAnnot.isPresent()) {
                inputs.put(i, getParameterName(method, i));
            }
        }
        return inputs;
    }

    public static String getParameterName(final Method method, final int paramIndex) {
        final Optional<Annotation> argAnnot = Arrays.stream(method.getParameterAnnotations()[paramIndex])
                .filter(a -> a instanceof GraphQLParam)
                .findAny();
        Preconditions.checkArgument(argAnnot.isPresent(), "Expecting GraphQLParam annotation on parameter #" + paramIndex + " at " + method);
        final String annotationDeclared = ((GraphQLParam) argAnnot.get()).value();
        if (!annotationDeclared.isBlank()) {
            return annotationDeclared.strip();
        }
        final Parameter param = method.getParameters()[paramIndex];
        if (param.isNamePresent()) {
            return param.getName();
        } else {
            LOGGER.warn("Unable to determine parameter name for parameter #{} in {}, name is not present, using default value of {}", paramIndex, method, GraphQLParam.DEFAULT_ARG_NAME);
            return GraphQLParam.DEFAULT_ARG_NAME;
        }
    }

    public String getParentTypeName() {
        if (isQuery()) {
            return "Queries";
        } else if (isMutation()) {
            return "Mutations";
        } else if (isSubscription()) {
            return "Subscriptions";
        } else if (isField()) {
            final String parentType = getMethod().getAnnotation(GraphQLField.class).parentType();
            Preconditions.checkArgument(!parentType.isBlank(), "No parent type for field");
            return parentType;
        } else throw new IllegalArgumentException("Unknown parent type name");
    }

    public String getFieldName() {
        return getQueryName();
    }

    private String getQueryName() {
        if (getMethod().getAnnotation(GraphQlQuery.class) != null) {
            return getMethod().getAnnotation(GraphQlQuery.class).value();
        } else if (getMethod().getAnnotation(GraphQlMutation.class) != null) {
            return getMethod().getAnnotation(GraphQlMutation.class).value();
        } else if (getMethod().getAnnotation(GraphQlSubscription.class) != null) {
            return getMethod().getAnnotation(GraphQlSubscription.class).value();
        } else if (getMethod().getAnnotation(GraphQLField.class) != null) {
            return getMethod().getAnnotation(GraphQLField.class).value();
        } else {
            throw new IllegalStateException(
                    "Unable to get query name! User annotation @GraphQlQuery/@GraphQLMutation/@GraphQLSubscription");
        }
    }

    public boolean isMutation() {
        return method.getAnnotation(GraphQlMutation.class) != null;
    }

    public boolean isSubscription() {
        return method.getAnnotation(GraphQlSubscription.class) != null;
    }

    public boolean isQuery() {
        return method.getAnnotation(GraphQlQuery.class) != null;
    }

    public boolean isField() {
        return method.getAnnotation(GraphQLField.class) != null;
    }

    public boolean isFields() {
        return method.getAnnotation(GraphQLFields.class) != null;
    }

    public Method getMethod() {
        return method;
    }

    public Object getResourceInstance() {
        return resourceInstance;
    }

    @Override
    public String toString() {
        return "ResourceMethod{" +
                "m=" + method +
                ", resourceInstance=" + resourceInstance +
                '}';
    }

    public List<FieldCoordinates> getFieldCoordinates() {
        if (isFields()) {
            final GraphQLFields fields = method.getAnnotation(GraphQLFields.class);
            return Arrays.stream(fields.value()).map(f -> {
                Preconditions.checkArgument(!f.parentType().isBlank(), "No parent type for field " + this.getMethod());
                return FieldCoordinates.coordinates(f.parentType(), f.value());
            }).collect(Collectors.toList());
        } else {
            return List.of(FieldCoordinates.coordinates(getParentTypeName(), getFieldName()));
        }
    }
}
