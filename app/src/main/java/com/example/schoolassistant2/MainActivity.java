package com.example.schoolassistant2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.schoolassistant2.databinding.ActivityMainBinding;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateQuestions();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new StudyFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_study) {
                replaceFragment(new StudyFragment());
            } else if (id == R.id.navigation_papers) {
                replaceFragment(new PapersFragment());
            } else if (id == R.id.navigation_scan) {
                replaceFragment(new ScanFragment());
            } else if (id == R.id.navigation_analysis) {
                replaceFragment(new AnalysisFragment());
            }

            return true;
        });
    }

    private void updateQuestions() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check if connected to WiFi
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        boolean isWiFiConnected = capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);

        if (isWiFiConnected) {
            new DownloadJsonTask().execute("https://schoolassistantapi-7yv2y48s.b4a.run/getquestions");
        } else {
            Log.d("UpdateQuestions", "WiFi not available");
        }
    }

    private class DownloadJsonTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... urls) {
            String urlString = urls[0];
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                String fileDir = getFilesDir().getPath();
                File outputFile = new File(fileDir, "q_and_a.json");
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }

                fileOutputStream.close();
                inputStream.close();
                urlConnection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("DownloadJsonTask", "Error downloading JSON: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    // Change page based on where you navigate to in bottom navigation bar
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

}
