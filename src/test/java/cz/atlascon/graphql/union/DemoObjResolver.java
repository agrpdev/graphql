package cz.atlascon.graphql.union;

import cz.atlascon.graphql.DemoObjImpl;
import cz.atlascon.graphql.DemoObjRef;
import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLResource;


@GraphQLResource
public class DemoObjResolver {

    @GraphQlQuery("demo")
    public DemoObjImpl get(final @GraphQLParam("arg") DemoObjRef input) {
        return new DemoObjImpl(input.getId(), "valvalval");
    }
}
