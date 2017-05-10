package com.zego.livedemo5.ui.activities.gamelive;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.zego.livedemo5.R;
import com.zego.livedemo5.ZegoApiManager;
import com.zego.livedemo5.constants.IntentExtra;
import com.zego.livedemo5.utils.ShareUtils;
import com.zego.livedemo5.utils.ZegoRoomUtil;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright © 2016 Zego. All rights reserved.
 */
@TargetApi(21)
public  class GameLiveActivity extends AppCompatActivity {

    private static final String TAG = GameLiveActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 1001;

    private Button mStartBtn;
    private Button mShare;
    private boolean mIsRunning = false;

    private String mPublishTitle;
    private String mPublishStreamID;
    private String mRoomID;
    private ZegoLiveRoom mZegoLiveRoom;
    private int mAppOrientation = 0;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private List<String> mListUrls;

    public static void actionStart(Activity activity, String publishTitle, int appOrientation){
        Intent intent = new Intent(activity, GameLiveActivity.class);
        intent.putExtra(IntentExtra.PUBLISH_TITLE, publishTitle);
        intent.putExtra(IntentExtra.APP_ORIENTATION, appOrientation);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_live);

        // 检测系统版本
        if(Build.VERSION.SDK_INT < 21){
            Toast.makeText(this, "录屏功能只能在Android5.0及以上版本的系统中运行", Toast.LENGTH_LONG).show();
            finish();
        }else {

            if (savedInstanceState == null) {
                Intent intent = getIntent();
                mPublishTitle = intent.getStringExtra(IntentExtra.PUBLISH_TITLE);
                mAppOrientation = intent.getIntExtra(IntentExtra.APP_ORIENTATION, Surface.ROTATION_0);
            }

            // 手机横屏直播
            if((mAppOrientation == Surface.ROTATION_90 || mAppOrientation == Surface.ROTATION_270)){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            mZegoLiveRoom = ZegoApiManager.getInstance().getZegoLiveRoom();

            mStartBtn = (Button) findViewById(R.id.start_record);
            mStartBtn.setEnabled(false);
            mStartBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mIsRunning){
                        mIsRunning = false;
                        mStartBtn.setText("开始录屏");

                        mShare.setEnabled(false);

                        stopCapture();
                    }else {
                        mIsRunning = true;
                        mStartBtn.setText("停止录屏");

                        startCapture();
                    }
                }
            });

            mShare = (Button)findViewById(R.id.btn_share);
            mShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListUrls != null){
                        ShareUtils.getInstance().shareToQQ(GameLiveActivity.this, mListUrls, mRoomID, mPublishStreamID);
                    }
                }
            });

            ZegoApiManager.getInstance().releaseSDK();
            // 设置直播模式为"录屏模式"
            mZegoLiveRoom.setLiveMode(ZegoLiveRoom.LIVE_MODE_SCREEN_CAPTURE);
            ZegoApiManager.getInstance().initSDK();

            initCallback();
            // 请求录屏权限, 等待用户授权
            mMediaProjectionManager =  (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        }
    }


    private void initCallback(){
        mZegoLiveRoom.setZegoLivePublisherCallback(new IZegoLivePublisherCallback() {
            @Override
            public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> streamInfo) {
                //推流状态更新
                if (stateCode == 0) {
                    mListUrls = new ArrayList<>();
                    if (streamInfo != null) {

                        mShare.setEnabled(true);

                        String[] hlsList = (String[]) streamInfo.get("hlsList");
                        if (hlsList != null && hlsList.length > 0) {
                            mListUrls.add(hlsList[0]);
                        }

                        String[] rtmpList = (String[]) streamInfo.get("rtmpList");
                        if (rtmpList != null && rtmpList.length > 0) {
                            mListUrls.add(rtmpList[0]);
                        }
                    }
                } else {
                    Log.i(TAG, "sdk停止推流");
                }
            }

            @Override
            public void onJoinLiveRequest(final int seq, String fromUserID, String fromUserName, String roomID) {
            }

            @Override
            public void onPublishQualityUpdate(String streamID, int quality, double videoFPS, double videoBitrate) {
            }

            @Override
            public AuxData onAuxCallback(int dataLen) {
                return null;
            }

            @Override
            public void onCaptureVideoSizeChangedTo(int width, int height) {
                Log.i(TAG, "onCaptureVideoSizeChangedTo");
            }

            @Override
            public void onMixStreamConfigUpdate(int errorCode, String streamID, HashMap<String, Object> streamInfo) {
            }
        });
    }

    private void startCapture(){
        ZegoAvConfig zegoAvConfig = ZegoApiManager.getInstance().getZegoAvConfig();
        mZegoLiveRoom.startScreenCapture(mMediaProjection, zegoAvConfig.getVideoEncodeResolutionWidth(),
                zegoAvConfig.getVideoEncodeResolutionHeight());

        mPublishStreamID = ZegoRoomUtil.getPublishStreamID();
        mZegoLiveRoom.startPublishing(mPublishTitle, mPublishStreamID, 0);
    }

    private void stopCapture(){
        mZegoLiveRoom.stopScreenCapture();
        mZegoLiveRoom.stopPublishing();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            Log.i(TAG, "获取MediaProjection成功");

            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);

            mRoomID = ZegoRoomUtil.getRoomID(ZegoRoomUtil.ROOM_TYPE_SINGLE);
            mZegoLiveRoom.loginRoom(mRoomID, mPublishTitle, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
                @Override
                public void onLoginCompletion(int stateCode, ZegoStreamInfo[] zegoStreamInfos) {
                    if(stateCode == 0){

                        Log.i(TAG, "创建房间成功");
                        mIsRunning = true;
                        mStartBtn.setText("停止录屏");
                        mStartBtn.setEnabled(true);

                        startCapture();
                    }else {
                        Log.i(TAG, "创建房间失败");
                    }
                }
            });
        }else {
            Log.i(TAG, "获取MediaProjection失败");
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog dialog = new AlertDialog.Builder(this).setMessage(getString(R.string.do_you_really_want_to_leave)).setTitle(getString(R.string.hint)).setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    stopCapture();
                    mZegoLiveRoom.logoutRoom();
                    mZegoLiveRoom.setZegoLivePublisherCallback(null);

                    ZegoApiManager.getInstance().releaseSDK();
                    mZegoLiveRoom.setLiveMode(ZegoLiveRoom.LIVE_MODE_CAMERA);
                    ZegoApiManager.getInstance().initSDK();

                    dialog.dismiss();
                    finish();
                }
            }).setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).create();

            dialog.show();

        }
        return super.onKeyDown(keyCode, event);
    }
}
