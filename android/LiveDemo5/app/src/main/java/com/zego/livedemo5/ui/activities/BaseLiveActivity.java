package com.zego.livedemo5.ui.activities;


import android.Manifest;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zego.livedemo5.R;
import com.zego.livedemo5.ZegoApiManager;
import com.zego.livedemo5.ui.activities.base.AbsBaseLiveActivity;
import com.zego.livedemo5.ui.adapters.CommentsAdapter;
import com.zego.livedemo5.ui.widgets.PublishSettingsPannel;
import com.zego.livedemo5.ui.widgets.ViewLive;
import com.zego.livedemo5.utils.LiveQualityLogger;
import com.zego.livedemo5.utils.PreferenceUtil;
import com.zego.livedemo5.utils.ZegoRoomUtil;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.im.IZegoRoomMessageCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoIM;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoConversationMessage;
import com.zego.zegoliveroom.entity.ZegoRoomMessage;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;
import com.zego.zegoliveroom.entity.ZegoUserState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import butterknife.OnClick;

/**
 * des: 主页面
 */
public abstract class BaseLiveActivity extends AbsBaseLiveActivity {

    public static final String MY_SELF = "MySelf";

    protected InputStream mIsBackgroundMusic = null;

    protected LinkedList<ViewLive> mListViewLive = new LinkedList<>();

    protected LinkedList<String> mListLog = new LinkedList<>();

    protected TextView mTvPublisnControl = null;

    protected TextView mTvPublishSetting = null;

    protected TextView mTvSpeaker = null;

    protected EditText mEdtMessage = null;

    protected TextView mTvSendRoomMsg = null;

    protected BottomSheetBehavior mBehavior = null;

    protected RelativeLayout mRlytControlHeader = null;

    protected TextView mTvTag = null;

    protected String mPublishTitle = null;

    protected String mPublishStreamID = null;

    protected boolean mIsPublishing = false;

    protected boolean mEnableSpeaker = true;

    protected boolean mEnableCamera = true;

    protected boolean mEnableFrontCam = true;

    protected boolean mEnableMic = true;

    protected boolean mEnableTorch = false;

    protected boolean mEnableBackgroundMusic = false;

    protected int mSelectedBeauty = 0;

    protected int mSelectedFilter = 0;

    protected int mLiveCount = 0;

    protected boolean mHostHasBeenCalled = false;

    protected ZegoLiveRoom mZegoLiveRoom = null;

    protected String mRoomID = null;

    protected PhoneStateListener mPhoneStateListener = null;

    protected PublishSettingsPannel mSettingsPannel = null;

    protected AlertDialog mDialogHandleRequestPublish = null;

    /**
     * 推流标记, PublishFlag.JoinPublish:连麦, PublishFlag.MixStream:混流, PublishFlag.SingleAnchor:单主播
     */
    protected int mPublishFlag = ZegoConstants.PublishFlag.JoinPublish;

    /**
     * app朝向, Surface.ROTATION_0或者Surface.ROTATION_180表示竖屏推流,
     * Surface.ROTATION_90或者Surface.ROTATION_270表示横屏推流.
     */
    protected int mAppOrientation = Surface.ROTATION_0;

    protected ListView mLvComments = null;

    private CommentsAdapter mCommentsAdapter = null;

    protected List<ZegoStreamInfo> mListStreamOfRoom = new ArrayList<>();

    protected String mMixStreamID = null;

    protected List<ZegoUserState> mListRoomUser = new ArrayList<>();

    protected abstract void initPublishControlText();

    protected abstract void doPublish();

    protected abstract void hidePlayBackground();

    protected abstract void initPublishConfigs();

    protected abstract void initPlayConfgis(ViewLive viewLive, String streamID);

    protected abstract void sendRoomMessage();


    @Override
    protected int getContentViewLayout() {
        return R.layout.activity_live;
    }


    @Override
    protected void initExtraData(Bundle savedInstanceState) {
        LiveQualityLogger.open();
    }


    @Override
    protected void initVariables(final Bundle savedInstanceState) {
        mZegoLiveRoom = ZegoApiManager.getInstance().getZegoLiveRoom();
        // 初始化电话监听器
        initPhoneCallingListener();
    }

