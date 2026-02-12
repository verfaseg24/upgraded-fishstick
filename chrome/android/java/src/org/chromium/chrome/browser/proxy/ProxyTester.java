// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.proxy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

/**
 * Утилита для тестирования прокси подключения
 */
public class ProxyTester {
    
    public interface TestCallback {
        void onTestComplete(boolean success, String message);
    }
    
    public static void testProxy(ProxyConfig config, TestCallback callback) {
        new Thread(() -> {
            try {
                if (!config.isEnabled() || config.getType() == ProxyConfig.ProxyType.DIRECT) {
                    callback.onTestComplete(false, "Прокси не настроен");
                    return;
                }
                
                Proxy.Type proxyType;
                switch (config.getType()) {
                    case ProxyConfig.ProxyType.HTTP:
                    case ProxyConfig.ProxyType.HTTPS:
                        proxyType = Proxy.Type.HTTP;
                        break;
                    case ProxyConfig.ProxyType.SOCKS4:
                    case ProxyConfig.ProxyType.SOCKS5:
                        proxyType = Proxy.Type.SOCKS;
                        break;
                    default:
                        proxyType = Proxy.Type.DIRECT;
                }
                
                Proxy proxy = new Proxy(proxyType, 
                    new InetSocketAddress(config.getHost(), config.getPort()));
                
                URL url = new URL("https://www.google.com");
                HttpURLConnection connection = 
                    (HttpURLConnection) url.openConnection(proxy);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod("HEAD");
                
                long startTime = System.currentTimeMillis();
                int responseCode = connection.getResponseCode();
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                
                connection.disconnect();
                
                if (responseCode == 200) {
                    callback.onTestComplete(true, 
                        String.format("✓ Прокси работает (время ответа: %d мс)", responseTime));
                } else {
                    callback.onTestComplete(false, 
                        String.format("✗ Ошибка: HTTP код %d", responseCode));
                }
                
            } catch (IOException e) {
                String errorMsg = e.getMessage();
                if (errorMsg.contains("timeout")) {
                    callback.onTestComplete(false, "✗ Превышено время ожидания");
                } else if (errorMsg.contains("refused")) {
                    callback.onTestComplete(false, "✗ Соединение отклонено");
                } else if (errorMsg.contains("host")) {
                    callback.onTestComplete(false, "✗ Не удалось найти хост");
                } else {
                    callback.onTestComplete(false, 
                        "✗ Ошибка: " + errorMsg);
                }
            } catch (Exception e) {
                callback.onTestComplete(false, 
                    "✗ Неизвестная ошибка: " + e.getMessage());
            }
        }).start();
    }
    
    public static void testCurrentProxy(TestCallback callback) {
        ProxyConfig config = ProxyManager.getInstance().getCurrentConfig();
        testProxy(config, callback);
    }
}
