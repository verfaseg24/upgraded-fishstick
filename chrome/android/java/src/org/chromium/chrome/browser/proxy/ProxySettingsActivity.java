// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.proxy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity для настройки прокси
 */
public class ProxySettingsActivity extends AppCompatActivity {
    private CheckBox mEnableProxyCheckbox;
    private Spinner mProxyTypeSpinner;
    private EditText mHostEditText;
    private EditText mPortEditText;
    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private Button mSaveButton;
    private Button mTestButton;
    private Button mAutoProxyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proxy_settings);
        
        initViews();
        loadCurrentConfig();
        setupListeners();
    }

    private void initViews() {
        mEnableProxyCheckbox = findViewById(R.id.enable_proxy_checkbox);
        mProxyTypeSpinner = findViewById(R.id.proxy_type_spinner);
        mHostEditText = findViewById(R.id.host_edittext);
        mPortEditText = findViewById(R.id.port_edittext);
        mUsernameEditText = findViewById(R.id.username_edittext);
        mPasswordEditText = findViewById(R.id.password_edittext);
        mSaveButton = findViewById(R.id.save_button);
        mTestButton = findViewById(R.id.test_button);
        mAutoProxyButton = findViewById(R.id.auto_proxy_button);
        
        // Настройка spinner с типами прокси
        String[] proxyTypes = {"Direct", "HTTP", "HTTPS", "SOCKS4", "SOCKS5"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, proxyTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mProxyTypeSpinner.setAdapter(adapter);
    }

    private void loadCurrentConfig() {
        ProxyConfig config = ProxyManager.getInstance().getCurrentConfig();
        
        mEnableProxyCheckbox.setChecked(config.isEnabled());
        mProxyTypeSpinner.setSelection(config.getType());
        mHostEditText.setText(config.getHost());
        mPortEditText.setText(String.valueOf(config.getPort()));
        mUsernameEditText.setText(config.getUsername());
        mPasswordEditText.setText(config.getPassword());
        
        updateFieldsEnabled(config.isEnabled());
    }

    private void setupListeners() {
        mSaveButton.setOnClickListener(v -> saveConfig());
        mTestButton.setOnClickListener(v -> testProxy());
        mAutoProxyButton.setOnClickListener(v -> openAutoProxy());
        
        mEnableProxyCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateFieldsEnabled(isChecked);
        });
    }
    
    private void openAutoProxy() {
        Intent intent = new Intent(this, AutoProxyActivity.class);
        startActivity(intent);
    }
    
    private void testProxy() {
        ProxyConfig config = new ProxyConfig();
        config.setEnabled(true);
        config.setType(mProxyTypeSpinner.getSelectedItemPosition());
        config.setHost(mHostEditText.getText().toString());
        
        String portStr = mPortEditText.getText().toString();
        config.setPort(portStr.isEmpty() ? 8080 : Integer.parseInt(portStr));
        
        Toast.makeText(this, "Тестирование прокси...", Toast.LENGTH_SHORT).show();
        
        ProxyTester.testProxy(config, (success, message) -> {
            runOnUiThread(() -> {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void updateFieldsEnabled(boolean enabled) {
        mProxyTypeSpinner.setEnabled(enabled);
        mHostEditText.setEnabled(enabled);
        mPortEditText.setEnabled(enabled);
        mUsernameEditText.setEnabled(enabled);
        mPasswordEditText.setEnabled(enabled);
    }

    private void saveConfig() {
        ProxyConfig config = new ProxyConfig();
        
        config.setEnabled(mEnableProxyCheckbox.isChecked());
        config.setType(mProxyTypeSpinner.getSelectedItemPosition());
        config.setHost(mHostEditText.getText().toString());
        
        String portStr = mPortEditText.getText().toString();
        config.setPort(portStr.isEmpty() ? 8080 : Integer.parseInt(portStr));
        
        config.setUsername(mUsernameEditText.getText().toString());
        config.setPassword(mPasswordEditText.getText().toString());
        
        ProxyManager.getInstance().setProxyConfig(config);
        finish();
    }
}
