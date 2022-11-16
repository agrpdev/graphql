package cz.atlascon.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.atlascon.graphql.ng.GraphQLParam;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.cachecontrol.CacheControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterfaceReturnTest {


    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addOutputTypes(List.of(
                        DemoObjImpl.class,
                        DemoObjExtended.class,
                        DemoObjImplDto.class
                ))
                .addGqlResource(new Object() {
                    @GraphQlQuery("test")
                    public DemoObj get(final @GraphQLParam("arg") @Nonnull Boolean extended) {
                        if (!extended) {
                            return new DemoObjImpl(666, "blalba");
                        } else {
                            return new DemoObjExtended(667, "lalala", "ffaaffaa");
                        }
                    }

                })
                .build();
    }

    @Test
    public void shouldGetImpl() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ test(arg : true) { id, val } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"test\":{\"id\":667,\"val\":\"lalala\"}}}",
                response);
    }

}