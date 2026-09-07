package ai.moataz.app;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;

import com.google.androidbrowserhelper.trusted.LauncherActivity;
import com.google.androidbrowserhelper.trusted.TwaLauncher;

public class MoatazTwaActivity extends LauncherActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Transparent launcher activities can crash when orientation is forced on Android 8.0.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    @Override
    protected TwaLauncher.FallbackStrategy getFallbackStrategy() {
        return (context, twaBuilder, providerPackage, completionCallback) -> {
            Intent intent = new Intent(context, MoatazWebViewActivity.class);
            intent.putExtra(MainActivity.EXTRA_URL, twaBuilder.getUri().toString());
            context.startActivity(intent);
            if (completionCallback != null) completionCallback.run();
        };
    }
}
