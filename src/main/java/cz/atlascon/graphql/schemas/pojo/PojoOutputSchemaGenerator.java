package cz.atlascon.graphql.schemas.pojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import cz.atlascon.graphql.common.Common;
import cz.atlascon.graphql.ng.GraphQLReference;
import cz.atlascon.graphql.ng.GraphQLUnion;
import cz.atlascon.graphql.schemas.CommonSchemaGenerator;
import cz.atlascon.graphql.schemas.types.GraphQLTypeFactory;
import graphql.TypeResolutionEnvironment;
import graphql.schema.*;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

public class PojoOutputSchemaGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PojoOutputSchemaGenerator.class);
    private final GraphQLTypeFactory typeFactory;
    private final ConcurrentMap<Type, GraphQLOutputType> outputMap = Maps.newConcurrentMap();
    private final ConcurrentMap<Class, GraphQLTypeReference> references = Maps.newConcurrentMap();
    private final ClassOutputSchemaCreator classOutputSchemaCreator;
    private final Set<String> unions = Sets.newConcurrentHashSet();
    private final TypeResolver resolver = new TypeResolver() {
        @Override
        public GraphQLObjectType getType(final TypeResolutionEnvironment env) {
            if (env.getObject() instanceof CompletableFuture) {
                throw new RuntimeException("CompletableFuture in TypeResolver - REPORT BUG!");
            }
            final Class<?> clz = env.getObject().getClass();
            return resolveType(env, clz);
        }
    };
    private CommonSchemaGenerator commonSchemaGenerator;

    private GraphQLObjectType resolveType(final TypeResolutionEnvironment env, final Class clz) {
        final GraphQLObjectType type = env.getSchema().getObjectType(Common.getName(clz));
        if (type == null) {
            Preconditions.checkArgument(GraphQLReference.class.isAssignableFrom(clz),
                    "Unknown return class " + clz + ", expecting GraphQLReference");
            final Type referencedType = getReferencedClass(clz).get();
            Class refClz = (Class) referencedType;
            final String refName = Common.getName(refClz);
            checkResolvedClassIsNotInterface(refClz, env);
            return env.getSchema().getObjectType(refName);
        } else {
            return type;
        }
    }

    public PojoOutputSchemaGenerator(final GraphQLTypeFactory typeFactory,
                                     final PojoInputSchemaGenerator inputSchemaGenerator,
                                     final CommonSchemaGenerator commonSchemaGenerator) {
        Preconditions.checkNotNull(typeFactory);
        Preconditions.checkNotNull(inputSchemaGenerator);
        this.commonSchemaGenerator = commonSchemaGenerator;
        this.classOutputSchemaCreator = new ClassOutputSchemaCreator(inputSchemaGenerator, this);
        this.typeFactory = typeFactory;
    }

    public ClassOutputSchemaCreator getClassOutputSchemaCreator() {
        return classOutputSchemaCreator;
    }

    public Map<String, TypeResolver> getTypeResolvers() {
        final Set<Class> ifaces = classOutputSchemaCreator.getInterfaces();
        final Map<String, TypeResolver> resolvers = Maps.newConcurrentMap();
        // interfaces
        ifaces.forEach(iface -> {
            final String ifaceName = Common.getName(iface);
            resolvers.put(ifaceName, resolver);
        });
        // unions
        unions.forEach(union -> {
            resolvers.put(union, resolver);
        });
        return resolvers;
    }

    public Collection<GraphQLOutputType> getTypes() {
        final Collection<GraphQLOutputType> types = outputMap.values();
        final Set<GraphQLOutputType> additionalTypes = classOutputSchemaCreator.getAllTypes();
        return Sets.union(Set.copyOf(types), additionalTypes);
    }

    public GraphQLOutputType createOutputType(final Type type) {
        Preconditions.checkNotNull(type, "Got null schema");

        if (outputMap.containsKey(type)) {
            return outputMap.get(type);
        }

        final Optional<GraphQLType> common = typeFactory.createType(type);
        if (common.isPresent()) {
            return (GraphQLOutputType) common.get();
        }
        // is type a class?
        if (type instanceof Class) {
            Class clz = (Class) type;
            // is reference?
            final Optional<Type> referencedClass = getReferencedClass(clz);
            if (referencedClass.isPresent()) {
                return createOutputType(referencedClass.get());
            }
            // union?
            if (GraphQLUnion.class.isAssignableFrom(clz)) {
                return createUnion(clz);
            }
            GraphQLOutputType out = classOutputSchemaCreator.create(clz);
            outputMap.putIfAbsent(type, out);
            return out;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() instanceof Class) {
                final Class ptRaw = (Class) pt.getRawType();
                if (Map.class.isAssignableFrom(ptRaw)) {
                    // MAP
                    return commonSchemaGenerator.createMapOutputSchema(
                            CommonSchemaGenerator.wrapNonNull(createOutputType(pt.getActualTypeArguments()[0])),
                            CommonSchemaGenerator.wrapNonNull(createOutputType(pt.getActualTypeArguments()[1]))
                    );
                } else if (Collection.class.isAssignableFrom(ptRaw)) {
                    // Collection
                    final GraphQLOutputType collectionMemberType = CommonSchemaGenerator.wrapNonNull(
                            createOutputType(pt.getActualTypeArguments()[0]));
                    final GraphQLOutputType list = GraphQLNonNull.nonNull(GraphQLList.list(collectionMemberType));
                    outputMap.putIfAbsent(type, list);
                    return list;
                } else if (GraphQLReference.class.isAssignableFrom(ptRaw)) {
                    final Type refType = pt.getActualTypeArguments()[0];
                    return createOutputType(refType);
                } else if (Flowable.class.isAssignableFrom(ptRaw)) {
                    final Type refType = pt.getActualTypeArguments()[0];
                    return createOutputType(refType);
                }
            }
        }
        throw new IllegalArgumentException("Unknown type " + type);
    }

    private GraphQLOutputType createUnion(final Class clz) {
        if (outputMap.containsKey(clz)) {
            return outputMap.get(clz);
        }
        if (references.containsKey(clz)) {
            return references.get(clz);
        }
        final String unionName = Common.getName(clz);
        references.putIfAbsent(clz, GraphQLTypeReference.typeRef(unionName));

        final Set<Type> outs = Common.getPossibleOutputs(clz);
        final GraphQLUnionType.Builder builder = GraphQLUnionType.newUnionType()
                .name(unionName);
        outs.stream().map(this::createOutputType)
                .forEach(out -> {
                    if (out instanceof GraphQLInterfaceType) {
                        final Set<String> impls = classOutputSchemaCreator.getInterfaceTypes().get(((GraphQLInterfaceType) out).getName());
                        impls.forEach(impl -> builder.possibleType(GraphQLTypeReference.typeRef(impl)));
                    } else if (out instanceof GraphQLObjectType) {
                        builder.possibleType((GraphQLObjectType) out);
                    } else if (out instanceof GraphQLTypeReference) {
                        builder.possibleType((GraphQLTypeReference) out);
                    } else {
                        throw new IllegalArgumentException("Unknown output type " + out.getClass());
                    }
                });
        unions.add(unionName);
        final GraphQLUnionType union = builder.build();
        outputMap.put(clz, union);
        return union;
    }

    private Optional<Type> getReferencedClass(final Class clz) {
        Class actual = clz;
        while (actual != Object.class && actual != null) {
            final Type[] interfaces = actual.getGenericInterfaces();
            for (Type i : interfaces) {
                if (i instanceof ParameterizedType) {
                    final ParameterizedType pt = (ParameterizedType) i;
                    if (pt.getRawType() == GraphQLReference.class) {
                        return Optional.of(pt.getActualTypeArguments()[0]);
                    }
                }
            }
            actual = actual.getSuperclass();
        }
        return Optional.empty();
    }

    private void checkResolvedClassIsNotInterface(Class referenceClass, TypeResolutionEnvironment environment) {
        if (!referenceClass.isInterface()) {
            return;
        }
        String errorMessage = "Unable to resolve type of: " +
                environment.getObject() +
                ", field: " +
                environment.getField() +
                " of type: " +
                environment.getFieldType() +
                " because it can't be interface: " +
                referenceClass +
                ". Maybe missing annotation: " +
                GraphQLArgument.class.getName() +
                "?";
        throw new IllegalStateException(errorMessage);
    }
}
