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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReferenceForInterfaceReturnTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addOutputTypes(List.of(
                        DemoObjImpl.class, DemoObjImplDto.class, DemoObjExtended.class
                ))
                .addGqlResource(
                        new Object() {
                            @GraphQlQuery("demoObj")
                            public DemoObj get(final @GraphQLParam("arg") DemoObjIfaceRef ref) {
                                if (ref.getId() == 0) {
                                    return new DemoObjImpl(ref.getId(), ref.getId() + " - was referenced ID");
                                } else if (ref.getId() == 1) {
                                    return new DemoObjImplDto(ref.getId(), ref.getId() + " - was referenced ID");
                                } else if (ref.getId() == 2) {
                                    return new DemoObjExtended(ref.getId(), ref.getId() + " - was referenced ID", UUID.randomUUID().toString());
                                } else {
                                    throw new IllegalArgumentException("?");
                                }
                            }

                        }).addGqlResource(
                        new Object() {
                            @GraphQlQuery("noImplRef")
                            public DemoObjIfaceRef get(final @GraphQLParam("arg") String ref) {
                                return new DemoObjIfaceRef(ref == null ? 0 : ref.length());
                            }
                        }
                ).build();
    }

    @Test
    public void shouldReturnNoImplRef() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ noImplRef(arg: \"b\") { id, val, list } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"noImplRef\":{\"id\":1,\"val\":\"1 - was referenced ID\",\"list\":[]}}}",
                response);
    }


}