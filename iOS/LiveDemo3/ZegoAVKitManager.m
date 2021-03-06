//
//  ZegoAVKitManager.m
//  LiveDemo
//
//  Copyright © 2015年 Zego. All rights reserved.
//

#include "ZegoAVKitManager.h"
#import "ZegoSettings.h"
#import "ZegoVideoFilterDemo.h"

static ZegoLiveRoomApi *g_ZegoApi = nil;

NSData *g_signKey = nil;
uint32_t g_appID = 0;

BOOL g_useTestEnv = NO;
BOOL g_useAlphaEnv = NO;

#if TARGET_OS_SIMULATOR
BOOL g_useHardwareEncode = NO;
BOOL g_useHardwareDecode = NO;
#else

#if defined(ZEGO_TEST_RTP3) || defined(ZEGO_TEST_RTP_INTEL)
BOOL g_useHardwareEncode = NO;
#else
BOOL g_useHardwareEncode = YES;
#endif

BOOL g_useHardwareDecode = YES;
#endif

BOOL g_enableVideoRateControl = NO;

BOOL g_useExternalCaptrue = NO;
BOOL g_useExternalRender = NO;


BOOL g_enableReverb = NO;

#ifdef ZEGO_TEST_RTP_INTEL
BOOL g_recordTime = YES;
BOOL g_useInternationDomain = YES;
BOOL g_useExternalFilter = YES;
#else
BOOL g_recordTime = NO;
BOOL g_useInternationDomain = NO;
BOOL g_useExternalFilter = NO;
#endif

BOOL g_useHeadSet = NO;

static Byte toByte(NSString* c);
static NSData* ConvertStringToSign(NSString* strSign);

static __strong id<ZegoVideoCaptureFactory> g_factory = nullptr;
static __strong id<ZegoVideoFilterFactory> g_filterFactory = nullptr;

@interface ZegoDemoHelper ()

+ (void)setupVideoCaptureDevice;
+ (NSData *)zegoAppSignFromServer;

@end


@implementation ZegoDemoHelper

+ (ZegoLiveRoomApi *)api
{
    if (g_ZegoApi == nil) {
        [ZegoLiveRoomApi setUseTestEnv:g_useTestEnv];
        [ZegoLiveRoomApi enableExternalRender:[self usingExternalRender]];
        
#ifdef DEBUG
        [ZegoLiveRoomApi setVerbose:YES];
#endif
        
        [self setupVideoCaptureDevice];
        [self setupVideoFilter];
    
        [ZegoLiveRoomApi setUserID:[ZegoSettings sharedInstance].userID userName:[ZegoSettings sharedInstance].userName];

        NSData * appSign = [self zegoAppSignFromServer];
        g_ZegoApi = [[ZegoLiveRoomApi alloc] initWithAppID:[self appID] appSignature:appSign];
        
        [ZegoLiveRoomApi requireHardwareDecoder:g_useHardwareDecode];
        [ZegoLiveRoomApi requireHardwareEncoder:g_useHardwareEncode];
    }
    
    return g_ZegoApi;
}

+ (void)checkHeadSet
{
#if TARGET_IPHONE_SIMULATOR
    g_useHeadSet = NO;
#else
    AVAudioSessionRouteDescription *route = [AVAudioSession sharedInstance].currentRoute;
    for (AVAudioSessionPortDescription *desc in route.outputs)
    {
        if ([desc.portType isEqualToString:AVAudioSessionPortHeadphones] ||
            [desc.portType isEqualToString:AVAudioSessionPortBluetoothA2DP])
        {
            g_useHeadSet = YES;
            return;
        }
    }
    
    g_useHeadSet = NO;
#endif
}

+ (void)releaseApi
{
    g_ZegoApi = nil;
}

+ (void)setCustomAppID:(uint32_t)appid sign:(NSString *)sign
{
    g_appID = appid;
    NSData *d = ConvertStringToSign(sign);
    
    if (d.length == 32 && appid != 0)
    {
        g_appID = appid;
        g_signKey = [[NSData alloc] initWithData:d];
        
        g_ZegoApi = nil;
        [[NSNotificationCenter defaultCenter] postNotificationName:@"RoomInstanceClear" object:nil userInfo:nil];
    }
}

+ (void)setUsingTestEnv:(bool)testEnv
{
    if (g_useTestEnv != testEnv)
    {
        [self releaseApi];
    }
    
    g_useTestEnv = testEnv;
    [ZegoLiveRoomApi setUseTestEnv:testEnv];
}

+ (bool)usingTestEnv
{
    return g_useTestEnv;
}

+ (bool)usingAlphaEnv
{
    return g_useAlphaEnv;
}

