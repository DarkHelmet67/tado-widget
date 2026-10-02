package it.darkhelmet67.tado;

import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.darkhelmet67.tado.api.DeviceAuth;
import it.darkhelmet67.tado.api.Home;
import it.darkhelmet67.tado.api.TadoApi;
import it.darkhelmet67.tado.api.TadoException;
import it.darkhelmet67.tado.api.Zone;
import it.darkhelmet67.tado.auth.SecureTokenStore;
import it.darkhelmet67.tado.widget.AppPrefs;
import it.darkhelmet67.tado.widget.RefreshScheduler;
import it.darkhelmet67.tado.widget.TadoWidgetProvider;
import it.darkhelmet67.tado.widget.WidgetState;

/**
 * Sign-in and settings screen. It is both the widget's configure activity and the app's launcher
 * screen. Sign-in uses tado°'s OAuth device-code flow: the user approves in a browser, so the
 * app never sees their password.
 */
public class ConfigActivity extends AppCompatActivity {

    private static final class ZoneChoice {
        final long homeId;
        final long zoneId;
        final String label;

        ZoneChoice(long homeId, long zoneId, String label) {
            this.homeId = homeId;
            this.zoneId = zoneId;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final Object POLL_LOCK = new Object();
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<ZoneChoice> choices = new ArrayList<>();
    private volatile boolean loginCancelled;

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SecureTokenStore tokens;
    private TadoApi api;
    private AppPrefs prefs;

    private View groupSignIn;
    private View groupPending;
    private View groupSetup;
    private TextView textInstructions;
    private TextView textStatus;
    private Spinner spinnerZone;
    private Spinner spinnerInterval;
    private Button buttonOpenLogin;
    private ArrayAdapter<ZoneChoice> zoneAdapter;
    private String loginUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tado_config);

        tokens = new SecureTokenStore(this);
        api = new TadoApi(tokens);
        prefs = new AppPrefs(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // If the user backs out while adding a widget, the widget must not be placed.
        setResult(RESULT_CANCELED, resultIntent());

        groupSignIn = findViewById(R.id.groupSignIn);
        groupPending = findViewById(R.id.groupPending);
        groupSetup = findViewById(R.id.groupSetup);
        textInstructions = findViewById(R.id.textInstructions);
        textStatus = findViewById(R.id.textStatus);
        spinnerZone = findViewById(R.id.spinnerZone);
        spinnerInterval = findViewById(R.id.spinnerInterval);
        buttonOpenLogin = findViewById(R.id.buttonOpenLogin);

        zoneAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
        zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerZone.setAdapter(zoneAdapter);

        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(
                this, R.array.update_interval_labels, android.R.layout.simple_spinner_item);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInterval.setAdapter(intervalAdapter);
        spinnerInterval.setSelection(indexOfInterval(prefs.getIntervalMinutes()));

        findViewById(R.id.buttonSignIn).setOnClickListener(v -> startLogin());
        buttonOpenLogin.setOnClickListener(v -> openLoginPage());
        findViewById(R.id.buttonCheckLogin).setOnClickListener(v -> checkLoginNow(true));
        findViewById(R.id.buttonCancelLogin).setOnClickListener(v -> cancelLogin());
        findViewById(R.id.buttonSave).setOnClickListener(v -> save());
        findViewById(R.id.buttonSignOut).setOnClickListener(v -> signOut());

        String[] pending = prefs.getPendingLogin();
        if (api.isSignedIn()) {
            prefs.clearPendingLogin();
            loadZones();
        } else if (pending != null) {
            // Resume a login started before this screen was recreated (e.g. while in the browser).
            loginUrl = pending[2];
            textInstructions.setText(getString(R.string.login_instructions, pending[1]));
            show(groupPending);
            DeviceAuth auth = new DeviceAuth(pending[0], pending[1], pending[2], pending[2],
                    Integer.parseInt(pending[3]), 0);
            long expiresAt = Long.parseLong(pending[4]);
            loginCancelled = false;
            io.execute(() -> pollForApproval(auth, expiresAt));
        } else {
            show(groupSignIn);
        }
    }

