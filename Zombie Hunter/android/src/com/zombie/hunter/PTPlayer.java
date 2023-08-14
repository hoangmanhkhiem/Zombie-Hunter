package com.zombie.hunter;

import org.cocos2dx.lib.Cocos2dxActivity;
import org.cocos2dx.lib.Cocos2dxGLSurfaceView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import com.secrethq.store.PTStoreBridge;
import com.google.android.gms.games.GamesActivityResultCodes;

import com.secrethq.ads.*;
import com.secrethq.utils.*;

public class PTPlayer extends Cocos2dxActivity {

	private static native void loadModelController();

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    try {
		    Log.v("----------","onActivityResult: request: " + requestCode + " result: "+ resultCode);
		    if(PTStoreBridge.iabHelper().handleActivityResult(requestCode, resultCode, data)){
		    	Log.v("-----------", "handled by IABHelper");
		    }
		    else if(requestCode == PTServicesBridge.RC_SIGN_IN){
		    	if(resultCode == RESULT_OK){
		    		PTServicesBridge.instance().onActivityResult(requestCode, resultCode, data);
		    	}
		    	else if(resultCode == GamesActivityResultCodes.RESULT_SIGN_IN_FAILED){
		    		int duration = Toast.LENGTH_SHORT;
		    		Toast toast = Toast.makeText(this, "Google Play Services: Sign in error", duration);
		    		toast.show();
		    	}
		    	else if(resultCode == GamesActivityResultCodes.RESULT_APP_MISCONFIGURED){
		    		int duration = Toast.LENGTH_SHORT;
		    		Toast toast = Toast.makeText(this, "Google Play Services: App misconfigured", duration);
		    		toast.show();	    		
		    	}
		    }
	    } catch (Exception e) {
		    	Log.v("-----------", "onActivityResult FAIL on iabHelper : " + e.toString());
		}
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PTServicesBridge.initBridge(this, getString( R.string.app_id ));
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public void onNativeInit(){
			initBridges();				
	}

	private void initBridges(){
		PTStoreBridge.initBridge( this );


		if (PTJniHelper.isAdNetworkActive("kChartboost")) {
			PTAdChartboostBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kRevMob")) {
			PTAdRevMobBridge.initBridge(this);
		}
		
		if (PTJniHelper.isAdNetworkActive("kAdMob") || PTJniHelper.isAdNetworkActive("kFacebook")) {
			PTAdAdMobBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kAppLovin")) {
			PTAdAppLovinBridge.initBridge(this);
		}

		if (PTJniHelper.isAdNetworkActive("kLeadBolt")) {
			PTAdLeadBoltBridge.initBridge(this);
		}
		
		if (PTJniHelper.isAdNetworkActive("kFacebook")) {
			PTAdFacebookBridge.initBridge(this);
		}
		
		if (PTJniHelper.isAdNetworkActive("kHeyzap")) {
			PTAdHeyzapBridge.initBridge(this);
		}
	}

	@Override
	public Cocos2dxGLSurfaceView onCreateView() {
		Cocos2dxGLSurfaceView glSurfaceView = new Cocos2dxGLSurfaceView(this);
		glSurfaceView.setEGLConfigChooser(8, 8, 8, 0, 0, 0);

		return glSurfaceView;
	}

	static {
		System.loadLibrary("player");
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (PTJniHelper.isAdNetworkActive("kChartboost")) {
			PTAdChartboostBridge.onResume( this );
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (PTJniHelper.isAdNetworkActive("kChartboost")) {
			PTAdChartboostBridge.onStart( this );
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (PTJniHelper.isAdNetworkActive("kChartboost")) {
			PTAdChartboostBridge.onStop( this );
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
