package cz.atlascon.graphql.selfref;

import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLResource;

import java.util.UUID;


@GraphQLResource
public class ObjAResolver {

    @GraphQlQuery("resolveA")
    public ObjA get(final @GraphQLParam("arg") RefObjA input) throws Exception {
        return new ObjA(input.getId(), UUID.randomUUID().toString());
    }
}
