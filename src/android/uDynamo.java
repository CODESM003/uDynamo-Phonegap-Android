package com.codesm.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;

import com.magtek.mobile.android.scra.MTSCRAException;
import com.magtek.mobile.android.scra.MagTekSCRA;
import com.magtek.mobile.android.scra.ProcessMessageResponse;
import com.magtek.mobile.android.scra.SCRAConfiguration;
import com.magtek.mobile.android.scra.ConfigParam;
import com.magtek.mobile.android.scra.SCRAConfigurationDeviceInfo;
import com.magtek.mobile.android.scra.SCRAConfigurationReaderType;
import com.magtek.mobile.android.scra.StatusCode;

import java.lang.Override;
import java.lang.String;

public class uDynamo extends CordovaPlugin {

    public static final int STATUS_IDLE = 1;
    public static final int STATUS_PROCESSCARD = 2;

    private AudioManager mAudioMgr;

    private MagTekSCRA mMTSCRA;

    private Handler mSCRADataHandler = new Handler(new SCRAHandlerCallback());
    final headSetBroadCastReceiver mHeadsetReceiver = new headSetBroadCastReceiver();
    final NoisyAudioStreamReceiver mNoisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

    String mStringCardDataBuffer;

    private int mIntCurrentDeviceStatus;

    private CallbackContext mEventListenerCb;

    // =============================================================================================================

    private boolean mbAudioConnected;

    private long mLongTimerInterval;

    private int mIntCurrentStatus;

    private int mIntCurrentVolume;

    private boolean mbActiveBeforePaused;


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        PluginResult pr = new PluginResult(PluginResult.Status.ERROR, "Unhandled execute call: " + action);

        // js actions
        if (action.equals("isDeviceConnected")) {
            if(mMTSCRA.isDeviceConnected()) {
                pr = new PluginResult(PluginResult.Status.OK);
                closeDevice();
            } else {
                pr = new PluginResult(PluginResult.Status.ERROR);
            }

        } else if (action.equals("getSwipeData")) {

            if(mMTSCRA.isDeviceConnected()) {
                pr = new PluginResult(PluginResult.Status.NO_RESULT);
                pr.setKeepCallback(true);

                mEventListenerCb = callbackContext;
            } else {
                pr = new PluginResult(PluginResult.Status.ERROR);
            }

        } else if (action.equals("cancelSwipe")) {
            closeDevice();

            if(mMTSCRA.isDeviceConnected()) {
                pr = new PluginResult(PluginResult.Status.ERROR);
            } else {
                pr = new PluginResult(PluginResult.Status.OK);
            }
        }

