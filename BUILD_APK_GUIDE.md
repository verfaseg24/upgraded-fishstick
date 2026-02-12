# Полная инструкция по сборке APK с поддержкой прокси

## Предварительные требования

### 1. Системные требования
- **ОС**: Linux (Ubuntu 20.04+ рекомендуется) или macOS
- **RAM**: минимум 16 GB (рекомендуется 32 GB)
- **Диск**: минимум 100 GB свободного места
- **CPU**: многоядерный процессор (рекомендуется 8+ ядер)

### 2. Установка зависимостей

#### Ubuntu/Debian:
```bash
sudo apt-get update
sudo apt-get install -y git python3 python3-pip curl lsb-release sudo

# Установка depot_tools
git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
export PATH="$PATH:${HOME}/depot_tools"
echo 'export PATH="$PATH:${HOME}/depot_tools"' >> ~/.bashrc

# Установка зависимостей для сборки Android
sudo apt-get install -y openjdk-11-jdk
```

#### macOS:
```bash
# Установка Xcode Command Line Tools
xcode-select --install

# Установка depot_tools
git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
export PATH="$PATH:${HOME}/depot_tools"
echo 'export PATH="$PATH:${HOME}/depot_tools"' >> ~/.zshrc
```

### 3. Установка Android SDK и NDK

```bash
# Скачать Android SDK командной строки
mkdir -p ~/android-sdk
cd ~/android-sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip

# Установить необходимые компоненты
cd cmdline-tools/bin
./sdkmanager --sdk_root=$HOME/android-sdk "platform-tools" "platforms;android-33" "build-tools;33.0.0"
./sdkmanager --sdk_root=$HOME/android-sdk "ndk;23.1.7779620"

# Установить переменные окружения
export ANDROID_SDK_ROOT=$HOME/android-sdk
export ANDROID_NDK_HOME=$HOME/android-sdk/ndk/23.1.7779620
echo 'export ANDROID_SDK_ROOT=$HOME/android-sdk' >> ~/.bashrc
echo 'export ANDROID_NDK_HOME=$HOME/android-sdk/ndk/23.1.7779620' >> ~/.bashrc
```

## Шаг 1: Подготовка исходного кода

```bash
# Перейти в директорию проекта Kiwi
cd /path/to/kiwi/src.next

# Убедиться, что все файлы прокси на месте
ls -la chrome/browser/proxy/
ls -la chrome/android/java/src/org/chromium/chrome/browser/proxy/
```

## Шаг 2: Интеграция прокси в сборку

### 2.1 Обновить chrome/browser/BUILD.gn

Найдите секцию `source_set("browser")` и добавьте:

```bash
# Открыть файл
nano chrome/browser/BUILD.gn

# Найти секцию source_set("browser") и добавить в deps:
# (если is_android блок уже есть, добавьте в него)
```

Добавьте в файл:
```gn
if (is_android) {
  deps += [
    "//chrome/browser/proxy",
  ]
}
```

### 2.2 Обновить chrome/android/BUILD.gn

```bash
nano chrome/android/BUILD.gn
```

Найдите `android_library("chrome_java")` и добавьте в `java_files`:

```gn
java_files += [
  "java/src/org/chromium/chrome/browser/proxy/ProxyConfig.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxyManager.java",
  "java/src/org/chromium/chrome/browser/proxy/ProxySettingsActivity.java",
]
```

### 2.3 Добавить Activity в AndroidManifest.xml

```bash
nano chrome/android/java/AndroidManifest.xml
```

Добавьте перед закрывающим тегом `</application>`:

```xml
<activity
    android:name="org.chromium.chrome.browser.proxy.ProxySettingsActivity"
    android:label="Proxy Settings"
    android:exported="false" />
```

## Шаг 3: Настройка сборки

### 3.1 Создать конфигурацию сборки

```bash
# Создать директорию для сборки
mkdir -p out/Default

# Создать файл args.gn
cat > out/Default/args.gn << 'EOF'
target_os = "android"
target_cpu = "arm64"

is_debug = false
is_official_build = true
is_component_build = false

# Android specific
android_channel = "stable"
android_default_version_name = "105.0.5195.33"
android_default_version_code = "519533000"

# Оптимизации
symbol_level = 1
enable_nacl = false
remove_webcore_debug_symbols = true

# Отключить ненужные функции для уменьшения размера
enable_reporting = false
enable_nacl = false
enable_remoting = false
enable_google_now = false

# Включить оптимизации
use_thin_lto = true
chrome_pgo_phase = 0

# Branding
is_chrome_branded = false
use_official_google_api_keys = false

# Подпись
android_keystore_path = "//android_keystore"
android_keystore_name = "chromium-debug"
android_keystore_password = "chromium"
EOF
```

### 3.2 Сгенерировать файлы сборки

```bash
gn gen out/Default
```

Если возникают ошибки, проверьте:
```bash
gn check out/Default
```

## Шаг 4: Сборка APK

### 4.1 Полная сборка

```bash
# Сборка основного APK (может занять 2-4 часа при первой сборке)
autoninja -C out/Default chrome_public_apk

# Или для более быстрой сборки (debug версия):
autoninja -C out/Default chrome_public_apk
```

### 4.2 Инкрементальная сборка (после изменений)

```bash
# Пересобрать только измененные файлы
autoninja -C out/Default chrome_public_apk
```

### 4.3 Сборка для разных архитектур

