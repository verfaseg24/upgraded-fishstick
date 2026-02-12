// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef CHROME_BROWSER_PROXY_PROXY_MANAGER_ANDROID_H_
#define CHROME_BROWSER_PROXY_PROXY_MANAGER_ANDROID_H_

#include <jni.h>
#include <string>

#include "base/android/jni_android.h"
#include "base/observer_list.h"
#include "net/proxy_resolution/proxy_config.h"

namespace chrome {
namespace android {

class ProxyManagerAndroid {
 public:
  class Observer {
   public:
    virtual void OnProxyConfigChanged() = 0;
   protected:
    virtual ~Observer() = default;
  };

  static ProxyManagerAndroid* GetInstance();

  ProxyManagerAndroid(const ProxyManagerAndroid&) = delete;
  ProxyManagerAndroid& operator=(const ProxyManagerAndroid&) = delete;

  void ApplyProxyConfig(JNIEnv* env,
                       jboolean enabled,
                       jint type,
                       const base::android::JavaParamRef<jstring>& host,
                       jint port,
                       const base::android::JavaParamRef<jstring>& username,
                       const base::android::JavaParamRef<jstring>& password);

  net::ProxyConfig GetCurrentConfig() const;
  
  void AddObserver(Observer* observer);
  void RemoveObserver(Observer* observer);

 private:
  ProxyManagerAndroid();
  ~ProxyManagerAndroid();

  void NotifyProxyConfigChanged();

  net::ProxyConfig current_config_;
  std::string proxy_username_;
  std::string proxy_password_;
  base::ObserverList<Observer> observers_;
};

}  // namespace android
}  // namespace chrome

#endif  // CHROME_BROWSER_PROXY_PROXY_MANAGER_ANDROID_H_
