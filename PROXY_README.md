# 🌐 Kiwi Browser с поддержкой прокси

Полнофункциональный браузер на базе Chromium с встроенной поддержкой HTTP, HTTPS, SOCKS4 и SOCKS5 прокси.

## 🚀 Быстрый старт

### Вариант 1: Собрать через GitHub Actions (рекомендуется)

```bash
# 1. Форкнуть репозиторий
# 2. Сделать 3 правки (см. CHECKLIST.md)
# 3. Запустить Actions → "Build APK with Proxy Support"
# 4. Скачать APK через 2-4 часа
```

**Подробнее:** [GITHUB_ACTIONS_BUILD.md](GITHUB_ACTIONS_BUILD.md)

### Вариант 2: Собрать локально

```bash
# Установить зависимости
sudo apt-get install python3 openjdk-11-jdk git

# Настроить depot_tools и Android SDK
# (см. BUILD_APK_GUIDE.md)

# Собрать
gn gen out/Default
autoninja -C out/Default chrome_public_apk
```

**Подробнее:** [BUILD_APK_GUIDE.md](BUILD_APK_GUIDE.md)

## ✨ Возможности

- ✅ **HTTP прокси** - стандартный HTTP прокси
- ✅ **HTTPS прокси** - защищенный HTTPS прокси
- ✅ **SOCKS4 прокси** - SOCKS версии 4
- ✅ **SOCKS5 прокси** - SOCKS версии 5 с поддержкой UDP
- ✅ **Аутентификация** - поддержка username/password
- ✅ **Удобный UI** - простой интерфейс настройки
- ✅ **Тестирование** - встроенная проверка прокси
- ✅ **Быстрое переключение** - включение/выключение одним нажатием
- ✅ **НОВОЕ: Автоматические бесплатные прокси** - получение проверенных прокси из API

## 📁 Структура файлов

```
chrome/
├── browser/proxy/                          # C++ код
│   ├── proxy_manager_android.cc/h         # JNI мост
│   ├── kiwi_proxy_config_service.cc/h     # Сервис конфигурации
│   ├── BUILD.gn                           # Конфигурация сборки
│   ├── README_PROXY_INTEGRATION.md        # Документация
│   └── INTEGRATION_EXAMPLE.md             # Примеры интеграции
│
└── android/java/
    ├── src/org/chromium/chrome/browser/proxy/
    │   ├── ProxyConfig.java               # Модель данных
    │   ├── ProxyManager.java              # Менеджер прокси
    │   ├── ProxySettingsActivity.java     # UI настроек
    │   ├── ProxyQuickToggle.java          # Быстрое переключение
    │   ├── ProxyTester.java               # Тестирование прокси
    │   ├── ProxyFetcher.java              # 🆕 Получение бесплатных прокси
    │   └── AutoProxyActivity.java         # 🆕 UI автоматических прокси
    │
    └── res/layout/
        ├── activity_proxy_settings.xml    # Layout UI
        └── activity_auto_proxy.xml        # 🆕 Layout автопрокси

.github/workflows/
├── build_proxy_apk.yml                    # Полная сборка
└── build_simple_proxy_apk.yml             # Простая сборка

BUILD_APK_GUIDE.md                         # Руководство по локальной сборке
GITHUB_ACTIONS_BUILD.md                    # Руководство по GitHub Actions
CHECKLIST.md                               # Чеклист задач
AUTO_PROXY_GUIDE.md                        # 🆕 Руководство по автопрокси
```

## 🔧 Что нужно сделать

### Минимальные изменения (3 файла):

1. **chrome/browser/BUILD.gn** - добавить зависимость на proxy
2. **chrome/android/BUILD.gn** - добавить Java файлы
3. **chrome/android/java/AndroidManifest.xml** - зарегистрировать Activity

**Подробнее:** [CHECKLIST.md](CHECKLIST.md)

## 📱 Использование

### Способ 1: Автоматические бесплатные прокси (НОВОЕ!)

1. Откройте браузер
2. Меню (⋮) → Settings → Proxy Settings
3. Нажмите **"🌐 Бесплатные прокси (авто)"**
4. Выберите прокси из списка
5. Прокси автоматически активируется

**Подробнее:** [AUTO_PROXY_GUIDE.md](AUTO_PROXY_GUIDE.md)

### Способ 2: Ручная настройка прокси:

1. Откройте браузер
2. Меню (⋮) → Settings
3. Proxy Settings
4. Включите прокси
5. Настройте:
   - **Type**: HTTP / HTTPS / SOCKS4 / SOCKS5
   - **Host**: адрес прокси сервера
   - **Port**: порт (обычно 8080, 1080, 3128)
   - **Username**: имя пользователя (опционально)
   - **Password**: пароль (опционально)