Для ARM64:
```bash
gn gen out/arm64 --args='target_os="android" target_cpu="arm64" is_debug=false'
autoninja -C out/arm64 chrome_public_apk
```

Для ARM32:
```bash
gn gen out/arm --args='target_os="android" target_cpu="arm" is_debug=false'
autoninja -C out/arm chrome_public_apk
```

Для x86_64 (эмулятор):
```bash
gn gen out/x64 --args='target_os="android" target_cpu="x64" is_debug=false'
autoninja -C out/x64 chrome_public_apk
```

## Шаг 5: Подписание APK

### 5.1 Создать ключ для подписи (если нет)

```bash
keytool -genkey -v -keystore kiwi-release-key.keystore \
  -alias kiwi-key -keyalg RSA -keysize 2048 -validity 10000

# Введите пароль и информацию о разработчике
```

### 5.2 Подписать APK

```bash
# Найти собранный APK
APK_PATH="out/Default/apks/ChromePublic.apk"

# Выровнять APK
zipalign -v -p 4 $APK_PATH kiwi-browser-aligned.apk

# Подписать APK
apksigner sign --ks kiwi-release-key.keystore \
  --out kiwi-browser-signed.apk kiwi-browser-aligned.apk

# Проверить подпись
apksigner verify kiwi-browser-signed.apk
```

## Шаг 6: Установка и тестирование

### 6.1 Установить на устройство

```bash
# Подключить Android устройство по USB и включить отладку по USB

# Установить APK
adb install -r kiwi-browser-signed.apk

# Или установить напрямую из директории сборки
adb install -r out/Default/apks/ChromePublic.apk
```

### 6.2 Запустить браузер

```bash
# Запустить приложение
adb shell am start -n org.chromium.chrome/com.google.android.apps.chrome.Main

# Посмотреть логи
adb logcat | grep -i chromium
```

### 6.3 Тестирование прокси

1. Откройте браузер на устройстве
2. Откройте меню → Настройки
3. Найдите "Proxy Settings" или "Настройки прокси"
4. Настройте прокси сервер:
   - Включите прокси
   - Выберите тип (HTTP/HTTPS/SOCKS4/SOCKS5)
   - Введите хост и порт
   - (Опционально) Введите логин и пароль
5. Сохраните настройки
6. Проверьте работу через https://whatismyipaddress.com/

## Шаг 7: Оптимизация и уменьшение размера APK

### 7.1 Включить ProGuard/R8

В `out/Default/args.gn` добавьте:
```gn
is_java_debug = false
enable_proguard_obfuscation = true
```

### 7.2 Удалить ненужные ресурсы

```bash
# Пересобрать с оптимизациями
gn gen out/Default
autoninja -C out/Default chrome_public_apk
```

### 7.3 Создать bundle вместо APK (для Google Play)

```bash
autoninja -C out/Default chrome_public_bundle
```

## Устранение проблем

### Ошибка: "No Android SDK found"

```bash
export ANDROID_SDK_ROOT=$HOME/android-sdk
gn gen out/Default
```

### Ошибка: "ninja: error: loading 'build.ninja'"

```bash
rm -rf out/Default
gn gen out/Default
```

### Ошибка компиляции Java

```bash
# Проверить версию Java
java -version

# Должна быть Java 11
sudo update-alternatives --config java
```

### APK не устанавливается

```bash
# Удалить старую версию
adb uninstall org.chromium.chrome

# Установить заново
adb install -r kiwi-browser-signed.apk
```

### Прокси не работает

Проверьте логи:
```bash
adb logcat | grep -i "proxy\|ProxyManager"
```

## Быстрая сборка (для разработки)

Для быстрой итерации во время разработки:

```bash
# Debug сборка (быстрее, но больше размер)
gn gen out/Debug --args='target_os="android" target_cpu="arm64" is_debug=true'
autoninja -C out/Debug chrome_public_apk

# Установить и запустить
adb install -r out/Debug/apks/ChromePublic.apk
adb shell am start -n org.chromium.chrome/com.google.android.apps.chrome.Main
```

## Автоматизация сборки

Создайте скрипт `build.sh`:

```bash
#!/bin/bash
set -e

echo "=== Сборка Kiwi Browser с поддержкой прокси ==="

# Настройка окружения
export ANDROID_SDK_ROOT=$HOME/android-sdk
export ANDROID_NDK_HOME=$HOME/android-sdk/ndk/23.1.7779620

# Генерация файлов сборки
echo "Генерация файлов сборки..."
gn gen out/Default

# Сборка APK
echo "Сборка APK..."
autoninja -C out/Default chrome_public_apk

# Подписание
echo "Подписание APK..."
APK_PATH="out/Default/apks/ChromePublic.apk"
zipalign -v -p 4 $APK_PATH kiwi-browser-aligned.apk
apksigner sign --ks kiwi-release-key.keystore \
  --out kiwi-browser-signed.apk kiwi-browser-aligned.apk

echo "=== Сборка завершена! ==="
echo "APK: kiwi-browser-signed.apk"
```

Запуск:
```bash
chmod +x build.sh
./build.sh
```

## Итоговый APK

После успешной сборки вы получите:
- **Файл**: `kiwi-browser-signed.apk`
- **Размер**: ~50-80 MB (зависит от архитектуры и оптимизаций)
- **Поддержка**: HTTP, HTTPS, SOCKS4, SOCKS5 прокси
- **Платформа**: Android 7.0+ (API 24+)

Готово! Теперь у вас есть полнофункциональный браузер с поддержкой прокси.
