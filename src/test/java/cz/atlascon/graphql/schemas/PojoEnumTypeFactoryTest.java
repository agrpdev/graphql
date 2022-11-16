package cz.atlascon.graphql.schemas;

import cz.atlascon.graphql.schemas.types.EnumTypeFactory;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class PojoEnumTypeFactoryTest {

    enum Bla {
        BLA,
        BLABLA,
        @Deprecated BLABLABLA
    }

    @Test
    public void shouldGenerateCorrectEnumSchema() {
        final EnumTypeFactory enumTypeFactory = new EnumTypeFactory();
        final Optional<GraphQLType> typeOpt = enumTypeFactory.createType(Bla.class);
        final GraphQLType type = typeOpt.get();
        Assert.assertEquals(type.getClass(), GraphQLEnumType.class);
        Assert.assertEquals(List.of("BLA", "BLABLA", "BLABLABLA"),
            ((GraphQLEnumType) type).getValues()
                .stream()
                .map(GraphQLEnumValueDefinition::getName)
                .collect(Collectors.toList()));
        Assert.assertTrue(((GraphQLEnumType) type).getValue("BLABLABLA").isDeprecated());
    }

}