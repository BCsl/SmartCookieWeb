package com.cookiegames.smartcookie.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cookiegames.smartcookie.BrowserApp;
import com.cookiegames.smartcookie.BuildConfig;
import com.cookiegames.smartcookie.MainActivity;
import com.cookiegames.smartcookie.R;
import com.cookiegames.smartcookie.adblock.AdBlocker;
import com.cookiegames.smartcookie.constant.Constants;
import com.cookiegames.smartcookie.controller.UIController;
import com.cookiegames.smartcookie.dialog.BrowserDialog;
import com.cookiegames.smartcookie.malwareblock.MalwareBlock;
import com.cookiegames.smartcookie.preference.PreferenceManager;
import com.cookiegames.smartcookie.utils.IntentUtils;
import com.cookiegames.smartcookie.utils.Preconditions;
import com.cookiegames.smartcookie.utils.ProxyUtils;
import com.cookiegames.smartcookie.utils.UrlUtils;
import com.cookiegames.smartcookie.utils.Utils;

import static android.content.Context.MODE_PRIVATE;

public class LightningWebClient extends WebViewClient {

    private static final String TAG = "LightningWebClient";

    @NonNull private final Activity mActivity;
    @NonNull private final LightningView mLightningView;
    @NonNull private final UIController mUIController;
    @NonNull private final IntentUtils mIntentUtils;
    private String urlName;
    private int adsBlocked;
    SharedPreferences sharedPref;

    @Inject ProxyUtils mProxyUtils;
    @Inject PreferenceManager mPreferences;

    @NonNull private AdBlocker mAdBlock;
    @NonNull private MalwareBlock mMalwareBlock;

    LightningWebClient(@NonNull Activity activity, @NonNull LightningView lightningView) {
        BrowserApp.getAppComponent().inject(this);
        Preconditions.checkNonNull(activity);
        Preconditions.checkNonNull(lightningView);
        mActivity = activity;
        mUIController = (UIController) activity;
        mLightningView = lightningView;
        mAdBlock = chooseAdBlocker();
        mMalwareBlock = chooseMalwareBlocker();
        mIntentUtils = new IntentUtils(activity);
    }

    public void updatePreferences() {
            mAdBlock = chooseAdBlocker();
        mMalwareBlock = chooseMalwareBlocker();
    }

    private AdBlocker chooseAdBlocker() {
        if (mPreferences.getAdBlockEnabled()) {
            return BrowserApp.getAppComponent().provideAssetsAdBlocker();
        } else {
            return BrowserApp.getAppComponent().provideNoOpAdBlocker();
        }
    }

