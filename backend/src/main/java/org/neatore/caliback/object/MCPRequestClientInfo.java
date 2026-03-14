package org.neatore.caliback.object;

import lombok.Data;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Data
@Component
@RequestScope
public class MCPRequestClientInfo {
    private String sessionToken;
}
