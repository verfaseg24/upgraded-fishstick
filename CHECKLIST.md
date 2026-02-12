# ✅ Чеклист: Что нужно сделать для сборки APK с прокси

## 📋 Быстрый старт (для нетерпеливых)

1. ✅ Все файлы прокси уже созданы
2. ⚠️ Нужно интегрировать в сборку (см. ниже)
3. 🚀 Запустить GitHub Actions или собрать локально

---

## 🔧 Что уже готово

### ✅ Java файлы (Android UI)
- [x] `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxyConfig.java`
- [x] `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxyManager.java`
- [x] `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxySettingsActivity.java`
- [x] `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxyQuickToggle.java`
- [x] `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxyTester.java`
- [x] `chrome/android/java/src/org/chromium/chrome/browser/proxy/ProxyFetcher.java` 🆕
- [x] `chrome/android/java/src/org/chromium/chrome/browser/proxy/AutoProxyActivity.java` 🆕

### ✅ C++ файлы (Native код)
- [x] `chrome/browser/proxy/proxy_manager_android.h`
- [x] `chrome/browser/proxy/proxy_manager_android.cc`
- [x] `chrome/browser/proxy/kiwi_proxy_config_service.h`
- [x] `chrome/browser/proxy/kiwi_proxy_config_service.cc`

### ✅ Конфигурация
- [x] `chrome/browser/proxy/BUILD.gn`
- [x] `chrome/android/java/res/layout/activity_proxy_settings.xml`
- [x] `chrome/android/java/res/layout/activity_auto_proxy.xml` 🆕

### ✅ Документация
- [x] `chrome/browser/proxy/README_PROXY_INTEGRATION.md`
- [x] `chrome/browser/proxy/INTEGRATION_EXAMPLE.md`
- [x] `BUILD_APK_GUIDE.md`
- [x] `GITHUB_ACTIONS_BUILD.md`
- [x] `AUTO_PROXY_GUIDE.md` 🆕

### ✅ GitHub Actions
- [x] `.github/workflows/build_proxy_apk.yml` - Полная сборка
- [x] `.github/workflows/build_simple_proxy_apk.yml` - Простая сборка

---

## ⚠️ Что нужно сделать вручную

### 1. Интегрировать в BUILD.gn файлы

#### 📝 Файл: `chrome/browser/BUILD.gn`

Найдите секцию `source_set("browser")` и добавьте:

```gn
if (is_android) {
  deps += [
    "//chrome/browser/proxy",
  ]
}
```

**Как найти:**
```bash
grep -n "source_set(\"browser\")" chrome/browser/BUILD.gn
```

#### 📝 Файл: `chrome/android/BUILD.gn`

Найдите `android_library("chrome_java")` и добавьте в `java_files`:

```gn
java_files += [
  "java/src/org/chromium/chrome/browser/proxy/ProxyConfig.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxyManager.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxySettingsActivity.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxyQuickToggle.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxyTester.java",
]
```

**Как найти:**
```bash
grep -n "android_library(\"chrome_java\")" chrome/android/BUILD.gn
```

### 2. Добавить Activity в AndroidManifest.xml

#### 📝 Файл: `chrome/android/java/AndroidManifest.xml`

Добавьте перед закрывающим тегом `</application>`:

```xml
<activity
    android:name="org.chromium.chrome.browser.proxy.ProxySettingsActivity"
    android:label="Proxy Settings"
    android:exported="false" />
```

**Где добавить:**
```bash
grep -n "</application>" chrome/android/java/AndroidManifest.xml
# Добавьте ПЕРЕД этой строкой
```

### 3. (Опционально) Добавить пункт меню

Если хотите добавить пункт меню для открытия настроек прокси, найдите файл с меню настроек и добавьте код из `INTEGRATION_EXAMPLE.md`.

---

## 🚀 Способы сборки APK

### Вариант 1: GitHub Actions (РЕКОМЕНДУЕТСЯ)

**Преимущества:**
- ✅ Не нужен мощный компьютер
- ✅ Автоматическая сборка
- ✅ Бесплатно для публичных репозиториев

**Шаги:**
1. Форкните репозиторий на GitHub
2. Сделайте изменения из раздела "Что нужно сделать вручную"
3. Закоммитьте и запушьте изменения
4. Перейдите в Actions → "Build APK with Proxy Support"
5. Нажмите "Run workflow"
6. Выберите `arm64` и `release`
7. Подождите 2-4 часа
8. Скачайте APK из Artifacts или Releases

**Подробнее:** `GITHUB_ACTIONS_BUILD.md`

### Вариант 2: Локальная сборка

**Требования:**
- 💻 Linux (Ubuntu 20.04+) или macOS
- 💾 100+ GB свободного места
- 🧠 16+ GB RAM (рекомендуется 32 GB)
- ⏱️ 2-4 часа времени

