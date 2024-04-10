package com.sithum.mediatracker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout storage, boot;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int BOOT_PERMISSION_CODE = 102;
    private AppCompatButton btnhide, btnstart, btnstop;
    private boolean appHidden = false;

    private static final int REQUEST_CODE_PICK_DIRECTORY = 1;
    private static final int REQUEST_CODE_PICK_DESTINATION_DIRECTORY = 2;

    TextView sourcedirectorypath, destinationdirectorypath;
    String sourcedirectory, destinationdirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = findViewById(R.id.storagepermission);
        boot = findViewById(R.id.bootpermission);
        btnhide = findViewById(R.id.btnhide);
        btnstart = findViewById(R.id.btnstart);
        btnstop = findViewById(R.id.btnstop);

        sourcedirectorypath = findViewById(R.id.sourcedirectorypath);
        destinationdirectorypath = findViewById(R.id.destinationdirectorypath);

        findViewById(R.id.sourcefolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDirectoryPicker();
            }
        });

        findViewById(R.id.destinationfolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDestinationDirectoryPicker();
            }
        });

        btnstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to start the service
                Intent serviceIntent = new Intent(MainActivity.this, MainService.class);

                // Add the source and destination folder paths as extras to the intent
                serviceIntent.putExtra("sourceDir", sourcedirectory);
                serviceIntent.putExtra("destDir", destinationdirectory);

                // Start the service with the intent
                startService(serviceIntent);
            }
        });

        btnstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent for the service you want to stop
                Intent serviceIntent = new Intent(MainActivity.this, MainService.class);

                // Stop the service using the intent
                stopService(serviceIntent);
            }
        });


        updateStorageVisibility();
        updateBootVisibility();

        storage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestStoragePermission();
            }
        });

        boot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestBootPermission();
            }
        });

        btnhide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideAppIcon();
            }
        });
    }

    private void openDirectoryPicker() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE_PICK_DIRECTORY);

    }

    private void openDestinationDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_PICK_DESTINATION_DIRECTORY);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri selectedUri = data.getData();
                Log.d("DirectoryPickerActivity", "Selected URI: " + selectedUri.toString());

                // Convert the URI to a DocumentFile
                DocumentFile documentFile = DocumentFile.fromTreeUri(this, selectedUri);

                // Get the human-readable path
                String selectedDirPath = null;
                if (documentFile != null && documentFile.exists()) {
                    selectedDirPath = getFullPathFromTreeUri(selectedUri,requestCode);
                }

                Log.d("DirectoryPickerActivity", "Selected directory path: " + selectedDirPath);

                // Now you can use 'selectedDirPath' for file operations like copying files
                if (selectedDirPath != null) {
                    Toast.makeText(this, "Selected directory: " + selectedDirPath, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to get directory path", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // Handle cases where the directory picking was canceled or unsuccessful
            Toast.makeText(this, "Directory picking canceled", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFullPathFromTreeUri(Uri treeUri, int requestCode) {
        String selectedDirPath = null;
        String documentId = DocumentsContract.getTreeDocumentId(treeUri);
        if (documentId != null) {
            String[] split = documentId.split(":");
            if (split.length > 1) {
                String storageId = split[0];
                String path = split[1];
                // Get the base path of storage
                File externalStorage = Environment.getExternalStorageDirectory();
                String basePath = externalStorage.getAbsolutePath();

                // Combine the base path with the chosen directory path
                selectedDirPath = basePath + "/" + path;
            }
        }
        if (requestCode == REQUEST_CODE_PICK_DIRECTORY) {
            sourcedirectorypath.setText(selectedDirPath);
            sourcedirectory = selectedDirPath;
        } else if (requestCode == REQUEST_CODE_PICK_DESTINATION_DIRECTORY) {
            destinationdirectorypath.setText(selectedDirPath);
            destinationdirectory = selectedDirPath;
        }

        return selectedDirPath;
    }









    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    STORAGE_PERMISSION_CODE);
        }
    }

    private void requestBootPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED},
                    BOOT_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            updateStorageVisibility();
        } else if (requestCode == BOOT_PERMISSION_CODE) {
            updateBootVisibility();
        }
    }

    private void updateStorageVisibility() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            storage.setVisibility(View.GONE);
        } else {
            storage.setVisibility(View.VISIBLE);
        }
    }

    private void updateBootVisibility() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) == PackageManager.PERMISSION_GRANTED) {
            boot.setVisibility(View.GONE);
        } else {
            boot.setVisibility(View.VISIBLE);
        }
    }

    private void hideAppIcon() {
        if (!appHidden) { // Check if app icon is not already hidden
            PackageManager p = getPackageManager();
            ComponentName componentName = new ComponentName(this, MainActivity.class);
            p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            Toast.makeText(this, "App icon hidden", Toast.LENGTH_SHORT).show();
            appHidden = true; // Update appHidden flag
        }
    }
}
