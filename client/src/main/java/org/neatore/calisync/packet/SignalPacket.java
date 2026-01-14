package org.neatore.calisync.packet;

import org.json.JSONObject;

import java.util.Map;

public final class SignalPacket {
    public enum Method { GET, UPDATE_INFO, HARD_UPDATE, UPDATE, POST, DELETE }

    private final Method type;
    private final Map<String, Object> data;

    public SignalPacket(Method type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
    }

    public JSONObject toJSONObject() {
        return new JSONObject()
                .put("method", type.name())
                .put("data", new JSONObject(data));
    }

    @Override
    public String toString() {
        return toJSONObject().toString();
    }
}
