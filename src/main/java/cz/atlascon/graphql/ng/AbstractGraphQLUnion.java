package cz.atlascon.graphql.ng;

public abstract class AbstractGraphQLUnion implements GraphQLUnion {

    private final Object value;

    protected AbstractGraphQLUnion(final Object value) {
        this.value = value;
    }

    /**
     * Get object value
     *
     * @return
     */
    public Object getValue() {
        return value;
    }

}
