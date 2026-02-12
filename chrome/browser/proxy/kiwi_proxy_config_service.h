// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef CHROME_BROWSER_PROXY_KIWI_PROXY_CONFIG_SERVICE_H_
#define CHROME_BROWSER_PROXY_KIWI_PROXY_CONFIG_SERVICE_H_

#include "base/observer_list.h"
#include "chrome/browser/proxy/proxy_manager_android.h"
#include "net/proxy_resolution/proxy_config_service.h"
#include "net/proxy_resolution/proxy_config_with_annotation.h"

namespace chrome {
namespace android {

class KiwiProxyConfigService : public net::ProxyConfigService,
                               public ProxyManagerAndroid::Observer {
 public:
  KiwiProxyConfigService();
  ~KiwiProxyConfigService() override;

  KiwiProxyConfigService(const KiwiProxyConfigService&) = delete;
  KiwiProxyConfigService& operator=(const KiwiProxyConfigService&) = delete;

  // ProxyConfigService implementation
  void AddObserver(Observer* observer) override;
  void RemoveObserver(Observer* observer) override;
  ConfigAvailability GetLatestProxyConfig(
      net::ProxyConfigWithAnnotation* config) override;

  // ProxyManagerAndroid::Observer implementation
  void OnProxyConfigChanged() override;

 private:
  base::ObserverList<Observer> observers_;
};

}  // namespace android
}  // namespace chrome

#endif  // CHROME_BROWSER_PROXY_KIWI_PROXY_CONFIG_SERVICE_H_
