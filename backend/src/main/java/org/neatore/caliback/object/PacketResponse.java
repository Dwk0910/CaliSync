package org.neatore.caliback.object;

import org.jetbrains.annotations.Nullable;

public class PacketResponse {
    private Object responseBody = null;
    private String requestId = null;
    private int responseCode;

    public PacketResponse(int responseCode, Object responseBody, @Nullable String... requestId) {
        this.responseBody = responseBody;
        this.responseCode = responseCode;
        this.requestId = (requestId.length == 0) ? null : requestId[0];
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void setResponseBody(Object responseBody) {
        this.responseBody = responseBody;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResponseBody() {
        return responseBody;
    }

    public String getRequestId() {
        return requestId;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
