package cz.atlascon.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.atlascon.graphql.pojo.PojoResolver;
import cz.atlascon.graphql.schemas.PojoOutputSchemaGeneratorTest.ExtendedItem;
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

public class GraphQLGeneratorTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addOutputType(DemoObjImplDto.class)
                .addOutputType(DemoObj.class)
                .addGqlResource(new ExtendedItemResolver())
                .addGqlResource(new PojoResolver())
                .addGqlResource(new Object() {
                    @GraphQlQuery("specialExtendedItem")
                    public ExtendedItem get() {
                        return new ExtendedItem(666);
                    }
                }).build();
    }

    @Test
    public void shouldCallParameter() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ pojo(arg: { id: 1}) { id, type, name(prependBs: true)} }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"pojo\":{\"id\":1,\"type\":\"NORMAL\",\"name\":\"BS_my name\"}}}",
                response);
    }

    @Test
    public void shouldCallResolverWithoutParams() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", "{ specialExtendedItem { thisIsIdField} }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"specialExtendedItem\":{\"thisIsIdField\":666}}}",
                response);
    }

    @Test
    public void shouldGetGraphqlData() throws IOException {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query",
                "{ extendedItem(arg: {docId: 1}) { "
                        + "thisIsIdField, "
                        + "complexMap {key, val {key, val}}, "
                        + "gg, "
                        + "extendedItem { thisIsIdField }, "
                        + "extendedItemMap { key, val { thisIsIdField } }"
                        + "} "
                        + "}");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(
                "{\"data\":{\"extendedItem\":{\"thisIsIdField\":1,\"complexMap\":[{\"key\":1,\"val\":[{\"key\":\"a\",\"val\":\"aa\"},{\"key\":\"b\",\"val\":\"bb\"}]},{\"key\":2,\"val\":[{\"key\":\"c\",\"val\":\"cc\"},{\"key\":\"d\",\"val\":\"cc\"}]}],\"gg\":\"WOT\",\"extendedItem\":{\"thisIsIdField\":889},\"extendedItemMap\":[{\"key\":\"a\",\"val\":{\"thisIsIdField\":123}},{\"key\":\"b\",\"val\":{\"thisIsIdField\":456}},{\"key\":\"c\",\"val\":{\"thisIsIdField\":789}}]}}}",
                response);
    }

}