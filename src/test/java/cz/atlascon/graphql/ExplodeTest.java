package cz.atlascon.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.Map;

public class ExplodeTest {

    public static class Input {

        private final String val;
        private final int intVal;

        public Input(final String val, int intVal) {
            this.val = val;
            this.intVal = intVal;
        }

        @JsonCreator
        public static Input create(@JsonProperty("val") String val,
                                   @JsonProperty("intVal") int intVal) {
            return new Input(val, intVal);
        }
    }

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addGqlResource(
                        new Object() {
                            @GraphQlQuery("test")
                            public String get(final @GraphQLParam(value = "namedArg") Input input) {
                                return input.val + input.intVal;
                            }
                        })
                .build();
    }

    @Test
    public void shouldReturnNoImplRef() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ test(namedArg: { val: \"test-val\", intVal: 123 }) }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals("{\"data\":{\"test\":\"test-val123\"}}", response);
    }


}