    /**
     * 初始化设置面板.
     */
    private void initSettingPannel() {

        mSettingsPannel = (PublishSettingsPannel) findViewById(R.id.publishSettingsPannel);
        mSettingsPannel.initPublishSettings(mEnableCamera, mEnableFrontCam, mEnableMic, mEnableTorch, mEnableBackgroundMusic, mSelectedBeauty, mSelectedFilter);
        mSettingsPannel.setPublishSettingsCallback(new PublishSettingsPannel.PublishSettingsCallback() {
            @Override
            public void onEnableCamera(boolean isEnable) {
                mEnableCamera = isEnable;
                mZegoLiveRoom.enableCamera(isEnable);
            }

            @Override
            public void onEnableFrontCamera(boolean isEnable) {
                mEnableFrontCam = isEnable;
                mZegoLiveRoom.setFrontCam(isEnable);
            }

            @Override
            public void onEnableMic(boolean isEnable) {
                mEnableMic = isEnable;
                mZegoLiveRoom.enableMic(isEnable);
            }

            @Override
            public void onEnableTorch(boolean isEnable) {
                mEnableTorch = isEnable;
                mZegoLiveRoom.enableTorch(isEnable);
            }

            @Override
            public void onEnableBackgroundMusic(boolean isEnable) {
                mEnableBackgroundMusic = isEnable;
                mZegoLiveRoom.enableAux(isEnable);

                if (!isEnable) {
                    if (mIsBackgroundMusic != null) {
                        try {
                            mIsBackgroundMusic.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mIsBackgroundMusic = null;
                    }
                }
            }

            @Override
            public void onSetBeauty(int beauty) {
                mSelectedBeauty = beauty;
                mZegoLiveRoom.enableBeautifying(ZegoRoomUtil.getZegoBeauty(beauty));
            }

            @Override
            public void onSetFilter(int filter) {
                mSelectedFilter = filter;
                mZegoLiveRoom.setFilter(filter);
            }
        });

        mBehavior = BottomSheetBehavior.from(mSettingsPannel);
        FrameLayout flytMainContent = (FrameLayout) findViewById(R.id.main_content);
        if (flytMainContent != null) {
            flytMainContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }
            });
        }
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {

        mTvSpeaker = (TextView) findViewById(R.id.tv_speaker);
        mTvPublishSetting = (TextView) findViewById(R.id.tv_publish_settings);
        mTvPublisnControl = (TextView) findViewById(R.id.tv_publish_control);
        // 初始化推流控制按钮
        initPublishControlText();
        mTvPublisnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doPublish();
            }
        });

        mRlytControlHeader = (RelativeLayout) findViewById(R.id.rlyt_control_header);

        initSettingPannel();

        final ViewLive vlBigView = (ViewLive) findViewById(R.id.vl_big_view);
        if (vlBigView != null) {
            vlBigView.setActivityHost(this);
            vlBigView.setZegoLiveRoom(mZegoLiveRoom);
            vlBigView.setShareToQQCallback(new ViewLive.IShareToQQCallback() {
                @Override
                public String getRoomID() {
                    return mRoomID;
                }
            });
            mListViewLive.add(vlBigView);
        }

        initViewList(vlBigView);

        mTvSpeaker.setSelected(!mEnableSpeaker);

        mTvSendRoomMsg = (TextView) findViewById(R.id.tv_send);
        mTvSendRoomMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRoomMessage();
            }
        });

        mEdtMessage = (EditText) findViewById(R.id.et_msg);
        mEdtMessage.setSelection(mEdtMessage.getText().length());

        mLvComments = (ListView) findViewById(R.id.lv_comments);
        mCommentsAdapter = new CommentsAdapter(this, new ArrayList<ZegoRoomMessage>());

        mLvComments.setAdapter(mCommentsAdapter);

        mTvTag = (TextView) findViewById(R.id.tv_tag);

        mRlytControlHeader.bringToFront();
    }

    private void initViewList(final ViewLive vlBigView) {

        List<ViewLive> list = new ArrayList<>();

        LinearLayout llViewList = (LinearLayout) findViewById(R.id.ll_viewlist);
        for (int i = 0, llChildListSize = llViewList.getChildCount(); i < llChildListSize; i++) {
            if (llViewList.getChildAt(i) instanceof LinearLayout) {
                LinearLayout llChildList = (LinearLayout) llViewList.getChildAt(i);

                for (int j = 0, viewLiveSize = llChildList.getChildCount(); j < viewLiveSize; j++) {
                    if (llChildList.getChildAt(j) instanceof ViewLive) {
                        final ViewLive viewLive = (ViewLive) llChildList.getChildAt(j);

                        viewLive.setActivityHost(this);
                        viewLive.setZegoLiveRoom(mZegoLiveRoom);
                        viewLive.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                viewLive.toExchangeView(vlBigView);
                            }
                        });

                        list.add((ViewLive) llChildList.getChildAt(j));
                    }
                }
            }
        }

        for (int size = list.size(), i = size - 1; i >= 0; i--) {
            mListViewLive.add(list.get(i));
        }
    }

    @Override
    protected void doBusiness(Bundle savedInstanceState) {
    }

    @Override
    protected void onPause() {
        super.onPause();

        PreferenceUtil.getInstance().setObjectToString(LogListActivity.KEY_LIST_LOG, mListLog);
    }

    /**
     * 电话状态监听.
     */
    protected void initPhoneCallingListener() {
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mHostHasBeenCalled) {
                            mHostHasBeenCalled = false;
                            recordLog(MY_SELF + ": call state idle");
                            // 登陆频道
                            for (ViewLive viewLive : mListViewLive) {
                                if (viewLive.isPublishView()) {
                                    startPublish();
                                } else if (viewLive.isPlayView()) {
                                    startPlay(viewLive.getStreamID());
                                }
                            }
                        }

                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        recordLog(MY_SELF + ": call state ringing");
                        mHostHasBeenCalled = true;
                        // 来电停止发布与播放
                        stopAllStream();
                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        break;
                }
            }
        };

        TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * 获取空闲的View用于播放或者发布.
     *
     * @return
     */
    protected ViewLive getFreeViewLive() {
        ViewLive vlFreeView = null;
        for (int i = 0, size = mListViewLive.size(); i < size; i++) {
            ViewLive viewLive = mListViewLive.get(i);
            if (viewLive.isFree()) {
                vlFreeView = viewLive;
                vlFreeView.setVisibility(View.VISIBLE);
                break;
            }
        }
        return vlFreeView;
    }

    /**
     * 释放View用于再次播放.
     *
     * @param streamID
     */
    protected void releaseLiveView(String streamID) {
        if (TextUtils.isEmpty(streamID)) {
            return;
        }

        for (int i = 0, size = mListViewLive.size(); i < size; i++) {
            ViewLive currentViewLive = mListViewLive.get(i);
            if (streamID.equals(currentViewLive.getStreamID())) {
                int j = i;
                for (; j < size - 1; j++) {
                    ViewLive nextViewLive = mListViewLive.get(j + 1);
                    if (nextViewLive.isFree()) {
                        break;
                    }

                    if (nextViewLive.isPublishView()) {
                        mZegoLiveRoom.setPreviewView(currentViewLive.getTextureView());
                    } else {
                        mZegoLiveRoom.updatePlayView(nextViewLive.getStreamID(), currentViewLive.getTextureView());
                    }

                    currentViewLive.toExchangeView(nextViewLive);
                    currentViewLive = nextViewLive;
                }
                // 标记最后一个View可用
                mListViewLive.get(j).setFree();
                break;
            }
        }
    }

    /**
     * 通过streamID查找正在publish或者play的ViewLive.
     *
     * @param streamID
     * @return
     */
    protected ViewLive getViewLiveByStreamID(String streamID) {
        if (TextUtils.isEmpty(streamID)) {
            return null;
        }

        ViewLive viewLive = null;
        for (ViewLive vl : mListViewLive) {
            if (streamID.equals(vl.getStreamID())) {
                viewLive = vl;
                break;
            }
        }

        return viewLive;
    }

    protected void recordLog(String msg) {
        mListLog.addFirst(msg);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            publishStream();
                        }
                    });
                } else {


                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, R.string.allow_camera_permission, Toast.LENGTH_LONG).show();
                    }
                    if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, R.string.open_recorder_permission, Toast.LENGTH_LONG).show();
                    }

                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
                break;
        }
    }

    protected void publishStream() {

        if (TextUtils.isEmpty(mPublishStreamID)) {
            return;
        }

        ViewLive freeViewLive = getFreeViewLive();
        if (freeViewLive == null) {
            return;
        }

        // 设置流信息
        freeViewLive.setStreamID(mPublishStreamID);
        freeViewLive.setPublishView(true);

        // 初始化配置信息, 混流模式使用
        initPublishConfigs();

        // 输出发布状态
        recordLog(MY_SELF + ": start publishing(" + mPublishStreamID + ")");

        // 设置水印
        mZegoLiveRoom.setWaterMarkImagePath("asset:watermark.png");
        Rect rect = new Rect();
        rect.left = 50;
        rect.top = 20;
        rect.right = 200;
        rect.bottom = 170;
        mZegoLiveRoom.setPreviewWaterMarkRect(rect);
        mZegoLiveRoom.setPublishWaterMarkRect(rect);


        // 开始播放
        mZegoLiveRoom.setPreviewView(freeViewLive.getTextureView());
        mZegoLiveRoom.startPreview();
        mZegoLiveRoom.startPublishing(mPublishStreamID, mPublishTitle, mPublishFlag);
        mZegoLiveRoom.setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
    }

    /**
     * 开始发布.
     */
    protected void startPublish() {
        // 6.0及以上的系统需要在运行时申请CAMERA RECORD_AUDIO权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 101);
            } else {
                publishStream();
            }
        } else {
            publishStream();
        }
    }

    protected void stopPublish() {
        if (mIsPublishing) {
            // 临时处理
            handlePublishStop(1, mPublishStreamID);
            initPublishControlText();

            recordLog(MY_SELF + ": stop publishing(" + mPublishStreamID + ")");
            mZegoLiveRoom.stopPreview();
            mZegoLiveRoom.stopPublishing();
            mZegoLiveRoom.setPreviewView(null);
        }
    }

    protected void stopPlay(String streamID) {
        if (!TextUtils.isEmpty(streamID)) {
            // 临时处理
            handlePlayStop(1, streamID);

            // 输出播放状态
            recordLog(MY_SELF + ": stop play stream(" + streamID + ")");
            mZegoLiveRoom.stopPlayingStream(streamID);
        }
    }

    private boolean isStreamExisted(String streamID) {
        if (TextUtils.isEmpty(streamID)) {
            return true;
        }

        boolean isExisted = false;

        for (ViewLive viewLive : mListViewLive) {
            if (streamID.equals(viewLive.getStreamID())) {
                isExisted = true;
                break;
            }
        }

        return isExisted;
    }

    /**
     * 开始播放流.
     */
    protected void startPlay(String streamID) {

        if (TextUtils.isEmpty(streamID)) {
            return;
        }

        if (isStreamExisted(streamID)) {
            Toast.makeText(this, "流已存在", Toast.LENGTH_SHORT).show();
            return;
        }

        ViewLive freeViewLive = getFreeViewLive();
        if (freeViewLive == null) {
            return;
        }

        // 设置流信息
        freeViewLive.setStreamID(streamID);
        freeViewLive.setPlayView(true);

        // 输出播放状态
        recordLog(MY_SELF + ": start play stream(" + streamID + ")");

        // 初始化拉流参数, 外部渲染模式使用
        initPlayConfgis(freeViewLive, streamID);

        // 播放
        mZegoLiveRoom.startPlayingStream(streamID, freeViewLive.getTextureView());
        mZegoLiveRoom.setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamID);
    }

    protected void logout() {

        if (mIsPublishing) {
            AlertDialog dialog = new AlertDialog.Builder(this).setMessage(getString(R.string.do_you_really_want_to_leave)).setTitle(getString(R.string.hint)).setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    stopAllStream();
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
        } else {

            stopAllStream();
            finish();
        }

    }


    protected void stopAllStream() {
        for (ViewLive viewLive : mListViewLive) {
            if (viewLive.isPublishView()) {
                stopPublish();
            } else if (viewLive.isPlayView()) {
                stopPlay(viewLive.getStreamID());
            }
            // 释放view
            viewLive.setFree();
        }
    }

    protected void setPublishEnabled() {
        if (!mIsPublishing) {
            if (mLiveCount < ZegoLiveRoom.getMaxPlayChannelCount()) {
                mTvPublisnControl.setEnabled(true);
            } else {
                mTvPublisnControl.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                return false;
            } else {
                // 退出
                logout();
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @OnClick(R.id.tv_log_list)
    public void openLogList() {
        LogListActivity.actionStart(this);
    }

    @OnClick(R.id.tv_publish_settings)
    public void publishSettings() {
        if (mBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @OnClick(R.id.tv_speaker)
    public void doMute() {
        if (mEnableSpeaker) {
            mEnableSpeaker = false;
        } else {
            mEnableSpeaker = true;
        }

        mZegoLiveRoom.enableSpeaker(mEnableSpeaker);
        mTvSpeaker.setSelected(!mEnableSpeaker);
    }

    @OnClick(R.id.tv_close)
    public void close() {
        logout();
    }

    /**
     * 推流成功.
     */
    protected void handlePublishSucc(String streamID) {
        mIsPublishing = true;
        recordLog(MY_SELF + ": onPublishSucc(" + streamID + ")");

        initPublishControlText();
//        mRlytControlHeader.bringToFront();
    }

    /**
     * 停止推流.
     */
    protected void handlePublishStop(int stateCode, String streamID) {
        mIsPublishing = false;
        recordLog(MY_SELF + ": onPublishStop(" + streamID + ") --stateCode:" + stateCode);

        // 释放View
        releaseLiveView(streamID);

        initPublishControlText();
//        mRlytControlHeader.bringToFront();
    }

    /**
     * 拉流成功.
     */
    protected void handlePlaySucc(String streamID) {
        recordLog(MY_SELF + ": onPlaySucc(" + streamID + ")");

        mLiveCount++;
        setPublishEnabled();

        mRlytControlHeader.bringToFront();
    }

    /**
     * 停止拉流.
     */
    protected void handlePlayStop(int stateCode, String streamID) {
        recordLog(MY_SELF + ": onPlayStop(" + streamID + ") --stateCode:" + stateCode);

        // 释放View
        releaseLiveView(streamID);

        mLiveCount--;
        setPublishEnabled();

        mRlytControlHeader.bringToFront();
    }

    /**
     * 推流质量更新.
     */
    protected void handlePublishQualityUpdate(String streamID, int quality, double videoFPS, double videoBitrate) {
        ViewLive viewLive = getViewLiveByStreamID(streamID);
        if (viewLive != null) {
            viewLive.setLiveQuality(quality, videoFPS, videoBitrate);
        }

        // for espresso test, don't delete the log
        LiveQualityLogger.write("publishStreamQuality:%d, streamId: %s, videoFPS: %.2f, videoBitrate: %.2fKb/s", quality, streamID, videoFPS, videoBitrate);
    }

    protected AuxData handleAuxCallback(int dataLen) {
        // 开启伴奏后, sdk每20毫秒一次取数据
        if (!mEnableBackgroundMusic || dataLen <= 0) {
            return null;
        }

        AuxData auxData = new AuxData();
        auxData.dataBuf = new byte[dataLen];

        try {
            AssetManager am = getAssets();
            if (mIsBackgroundMusic == null) {
                mIsBackgroundMusic = am.open("a.pcm");
            }
            int len = mIsBackgroundMusic.read(auxData.dataBuf);

            if (len <= 0) {
                // 歌曲播放完毕
                mIsBackgroundMusic.close();
                mIsBackgroundMusic = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        auxData.channelCount = 2;
        auxData.sampleRate = 44100;


        return auxData;
    }

    /**
     * 拉流质量更新.
     */
    protected void handlePlayQualityUpdate(String streamID, int quality, double videoFPS, double videoBitrate) {
        ViewLive viewLive = getViewLiveByStreamID(streamID);
        if (viewLive != null) {
            viewLive.setLiveQuality(quality, videoFPS, videoBitrate);
        }

        // for espresso test, don't delete the log
        LiveQualityLogger.write("playStreamQuality: %d, streamId: %s, videoFPS: %.2f, videoBitrate: %.2fKb/s", quality, streamID, videoFPS, videoBitrate);
    }

    /**
     * 拉流分辨率更新.
     */
    protected void handleVideoSizeChanged(String streamID, int width, int height) {
        hidePlayBackground();

        if (width > height) {
            ViewLive viewLivePlay = getViewLiveByStreamID(streamID);
            if (viewLivePlay != null) {
                if (viewLivePlay.getWidth() < viewLivePlay.getHeight()) {
                    viewLivePlay.setZegoVideoViewMode(true, ZegoVideoViewMode.ScaleAspectFit);
                    mZegoLiveRoom.setViewMode(ZegoVideoViewMode.ScaleAspectFit, streamID);
                } else {
                    viewLivePlay.setZegoVideoViewMode(true, ZegoVideoViewMode.ScaleAspectFill);
                    mZegoLiveRoom.setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamID);
                }
            }
        }

//        mRlytControlHeader.bringToFront();

    }

    /**
     * 房间内用户创建流.
     */
    protected void handleStreamAdded(final ZegoStreamInfo[] listStream, final String roomID) {
        if (listStream != null && listStream.length > 0) {
            for (int i = 0; i < listStream.length; i++) {
                recordLog(listStream[i].userName + ": added stream(" + listStream[i].streamID + ")");
                startPlay(listStream[i].streamID);
            }
        }
    }

    /**
     * 房间内用户删除流.
     */
    protected void handleStreamDeleted(final ZegoStreamInfo[] listStream, final String roomID) {
        if (listStream != null && listStream.length > 0) {
            for (int i = 0; i < listStream.length; i++) {
                recordLog(listStream[i].userName + ": deleted stream(" + listStream[i].streamID + ")");
                stopPlay(listStream[i].streamID);
            }
        }
    }


    /**
     * 用户掉线.
     */
    protected void handleDisconnect(int errorCode, String roomID){
        recordLog(MY_SELF + ": onDisconnected, roomID:" + roomID + ", errorCode:" + errorCode);
    }

    /**
     * 用户更新.
     */
    protected void handleUserUpdate(ZegoUserState[] listUser, int updateType) {
       if(listUser != null){
           if(updateType == ZegoIM.UserUpdateType.Total){
               mListRoomUser.clear();
           }

           if(updateType == ZegoIM.UserUpdateType.Increase){
               for(ZegoUserState zegoUserState : listUser) {
                   if(zegoUserState.updateFlag == ZegoIM.UserUpdateFlag.Added){
                        mListRoomUser.add(zegoUserState);
                   }else if(zegoUserState.updateFlag == ZegoIM.UserUpdateFlag.Deleted){
                        mListRoomUser.remove(zegoUserState);
                   }
               }
           }
       }
    }

    /**
     * 房间聊天消息.
     */
    protected void handleRecvRoomMsg(String roomID, ZegoRoomMessage[] listMsg) {

        List<ZegoRoomMessage> listTextMsg = new ArrayList<>();
        for (ZegoRoomMessage message : listMsg) {

            // 文字聊天消息
            if (message.messageType == ZegoIM.MessageType.Text && message.messageCategory == ZegoIM.MessageCategory.Chat) {
                listTextMsg.add(message);
            }

        }

        if (listTextMsg.size() > 0) {
            mCommentsAdapter.addMsgList(listTextMsg);

            mLvComments.post(new Runnable() {
                @Override
                public void run() {
                    // 滚动到最后一行
                    mLvComments.setSelection(mCommentsAdapter.getListMsg().size() - 1);
                }
            });
        }
    }

    protected void doSendRoomMsg(final String msg){
        if(TextUtils.isEmpty(msg)){
            Toast.makeText(this, getString(R.string.message_can_not_be_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        ZegoRoomMessage roomMessage = new ZegoRoomMessage();
        roomMessage.fromUserID = PreferenceUtil.getInstance().getUserID();
        roomMessage.fromUserName = getString(R.string.me);
        roomMessage.content = msg;
        roomMessage.messageType = ZegoIM.MessageType.Text;
        roomMessage.messageCategory =  ZegoIM.MessageCategory.Chat;
        roomMessage.messagePriority = ZegoIM.MessagePriority.Default;

        mCommentsAdapter.addMsg(roomMessage);
        mLvComments.post(new Runnable() {
            @Override
            public void run() {
                // 滚动到最后一行
                mLvComments.setSelection(mCommentsAdapter.getListMsg().size() - 1);
            }
        });

        mZegoLiveRoom.sendRoomMessage(ZegoIM.MessageType.Text, ZegoIM.MessageCategory.Chat, ZegoIM.MessagePriority.Default, msg, new IZegoRoomMessageCallback() {
            @Override
            public void onSendRoomMessage(int errorCode, String roomID, long messageID) {
                if (errorCode == 0) {
                    recordLog(MY_SELF + ": 发送房间消息成功, roomID:" + roomID);
                } else {
                    recordLog(MY_SELF + ": 发送房间消息失败, roomID:" + roomID + ", messageID:" + messageID);
                }
            }
        });
    }

    /**
     * 会话消息.
     */
    protected void handleRecvConversationMsg(String roomID, String conversationID, ZegoConversationMessage message) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 注销电话监听
        TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mPhoneStateListener = null;

        // 清空回调, 避免内存泄漏
        mZegoLiveRoom.setZegoLivePublisherCallback(null);
        mZegoLiveRoom.setZegoLivePlayerCallback(null);
        mZegoLiveRoom.setZegoRoomCallback(null);

        // 退出房间
        mZegoLiveRoom.logoutRoom();
        LiveQualityLogger.close();
    }


    /**
     * 设置推流朝向.
     */
    protected void setAppOrientation() {
        // 设置app朝向
        int currentOrientation = getWindowManager().getDefaultDisplay().getRotation();
        mZegoLiveRoom.setAppOrientation(currentOrientation);

        // 设置推流配置
        ZegoAvConfig currentConfig = ZegoApiManager.getInstance().getZegoAvConfig();
        int videoWidth = currentConfig.getVideoEncodeResolutionWidth();
        int videoHeight = currentConfig.getVideoEncodeResolutionHeight();
        if (((currentOrientation == Surface.ROTATION_0 || currentOrientation == Surface.ROTATION_180) && videoWidth > videoHeight) ||
                ((currentOrientation == Surface.ROTATION_90 || currentOrientation == Surface.ROTATION_270) && videoHeight > videoWidth)) {
            currentConfig.setVideoEncodeResolution(videoHeight, videoWidth);
            currentConfig.setVideoCaptureResolution(videoHeight, videoWidth);
        }
        ZegoApiManager.getInstance().setZegoConfig(currentConfig);
    }

    /**
     * 处理页面朝向变化, 目前只针对拉流.
     */
    protected void handleConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int currentOrientation = getWindowManager().getDefaultDisplay().getRotation();
        for (ViewLive viewLive : mListViewLive) {
            if (viewLive.isPlayView()) {
                if (viewLive.isNeedToSwitchFullScreen() && viewLive.getZegoVideoViewMode() == ZegoVideoViewMode.ScaleAspectFill) {
                    if (currentOrientation == Surface.ROTATION_90 || currentOrientation == Surface.ROTATION_270) {
                        mZegoLiveRoom.setViewRotation(Surface.ROTATION_0, viewLive.getStreamID());
                    } else {
                        mZegoLiveRoom.setViewRotation(Surface.ROTATION_0, viewLive.getStreamID());
                    }
                } else {
                    mZegoLiveRoom.setViewRotation(currentOrientation, viewLive.getStreamID());
                }
            }
        }
    }

    /**
     * 获取流地址.
     */
    protected List<String> getShareUrlList(HashMap<String, Object> info){
        List<String> listUrls = new ArrayList<>();

        if(info != null){
            String[] hlsList = (String[]) info.get(ZegoConstants.StreamKey.HLS_URL_LST);
            if (hlsList != null && hlsList.length > 0) {
                listUrls.add(hlsList[0]);
            }

            String[] rtmpList = (String[]) info.get(ZegoConstants.StreamKey.RTMP_URL_LIST);
            if (rtmpList != null && rtmpList.length > 0) {
                listUrls.add(rtmpList[0]);
            }
        }
        return listUrls;
    }

}
