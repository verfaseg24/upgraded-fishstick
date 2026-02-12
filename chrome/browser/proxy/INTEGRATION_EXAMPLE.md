# Пример интеграции системы прокси

## 1. Модификация chrome/browser/BUILD.gn

Добавьте в секцию `source_set("browser")`:

```gn
if (is_android) {
  deps += [
    "//chrome/browser/proxy",
  ]
}
```

## 2. Инициализация при запуске браузера

В файле `chrome/browser/chrome_browser_main_android.cc` добавьте:

```cpp
#include "chrome/browser/proxy/proxy_manager_android.h"

void ChromeBrowserMainPartsAndroid::PreMainMessageLoopRun() {
  ChromeBrowserMainParts::PreMainMessageLoopRun();
  
  // Инициализация прокси менеджера
  chrome::android::ProxyManagerAndroid::GetInstance();
}
```

## 3. Использование в ProfileNetworkContextService

Файл: `chrome/browser/net/profile_network_context_service.cc`

```cpp
#include "chrome/browser/proxy/kiwi_proxy_config_service.h"

void ProfileNetworkContextService::ConfigureNetworkContextParams(
    bool in_memory,
    const base::FilePath& relative_partition_path,
    network::mojom::NetworkContextParams* network_context_params,
    cert_verifier::mojom::CertVerifierCreationParams*
        cert_verifier_creation_params) {
  
  // ... существующий код ...

#if BUILDFLAG(IS_ANDROID)
  // Использование Kiwi прокси сервиса
  mojo::PendingRemote<network::mojom::ProxyConfigClient> proxy_config_client;
  network_context_params->proxy_config_client_receiver =
      proxy_config_client.InitWithNewPipeAndPassReceiver();
  
  proxy_config_service_ = 
      std::make_unique<chrome::android::KiwiProxyConfigService>();
  
  // Получаем текущую конфигурацию
  net::ProxyConfigWithAnnotation proxy_config;
  proxy_config_service_->GetLatestProxyConfig(&proxy_config);
  network_context_params->initial_proxy_config = 
      net::ProxyConfigWithAnnotation::ToValue(proxy_config);
#endif

  // ... остальной код ...
}
```

## 4. Добавление пункта меню в настройки

Файл: `chrome/android/java/src/org/chromium/chrome/browser/settings/MainSettings.java`

```java
import org.chromium.chrome.browser.proxy.ProxySettingsActivity;

public class MainSettings extends PreferenceFragmentCompat {
    
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // ... существующий код ...
        
        // Добавляем настройки прокси
        Preference proxyPref = new Preference(getContext());
        proxyPref.setTitle("Настройки прокси");
        proxyPref.setSummary("Настроить HTTP, HTTPS, SOCKS прокси");
        proxyPref.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), ProxySettingsActivity.class);
            startActivity(intent);
            return true;
        });
        
        getPreferenceScreen().addPreference(proxyPref);
    }
}
```

## 5. Быстрое переключение прокси через меню

Создайте файл `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxyQuickToggle.java`:

```java
package org.chromium.chrome.browser.proxy;

import android.content.Context;
import android.widget.Toast;

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
}
```

Добавьте в меню браузера:

```java
// В ChromeActivity или подобном классе
MenuItem proxyToggle = menu.add("Переключить прокси");
proxyToggle.setOnMenuItemClickListener(item -> {
    ProxyQuickToggle.toggleProxy(this);
    return true;
});
```

## 6. Индикатор статуса прокси

Создайте `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxyStatusIndicator.java`:

```java
package org.chromium.chrome.browser.proxy;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

public class ProxyStatusIndicator {
    
    public static void updateIndicator(ImageView indicator) {
        ProxyConfig config = ProxyManager.getInstance().getCurrentConfig();
        
        if (config.isEnabled()) {
            indicator.setVisibility(View.VISIBLE);
            indicator.setColorFilter(Color.GREEN);
        } else {
            indicator.setVisibility(View.GONE);
        }
    }
}
```

