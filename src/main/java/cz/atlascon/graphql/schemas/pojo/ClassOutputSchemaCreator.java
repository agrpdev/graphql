package cz.atlascon.graphql.schemas.pojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import cz.atlascon.graphql.GraphQLGenerator;
import cz.atlascon.graphql.common.Common;
import cz.atlascon.graphql.ng.GraphQLUnion;
import cz.atlascon.graphql.schemas.CommonSchemaGenerator;
import cz.atlascon.graphql.schemas.decorators.ArgumentsDecorator;
import cz.atlascon.graphql.schemas.decorators.DeprecationDecorator;
import cz.atlascon.graphql.schemas.decorators.DescriptionDecorator;
import cz.atlascon.graphql.schemas.decorators.NullableDecorator;
import graphql.schema.*;
import graphql.schema.GraphQLObjectType.Builder;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ClassOutputSchemaCreator {

    private final ConcurrentMap<Type, GraphQLNamedOutputType> outputMap = Maps.newConcurrentMap();
    private final Set<GraphQLOutputType> additionalTypes = Sets.newHashSet();
    private final ConcurrentMap<Class, GraphQLTypeReference> references = Maps.newConcurrentMap();
    private final Set<Class> interfaces = Sets.newConcurrentHashSet();
    private final Map<String, Set<String>> interfaceTypesMap = Maps.newConcurrentMap();
    private final Map<FieldCoordinates, Method> fetchers = Maps.newConcurrentMap();

    private final PojoInputSchemaGenerator inputSchemaGenerator;
    private final PojoOutputSchemaGenerator pojoOutputSchemaGenerator;

    public ClassOutputSchemaCreator(final PojoInputSchemaGenerator inputSchemaGenerator,
                                    final PojoOutputSchemaGenerator pojoOutputSchemaGenerator) {
        Preconditions.checkNotNull(inputSchemaGenerator);
        Preconditions.checkNotNull(pojoOutputSchemaGenerator);
        this.inputSchemaGenerator = inputSchemaGenerator;
        this.pojoOutputSchemaGenerator = pojoOutputSchemaGenerator;
    }

    public Set<Class> getInterfaces() {
        return Set.copyOf(interfaces);
    }

    public Set<GraphQLOutputType> getAdditionalTypes() {
        return Set.copyOf(additionalTypes);
    }

    public GraphQLOutputType create(final Class clz) {
        if (outputMap.containsKey(clz)) {
            return outputMap.get(clz);
        }
        if (references.containsKey(clz)) {
            return references.get(clz);
        }

        final String name = Common.getName(clz);
        references.putIfAbsent(clz, GraphQLTypeReference.typeRef(name));

        if (GraphQLUnion.class.isAssignableFrom(clz)) {
            return pojoOutputSchemaGenerator.createOutputType(clz);
        }

        final List<Method> methods = CommonSchemaGenerator.getGraphQLMethods(clz);
        final List<GraphQLFieldDefinition.Builder> fields = methods.stream()
                .map(this::processMethod)
                .collect(Collectors.toList());
        Preconditions.checkArgument(!fields.isEmpty(), "No fields defined in " + clz);

        final GraphQLNamedOutputType outputType;
        if (clz.isInterface()) {
            final GraphQLInterfaceType.Builder ifaceBuilder = GraphQLInterfaceType.newInterface().name(name);
            fields.forEach(ifaceBuilder::field);
            final GraphQLInterfaceType iface = ifaceBuilder.build();
            interfaces.add(clz);
            outputType = iface;
        } else {
            final Builder builder = GraphQLObjectType.newObject().name(name);
            fields.forEach(builder::field);
            methods.forEach(m -> this.fetchers.put(FieldCoordinates.coordinates(name, CommonSchemaGenerator.getFieldName(m)), m));
            final List<GraphQLInterfaceType> interfaces = getInterfaces(clz);
            interfaces.forEach(builder::withInterface);
            outputType = builder.build();
            interfaces.forEach(i -> {
                interfaceTypesMap.computeIfAbsent(i.getName(), n -> Sets.newConcurrentHashSet()).add(outputType.getName());
            });
        }
        outputMap.putIfAbsent(clz, outputType);
        return outputType;
    }

    public Map<FieldCoordinates, Method> getFetchers() {
        return fetchers;
    }

    public Map<String, Set<String>> getInterfaceTypes() {
        return interfaceTypesMap;
    }


    private List<GraphQLInterfaceType> getInterfaces(final Class clz) {
        final List<GraphQLInterfaceType> ifaces = Lists.newArrayList();
        // all real interfaces
        final List<Class<?>> interfaces = ClassUtils.getAllInterfaces(clz);
        for (Class iface : interfaces) {
            final GraphQLInterfaceType ifc = (GraphQLInterfaceType) create(iface);
            ifaces.add(ifc);
        }
        return ifaces;
    }

    private GraphQLFieldDefinition.Builder processMethod(final Method m) {
        final GraphQLOutputType fieldType;
        if (m.getGenericReturnType() != null && m.getGenericReturnType() instanceof ParameterizedType) {
            // returning generic type?
            fieldType = pojoOutputSchemaGenerator.createOutputType(m.getGenericReturnType());
        } else {
            // or simple type?
            fieldType = pojoOutputSchemaGenerator.createOutputType(m.getReturnType());
        }
        final GraphQLFieldDefinition.Builder fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
                .name(CommonSchemaGenerator.getFieldName(m));

        // apply decorators
        new NullableDecorator(fieldType).decorate(fieldDefinition, m);
        new ArgumentsDecorator(inputSchemaGenerator).decorate(fieldDefinition, m);
        new DescriptionDecorator().decorate(fieldDefinition, m);
        new DeprecationDecorator().decorate(fieldDefinition, m);

        return fieldDefinition;
    }


    public Set<GraphQLOutputType> getAllTypes() {
        return Set.copyOf(Sets.union(additionalTypes, Set.copyOf(outputMap.values())));
    }
}
