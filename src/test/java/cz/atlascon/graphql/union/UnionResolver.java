package cz.atlascon.graphql.union;

import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.ng.*;
import cz.atlascon.graphql.pojo.PojoObject;
import cz.atlascon.graphql.pojo.PojoObject.Type;


@GraphQLResource
public class UnionResolver {

    @GraphQLTypeName("testUnion")
    @GraphQLReturnTypes(values = {PojoObject.class})
    public static class TestUnion extends AbstractGraphQLUnion {

        public TestUnion(final Object value) {
            super(value);
        }
    }

    @GraphQlQuery("union")
    public TestUnion get(final @GraphQLParam("arg") String input) {
        return new TestUnion(new PojoObject(0, Type.NORMAL, "re: " + input));
    }
}
