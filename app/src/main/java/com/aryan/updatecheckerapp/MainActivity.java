package com.aryan.updatecheckerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private TextView versionText;
    private Button checkBtn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        versionText = findViewById(R.id.versionText);
        checkBtn = findViewById(R.id.checkUpdateBtn);
        progressBar = findViewById(R.id.progressBar);

        String currentVersion = getVersionName();
        versionText.setText("Current Version: " + currentVersion);

        checkBtn.setOnClickListener(v -> checkUpdateFromServer(currentVersion));
    }

    private String getVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    private void checkUpdateFromServer(String currentVersion) {
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        checkBtn.setEnabled(false);

        new Thread(() -> {
            try {
                JSONObject json = getJsonObject();
                String latestVersion = json.getString("version");
                String downloadUrl = json.getString("url");

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    checkBtn.setEnabled(true);

                    if (!currentVersion.equals(latestVersion)) {
                        showUpdateDialog(latestVersion, downloadUrl);
                    } else {
                        showNoUpdateDialog();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    checkBtn.setEnabled(true);
                    showErrorDialog();
                });
            }
        }).start();
    }

    @NonNull
    private static JSONObject getJsonObject() throws IOException, JSONException {
        URL url = new URL("server-url/update.json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        InputStream in = connection.getInputStream();
        Scanner scanner = new Scanner(in).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";

        return new JSONObject(response);
    }

    private void showUpdateDialog(String latestVersion, String updateUrl) {
        new AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("A new version (" + latestVersion + ") is available. Would you like to update now?")
                .setPositiveButton("Update", (dialog, which) -> {
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

    private void showErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Could not check for updates. Please try again later.")
                .setPositiveButton("OK", null)
                .show();
    }
}

/* JSON Format:
    {
        "version": "1.0",
        "url": "server-url/UpdateCheckerApp.apk"
    }
 */