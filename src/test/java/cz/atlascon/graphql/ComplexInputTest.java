package cz.atlascon.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLDto;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLTypeName;
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
import java.util.Map;

public class ComplexInputTest {

    @GraphQLTypeName("Arg")
    @GraphQLDto
    public static class Arg {
        private final String inner1;
        private final boolean inner2;

        @JsonCreator
        public Arg(@JsonProperty("inner1") String inner1,
                   @JsonProperty("inner2") boolean inner2) {
            this.inner1 = inner1;
            this.inner2 = inner2;
        }

        public String getInner1() {
            return inner1;
        }

        public boolean isInner2() {
            return inner2;
        }
    }

    @GraphQLTypeName("ComplexOutput")
    @GraphQLDto
    public static class ComplexOutput {

        private final String val;

        public ComplexOutput(String val) {
            this.val = val;
        }

        public String getVal() {
            return val;
        }

        public Arg getInner(@GraphQLParam("arg0") int arg0,
                            @GraphQLParam("arg1") ComplexInputTest.Arg inner) {
            return inner;
        }
    }

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addGqlResource(new Object() {
                    @GraphQlQuery("test")
                    public ComplexOutput test(final @Nonnull @GraphQLParam("input") String input) {
                        return new ComplexOutput("bla");
                    }

                })
                .build();
    }

    @Test
    public void shouldGetImpl() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ test(input: \"test\") {val, inner(arg0: 123, arg1: {inner1: \"test\", inner2: true}) {inner1}} }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals("{\"data\":{\"test\":{\"val\":\"bla\",\"inner\":{\"inner1\":\"test\"}}}}", response);
    }

}