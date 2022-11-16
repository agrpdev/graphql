package cz.atlascon.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.schemas.ExtendedItemRef;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.cachecontrol.CacheControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ComplexResolversTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addGqlResource(
                        new Object() {
                            @GraphQlQuery("mapResolveObjList")
                            public Map<String, String> get(final @GraphQLParam List<ExtendedItemRef> refList) {
                                final String ids = refList.stream().map(ExtendedItemRef::getDocId).map(Object::toString)
                                        .collect(Collectors.joining(","));
                                return Map.of("requestedIds", ids);
                            }
                        })
                .addGqlResource(
                        new Object() {
                            @GraphQlQuery("mapResolveStrList")
                            public Map<String, String> get(final @GraphQLParam List<String> arg) {
                                final String argsStr = String.join(",", arg);
                                return Map.of("args", argsStr);
                            }
                        })
                .addGqlResource(
                        new Object() {
                            @GraphQlQuery("mapResolveStrListList")
                            public Map<String, String> get(final @GraphQLParam List<List<String>> args) {
                                final HashMap<String, String> m = Maps.newLinkedHashMap();
                                for (int i = 0; i < args.size(); i++) {
                                    m.put("arg" + i, args.get(i).toString());
                                }
                                return m;
                            }
                        }).build();
    }

    @Test
    public void shouldParseInputListOfObjects() throws Exception {
        String response = query("{ mapResolveObjList(refList: [{docId: 1}, {docId: 2}]) { key, val }} ");
        Assert
                .assertEquals("{\"data\":{\"mapResolveObjList\":[{\"key\":\"requestedIds\",\"val\":\"1,2\"}]}}", response);
    }

    @Test
    public void shouldParseInputListOfStrings() throws Exception {
        String response = query("{ mapResolveStrList(arg: [\"in0\", \"in1\"]) { key, val }} ");
        Assert
                .assertEquals("{\"data\":{\"mapResolveStrList\":[{\"key\":\"args\",\"val\":\"in0,in1\"}]}}", response);
    }

    @Test
    public void shouldParseInputListOfListOfStrings() throws Exception {
        String response = query("{ mapResolveStrListList(args: [ [\"in0\",\"in1\"], [\"in2\",\"in3\"]]) { key, val }} ");
        Assert
                .assertEquals(
                        "{\"data\":{\"mapResolveStrListList\":[{\"key\":\"arg0\",\"val\":\"[in0, in1]\"},{\"key\":\"arg1\",\"val\":\"[in2, in3]\"}]}}",
                        response);
    }

    private String query(final String query) throws Exception {
        Map map = new HashMap();
        map.put("variables", Map.of());
        map.put("query", query);
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .context("user")
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        return response;
    }

}