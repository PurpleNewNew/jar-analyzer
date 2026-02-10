/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.action;

import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.MainForm;

import java.awt.Desktop;
import java.net.URI;

public class ApiAction {
    public static void register() {
        MainForm instance = MainForm.getInstance();
        if (instance == null) {
            return;
        }

        instance.getBindText().setText(GlobalOptions.getServerConfig().getBind());
        instance.getAuthText().setText(GlobalOptions.getServerConfig().isAuth() ? "TRUE" : "FALSE");
        instance.getPortText().setText(String.valueOf(GlobalOptions.getServerConfig().getPort()));
        String token = GlobalOptions.getServerConfig().getToken();
        if (token != null && !token.isEmpty()) {
            int length = token.length();
            if (length > 2) {
                char[] chars = token.toCharArray();
                for (int i = 1; i < length - 1; i++) {
                    chars[i] = '*';
                }
                token = new String(chars);
            } else {
                char[] masked = new char[length];
                java.util.Arrays.fill(masked, '*');
                token = new String(masked);
            }
        }
        instance.getTokenText().setText(token);

        instance.getApiDocBtn().addActionListener(e -> openDoc("doc/README-api.md"));
        instance.getMcpDocBtn().addActionListener(e -> openDoc("mcp-doc/README.md"));
        instance.getN8nDocBtn().addActionListener(e -> openDoc("n8n-doc/README.md"));
    }

    private static void openDoc(String path) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/jar-analyzer/jar-analyzer/blob/master/" + path));
        } catch (Exception ignored) {
        }
    }
}

