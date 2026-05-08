package org.neatore.caliback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import org.json.JSONArray;

import org.neatore.caliback.services.LogSenderService;

public class SocketLogAppender extends AppenderBase<ILoggingEvent> {
    private boolean isSenderServiceAvailable = false;
    private final JSONArray logBuffer = new JSONArray();

    private LogSenderService logSenderService;

    private void enableSenderService() {
        this.isSenderServiceAvailable = true;
        logSenderService.logAll(this.logBuffer);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        String msg = eventObject.getFormattedMessage();
        if (!isSenderServiceAvailable) {
            logBuffer.put(msg);

            LogSenderService instance = LogSenderService.getInstance();
            if (instance != null) {
                this.logSenderService = instance;
                this.enableSenderService();
            }
        } else logSenderService.log(msg);
    }
}
