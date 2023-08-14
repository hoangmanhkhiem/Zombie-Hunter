package com.secrethq.ads;

import java.lang.ref.WeakReference;

import org.cocos2dx.lib.Cocos2dxActivity;

import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.*;

public class PTAdAdMobBridge {
	private static final String TAG = "PTAdAdMobBridge";
	private static Cocos2dxActivity activity;
	private static WeakReference<Cocos2dxActivity> s_activity;
	private static AdView adView;
	private static InterstitialAd interstitial;
	private static LinearLayout layout;
	private static boolean isScheduledForShow;
	private static native String bannerId();
	private static native String interstitialId();
	private static native void interstitialDidFail();
    private static native void bannerDidFail();
	
	public static void initBridge(Cocos2dxActivity activity){
		Log.v(TAG, "PTAdAdMobBridge  -- INIT");
		
		isScheduledForShow = false;
		
		PTAdAdMobBridge.s_activity = new WeakReference<Cocos2dxActivity>(activity);	
		PTAdAdMobBridge.activity = activity;
	

    	PTAdAdMobBridge.initBanner();
    	PTAdAdMobBridge.initInterstitial();
    	
	}

	public static void initBanner(){
		Log.v(TAG, "PTAdAdMobBridge  -- initBanner");
		PTAdAdMobBridge.s_activity.get().runOnUiThread( new Runnable() {
            public void run() {

            	if(PTAdAdMobBridge.adView != null){
            		return;
            	}
            	
        		FrameLayout frameLayout = (FrameLayout)PTAdAdMobBridge.activity.findViewById(android.R.id.content);
        		RelativeLayout layout = new RelativeLayout( PTAdAdMobBridge.activity );
        		frameLayout.addView( layout );
        		
        		RelativeLayout.LayoutParams adViewParams = new RelativeLayout.LayoutParams(
        				AdView.LayoutParams.WRAP_CONTENT,
        				AdView.LayoutParams.WRAP_CONTENT);
        		adViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        		adViewParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        		PTAdAdMobBridge.adView = new AdView( PTAdAdMobBridge.activity );
        		PTAdAdMobBridge.adView.setAdSize(AdSize.SMART_BANNER);
        		PTAdAdMobBridge.adView.setAdUnitId( PTAdAdMobBridge.bannerId() );
        		
        		layout.addView(PTAdAdMobBridge.adView, adViewParams);
        		PTAdAdMobBridge.adView.setVisibility( View.INVISIBLE );

        		AdRequest adRequest = new AdRequest.Builder().build();
        		PTAdAdMobBridge.adView.loadAd( adRequest );	
        		
            }
        });
	
	}
	
	public static boolean isBannerVisible(){
		if(PTAdAdMobBridge.adView == null){
			return false;
		}
		else{
			if(PTAdAdMobBridge.adView.getVisibility() == View.VISIBLE){
				return true;
			}
			else{
				return false;
			}
		}
	}
	
	public static void initInterstitial(){
		Log.v(TAG, "PTAdAdMobBridge  -- initInterstitial");
		PTAdAdMobBridge.s_activity.get().runOnUiThread( new Runnable() {
            public void run() {

            	if(PTAdAdMobBridge.interstitial != null){
            		return;
            	}
            	
				AdRequest adRequest = new AdRequest.Builder().build();
				
				PTAdAdMobBridge.interstitial = new InterstitialAd( PTAdAdMobBridge.activity );
				PTAdAdMobBridge.interstitial.setAdUnitId( PTAdAdMobBridge.interstitialId() );
				PTAdAdMobBridge.interstitial.setAdListener(new AdListener() {
		            @Override
		            public void onAdLoaded() {
		            	if(PTAdAdMobBridge.isScheduledForShow){
		            		PTAdAdMobBridge.showFullScreen();
		            	}
		            }
		
		            @Override
		            public void onAdClosed() {
					    AdRequest adRequest = new AdRequest.Builder().build();
					    PTAdAdMobBridge.interstitial.loadAd(adRequest);
		            }
		            
		            @Override
		            public void onAdFailedToLoad(int errorCode) {
		            	PTAdAdMobBridge.interstitialDidFail();
		            }
		        });
		
				PTAdAdMobBridge.interstitial.loadAd(adRequest);
        		
            }
        });
	}
	
	
	
	public static void showFullScreen(){
		Log.v(TAG, "showFullScreen");
		
		if(PTAdAdMobBridge.interstitial != null){
			PTAdAdMobBridge.s_activity.get().runOnUiThread( new Runnable() {
				public void run() {
					if(PTAdAdMobBridge.interstitial.isLoaded()){
						PTAdAdMobBridge.interstitial.show();
						PTAdAdMobBridge.isScheduledForShow = false;
					}
					else{
						PTAdAdMobBridge.isScheduledForShow = true;
					}
				}
			});
 		}
	}

	

	public static void showBannerAd(){
		Log.v(TAG, "showBannerAd");
		
		if(PTAdAdMobBridge.adView != null){
			PTAdAdMobBridge.s_activity.get().runOnUiThread( new Runnable() {
				public void run() {
					
					AdRequest adRequest = new AdRequest.Builder().build();
					PTAdAdMobBridge.adView.loadAd(adRequest);
					PTAdAdMobBridge.adView.setAdListener(new AdListener() {		            
			            @Override
			            public void onAdFailedToLoad(int errorCode) {
			            	PTAdAdMobBridge.bannerDidFail();
			            }
			        });
					PTAdAdMobBridge.adView.setVisibility( View.VISIBLE );
				}
			});			
		}

		
		
	}

	public static void hideBannerAd(){
		Log.v(TAG, "hideBannerAd");
		 if(PTAdAdMobBridge.adView != null){
		 		PTAdAdMobBridge.s_activity.get().runOnUiThread( new Runnable() {
		 			public void run() {
		 				PTAdAdMobBridge.adView.setVisibility( View.INVISIBLE );
		 			}
		 		});
		 }
	}

}
