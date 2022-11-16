package cz.atlascon.graphql;

import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLResource;
import cz.atlascon.graphql.schemas.ExtendedItemRef;
import cz.atlascon.graphql.schemas.PojoOutputSchemaGeneratorTest.ExtendedItem;


@GraphQLResource
public class ExtendedItemResolver {

    @GraphQlQuery("extendedItem")
    public ExtendedItem get(final @GraphQLParam("arg") ExtendedItemRef extendedItemRef) {
        return new ExtendedItem(extendedItemRef.getDocId());
    }
}
