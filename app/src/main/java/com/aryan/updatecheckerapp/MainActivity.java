package com.aryan.updatecheckerapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView versionText = findViewById(R.id.versionText);
        Button checkBtn = findViewById(R.id.checkUpdateBtn);

        // Get version from BuildConfig
        String currentVersion = AppConfig.VERSION_NAME;
        versionText.setText("Current Version: " + currentVersion);

        checkBtn.setOnClickListener(v -> checkForUpdate(currentVersion));
    }

    private void checkForUpdate(String currentVersion) {
        String latestVersion = "2.0";
        String updateUrl = ""; // server-url

        if (!currentVersion.equals(latestVersion)) {
            showUpdateDialog(latestVersion, updateUrl);
        } else {
            showNoUpdateDialog();
        }
    }

    private void showUpdateDialog(String latestVersion, String updateUrl) {
        new AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("A new version (" + latestVersion + ") is available. Would you like to update now?")
                .setPositiveButton("Update", (dialog, which) -> {
                    // Open browser to download the update
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(updateUrl));
                    startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void showNoUpdateDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Update")
                .setMessage("You are on the latest version.")
                .setPositiveButton("OK", null)
                .show();
    }
}