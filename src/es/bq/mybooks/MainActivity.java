package es.bq.mybooks;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

public class MainActivity extends Activity {

	final static private String APP_KEY = "CHANGE_ME";
    final static private String APP_SECRET = "CHANGE_ME_SECRET";
    
    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    
    DropboxAPI<AndroidAuthSession> mApi;

    private boolean mLoggedIn;

    private Button loginButton;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        
//      loadAuth(session);
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0){
        	return;
        }

        if (key.equals("oauth2:")) {
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            session.setOAuth2AccessToken(secret);
        } else {
            // Still support using old OAuth 1 tokens.
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }


        mApi = new DropboxAPI<AndroidAuthSession>(session);

        // Basic Android widgets
        
		setContentView(R.layout.activity_main);
		
		//Boton
		 loginButton = (Button)findViewById(R.id.button1);

	        loginButton.setOnClickListener(new OnClickListener() {
	            public void onClick(View v) {
	                // This logs you out if you're logged in, or vice versa
	                if (mLoggedIn) {
	                    logOut();
	                } else {
	                    // Start the remote authentication
//	                    if (USE_OAUTH1) {
//	                        mApi.getSession().startAuthentication(DBRoulette.this);
//	                    } else {
	                        mApi.getSession().startOAuth2Authentication(MainActivity.this);
//	                    }
	                }
	            }
	        });
	}
	
    private void logOut() {
        // Remove credentials from the session
        mApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
//        setLoggedIn(false);
    }

    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }
    
//    /**
//     * Convenience function to change UI state based on being logged in
//     */
//    private void setLoggedIn(boolean loggedIn) {
//    	mLoggedIn = loggedIn;
//    	if (loggedIn) {
//    		mSubmit.setText("Unlink from Dropbox");
//            mDisplay.setVisibility(View.VISIBLE);
//    	} else {
//    		mSubmit.setText("Link with Dropbox");
//            mDisplay.setVisibility(View.GONE);
//            mImage.setImageDrawable(null);
//    	}
//    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
