package cz.atlascon.graphql.filters;

import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLDto;
import cz.atlascon.graphql.ng.GraphQLResource;
import cz.atlascon.graphql.pojo.PojoObject;

import javax.annotation.Nonnull;

@GraphQLResource
public class FilterResource {

    @GraphQLDto("Test")
    public static class Test {
        private final String id;
        private final String info;

        public Test(String id, String info) {
            this.id = id;
            this.info = info;
        }

        public String getId() {
            return id;
        }

        public String getInfo() {
            return info;
        }
    }

    @GraphQlQuery("test")
    public Test get(@Nonnull @GraphQLParam("id") final String id,
                    final PojoObject pojoObject) {
        return new Test(id, "blablabla");
    }
}
