// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.proxy;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Конфигурация прокси для браузера
 */
public class ProxyConfig {
    @IntDef({ProxyType.DIRECT, ProxyType.HTTP, ProxyType.HTTPS, ProxyType.SOCKS4, ProxyType.SOCKS5})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProxyType {
        int DIRECT = 0;
        int HTTP = 1;
        int HTTPS = 2;
        int SOCKS4 = 3;
        int SOCKS5 = 4;
    }

    private @ProxyType int mType;
    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private boolean mEnabled;

    public ProxyConfig() {
        mType = ProxyType.DIRECT;
        mEnabled = false;
    }

    public ProxyConfig(@ProxyType int type, String host, int port) {
        mType = type;
        mHost = host;
        mPort = port;
        mEnabled = true;
    }

    public @ProxyType int getType() {
        return mType;
    }

    public void setType(@ProxyType int type) {
        mType = type;
    }

    public String getHost() {
        return mHost;
    }

    public void setHost(String host) {
        mHost = host;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int port) {
        mPort = port;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public String getProxyString() {
        if (!mEnabled || mType == ProxyType.DIRECT) {
            return "direct://";
        }

        StringBuilder sb = new StringBuilder();
        switch (mType) {
            case ProxyType.HTTP:
                sb.append("http://");
                break;
            case ProxyType.HTTPS:
                sb.append("https://");
                break;
            case ProxyType.SOCKS4:
                sb.append("socks4://");
                break;
            case ProxyType.SOCKS5:
                sb.append("socks5://");
                break;
        }

        if (mUsername != null && !mUsername.isEmpty()) {
            sb.append(mUsername);
            if (mPassword != null && !mPassword.isEmpty()) {
                sb.append(":").append(mPassword);
            }
            sb.append("@");
        }

        sb.append(mHost).append(":").append(mPort);
        return sb.toString();
    }
}
