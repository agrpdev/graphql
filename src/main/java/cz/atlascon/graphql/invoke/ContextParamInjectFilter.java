package cz.atlascon.graphql.invoke;

import cz.atlascon.graphql.ng.resources.ResourceMethod;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class ContextParamInjectFilter implements GraphQLFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextParamInjectFilter.class);

    @Override
    public void onBeforeInvoke(DataFetchingEnvironment environment, Object source, ResourceMethod method, Object[] params) {
        injectParams(environment, method.getMethod(), params);
    }

    private void injectParams(DataFetchingEnvironment environment, final Method method, Object[] params) {
        final GraphQLContext ctx = environment.getGraphQlContext();
        final Type[] genericParameterTypes = method.getGenericParameterTypes();
        final Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < genericParameterTypes.length; i++) {
            final Type paramType = genericParameterTypes[i];
            // data fetching environment inject
            if (paramType == DataFetchingEnvironment.class) {
                params[i] = environment;
                continue;
            }
            final List<Annotation> anots = Arrays.asList(annotations[i] == null ? new Annotation[0] : annotations[i]);
            final Optional<Annotation> argAnnot = anots.stream().filter(a -> a instanceof ContextInject).findAny();
            if (argAnnot.isPresent()) {
                final Optional<Object> val = ctx.stream().filter(e -> {
                            final Object key = e.getKey();
                            if (key instanceof Class && paramType instanceof Class) {
                                return ((Class<?>) paramType).isAssignableFrom((Class) key);
                            } else if (key instanceof Type) {
                                return Objects.equals(((Type) e).getTypeName(), paramType.getTypeName());
                            }
                            return false;
                        }).map(Map.Entry::getValue)
                        .findAny();
                if (val.isPresent()) {
                    params[i] = val.get();
                } else if (!((ContextInject) argAnnot.get()).optional()) {
                    // missing non-optional value
                    throw new IllegalArgumentException("Unable to inject value of type " + paramType + " to " + method + " from GraphQLContext");
                }
            }
        }
    }

}
