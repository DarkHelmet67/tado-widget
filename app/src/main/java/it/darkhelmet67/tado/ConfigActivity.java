package it.darkhelmet67.tado;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import it.darkhelmet67.tado.network.VolleyCallback;
import it.darkhelmet67.tado.network.VolleyFunctions;
import it.darkhelmet67.tado.utils.DLog;
import it.darkhelmet67.tado.utils.Prefs;
import it.darkhelmet67.tado.utils.UI;

/**
 * @author Luca Ceccatelli
 */
public class ConfigActivity extends AppCompatActivity {

    private static final String TAG = ConfigActivity.class.getSimpleName();

    // WIDGET references
    int idAppWidget = AppWidgetManager.INVALID_APPWIDGET_ID;

    // UI references.
    private Toolbar toolbar;
    private View viewProgress;
    private EditText editTextUsername, editTextPassword, editTextDevice;
    private Spinner spinnerUpdateTime;  // spinnerDevices
    private LinearLayout layoutLogin, layoutConfig;
    private Button buttonDevices, buttonSave;
    private Switch swichTextSize;
    private ListView listDevices;
    private ArrayAdapter<String> adapterDevices;
    private boolean isLogin = true, isDeviceNew = true;

    // VOLLEY LIB
    private VolleyFunctions volleyFunctions;

    private String username, password, device;
    // FIXME LC 2016-01-01 - DEVICE INFO
    private String deviceModel, deviceOsVer;
    private boolean isDebug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tado_config);

        // LOG DEBUG
        isDebug = getResources().getBoolean(R.bool.isDebug);
        DLog.setIsDebugEnabled(isDebug);
        DLog.d(TAG, "onCreate @ConfigActivity");

        // FIXME LC 2015-12-29 - TOOLBAR
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getTitle());

        // WIDGET INFO - if user QUIT CONFIG widget is REMOVED
        setResult(RESULT_CANCELED);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            idAppWidget = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (idAppWidget == AppWidgetManager.INVALID_APPWIDGET_ID) {
                DLog.w(TAG, "onCreate @ConfigActivity - INVALID_APPWIDGET_ID");
                finish();
            } else
                DLog.i(TAG, "onCreate @ConfigActivity - idAppWidget=" + idAppWidget);
        } else {
            DLog.w(TAG, "onCreate @ConfigActivity - NO EXTRAS");
            finish();
        }

        // FIXME LC 2016-01-01 - DEVICE INFO
        deviceModel = android.os.Build.MODEL;           // %DEVICE_MODEL
        deviceOsVer = android.os.Build.VERSION.RELEASE; // %DEVICE_OSVER
        DLog.d(TAG, "onCreate @ConfigActivity - deviceModel=" + deviceModel + ", deviceOsVer=" + deviceOsVer);

        // PREFS data
        username = Prefs.getUsername(ConfigActivity.this);
        password = Prefs.getPassword(ConfigActivity.this);
        device = Prefs.getDeviceName(ConfigActivity.this);
        DLog.d(TAG, "onCreate @ConfigActivity - username=" + username + ", device=" + device);

        // Set up the login form.
        layoutLogin = (LinearLayout) findViewById(R.id.layout_login);
        viewProgress = findViewById(R.id.config_progress);

        // EDITTEXT
        editTextUsername = (EditText) findViewById(R.id.config_username);
        editTextUsername.setText(username);
        editTextPassword = (EditText) findViewById(R.id.config_password);
        editTextPassword.setText(password);

        // SPINNERS
        layoutConfig = (LinearLayout) findViewById(R.id.layout_config);
