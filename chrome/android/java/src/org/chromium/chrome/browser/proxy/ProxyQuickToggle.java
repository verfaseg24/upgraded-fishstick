// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.proxy;

import android.content.Context;
import android.widget.Toast;

/**
 * Утилита для быстрого переключения прокси
 */
public class ProxyQuickToggle {
    
    public static void toggleProxy(Context context) {
        ProxyManager manager = ProxyManager.getInstance();
        ProxyConfig config = manager.getCurrentConfig();
        
        boolean newState = !config.isEnabled();
        manager.enableProxy(newState);
        
        String message = newState ? "Прокси включен" : "Прокси выключен";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    public static boolean isProxyEnabled() {
        return ProxyManager.getInstance().getCurrentConfig().isEnabled();
    }
    
    public static String getProxyStatus() {
        ProxyConfig config = ProxyManager.getInstance().getCurrentConfig();
        if (!config.isEnabled()) {
            return "Прокси выключен";
        }
        
        String type = getProxyTypeName(config.getType());
        return String.format("Прокси: %s (%s:%d)", 
            type, config.getHost(), config.getPort());
    }
    
    private static String getProxyTypeName(int type) {
        switch (type) {
            case ProxyConfig.ProxyType.HTTP:
                return "HTTP";
            case ProxyConfig.ProxyType.HTTPS:
                return "HTTPS";
            case ProxyConfig.ProxyType.SOCKS4:
                return "SOCKS4";
            case ProxyConfig.ProxyType.SOCKS5:
                return "SOCKS5";
            default:
                return "Direct";
        }
    }
}
