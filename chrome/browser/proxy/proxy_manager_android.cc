// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "chrome/browser/proxy/proxy_manager_android.h"

#include "base/android/jni_string.h"
#include "chrome/android/chrome_jni_headers/ProxyManager_jni.h"
#include "content/public/browser/browser_thread.h"
#include "net/base/proxy_server.h"
#include "net/proxy_resolution/proxy_config.h"
#include "net/proxy_resolution/proxy_config_service.h"

namespace chrome {
namespace android {

namespace {

net::ProxyServer::Scheme ConvertProxyType(int type) {
  switch (type) {
    case 1:  // HTTP
      return net::ProxyServer::SCHEME_HTTP;
    case 2:  // HTTPS
      return net::ProxyServer::SCHEME_HTTPS;
    case 3:  // SOCKS4
      return net::ProxyServer::SCHEME_SOCKS4;
    case 4:  // SOCKS5
      return net::ProxyServer::SCHEME_SOCKS5;
    default:
      return net::ProxyServer::SCHEME_DIRECT;
  }
}

}  // namespace

ProxyManagerAndroid::ProxyManagerAndroid() = default;
ProxyManagerAndroid::~ProxyManagerAndroid() = default;

// static
ProxyManagerAndroid* ProxyManagerAndroid::GetInstance() {
  static ProxyManagerAndroid* instance = new ProxyManagerAndroid();
  return instance;
}

void ProxyManagerAndroid::ApplyProxyConfig(
    JNIEnv* env,
    jboolean enabled,
    jint type,
    const base::android::JavaParamRef<jstring>& host,
    jint port,
    const base::android::JavaParamRef<jstring>& username,
    const base::android::JavaParamRef<jstring>& password) {
  
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);

  net::ProxyConfig config;
  
  if (!enabled || type == 0) {
    config = net::ProxyConfig::CreateDirect();
  } else {
    std::string host_str = base::android::ConvertJavaStringToUTF8(env, host);
    std::string username_str = base::android::ConvertJavaStringToUTF8(env, username);
    std::string password_str = base::android::ConvertJavaStringToUTF8(env, password);
    
    net::ProxyServer::Scheme scheme = ConvertProxyType(type);
    net::ProxyServer proxy_server(scheme, net::HostPortPair(host_str, port));
    
    config.proxy_rules().type = net::ProxyConfig::ProxyRules::Type::PROXY_LIST;
    config.proxy_rules().single_proxies.SetSingleProxyServer(proxy_server);
    
    // Сохраняем учетные данные для аутентификации
    if (!username_str.empty()) {
      proxy_username_ = username_str;
      proxy_password_ = password_str;
    }
  }
  
  current_config_ = config;
  NotifyProxyConfigChanged();
}

net::ProxyConfig ProxyManagerAndroid::GetCurrentConfig() const {
  return current_config_;
}

void ProxyManagerAndroid::NotifyProxyConfigChanged() {
  // Уведомляем систему о изменении конфигурации прокси
  for (auto& observer : observers_) {
    observer.OnProxyConfigChanged();
  }
}

void ProxyManagerAndroid::AddObserver(Observer* observer) {
  observers_.AddObserver(observer);
}

void ProxyManagerAndroid::RemoveObserver(Observer* observer) {
  observers_.RemoveObserver(observer);
}

static void JNI_ProxyManager_ApplyProxyConfig(
    JNIEnv* env,
    jboolean enabled,
    jint type,
    const base::android::JavaParamRef<jstring>& host,
    jint port,
    const base::android::JavaParamRef<jstring>& username,
    const base::android::JavaParamRef<jstring>& password) {
  ProxyManagerAndroid::GetInstance()->ApplyProxyConfig(
      env, enabled, type, host, port, username, password);
}

}  // namespace android
}  // namespace chrome