## 7. Обработка ошибок прокси

Создайте `chrome/browser/proxy/proxy_error_handler.cc`:

```cpp
#include "chrome/browser/proxy/proxy_error_handler.h"
#include "chrome/browser/proxy/proxy_manager_android.h"

namespace chrome {
namespace android {

void ProxyErrorHandler::OnProxyError(int error_code) {
  // Логирование ошибки
  LOG(ERROR) << "Proxy error: " << error_code;
  
  // Можно автоматически отключить прокси при критических ошибках
  if (error_code == net::ERR_PROXY_CONNECTION_FAILED) {
    // Уведомить пользователя
    // Опционально: отключить прокси
    // ProxyManagerAndroid::GetInstance()->DisableProxy();
  }
}

}  // namespace android
}  // namespace chrome
```

## 8. Тестирование прокси подключения

Создайте `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxyTester.java`:

```java
package org.chromium.chrome.browser.proxy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class ProxyTester {
    
    public interface TestCallback {
        void onTestComplete(boolean success, String message);
    }
    
    public static void testProxy(ProxyConfig config, TestCallback callback) {
        new Thread(() -> {
            try {
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
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                
                if (responseCode == 200) {
                    callback.onTestComplete(true, "Прокси работает");
                } else {
                    callback.onTestComplete(false, 
                        "Ошибка: код " + responseCode);
                }
                
            } catch (IOException e) {
                callback.onTestComplete(false, 
                    "Ошибка подключения: " + e.getMessage());
            }
        }).start();
    }
}
```

Использование в UI:

```java
Button testButton = findViewById(R.id.test_proxy_button);
testButton.setOnClickListener(v -> {
    ProxyConfig config = getCurrentConfig();
    ProxyTester.testProxy(config, (success, message) -> {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    });
});
```

## 9. Сохранение списка прокси серверов

Создайте `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxyListManager.java`:

```java
package org.chromium.chrome.browser.proxy;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ProxyListManager {
    private static final String PREF_PROXY_LIST = "proxy_list";
    
    public static void saveProxyList(List<ProxyConfig> proxies) {
        try {
            JSONArray array = new JSONArray();
            for (ProxyConfig config : proxies) {
                JSONObject obj = new JSONObject();
                obj.put("type", config.getType());
                obj.put("host", config.getHost());
                obj.put("port", config.getPort());
                obj.put("username", config.getUsername());
                obj.put("password", config.getPassword());
                array.put(obj);
            }
            
            SharedPreferences prefs = getPreferences();
            prefs.edit().putString(PREF_PROXY_LIST, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static List<ProxyConfig> loadProxyList() {
        List<ProxyConfig> proxies = new ArrayList<>();
        try {
            SharedPreferences prefs = getPreferences();
            String json = prefs.getString(PREF_PROXY_LIST, "[]");
            JSONArray array = new JSONArray(json);
            
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ProxyConfig config = new ProxyConfig(
                    obj.getInt("type"),
                    obj.getString("host"),
                    obj.getInt("port")
                );
                config.setUsername(obj.optString("username", ""));
                config.setPassword(obj.optString("password", ""));
                proxies.add(config);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proxies;
    }
    
    private static SharedPreferences getPreferences() {
        return ContextUtils.getApplicationContext()
            .getSharedPreferences("proxy_list", Context.MODE_PRIVATE);
    }
}
```

## 10. Компиляция

После добавления всех файлов:

```bash
# Сгенерировать JNI заголовки
gn gen out/Default

# Собрать проект
ninja -C out/Default chrome_public_apk
```

## Проверка работы

1. Установите APK на устройство
2. Откройте настройки браузера
3. Найдите "Настройки прокси"
4. Настройте прокси сервер
5. Включите прокси
6. Проверьте работу через любой сайт

Готово! Система прокси интегрирована в браузер.
