package es.bq.mybooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
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

	public static final String PREF_ORDEN_FECHA = "PREF_ORDEN_FECHA";
	public static final String PREF_ORDEN_NOMBRE = "PREF_ORDEN_NOMBRE";

	public static String downloadPath;
	DropboxAPI<AndroidAuthSession> mApi;

	private boolean mLoggedIn = false;

	public static boolean updateListOrder = false;

	// Android widgets
	private Button mSubmit;
	private LinearLayout mDisplay;
	private Button getEbooks;

	private ListView listView;

	private final String MAIN_DIR = "/";

	public static boolean downloaded = false;

	SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		clearKeys();

		// We create a new AuthSession so that we can use the Dropbox API.
		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);

		setContentView(R.layout.activity_main);

		mSubmit = (Button) findViewById(R.id.auth_button);

		mSubmit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// This logs you out if you're logged in, or vice versa
				if (mLoggedIn) {
					logOut();
				} else {
					// Start the remote authentication
						mApi.getSession().startOAuth2Authentication(
								MainActivity.this);
				}
			}
		});

		mDisplay = (LinearLayout) findViewById(R.id.logged_in_display);

		//view to show the list of ebooks
		listView = (ListView) findViewById(R.id.list_view);
		listView.setLongClickable(true);
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			//En las especificaciones del caso se pedía descargar la portada en doble click.
			// Yo me he decantado por hacerlo de una forma más acorde con el uso de las interfaces Android, 
			// utilizando los controles para click largo. En cualquier caso, para hacerlo mediante doble click, 
			// habría que establecer un listener específico, que por falta de tiempo no he podido desarrollar
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				String str = listView.getItemAtPosition(position).toString();
				String path = null;
				for (Entry entry : DownloadEbookList.ebooks) {
					//Obtenemos el path al ebook seleccionado
					if (entry.fileName().equals(str)) {
						path = entry.path;
						break;
					}
				}

				if (path != null) {
					//lanzamos en una tarea asíncrona la descarga del ebook para obtener su portada
					DownloadEbook ebook = new DownloadEbook(MainActivity.this,
							mApi, path, listView);
					ebook.execute();
					return true;
				} else {
					return false;
				}

			}

		});

		getEbooks = (Button) findViewById(R.id.ebooks_button);

		//Listener del boton de obtención que ebooks que lanza la descarga de los contenidos de Dropbox de forma asíncrona
		getEbooks.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				DownloadEbookList download = new DownloadEbookList(
						MainActivity.this, mApi, MAIN_DIR, listView);
				download.execute();
			}
		});

		// Display the proper UI state if logged in or not
		setLoggedIn(mApi.getSession().isLinked());

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Creación del menu desplegable con las opciones definidas en el menu.xml
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
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
				
			} catch (IllegalStateException e) {
				showToast("Couldn't authenticate with Dropbox:"
						+ e.getLocalizedMessage());
				Log.i(TAG, "Error authenticating", e);
			}
		}

	}

	/**
	 * Función para actualizar el orden de los elementos de la lista de acuerdo con las preferencias de usuario 
	 */
	private void updateOrder() {
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean ordenFecha = prefs.getBoolean(PREF_ORDEN_FECHA, false);
		boolean ordenNombre = prefs.getBoolean(PREF_ORDEN_NOMBRE, false);

		listView = (ListView) findViewById(R.id.list_view);
		listView.setBackgroundColor(Color.BLACK);
		ArrayList<String> lista = new ArrayList<String>();

		Map<String, String> libros = new HashMap<String, String>();
		//Utilizamos listas, maps y arrayAdapters para notificar los cambios de orden a la listView
		for (Entry entry : DownloadEbookList.ebooks) {
			if (ordenNombre) {
				lista.add(entry.fileName());
			} else if (ordenFecha) {
				libros.put(entry.modified, entry.fileName());

				lista.add(entry.modified);
			}

		}
		if (ordenFecha) {
			ArrayList<String> listaFecha = new ArrayList<String>();
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
					context, android.R.layout.simple_list_item_1, listaFecha);
			listView.setAdapter(arrayAdapter);
			Collections.sort(lista);
			for (String date : lista) {
				listaFecha.add(libros.get(date));
				arrayAdapter.notifyDataSetChanged();
			}

		} else if (ordenNombre) {
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
					context, android.R.layout.simple_list_item_1, lista);
			listView.setAdapter(arrayAdapter);
			Collections.sort(lista);
			arrayAdapter.notifyDataSetChanged();
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//Al seleccionar una opción del menu, guardamos las nuevas preferencias y llamamos al metodo que actualiza el orden
		//No sería necesario hacerlo a través de las preferencias de usuario, 
		//se podría pasar directamente por parámetro el orden seleccionado ahorrando así tiempo y ganando en eficiencia.
		//No se implementa por falta de tiempo.
		Editor editor = prefs.edit();
		switch (item.getItemId()) {
		case R.id.item_order_name:
			editor.putBoolean(PREF_ORDEN_FECHA, false);
			editor.putBoolean(PREF_ORDEN_NOMBRE, true);
			break;
		case R.id.item_order_date:
			editor.putBoolean(PREF_ORDEN_FECHA, true);
			editor.putBoolean(PREF_ORDEN_NOMBRE, false);
			break;
		}
		editor.commit();
		updateOrder();
		return true;

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
