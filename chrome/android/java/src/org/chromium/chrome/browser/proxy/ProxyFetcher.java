// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.proxy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.chromium.base.ContextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Автоматическое получение бесплатных прокси из API
 */
public class ProxyFetcher {
    private static final String API_KEY = "019c5236e4417ddb8e9247c61c33336a";
    private static final String BASE_URL = "https://api.getfreeproxy.com/v1/proxies";
    
    private static final String PREF_NAME = "proxy_fetcher";
    private static final String PREF_LAST_UPDATE = "last_update";
    private static final String PREF_CACHED_PROXIES = "cached_proxies";
    
    private static final long UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // 24 часа
    
    private static final String[] COUNTRIES = {"DE", "US", "SE", "FI"}; // Германия, США, Швеция, Финляндия
    private static final int PAGES_PER_COUNTRY = 3;
    
    public interface FetchCallback {
        void onSuccess(List<ProxyInfo> proxies);
        void onError(String error);
    }
    
    public static class ProxyInfo {
        public String ip;
        public int port;
        public String protocol;
        public String country;
        public double uptime;
        public double responseTime;
        public String anonymity;
        
        public ProxyInfo(String ip, int port, String protocol, String country, 
                        double uptime, double responseTime, String anonymity) {
            this.ip = ip;
            this.port = port;
            this.protocol = protocol;
            this.country = country;
            this.uptime = uptime;
            this.responseTime = responseTime;
            this.anonymity = anonymity;
        }
        
        public String toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("ip", ip);
                obj.put("port", port);
                obj.put("protocol", protocol);
                obj.put("country", country);
                obj.put("uptime", uptime);
                obj.put("responseTime", responseTime);
                obj.put("anonymity", anonymity);
                return obj.toString();
            } catch (Exception e) {
                return "{}";
            }
        }
        
        public static ProxyInfo fromJson(String json) {
            try {
                JSONObject obj = new JSONObject(json);
                return new ProxyInfo(
                    obj.getString("ip"),
                    obj.getInt("port"),
                    obj.getString("protocol"),
                    obj.getString("country"),
                    obj.getDouble("uptime"),
                    obj.getDouble("responseTime"),
                    obj.getString("anonymity")
                );
            } catch (Exception e) {
                return null;
            }
        }
    }
    
    public static void fetchProxies(FetchCallback callback) {
        // Проверяем, нужно ли обновление
        SharedPreferences prefs = getPreferences();
        long lastUpdate = prefs.getLong(PREF_LAST_UPDATE, 0);
        long now = System.currentTimeMillis();
        
        if (now - lastUpdate < UPDATE_INTERVAL) {
            // Используем кэшированные прокси
            List<ProxyInfo> cached = getCachedProxies();
            if (!cached.isEmpty()) {
                callback.onSuccess(cached);
                return;
            }
        }
        
        // Запускаем обновление в фоне
        new Thread(() -> {
            try {
                List<ProxyInfo> allProxies = new ArrayList<>();
                
                // Получаем прокси из каждой страны
                for (String country : COUNTRIES) {
                    for (int page = 1; page <= PAGES_PER_COUNTRY; page++) {
                        List<ProxyInfo> proxies = fetchFromAPI(country, page);
                        if (proxies != null) {
                            allProxies.addAll(proxies);
                        }
                        // Небольшая задержка между запросами
                        Thread.sleep(500);
                    }
                }
                
                // Фильтруем качественные прокси
                List<ProxyInfo> qualityProxies = filterQualityProxies(allProxies);
                
                // Проверяем прокси
                List<ProxyInfo> workingProxies = verifyProxies(qualityProxies);
                
                // Сохраняем в кэш
                cacheProxies(workingProxies);
                prefs.edit().putLong(PREF_LAST_UPDATE, now).apply();
                
                // Возвращаем результат в UI потоке
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(workingProxies);
                });
                
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onError("Ошибка получения прокси: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private static List<ProxyInfo> fetchFromAPI(String country, int page) {
        List<ProxyInfo> proxies = new ArrayList<>();
        
        try {
            String urlString = BASE_URL + "?country=" + country + "&page=" + page;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Парсим JSON
                JSONArray jsonArray = new JSONArray(response.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    ProxyInfo proxy = new ProxyInfo(
                        obj.getString("ip"),
                        obj.getInt("port"),
                        obj.optString("protocol", "http"),
                        obj.optString("countryCode", country),
                        obj.optDouble("uptime", 0),
                        obj.optDouble("responseTime", 999),
                        obj.optString("anonymity", "unknown")
                    );
                    proxies.add(proxy);
                }
            }
            conn.disconnect();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return proxies;
    }
    
    private static List<ProxyInfo> filterQualityProxies(List<ProxyInfo> proxies) {
        List<ProxyInfo> filtered = new ArrayList<>();
        
        for (ProxyInfo proxy : proxies) {
            // Фильтруем: uptime > 80%, responseTime < 1s
            if (proxy.uptime > 80 && proxy.responseTime < 1.0) {
                filtered.add(proxy);
            }
        }
        
        return filtered;
    }
    
    private static List<ProxyInfo> verifyProxies(List<ProxyInfo> proxies) {
        List<ProxyInfo> working = new ArrayList<>();
        
        // Проверяем первые 20 прокси (чтобы не тратить много времени)
        int toCheck = Math.min(proxies.size(), 20);
        
        for (int i = 0; i < toCheck; i++) {
            ProxyInfo proxy = proxies.get(i);
            if (testProxy(proxy)) {
                working.add(proxy);
            }
        }
        
        return working;
    }
    
    private static boolean testProxy(ProxyInfo proxy) {
        try {
            // Простая проверка подключения
            java.net.Proxy netProxy = new java.net.Proxy(
                java.net.Proxy.Type.HTTP,
                new java.net.InetSocketAddress(proxy.ip, proxy.port)
            );
            
            URL url = new URL("http://www.google.com");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(netProxy);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("HEAD");
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void cacheProxies(List<ProxyInfo> proxies) {
        try {
            JSONArray array = new JSONArray();
            for (ProxyInfo proxy : proxies) {
                array.put(new JSONObject(proxy.toJson()));
            }
            
            SharedPreferences prefs = getPreferences();
            prefs.edit().putString(PREF_CACHED_PROXIES, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static List<ProxyInfo> getCachedProxies() {
        List<ProxyInfo> proxies = new ArrayList<>();
        
        try {
            SharedPreferences prefs = getPreferences();
            String json = prefs.getString(PREF_CACHED_PROXIES, "[]");
            JSONArray array = new JSONArray(json);
            
            for (int i = 0; i < array.length(); i++) {
                ProxyInfo proxy = ProxyInfo.fromJson(array.getJSONObject(i).toString());
                if (proxy != null) {
                    proxies.add(proxy);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return proxies;
    }
    
    private static SharedPreferences getPreferences() {
        return ContextUtils.getApplicationContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static void forceUpdate(FetchCallback callback) {
        // Сбрасываем время последнего обновления
        getPreferences().edit().putLong(PREF_LAST_UPDATE, 0).apply();
        fetchProxies(callback);
    }
}
