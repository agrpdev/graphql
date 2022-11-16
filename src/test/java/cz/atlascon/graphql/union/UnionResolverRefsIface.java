package cz.atlascon.graphql.union;

import cz.atlascon.graphql.DemoObj;
import cz.atlascon.graphql.DemoObjRef;
import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.ng.*;

@GraphQLResource
public class UnionResolverRefsIface {

    @GraphQLTypeName("testUnion3")
    @GraphQLReturnTypes(values = {DemoObj.class})
    public static class TestUnion3 extends AbstractGraphQLUnion {

        public TestUnion3(final Object value) {
            super(value);
        }
    }

    @GraphQlQuery("union3")
    public TestUnion3 get(final @GraphQLParam("arg") String input) {
        return new TestUnion3(new DemoObjRef(12));
    }
}
