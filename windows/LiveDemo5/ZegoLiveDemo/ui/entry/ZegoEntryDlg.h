#pragma once
#include "ZegoRoomDlg.h"
#include "ZegoSettingsModel.h"
#include "ZegoUserConfig.h"
#include "CGridListCtrlX/CGridListCtrlEx.h"
#include "afxwin.h"
#include <afxinet.h>

// CZegoEntryDlg �Ի���

class CZegoEntryDlg : public CDialogEx, public sigslot::has_slots<>
{
	DECLARE_DYNAMIC(CZegoEntryDlg)

public:
	CZegoEntryDlg(CWnd* pParent = NULL);   // ��׼���캯��
	virtual ~CZegoEntryDlg();

// �Ի�������
#ifdef AFX_DESIGN_TIME
	enum { IDD = IDD_DIALOG_ENTRY };
#endif

protected:
	virtual BOOL OnInitDialog();
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV ֧��
	void OnSysCommand(UINT nID, LPARAM lParam);
	void OnClose();
	void OnNcLButtonDown(UINT nHitTest, CPoint point);
	BOOL SetTipText(UINT id, NMHDR *pTTTStruct, LRESULT *pResult);
	DECLARE_MESSAGE_MAP()

	afx_msg void OnCbnSelchangeComboQuality();
	afx_msg void OnBnClickedButtonSetting();
	afx_msg void OnBnClickedButtonRefresh();
	afx_msg void OnBnClickedCheckAutorefresh();
	afx_msg void OnBnClickedOk();
	afx_msg void OnNMDblclkListRoom(NMHDR *pNMHDR, LRESULT *pResult);

	void OnBnRoomListEntry(int nRow);

	// Auto refresh timer proc
	static void CALLBACK AutoGetRoomInfoList(HWND hWnd, UINT nMsg, UINT_PTR nIDEvent, DWORD dwTime);

private:
    void PullRoomList();
    void ParseRoomList(std::string json);
    void RefreshRoomList(std::vector<RoomPtr> roomList);

private:
	HICON m_hIcon;

	CZegoRoomDlg* m_pRoomDlg;

	CString m_strEdUserId;
	CString m_strEdUserName;
	CComboBox m_cbQuality;
	CGridListCtrlEx m_lsRooms;
	BOOL m_bCKAutoRefresh;
	CEdit m_edNewRoomTitle;

	CZegoUserConfig m_userConfig;
	std::vector<RoomPtr> m_roomList;
};
