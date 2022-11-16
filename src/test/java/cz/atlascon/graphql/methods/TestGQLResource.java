package cz.atlascon.graphql.methods;

import com.google.common.collect.Maps;
import cz.atlascon.graphql.GraphQLDataLoader;
import cz.atlascon.graphql.GraphQlMutation;
import cz.atlascon.graphql.GraphQlQuery;
import cz.atlascon.graphql.invoke.ContextInject;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLFields;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLResource;
import cz.atlascon.graphql.pojo.PojoObject;
import cz.atlascon.graphql.pojo.PojoRequest;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.MappedBatchLoaderWithContext;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@GraphQLResource
public class TestGQLResource {

    @GraphQlQuery("dataLoaderTest")
    public List<PojoRequest> getDatas() {
        return List.of(new PojoRequest(1, true),
                new PojoRequest(2, true),
                new PojoRequest(3, true));
    }

    @GraphQLDataLoader(referenceType = PojoRequest.class)
    public static class PojoDataLoader implements MappedBatchLoaderWithContext<PojoRequest, PojoObject> {

        @Override
        public CompletionStage<Map<PojoRequest, PojoObject>> load(Set<PojoRequest> keys, BatchLoaderEnvironment environment) {
            final Object globalContext = environment.getContext();
            Map<PojoRequest, PojoObject> m = Maps.newHashMap();
            keys.forEach(r -> {
                final Object keyContext = environment.getKeyContexts().get(r);
                m.put(r, new PojoObject(r.getId(), PojoObject.Type.NORMAL, "name-" + r.getId()));
            });
            return CompletableFuture.completedFuture(m);
        }
    }

    @GraphQlQuery("multiInputs")
    public List<String> testMultiInputs(@Nonnull @GraphQLParam final String arg0,
                                        @GraphQLParam final int arg1,
                                        @GraphQLParam final long arg2) {
        return List.of(arg0, arg1 + "", arg2 + "");
    }


    @GraphQlQuery("testListInOut")
    public List<String> testListInOut(@Nonnull @GraphQLParam("inputArg") final List<String> input) {
        return List.of("bla bla bla input " + input + " from " + input);
    }

    @GraphQLField(parentType = "cz_atlascon_graphql_pojo_PojoObject", value = "infoList")
    public List<String> getInfoList(DataFetchingEnvironment dataFetchingEnvironment,
                                    @ContextInject InjectedObj injected,
                                    @ContextInject String username) {
        return List.of("success", "calling", "field", "from", "resource");
    }

    @GraphQLFields(value = {
            @GraphQLField(parentType = "cz_atlascon_graphql_pojo_PojoObject", value = "infoList2"),
            @GraphQLField(parentType = "cz_atlascon_graphql_pojo_PojoObject", value = "infoList3")
    })
    public List<String> getInfoList2_3(DataFetchingEnvironment dataFetchingEnvironment,
                                       @ContextInject InjectedObj injected,
                                       @ContextInject String username) {
        return List.of("success23", "calling23", "field23", "from23", "resource23");
    }

    @GraphQlQuery("resourceSimpleInOutTest")
    public String queryTest(@Nonnull @GraphQLParam("inputArg") final String input,
                            @ContextInject InjectedObj injected,
                            @ContextInject String username) {
        return "bla bla bla input " + input + " from " + username;
    }

    @GraphQlQuery("resourceComplexInOutTest")
    public PojoObject queryTest2(@Nonnull @GraphQLParam("pojo") final PojoObject input,
                                 @ContextInject(optional = true) InjectedObj injected,
                                 @ContextInject String username) {
        return new PojoObject(123, PojoObject.Type.NORMAL, "bla bla bla input " + input.getId() + " from " + username + " injobj? - " + (injected == null));
    }

    @GraphQlMutation("queryTestNoArg")
    public PojoObject queryTest4(@ContextInject(optional = true) InjectedObj injected,
                                 @ContextInject(optional = true) String username) {
        return new PojoObject(1234, PojoObject.Type.NORMAL, "bla bla bla input " + injected + " from " + username);
    }

    @GraphQlMutation("mutationTestSimplest")
    public String queryTest4() {
        return "simple mutation";
    }

}