        callbackContext.sendPluginResult(pr);
        return true;
    }

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();

        mMTSCRA = new MagTekSCRA(mSCRADataHandler);
        mAudioMgr = (AudioManager)cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);

        InitializeData();

        mIntCurrentVolume = mAudioMgr.getStreamVolume(AudioManager.STREAM_MUSIC);

        connectAudio();
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);

        //cordova.getActivity().getApplicationContext().registerReceiver(mHeadsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        //cordova.getActivity().getApplicationContext().registerReceiver(mNoisyAudioStreamReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        if(mbActiveBeforePaused)
            connectAudio();
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);

        //cordova.getActivity().getApplicationContext().unregisterReceiver(mHeadsetReceiver);
        //cordova.getActivity().getApplicationContext().unregisterReceiver(mNoisyAudioStreamReceiver);

        mbActiveBeforePaused = mMTSCRA.isDeviceConnected();

        closeDevice();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //cordova.getActivity().getApplicationContext().unregisterReceiver(mHeadsetReceiver);
        cordova.getActivity().getApplicationContext().unregisterReceiver(mNoisyAudioStreamReceiver);

        closeDevice();
    }

    private void InitializeData() {
        mMTSCRA.clearBuffers();
        mLongTimerInterval = 0;
        mbAudioConnected = true;
        mIntCurrentVolume = 0;
        mIntCurrentStatus = STATUS_IDLE;
        mIntCurrentDeviceStatus = MagTekSCRA.DEVICE_STATE_DISCONNECTED;
    }

    private void connectAudio() {
        if (!mMTSCRA.isDeviceConnected()) {
            if (mAudioMgr.isWiredHeadsetOn()) {
                mMTSCRA.setDeviceType(MagTekSCRA.DEVICE_TYPE_AUDIO);
                openDevice();
            }
        }
    }

    private void openDevice() {
        try {

            if (mMTSCRA.getDeviceType() == MagTekSCRA.DEVICE_TYPE_AUDIO) {
                setAudioConfigManual();
                mMTSCRA.openDevice();
            }

        } catch (MTSCRAException ex) {}
    }

    private void closeDevice() {
        if (mMTSCRA != null)
            mMTSCRA.closeDevice();
    }

    void setAudioConfigManual() throws MTSCRAException {

        String model = android.os.Build.MODEL.toUpperCase();
        try {
            if (model.contains("DROID RAZR") || model.toUpperCase().contains("XT910")) {

                mMTSCRA.setConfigurationParams("INPUT_SAMPLE_RATE_IN_HZ=48000,");

            } else if ((model.equals("DROID PRO")) ||
                    (model.equals("MB508")) ||
                    (model.equals("DROIDX")) ||
                    (model.equals("DROID2")) ||
                    (model.equals("MB525"))) {

                mMTSCRA.setConfigurationParams("INPUT_SAMPLE_RATE_IN_HZ=32000,");

            } else if ((model.equals("GT-I9300")) ||//S3 GSM Unlocked
                    (model.equals("SPH-L710")) ||//S3 Sprint
                    (model.equals("SGH-T999")) ||//S3 T-Mobile
                    (model.equals("SCH-I535")) ||//S3 Verizon
                    (model.equals("SCH-R530")) ||//S3 US Cellular
                    (model.equals("SAMSUNG-SGH-I747")) ||// S3 AT&T
                    (model.equals("M532")) ||//Fujitsu
                    (model.equals("GT-N7100")) ||//Notes 2
                    (model.equals("GT-N7105")) ||//Notes 2
                    (model.equals("SAMSUNG-SGH-I317")) ||// Notes 2
                    (model.equals("SCH-I605")) ||// Notes 2
                    (model.equals("SCH-R950")) ||// Notes 2
                    (model.equals("SGH-T889")) ||// Notes 2
                    (model.equals("SPH-L900")) ||// Notes 2
                    (model.equals("GT-P3113")) ||//Galaxy Tab 2, 7.0
                    (model.equals("GT-P3100"))) //Galaxy Tab 2, 7.0

            {
                mMTSCRA.setConfigurationParams("INPUT_AUDIO_SOURCE=VRECOG,");

            } else if ((model.equals("XT907"))) {
                mMTSCRA.setConfigurationParams("INPUT_WAVE_FORM=0,");
            } else {
                mMTSCRA.setConfigurationParams("INPUT_AUDIO_SOURCE=VRECOG,");
            }

        } catch (MTSCRAException ex) {
            throw new MTSCRAException(ex.getMessage());
        }

    }

    private void maxVolume() {
        mAudioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI);
    }

    private void minVolume() {
        mAudioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, mIntCurrentVolume, AudioManager.FLAG_SHOW_UI);
    }

    private void sendCardData() throws JSONException {
        JSONObject response = new JSONObject();

        response.put("ResponseType", mMTSCRA.getResponseType());
        response.put("TrackStatus", mMTSCRA.getTrackDecodeStatus());
        response.put("CardStatus", mMTSCRA.getCardStatus());
        response.put("EncryptionStatus", mMTSCRA.getEncryptionStatus());
        response.put("BatteryLevel", mMTSCRA.getBatteryLevel());
        response.put("TrackMasked", mMTSCRA.getMaskedTracks());
        response.put("MagnePrintStatus", mMTSCRA.getMagnePrintStatus());
        response.put("SessionID", mMTSCRA.getSessionID());
        response.put("CardSvcCode", mMTSCRA.getCardServiceCode());
        response.put("CardPANLength", mMTSCRA.getCardPANLength());
        response.put("KSN", mMTSCRA.getKSN());
        response.put("DeviceSerialNumber", mMTSCRA.getDeviceSerial());
        response.put("TLV.CARDIIN", mMTSCRA.getTagValue("TLV_CARDIIN", ""));
        response.put("MagTekSN", mMTSCRA.getMagTekDeviceSerial());
        response.put("FirmPartNumber", mMTSCRA.getFirmware());
        response.put("TLVVersion", mMTSCRA.getTLVVersion());
        response.put("DeviceModelName", mMTSCRA.getDeviceName());
        response.put("MSRCapability", mMTSCRA.getCapMSR());
        response.put("TracksCapability", mMTSCRA.getCapTracks());
        response.put("EncryptionCapability", mMTSCRA.getCapMagStripeEncryption());
        response.put("CardIIN", mMTSCRA.getCardIIN());
        response.put("CardName", mMTSCRA.getCardName());
        response.put("CardLast4", mMTSCRA.getCardLast4());
        response.put("CardExpDate", mMTSCRA.getCardExpDate());
        response.put("Track1Masked", mMTSCRA.getTrack1Masked());
        response.put("Track2Masked", mMTSCRA.getTrack2Masked());
        response.put("Track3Masked", mMTSCRA.getTrack3Masked());
        response.put("Track1", mMTSCRA.getTrack1());
        response.put("Track2", mMTSCRA.getTrack2());
        response.put("Track3", mMTSCRA.getTrack3());
        response.put("MagnePrint", mMTSCRA.getMagnePrint());
        response.put("RawResponse", mMTSCRA.getResponseData());

        mEventListenerCb.success(response);
    }

    private void debugMsg(String lpstrMessage) {
        Log.i("hello", lpstrMessage);
    }

    // callback

    private class SCRAHandlerCallback implements Callback {
        public boolean handleMessage(Message msg) {

            try {
                switch (msg.what) {
                    case MagTekSCRA.DEVICE_MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case MagTekSCRA.DEVICE_STATE_CONNECTED:
                                mIntCurrentStatus = STATUS_IDLE;
                                mIntCurrentDeviceStatus = MagTekSCRA.DEVICE_STATE_CONNECTED;
                                maxVolume();
                                break;

                            case MagTekSCRA.DEVICE_STATE_CONNECTING:
                                mIntCurrentDeviceStatus = MagTekSCRA.DEVICE_STATE_CONNECTING;
                                break;

                            case MagTekSCRA.DEVICE_STATE_DISCONNECTED:
                                mIntCurrentDeviceStatus = MagTekSCRA.DEVICE_STATE_DISCONNECTED;
                                break;
                        }
                        break;
                    case MagTekSCRA.DEVICE_MESSAGE_DATA_START:
                        if (msg.obj != null) {
                            debugMsg("Transfer started");
                            return true;
                        }
                        break;
                    case MagTekSCRA.DEVICE_MESSAGE_DATA_CHANGE:
                        if (msg.obj != null) {
                            debugMsg("Transfer ended");
                            closeDevice();
                            sendCardData();
                            msg.obj = null;

                            return true;
                        }
                        break;
                    case MagTekSCRA.DEVICE_MESSAGE_DATA_ERROR:
                        mEventListenerCb.error("The card was not property swiped. Please try again.");
                        return true;
                    default:
                        if (msg.obj != null) {
                            return true;
                        }
                        break;
                }
            } catch (Exception ex) {

            }

            return false;
        }
    }

    // BroadcastReceiver

    public class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            debugMsg("noisy?");
            /* If the device is unplugged, this will immediately detect that action,
             * and close the device.
    		 */
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mbAudioConnected = false;
                if (mMTSCRA.getDeviceType() == MagTekSCRA.DEVICE_TYPE_AUDIO) {
                    if (mMTSCRA.isDeviceConnected()) {
                        closeDevice();
                    }
                }
            }
        }
    }

    public class headSetBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            debugMsg("headSet called:"+mbAudioConnected);

            try {
                String action = intent.getAction();

                if ((action.compareTo(Intent.ACTION_HEADSET_PLUG)) == 0) {  //if the action match a headset one
                    int headSetState = intent.getIntExtra("state", 0);      //get the headset state property
                    int hasMicrophone = intent.getIntExtra("microphone", 0);//get the headset microphone property
                    debugMsg("headerphone action? : " + action);
                    if ((headSetState == 1) && (hasMicrophone == 1)) {       //headset was unplugged & has no microphone
                        mbAudioConnected = true;
                    } else {
                        mbAudioConnected = false;
                        if (mMTSCRA.getDeviceType() == MagTekSCRA.DEVICE_TYPE_AUDIO) {
                            if (mMTSCRA.isDeviceConnected()) {
                                closeDevice();
                            }
                        }

                    }

                }

            } catch (Exception ex) {}

        }

    }
}
