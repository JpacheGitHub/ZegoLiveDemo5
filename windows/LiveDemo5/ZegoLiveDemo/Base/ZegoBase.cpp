#include "stdafx.h"
#include "ZegoBase.h"
#include "ZegoUtility.h"
#include "ZegoSigslotDefine.h"
#include "LiveRoom.h"
#include "LiveRoom-IM.h"
#include "LiveRoom-Player.h"
#include "LiveRoom-Publisher.h"
#include "zego_sdk_protocol.h"


#ifdef ZEGO_PROTOCOL_UDP
static DWORD g_dwAppID2 = 10;

static BYTE g_bufSignKey2[] =
{
};
#else
static DWORD g_dwAppID2 = 0;
static BYTE g_bufSignKey2[] =
{
};
#endif


LRESULT CALLBACK ZegoCommuExchangeWndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
	if (uMsg == WM_ZEGO_SWITCH_THREAD)
	{
		std::function<void(void)>* pFunc = (std::function<void(void)>*)wParam;
		(*pFunc)();
		delete pFunc;
	}

	return DefWindowProc(hWnd, uMsg, wParam, lParam);
}

CZegoBase::CZegoBase(void) : m_dwInitedMask(INIT_NONE)
{
	WCHAR szAppName[MAX_PATH] = {0};
	::GetModuleFileNameW(NULL, szAppName, MAX_PATH);
	CString strAppFullName = szAppName;
	CString strPath = strAppFullName.Left(strAppFullName.ReverseFind('\\') + 1);
	m_strLogPathUTF8 = WStringToUTF8(strPath);

	// 创建隐藏的通信窗体
	WNDCLASSEX wcex = { sizeof(WNDCLASSEX) };
	wcex.hInstance = GetModuleHandle(0);
	wcex.lpszClassName = ZegoCommWndClassName;
	wcex.lpfnWndProc = &ZegoCommuExchangeWndProc;
	wcex.hbrBackground = (HBRUSH)GetStockObject(NULL_BRUSH);
	RegisterClassEx(&wcex);
	m_hCommuWnd = CreateWindowEx(WS_EX_TOOLWINDOW, wcex.lpszClassName, ZegoCommWndName, WS_POPUP, 0, 0, 100, 100,
		NULL, NULL, wcex.hInstance, NULL);
	ShowWindow(m_hCommuWnd, SW_HIDE);

	m_pAVSignal = new CZegoAVSignal;
}

CZegoBase::~CZegoBase(void)
{
	UninitAVSdk();

	delete m_pAVSignal;

	DestroyWindow(m_hCommuWnd);
	CloseWindow(m_hCommuWnd);
}

bool CZegoBase::InitAVSdk(SettingsPtr pCurSetting, std::string userID, std::string userName)
{
	if (!IsAVSdkInited())
	{
        LIVEROOM::SetLogDir(m_strLogPathUTF8.c_str());
        LIVEROOM::SetBusinessType(0);
        LIVEROOM::SetUser(userID.c_str(), userName.c_str());
        // ToDo: 需要通过代码获取网络类型
        LIVEROOM::SetNetType(2);
		LIVEROOM::InitSDK(g_dwAppID2, g_bufSignKey2, 32);

        LIVEROOM::SetLivePublisherCallback(m_pAVSignal);
        LIVEROOM::SetLivePlayerCallback(m_pAVSignal);
        LIVEROOM::SetRoomCallback(m_pAVSignal);
        LIVEROOM::SetIMCallback(m_pAVSignal);
        LIVEROOM::SetDeviceStateCallback(m_pAVSignal);
	}

    LIVEROOM::EnableAux(false);
    LIVEROOM::SetPlayVolume(100);
	if (!pCurSetting->GetMircophoneId().empty())
	{
        LIVEROOM::SetAudioDevice(AV::AudioDevice_Input, pCurSetting->GetMircophoneId().c_str());
	}

    LIVEROOM::SetVideoCaptureResolution(pCurSetting->GetResolution().cx, pCurSetting->GetResolution().cy);
    LIVEROOM::SetVideoEncodeResolution(pCurSetting->GetResolution().cx, pCurSetting->GetResolution().cy);
    LIVEROOM::SetVideoBitrate(pCurSetting->GetBitrate());
    LIVEROOM::SetVideoFPS(pCurSetting->GetFps());
	if (!pCurSetting->GetCameraId().empty())
	{
        LIVEROOM::SetVideoDevice(pCurSetting->GetCameraId().c_str());
	}

	m_dwInitedMask |= INIT_AVSDK;
	return true;
}

void CZegoBase::UninitAVSdk(void)
{
	if (IsAVSdkInited())
	{
        LIVEROOM::SetLivePublisherCallback(nullptr);
        LIVEROOM::SetLivePlayerCallback(nullptr);
        LIVEROOM::SetRoomCallback(nullptr);
        LIVEROOM::SetIMCallback(nullptr);
        LIVEROOM::SetDeviceStateCallback(nullptr);

        LIVEROOM::UnInitSDK();

		DWORD dwNegation = ~(DWORD)INIT_AVSDK;
		m_dwInitedMask &= dwNegation;
	}
}

bool CZegoBase::IsAVSdkInited(void)
{
	return (m_dwInitedMask & INIT_AVSDK) == INIT_AVSDK;
}

CZegoAVSignal& CZegoBase::GetAVSignal(void)
{
	return *m_pAVSignal;
}

DWORD CZegoBase::GetAppID(void)
{
    return g_dwAppID2;
}
