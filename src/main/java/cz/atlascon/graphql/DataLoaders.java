package cz.atlascon.graphql;

import cz.atlascon.graphql.common.Common;
import org.dataloader.*;

import java.util.Map;

public class DataLoaders {

    public static String getDataLoaderName(final Object dataLoaderInstance) {
        final GraphQLDataLoader an = dataLoaderInstance.getClass().getAnnotation(GraphQLDataLoader.class);
        if (an == null) {
            throw new IllegalArgumentException("No @GraphQLDataLoader annotation present on " + dataLoaderInstance.getClass());
        }
        if (an.dataFetcherName().isEmpty() && an.referenceType() == Object.class) {
            throw new IllegalArgumentException("DataFetcherName or ReferenceType must be specified on @GraphQLDataLoader annotation present on " + dataLoaderInstance.getClass());
        }
        if (!an.dataFetcherName().isBlank()) {
            return an.dataFetcherName();
        } else {
            return Common.getInputName(an.referenceType());
        }
    }

    public static DataLoaderOptions optionsFromContext(final Map<Class<?>, Object> ctx) {
        return DataLoaderOptions.newOptions().setBatchLoaderContextProvider(() -> ctx);
    }

    public static <K, V> DataLoader<K, V> create(BatchLoaderWithContext<K, V> loader, Map<Class<?>, Object> graphQLContext) {
        return create(loader, optionsFromContext(graphQLContext));
    }

    public static <K, V> DataLoader<K, V> create(BatchLoader<K, V> loader, Map<Class<?>, Object> graphQLContext) {
        return create(loader, optionsFromContext(graphQLContext));
    }

    public static <K, V> DataLoader<K, V> create(MappedBatchLoader<K, V> loader, Map<Class<?>, Object> graphQLContext) {
        return create(loader, optionsFromContext(graphQLContext));
    }

    public static <K, V> DataLoader<K, V> create(MappedBatchLoaderWithContext<K, V> loader, Map<Class<?>, Object> graphQLContext) {
        return create(loader, optionsFromContext(graphQLContext));
    }

    public static <K, V> DataLoader<K, V> createWithTry(BatchLoaderWithContext<K, Try<V>> loader, Map<Class<?>, Object> graphQLContext) {
        return createWithTry(loader, optionsFromContext(graphQLContext));
    }

    public static <K, V> DataLoader<K, V> createWithTry(BatchLoader<K, Try<V>> loader, Map<Class<?>, Object> graphQLContext) {
        return createWithTry(loader, optionsFromContext(graphQLContext));
    }

    public static <K, V> DataLoader<K, V> createWithTry(MappedBatchLoader<K, Try<V>> loader, Map<Class<?>, Object> graphQLContext) {
        return createWithTry(loader, optionsFromContext(graphQLContext));
    }

    public static <K, V> DataLoader<K, V> createWithTry(MappedBatchLoaderWithContext<K, Try<V>> loader, Map<Class<?>, Object> graphQLContext) {
        return createWithTry(loader, optionsFromContext(graphQLContext));
    }

    public static <K, V> DataLoader<K, V> create(BatchLoaderWithContext<K, V> loader, DataLoaderOptions options) {
        return DataLoaderFactory.newDataLoader(loader, options);
    }

    public static <K, V> DataLoader<K, V> create(BatchLoader<K, V> loader, DataLoaderOptions options) {
        return DataLoaderFactory.newDataLoader(loader, options);
    }

    public static <K, V> DataLoader<K, V> create(MappedBatchLoader<K, V> loader, DataLoaderOptions options) {
        return DataLoaderFactory.newMappedDataLoader(loader, options);
    }

    public static <K, V> DataLoader<K, V> create(MappedBatchLoaderWithContext<K, V> loader, DataLoaderOptions options) {
        return DataLoaderFactory.newMappedDataLoader(loader, options);
    }

    public static <K, V> DataLoader<K, V> createWithTry(BatchLoaderWithContext<K, Try<V>> loader, DataLoaderOptions options) {
        return DataLoaderFactory.newDataLoaderWithTry(loader, options);
    }

    public static <K, V> DataLoader<K, V> createWithTry(BatchLoader<K, Try<V>> loader, DataLoaderOptions options) {
        return DataLoaderFactory.newDataLoaderWithTry(loader, options);
    }

    public static <K, V> DataLoader<K, V> createWithTry(MappedBatchLoader<K, Try<V>> loader, DataLoaderOptions options) {
        return DataLoaderFactory.newMappedDataLoaderWithTry(loader, options);
    }

    public static <K, V> DataLoader<K, V> createWithTry(MappedBatchLoaderWithContext<K, Try<V>> loader, DataLoaderOptions options) {
        return DataLoaderFactory.newMappedDataLoaderWithTry(loader, options);
    }


}
