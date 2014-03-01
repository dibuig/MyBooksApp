package es.bq.mybooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

public class MainActivity extends Activity {

	private static final String TAG = "MyBooksApp";

	final static private String APP_KEY = "7ypz7b6r0flze8l";
	final static private String APP_SECRET = "rh7ot40pmzq42dl";

	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	DropboxAPI<AndroidAuthSession> mApi;

	private boolean mLoggedIn = false;

	// private Button loginButton;

	private static final boolean USE_OAUTH1 = false;

	EditText user;
	EditText pass;
	TextView username;
	TextView password;

	// Android widgets
	private Button mSubmit;
	private LinearLayout mDisplay;
	private Button mRoulette;

	private ListView listView;

	private final String MAIN_DIR = "/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		clearKeys();

		// We create a new AuthSession so that we can use the Dropbox API.
		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);

		// Basic Android widgets
		setContentView(R.layout.activity_main);

		mSubmit = (Button) findViewById(R.id.auth_button);

		mSubmit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// This logs you out if you're logged in, or vice versa
				if (mLoggedIn) {
					logOut();
				} else {
					// Start the remote authentication
					if (USE_OAUTH1) {
						mApi.getSession()
						.startAuthentication(MainActivity.this);
					} else {
						mApi.getSession().startOAuth2Authentication(
								MainActivity.this);
					}
				}
			}
		});

		mDisplay = (LinearLayout) findViewById(R.id.logged_in_display);

		// This is where a photo is displayed
		listView = (ListView) findViewById(R.id.list_view);

		// This is the button to take a photo
		mRoulette = (Button) findViewById(R.id.roulette_button);

		mRoulette.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				DownloadEbookList download = new DownloadEbookList(
						MainActivity.this, mApi, MAIN_DIR, listView);
				download.execute();
			}
		});

		// Display the proper UI state if logged in or not
		setLoggedIn(mApi.getSession().isLinked());

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// outState.putString("mCameraFileName", mCameraFileName);
		super.onSaveInstanceState(outState);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add("Settings");
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		AndroidAuthSession session = mApi.getSession();

		// The next part must be inserted in the onResume() method of the
		// activity from which session.startAuthentication() was called, so
		// that Dropbox authentication completes properly.
		if (session.authenticationSuccessful()) {
			try {
				// Mandatory call to complete the auth
				session.finishAuthentication();

				// Store it locally in our app for later use
				storeAuth(session);
				setLoggedIn(true);
				updateOrder();
			} catch (IllegalStateException e) {
				showToast("Couldn't authenticate with Dropbox:"
						+ e.getLocalizedMessage());
				Log.i(TAG, "Error authenticating", e);
			}
		}
	}

	private void updateOrder() {
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean ordenFecha = prefs.getBoolean(
				PreferencesActivity.PREF_ORDEN_FECHA, false);
		boolean ordenNombre = prefs.getBoolean(
				PreferencesActivity.PREF_ORDEN_NOMBRE, false);

		listView = (ListView) findViewById(R.id.list_view);
		listView.setBackgroundColor(Color.BLACK);
		if (ordenNombre) {
			ArrayList<String> lista = new ArrayList<String>();
			ArrayList<String> listaNombres = new ArrayList<String>();
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
					context, android.R.layout.simple_list_item_1, listaNombres);

			for (Entry entry : DownloadEbookList.thumbs) {
				lista.add(entry.fileName());
			}
			Collections.sort(lista);
			listView.setAdapter(arrayAdapter);
			for (String nombre : lista) {
				listaNombres.add(nombre);
				arrayAdapter.notifyDataSetChanged();
			}
		
		} else if (ordenFecha) {
			ArrayList<String> lista = new ArrayList<String>();
			ArrayList<String> listaFecha = new ArrayList<String>();

			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context,
					android.R.layout.simple_list_item_1, listaFecha);
//			SimpleDateFormat dateFormat = new SimpleDateFormat("ddd, dd mmm yyy hh:mm:ss",
//					Locale.ENGLISH);
			Map<String,String> libros = new HashMap<String,String>();
			for (Entry entry : DownloadEbookList.thumbs) {
//				Date fecha = null; //Thu, 27 Feb 2014 22:13:03 +0000
				libros.put(entry.modified, entry.fileName());
//				fecha = dateFormat.parse(entry.modified);
				lista.add(entry.modified);
			}
			
			Collections.sort(lista);
			listView.setAdapter(arrayAdapter);
			for (String date : lista) {
				listaFecha.add(libros.get(date));
				arrayAdapter.notifyDataSetChanged();
			}

		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent = new Intent(this, PreferencesActivity.class);
		startActivity(intent);

		return super.onOptionsItemSelected(item);
	}

	private void logOut() {
		// Remove credentials from the session
		mApi.getSession().unlink();

		// Clear our stored keys
		clearKeys();
		// Change UI state to display logged out version
		setLoggedIn(false);
	}

	/**
	 * Convenience function to change UI state based on being logged in
	 */
	private void setLoggedIn(boolean loggedIn) {
		mLoggedIn = loggedIn;
		if (loggedIn) {
			mSubmit.setText("Unlink from Dropbox");
			mDisplay.setVisibility(View.VISIBLE);
		} else {
			mSubmit.setText("Link with Dropbox");
			mDisplay.setVisibility(View.GONE);
		}
	}

	private void showToast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a
	 * local store, rather than storing user name & password, and
	 * re-authenticating each time (which is not to be done, ever).
	 */
	private void loadAuth(AndroidAuthSession session) {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key == null || secret == null || key.length() == 0
				|| secret.length() == 0)
			return;

		if (key.equals("oauth2:")) {
			// If the key is set to "oauth2:", then we can assume the token is
			// for OAuth 2.
			session.setOAuth2AccessToken(secret);
		} else {
			// Still support using old OAuth 1 tokens.
			session.setAccessTokenPair(new AccessTokenPair(key, secret));
		}
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a
	 * local store, rather than storing user name & password, and
	 * re-authenticating each time (which is not to be done, ever).
	 */
	private void storeAuth(AndroidAuthSession session) {
		// Store the OAuth 2 access token, if there is one.
		String oauth2AccessToken = session.getOAuth2AccessToken();
		if (oauth2AccessToken != null) {
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME,
					0);
			Editor edit = prefs.edit();
			edit.putString(ACCESS_KEY_NAME, "oauth2:");
			edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
			edit.commit();
			return;
		}
		// Store the OAuth 1 access token, if there is one. This is only
		// necessary if
		// you're still using OAuth 1.
		AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
		if (oauth1AccessToken != null) {
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME,
					0);
			Editor edit = prefs.edit();
			edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
			edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
			edit.commit();
			return;
		}
	}

	private void clearKeys() {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}

	private AndroidAuthSession buildSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

		AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
		loadAuth(session);
		return session;
	}

}
