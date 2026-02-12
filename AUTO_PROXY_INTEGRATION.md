# 🚀 Интеграция автоматических прокси - Быстрая инструкция

## Что добавлено

### Новые файлы:
1. `ProxyFetcher.java` - получение прокси из API
2. `AutoProxyActivity.java` - UI для выбора прокси
3. `activity_auto_proxy.xml` - layout для UI

### Обновленные файлы:
1. `ProxySettingsActivity.java` - добавлена кнопка "Бесплатные прокси"
2. `activity_proxy_settings.xml` - добавлена кнопка в layout

## Шаги интеграции

### 1. Обновить chrome/android/BUILD.gn

Добавьте в `java_files`:
```gn
"java/src/org/chromium/chrome/browser/proxy/ProxyFetcher.java",
"java/src/org/chromium/chrome/browser/proxy/AutoProxyActivity.java",
```

Итого должно быть 7 файлов:
```gn
java_files += [
  "java/src/org/chromium/chrome/browser/proxy/ProxyConfig.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxyManager.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxySettingsActivity.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxyQuickToggle.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxyTester.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxyFetcher.java",
  "java/src/org/chromium/chrome/browser/proxy/AutoProxyActivity.java",
]
```

### 2. Обновить AndroidManifest.xml

Добавьте AutoProxyActivity:
```xml
<activity
    android:name="org.chromium.chrome.browser.proxy.AutoProxyActivity"
    android:label="Auto Proxy"
    android:exported="false" />
```

Итого должно быть 2 Activity:
```xml
<activity android:name="org.chromium.chrome.browser.proxy.ProxySettingsActivity" ... />
<activity android:name="org.chromium.chrome.browser.proxy.AutoProxyActivity" ... />
```

### 3. Проверить разрешения

Убедитесь, что в AndroidManifest.xml есть:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Как это работает

### Пользовательский сценарий:

1. Пользователь открывает Settings → Proxy Settings
2. Видит зеленую кнопку "🌐 Бесплатные прокси (авто)"
3. Нажимает на кнопку
4. Открывается список прокси из DE, US, SE, FI
5. Выбирает прокси одним нажатием
6. Прокси автоматически активируется

### Технический процесс:

```
ProxySettingsActivity
    ↓ (нажатие кнопки)
AutoProxyActivity
    ↓ (onCreate)
ProxyFetcher.fetchProxies()
    ↓
Проверка кэша (24 часа)
    ↓ (если устарел)
API запросы (12 запросов)
    ├── DE: страницы 1, 2, 3
    ├── US: страницы 1, 2, 3
    ├── SE: страницы 1, 2, 3
    └── FI: страницы 1, 2, 3
    ↓
Фильтрация (uptime > 80%, response < 1s)
    ↓
Проверка (первые 20 прокси)
    ↓
Кэширование в SharedPreferences
    ↓
Показ списка пользователю
    ↓ (выбор прокси)
ProxyManager.setProxyConfig()
    ↓
Прокси активирован!
```

## API Configuration

### API Key
```java
private static final String API_KEY = "019c5236e4417ddb8e9247c61c33336a";
```

### Endpoint
```
GET https://api.getfreeproxy.com/v1/proxies?country={country}&page={page}
```

### Страны
```java
private static final String[] COUNTRIES = {"DE", "US", "SE", "FI"};
```

### Страниц на страну
```java
private static final int PAGES_PER_COUNTRY = 3;
```

### Интервал обновления
```java
private static final long UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // 24 часа
```

## Кастомизация

### Изменить страны

В `ProxyFetcher.java`:
```java
private static final String[] COUNTRIES = {"UK", "FR", "NL", "CA"};
```

### Изменить критерии фильтрации

```java
if (proxy.uptime > 90 && proxy.responseTime < 0.5) {
    filtered.add(proxy);
}
```

### Изменить количество проверяемых прокси

```java
int toCheck = Math.min(proxies.size(), 50); // было 20
```

### Изменить интервал обновления

```java
private static final long UPDATE_INTERVAL = 12 * 60 * 60 * 1000; // 12 часов
```

## Тестирование

### 1. Проверить компиляцию

```bash
gn gen out/Default
autoninja -C out/Default chrome_public_apk
```

### 2. Установить APK

```bash
adb install -r out/Default/apks/ChromePublic.apk
```

### 3. Проверить функциональность

1. Откройте браузер
2. Settings → Proxy Settings
3. Нажмите "🌐 Бесплатные прокси (авто)"
4. Должен появиться список прокси
5. Выберите любой прокси
6. Проверьте на https://whatismyipaddress.com/

### 4. Проверить логи

```bash
adb logcat | grep -i "ProxyFetcher\|AutoProxy"
```

Должны увидеть:
```
ProxyFetcher: Fetching proxies from API...
ProxyFetcher: Found 15 proxies from DE
ProxyFetcher: Found 12 proxies from US
ProxyFetcher: Filtered 45 quality proxies
ProxyFetcher: Verified 18 working proxies
ProxyFetcher: Cached 18 proxies
```

## Устранение проблем

### Прокси не загружаются

**Проверьте:**
1. Интернет подключение
2. API key корректный
3. Логи: `adb logcat | grep ProxyFetcher`

**Решение:**
```java
// Добавить больше логирования
Log.d("ProxyFetcher", "API Response: " + response);
```

### Все прокси не работают

**Причина:** Строгие критерии фильтрации

**Решение:** Ослабить фильтры
```java
if (proxy.uptime > 70 && proxy.responseTime < 2.0) { // было 80 и 1.0
```

### Медленная загрузка

**Причина:** Проверка всех прокси

**Решение:** Уменьшить количество проверок
```java
int toCheck = Math.min(proxies.size(), 10); // было 20
```

### Ошибка 429 (Rate Limit)

**Причина:** Слишком много запросов к API

**Решение:** Увеличить интервал обновления
```java
private static final long UPDATE_INTERVAL = 48 * 60 * 60 * 1000; // 48 часов
```

## Безопасность

### Production рекомендации:

1. **API Key в BuildConfig**
```java
private static final String API_KEY = BuildConfig.PROXY_API_KEY;
```

2. **EncryptedSharedPreferences**
```java
EncryptedSharedPreferences.create(
    context,
    "proxy_fetcher",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
);
```

3. **ProGuard обфускация**
```proguard
-keep class org.chromium.chrome.browser.proxy.ProxyFetcher {
    public *;
}
```

## Производительность

### Оптимизации:

1. **Кэширование** - запросы раз в 24 часа
2. **Фоновая загрузка** - не блокирует UI
3. **Ограничение проверки** - только 20 прокси
4. **Задержка между запросами** - 500ms

### Метрики:

- **Время первой загрузки**: 10-30 секунд
- **Время из кэша**: мгновенно
- **Трафик**: ~12-60 KB
- **Запросов к API**: 12 (4 страны × 3 страницы)

## Готово!

После интеграции пользователи смогут:
- ✅ Получать бесплатные прокси одним нажатием
- ✅ Выбирать из проверенных прокси
- ✅ Автоматически подключаться к прокси
- ✅ Обновлять список вручную
- ✅ Видеть информацию о каждом прокси

**Документация:** [AUTO_PROXY_GUIDE.md](AUTO_PROXY_GUIDE.md)
