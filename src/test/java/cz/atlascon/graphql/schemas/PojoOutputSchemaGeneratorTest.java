package cz.atlascon.graphql.schemas;

import com.google.common.collect.ImmutableMap;
import cz.atlascon.graphql.ng.GraphQLDesc;
import cz.atlascon.graphql.ng.GraphQLField;
import cz.atlascon.graphql.ng.GraphQLParam;
import cz.atlascon.graphql.ng.GraphQLReference;
import org.junit.Ignore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

@Ignore
public class PojoOutputSchemaGeneratorTest {

    @GraphQLDesc("Describing ID of item")
    public interface ItemId {

        @GraphQLField(value = "thisIsIdField")
        int getId();

    }

    public static class BaseItem implements ItemId {

        private final int id;

        private BaseItem(final int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        enum Wot {
            WOT, WAT, WET
        }

        final Wot gg = Wot.WOT;
        final int intField = 123;

        @GraphQLField
        public boolean isNormal(@GraphQLParam("negate") boolean negate) {
            return negate ? false : true;
        }

        @GraphQLField
        public Wot getGg() {
            return gg;
        }

        @GraphQLField
        @GraphQLDesc("describing size of int")
        public int getIntField() {
            return intField;
        }

    }

    public static class ExtendedItem extends BaseItem {

        public ExtendedItem(int id) {
            super(id);
        }

        @GraphQLField
        public GraphQLReference<ExtendedItem> getExtendedItem(
//            @GraphQLParam("input") String input
        ) {
            return new ExtendedItemRef(889);
        }

        @GraphQLField
        public Map<String, GraphQLReference<ExtendedItem>> getExtendedItemMap(
                @GraphQLParam("input") String input
        ) {
            return ImmutableMap.of(
                    "a", new ExtendedItemRef(123),
                    "b", new ExtendedItemRef(456),
                    "c", new ExtendedItemRef(789)
            );
        }

        @Nonnull
        @GraphQLField
        public Integer getIntegerField() {
            return 456;
        }

        @GraphQLField
        public Integer getNullableIntegerField() {
            return 7891;
        }

        @GraphQLField
        public Map<Integer, Map<String, String>> getComplexMap() {
            return ImmutableMap.of(
                    1, ImmutableMap.of("a", "aa", "b", "bb"),
                    2, ImmutableMap.of("c", "cc", "d", "cc")
            );
        }

        @GraphQLField
        public Map<Integer, Map<String, String>> getComplexMap2() {
            return ImmutableMap.of(
                    1, ImmutableMap.of("aa", "aaaa", "bb", "bbbb"),
                    2, ImmutableMap.of("cc", "cccc", "dd", "cccc")
            );
        }
    }

    private static class ExtendedItemReference implements GraphQLReference<ExtendedItem> {

        private final int id;

        private ExtendedItemReference(final int id) {
            this.id = id;
        }

    }

    private static class Outer {

        @GraphQLField
        public List<GraphQLReference<ExtendedItem>> getItems() {
            return List.of(
                    new ExtendedItemReference(1),
                    new ExtendedItemReference(2),
                    new ExtendedItemReference(3)
            );
        }

    }

}