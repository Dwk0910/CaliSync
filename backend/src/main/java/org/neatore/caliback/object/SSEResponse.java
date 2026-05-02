package org.neatore.caliback.object;

import lombok.Getter;
import lombok.Setter;

import org.json.JSONArray;
import org.json.JSONObject;

@Getter
@Setter
public class SSEResponse {
    private int code;
    private Object body;

    public SSEResponse(int code, Object body) {
        this.code  = code;
        this.body = body;
    }

    @Override
    public String toString() {
        JSONObject response = new JSONObject();
        response.put("code", code);
        response.put("body", new JSONArray(body));

        return response.toString();
    }
}
