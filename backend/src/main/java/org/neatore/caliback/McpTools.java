package org.neatore.caliback;

import lombok.RequiredArgsConstructor;

import org.neatore.caliback.services.DBCService;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class McpTools {
    private final DBCService dbcService;

    @McpTool(description = "Calisync에서 특정 날짜의 일정을 모두 가져옵니다.")
    public String getEvents(@McpToolParam(description = "가져올 일정의 날짜를 yyyy.MM.dd.의 형식으로 작성") String date) {
        return "일정이 없습니다.";
    }
}
