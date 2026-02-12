# ⚡ Быстрый старт - 5 минут до сборки

## 🎯 Цель
Собрать APK браузера с поддержкой прокси через GitHub Actions

## 📝 Шаги (5 минут)

### 1. Форкнуть репозиторий (30 секунд)
```bash
# Через GitHub UI: нажмите "Fork" вверху страницы
# Или через CLI:
gh repo fork kiwibrowser/src.next --clone
cd src.next
```

### 2. Сделать 3 правки (3 минуты)

#### Правка 1: `chrome/browser/BUILD.gn`
Найдите `source_set("browser")` и добавьте в `deps`:
```gn
if (is_android) {
  deps += [ "//chrome/browser/proxy" ]
}
```

#### Правка 2: `chrome/android/BUILD.gn`
Найдите `android_library("chrome_java")` и добавьте в `java_files`:
```gn
"java/src/org/chromium/chrome/browser/proxy/ProxyConfig.java",
"java/src/org/chromium/chrome/browser/proxy/ProxyManager.java",
"java/src/org/chromium/chrome/browser/proxy/ProxySettingsActivity.java",
"java/src/org/chromium/chrome/browser/proxy/ProxyQuickToggle.java",
"java/src/org/chromium/chrome/browser/proxy/ProxyTester.java",
```

#### Правка 3: `chrome/android/java/AndroidManifest.xml`
Добавьте перед `</application>`:
```xml
<activity android:name="org.chromium.chrome.browser.proxy.ProxySettingsActivity" 
          android:label="Proxy Settings" android:exported="false" />
```

### 3. Закоммитить и запушить (1 минута)
```bash
git add .
git commit -m "Add proxy support"
git push origin main
```

### 4. Запустить сборку (30 секунд)
1. Откройте ваш форк на GitHub
2. Перейдите в **Actions**
3. Выберите **"Build APK with Proxy Support"**
4. Нажмите **"Run workflow"**
5. Выберите:
   - architecture: **arm64**
   - build_type: **release**
6. Нажмите **"Run workflow"**

### 5. Подождать и скачать (2-4 часа)
- Сборка займет 2-4 часа
- Скачайте APK из **Artifacts** или **Releases**

## 🎉 Готово!

Установите APK на устройство и настройте прокси:
1. Меню → Settings → Proxy Settings
2. Включите прокси
3. Настройте хост, порт, тип
4. Сохраните

## 🔍 Быстрая проверка

Перед коммитом проверьте, что все файлы на месте:
```bash
ls chrome/browser/proxy/*.cc
ls chrome/android/java/src/org/chromium/chrome/browser/proxy/*.java
ls chrome/android/java/res/layout/activity_proxy_settings.xml
```

Все должно быть ✅

## 🆘 Проблемы?

- **Сборка не запускается**: Проверьте, что все 3 правки сделаны
- **Ошибка компиляции**: Смотрите логи в Actions
- **APK не устанавливается**: Проверьте архитектуру устройства

## 📚 Подробнее

- [CHECKLIST.md](CHECKLIST.md) - Полный чеклист
- [GITHUB_ACTIONS_BUILD.md](GITHUB_ACTIONS_BUILD.md) - Детали GitHub Actions
- [BUILD_APK_GUIDE.md](BUILD_APK_GUIDE.md) - Локальная сборка

---

**Время до готового APK: 5 минут работы + 2-4 часа ожидания** ⏱️
