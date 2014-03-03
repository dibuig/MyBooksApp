package es.bq.mybooks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

public class ShowCoverActivity extends Activity {

	private ImageView image;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cover);

		try {
			String data = getIntent().getDataString();
			File sdcard = Environment.getExternalStorageDirectory();
			String ebookfile = data.substring(data.lastIndexOf("/"));
			File ebook = new File(sdcard, ebookfile);

			InputStream epubInputStream = new FileInputStream(ebook);
			// Obtenemos el ebook de la tarjeta SD y creamos el objeto Book con
			// la librería epublib-core
			// Load Book from inputStream
			Book book = (new EpubReader()).readEpub(epubInputStream);

			// Obtenemos la imagen de portada y se la asociamos al imageView
			Bitmap coverImage = BitmapFactory.decodeStream(book.getCoverImage()
					.getInputStream());

			image = (ImageView) findViewById(R.id.imageView1);

			image.setImageBitmap(coverImage);

		} catch (IOException e) {
			Log.e("epublib", e.getMessage());
		}
	}

}
