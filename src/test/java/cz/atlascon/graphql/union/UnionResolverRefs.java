package cz.atlascon.graphql.union;

import cz.atlascon.graphql.DemoObjRef;
import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.ng.*;


@GraphQLResource
public class UnionResolverRefs {

    @GraphQLTypeName("testUnion2")
    @GraphQLReturnTypes(values = {DemoObjRef.class})
    public static class TestUnion2 extends AbstractGraphQLUnion {

        public TestUnion2(final Object value) {
            super(value);
        }
    }

    @GraphQlQuery("union2")
    public TestUnion2 get(final @GraphQLParam("arg") String input) {
        return new TestUnion2(new DemoObjRef(0));
    }
}
