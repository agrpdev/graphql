package cz.atlascon.graphql.schemas.types;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class MoreScalars {

    public static final GraphQLScalarType GraphQLLocalDate = GraphQLScalarType.newScalar()
            .name("LocalDate")
            .description("Local date in ISO-8601 - YYYY-mm-dd - eg. 2019-12-24")
            .coercing(new Coercing<LocalDate, String>() {
                @Override
                public String serialize(final Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult == null) {
                        return null;
                    }
                    if (dataFetcherResult instanceof LocalDate) {
                        return ((LocalDate) dataFetcherResult).format(DateTimeFormatter.ISO_DATE);
                    }
                    throw new IllegalArgumentException("Unknown class " + dataFetcherResult.getClass());
                }

                @Override
                public LocalDate parseValue(final Object input) throws CoercingParseValueException {
                    if (input == null) {
                        return null;
                    }
                    if (!(input instanceof String)) {
                        throw new CoercingParseValueException("Unable to parse " + input + " to LocalDate");
                    }
                    return LocalDate.parse((String) input, DateTimeFormatter.ISO_DATE);
                }

                @Override
                public LocalDate parseLiteral(final Object input) throws CoercingParseLiteralException {
                    if (!(input instanceof StringValue)) {
                        throw new CoercingParseLiteralException("Invalid value " + input + ", expecting StringValue");
                    }
                    return parseValue(((StringValue) input).getValue());
                }
            })
            .build();

    public static final GraphQLScalarType GraphQLLocalDateTime = GraphQLScalarType.newScalar()
            .name("LocalDateTime")
            .description("Local date and time in ISO-8601 - YYYY-mm-ddThh:mm:ss - eg. 2019-12-24T10:15:30")
            .coercing(new Coercing<LocalDateTime, String>() {
                @Override
                public String serialize(final Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult == null) {
                        return null;
                    }
                    if (dataFetcherResult instanceof LocalDateTime) {
                        return ((LocalDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    }
                    throw new IllegalArgumentException("Unknown class " + dataFetcherResult.getClass());
                }

                @Override
                public LocalDateTime parseValue(final Object input) throws CoercingParseValueException {
                    if (input == null) {
                        return null;
                    }
                    if (!(input instanceof String)) {
                        throw new CoercingParseValueException("Unable to parse " + input + " to LocalDateTime");
                    }
                    return LocalDateTime.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }

                @Override
                public LocalDateTime parseLiteral(final Object input) throws CoercingParseLiteralException {
                    if (!(input instanceof StringValue)) {
                        throw new CoercingParseLiteralException("Invalid value " + input + ", expecting StringValue");
                    }
                    return parseValue(((StringValue) input).getValue());
                }
            })
            .build();

    public static final GraphQLScalarType GraphQLInstant = GraphQLScalarType.newScalar()
            .name("Instant")
            .description("Linux epoch instant in milliseconds")
            .coercing(new Coercing<Instant, Long>() {
                @Override
                public Long serialize(final Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult == null) {
                        return null;
                    }
                    if (dataFetcherResult instanceof Instant) {
                        return ((Instant) dataFetcherResult).toEpochMilli();
                    }
                    throw new IllegalArgumentException("Unknown class " + dataFetcherResult.getClass());
                }

                @Override
                public Instant parseValue(final Object input) throws CoercingParseValueException {
                    if (input == null) {
                        return null;
                    }
                    if (!(input instanceof Long)) {
                        throw new CoercingParseValueException("Unable to parse " + input + " to Instant");
                    }
                    return Instant.ofEpochMilli((Long) input);
                }

                @Override
                public Instant parseLiteral(final Object input) throws CoercingParseLiteralException {
                    if (!(input instanceof IntValue)) {
                        throw new CoercingParseLiteralException("Invalid value " + input + ", expecting IntValue");
                    }
                    return parseValue(((IntValue) input).getValue().longValue());
                }
            })
            .build();

    public static final GraphQLScalarType GraphQLUuid = GraphQLScalarType.newScalar()
            .name("UUID")
            .description("UUID represented as string")
            .coercing(new Coercing<UUID, String>() {
                @Override
                public String serialize(final Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult == null) {
                        return null;
                    }
                    if (dataFetcherResult instanceof UUID) {
                        return dataFetcherResult.toString();
                    }
                    throw new IllegalArgumentException("Unknown class " + dataFetcherResult.getClass());
                }

                @Override
                public UUID parseValue(final Object input) throws CoercingParseValueException {
                    if (input == null) {
                        return null;
                    }
                    if (!(input instanceof String)) {
                        throw new CoercingParseValueException("Unable to parse " + input + " to ByteArray");
                    }
                    return UUID.fromString((String) input);
                }

                @Override
                public UUID parseLiteral(final Object input) throws CoercingParseLiteralException {
                    if (!(input instanceof StringValue)) {
                        throw new CoercingParseLiteralException("Invalid value " + input + ", expecting StringValue");
                    }
                    return parseValue(((StringValue) input).getValue());
                }
            })
            .build();

    private MoreScalars() throws IllegalAccessException {
        throw new IllegalAccessException("not invokable");
    }


}
