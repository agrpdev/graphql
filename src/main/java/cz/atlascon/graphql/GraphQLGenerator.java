package cz.atlascon.graphql;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import cz.atlascon.graphql.invoke.GraphQLFilter;
import cz.atlascon.graphql.invoke.InvokeContext;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLReference;
import cz.atlascon.graphql.ng.resources.ResourceMethod;
import cz.atlascon.graphql.schemas.CommonSchemaGenerator;
import cz.atlascon.graphql.schemas.pojo.PojoInputSchemaGenerator;
import cz.atlascon.graphql.schemas.pojo.PojoOutputSchemaGenerator;
import cz.atlascon.graphql.schemas.types.GraphQLTypeFactory;
import graphql.schema.*;
import graphql.schema.GraphQLSchema.Builder;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tomas on 6.6.17.
 */
public class GraphQLGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLGenerator.class);
    private final List<ResourceMethod> ops;
    private final PojoInputSchemaGenerator pojoInputSchemaGenerator;
    private final PojoOutputSchemaGenerator pojoOutputSchemaGenerator;
    private final List<Type> extraOutputTypes;
    private final GraphQLTypeFactory typeFactory;
    private final Map<Class, ResourceMethod> resolverMap;
    private final List<GraphQLFilter> filters;
    private final Map<FieldCoordinates, ResourceMethod> fields;

    public GraphQLGenerator(final List<ResourceMethod> ops,
                            final List<ResourceMethod> fields,
                            final List<Type> extraOutputTypes,
                            final List<GraphQLFilter> filters,
                            final GraphQLTypeFactory typeFactory) {
        this.filters = filters == null ? List.of() : List.copyOf(filters);
        Preconditions.checkNotNull(ops);
        Preconditions.checkNotNull(extraOutputTypes);
        Preconditions.checkNotNull(typeFactory);
        this.ops = ops;
        // map fields to coordinates
        this.fields = fields.stream()
                .map(rm -> rm.getFieldCoordinates().stream().map(coords -> Map.entry(coords, rm)).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.resolverMap = createResolverMap(ops);
        final CommonSchemaGenerator commonSchemaGenerator = new CommonSchemaGenerator();
        this.typeFactory = typeFactory;
        this.pojoInputSchemaGenerator = new PojoInputSchemaGenerator(typeFactory, commonSchemaGenerator);
        this.pojoOutputSchemaGenerator = new PojoOutputSchemaGenerator(typeFactory, pojoInputSchemaGenerator,
                commonSchemaGenerator);
        this.extraOutputTypes = extraOutputTypes;
    }

    private static Map<Class, ResourceMethod> createResolverMap(List<ResourceMethod> ops) {
        return ops.stream()
                .filter(op -> op.getInputs().size() == 1)
                .map(op -> {
                    final int paramIndex = Iterables.getOnlyElement(op.getInputs().keySet());
                    final Type type = op.getMethod().getGenericParameterTypes()[paramIndex];
                    if (type instanceof Class && ClassUtils.getAllInterfaces(((Class) type)).contains(GraphQLReference.class)) {
                        return Map.entry((Class) type, op);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public GraphQLSchema.Builder getSchemaBuilder() {
        // pojo
        LOGGER.debug("Creating graphql POJO types");
        extraOutputTypes.forEach(pojoOutputSchemaGenerator::createOutputType);

        // code registry
        final GraphQLCodeRegistry.Builder crBuilder = GraphQLCodeRegistry.newCodeRegistry();

        final Map<String, List<GraphQLFieldDefinition>> fields = Maps.newLinkedHashMap();

        // query resolvers
        ops.stream().forEach(r -> {
            final GraphQLFieldDefinition resolverField = createResolverField(r);
            final DataFetcher resolverDataFetcher = createDataFetcher(r);
            final String parentTypeName = r.getParentTypeName();
            fields.computeIfAbsent(parentTypeName, f -> Lists.newArrayList()).add(resolverField);
            crBuilder.dataFetcher(FieldCoordinates.coordinates(parentTypeName, resolverField.getName()), resolverDataFetcher);
        });

        // attach fetchers
        pojoOutputSchemaGenerator.getClassOutputSchemaCreator().getFetchers().forEach((coords, method) -> {
            final ResourceMethod overrideResourceMethod = this.fields.get(coords);
            if (overrideResourceMethod != null) {
                final DataFetcher dataFetcher = createDataFetcher(overrideResourceMethod);
                crBuilder.dataFetcher(coords, dataFetcher);
            } else {
                final DataFetcher dataFetcher = createDataFetcher(new ResourceMethod(method, null));
                crBuilder.dataFetcher(coords, dataFetcher);
            }
        });


        LOGGER.debug("Types created, creating root schema");

        final Builder builder = GraphQLSchema.newSchema();

        Optional.ofNullable(fields.get("Queries"))
                .ifPresent(list -> builder.query(GraphQLObjectType.newObject().fields(list).name("Queries").build()));
        Optional.ofNullable(fields.get("Mutations"))
                .ifPresent(list -> builder.mutation(GraphQLObjectType.newObject().fields(list).name("Mutations").build()));
        Optional.ofNullable(fields.get("Subscriptions"))
                .ifPresent(list -> builder.subscription(GraphQLObjectType.newObject().fields(list).name("Subscriptions").build()));

        final Map<String, TypeResolver> resolvers = pojoOutputSchemaGenerator.getTypeResolvers();
        resolvers.forEach(crBuilder::typeResolver);
        final Set<GraphQLType> additional = Sets.newHashSet(Set.copyOf(pojoOutputSchemaGenerator.getTypes()));

        builder.additionalTypes(additional).codeRegistry(crBuilder.build());

        LOGGER.debug("Root schema created");
        return builder;
    }

    private GraphQLInputType getInputType(final Method method, final String paramName) {
        final Type[] genericParameterTypes = method.getGenericParameterTypes();
        final Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            final Type type = genericParameterTypes[i];
            final List<Annotation> anots = Arrays.asList(annotations[i] == null ? new Annotation[0] : annotations[i]);
            final Optional<Annotation> argAnnot = anots.stream().filter(a -> a instanceof GraphQLParam).findAny();
            if (argAnnot.isPresent()) {
                final String value = ResourceMethod.getParameterName(method, i);
                if (Objects.equals(value, paramName)) {
                    return pojoInputSchemaGenerator.createInputType(anots, type);
                }
            }
        }
        throw new IllegalArgumentException("Parameter " + paramName + " not found on " + method);
    }

    private DataFetcher createDataFetcher(final ResourceMethod rm) {
        return new DataFetcher<>() {
            @Override
            public Object get(final DataFetchingEnvironment environment) {
                return new InvokeContext(environment, rm, resolverMap, filters).invoke();
            }
        };
    }

    private GraphQLFieldDefinition createResolverField(final ResourceMethod operationInfo) {
        try {
            final GraphQLOutputType outputType = pojoOutputSchemaGenerator.createOutputType(operationInfo.getMethod().getGenericReturnType());
            final GraphQLFieldDefinition.Builder builder = newResolverFieldDefinitionBuilder(operationInfo);
            return builder.type(outputType)
                    .name(operationInfo.getFieldName())
                    .description("Generated graphQL schema for op: " + operationInfo.getFieldName())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Exception creating schema for operation " + operationInfo, e);
        }
    }

    private GraphQLFieldDefinition.Builder newResolverFieldDefinitionBuilder(final ResourceMethod operationInfo) {
        Preconditions.checkNotNull(operationInfo);
        final GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition();
        operationInfo.getInputs().forEach((paramIndex, paramName) -> {
            final GraphQLInputType inputType = getInputType(operationInfo.getMethod(), paramName);
            builder.argument(
                    GraphQLArgument.newArgument()
                            .name(paramName)
                            // TODO description from parameter
                            .description(paramIndex + ":" + paramName + ":" + inputType)
                            .type(inputType)
            );
        });
        return builder;
    }


}
