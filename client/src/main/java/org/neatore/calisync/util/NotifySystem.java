package org.neatore.calisync.util;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

import org.neatore.calisync.CaliSync;

public class NotifySystem {
    private final String iconPath;

    public NotifySystem() {
        // 맨 앞의 슬래시(/) 제거, 모든 슬래시를 File.separator(범OS 구분자)로 변경
        this.iconPath = Objects.requireNonNull(CaliSync.class.getResource("/icon.png")).getFile()
                .substring(1)
                .replace("/", File.separator);
        CaliSync.LOGGER.info("[NotifySystem] Registration completed.");
    }

    public void openErrorWindow(String title, String description) {
        CUser32.INSTANCE.MessageBoxW(null, new WString(description), new WString(title), 0x0000010);
    }

    public void notify(String title, String description) {
        try {
            String script = String.format(
                "$title = '%s'; $msg = '%s'; $icon = '%s'; " +
                "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null; " +
                "$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastImageAndText02); " +
                "$images = $template.GetElementsByTagName('image'); " +
                "$images.Item(0).Attributes.GetNamedItem('src').AppendChild($template.CreateTextNode($icon)) | Out-Null; " +
                "$textNodes = $template.GetElementsByTagName('text'); " +
                "$textNodes.Item(0).AppendChild($template.CreateTextNode($title)) | Out-Null; " +
                "$textNodes.Item(1).AppendChild($template.CreateTextNode($msg)) | Out-Null; " +
                "$toast = [Windows.UI.Notifications.ToastNotification]::new($template); " +
                "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('CaliSync').Show($toast);",
            title, description, iconPath);

            new ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-Command", script).start();
        } catch (IOException e) {
            CaliSync.LOGGER.error("", e);
        }
    }
}

interface CUser32 extends StdCallLibrary {
    CUser32 INSTANCE = Native.load("user32", CUser32.class);
    void MessageBoxW(WinDef.HWND hWnd, WString IpText, WString IpCaption, int uType);
}
