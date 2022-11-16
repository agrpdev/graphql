package cz.atlascon.graphql.schemas.types;

import graphql.schema.GraphQLType;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class DatesTypeFactory implements GraphQLTypeFactory {

    @Override
    public Optional<GraphQLType> createType(final Type type) {
        if (type == LocalDate.class) {
            return Optional.of(MoreScalars.GraphQLLocalDate);
        }
        if (type == LocalDateTime.class) {
            return Optional.of(MoreScalars.GraphQLLocalDateTime);
        }
        if (type == Instant.class) {
            return Optional.of(MoreScalars.GraphQLInstant);
        }
        if (type == UUID.class) {
            return Optional.of(MoreScalars.GraphQLUuid);
        }
        return Optional.empty();
    }

}
