// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "chrome/browser/proxy/kiwi_proxy_config_service.h"

#include "chrome/browser/proxy/proxy_manager_android.h"
#include "content/public/browser/browser_thread.h"

namespace chrome {
namespace android {

KiwiProxyConfigService::KiwiProxyConfigService()
    : ProxyConfigService() {
  ProxyManagerAndroid::GetInstance()->AddObserver(this);
}

KiwiProxyConfigService::~KiwiProxyConfigService() {
  ProxyManagerAndroid::GetInstance()->RemoveObserver(this);
}

void KiwiProxyConfigService::AddObserver(Observer* observer) {
  observers_.AddObserver(observer);
}

void KiwiProxyConfigService::RemoveObserver(Observer* observer) {
  observers_.RemoveObserver(observer);
}

net::ProxyConfigService::ConfigAvailability
KiwiProxyConfigService::GetLatestProxyConfig(
    net::ProxyConfigWithAnnotation* config) {
  DCHECK(config);
  
  net::ProxyConfig proxy_config = 
      ProxyManagerAndroid::GetInstance()->GetCurrentConfig();
  
  *config = net::ProxyConfigWithAnnotation(
      proxy_config,
      TRAFFIC_ANNOTATION_WITHOUT_PROTO("kiwi_proxy_config"));
  
  return CONFIG_VALID;
}

void KiwiProxyConfigService::OnProxyConfigChanged() {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
  
  net::ProxyConfigWithAnnotation config;
  GetLatestProxyConfig(&config);
  
  for (auto& observer : observers_) {
    observer.OnProxyConfigChanged(config, CONFIG_VALID);
  }
}

}  // namespace android
}  // namespace chrome
