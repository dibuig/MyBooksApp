package es.bq.mybooks;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class PreferencesActivity extends Activity {

	CheckBox checkfecha;
	CheckBox checkNombre;
	
	Button botonOk;
	Button botonCancel;

	public static final String PREF_ORDEN_FECHA = "PREF_ORDEN_FECHA";
	public static final String PREF_ORDEN_NOMBRE = "PREF_ORDEN_NOMBRE";

	SharedPreferences prefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferences);
		
		checkfecha = (CheckBox) findViewById(R.id.checkbox_fecha);
		checkNombre = (CheckBox) findViewById(R.id.checkbox_nombre);
		
		botonOk = (Button) findViewById(R.id.okButton);
		
		botonOk.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if (checkfecha.isChecked() && checkNombre.isChecked() ){
					Context context = getApplicationContext();
					Toast error = Toast.makeText(context, "No se pueden marcar ambas opciones a la vez", Toast.LENGTH_LONG);
			        error.show();
				} else {
					savePreferences();
				}
				finish();
				
			}
		});
		
		botonCancel = (Button) findViewById(R.id.cancelButton);
		
		botonCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				erasePreferences();
				finish();
				
			}
		});

		Context context = getApplicationContext();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		updateUIFromPreferences();
	}
	
	private void updateUIFromPreferences() {
		boolean fecha = prefs.getBoolean(PREF_ORDEN_FECHA,
				false);
		boolean nombre = prefs.getBoolean(PREF_ORDEN_NOMBRE, false);
		checkfecha.setChecked(fecha);
		checkNombre.setChecked(nombre);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void savePreferences() {
		boolean ordenFecha = checkfecha.isChecked();
		boolean ordenNombre = checkNombre.isChecked();
		Editor editor = prefs.edit();
		editor.putBoolean(PREF_ORDEN_FECHA, ordenFecha);
		editor.putBoolean(PREF_ORDEN_NOMBRE, ordenNombre);
		editor.commit();
		}
	
	private void erasePreferences() {
		Editor editor = prefs.edit();
		editor.putBoolean(PREF_ORDEN_FECHA, false);
		editor.putBoolean(PREF_ORDEN_NOMBRE, false);
		editor.commit();
		}

}

