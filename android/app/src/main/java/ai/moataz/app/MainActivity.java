package ai.moataz.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    static final String PREFS = "moataz_ai_android";
    static final String SERVER_URL = "server_url";
    static final String EXTRA_URL = "ai.moataz.app.URL";
    static final String ACTION_SETTINGS = "ai.moataz.app.SETTINGS";

    private SharedPreferences prefs;
    private EditText serverInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8, 11, 20));
        getWindow().setNavigationBarColor(Color.rgb(8, 11, 20));
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        String deepLinkUrl = readDeepLinkUrl(getIntent());
        if (deepLinkUrl != null) {
            openServer(deepLinkUrl, false);
            return;
        }

        String configured = prefs.getString(SERVER_URL, "");
        if ((configured == null || configured.isBlank()) && !BuildConfig.DEFAULT_ORIGIN.isBlank()) {
            configured = BuildConfig.DEFAULT_ORIGIN;
            prefs.edit().putString(SERVER_URL, configured).apply();
        }

        boolean forceSettings = ACTION_SETTINGS.equals(getIntent().getAction());
        if (!forceSettings && configured != null && !configured.isBlank()) {
            openServer(configured, false);
            return;
        }

        renderSettings(configured == null ? "" : configured);
    }

    private void renderSettings(String configured) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(42), dp(24), dp(32));
        root.setBackgroundColor(Color.rgb(8, 11, 20));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText(getString(R.string.app_name));
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(12)));

        TextView description = new TextView(this);
        description.setText(R.string.server_setup_description);
        description.setTextColor(Color.rgb(190, 198, 220));
        description.setTextSize(15);
        description.setGravity(Gravity.CENTER);
        root.addView(description, matchWrap(dp(28)));

        serverInput = new EditText(this);
        serverInput.setSingleLine(true);
        serverInput.setText(configured);
        serverInput.setHint(R.string.server_url_hint);
        serverInput.setTextColor(Color.WHITE);
        serverInput.setHintTextColor(Color.rgb(126, 137, 165));
        serverInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(120, 87, 255)));
        root.addView(serverInput, matchWrap(dp(18)));

        Button saveOpen = new Button(this);
        saveOpen.setText(R.string.save_and_open);
        saveOpen.setMinHeight(dp(52));
        saveOpen.setOnClickListener((view) -> saveAndOpen());
        root.addView(saveOpen, matchWrap(dp(12)));

        Button clear = new Button(this);
        clear.setText(R.string.clear_server);
        clear.setMinHeight(dp(48));
        clear.setOnClickListener((view) -> {
            prefs.edit().remove(SERVER_URL).apply();
            serverInput.setText("");
            Toast.makeText(this, R.string.server_cleared, Toast.LENGTH_SHORT).show();
        });
        root.addView(clear, matchWrap(0));

        TextView security = new TextView(this);
        security.setText(R.string.http_local_warning);
        security.setTextColor(Color.rgb(255, 190, 85));
        security.setTextSize(12);
        security.setPadding(0, dp(22), 0, 0);
        root.addView(security, matchWrap(0));

        setContentView(scroll);
        serverInput.requestFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
            .showSoftInput(serverInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void saveAndOpen() {
        try {
            String normalized = ServerUrlPolicy.normalize(serverInput.getText().toString());
            prefs.edit().putString(SERVER_URL, normalized).apply();
            openServer(normalized, true);
        } catch (IllegalArgumentException error) {
            serverInput.setError(error.getMessage());
        }
    }

    private void openServer(String rawUrl, boolean fromSettings) {
        final String normalized;
        try {
            normalized = ServerUrlPolicy.normalize(rawUrl);
        } catch (IllegalArgumentException error) {
            renderSettings(rawUrl);
            Toast.makeText(this, R.string.invalid_server_url, Toast.LENGTH_LONG).show();
            return;
        }

        Intent target;
        if (normalized.startsWith("https://")
            && ServerUrlPolicy.isTrustedOrigin(normalized, BuildConfig.TRUSTED_ORIGIN)) {
            target = new Intent(this, MoatazTwaActivity.class);
            target.setData(Uri.parse(workspaceUrl(normalized)));
        } else {
            target = new Intent(this, MoatazWebViewActivity.class);
            target.putExtra(EXTRA_URL, workspaceUrl(normalized));
        }
        startActivity(target);
        if (!fromSettings) finish();
    }

    private String readDeepLinkUrl(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data == null) return null;
        if ("moataz-ai".equalsIgnoreCase(data.getScheme()) && "open".equalsIgnoreCase(data.getHost())) {
            return data.getQueryParameter("url");
        }
        if ("http".equalsIgnoreCase(data.getScheme()) || "https".equalsIgnoreCase(data.getScheme())) {
            return data.getScheme() + "://" + data.getEncodedAuthority();
        }
        return null;
    }

    private static String workspaceUrl(String origin) {
        String lower = origin.toLowerCase();
        return lower.contains("/workspace") ? origin : origin + "/workspace";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = bottomMargin;
        return params;
    }
}
