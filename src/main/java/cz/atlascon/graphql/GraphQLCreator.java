package cz.atlascon.graphql;

import com.google.common.collect.Lists;
import cz.atlascon.graphql.invoke.GraphQLFilter;
import cz.atlascon.graphql.ng.resources.ResourceMethod;
import cz.atlascon.graphql.ng.resources.ResourceProcessor;
import cz.atlascon.graphql.schemas.types.CompositeGraphQLTypeFactory;
import cz.atlascon.graphql.schemas.types.GraphQLTypeFactory;
import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GraphQLCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLCreator.class);
    private List<Type> extraOutputTypes = Lists.newArrayList();
    private List<ResourceProcessor> resourceProcessors = Lists.newArrayList();
    private final List<GraphQLFilter> filters = Lists.newArrayList();
    private GraphQLTypeFactory typeFactory = null;
    private Instrumentation instrumentation;

    public GraphQLCreator setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        return this;
    }

    public List<GraphQLFilter> getFilters() {
        return filters;
    }

    public GraphQLCreator addFilter(final GraphQLFilter filter) {
        filters.add(filter);
        return this;
    }

    public static GraphQLCreator newBuilder() {
        return new GraphQLCreator();
    }

    public GraphQLCreator setTypeFactory(GraphQLTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
        return this;
    }

    public List<Type> getExtraOutputTypes() {
        return extraOutputTypes;
    }

    public GraphQLCreator addOutputTypes(final Collection<Type> types) {
        types.forEach(this::addOutputType);
        return this;
    }

    public GraphQLCreator addOutputType(final Type type) {
        this.extraOutputTypes.add(type);
        return this;
    }

    public GraphQLCreator addGqlResource(final Object gqlResource) {
        this.resourceProcessors.add(new ResourceProcessor(gqlResource));
        return this;
    }


    private GraphQLSchema.Builder createSchemaBuilder() {
        LOGGER.info("Creating root schema");
        final List<ResourceMethod> ops = resourceProcessors.stream()
                .map(ResourceProcessor::getResourceMethods)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        final List<ResourceMethod> fields = resourceProcessors.stream()
                .map(ResourceProcessor::getResourceFields)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        final GraphQLGenerator generator = new GraphQLGenerator(ops, fields, extraOutputTypes, filters,
                typeFactory == null ? CompositeGraphQLTypeFactory.createDefault() : typeFactory);
        return generator.getSchemaBuilder();
    }

    public GraphQL build() {
        return buildWithCustomization(null);
    }

    public GraphQL buildWithCustomization(Consumer<GraphQL.Builder> customizer) {
        final GraphQLSchema.Builder schemaBuilder = createSchemaBuilder();
        final GraphQLSchema graphQLSchema = schemaBuilder.build();
        final GraphQL.Builder builder = GraphQL.newGraphQL(graphQLSchema);
        if (instrumentation != null) {
            builder.instrumentation(instrumentation);
        }
        if (customizer != null) {
            customizer.accept(builder);
        }
        final GraphQL graphql = builder.build();
        LOGGER.info("GraphQL schema created");
        return graphql;
    }

}
