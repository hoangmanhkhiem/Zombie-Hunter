
package com.secrethq.utils;

import java.lang.ref.WeakReference;
import java.io.File;
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import org.cocos2dx.lib.Cocos2dxActivity;

import com.google.android.gms.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.plus.Plus;
import com.secrethq.ads.PTAdAdMobBridge;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Activity;
import android.content.*;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.app.UiModeManager;
import android.content.res.Configuration;


public class PTServicesBridge
	implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
	private static PTServicesBridge sInstance;
	private static final String TAG = "PTServicesBridge";

	private static native String getLeaderboardId();
	private static native void warningMessageClicked(boolean accepted);
	
	private static Cocos2dxActivity activity;
	private static WeakReference<Cocos2dxActivity> s_activity;

	private static GoogleApiClient mGoogleApiClient;

	private static String urlString;
	private static int scoreValue;

    public static final int RC_SIGN_IN = 9001;	
	private static final int REQUEST_LEADERBOARD = 5000;
	
	public static PTServicesBridge instance() {
		if (sInstance == null)
			sInstance = new PTServicesBridge();
		return sInstance;
	}

	public static void initBridge(Cocos2dxActivity activity, String appId){
		Log.v(TAG, "PTServicesBridge  -- INIT");

		PTServicesBridge.s_activity = new WeakReference<Cocos2dxActivity>(activity);
		PTServicesBridge.activity = activity;

		if(appId == null || appId.length() == 0 || appId.matches("[0-9]+") == false){
			return;
		}
		
		// Create a GoogleApiClient instance
		PTServicesBridge.mGoogleApiClient = new GoogleApiClient.Builder(PTServicesBridge.activity)
        		.addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
        		.addApi(Games.API).addScope(Games.SCOPE_GAMES)
        		.addConnectionCallbacks(instance())
				.addOnConnectionFailedListener(instance())
				.build();
	}

	
     public static void openShareWidget( String message ){
            Log.v(TAG, "PTServicesBridge  -- openShareWidget with text:" + message);
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
            PTServicesBridge.activity.startActivity(Intent.createChooser(sharingIntent, "Share" ));
	}

	public static int availableProcessors() {
		int processorsNum = Runtime.getRuntime().availableProcessors();
		Log.d(TAG, "availableProcessors: " + processorsNum);
		return processorsNum;
	}
	
	public static int getCoresNumber() {
		
		class CpuFilter implements FileFilter {

	        @Override
	        public boolean accept(File pathname) {
	            //Check if filename is "cpu", followed by a single digit number
	            if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
	                return true;
	            }

	            return false;
	        }      
	    }
		
		try {
	        //Get directory containing CPU info
	        File dir = new File("/sys/devices/system/cpu/");

	        //Filter to only list the devices we care about
	        File[] files = dir.listFiles(new CpuFilter());
	        Log.d(TAG, "CPU Count: "+files.length);

	        //Return the number of cores (virtual CPU devices)
	        return files.length;

	    } catch(Exception e) {
	        //Print exception
	        Log.d(TAG, "CPU Count: Failed.");
	        e.printStackTrace();

	        //Default to return 1 core
	        return 1;
	    }
	}
	
	public static void openUrl( String url ){
		Log.v(TAG, "PTServicesBridge  -- Open URL " + url);

		PTServicesBridge.urlString = url;

		PTServicesBridge.s_activity.get().runOnUiThread( new Runnable() {
			public void run() {
				try {
					final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(PTServicesBridge.urlString));
					PTServicesBridge.activity.startActivity(intent);
				} catch(Exception e) {
			        //Print exception
			        Log.d(TAG, "OpenURL: Failed.");
			        e.printStackTrace();
			    }
			}
		});
	}

	  
	public static void showLeaderboard( ){
		Log.v(TAG, "PTServicesBridge  -- Show Leaderboard ");

		if(PTServicesBridge.mGoogleApiClient == null || PTServicesBridge.mGoogleApiClient.isConnected() == false){
			Log.e(TAG, "Google play Servioces is not sigend");
			return;
		}
		
		PTServicesBridge.s_activity.get().runOnUiThread( new Runnable() {
			public void run() {
				String leaderboardId = PTServicesBridge.getLeaderboardId();
				if(leaderboardId == null || leaderboardId.isEmpty()){
					return;
				}
				PTServicesBridge.activity.startActivityForResult(Games.Leaderboards.getLeaderboardIntent(PTServicesBridge.mGoogleApiClient,
						leaderboardId), REQUEST_LEADERBOARD);
			}
		});
	}

	public static void showCustomFullScreenAd() {
		Log.e(TAG, "PTServicesBridge  -- showCustomFullScreenAd");
	}

	public static void loadingDidComplete() {
		Log.e(TAG, "PTServicesBridge  -- loadingDidComplete");
	}

	public static void submitScrore( int score ){
		Log.v(TAG, "PTServicesBridge  -- Submit Score " + score);

		if(PTServicesBridge.mGoogleApiClient == null || PTServicesBridge.mGoogleApiClient.isConnected() == false){
			Log.e(TAG, "Google play Servioces is not sigend");
			return;
		}

		String leaderboardId = PTServicesBridge.getLeaderboardId();
		if(leaderboardId == null || leaderboardId.isEmpty()){
			return;
		}
		PTServicesBridge.scoreValue = score;
		
		if ( PTServicesBridge.mGoogleApiClient.isConnected() ) {
			Games.Leaderboards.submitScore(PTServicesBridge.mGoogleApiClient, leaderboardId, PTServicesBridge.scoreValue);
		}
	}
	
	public static boolean isRunningOnTV(){
		UiModeManager uiModeManager = (UiModeManager)PTServicesBridge.activity.getSystemService( Context.UI_MODE_SERVICE );
		if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
			Log.d("DeviceTypeRuntimeCheck", "Running on a TV Device");
			return true;
		    
		} else {
			Log.d("DeviceTypeRuntimeCheck", "Running on a non-TV Device");
			return false;
		    
		}
	}

	public static void showFacebookPage( final String facebookURL, final String facebookID){
		Log.v(TAG, "Show facebook page for URL: " + facebookURL + " ID: " + facebookID);
		
		PTServicesBridge.s_activity.get().runOnUiThread( new Runnable() {
			public void run() {
				try {
	            	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + facebookID));
	            	PTServicesBridge.activity.startActivity(intent);
	        	} catch(Exception e) {
	        		Log.v(TAG, "Show facebook FAILED going to exception handler : " + e.getMessage());
	        		try {
	        			PTServicesBridge.activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse( facebookURL )));
					} catch (Exception e2) {
						Log.v(TAG, "Show facebook exception handle FAILED : " + e2.getMessage());
					}
	        		
		        }
			}
		});
	}

	public static void showWarningMessage(final String message){
		Log.v(TAG, "Show warning with message: " + message);
		PTServicesBridge.s_activity.get().runOnUiThread( new Runnable() {
			public void run() {
				AlertDialog.Builder dlgAlert  = new AlertDialog.Builder( PTServicesBridge.activity );

				dlgAlert.setMessage(message);
				dlgAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			            PTServicesBridge.warningMessageClicked( false );
			          }
			      });
				dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			        	PTServicesBridge.warningMessageClicked( true ); 
			          }
			      });
				dlgAlert.setCancelable(true);
				dlgAlert.create().show();
			}
		});
		
	}
	
	public static void loginGameServices( ){
		Log.v(TAG, "PTServicesBridge  -- Login Game Services ");
		
		if(PTServicesBridge.mGoogleApiClient != null){
			PTServicesBridge.mGoogleApiClient.connect();
		}
	}
	

	public static boolean isGameServiceAvialable( ){
		Log.v(TAG, "PTServicesBridge  -- Is Game Service Avialable ");

		return (PTServicesBridge.mGoogleApiClient != null && PTServicesBridge.mGoogleApiClient.isConnected());
	}

	@Override
	public void onConnected(Bundle arg0) {
		Log.v(TAG, "PTServicesBridge  -- API Client Connected bundle:" + arg0);
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Log.v(TAG, "PTServicesBridge  -- API Client Connection Suspended ");
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.v(TAG, "PTServicesBridge  -- API Client Connection FAILED:" + connectionResult);
		
		if(connectionResult.hasResolution()){
	  		try {
	  			connectionResult.startResolutionForResult(activity, RC_SIGN_IN);
	  		} catch (SendIntentException e) {
	  			mGoogleApiClient.connect();
	  		}
		}
	}

	public void  onActivityResult(int requestCode, int responseCode, Intent intent){
		if(requestCode == RC_SIGN_IN && responseCode == -1){
			mGoogleApiClient.connect();
		}
	}

	public static String sha1( byte[] data, int length) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(data, 0, length);
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
	}

		
    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }


}
