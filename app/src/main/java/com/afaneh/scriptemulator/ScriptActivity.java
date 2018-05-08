package com.afaneh.scriptemulator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import com.topjohnwu.superuser.CallbackList;
import com.topjohnwu.superuser.Shell;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

public class ScriptActivity extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_CODE = 0;
    public static final int FILE_PICKER_REQUEST_CODE = 1;
    private static final String TAG = "Script";
    private List<String> consoleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView console = findViewById(R.id.console);
        EditText input = findViewById(R.id.cmd_input);
        ScrollView sv = findViewById(R.id.sv);

        Button async_cmd = findViewById(R.id.async_cmd);
        Button close_shell = findViewById(R.id.close_shell);
        Button clear = findViewById(R.id.clear);
        Button pick_script = findViewById(R.id.pick_script);

        // Pick a script from internal storage
        pick_script.setOnClickListener(v -> checkPermissionsAndOpenFilePicker());

        input.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)){
                async_cmd.performClick();
            }
            return false;
        });

        // Run the shell command in the input box asynchronously.
        // Also demonstrates that Async.Callback works
        async_cmd.setOnClickListener(v -> {
            Shell.Async.sh(consoleList, consoleList,
                    (out, err) -> Log.d(TAG, "in_async_callback"),
                    input.getText().toString());
            input.setText("");
        });

        // Closing a shell is always synchronous
        close_shell.setOnClickListener(v -> {
            try {
                Shell.getShell().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        clear.setOnClickListener(v -> consoleList.clear());

        // We create a CallbackList to update the UI with the Shell output
        consoleList = new CallbackList<String>() {
            private StringBuilder builder = new StringBuilder();

            @Override
            public void onAddElement(String s) {
                builder.append(s).append('\n');
                console.setText(builder);
                sv.postDelayed(() -> sv.fullScroll(ScrollView.FOCUS_DOWN), 10);
            }

            @Override
            public void clear() {
                builder = new StringBuilder();
                handler.post(() -> console.setText(""));
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_script, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(ScriptActivity.this);
            dialog.setCancelable(false);
            dialog.setIcon(R.mipmap.ic_launcher);
            dialog.setTitle("About");
            dialog.setMessage(getString(R.string.what_is_safestrap) + "\n\n" + getString(R.string.special_thanks) + "\n\n" + getString(R.string.copyright_info));
            dialog.setPositiveButton("OK", (dialog1, id1) -> {
                //Action for "OK".
            });

            final AlertDialog alert = dialog.create();
            alert.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkPermissionsAndOpenFilePicker() {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                showError();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            openFilePicker();
        }
    }

    private void showError() {
        Toast.makeText(this, "Allow external storage reading", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker();
                } else {
                    showError();
                }
            }
        }
    }

    private void openFilePicker() {
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(FILE_PICKER_REQUEST_CODE)
                .withFilter(Pattern.compile(".*\\.sh$")) // Filtering shell script files
                .withHiddenFiles(true)
                .withTitle("Sample title")
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            String path = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            InputStream is_path = null;
            try {
                is_path = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (is_path != null) {
                Log.d("Path: ", path);
                Toast.makeText(this, "Picked file: " + path, Toast.LENGTH_LONG).show();
                Shell.Sync.loadScript(consoleList, is_path);
            }
        }
    }
}