/*
 * Copyright (C) 2016 Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.gpstest.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.gpstest.Application;
import com.android.gpstest.BuildConfig;
import com.android.gpstest.R;
import com.android.gpstest.library.util.IOUtils;

public class HelpActivity extends AppCompatActivity {

    public static final String TAG = "HelpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean darkTheme = Application.Companion.getPrefs().getBoolean(
                getString(R.string.pref_key_dark_theme), false);
        setTheme(darkTheme ? R.style.AppTheme_Dark_NoActionBar : R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        setSupportActionBar(findViewById(R.id.help_toolbar));
        setTitle(R.string.pref_about_category_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Keep the toolbar and scrolling content inside the system bars and display cutouts.
        View content = findViewById(R.id.activity_help);
        int left = content.getPaddingLeft();
        int top = content.getPaddingTop();
        int right = content.getPaddingRight();
        int bottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(left + bars.left, top + bars.top,
                    right + bars.right, bottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);

        TextView versionView = (TextView) findViewById(R.id.app_version);
        TextView helpView = (TextView) findViewById(R.id.help_text);

        String versionString = "";
        int versionCode = 0;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionString = info.versionName;
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder version = new StringBuilder();
        // Version info
        version.append("v")
                .append(versionString)
                .append(" (")
                .append(versionCode)
                .append("-" + BuildConfig.FLAVOR + ")\n");

        version.append("GNSS HW Year: " + IOUtils.getGnssHardwareYear(Application.Companion.getApp()) + "\n");
        version.append("GNSS HW Name: " + IOUtils.getGnssHardwareModelName(Application.Companion.getApp()) + "\n");

        String versionRelease = Build.VERSION.RELEASE;
        version.append("Platform: " + versionRelease + "\n");
        int apiLevel = Build.VERSION.SDK_INT;
        version.append("API Level: " + apiLevel + "\n");

        versionView.setText(version.toString());

        helpView.setText(R.string.help_text);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // For dynamically changing the locale
        super.attachBaseContext(Application.Companion.getLocaleManager().setLocale(base));
    }
}
