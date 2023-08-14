#include "PTPAppDelegate.h"
#include "cocos2d.h"
#include "PTPConfig.h"
#include "models/PTModelController.h"
#include "PTPSettingsController.h"
#include "platform/android/jni/JniHelper.h"
#include <jni.h>
#include <android/log.h>
#include "models/PTModelScreen.h"
#include "models/PTModelAtlas.h"
#include "models/PTModelGeneralSettings.h"
#include "models/objects/PTModelAssetCharacter.h"
#include "models/objects/PTModelObjectAssetTrail.h"
#include "models/objects/PTModelObjectAssetFlag.h"
#include "models/objects/PTModelObjectButtonPurchase.h"

#define  LOG_TAG    "main"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

using namespace cocos2d;

extern "C"
{

jint JNI_OnLoad(JavaVM *vm, void *reserved){
    JniHelper::setJavaVM(vm);
    return JNI_VERSION_1_4;
}

void Java_com_secrethq_utils_PTJniHelper_loadModelController(JNIEnv*  env, jobject thiz){
        PTLog("start laoding XML");

        PTModelController *mc = PTModelController::shared();
        mc->loadFromFile( CCString("data/main.xml") );

        PTLog("end loading XML");
}

jboolean Java_com_secrethq_utils_PTJniHelper_isAdNetworkActive(JNIEnv*  env, jobject thiz, jstring adNetworkNameString ){
    const char* adNetworkName = env->GetStringUTFChars(adNetworkNameString, 0);
    std::string platformName = "Google Play Store";

    PTModelController *mc = PTModelController::shared();
    PTPSettingsController* sc = PTPSettingsController::shared();

    CCArray *screensArray  = mc->getModelArray("PTModelScreen");
    if(screensArray == NULL || sc->removeAds()){
        return false;
    }
    for(int i=0; i < screensArray->count(); i++){
        PTModelScreen* model = (PTModelScreen*)screensArray->objectAtIndex( i );
        if(model->adNetworkFullscreen().compare( adNetworkName ) == 0){
            return true;
        }
        if(model->adNetworkBanner().compare( adNetworkName ) == 0){
            return true;    
        }
    }

    // check if this network in backup list
    // banner
    std::list<std::string> bannerList = PTModelGeneralSettings::shared()->adBannersList( platformName );
    std::list<std::string>::const_iterator bannerIterator;
    for (bannerIterator = bannerList.begin(); bannerIterator != bannerList.end(); ++bannerIterator) {
        if(strcmp((*bannerIterator).c_str(), adNetworkName) == 0){
            PTLog("ad Network (%s) in backup list (banner)", adNetworkName);
            return true;
        }
    }

    // interstitials
    std::list<std::string> interstitialList = PTModelGeneralSettings::shared()->adInterstitialsList( platformName );
    std::list<std::string>::const_iterator interstittialsIterator;
    for (interstittialsIterator = interstitialList.begin(); interstittialsIterator != interstitialList.end(); ++interstittialsIterator) {
        if(strcmp((*interstittialsIterator).c_str(), adNetworkName) == 0){
            PTLog("ad Network (%s) in backup list (interstitials)", adNetworkName);
            return true;
        }
    }

    // check if network is used as reward video
    CCArray *buttonsArray  = mc->getModelArray("PTModelObjectButtonPurchase");
    if ( buttonsArray ){
        for(int i=0; i < buttonsArray->count(); i++){
            PTModelObjectButtonPurchase* model = (PTModelObjectButtonPurchase*)buttonsArray->objectAtIndex( i );
            if ( model->isRewardedVideo()
                 && model->rewardedVideoAdNetwork().compare( adNetworkName ) == 0 ) {
                return true;
            }
        }
    }

    // check if network is used as reward video
    CCArray *charsArray  = mc->getModelArray("PTModelAssetCharacter");
    if ( charsArray ){
        for(int i=0; i < charsArray->count(); i++){
            PTModelAssetCharacter* model = (PTModelAssetCharacter*)charsArray->objectAtIndex( i );
            if ( model->purchaseMethod().compare("kRewardedVideos") == 0
                 && model->rewardedVideoAdNetwork().compare( adNetworkName ) == 0 ) {
                return true;
            }
        }
    }

    return false;
}

void Java_org_cocos2dx_lib_Cocos2dxRenderer_nativeInit(JNIEnv*  env, jobject thiz, jint w, jint h){
    PTModelController *mc = PTModelController::shared();
    if (!CCDirector::sharedDirector()->getOpenGLView()){
        CCEGLView *view = CCEGLView::sharedOpenGLView();
        view->setFrameSize(w, h);
        
        //loading general info 
        mc->clean();
        mc->loadDataForClass( CCString::create("data/PTModelGeneralSettings.0.attributes.xml"), PTModelControllerDataTypeAttributes );
        mc->loadDataForClass( CCString::create("data/PTModelFont.0.attributes.xml"), PTModelControllerDataTypeAttributes );     
        mc->loadDataForClass( CCString::create("data/PTModelScreen.0.attributes.xml"), PTModelControllerDataTypeAttributes );
        mc->loadDataForClass( CCString::create("data/PTModelObjectLabel.0.attributes.xml"), PTModelControllerDataTypeAttributes );
        mc->loadDataForClass( CCString::create("data/PTModelObjectLoadingBar.0.attributes.xml"), PTModelControllerDataTypeAttributes );
        mc->loadDataForClass( CCString::create("data/PTModelScreen.0.connections.xml"), PTModelControllerDataTypeConnections );

        PTPAppDelegate *pAppDelegate = new PTPAppDelegate();
        CCApplication::sharedApplication()->run();

        //clean up main model controller before starting loading Objects form XML files
        mc->clean();


    }
    else {
        PTLog("NATIVE RE INIT");
        ccGLInvalidateStateCache();        
        CCShaderCache::sharedShaderCache()->reloadDefaultShaders();
        ccDrawInit();
        CCTextureCache::reloadAllTextures();
        CCNotificationCenter::sharedNotificationCenter()->postNotification(EVNET_COME_TO_FOREGROUND, NULL);
        CCDirector::sharedDirector()->setGLDefaultValues(); 

    }
}

}
