package cz.atlascon.graphql.methods;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.atlascon.graphql.DataLoaders;
import cz.atlascon.graphql.GraphQLCreator;
import cz.atlascon.graphql.GraphqlRequest;
import cz.atlascon.graphql.invoke.ContextInject;
import cz.atlascon.graphql.invoke.ContextParamInjectFilter;
import cz.atlascon.graphql.invoke.GraphQLFilter;
import cz.atlascon.graphql.ng.resources.ResourceMethod;
import cz.atlascon.graphql.pojo.PojoObject;
import cz.atlascon.graphql.pojo.PojoRequest;
import graphql.*;
import graphql.cachecontrol.CacheControl;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldCompleteParameters;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphQLResourceTest {

    private GraphQL grapql;

    @Before
    public void setUp() {
        this.grapql = GraphQLCreator.newBuilder()
                .addFilter(new ContextParamInjectFilter())
                .addFilter(new GraphQLFilter() {
                    @Override
                    public void onBeforeInvoke(DataFetchingEnvironment environment, Object source, ResourceMethod method, Object[] params) {
                        for (int i = 0; i < method.getMethod().getParameterCount(); i++) {
                            final Type paramType = method.getMethod().getGenericParameterTypes()[i];
                            final ContextInject ci = method.getMethod().getParameters()[i].getAnnotation(ContextInject.class);
                            if (ci != null && paramType == InjectedObj.class) {
                                final InjectedObj param = (InjectedObj) params[i];
                                if (param == null && ci.optional()) {
                                    return;
                                }
                                if (param == null || !param.getValue().equals("pes")) {
                                    throw new IllegalArgumentException("not ok - no pes");
                                }
                            }
                        }
                    }
                })
                .addGqlResource(new TestGQLResource())
                .buildWithCustomization(builder -> {
                    builder.instrumentation(new SimpleInstrumentation() {
                        @Override
                        public InstrumentationContext<ExecutionResult> beginFieldComplete(InstrumentationFieldCompleteParameters parameters) {
                            return super.beginFieldComplete(parameters);
                        }

                        @Override
                        public InstrumentationContext<ExecutionResult> beginFieldListComplete(InstrumentationFieldCompleteParameters parameters) {
                            return super.beginFieldListComplete(parameters);
                        }

                    });
                });
    }

    @Test
    public void shouldUseSingleContextVariable() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "resourceComplexInOutTest");
        map.put("variables", Map.of());
        map.put("query", "{ resourceComplexInOutTest(pojo : { id: 654, type: NORMAL, name: \"testinput\"}) { id, type, name(prependBs: false) } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                // context injected object with "pejsek" will fail - expected "pes"
                .graphQLContext(Map.of(String.class, "username - non null context param"))
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());

        Assert.assertEquals(List.of(), result.getErrors());
        Assert.assertEquals("{\"data\":{\"resourceComplexInOutTest\":{\"id\":123,\"type\":\"NORMAL\",\"name\":\"bla bla bla input 654 from username - non null context param injobj? - true\"}}}", response);
    }

    @Test
    public void shouldFailValidation() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "resourceComplexInOutTest");
        map.put("variables", Map.of());
        map.put("query", "{ resourceComplexInOutTest(pojo : { id: 654, type: NORMAL, name: \"testinput\"}) { id, type, name(prependBs: false) } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                // context injected object with "pejsek" will fail - expected "pes"
                .graphQLContext(
                        Map.of(
                                String.class, "username - non null context param",
                                InjectedObj.class, new InjectedObj("pejsek")
                        ))
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(1, result.getErrors().size());
        final GraphQLError er = result.getErrors().get(0);
        Assert.assertTrue(er instanceof ExceptionWhileDataFetching);
        final ExceptionWhileDataFetching dfe = (ExceptionWhileDataFetching) er;
        final Throwable cause = dfe.getException();
        Assert.assertTrue(cause instanceof IllegalArgumentException);
        Assert.assertNull(cause.getCause());
        Assert.assertEquals(cause.getMessage(), "not ok - no pes");
    }

    @Test
    public void shouldTestListInOut() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "testListInOut");
        map.put("variables", Map.of());
        map.put("query", "{ testListInOut(inputArg : [\"testValForArg\",\"arg1\"])  }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        Assert.assertTrue(!((Map) result.getData()).isEmpty());


    }

    @Test
    public void shouldTestSimpleInOutResource() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "queryTest");
        map.put("variables", Map.of());
        map.put("query", "{ resourceSimpleInOutTest(inputArg : \"testValForArg\")  }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .graphQLContext(
                        Map.of(
                                String.class, "username",
                                InjectedObj.class, new InjectedObj("pes")
                        ))
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        Assert.assertTrue(!((Map) result.getData()).isEmpty());


    }

    @Test
    public void shouldFailOnNullRequiredContextParam() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "resourceComplexInOutTest");
        map.put("variables", Map.of());
        map.put("query", "{ resourceComplexInOutTest(pojo : { id: 654, type: NORMAL, name: \"testinput\"}) { id, type, name(prependBs: false) } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(1, result.getErrors().size());
        Assert.assertTrue(result.getErrors().get(0).getMessage().contains("Unable to inject value of type class java.lang.String to public cz.atlascon.graphql.pojo.PojoObject cz.atlascon.graphql.methods.TestGQLResource.queryTest2(cz.atlascon.graphql.pojo.PojoObject,cz.atlascon.graphql.methods.InjectedObj,java.lang.String) from GraphQLContext"));
    }

    @Test
    public void shouldQueryTestNoArg() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "Subscriptions");
        map.put("variables", Map.of());
        map.put("query", "mutation { queryTestNoArg { id, type, name(prependBs: false) } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        Assert.assertTrue(!((Map) result.getData()).isEmpty());
    }

    @Test
    public void shouldPassSimplestMutation() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "mutationTestSimplest");
        map.put("variables", Map.of());
        map.put("query", "mutation { mutationTestSimplest }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        Assert.assertTrue(!((Map) result.getData()).isEmpty());
    }

    @Test
    public void shouldCallFieldResolverFromDetacchedMethod() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "Subscriptions");
        map.put("variables", Map.of());
        map.put("query", "mutation { queryTestNoArg { infoList } }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .graphQLContext(
                        Map.of(
                                String.class, "username",
                                InjectedObj.class, new InjectedObj("pes")
                        ))
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());
        Assert.assertEquals("{\"data\":{\"queryTestNoArg\":{\"infoList\":[\"success\",\"calling\",\"field\",\"from\",\"resource\"]}}}", response);
    }

    @Test
    public void shouldCallMultipleParamsQuery() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "Subscriptions");
        map.put("variables", Map.of());
        map.put("query", "{ multiInputs(arg0: \"ar0val\", arg1: 123, arg2: 456) }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .graphQLContext(
                        Map.of(
                                String.class, "username",
                                InjectedObj.class, new InjectedObj("pes")
                        ))
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());
        Assert.assertEquals("{\"data\":{\"multiInputs\":[\"ar0val\",\"123\",\"456\"]}}", response);
    }

    @Test
    public void shouldCallDataLoader() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "Subscriptions");
        map.put("variables", Map.of());
        map.put("query", "{ dataLoaderTest {id, type, name(prependBs: false)} }");
        GraphqlRequest request = new GraphqlRequest(map);
        final Map<Class<?>, Object> ctx = Map.of(
                String.class, "username",
                InjectedObj.class, new InjectedObj("pes")
        );

        final TestGQLResource.PojoDataLoader pojoDataLoader = new TestGQLResource.PojoDataLoader();

        final DataLoader<PojoRequest, PojoObject> dataLoader = DataLoaders.create(pojoDataLoader, ctx);
        final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        dataLoaderRegistry.register(DataLoaders.getDataLoaderName(pojoDataLoader), dataLoader);

        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .dataLoaderRegistry(dataLoaderRegistry)
                .graphQLContext(ctx)
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());
        Assert.assertEquals("{\"data\":{\"dataLoaderTest\":[{\"id\":1,\"type\":\"NORMAL\",\"name\":\"name-1\"},{\"id\":2,\"type\":\"NORMAL\",\"name\":\"name-2\"},{\"id\":3,\"type\":\"NORMAL\",\"name\":\"name-3\"}]}}", response);
    }

    @Test
    public void shouldInjectToInputObject() throws Exception {
        Map map = new HashMap();
        map.put("operationName", "inputObjectInject");
        map.put("variables", Map.of());
        map.put("query", "{ inputObjectInject(input: { value : \"input value\"  } ) }");
        GraphqlRequest request = new GraphqlRequest(map);
        ExecutionInput execInput = ExecutionInput.newExecutionInput(request.getQuery())
                .variables(request.getVars())
                .graphQLContext(
                        Map.of(
                                String.class, "usernameXX"
                        ))
                .cacheControl(CacheControl.newCacheControl())
                .build();
        final ExecutionResult result = grapql.execute(execInput);

        Assert.assertEquals(List.of(), result.getErrors());
        final String response = new ObjectMapper().writeValueAsString(result.toSpecification());
        Assert.assertEquals("{\"data\":{\"inputObjectInject\":[\"usernameXX/input value\"]}}", response);
    }

}