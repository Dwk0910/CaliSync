package org.neatore.caliback;

import lombok.RequiredArgsConstructor;

import org.json.JSONArray;
import org.json.JSONObject;

import org.neatore.caliback.controller.WebServiceController;
import org.neatore.caliback.object.MCPRequestClientInfo;
import org.neatore.caliback.object.PacketResponse;
import org.neatore.caliback.services.DBCService;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class McpTools {
    // 죄송합니다 너무 귀찮아서 어쩔 수 없었어요
    private final WebServiceController controller;
    private final DBCService dbcService;

    private final MCPRequestClientInfo mcpci;

    private static final String desc_date = "설정할 날짜를 입력합니다. yyyy.MM.dd. 의 형식으로 입력하여야 합니다. (예) 2025.07.24.";
    private static final String desc_schedules = "일정을 JSON배열 형식으로 입력합니다. 일정이 없다면 빈 배열([])을 입력하며, 일정이 있을 경우 배열 안에 JSON 객체({})가 들어갑니다. 들어가는 객체는 content라는 필드를 가집니다. content필드는 해당 일정의 이름을 의미합니다. 예를 들어, '학원 가기'와 '여행 가기' 일정을 넣으려면 [{\"content\":\"학원 가기\"}, {\"content\":\"여행 가기\"}] 처럼 값을 삽입합니다.";

    private String getResponseJSON(int status, String memo, JSONObject body) {
        JSONObject obj = new JSONObject();

        JSONObject headerjson = new JSONObject();
        headerjson.put("status", status);
        obj.put("header", headerjson);

        obj.put("memo", memo);
        obj.put("body", body);

        return obj.toString();
    }

    private String getResponseJSON(ResponseEntity<String> entity, String memo) {
        JSONObject body = new JSONObject(entity.getBody() == null ? "{}" : entity.getBody());
        return getResponseJSON(entity.getStatusCode().value(), memo, body);
    }

    @McpTool(name = "Get-schedules", description = "CaliSync에서 특정 달의 일정과 기념일/공휴일 정보를 모두 가져옵니다. 국경일(holi) > 공휴일(rest) > 기념일(anni) > 24절기(tfst) > 잡절(other)")
    public String getSchedules(
            @McpToolParam(description = "가져올 특정 년도를 yyyy 형식으로 작성하세요. 예를 들어 2025년 7월의 정보를 가져오려면 이 필드에는 2025를 작성합니다.") String year_,
            @McpToolParam(description = "가져올 특정 달을 M 형식으로 작성하세요. 예를 들어 2025년 7월의 정보를 가져오려면 이 필드에는 7을 작성하며, 12월의 정보를 가져온다면 12를 작성합니다.") String month_
    ) {
        ResponseEntity<String> responseEntity = controller.getMonthInfo(mcpci.getSessionToken(), year_, month_);
        return getResponseJSON(responseEntity, (new JSONObject(Objects.requireNonNull(responseEntity.getBody())).getJSONArray("schedules").isEmpty()) ? "해당 날짜에는 일정이 없습니다." : "");
    }

    @McpTool(name = "Set-schedules", description = "특정 날짜의 일정을 제공된 일정으로 변경합니다. 일정은 배열로 전달하며, 배열이 비어 있으면([]) 모든 일정이 지워지고, 3개의 JSON객체가 채워지면 일정이 3개가 되는 방식입니다. 이 도구는 일정 데이터베이스를 직접적으로 건드립니다.")
    public String setSchedules(
            @McpToolParam(description = desc_date) String date,
            @McpToolParam(description = desc_schedules) String schedules
    ) {
        JSONObject obj = new JSONObject();
        obj.put("date", date);
        obj.put("schedules", new JSONArray(schedules));

        return getResponseJSON(controller.setSchedules(mcpci.getSessionToken(), "", obj.toMap()), "");
    }

    @McpTool(name = "Add-schedules", description = "특정 날짜에 일정을 추가합니다. 등록된 기존 일정을 두고 새로운 일정을 추가할 때 사용합니다.")
    public String addSchedules(
            @McpToolParam(description = desc_date) String date,
            @McpToolParam(description = desc_schedules) String schedules
    ) {
        AtomicReference<Integer> succeed = new AtomicReference<>(0);

        new JSONArray(schedules).forEach((v) -> {
            JSONObject obj = new JSONObject();
            obj.put("method", "POST");

            JSONObject data = (JSONObject) v;
            data.put("date", date);
            obj.put("data", data);

            PacketResponse response = dbcService.process(obj);
            if (response.getResponseCode() == 201) succeed.set(succeed.get() + 1);
            else CaliBack.LOGGER.error("DBCService returned non-2xx code: ", response.getResponseCode());
        });

        return getResponseJSON(201, "작업 실행 완료. 성공한 작업 갯수 : %d개".formatted(succeed.get()), null);
    }
}
