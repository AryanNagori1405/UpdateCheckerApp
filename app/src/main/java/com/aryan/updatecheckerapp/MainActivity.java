package com.aryan.updatecheckerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private TextView versionText;
    private ProgressBar progressBar;
    private TextView statusText;
    private long downloadId;
    private String downloadUrl;

    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        versionText = findViewById(R.id.versionText);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);

        // Show current version
        String currentVersion = getVersionName();
        versionText.setText("Current Version: " + currentVersion);

        // Register receiver for download completion
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // Start automatic update check immediately
        startAutoUpdateCheck(currentVersion);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(onDownloadComplete);
        } catch (Exception e) {
            // Receiver was not registered, ignore
        }
    }

    @SuppressLint("SetTextI18n")
    private void startAutoUpdateCheck(String currentVersion) {
        // Show checking status
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Checking for updates...");
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // Get update info from server
                JSONObject json = getJsonObject();
                String latestVersion = json.getString("version");
                downloadUrl = json.getString("url");
                boolean mandatory = json.optBoolean("mandatory", true);

                runOnUiThread(() -> {
                    if (!currentVersion.equals(latestVersion)) {
                        // Update available - start download automatically
                        statusText.setText("Update available! Downloading...");
                        startAutomaticDownload(latestVersion);
                    } else {
                        // Already up to date
                        statusText.setText("App is up to date");
                        progressBar.setVisibility(View.GONE);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("Update check failed");
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not check for updates", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String getVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    @SuppressLint("SetTextI18n")
    private void startAutomaticDownload(String latestVersion) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("Updating to version " + latestVersion);
            request.setDescription("Downloading update...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app_update.apk");

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadId = downloadManager.enqueue(request);
                statusText.setText("Downloading update...");
            }

        } catch (Exception e) {
            statusText.setText("Download failed");
            Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == id) {
                // Download complete - install automatically
                statusText.setText("Installing update...");
                installUpdate();
            }
        }
    };

    @SuppressLint("SetTextI18n")
    private void installUpdate() {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "app_update.apk");

            if (file.exists()) {
                Uri apkUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    apkUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
                } else {
                    apkUri = Uri.fromFile(file);
                }

                Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                install.setData(apkUri);
                install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                startActivity(install);

                // Close the app to allow installation
                finish();
            } else {
                statusText.setText("Update file not found");
            }
        } catch (Exception e) {
            statusText.setText("Installation failed");
            Toast.makeText(this, "Installation failed", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private static JSONObject getJsonObject() throws IOException, JSONException {
        // Replace with your actual server URL
        URL url = new URL("https://your-server.com/update.json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.connect();

        InputStream in = connection.getInputStream();
        Scanner scanner = new Scanner(in).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";

        connection.disconnect();
        return new JSONObject(response);
    }
}

/* JSON Format
    {
        "version": "versionOnServer",
        "url": "https://your-server.com/your-app.apk"
    }
*/