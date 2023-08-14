package com.secrethq.store;

import java.lang.ref.WeakReference;
import java.util.List;

import org.cocos2dx.lib.Cocos2dxActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;

import com.secrethq.store.util.*;
import com.secrethq.utils.PTServicesBridge;



public class PTStoreBridge {
	private static boolean readyToPurchase = false;
	
	private static IabHelper mHelper;
	private static Cocos2dxActivity activity;
	private static WeakReference<Cocos2dxActivity> s_activity;
	private static Inventory inventory;
	
	private static final String TAG = "PTStoreBridge";
	
    private static native String licenseKey();
    public static native void purchaseDidComplete( String productId );
    public static native void purchaseDidCompleteRestoring(String productId);
    public static native boolean isProductConsumible( String productId );
   
	static public void initBridge(Cocos2dxActivity _activity){
		activity = _activity;
		s_activity = new WeakReference<Cocos2dxActivity>(activity);	
		mHelper = new IabHelper(activity, licenseKey());
		
		try {
			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				   public void onIabSetupFinished(IabResult result) {
				      if (!result.isSuccess()) {
				         Log.d(TAG, "Problem setting up In-app Billing: " + result);
				         readyToPurchase = false;
				      } else {
				    	  readyToPurchase = true;
				      }
				   }
				});
		} catch (Exception e) {
			Log.v(TAG, "IabHelper.startSetup : FAILED : " + e.getMessage());
		}
	
	}
	
	
	
	
    public static IabHelper iabHelper() {
		return mHelper;
	}
    
    public static void setInventory( Inventory _inventory){
    	inventory = _inventory;
    }
    
    

	static public void purchase( final String storeId ){
		if ( !readyToPurchase ) {
			Log.e(TAG, "In-app Billing not Ready");
			return;
		}
	
		IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener 
		   = new IabHelper.OnIabPurchaseFinishedListener() {
		   public void onIabPurchaseFinished(IabResult result, Purchase purchase) 
		   {
		      if (result.isFailure()) {
		         Log.d(TAG, "Error purchasing: " + result);
		         if(result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED){
		        	 if(!isProductConsumible( storeId )){
		        		 Log.v(TAG, "Product alreasdy ownded - restored:" + storeId);
		        		 purchaseDidComplete( storeId );
		        	 }
		         }
		         return;
		      }     
		      if( isProductConsumible( purchase.getSku() ) ){
		    	  Log.v(TAG, "Consuming product: " + purchase.getSku());
		    	  consumePurchase( purchase );
		      }
		      else{
		    	  Log.v(TAG, "Purchase did completed:" + purchase.getSku());
		    	  purchaseDidComplete( purchase.getSku() );
		      }
		      
		   }
		};
		
		try {
			mHelper.launchPurchaseFlow(activity, storeId, 10001, mPurchaseFinishedListener, null);
		} catch (Exception e) {
			Log.v(TAG, "launchPurchaseFlow : FAILED : " + e.getMessage());
		}
	}
	
	static public void restorePurchases(){
		if ( !readyToPurchase ) {
			Log.e(TAG, "In-app Billing not Ready");
			return;
		}
		
		s_activity.get().runOnUiThread( new Runnable() {
            public void run() {
        		final ProgressDialog progress;
        		progress = ProgressDialog.show(activity, null,
        			    "Restoring purchases...", true);
        		
	        	IabHelper.QueryInventoryFinishedListener mGotInventoryListener 
	     		   = new IabHelper.QueryInventoryFinishedListener() {
	     		   public void onQueryInventoryFinished(IabResult result,
	     		      Inventory inventory) {
	     		      if (result.isFailure()) {
	     		    	 Log.v(TAG, "INVENTORY FAILURE: " + result);
	     		      }
	     		      else {
	     		    	  setInventory( inventory );
	     		    	  
	     		    	 List<Purchase> purchases = inventory.getAllPurchases();
	     		    	 for (Purchase purchase : purchases){
	     		    		 Log.v(TAG, "Inventory: " + purchase.getSku());
							if( ! isProductConsumible( purchase.getSku() )){
								purchaseDidCompleteRestoring( purchase.getSku() );
							}
	     		    	 }
	     		      }
	     		     progress.dismiss();
	     			
	     		     AlertDialog.Builder dlgAlert  = new AlertDialog.Builder( activity );
	    			dlgAlert.setMessage("Restore purchases complete");
	    			dlgAlert.setPositiveButton("OK", null);
	    			dlgAlert.create().show();	     		     
	     		     
	     		   }
	     		};
	     		
	    		try {
	    			mHelper.queryInventoryAsync(mGotInventoryListener);
	    		} catch (Exception e) {
	    			Log.v(TAG, "queryInventoryAsync : FAILED : " + e.getMessage());
	    		}
            }
		});
	}
	
	
	static public void consumePurchase( final Purchase purchase ){		
		if ( !readyToPurchase ) {
			Log.e(TAG, "In-app Billing not Ready");
			return;
		}
		
		final ProgressDialog progress;
		progress = ProgressDialog.show(activity, null,
			    "Finalizing Purchase ...", true);
		
		s_activity.get().runOnUiThread( new Runnable() {
            public void run() {
		
				IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
						   new IabHelper.OnConsumeFinishedListener() {
						   public void onConsumeFinished(Purchase purchase, IabResult result) {
						      if (result.isSuccess()) {
						    	  purchaseDidComplete( purchase.getSku() );
						      }
						      else {
						    	  Log.v(TAG, "Error consume: " + result);
						      }
						      	progress.dismiss();
						   }
						};
						
				if(purchase !=  null){
					try {
		    			mHelper.consumeAsync(purchase, mConsumeFinishedListener);
		    		} catch (Exception e) {
		    			Log.v(TAG, "consumeAsync : FAILED : " + e.getMessage());
		    		}
				}
            }
		});
	}
}

