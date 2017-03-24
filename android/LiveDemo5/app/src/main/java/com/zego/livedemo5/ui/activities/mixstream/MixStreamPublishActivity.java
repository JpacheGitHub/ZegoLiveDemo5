package com.zego.livedemo5.ui.activities.mixstream;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.zego.livedemo5.R;
import com.zego.livedemo5.ZegoApiManager;
import com.zego.livedemo5.constants.Constants;
import com.zego.livedemo5.constants.IntentExtra;
import com.zego.livedemo5.ui.activities.BasePublishActivity;
import com.zego.livedemo5.ui.widgets.ViewLive;
import com.zego.livedemo5.utils.ZegoRoomUtil;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.callback.im.IZegoIMCallback;
import com.zego.zegoliveroom.callback.im.IZegoRoomMessageCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoIM;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoConversationMessage;
import com.zego.zegoliveroom.entity.ZegoMixStreamInfo;
import com.zego.zegoliveroom.entity.ZegoRoomMessage;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;
import com.zego.zegoliveroom.entity.ZegoUserState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.OnClick;

/**
 * Copyright © 2016 Zego. All rights reserved.
 * des:
 */
public class MixStreamPublishActivity extends BasePublishActivity {

    protected List<ZegoMixStreamInfo> mMixStreamInfos = new ArrayList<>();

    /**
     * 启动入口.
     *
     * @param activity     源activity
     * @param publishTitle 视频标题
     */
    public static void actionStart(Activity activity, String publishTitle, boolean enableFrontCam, boolean enableTorch, int selectedBeauty, int selectedFilter, int appOrientation) {
        Intent intent = new Intent(activity, MixStreamPublishActivity.class);
        intent.putExtra(IntentExtra.PUBLISH_TITLE, publishTitle);
        intent.putExtra(IntentExtra.ENABLE_FRONT_CAM, enableFrontCam);
        intent.putExtra(IntentExtra.ENABLE_TORCH, enableTorch);
        intent.putExtra(IntentExtra.SELECTED_BEAUTY, selectedBeauty);
        intent.putExtra(IntentExtra.SELECTED_FILTER, selectedFilter);
        intent.putExtra(IntentExtra.APP_ORIENTATION, appOrientation);
        activity.startActivity(intent);
    }


    @Override
    protected void initViews(Bundle savedInstanceState) {
        super.initViews(savedInstanceState);
    }