//        spinnerDevices = (Spinner) findViewById(R.id.spinner_devices);
//        spinnerDevices.setOnItemSelectedListener(new SpinnerOnItemSelectedListener());

        spinnerUpdateTime = (Spinner) findViewById(R.id.spinner_update_time);
        spinnerUpdateTime.setSelection(Prefs.getUpdatePosition(this));
        spinnerUpdateTime.setOnItemSelectedListener(new SpinnerOnItemSelectedListener());

        // TODO 2016-01-06
        listDevices = (ListView) findViewById(R.id.listview_devices);
        editTextDevice = (EditText) findViewById(R.id.config_device);
        editTextDevice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isDeviceNew = !(findDevice(s.toString()) >= 0);
                DLog.d(TAG, "onCreate @ConfigActivity - editTextDevice.onTextChanged - count="
                        + count + ", s=" + s + ", isDeviceNew=" + isDeviceNew);
                editTextDevice.setTextColor(isDeviceNew ? Color.BLACK : Color.RED);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // SWITCH
        swichTextSize = (Switch) findViewById(R.id.swich_text_size);
        swichTextSize.setChecked(Prefs.getSwitchTextSize(this));

        // BUTTONS
        buttonDevices = (Button) findViewById(R.id.config_devices);
        buttonDevices.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DLog.d(TAG, "onCreate @ConfigActivity - buttonDevices.onClick=" + view.toString());

                // GET USERNAME/PASSWORD for SELECTED DEVICE
                View focusView = null;
                boolean cancel = false;
                username = editTextUsername.getText().toString();
                password = editTextPassword.getText().toString();

                // Check for a valid email address.
                if (TextUtils.isEmpty(username)) {
                    editTextUsername.setError(getString(R.string.error_field_required));
                    focusView = editTextUsername;
                    cancel = true;
                }

                // Check for a valid password, if the user entered one.
                if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
                    editTextPassword.setError(getString(R.string.error_invalid_password));
                    focusView = editTextPassword;
                    cancel = true;
                }

                if (cancel) {
                    // There was an error; don't attempt login and focus the first form field with an error.
                    focusView.requestFocus();
                } else {
                    attemptGetDevices();
//                createAppUser();
                }
            }
        });

        buttonSave = (Button) findViewById(R.id.config_save);
        buttonSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DLog.d(TAG, "onCreate @ConfigActivity - buttonSave.onClick=" + view.toString());
                configSave();
            }
        });

        // VOLLEY LIB
        volleyFunctions = new VolleyFunctions(this);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        DLog.d(TAG, "showProgress @ConfigActivity - show=" + show);

        buttonDevices.setEnabled(!show);
        buttonSave.setEnabled(!show);

        viewProgress.setVisibility(show ? View.VISIBLE : View.GONE);

        // LOGIN or CONFIG?
        layoutLogin.setVisibility(isLogin ? View.VISIBLE : View.GONE);
        layoutConfig.setVisibility(isLogin ? View.GONE : View.VISIBLE);
        if(isLogin)
            layoutLogin.setAlpha(show ? 0.25f : 1.0f);
        else
            layoutConfig.setAlpha(show ? 0.25f : 1.0f);
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    private void attemptGetDevices() {
        DLog.d(TAG, "attemptGetDevices @ConfigActivity");

        volleyFunctions.getDevices(username, password, new VolleyCallback() {
            @Override
            public void onSuccess(String result) {
                DLog.d(TAG, "attemptGetDevices @ConfigActivity - result=" + result);
            }

            @Override
            public void onResponse(JSONObject response) {
                DLog.d(TAG, "attemptGetDevices @ConfigActivity - response=" + response.toString());
                // SPINNER DEVICES - CREATE ADAPTER
                setSpinnerDevices(response);
            }

            @Override
            public void onError(VolleyError e) {

                // String error = e.getMessage();
                String error = e.toString();
                DLog.e(TAG, "attemptGetDevices @ConfigActivity - ERROR=" + error);
//                Toast.makeText(ConfigActivity.this, error, Toast.LENGTH_SHORT).show();
                /*
                if(!isDebug) {
                    if (error.contains("AuthFailureError"))
                        error = getString(R.string.error_login);
                    Toast.makeText(ConfigActivity.this, error, Toast.LENGTH_SHORT).show();
                } else {
                    // FIXME 2016-01-12 - ERROR DIALOG
                    NetworkResponse networkResponse = e.networkResponse;
                    String responseString = "";
                    if (networkResponse != null) {
                        try {
                            responseString = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                            error += "\n" + responseString;
                        } catch (UnsupportedEncodingException e1) {
                            e1.printStackTrace();
                        }
                    }
                    UI.showDialog(ConfigActivity.this, UI.AlertType.OK, "tado° Login Error", error, null, null);
                }
                */
            }
        });
    }

    private void setSpinnerDevices(JSONObject response) {
        // SPINNER DEVICES - CREATE ADAPTER
        DLog.d(TAG, "setSpinnerDevices @ConfigActivity");

        try {
            boolean success = response.getBoolean("success");
            if (success) {
                // LC - STORE USERNAME/PASSWORD
                Prefs.setUsername(ConfigActivity.this, username);
                Prefs.setPassword(ConfigActivity.this, password);

                List<String> spinnerArray = new ArrayList<String>();
                JSONArray appUsers = response.getJSONArray("appUsers");
                int position = -1;
                for (int i = 0; i < appUsers.length(); i++) {
                    JSONObject appUser = appUsers.getJSONObject(i);
                    String nickname = appUser.getString("nickname");
                    DLog.d(TAG, "setSpinnerDevices @ConfigActivity - index="
                            + i + ", nickname=" + nickname);
                    spinnerArray.add(nickname);
                    // FIXME LC 2016-01-01 - DEFAULT STARTUP VALUE
                    if(nickname.equalsIgnoreCase(getString(R.string.app_name)))
                        position = i;
                }

                // SHOW SPINNER - HIDE BUTTON
                /*
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        ConfigActivity.this, android.R.layout.simple_spinner_item, spinnerArray);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDevices.setAdapter(adapter);
                // FIXME LC 2016-01-01 - SELECT DEFAULT or SAVED VALUE
                int selection = Prefs.getDevicePosition(ConfigActivity.this);
                if (selection == -1) {
                    if (position != -1) {
                        selection = position;
                    } else {
                        selection = 0;
                    }
                }
                spinnerDevices.setSelection(selection);
                */

                // TODO 2016-01-06 - LISTVIEW
                adapterDevices = new ArrayAdapter<String>(
                        ConfigActivity.this, android.R.layout.simple_list_item_1, spinnerArray);
                listDevices.setAdapter(adapterDevices);
                listDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, final View view,
                                            int position, long id) {
                        DLog.d(TAG, "listDevices.onItemClick @ConfigActivity - position=" + position);
                        String device = (String) parent.getItemAtPosition(position);
                        editTextDevice.setText(device);
                        isDeviceNew = false;
                    }
                });
                // STORED DEVICE
                editTextDevice.setText(device);

                isLogin = false;
                showProgress(false);
            }
        } catch (JSONException e) {
            String error = e.getMessage();
            DLog.e(TAG, "setSpinnerDevices @ConfigActivity - JSONException=" + error);
            e.printStackTrace();
            Toast.makeText(ConfigActivity.this, error, Toast.LENGTH_SHORT).show();
        }
    }

    private int findDevice(String device) {
        DLog.d(TAG, "findDevice @ConfigActivity - device=" + device);

        if(adapterDevices != null)
            for(int i = 0; i < adapterDevices.getCount(); i++)
                if(adapterDevices.getItem(i).toString().equalsIgnoreCase(device))
                    return i;
        return -1;
    }

    private void configSave() {
        DLog.d(TAG, "configSave @ConfigActivity");

        // STORE VALUES in PREFS
//        int devicePosition = spinnerDevices.getSelectedItemPosition();
//        String deviceValue = spinnerDevices.getSelectedItem().toString();
//        DLog.d(TAG, "configSave @ConfigActivity - devicePosition="
//                + devicePosition + ", deviceValue=" + deviceValue);
//        Prefs.setDevicePosition(ConfigActivity.this, devicePosition);
//        Prefs.setDeviceValue(ConfigActivity.this, deviceValue);

        int updatePosition = spinnerUpdateTime.getSelectedItemPosition();
        String updateValue = spinnerUpdateTime.getSelectedItem().toString();
        int[] timeValues = getResources().getIntArray(R.array.array_update_time_values);
        int timeValue = timeValues[updatePosition];
        DLog.d(TAG, "configSave @ConfigActivity - updatePosition=" + updatePosition
                + ", updateValue=" + updateValue + ", timeValue=" + timeValue);
        Prefs.setUpdatePosition(ConfigActivity.this, updatePosition);
        Prefs.setUpdateValue(this, timeValue);

        // SWITCH
        Prefs.setSwitchTextSize(this, swichTextSize.isChecked());

        // TODO 2016-01-06 - NEW or EXISTING user?
        device = editTextDevice.getText().toString();
        if (isDeviceNew) {
            // NEW USER
            createAppUser();
        } else {
            // EXISTING USER
            claimAppUser();
        }
    }

    private void claimAppUser() {
        DLog.d(TAG, "claimAppUser @ConfigActivity");

        showProgress(true);
        volleyFunctions.claimAppUser(username, password, device,
                deviceModel, deviceOsVer, new VolleyCallback() {
                    @Override
                    public void onSuccess(String result) {
                        DLog.d(TAG, "claimAppUser @ConfigActivity - result=" + result);
                    }

                    @Override
                    public void onResponse(JSONObject response) {
                        DLog.d(TAG, "claimAppUser @ConfigActivity - response=" + response.toString());

                        // STORE DEVICE USERNAME/PASSWORD
                        showProgress(false);
                        try {
                            boolean success = response.getBoolean("success");
                            if (success) {
                                String deviceUsername = response.getString("username");
                                String devicePassword = response.getString("password");
                                String nickname = response.getString("nickname");
                                DLog.d(TAG, "claimAppUser @ConfigActivity - deviceUsername="
                                        + deviceUsername + ", devicePassword=" + devicePassword
                                        + ", nickname=" + nickname);
                                Prefs.setDeviceUsername(ConfigActivity.this, deviceUsername);
                                Prefs.setDevicePassword(ConfigActivity.this, devicePassword);
                                Prefs.setDeviceName(ConfigActivity.this, nickname);
                                addWidget();
                            }
                        } catch (JSONException e) {
                            String error = e.getMessage();
                            DLog.e(TAG, "claimAppUser @ConfigActivity - JSONException=" + error);
                            e.printStackTrace();
                            Toast.makeText(ConfigActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(VolleyError e) {
                        // String error = e.getMessage();
                        String error = e.toString();
                        DLog.e(TAG, "claimAppUser @ConfigActivity - ERROR=" + error);
//                        Toast.makeText(ConfigActivity.this, error, Toast.LENGTH_SHORT).show();
                        showProgress(false);
                    }
                });
    }

    private void createAppUser() {
        DLog.d(TAG, "createAppUser @ConfigActivity");

        showProgress(true);
        volleyFunctions.createAppUser(username, password, device, deviceModel, deviceOsVer,
                new VolleyCallback() {
                    @Override
                    public void onSuccess(String result) {
                        DLog.d(TAG, "createAppUser @ConfigActivity - result=" + result);
                    }

                    @Override
                    public void onResponse(JSONObject response) {
                        DLog.d(TAG, "createAppUser @ConfigActivity - response=" + response.toString());

                        // STORE DEVICE USERNAME/PASSWORD
                        showProgress(false);
                        try {
                            boolean success = response.getBoolean("success");
                            if (success) {
                                String deviceUsername = response.getString("username");
                                String devicePassword = response.getString("password");
                                String nickname = response.getString("nickname");
                                DLog.d(TAG, "createAppUser @ConfigActivity - deviceUsername="
                                        + deviceUsername + ", devicePassword=" + devicePassword
                                        + ", nickname=" + nickname);
                                Prefs.setDeviceUsername(ConfigActivity.this, deviceUsername);
                                Prefs.setDevicePassword(ConfigActivity.this, devicePassword);
                                Prefs.setDeviceName(ConfigActivity.this, nickname);
                                addWidget();
                            } else {
                                // FIXME - CONTROLLARE!
                            }
                        } catch (JSONException e) {
                            String error = e.getMessage();
                            DLog.e(TAG, "createAppUser @ConfigActivity - JSONException=" + error);
                            e.printStackTrace();
                            Toast.makeText(ConfigActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(VolleyError e) {
                        // String error = e.getMessage();
                        String error = e.toString();
                        DLog.e(TAG, "createAppUser @ConfigActivity - ERROR=" + error);
                        showProgress(false);
//                        if (error.contains("AuthFailureError"))
//                            error = getString(R.string.error_login);
//                        Toast.makeText(ConfigActivity.this, error, Toast.LENGTH_SHORT).show();
                        showProgress(false);
                    }
                });
    }

    private void addWidget() {
        DLog.d(TAG, "addWidget @ConfigActivity");

        // ADD WIDGET to HOME SCREEN
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, idAppWidget);
        setResult(RESULT_OK, resultValue);

        // CREATE/UPDATE WIDGET
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ConfigActivity.this);
        //                        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.tado_widget);
//                        appWidgetManager.updateAppWidget(idAppWidget, remoteViews);
//                        DLog.d(TAG, "configSave @ConfigActivity - idAppWidget="
//                                + idAppWidget + ", remoteViews=" + remoteViews.toString());
        ComponentName thisAppWidget = new ComponentName(ConfigActivity.this.getPackageName(),
                TadoWidgetProvider.class.getName());
        Intent updateIntent = new Intent(ConfigActivity.this, TadoWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        updateIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        ConfigActivity.this.sendBroadcast(updateIntent);

        // Done with Configure, finish Activity
        ConfigActivity.this.finish();
    }

    @Override
    public void onBackPressed() {
        DLog.d(TAG, "onBackPressed @ConfigActivity");
        if(isLogin)
            super.onBackPressed();
        else {
            // STEP BACK
            isLogin = true;
            showProgress(false);
        }
    }

    @Override
    protected void onDestroy() {
        DLog.d(TAG, "onDestroy @ConfigActivity");
        super.onDestroy();
    }

    public class SpinnerOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            DLog.d(TAG, "onItemSelected @SpinnerOnItemSelectedListener - view=" + view + ", pos=" + pos);

            Spinner spinner = (Spinner) parent;
            String value = "";
            switch (spinner.getId()) {
//                case R.id.spinner_devices:
//                    value = spinnerDevices.getSelectedItem().toString();
//                    break;
                case R.id.spinner_update_time:
                    value = spinnerUpdateTime.getSelectedItem().toString();
                    break;
            }
            DLog.d(TAG, "onItemSelected @SpinnerOnItemSelectedListener - value=" + value);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }
}
