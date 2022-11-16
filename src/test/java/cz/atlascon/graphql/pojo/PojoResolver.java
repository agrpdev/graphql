package cz.atlascon.graphql.pojo;

import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLResource;
import cz.atlascon.graphql.pojo.PojoObject.Type;

@GraphQLResource
public class PojoResolver {

    @GraphQlQuery("pojo")
    public PojoObject get(final @GraphQLParam("arg") PojoRequest pojoRequest) {
        return new PojoObject(pojoRequest.getId(), Type.NORMAL, "my name");
    }
}
