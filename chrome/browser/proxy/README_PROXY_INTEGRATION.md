# Интеграция системы прокси в Kiwi Browser

## Обзор

Эта система добавляет поддержку автоматической настройки прокси в Kiwi Browser для Android с поддержкой следующих типов:
- HTTP прокси
- HTTPS прокси
- SOCKS4 прокси
- SOCKS5 прокси

## Архитектура

### Java слой (Android)
1. **ProxyConfig.java** - Модель данных для конфигурации прокси
2. **ProxyManager.java** - Менеджер для управления настройками прокси
3. **ProxySettingsActivity.java** - UI для настройки прокси

### Native слой (C++)
1. **proxy_manager_android.h/cc** - JNI мост между Java и C++
2. **kiwi_proxy_config_service.h/cc** - Сервис конфигурации прокси для Chromium

## Шаги интеграции

### 1. Добавить файлы в BUILD.gn

В `chrome/browser/BUILD.gn` добавьте:

```gn
if (is_android) {
  sources += [
    "proxy/proxy_manager_android.cc",
    "proxy/proxy_manager_android.h",
    "proxy/kiwi_proxy_config_service.cc",
    "proxy/kiwi_proxy_config_service.h",
  ]
}
```

### 2. Зарегистрировать JNI

Создайте файл `chrome/browser/proxy/proxy_jni_registration.cc`:

```cpp
#include "base/android/jni_generator/jni_generator_helper.h"
#include "chrome/android/chrome_jni_headers/ProxyManager_jni.h"

namespace chrome {
namespace android {

bool RegisterProxyJni(JNIEnv* env) {
  return RegisterNativesImpl(env);
}

}  // namespace android
}  // namespace chrome
```

### 3. Интегрировать с URLRequestContext

В файле где создается `URLRequestContext` (обычно `chrome/browser/net/profile_network_context_service.cc`):

```cpp
#include "chrome/browser/proxy/kiwi_proxy_config_service.h"

// В методе создания контекста:
#if BUILDFLAG(IS_ANDROID)
  auto proxy_config_service = 
      std::make_unique<chrome::android::KiwiProxyConfigService>();
  context_builder.set_proxy_config_service(std::move(proxy_config_service));
#endif
```

### 4. Добавить Java классы в сборку

В `chrome/android/BUILD.gn` добавьте в секцию `android_library`:

```gn
java_files = [
  "java/src/org/chromium/chrome/browser/proxy/ProxyConfig.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxyManager.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxySettingsActivity.java",
]
```

### 5. Создать Layout для UI

Создайте `chrome/android/java/res/layout/activity_proxy_settings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <CheckBox
            android:id="@+id/enable_proxy_checkbox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Включить прокси" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Тип прокси:"
            android:layout_marginTop="16dp" />

        <Spinner
            android:id="@+id/proxy_type_spinner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Хост:"
            android:layout_marginTop="16dp" />

        <EditText
            android:id="@+id/host_edittext"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="proxy.example.com"
            android:inputType="textUri" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Порт:"
            android:layout_marginTop="16dp" />

        <EditText
            android:id="@+id/port_edittext"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="8080"
            android:inputType="number" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Имя пользователя (опционально):"
            android:layout_marginTop="16dp" />

        <EditText
            android:id="@+id/username_edittext"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Пароль (опционально):"
            android:layout_marginTop="16dp" />

        <EditText
            android:id="@+id/password_edittext"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textPassword" />

        <Button
            android:id="@+id/save_button"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Сохранить"
            android:layout_marginTop="24dp" />

    </LinearLayout>
</ScrollView>
```

### 6. Добавить Activity в AndroidManifest.xml

В `chrome/android/java/AndroidManifest.xml`:

```xml
<activity
    android:name="org.chromium.chrome.browser.proxy.ProxySettingsActivity"
    android:label="Настройки прокси"
    android:theme="@style/Theme.Chromium.Activity" />
```

### 7. Добавить пункт меню для открытия настроек

В коде меню браузера добавьте:

```java
Intent intent = new Intent(context, ProxySettingsActivity.class);
context.startActivity(intent);
```

## Использование

### Программная настройка прокси:

```java
ProxyConfig config = new ProxyConfig(
    ProxyConfig.ProxyType.SOCKS5,
    "proxy.example.com",
    1080
);
config.setUsername("user");
config.setPassword("pass");

ProxyManager.getInstance().setProxyConfig(config);
```

### Включение/выключение прокси:

```java
ProxyManager.getInstance().enableProxy(true);
```

## Дополнительные возможности

### Автоматическое переключение прокси

Можно добавить логику автоматического выбора прокси на основе:
- Геолокации
- Типа сети (WiFi/Mobile)
- Списка доменов

### PAC (Proxy Auto-Config) поддержка

Для поддержки PAC файлов нужно расширить `KiwiProxyConfigService`:

```cpp
config.set_pac_url(GURL("http://example.com/proxy.pac"));
```

### Обход прокси для локальных адресов

```cpp
config.proxy_rules().bypass_rules.AddRuleFromString("localhost");
config.proxy_rules().bypass_rules.AddRuleFromString("127.0.0.1");
config.proxy_rules().bypass_rules.AddRuleFromString("*.local");
```

## Тестирование

1. Запустите браузер
2. Откройте настройки прокси
3. Настройте прокси сервер
4. Проверьте подключение через сайт типа https://whatismyipaddress.com/

## Безопасность

- Пароли хранятся в SharedPreferences (рекомендуется использовать EncryptedSharedPreferences)
- Для production рекомендуется добавить шифрование учетных данных
- Добавить валидацию вводимых данных

## Известные ограничения

1. Аутентификация прокси требует дополнительной реализации через `HttpAuthHandler`
2. Для SOCKS прокси с аутентификацией нужна дополнительная настройка
3. Смена прокси требует перезагрузки сетевого стека

## Следующие шаги

1. Добавить UI для списка прокси серверов
2. Реализовать автоматическое тестирование прокси
3. Добавить статистику использования прокси
4. Реализовать ротацию прокси серверов