    private MalwareBlock chooseMalwareBlocker() {
        if (mPreferences.getBlockMalwareEnabled()) {
            return BrowserApp.getAppComponent().provideAssetsMalwareBlock();
        } else {
            return BrowserApp.getAppComponent().provideNoOpMalwareBlock();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, @NonNull WebResourceRequest request/*, Context ctx*/) {
        if (mAdBlock.isAd(request.getUrl().toString())) {
            /*adsBlocked = Integer.parseInt(sharedPref.getString("user_id", "0"));
            adsBlocked = adsBlocked + 1;
            sharedPref = ctx.getSharedPreferences("myPref", MODE_PRIVATE);
            sharedPref.edit().putString("user_id", Integer.toString(adsBlocked)).commit();*/

            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
            return new WebResourceResponse("text/plain", "utf-8", EMPTY);
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Nullable
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (mAdBlock.isAd(url)) {
            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
            return new WebResourceResponse("text/plain", "utf-8", EMPTY);
        }

        if (mMalwareBlock.isMalware(url)) {
                ByteArrayInputStream EMPTY = new ByteArrayInputStream("Something has gone wrong. Make sure SmartCookieWeb is up to date or try another website.".getBytes());
                return new WebResourceResponse("text/plain", "utf-8", EMPTY);
        }

        if(mPreferences.getSiteBlockChoice() == 1){
            if(mPreferences.getSiteBlockString("") != "" && mPreferences.getSiteBlockString("") != null){
                if(mPreferences.getSiteBlockString("").contains(url)){
                    ByteArrayInputStream EMPTY = new ByteArrayInputStream("Site blocked in settings.".getBytes());
                    return new WebResourceResponse("text/plain", "utf-8", EMPTY);
                }
            }
        }

        return null;

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onPageFinished(@NonNull WebView view, String url) {

        if (view.isShown()) {
            mUIController.updateUrl(url, false);
            mUIController.setBackButtonEnabled(view.canGoBack());
            mUIController.setForwardButtonEnabled(view.canGoForward());
            view.postInvalidate();
        }
        if (view.getTitle() == null || view.getTitle().isEmpty()) {
            mLightningView.getTitleInfo().setTitle(mActivity.getString(R.string.untitled));
        } else {
            mLightningView.getTitleInfo().setTitle(view.getTitle());
        }
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT &&
            mLightningView.getInvertePage()) {
            view.evaluateJavascript(Constants.JAVASCRIPT_INVERT_PAGE, null);
        }
        mUIController.tabChanged(mLightningView);
    }

    public void onReceivedError(WebView webview, int i, String s, String s1){
        webview.loadUrl("file:///android_asset/error.html");
    }

    public static boolean stringContainsItemFromList(String inputStr, String[] items)
    {
        for(int i =0; i < items.length; i++)
        {
            if(inputStr.contains(items[i]))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (mPreferences.getBlockMalwareEnabled()) {
            if (mMalwareBlock.isMalware(url)) {
                view.loadUrl("file:///android_asset/warn.html");
            }
        }

        if(mPreferences.getSiteBlockChoice() == 2){
            Log.d(TAG, "Yes1");
            if(mPreferences.getSiteBlockString("") != "" && mPreferences.getSiteBlockString("") != null){
                Log.d(TAG, "Yes2");
                String arrayOfURLs = mPreferences.getSiteBlockString("");
                String[] strgs = arrayOfURLs.split("   ");
                if(stringContainsItemFromList(url, strgs)){
                    Log.d(TAG, "Yes3");
                    view.loadUrl("file:///android_asset/block.html");
                }
            }
        }
        if(url=="sc:about" | url.endsWith("sc%3Aabout") && !url.contains("sc%3Aabout ")){
            view.loadUrl("file:///android_asset/about.html");
        }

        mLightningView.getTitleInfo().setFavicon(null);
        if (mLightningView.isShown()) {
            mUIController.updateUrl(url, true);
            mUIController.showActionBar();
        }
        mUIController.tabChanged(mLightningView);
    }

    @Override
    public void onReceivedHttpAuthRequest(final WebView view, @NonNull final HttpAuthHandler handler,
                                          final String host, final String realm) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        View dialogView = LayoutInflater.from(mActivity).inflate(R.layout.dialog_auth_request, null);

        final TextView realmLabel = dialogView.findViewById(R.id.auth_request_realm_textview);
        final EditText name = dialogView.findViewById(R.id.auth_request_username_edittext);
        final EditText password = dialogView.findViewById(R.id.auth_request_password_edittext);

        realmLabel.setText(mActivity.getString(R.string.label_realm, realm));

        builder.setView(dialogView)
            .setTitle(R.string.title_sign_in)
            .setCancelable(true)
            .setPositiveButton(R.string.title_sign_in,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String user = name.getText().toString();
                        String pass = password.getText().toString();
                        handler.proceed(user.trim(), pass.trim());
                        Log.d(TAG, "Attempting HTTP Authentication");
                    }
                })
            .setNegativeButton(R.string.action_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        handler.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    private volatile boolean mIsRunning = false;
    private float mZoomScale = 0.0f;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onScaleChanged(@NonNull final WebView view, final float oldScale, final float newScale) {
        if (view.isShown() && mLightningView.mPreferences.getTextReflowEnabled() &&
            Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (mIsRunning)
                return;
            float changeInPercent = Math.abs(100 - 100 / mZoomScale * newScale);
            if (changeInPercent > 2.5f && !mIsRunning) {
                mIsRunning = view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mZoomScale = newScale;
                        view.evaluateJavascript(Constants.JAVASCRIPT_TEXT_REFLOW, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                mIsRunning = false;
                            }
                        });
                    }
                }, 100);
            }

        }
    }

    @NonNull
    private static List<Integer> getAllSslErrorMessageCodes(@NonNull SslError error) {
        List<Integer> errorCodeMessageCodes = new ArrayList<>(1);

        if (error.hasError(SslError.SSL_DATE_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_date_invalid);
        }
        if (error.hasError(SslError.SSL_EXPIRED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_expired);
        }
        //causes issues
        //if (error.hasError(SslError.SSL_IDMISMATCH)) {
         //   errorCodeMessageCodes.add(R.string.message_certificate_domain_mismatch);
        //}
        if (error.hasError(SslError.SSL_NOTYETVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_not_yet_valid);
        }
        if (error.hasError(SslError.SSL_UNTRUSTED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_untrusted);
        }
        if (error.hasError(SslError.SSL_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_invalid);
        }

        return errorCodeMessageCodes;
    }

    @Override
    public void onReceivedSslError(WebView view, @NonNull final SslErrorHandler handler, @NonNull SslError error) {
        List<Integer> errorCodeMessageCodes = getAllSslErrorMessageCodes(error);

        StringBuilder stringBuilder = new StringBuilder();
        for (Integer messageCode : errorCodeMessageCodes) {
            stringBuilder.append(" - ").append(mActivity.getString(messageCode)).append('\n');
        }
        String alertMessage =
            mActivity.getString(R.string.message_insecure_connection, stringBuilder.toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getString(R.string.title_warning));
        builder.setMessage(alertMessage)
            .setCancelable(true)
            .setPositiveButton(mActivity.getString(R.string.action_yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        handler.proceed();
                    }
                })
            .setNegativeButton(mActivity.getString(R.string.action_no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        handler.cancel();
                    }
                });
        Dialog dialog = builder.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }

    @Override
    public void onFormResubmission(WebView view, @NonNull final Message dontResend, @NonNull final Message resend) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getString(R.string.title_form_resubmission));
        builder.setMessage(mActivity.getString(R.string.message_form_resubmission))
            .setCancelable(true)
            .setPositiveButton(mActivity.getString(R.string.action_yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        resend.sendToTarget();
                    }
                })
            .setNegativeButton(mActivity.getString(R.string.action_no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dontResend.sendToTarget();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
        BrowserDialog.setDialogSize(mActivity, alert);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
        return shouldOverrideLoading(view, request.getUrl().toString()) || super.shouldOverrideUrlLoading(view, request);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull String url) {
        return shouldOverrideLoading(view, url) || super.shouldOverrideUrlLoading(view, url);
    }

    private boolean shouldOverrideLoading(@NonNull WebView view, @NonNull String url) {

        // Check if configured proxy is available
        if (!mProxyUtils.isProxyReady(mActivity)) {
            // User has been notified
            return true;
        }

        Map<String, String> headers = mLightningView.getRequestHeaders();

        if (mLightningView.isIncognito()) {
            // If we are in incognito, immediately load, we don't want the url to leave the app
            return continueLoadingUrl(view, url, headers);
        }
        if (URLUtil.isAboutUrl(url)) {
            // If this is an about page, immediately load, we don't need to leave the app
            return continueLoadingUrl(view, url, headers);
        }

        if (isMailOrIntent(url, view) || mIntentUtils.startActivityForUrl(view, url)) {
            // If it was a mailto: link, or an intent, or could be launched elsewhere, do that
            return true;
        }

        // If none of the special conditions was met, continue with loading the url
        return continueLoadingUrl(view, url, headers);
    }

    private boolean continueLoadingUrl(@NonNull WebView webView,
                                       @NonNull String url,
                                       @NonNull Map<String, String> headers) {
        if (headers.isEmpty()) {
            return false;
        } else if (Utils.doesSupportHeaders()) {
            webView.loadUrl(url, headers);
            return true;
        } else {
            return false;
        }
    }


    private boolean isMailOrIntent(@NonNull String url, @NonNull WebView view) {
        if (url.startsWith("mailto:")) {
            MailTo mailTo = MailTo.parse(url);
            Intent i = Utils.newEmailIntent(mailTo.getTo(), mailTo.getSubject(),
                mailTo.getBody(), mailTo.getCc());
            mActivity.startActivity(i);
            view.reload();
            return true;
        } else if (url.startsWith("intent://")) {
            Intent intent;
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException ignored) {
                intent = null;
            }
            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    intent.setSelector(null);
                }
                try {
                    mActivity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "ActivityNotFoundException");
                }
                return true;
            }
        } else if (URLUtil.isFileUrl(url) && !UrlUtils.isSpecialUrl(url)) {
            File file = new File(url.replace(Constants.FILE, ""));

            if (file.exists()) {
                String newMimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(Utils.guessFileExtension(file.toString()));

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(mActivity, BuildConfig.APPLICATION_ID + ".fileprovider", file);
                intent.setDataAndType(contentUri, newMimeType);

                try {
                    mActivity.startActivity(intent);
                } catch (Exception e) {
                    System.out.println("SmartCookieWeb: cannot open downloaded file");
                }
            } else {
                Utils.showSnackbar(mActivity, R.string.message_open_download_fail);
            }
            return true;
        }
        return false;
    }
}
