package org.neatore.calisync.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

import org.neatore.calisync.CaliSync;

public class NotifySystem {
    public interface CUser32 extends StdCallLibrary {
        int RTN_OK = 1;
        int RTN_CANCEL = 2;
        int RTN_RETRY = 4;
        int RTN_TRYAGAIN = 10;

        int ICO_ERROR = 0x00000010;
        int ICO_WARN  = 0x00000030;

        int BTN_OK = 0x00000000; // 확인
        int BTN_OK_CANCEL = 0x00000001; // 확인, 취소
        int BTN_RETRY_CANCEL = 0x00000005; // 다시 시도, 취소

        CUser32 INSTANCE = Native.load("user32", CUser32.class);
        int MessageBoxW(WinDef.HWND hWnd, WString IpText, WString IpCaption, int uType);
    }

    private final File icon;

    public NotifySystem() {
        File icon = new File(System.getProperty("java.io.tmpdir"), "calisync_icon.png");
        try (InputStream is = CaliSync.class.getResourceAsStream("/icon.png")) {
            if (is == null) throw new Exception();
            Files.copy(is, icon.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            CaliSync.LOGGER.error("[NotifySystem] Failed to load icon resource.");
        }

        this.icon = icon;
        CaliSync.LOGGER.info("[NotifySystem] Registration completed.");
    }

    public int openErrorWindow(String title, String description) {
        return CUser32.INSTANCE.MessageBoxW(null, new WString(description), new WString(title), CUser32.ICO_ERROR | CUser32.BTN_RETRY_CANCEL);
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
            title, description, icon.getAbsolutePath());

            new ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-Command", script).start();
        } catch (IOException e) {
            CaliSync.LOGGER.error("", e);
        }
    }
}

