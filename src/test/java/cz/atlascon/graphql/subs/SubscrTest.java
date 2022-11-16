package cz.atlascon.graphql.subs;

import cz.atlascon.graphql.GraphQLCreator;
import cz.atlascon.graphql.GraphqlRequest;
import cz.atlascon.graphql.pojo.PojoResolver;
import graphql.Assert;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.cachecontrol.CacheControl;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.HashMap;
import java.util.Map;

public class SubscrTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addGqlResource(new SubscrResolver())
                .addGqlResource(new PojoResolver())
                .build();
    }


    @Test
    public void shouldGetFlowable() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "Subscriptions");
        map.put("variables", Map.of());
        map.put("query", "subscription { subscr(arg : \"testinput\") { id, type, name(prependBs: false) } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        final Object data = result.getData();
        Assert.assertTrue(data instanceof Publisher);
        Publisher p = ((Publisher) data);

        final Subscriber<Object> s = new Subscriber<>() {
            @Override
            public void onSubscribe(final Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(final Object o) {
                System.out.println(o);
            }

            @Override
            public void onError(final Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("DONE");
            }
        };
        p.subscribe(s);


    }

}