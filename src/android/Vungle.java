//Copyright (c) 2014 Sang Ki Kwon (Cranberrygame)
//Email: cranberrygame@yahoo.com
//Homepage: http://www.github.com/cranberrygame
//License: MIT (http://opensource.org/licenses/MIT)
package com.cranberrygame.cordova.plugin.ad.vungle;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.util.Log;

//
import com.vungle.publisher.AdConfig;
import com.vungle.publisher.EventListener;
import com.vungle.publisher.Orientation;
import com.vungle.publisher.VunglePub;

public class Vungle extends CordovaPlugin {
    private final String LOG_TAG = "Vungle";
    private final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 0;

    private CallbackContext callbackContextKeepCallback;
    //
    protected String email;
    protected String licenseKey;
    public boolean validLicenseKey;
    //
    protected String appId;
    protected String clientId;

    // get the VunglePub instance
    final VunglePub vunglePub = VunglePub.getInstance();

    @Override
    public void pluginInitialize() {
        super.pluginInitialize();
        //
        // Checking whether write permission is granted
        if (!cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // If system supports runtime permissions
            if (Build.VERSION.SDK_INT >= 23) {
                cordova.requestPermission(this, WRITE_EXTERNAL_STORAGE_REQUEST_CODE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            // If permission hasn't been granted
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                if (cordova.getActivity().shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Creating new dialog builder
                    AlertDialog.Builder ab = new AlertDialog.Builder(cordova.getActivity(), android.R.style.Theme_Material_Light_Dialog_Alert);

                    ab
                        .setTitle("Enable Free Coin Rewards")
                        .setMessage("We need the requested permission for video rewards to work properly. Video rewards are a way for users to get free coins within the app. Without this permission enabled, video ad availability will be extremely limited and waiting times will be longer.")
                        .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("Request again", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                cordova.requestPermission(Vungle.this, WRITE_EXTERNAL_STORAGE_REQUEST_CODE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            }
                        })
                        .show();
                }
            } else {
                // If app id and client id is set
                if (this.appId != null && this.clientId != null) {
                    // Initializing Vungle with parameters once again
                    _setUp(this.appId, this.clientId);
                }
            }
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        vunglePub.onPause();
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        vunglePub.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("setLicenseKey")) {
            setLicenseKey(action, args, callbackContext);

            return true;
        } else if (action.equals("setUp")) {
            setUp(action, args, callbackContext);

            return true;
        } else if (action.equals("showRewardedVideoAd")) {
            showRewardedVideoAd(action, args, callbackContext);

            return true;
        }

        return false; // Returning false results in a "MethodNotFound" error.
    }

    private void setLicenseKey(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        final String email = args.getString(0);
        final String licenseKey = args.getString(1);
        Log.d(LOG_TAG, String.format("%s", email));
        Log.d(LOG_TAG, String.format("%s", licenseKey));

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _setLicenseKey(email, licenseKey);
            }
        });
    }

    private void setUp(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        final String appId = args.getString(0);
        final String clientId = args.getString(1);
        Log.d(LOG_TAG, String.format("%s, %s", appId, clientId));

        callbackContextKeepCallback = callbackContext;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _setUp(appId, clientId);
            }
        });
    }

    private void showRewardedVideoAd(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _showRewardedVideoAd();
            }
        });
    }

    public void _setLicenseKey(String email, String licenseKey) {
        this.email = email;
        this.licenseKey = licenseKey;

        this.validLicenseKey = true;
    }

    private void _setUp(String appId, String clientId) {
        this.appId = appId;
        this.clientId = clientId;

        vunglePub.init(cordova.getActivity(), appId);
        vunglePub.setEventListeners(new MyEventListener());//listener needs to come after init on android vunlge sdk

        final AdConfig config = vunglePub.getGlobalAdConfig();
        config.setOrientation(Orientation.autoRotate);//for android
        config.setIncentivized(true); // Enabling server-side validation
        config.setIncentivizedUserId(clientId); // Setting client id
        //config.setOrientation(Orientation.matchVideo);
    }

    private void _showRewardedVideoAd() {
        vunglePub.playAd();
    }

    class MyEventListener implements EventListener {

        @Override
        public void onAdPlayableChanged(boolean isAdPlayable) {
            Log.d(LOG_TAG, "onAdPlayableChanged");

            if (isAdPlayable) {
                PluginResult pr = new PluginResult(PluginResult.Status.OK, "onRewardedVideoAdLoaded");
                pr.setKeepCallback(true);
                callbackContextKeepCallback.sendPluginResult(pr);
            }
        }

        @Override
        public void onAdUnavailable(String arg0) {
            Log.d(LOG_TAG, "onAdUnavailable");
        }

        @Override
        public void onAdStart() {//cranberrygame
            // Called before playing an ad
            Log.d(LOG_TAG, "onAdStart");

            PluginResult pr = new PluginResult(PluginResult.Status.OK, "onRewardedVideoAdShown");
            pr.setKeepCallback(true);
            callbackContextKeepCallback.sendPluginResult(pr);
        }

        @Override
        public void onAdEnd(boolean wasCallToActionClicked) {//cranberrygame
            // Called when the user leaves the ad and control is returned to your application
            Log.d(LOG_TAG, "onAdEnd");

            PluginResult pr = new PluginResult(PluginResult.Status.OK, "onRewardedVideoAdHidden");
            pr.setKeepCallback(true);
            callbackContextKeepCallback.sendPluginResult(pr);
        }

        @Override
        public void onVideoView(boolean isCompletedView, int watchedMillis, int videoDurationMillis) {
            // Called each time an ad completes. isCompletedView is true if at least
            // 80% of the video was watched, which constitutes a completed view.
            // watchedMillis is for the longest video view (if the user replayed the
            // video).
            if (isCompletedView) {
                Log.d(LOG_TAG, "onVideoView: completed");

                PluginResult pr = new PluginResult(PluginResult.Status.OK, "onRewardedVideoAdCompleted");
                pr.setKeepCallback(true);
                callbackContextKeepCallback.sendPluginResult(pr);
                //PluginResult pr = new PluginResult(PluginResult.Status.ERROR);
                //pr.setKeepCallback(true);
                //callbackContextKeepCallback.sendPluginResult(pr);
            } else {
                Log.d(LOG_TAG, "onVideoView: not completed");
/*
                PluginResult pr = new PluginResult(PluginResult.Status.OK, "onRewardedVideoAdNotCompleted");
				pr.setKeepCallback(true);
				callbackContextKeepCallback.sendPluginResult(pr);
				//PluginResult pr = new PluginResult(PluginResult.Status.ERROR);
				//pr.setKeepCallback(true);
				//callbackContextKeepCallback.sendPluginResult(pr);
*/
            }
        }
    }
}
