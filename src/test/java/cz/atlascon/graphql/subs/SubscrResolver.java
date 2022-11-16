package cz.atlascon.graphql.subs;

import cz.atlascon.graphql.GraphQlSubscription;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLResource;
import cz.atlascon.graphql.pojo.PojoObject;
import cz.atlascon.graphql.pojo.PojoObject.Type;
import io.reactivex.Flowable;


@GraphQLResource
public class SubscrResolver {

    @GraphQlSubscription("subscr")
    public Flowable<PojoObject> get(final @GraphQLParam("arg") String input) {
        return Flowable.just(new PojoObject(0, Type.NORMAL, "re: " + input));
    }
}