**Шаги:**
1. Установите зависимости
2. Настройте Android SDK/NDK
3. Сделайте изменения из раздела "Что нужно сделать вручную"
4. Запустите сборку: `autoninja -C out/Default chrome_public_apk`
5. Найдите APK в `out/Default/apks/ChromePublic.apk`

**Подробнее:** `BUILD_APK_GUIDE.md`

### Вариант 3: Использовать Kiwi buildbot (если есть доступ)

Если у вас есть `BUILD_KEY` для Kiwi buildbot:

1. Настройте секрет `BUILD_KEY` в GitHub
2. Запустите workflow "Simple: Build with Proxy"
3. Скачайте APK через 30-60 минут

---

## 📝 Минимальные изменения для сборки

Если хотите собрать APK ПРЯМО СЕЙЧАС с минимальными изменениями:

### Шаг 1: Отредактировать 3 файла

```bash
# 1. chrome/browser/BUILD.gn
# Добавить в deps (если is_android блок):
deps += [ "//chrome/browser/proxy" ]

# 2. chrome/android/BUILD.gn  
# Добавить в java_files:
"java/src/org/chromium/chrome/browser/proxy/ProxyConfig.java",
"java/src/org/chromium/chrome/browser/proxy/ProxyManager.java",
"java/src/org/chromium/chrome/browser/proxy/ProxySettingsActivity.java",
"java/src/org/chromium/chrome/browser/proxy/ProxyQuickToggle.java",
"java/src/org/chromium/chrome/browser/proxy/ProxyTester.java",

# 3. chrome/android/java/AndroidManifest.xml
# Добавить перед </application>:
<activity android:name="org.chromium.chrome.browser.proxy.ProxySettingsActivity" 
          android:label="Proxy Settings" android:exported="false" />
```

### Шаг 2: Собрать

**Локально:**
```bash
gn gen out/Default
autoninja -C out/Default chrome_public_apk
```

**Через GitHub Actions:**
1. Закоммитьте изменения
2. Запустите workflow
3. Подождите
4. Скачайте APK

---

## 🎯 Быстрая проверка перед сборкой

Выполните эти команды, чтобы убедиться, что все файлы на месте:

```bash
# Проверка Java файлов
ls -la chrome/android/java/src/org/chromium/chrome/browser/proxy/

# Проверка C++ файлов
ls -la chrome/browser/proxy/

# Проверка layout
ls -la chrome/android/java/res/layout/activity_proxy_settings.xml

# Проверка BUILD.gn
ls -la chrome/browser/proxy/BUILD.gn

# Проверка workflows
ls -la .github/workflows/build_proxy_apk.yml
```

Все файлы должны существовать! ✅

---

## 🐛 Частые проблемы

### Проблема: "No such file or directory: ProxyManager.java"

**Решение:** Убедитесь, что файл добавлен в `chrome/android/BUILD.gn`

### Проблема: "undefined reference to ProxyManagerAndroid"

**Решение:** Убедитесь, что `chrome/browser/proxy` добавлен в deps в `chrome/browser/BUILD.gn`

### Проблема: "Activity not found: ProxySettingsActivity"

**Решение:** Добавьте Activity в `AndroidManifest.xml`

### Проблема: Сборка занимает слишком много времени

**Решение:** 
- Используйте GitHub Actions
- Или соберите только debug версию: `is_debug = true`
- Или используйте ccache для кэширования

---

## 📱 После установки APK

1. Откройте браузер
2. Меню → Settings
3. Найдите "Proxy Settings"
4. Настройте прокси:
   - Type: HTTP/HTTPS/SOCKS4/SOCKS5
   - Host: ваш прокси сервер
   - Port: порт прокси
   - Username/Password (если нужно)
5. Нажмите "Test" для проверки
6. Нажмите "Save"
7. Проверьте на https://whatismyipaddress.com/

---

## 📚 Дополнительные ресурсы

- `README_PROXY_INTEGRATION.md` - Подробная документация по интеграции
- `INTEGRATION_EXAMPLE.md` - Примеры кода для интеграции
- `BUILD_APK_GUIDE.md` - Полное руководство по локальной сборке
- `GITHUB_ACTIONS_BUILD.md` - Руководство по сборке через GitHub Actions

---

## ✨ Итого

### Что готово:
- ✅ Все файлы прокси созданы
- ✅ UI для настройки прокси
- ✅ Поддержка HTTP, HTTPS, SOCKS4, SOCKS5
- ✅ Аутентификация
- ✅ Тестирование прокси
- ✅ GitHub Actions workflows
- ✅ Документация

### Что нужно сделать:
- ⚠️ Отредактировать 3 файла (BUILD.gn × 2, AndroidManifest.xml)
- ⚠️ Собрать APK (локально или через GitHub Actions)
- ⚠️ Установить и протестировать

### Время до готового APK:
- **GitHub Actions**: 2-4 часа (автоматически)
- **Локальная сборка**: 2-4 часа (вручную)
- **Kiwi buildbot**: 30-60 минут (если есть доступ)

---

**Удачи! 🚀**
