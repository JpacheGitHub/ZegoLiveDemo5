#pragma once

#include <tuple>
#include "ZegoSettingsModel.h"
#include "SimpleIni.h"
#include "ZegoSDKSignal.h"

class CZegoBase
{
public:
	CZegoBase(void);
	~CZegoBase(void);

	bool InitAVSdk(SettingsPtr pCurSetting, std::string userID, std::string userName);
	void UninitAVSdk(void);
	bool IsAVSdkInited(void);

	CZegoAVSignal& GetAVSignal(void);
    DWORD GetAppID(void);
private:
	typedef enum _INIT_MASK {
		INIT_NONE	= 0,
		INIT_AVSDK	= 1,
		INIT_CRSDK	= 2,
		INIT_ALL		= INIT_AVSDK | INIT_CRSDK,
	}INIT_MASK;

	DWORD m_dwInitedMask;
	std::string m_strLogPathUTF8;

	HWND m_hCommuWnd;
	CZegoAVSignal* m_pAVSignal;
};