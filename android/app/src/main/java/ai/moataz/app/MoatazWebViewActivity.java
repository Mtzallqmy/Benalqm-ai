package ai.moataz.app;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MoatazWebViewActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4101;
    private static final int STORAGE_PERMISSION_REQUEST = 4102;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private PendingDownload pendingDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8, 11, 20));
        getWindow().setNavigationBarColor(Color.rgb(8, 11, 20));

        String url = getIntent().getStringExtra(MainActivity.EXTRA_URL);
        if (url == null || url.isBlank()) {
            finish();
            return;
        }

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(8, 11, 20));
        setContentView(webView);

        configureWebView(url);
        if (savedInstanceState == null) webView.loadUrl(url);
        else webView.restoreState(savedInstanceState);
    }

    private void configureWebView(String initialUrl) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSafeBrowsingEnabled(true);
        settings.setMixedContentMode(
            initialUrl.startsWith("https://")
                ? WebSettings.MIXED_CONTENT_NEVER_ALLOW
                : WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        );

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException ignored) {
                    Toast.makeText(MoatazWebViewActivity.this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> filePath,
                FileChooserParams fileChooserParams
            ) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = filePath;
                try {
                    Intent chooser = fileChooserParams.createIntent();
                    startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException error) {
                    filePathCallback = null;
                    Toast.makeText(MoatazWebViewActivity.this, R.string.no_file_picker, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
            requestDownload(new PendingDownload(url, userAgent, contentDisposition, mimeType))
        );
    }

    private void requestDownload(PendingDownload download) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingDownload = download;
            requestPermissions(
                new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                STORAGE_PERMISSION_REQUEST
            );
            return;
        }
        enqueueDownload(download);
    }

    private void enqueueDownload(PendingDownload download) {
        try {
            String filename = URLUtil.guessFileName(
                download.url,
                download.contentDisposition,
                download.mimeType
            );
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(download.url));
            request.setTitle(filename);
            request.setMimeType(download.mimeType);
            request.addRequestHeader("User-Agent", download.userAgent);
            String cookie = CookieManager.getInstance().getCookie(download.url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
            Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;
        Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        filePathCallback.onReceiveValue(result);
        filePathCallback = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != STORAGE_PERMISSION_REQUEST || pendingDownload == null) return;
        PendingDownload download = pendingDownload;
        pendingDownload = null;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownload(download);
        } else {
            Toast.makeText(this, R.string.download_permission_denied, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }

    private static final class PendingDownload {
        final String url;
        final String userAgent;
        final String contentDisposition;
        final String mimeType;

        PendingDownload(String url, String userAgent, String contentDisposition, String mimeType) {
            this.url = url;
            this.userAgent = userAgent;
            this.contentDisposition = contentDisposition;
            this.mimeType = mimeType;
        }
    }
}
