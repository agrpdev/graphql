package cz.atlascon.graphql;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class OperationMessage {

    private ObjectNode payload;
    private String id;
    private String type;

    public static final String GQL_CONNECTION_INIT = "connection_init"; // client->server
    public static final String GQL_CONNECTION_ACK = "connection_ack"; // server->client
    public static final String GQL_CONNECTION_ERROR = "connection_error"; // server->client
    public static final String GQL_CONNECTION_KEEP_ALIVE = "ka"; // server->client
    public static final String GQL_CONNECTION_TERMINATE = "connection_terminate"; // client->server


    public static final String GQL_START = "start";
    public static final String GQL_DATA = "data";
    public static final String GQL_ERROR = "error";
    public static final String GQL_COMPLETE = "complete";
    public static final String GQL_STOP = "stop";

    public OperationMessage() {
    }

    public OperationMessage(String type) {
        this.type = type;
    }

    public OperationMessage(ObjectNode payload, String id, String type) {
        this.payload = payload;
        this.id = id;
        this.type = type;
    }

    public OperationMessage(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public ObjectNode getPayload() {
        return payload;
    }

    public void setPayload(ObjectNode payload) {
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}