6. Нажмите **Test** для проверки
7. Нажмите **Save**

### Проверка работы:

Откройте в браузере:
- https://whatismyipaddress.com/
- https://whoer.net/
- https://2ip.ru/

Ваш IP должен измениться на IP прокси сервера.

## 🎯 Примеры настройки

### HTTP прокси:
```
Type: HTTP
Host: proxy.example.com
Port: 8080
```

### SOCKS5 с аутентификацией:
```
Type: SOCKS5
Host: socks.example.com
Port: 1080
Username: myuser
Password: mypass
```

### HTTPS прокси:
```
Type: HTTPS
Host: secure-proxy.example.com
Port: 443
```

## 🏗️ Архитектура

```
┌─────────────────────────────────────────┐
│         Android UI (Java)               │
│  ┌─────────────────────────────────┐   │
│  │  ProxySettingsActivity          │   │
│  │  - UI для настройки прокси      │   │
│  └─────────────────────────────────┘   │
│              ↓                          │
│  ┌─────────────────────────────────┐   │
│  │  ProxyManager                   │   │
│  │  - Управление настройками       │   │
│  │  - Сохранение в SharedPrefs     │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
              ↓ JNI
┌─────────────────────────────────────────┐
│         Native Layer (C++)              │
│  ┌─────────────────────────────────┐   │
│  │  ProxyManagerAndroid            │   │
│  │  - JNI мост                     │   │
│  └─────────────────────────────────┘   │
│              ↓                          │
│  ┌─────────────────────────────────┐   │
│  │  KiwiProxyConfigService         │   │
│  │  - Интеграция с Chromium        │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      Chromium Network Stack             │
│  - ProxyResolutionService               │
│  - URLRequestContext                    │
│  - HTTP/SOCKS клиенты                   │
└─────────────────────────────────────────┘
```

## 📊 Поддерживаемые типы прокси

| Тип | Протокол | Порт по умолчанию | Аутентификация | UDP |
|-----|----------|-------------------|----------------|-----|
| HTTP | HTTP/1.1 | 8080, 3128 | ✅ | ❌ |
| HTTPS | HTTPS | 443 | ✅ | ❌ |
| SOCKS4 | SOCKS v4 | 1080 | ❌ | ❌ |
| SOCKS5 | SOCKS v5 | 1080 | ✅ | ✅ |

## 🔒 Безопасность

- Пароли хранятся в SharedPreferences
- Рекомендуется использовать EncryptedSharedPreferences для production
- Поддержка HTTPS прокси для защищенного соединения
- Валидация вводимых данных

## 🐛 Известные ограничения

1. Аутентификация SOCKS4 не поддерживается (ограничение протокола)
2. Смена прокси требует перезагрузки сетевого стека
3. PAC (Proxy Auto-Config) файлы пока не поддерживаются

## 📈 Планы развития

- [ ] Поддержка PAC файлов
- [ ] Список прокси серверов
- [ ] Автоматическое переключение прокси
- [ ] Ротация прокси
- [ ] Статистика использования
- [ ] Геолокация прокси
- [ ] Проверка скорости прокси

## 🤝 Вклад в проект

Приветствуются pull requests! Для крупных изменений сначала откройте issue.

## 📄 Лицензия

Этот проект основан на Kiwi Browser и Chromium, которые распространяются под BSD-style лицензией.

## 📚 Документация

- [CHECKLIST.md](CHECKLIST.md) - Чеклист задач
- [BUILD_APK_GUIDE.md](BUILD_APK_GUIDE.md) - Руководство по локальной сборке
- [GITHUB_ACTIONS_BUILD.md](GITHUB_ACTIONS_BUILD.md) - Сборка через GitHub Actions
- [chrome/browser/proxy/README_PROXY_INTEGRATION.md](chrome/browser/proxy/README_PROXY_INTEGRATION.md) - Детальная документация
- [chrome/browser/proxy/INTEGRATION_EXAMPLE.md](chrome/browser/proxy/INTEGRATION_EXAMPLE.md) - Примеры интеграции

## 💬 Поддержка

Если возникли вопросы:
1. Прочитайте документацию
2. Проверьте [CHECKLIST.md](CHECKLIST.md)
3. Создайте issue в репозитории

## ⭐ Благодарности

- Kiwi Browser team за отличный браузер
- Chromium project за мощный движок
- Сообщество за поддержку и feedback

---

**Сделано с ❤️ для свободного интернета**