+ (void)setUsingExternalCapture:(bool)bUse
{
    if (g_useExternalCaptrue == bUse)
        return;
    
    [self releaseApi];
    
    g_useExternalCaptrue = bUse;
    if (bUse)
    {
#if TARGET_OS_SIMULATOR
        if (g_factory == nil)
            g_factory = [[ZegoVideoCaptureFactory alloc] init];
#else 
        if (g_factory == nil)
            g_factory = [[VideoCaptureFactoryDemo alloc] init];
#endif
        
        [ZegoLiveRoomApi setVideoCaptureFactory:g_factory];
    }
    else
    {
        [ZegoLiveRoomApi setVideoCaptureFactory:nil];
    }
}

#if TARGET_OS_SIMULATOR
+ (ZegoVideoCaptureFactory *)getVideoCaptureFactory
{
    return g_factory;
}
#else
+ (VideoCaptureFactoryDemo *)getVideoCaptureFactory
{
    return g_factory;
}
#endif

+ (bool)usingExternalCapture
{
    return g_useExternalCaptrue;
}

+ (void)setUsingExternalRender:(bool)bUse
{
    if (g_useExternalRender != bUse)
    {
        [self releaseApi];
    }
    
    g_useExternalRender = bUse;
    [ZegoLiveRoomApi enableExternalRender:bUse];
}

+ (bool)usingExternalRender
{
    return g_useExternalRender;
}

+ (void)setUsingExternalFilter:(bool)bUse
{
    if (g_useExternalFilter == bUse)
        return;
    
    [self releaseApi];
    
    g_useExternalFilter = bUse;
    if (bUse)
    {
        if (g_filterFactory == nullptr)
            g_filterFactory = [[ZegoVideoFilterFactoryDemo alloc] init];
        
        [ZegoLiveRoomApi setVideoFilterFactory:g_filterFactory];
    }
    else
    {
        [ZegoLiveRoomApi setVideoFilterFactory:nil];
    }
}

+ (bool)usingExternalFilter
{
    return g_useExternalFilter;
}

+ (void)setUsingHardwareDecode:(bool)bUse
{
    if (g_useHardwareDecode == bUse)
        return;
    
    g_useHardwareDecode = bUse;
    [ZegoLiveRoomApi requireHardwareDecoder:g_useHardwareDecode];
}

+ (bool)usingHardwareDecode
{
    return g_useHardwareDecode;
}

+ (void)setUsingHardwareEncode:(bool)bUse
{
    if (g_useHardwareEncode == bUse)
        return;
    
    if (bUse)
    {
        if (g_enableVideoRateControl)
        {
            g_enableVideoRateControl = NO;
            [g_ZegoApi enableRateControl:false];
        }
    }
    
    g_useHardwareEncode = bUse;
    [ZegoLiveRoomApi requireHardwareEncoder:g_useHardwareEncode];
}

+ (bool)usingHardwareEncode
{
    return g_useHardwareEncode;
}

+ (void)setEnableRateControl:(bool)bEnable
{
    if (g_enableVideoRateControl == bEnable)
        return;
    
    if (bEnable)
    {
        if (g_useHardwareEncode)
        {
            g_useHardwareEncode = NO;
            [ZegoLiveRoomApi requireHardwareEncoder:false];
        }
    }
    
    g_enableVideoRateControl = bEnable;
    [g_ZegoApi enableRateControl:g_enableVideoRateControl];
}

+ (bool)rateControlEnabled
{
    return g_enableVideoRateControl;
}

void prep_func(const short* inData, int inSamples, int sampleRate, short *outData)
{
    memcpy(outData, inData, inSamples * sizeof(short));
}

+ (void)setEnableReverb:(bool)bEnable
{
    if (g_enableReverb == bEnable)
        return;
    
    g_enableReverb = bEnable;
    [self releaseApi];
    
    if (bEnable)
    {
        [ZegoLiveRoomApi setAudioPrep:&prep_func];
    }
    else
    {
        [ZegoLiveRoomApi setAudioPrep:nil];
    }
}

+ (bool)reverbEnabled
{
    return g_enableReverb;
}

+ (void)setRecordTime:(bool)record
{
    if (g_recordTime == record)
        return;
    
    g_recordTime = record;
    [self setUsingExternalFilter:g_recordTime];
}

+ (bool)recordTime
{
    return g_recordTime;
}

+ (bool)useHeadSet
{
    return g_useHeadSet;
}

+ (void)setUsingInternationDomain:(bool)bUse
{
    if (g_useInternationDomain == bUse)
        return;
    
    g_useInternationDomain = bUse;
}

+ (bool)usingInternationDomain
{
    return g_useInternationDomain;
}

#pragma mark - private

+ (void)setupVideoCaptureDevice
{

#if TARGET_OS_SIMULATOR
    g_useExternalCaptrue = YES;
    
    if (g_factory == nullptr) {
        g_factory = [[ZegoVideoCaptureFactory alloc] init];
        [ZegoLiveRoomApi setVideoCaptureFactory:g_factory];
    }
#else
    
     // try VideoCaptureFactoryDemo for camera
//     static __strong id<ZegoVideoCaptureFactory> g_factory = nullptr;

    /*
    g_useExternalCaptrue = YES;
    
     if (g_factory == nullptr)
     {
         g_factory = [[VideoCaptureFactoryDemo alloc] init];
         [ZegoLiveRoomApi setVideoCaptureFactory:g_factory];
     }
     */
#endif
}

