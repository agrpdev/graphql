package cz.atlascon.graphql.schemas.pojo;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import cz.atlascon.graphql.common.Common;
import cz.atlascon.graphql.invoke.InvokeContext;
import cz.atlascon.graphql.ng.GraphQLReference;
import cz.atlascon.graphql.ng.GraphQLUnion;
import cz.atlascon.graphql.ng.resources.ResourceMethod;
import graphql.schema.*;
import io.reactivex.Flowable;
import org.dataloader.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PojoResolvingConvertor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PojoResolvingConvertor.class);
    private final InvokeContext invokeContext;

    public PojoResolvingConvertor(InvokeContext invokeContext) {
        this.invokeContext = invokeContext;
    }

    public Object convert(Object val) {
        if (val == null) {
            if (invokeContext.getEnvironment().getFieldType() instanceof GraphQLNonNull) {
                final GraphQLType wrapped = ((GraphQLNonNull) invokeContext.getEnvironment().getFieldType()).getWrappedType();
                if (wrapped instanceof GraphQLList) {
                    return List.of();
                } else {
                    throw new IllegalArgumentException(
                            "Got null value at " + invokeContext.getEnvironment().getExecutionStepInfo() + ", expecting non null");
                }
            }
            return null;
        }
        if (val instanceof Flowable<?>) {
            return ((Flowable<?>) val).map(this::convert);
        }
        if (val instanceof GraphQLUnion) {
            return convert(((GraphQLUnion) val).getValue());
        }
        if (val instanceof GraphQLReference) {
            // resolved reference
            final GraphQLReference ref = (GraphQLReference) val;
            final DataLoader<Object, Object> dataLoader = getDataLoaderForReference(ref);
            if (dataLoader != null) {
                // using data loader
                final CompletableFuture<Object> future = dataLoader.load(ref, invokeContext.getEnvironment());
                return future.thenApply(this::convert);
            } else {
                return resolveRefByQuery(ref);
            }
        }
        if (val instanceof Map) {
            Map map = (Map) val;
            return map
                    .entrySet()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(e -> {
                                Map.Entry entry = (Map.Entry) e;
                                final Object key = entry.getKey();
                                final Object value = entry.getValue();
                                if (key == null || value == null) {
                                    LOGGER.warn("Entry with nulls in map, val {} at {}", entry, invokeContext.getEnvironment());
                                    return null;
                                } else {
                                    return Map.entry(
                                            convert(key),
                                            convert(value)
                                    );
                                }
                            }
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else if (val instanceof Collection) {
            final Collection collection = (Collection) val;
            final boolean allReferences = collection.stream().allMatch(e -> e instanceof GraphQLReference);
            if (allReferences) {
                // TODO references must be distributed to data loaders (if exists), batch resolved and then assembled back
                //  again in the order of arrival. If no data loader for reference exists, it must be resolved by resolver
                // collection of references
                final Collection<GraphQLReference> referenceList = (Collection<GraphQLReference>) collection;
                final Set<Optional<DataLoader<Object, Object>>> loaders = Sets.newHashSet();
                referenceList.stream()
                        .map(this::getDataLoaderForReference)
                        .map(Optional::ofNullable)
                        .forEach(loaders::add);
                // all loaders are present
                final boolean hasAllLoaders = loaders.stream().allMatch(Optional::isPresent);
                // and all present loaders are of single instance
                final boolean isSingleLoader = loaders.stream().filter(Optional::isPresent).map(Optional::get)
                        .collect(Collectors.toSet()).size() == 1;
                if (hasAllLoaders && isSingleLoader) {
                    // single loader
                    final List<Object> keyContextes = IntStream.range(0, collection.size()).mapToObj(i -> invokeContext.getEnvironment()).collect(Collectors.toList());
                    final CompletableFuture<List<Object>> futures = loaders.iterator().next().get().loadMany(List.copyOf(collection), keyContextes);
                    return futures.thenApply(this::convert);
                } else {
                    // if no or mutliple loaders -> load by reference
                    return collection.stream()
                            .map(r -> resolveRefByQuery((GraphQLReference) r))
                            .map(this::convert)
                            .collect(Collectors.toList());
                }
            } else {
                final Optional<GraphQLUnion> union = collection.stream().filter(e -> e instanceof GraphQLUnion).findAny();
                if (union.isPresent()) {
                    return convert(collection.stream()
                            .filter(Objects::nonNull)
                            .map(u -> ((GraphQLUnion) u).getValue())
                            .collect(Collectors.toList()));
                } else {
                    return ((Collection) val).stream()
                            .filter(Objects::nonNull)
                            .map(this::convert)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            }
        } else {
            // no conversion
            return val;
        }
    }

    private Object resolveRefByQuery(GraphQLReference ref) {
        final ResourceMethod rm = invokeContext.getResolvers().get(ref.getClass());
        if (rm == null) {
            LOGGER.warn("No resolver found for {}", ref.getClass());
            return null;
        }
        final String paramName = Iterables.getOnlyElement(rm.getInputs().values());
        final DataFetchingEnvironment resolverEnv = DataFetchingEnvironmentImpl
                .newDataFetchingEnvironment(invokeContext.getEnvironment())
                .arguments(Map.of(paramName, ref)).build();
        return invokeContext.forResolver(resolverEnv, rm).invoke();
    }

    private DataLoader<Object, Object> getDataLoaderForReference(GraphQLReference ref) {
        return invokeContext.getEnvironment().getDataLoader(Common.getInputName(ref.getClass()));
    }

}