    @Override
    protected void doBusiness(Bundle savedInstanceState) {
        super.doBusiness(savedInstanceState);
        mRoomID = ZegoRoomUtil.getRoomID(ZegoRoomUtil.ROOM_TYPE_MIX);

        // 登录房间
        mZegoLiveRoom.loginRoom(mRoomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                if (errorCode == 0) {
                    handleAnchorLoginRoomSuccess(zegoStreamInfos);
                } else {
                    handleAnchorLoginRoomFail(errorCode);
                }
            }
        });

        // 主播回调
        mZegoLiveRoom.setZegoLivePublisherCallback(new IZegoLivePublisherCallback() {
            @Override
            public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> streamInfo) {
                //推流状态更新
                if (stateCode == 0) {
                    handlePublishSuccMix(streamID, streamInfo);
                } else {
                    handlePublishStop(stateCode, streamID);
                }
            }

            @Override
            public void onJoinLiveRequest(final int seq, String fromUserID, String fromUserName, String roomID) {
                // 有人请求连麦
                handleJoinLiveRequest(seq, fromUserID, fromUserName, roomID);
            }

            @Override
            public void onPublishQualityUpdate(String streamID, int quality, double videoFPS, double videoBitrate) {

                // 推流质量回调
                handlePublishQualityUpdate(streamID, quality, videoFPS, videoBitrate);
            }

            @Override
            public AuxData onAuxCallback(int dataLen) {
                return handleAuxCallback(dataLen);
            }

            @Override
            public void onCaptureVideoSizeChangedTo(int width, int height) {

            }

            @Override
            public void onMixStreamConfigUpdate(int errorCode, String mixStreamID, HashMap<String, Object> streamInfo) {
                handleMixStreamStateUpdate(errorCode, mixStreamID, streamInfo);
            }
        });

        // 观众回调
        mZegoLiveRoom.setZegoLivePlayerCallback(new IZegoLivePlayerCallback() {
            @Override
            public void onPlayStateUpdate(int stateCode, String streamID) {
                // 拉流状态更新

                if (stateCode == 0) {
                    handlePlaySucc(streamID);
                } else {
                    handlePlayStop(stateCode, streamID);
                }
            }

            @Override
            public void onPlayQualityUpdate(String streamID, int quality, double videoFPS, double videoBitrate) {
                // 拉流质量回调
                handlePlayQualityUpdate(streamID, quality, videoFPS, videoBitrate);
            }

            @Override
            public void onInviteJoinLiveRequest(int seq, String fromUserID, String fromUserName, String roomID) {

            }

            @Override
            public void onVideoSizeChangedTo(String streamID, int width, int height) {
                handleVideoSizeChanged(streamID, width, height);
            }
        });

        mZegoLiveRoom.setZegoRoomCallback(new IZegoRoomCallback() {
            @Override
            public void onKickOut(int reason, String roomID) {

            }

            @Override
            public void onDisconnect(int errorCode, String roomID) {
                handleDisconnect(errorCode, roomID);
            }

            @Override
            public void onStreamUpdated(final int type, final ZegoStreamInfo[] listStream, final String roomID) {
                if (listStream != null && listStream.length > 0) {
                    switch (type) {
                        case ZegoConstants.StreamUpdateType.Added:
                            handleMixStreamAdded(listStream, roomID);
                            break;
                        case ZegoConstants.StreamUpdateType.Deleted:
                            handleMixStreamDeleted(listStream, roomID);
                            break;
                    }
                }
            }

            @Override
            public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String s) {

            }
        });


        mZegoLiveRoom.setZegoIMCallback(new IZegoIMCallback() {

            @Override
            public void onUserUpdate(ZegoUserState[] listUser, int updateType) {
                handleUserUpdate(listUser, updateType);
            }

            @Override
            public void onRecvRoomMessage(String roomID, ZegoRoomMessage[] listMsg) {
                handleRecvRoomMsg(roomID, listMsg);
            }

            @Override
            public void onRecvConversationMessage(String roomID, String conversationID, ZegoConversationMessage message) {
                handleRecvConversationMsg(roomID, conversationID, message);
            }
        });
    }

    /**
     * 房间内用户创建流.
     */
    protected void handleMixStreamAdded(final ZegoStreamInfo[] listStream, final String roomID) {
        ZegoAvConfig zegoAvConfig = ZegoApiManager.getInstance().getZegoAvConfig();

        int width = zegoAvConfig.getVideoEncodeResolutionWidth();
        int height = zegoAvConfig.getVideoEncodeResolutionHeight();
        if (listStream != null && listStream.length > 0) {
            for (ZegoStreamInfo streamInfo : listStream) {
                recordLog(streamInfo.userName + ": onStreamAdd(" + streamInfo.streamID + ")");
                startPlay(streamInfo.streamID);

                if (mMixStreamInfos.size() == 0) {
                    ZegoMixStreamInfo mixStreamInfo = new ZegoMixStreamInfo();
                    mixStreamInfo.streamID = mPublishStreamID;
                    mixStreamInfo.top = 0;
                    mixStreamInfo.bottom = height;
                    mixStreamInfo.left = 0;
                    mixStreamInfo.right = width;
                    mMixStreamInfos.add(mixStreamInfo);
                }

                if (mMixStreamInfos.size() == 1) {
                    ZegoMixStreamInfo mixStreamInfo = new ZegoMixStreamInfo();
                    mixStreamInfo.streamID = streamInfo.streamID;
                    mixStreamInfo.top = (int) (height * 2.0 / 3);
                    mixStreamInfo.bottom = height;
                    mixStreamInfo.left = (int) (width * 2.0 / 3);
                    mixStreamInfo.right = width;
                    mMixStreamInfos.add(mixStreamInfo);

                } else if (mMixStreamInfos.size() == 2) {
                    ZegoMixStreamInfo mixStreamInfo = new ZegoMixStreamInfo();
                    mixStreamInfo.streamID = streamInfo.streamID;
                    mixStreamInfo.top = (int) (height * 2.0 / 3);
                    mixStreamInfo.bottom = height;
                    mixStreamInfo.left = (int) (width * 1.0 / 3) - 10;
                    mixStreamInfo.right = (int) (width * 2.0 / 3) - 10;
                    mMixStreamInfos.add(mixStreamInfo);
                }
            }

            int size = mMixStreamInfos.size();
            ZegoMixStreamInfo[] infos = new ZegoMixStreamInfo[size];

            for (int i = 0; i < size; i++) {
                infos[i] = mMixStreamInfos.get(i);
            }
            mZegoLiveRoom.updateMixInputStreams(infos);
        }
    }

    /**
     * 房间内用户删除流.
     */
    protected void handleMixStreamDeleted(final ZegoStreamInfo[] listStream, final String roomID) {
        if (listStream != null && listStream.length > 0) {
            for (ZegoStreamInfo bizStream : listStream) {
                recordLog(bizStream.userName + ": onStreamDelete(" + bizStream.streamID + ")");
                stopPlay(bizStream.streamID);

                for (ZegoMixStreamInfo info : mMixStreamInfos) {
                    if (bizStream.streamID.equals(info.streamID)) {
                        mMixStreamInfos.remove(info);
                        break;
                    }
                }
            }


            // 更新混流信息
            int size = mMixStreamInfos.size();
            ZegoMixStreamInfo[] infos = new ZegoMixStreamInfo[size];

            for (int i = 0; i < size; i++) {
                infos[i] = mMixStreamInfos.get(i);
            }
            mZegoLiveRoom.updateMixInputStreams(infos);
        }
    }

    protected void handleMixStreamStateUpdate(int errorCode, String mixStreamID, HashMap<String, Object> streamInfo) {
        if (errorCode == 0) {

            ViewLive viewLivePublish = getViewLiveByStreamID(mPublishStreamID);
            List<String> listUrls = getShareUrlList(streamInfo);

            if(listUrls.size() == 0){
                recordLog("混流失败...errorCode:" + errorCode);
            }

            if(viewLivePublish != null && listUrls.size() == 2){
                recordLog("混流地址:" + listUrls.get(1));
                viewLivePublish.setListShareUrls(listUrls);

                // 将混流ID通知观众
                Map<String, String> mapUrls = new HashMap<>();
                mapUrls.put(Constants.FIRST_ANCHOR, String.valueOf(true));
                mapUrls.put(Constants.KEY_MIX_STREAM_ID, mixStreamID);
                mapUrls.put(Constants.KEY_HLS, listUrls.get(0));
                mapUrls.put(Constants.KEY_RTMP, listUrls.get(1));
                Gson gson = new Gson();
                String json = gson.toJson(mapUrls);
                mZegoLiveRoom.updateStreamExtraInfo(json);
            }
        } else {
            recordLog("混流失败...errorCode:" + errorCode);
        }

        mRlytControlHeader.bringToFront();
    }


    @Override
    protected void initPublishControlText() {
        if (mIsPublishing) {
            mTvPublisnControl.setText(R.string.stop_publishing);
            mTvPublishSetting.setEnabled(true);

            mRlytControlHeader.bringToFront();
        } else {
            mTvPublisnControl.setText(R.string.start_publishing);
            mTvPublishSetting.setEnabled(false);
        }

    }

    @Override
    protected void hidePlayBackground() {
    }

    @Override
    protected void initPublishConfigs() {
        // 设置混流标记为2
        mPublishFlag = 2;
        mMixStreamID = "mix-" + mPublishStreamID;

        // 混流配置
        final Map<String, Object> mixStreamConfig = new HashMap<>();
        mixStreamConfig.put(ZegoConstants.StreamKey.MIX_STREAM_ID, mMixStreamID);
        mixStreamConfig.put(ZegoConstants.StreamKey.MIX_STREAM_WIDTH, ZegoApiManager.getInstance().getZegoAvConfig().getVideoCaptureResolutionWidth());
        mixStreamConfig.put(ZegoConstants.StreamKey.MIX_STREAM_HEIGHT, ZegoApiManager.getInstance().getZegoAvConfig().getVideoCaptureResolutionHeight());
        mZegoLiveRoom.setMixStreamConfig(mixStreamConfig);
    }

    @Override
    protected void initPlayConfgis(ViewLive viewLive, String streamID) {
    }

    @Override
    protected void sendRoomMessage() {
        String msg = mEdtMessage.getText().toString();
        if (!TextUtils.isEmpty(msg)) {
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
    }

    @Override
    protected void doPublish() {
        if (mIsPublishing) {
            stopPublish();
        } else {
            startPublish();
        }
    }
}