+ (void)setupVideoFilter
{
    if (!g_useExternalFilter)
        return;
    
    if (g_filterFactory == nullptr)
        g_filterFactory = [[ZegoVideoFilterFactoryDemo alloc] init];
    
    [ZegoLiveRoomApi setVideoFilterFactory:g_filterFactory];
}

+ (uint32_t)appID
{
    if (g_appID != 0)
    {
        return g_appID;
    }
    else
    {
        
#ifdef ZEGO_TEST_RTP3
#warning "ZEGO_TEST_RTP3"
        return 10;  // * rtp
#endif
        
#ifdef ZEGO_TEST_RTP_INTEL
#warning "ZEGO_TEST_INTERNATIONAL"
        return 100;
#endif
        
#warning "ZEGO_DEMO"
        return 0;           // * demo
    }
}

+ (NSData *)zegoAppSignFromServer
{
    //!! Demo 把signKey先写到代码中
    //!! 规范用法：这个signKey需要从server下发到App，避免在App中存储，防止盗用

    if ([self appID] == 0)
    {
        Byte signkey[] = {};
        return [NSData dataWithBytes:signkey length:32];
    }
    else if ([self appID] == 10)
    {
        Byte signkey[] = {};
        return [NSData dataWithBytes:signkey length:32];
    }
    else if ([self appID] == 100)
    {
        Byte signkey[] = {};
        return [NSData dataWithBytes:signkey length:32];
    }
    else
    {
        return g_signKey;
    }
}


+ (NSString *)getMyRoomID:(ZegoDemoRoomType)roomType
{
    switch (roomType) {
        case SinglePublisherRoom: // * 单主播
            return [NSString stringWithFormat:@"#d-%@", [ZegoSettings sharedInstance].userID];
        case MultiPublisherRoom: // * 连麦
            return [NSString stringWithFormat:@"#m-%@", [ZegoSettings sharedInstance].userID];
        case MixStreamRoom: // * 混流
            return [NSString stringWithFormat:@"#s-%@", [ZegoSettings sharedInstance].userID];
        case WerewolfRoom:
            return [NSString stringWithFormat:@"#w-%@", [ZegoSettings sharedInstance].userID];
        case WerewolfInTurnRoom:
        {
            return [NSString stringWithFormat:@"#i-%@", [ZegoSettings sharedInstance].userID];
        }
        default:
            return nil;
    }
}

+ (NSString *)getPublishStreamID
{
    NSString *userID = [[ZegoSettings sharedInstance] userID];
    unsigned long currentTime = (unsigned long)[[NSDate date] timeIntervalSince1970];
    return [NSString stringWithFormat:@"s-%@-%lu", userID, currentTime];
}

@end

Byte toByte(NSString* c)
{
    NSString *str = @"0123456789abcdef";
    Byte b = [str rangeOfString:c].location;
    return b;
}

NSData* ConvertStringToSign(NSString* strSign)
{
    if(strSign == nil || strSign.length == 0)
        return nil;
    strSign = [strSign lowercaseString];
    strSign = [strSign stringByReplacingOccurrencesOfString:@" " withString:@""];
    strSign = [strSign stringByReplacingOccurrencesOfString:@"0x" withString:@""];
    NSArray* szStr = [strSign componentsSeparatedByString:@","];
    int nLen = (int)[szStr count];
    Byte szSign[32];
    for(int i = 0; i < nLen; i++)
    {
        NSString *strTmp = [szStr objectAtIndex:i];
        if(strTmp.length == 1)
            szSign[i] = toByte(strTmp);
        else
        {
            szSign[i] = toByte([strTmp substringWithRange:NSMakeRange(0, 1)]) << 4 | toByte([strTmp substringWithRange:NSMakeRange(1, 1)]);
        }
        NSLog(@"%x,", szSign[i]);
    }
    
    NSData *sign = [NSData dataWithBytes:szSign length:32];
    return sign;
}


#pragma mark - alpha support

@interface NSObject()
// * suppress warning
+ (void)setUseAlphaEnv:(id)useAlphaEnv;
@end

@implementation ZegoDemoHelper (Alpha)

+ (void)setUsingAlphaEnv:(bool)alphaEnv
{
    if ([ZegoLiveRoomApi respondsToSelector:@selector(setUseAlphaEnv:)])
    {
        if (g_useAlphaEnv != alphaEnv)
        {
            [self releaseApi];
        }
        
        g_useAlphaEnv = alphaEnv;
        [ZegoLiveRoomApi performSelector:@selector(setUseAlphaEnv:) withObject:@(alphaEnv)];
    }
}

@end


