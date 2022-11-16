package cz.atlascon.graphql.ng.resources;

import com.google.common.base.Preconditions;
import cz.atlascon.graphql.GraphQlMutation;
import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.GraphQlSubscription;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLFields;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceProcessor {

    private final Object resource;

    public ResourceProcessor(final Object gqlResource) {
        Preconditions.checkNotNull(gqlResource);
//        Preconditions.checkArgument(gqlResource.getClass().getAnnotation(GraphQLResource.class) != null, "Missing @GraphQLResource annotation on " + gqlResource);
        this.resource = gqlResource;
        validateResource();
    }

    public List<ResourceMethod> getResourceFields() {
        return Arrays.stream(resource.getClass().getMethods())
                .filter(m -> m.getAnnotation(GraphQLField.class) != null || m.getAnnotation(GraphQLFields.class) != null)
                .map(m -> new ResourceMethod(m, resource))
                .collect(Collectors.toList());
    }

    public List<ResourceMethod> getResourceMethods() {
        return Arrays.stream(resource.getClass().getMethods())
                .filter(m -> m.getAnnotation(GraphQlQuery.class) != null ||
                        m.getAnnotation(GraphQlMutation.class) != null ||
                        m.getAnnotation(GraphQlSubscription.class) != null)
                .map(m -> new ResourceMethod(m, resource))
                .collect(Collectors.toList());
    }

    private void validateResource() {
        final List<ResourceMethod> resourceMethods = getResourceMethods();
        final List<ResourceMethod> fields = getResourceFields();
        Preconditions.checkArgument(!resourceMethods.isEmpty() || !fields.isEmpty(), "No GraphQLQuery/Mutation/Subscription/Field found in " + resource);
    }

}
