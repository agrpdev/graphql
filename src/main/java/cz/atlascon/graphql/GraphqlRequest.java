package cz.atlascon.graphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tomas on 1.6.17.
 */
public class GraphqlRequest {

    private static final TypeReference<HashMap<String, Object>> TYPE_REF = new TypeReference<HashMap<String, Object>>() {
    };
    private final ObjectMapper om = new ObjectMapper();
    private final Map<String, Object> vars;
    private final String query;
    private final String operationName;

    public GraphqlRequest(Map qm) throws IOException {
        final Object variables = qm.get("variables");
        // sometimes, variables are just escaped JSON string (like for example from GraphqlFeen plugin fuck fuck)
        this.vars = variables instanceof Map ? (Map) variables : parseVars((String) variables);
        final String rawQuery = (String) qm.get("query");
        this.query = rawQuery.replaceAll("\\\\n", "");
        this.operationName = (String) qm.get("operationName");
    }

    private Map<String, Object> parseVars(final String variables) throws IOException {
        if (variables != null && !variables.isBlank()) {
            final Object o = om.readValue(variables, TYPE_REF);
            return (Map) o;
        }
        return Map.of();
    }

    public String getOperationName() {
        return operationName;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    @Override
    public String toString() {
        return "GraphqlRequest{" +
                "om=" + om +
                ", vars=" + vars +
                ", query='" + query + '\'' +
                ", operationName='" + operationName + '\'' +
                '}';
    }
}
