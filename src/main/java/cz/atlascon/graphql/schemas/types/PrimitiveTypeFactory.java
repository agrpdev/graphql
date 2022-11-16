package cz.atlascon.graphql.schemas.types;

import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import static graphql.Scalars.*;
import static graphql.scalars.java.JavaPrimitives.*;

public class PrimitiveTypeFactory implements GraphQLTypeFactory {

    @Override
    public Optional<GraphQLType> createType(final Type type) {
        return Optional.ofNullable(doCreateType(type));
    }

    private GraphQLType doCreateType(final Type type) {
        if (type == Integer.class) {
            return GraphQLInt;
        }
        if (type == int.class) {
            return GraphQLNonNull.nonNull(GraphQLInt);
        }
        if (type == Long.class) {
            return GraphQLLong;
        }
        if (type == long.class) {
            return GraphQLNonNull.nonNull(GraphQLLong);
        }
        if (type == Float.class) {
            return GraphQLFloat;
        }
        if (type == float.class) {
            return GraphQLNonNull.nonNull(GraphQLFloat);
        }
        if (type == Double.class) {
            return GraphQLFloat;
        }
        if (type == double.class) {
            return GraphQLNonNull.nonNull(GraphQLFloat);
        }
        if (type == String.class) {
            return GraphQLString;
        }
        if (type == Boolean.class) {
            return GraphQLBoolean;
        }
        if (type == boolean.class) {
            return GraphQLNonNull.nonNull(GraphQLBoolean);
        }
        if (type == Short.class) {
            return GraphQLShort;
        }
        if (type == short.class) {
            return GraphQLNonNull.nonNull(GraphQLShort);
        }
        if (type == Character.class) {
            return GraphQLChar;
        }
        if (type == char.class) {
            return GraphQLNonNull.nonNull(GraphQLChar);
        }
        if (type == Byte.class) {
            return GraphQLByte;
        }
        if (type == byte.class) {
            return GraphQLNonNull.nonNull(GraphQLByte);
        }
        if (type == BigDecimal.class) {
            return GraphQLBigDecimal;
        }
        if (type == BigInteger.class) {
            return GraphQLBigInteger;
        }
        // not supported -> return null
        return null;
    }
}
