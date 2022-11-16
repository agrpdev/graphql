package cz.atlascon.graphql.invoke;

import cz.atlascon.graphql.ng.resources.ResourceMethod;
import cz.atlascon.graphql.schemas.pojo.PojoDeserializer;
import cz.atlascon.graphql.schemas.pojo.PojoResolvingConvertor;
import graphql.execution.Async;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class InvokeContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvokeContext.class);
    private final DataFetchingEnvironment environment;
    private final ResourceMethod resourceMethod;
    private final Map<Class, ResourceMethod> resolvers;
    private final List<GraphQLFilter> preFilters;


    public InvokeContext(final DataFetchingEnvironment environment,
                         final ResourceMethod resourceMethod,
                         final Map<Class, ResourceMethod> resolvers,
                         final List<GraphQLFilter> preFilters) {
        this.environment = environment;
        this.resourceMethod = resourceMethod;
        this.resolvers = resolvers;
        this.preFilters = preFilters;
    }

    public Object invoke() {
        try {
            final Method method = resourceMethod.getMethod();
            final Map<Integer, String> inputs = resourceMethod.getInputs();
            final Object[] params = new Object[method.getParameters().length];
            inputs.forEach((argIndex, argName) -> {
                final Object arg = environment.getArgument(argName);
                // TODO default value
                final Type argType = method.getGenericParameterTypes()[argIndex];
                final Object converted = PojoDeserializer.convert(arg, argType);
                params[argIndex] = converted;
            });

            final Object src;
            if (resourceMethod.getResourceInstance() != null) {
                src = resourceMethod.getResourceInstance();
            } else if (environment.getSource() != null) {
                src = environment.getSource();
            } else {
                throw new IllegalStateException("Unknown source!");
            }
            if (src instanceof CompletableFuture) {
                return Async.toCompletableFuture(src)
                        .thenApply(source -> filterAndConvert(this, environment, resourceMethod, preFilters, source, params));
            } else {
                return filterAndConvert(this, environment, resourceMethod, preFilters, src, params);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Exception calling resolver", e);
            throw e;
        }
    }

    private static Object filterAndConvert(final InvokeContext ctx,
                                           final DataFetchingEnvironment environment,
                                           final ResourceMethod resourceMethod,
                                           final List<GraphQLFilter> filters,
                                           final Object source,
                                           final Object[] params) {
        try {
            filters.forEach(f -> f.onBeforeInvoke(environment, source, resourceMethod, params));
            Object result = resourceMethod.getMethod().invoke(source, params);
            return new PojoResolvingConvertor(ctx).convert(result);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            }
            throw new RuntimeException(e);
        }
    }

    public DataFetchingEnvironment getEnvironment() {
        return environment;
    }

    public ResourceMethod getResourceMethod() {
        return resourceMethod;
    }

    public Map<Class, ResourceMethod> getResolvers() {
        return resolvers;
    }

    public List<GraphQLFilter> getPreFilters() {
        return preFilters;
    }

    public InvokeContext forResolver(DataFetchingEnvironment resolverEnv, ResourceMethod rm) {
        return new InvokeContext(resolverEnv, rm, resolvers, preFilters);
    }
}
