// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.proxy;

import android.content.Context;
import android.content.SharedPreferences;

import org.chromium.base.ContextUtils;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.NativeMethods;

/**
 * Менеджер прокси для управления настройками прокси
 */
@JNINamespace("chrome::android")
public class ProxyManager {
    private static final String PREF_NAME = "proxy_settings";
    private static final String PREF_ENABLED = "proxy_enabled";
    private static final String PREF_TYPE = "proxy_type";
    private static final String PREF_HOST = "proxy_host";
    private static final String PREF_PORT = "proxy_port";
    private static final String PREF_USERNAME = "proxy_username";
    private static final String PREF_PASSWORD = "proxy_password";

    private static ProxyManager sInstance;
    private ProxyConfig mCurrentConfig;

    private ProxyManager() {
        mCurrentConfig = loadConfig();
    }

    public static ProxyManager getInstance() {
        if (sInstance == null) {
            sInstance = new ProxyManager();
        }
        return sInstance;
    }

    public ProxyConfig getCurrentConfig() {
        return mCurrentConfig;
    }

    public void setProxyConfig(ProxyConfig config) {
        mCurrentConfig = config;
        saveConfig(config);
        applyProxyConfig();
    }

    public void enableProxy(boolean enabled) {
        mCurrentConfig.setEnabled(enabled);
        saveConfig(mCurrentConfig);
        applyProxyConfig();
    }

    private void applyProxyConfig() {
        ProxyManagerJni.get().applyProxyConfig(
            mCurrentConfig.isEnabled(),
            mCurrentConfig.getType(),
            mCurrentConfig.getHost(),
            mCurrentConfig.getPort(),
            mCurrentConfig.getUsername(),
            mCurrentConfig.getPassword()
        );
    }

    private ProxyConfig loadConfig() {
        SharedPreferences prefs = ContextUtils.getApplicationContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        ProxyConfig config = new ProxyConfig();
        config.setEnabled(prefs.getBoolean(PREF_ENABLED, false));
        config.setType(prefs.getInt(PREF_TYPE, ProxyConfig.ProxyType.DIRECT));
        config.setHost(prefs.getString(PREF_HOST, ""));
        config.setPort(prefs.getInt(PREF_PORT, 8080));
        config.setUsername(prefs.getString(PREF_USERNAME, ""));
        config.setPassword(prefs.getString(PREF_PASSWORD, ""));
        
        return config;
    }

    private void saveConfig(ProxyConfig config) {
        SharedPreferences.Editor editor = ContextUtils.getApplicationContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit();
        
        editor.putBoolean(PREF_ENABLED, config.isEnabled());
        editor.putInt(PREF_TYPE, config.getType());
        editor.putString(PREF_HOST, config.getHost());
        editor.putInt(PREF_PORT, config.getPort());
        editor.putString(PREF_USERNAME, config.getUsername());
        editor.putString(PREF_PASSWORD, config.getPassword());
        editor.apply();
    }

    @CalledByNative
    private static String getCurrentProxyString() {
        return getInstance().getCurrentConfig().getProxyString();
    }

    @NativeMethods
    interface Natives {
        void applyProxyConfig(boolean enabled, int type, String host, int port, 
                            String username, String password);
    }
}