    @Override
    protected void onDestroy() {
        loginCancelled = true;
        io.shutdownNow();
        super.onDestroy();
    }

    // ---- Sign-in (device-code flow) ----

    private void startLogin() {
        loginCancelled = false;
        textStatus.setText(R.string.status_contacting);
        io.execute(() -> {
            try {
                DeviceAuth auth = api.startDeviceAuth();
                loginUrl = auth.verificationUriComplete;
                long expiresAt = System.currentTimeMillis() + auth.expiresInSeconds * 1000L;
                prefs.setPendingLogin(auth.deviceCode, auth.userCode, loginUrl, auth.intervalSeconds, expiresAt);
                ui.post(() -> {
                    textInstructions.setText(getString(R.string.login_instructions, auth.userCode));
                    show(groupPending);
                    openLoginPage();
                });
                pollForApproval(auth, expiresAt);
            } catch (TadoException e) {
                ui.post(() -> fail(e));
            }
        });
    }

    /** Runs on the io thread until the user approves, declines, cancels or the code expires. */
    private void pollForApproval(DeviceAuth auth, long deadline) {
        long intervalMillis = Math.max(auth.intervalSeconds, 1) * 1000L;
        try {
            while (!loginCancelled && System.currentTimeMillis() < deadline) {
                Thread.sleep(intervalMillis);
                if (loginCancelled) return;
                try {
                    if (pollOnce(auth.deviceCode)) {
                        prefs.clearPendingLogin();
                        ui.post(this::loadZones);
                        return;
                    }
                } catch (TadoException e) {
                    if (e.kind == TadoException.Kind.SLOW_DOWN) {
                        intervalMillis += 5000;
                    } else if (e.kind != TadoException.Kind.PENDING) {
                        prefs.clearPendingLogin();
                        ui.post(() -> fail(e));
                        return;
                    }
                }
            }
            if (!loginCancelled) {
                prefs.clearPendingLogin();
                ui.post(() -> {
                    Toast.makeText(this, R.string.error_login_expired, Toast.LENGTH_LONG).show();
                    show(groupSignIn);
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Asks tado° once whether the login was approved. Serialised so that several screens or threads
     * never use the same device code twice.
     *
     * @return true once signed in, false while approval is still pending
     */
    private boolean pollOnce(String deviceCode) throws TadoException {
        synchronized (POLL_LOCK) {
            if (api.isSignedIn()) return true;
            try {
                api.pollToken(deviceCode);
                return true;
            } catch (TadoException e) {
                if (e.kind == TadoException.Kind.PENDING) return false;
                throw e;
            }
        }
    }

    /** One immediate check, used by the Continue button and when returning from the browser. */
    private void checkLoginNow(boolean userInitiated) {
        String[] pending = prefs.getPendingLogin();
        if (pending == null && !api.isSignedIn()) return;
        if (userInitiated) textStatus.setText(R.string.status_checking);
        io.execute(() -> {
            try {
                boolean done = api.isSignedIn() || (pending != null && pollOnce(pending[0]));
                if (done) {
                    prefs.clearPendingLogin();
                    ui.post(this::loadZones);
                } else if (userInitiated) {
                    ui.post(() -> textStatus.setText(R.string.status_not_approved_yet));
                }
            } catch (TadoException e) {
                prefs.clearPendingLogin();
                ui.post(() -> fail(e));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from the browser: do not wait for the next background poll.
        if (groupPending.getVisibility() == View.VISIBLE) checkLoginNow(false);
    }

    private void openLoginPage() {
        if (loginUrl == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_no_browser, Toast.LENGTH_LONG).show();
        }
    }

    private void cancelLogin() {
        loginCancelled = true;
        prefs.clearPendingLogin();
        show(groupSignIn);
    }

    // ---- Zone selection ----

    private void loadZones() {
        textStatus.setText(R.string.status_loading_zones);
        io.execute(() -> {
            try {
                List<ZoneChoice> found = new ArrayList<>();
                List<Home> homes = api.homes();
                for (Home home : homes) {
                    for (Zone zone : api.zones(home.id)) {
                        String label = homes.size() > 1 ? home.name + " · " + zone.name : zone.name;
                        found.add(new ZoneChoice(home.id, zone.id, label));
                    }
                }
                ui.post(() -> showZones(found));
            } catch (TadoException e) {
                ui.post(() -> fail(e));
            }
        });
    }

    private void showZones(List<ZoneChoice> found) {
        choices.clear();
        choices.addAll(found);
        zoneAdapter.notifyDataSetChanged();
        for (int i = 0; i < choices.size(); i++) {
            ZoneChoice c = choices.get(i);
            if (prefs.hasZone() && c.homeId == prefs.getHomeId() && c.zoneId == prefs.getZoneId()) {
                spinnerZone.setSelection(i);
            }
        }
        if (choices.isEmpty()) {
            Toast.makeText(this, R.string.error_no_zones, Toast.LENGTH_LONG).show();
        }
        textStatus.setText("");
        show(groupSetup);
    }

    private void save() {
        ZoneChoice choice = (ZoneChoice) spinnerZone.getSelectedItem();
        if (choice == null) {
            Toast.makeText(this, R.string.error_no_zones, Toast.LENGTH_LONG).show();
            return;
        }
        prefs.setZone(choice.homeId, choice.zoneId, choice.label);
        prefs.setIntervalMinutes(intervalAt(spinnerInterval.getSelectedItemPosition()));
        prefs.setState(prefs.getState().withStatus(WidgetState.Status.OK));
        RefreshScheduler.schedulePeriodic(this);
        RefreshScheduler.refreshNow(this);
        TadoWidgetProvider.updateAll(this);
        setResult(RESULT_OK, resultIntent());
        finish();
    }

    private void signOut() {
        api.signOut();
        prefs.clearZoneAndState();
        RefreshScheduler.cancelPeriodic(this);
        TadoWidgetProvider.updateAll(this);
        show(groupSignIn);
    }

    // ---- Helpers ----

    private void fail(TadoException e) {
        int message;
        switch (e.kind) {
            case DENIED: message = R.string.error_login_denied; break;
            case EXPIRED: message = R.string.error_login_expired; break;
            case RATE_LIMITED: message = R.string.error_rate_limited; break;
            case NETWORK: message = R.string.error_network; break;
            default: message = R.string.error_generic; break;
        }
        if (e.kind == TadoException.Kind.AUTH_EXPIRED && api.isSignedIn()) return; // another poll won
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Keep the technical reason on screen so problems can be reported.
        textStatus.setText(getString(message) + " (" + e.kind + ": " + e.getMessage() + ")");
        show(api.isSignedIn() && e.kind != TadoException.Kind.AUTH_EXPIRED ? groupSetup : groupSignIn);
    }

    private void show(View group) {
        groupSignIn.setVisibility(group == groupSignIn ? View.VISIBLE : View.GONE);
        groupPending.setVisibility(group == groupPending ? View.VISIBLE : View.GONE);
        groupSetup.setVisibility(group == groupSetup ? View.VISIBLE : View.GONE);
    }

    private Intent resultIntent() {
        return new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
    }

    private int intervalAt(int position) {
        int[] values = getResources().getIntArray(R.array.update_interval_values);
        return values[Math.max(0, Math.min(position, values.length - 1))];
    }

    private int indexOfInterval(int minutes) {
        int[] values = getResources().getIntArray(R.array.update_interval_values);
        for (int i = 0; i < values.length; i++) if (values[i] == minutes) return i;
        return 0;
    }
}
