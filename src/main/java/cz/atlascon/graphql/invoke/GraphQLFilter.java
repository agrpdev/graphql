package cz.atlascon.graphql.invoke;

import cz.atlascon.graphql.ng.resources.ResourceMethod;
import graphql.schema.DataFetchingEnvironment;

public interface GraphQLFilter {

    default void onBeforeInvoke(final DataFetchingEnvironment environment,
                                final Object source, final ResourceMethod method,
                                final Object[] params) {
    }

}
