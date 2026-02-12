// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.proxy;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity для выбора автоматических бесплатных прокси
 */
public class AutoProxyActivity extends AppCompatActivity {
    private ListView mProxyListView;
    private ProgressBar mProgressBar;
    private TextView mStatusText;
    private Button mRefreshButton;
    
    private List<ProxyFetcher.ProxyInfo> mProxies = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // TODO: Создать layout
        // setContentView(R.layout.activity_auto_proxy);
        
        initViews();
        loadProxies();
    }
    
    private void initViews() {
        // TODO: После создания layout раскомментировать
        // mProxyListView = findViewById(R.id.proxy_list);
        // mProgressBar = findViewById(R.id.progress_bar);
        // mStatusText = findViewById(R.id.status_text);
        // mRefreshButton = findViewById(R.id.refresh_button);
        
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        if (mProxyListView != null) {
            mProxyListView.setAdapter(mAdapter);
            mProxyListView.setOnItemClickListener(this::onProxySelected);
        }
        
        if (mRefreshButton != null) {
            mRefreshButton.setOnClickListener(v -> forceRefresh());
        }
    }
    
    private void loadProxies() {
        showLoading(true);
        updateStatus("Загрузка прокси...");
        
        ProxyFetcher.fetchProxies(new ProxyFetcher.FetchCallback() {
            @Override
            public void onSuccess(List<ProxyFetcher.ProxyInfo> proxies) {
                mProxies = proxies;
                updateProxyList();
                showLoading(false);
                
                if (proxies.isEmpty()) {
                    updateStatus("Прокси не найдены. Попробуйте обновить.");
                } else {
                    updateStatus("Найдено " + proxies.size() + " рабочих прокси");
                }
            }
            
            @Override
            public void onError(String error) {
                showLoading(false);
                updateStatus("Ошибка: " + error);
                Toast.makeText(AutoProxyActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void forceRefresh() {
        showLoading(true);
        updateStatus("Обновление списка прокси...");
        
        ProxyFetcher.forceUpdate(new ProxyFetcher.FetchCallback() {
            @Override
            public void onSuccess(List<ProxyFetcher.ProxyInfo> proxies) {
                mProxies = proxies;
                updateProxyList();
                showLoading(false);
                updateStatus("Обновлено! Найдено " + proxies.size() + " прокси");
                Toast.makeText(AutoProxyActivity.this, 
                    "Список обновлен", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String error) {
                showLoading(false);
                updateStatus("Ошибка обновления: " + error);
                Toast.makeText(AutoProxyActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void updateProxyList() {
        mAdapter.clear();
        
        for (ProxyFetcher.ProxyInfo proxy : mProxies) {
            String displayText = String.format(
                "%s:%d (%s)\n↑ %.0f%% | ⚡ %.0fms | %s",
                proxy.ip,
                proxy.port,
                getCountryName(proxy.country),
                proxy.uptime,
                proxy.responseTime * 1000,
                proxy.protocol.toUpperCase()
            );
            mAdapter.add(displayText);
        }
        
        mAdapter.notifyDataSetChanged();
    }
    
    private void onProxySelected(AdapterView<?> parent, View view, int position, long id) {
        if (position < 0 || position >= mProxies.size()) {
            return;
        }
        
        ProxyFetcher.ProxyInfo proxy = mProxies.get(position);
        
        // Создаем конфигурацию прокси
        ProxyConfig config = new ProxyConfig();
        config.setEnabled(true);
        config.setHost(proxy.ip);
        config.setPort(proxy.port);
        
        // Определяем тип прокси
        int proxyType = ProxyConfig.ProxyType.HTTP;
        String protocol = proxy.protocol.toLowerCase();
        if (protocol.contains("https")) {
            proxyType = ProxyConfig.ProxyType.HTTPS;
        } else if (protocol.contains("socks5")) {
            proxyType = ProxyConfig.ProxyType.SOCKS5;
        } else if (protocol.contains("socks4")) {
            proxyType = ProxyConfig.ProxyType.SOCKS4;
        }
        config.setType(proxyType);
        
        // Применяем конфигурацию
        ProxyManager.getInstance().setProxyConfig(config);
        
        Toast.makeText(this, 
            "Прокси активирован: " + proxy.ip + ":" + proxy.port, 
            Toast.LENGTH_SHORT).show();
        
        finish();
    }
    
    private void showLoading(boolean show) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (mRefreshButton != null) {
            mRefreshButton.setEnabled(!show);
        }
        if (mProxyListView != null) {
            mProxyListView.setEnabled(!show);
        }
    }
    
    private void updateStatus(String status) {
        if (mStatusText != null) {
            mStatusText.setText(status);
        }
    }
    
    private String getCountryName(String code) {
        switch (code.toUpperCase()) {
            case "DE": return "🇩🇪 Германия";
            case "US": return "🇺🇸 США";
            case "SE": return "🇸🇪 Швеция";
            case "FI": return "🇫🇮 Финляндия";
            default: return code;
        }
    }
}
