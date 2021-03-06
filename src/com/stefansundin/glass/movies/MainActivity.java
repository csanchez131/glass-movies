package com.stefansundin.glass.movies;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.glass.touchpad.GestureDetector;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity {

	private String mMovieDirectory;
	private GestureDetector mGestureDetector;
	private TextToSpeech mSpeech;
	private BroadcastReceiver mIntentBlocker;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout);

		mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			public void onInit(int status) {
				// Must be declared for TTS
			}
		});

		// Block wink gesture
		mIntentBlocker = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				abortBroadcast();
				Log.d("stefan", "Blocking intent.");
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.google.glass.action.EYE_GESTURE");
		registerReceiver(mIntentBlocker, filter);

		ArrayAdapter<String> movieList = new ArrayAdapter<String>(this, android.R.layout.test_list_item);
		ListView listView = (ListView) findViewById(R.id.listView);
		listView.setAdapter(movieList);

		mMovieDirectory = Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_MOVIES;
		Log.d("stefan", "Movie directory: "+mMovieDirectory);

		File dir = new File(mMovieDirectory);
		File[] files = dir.listFiles();
		if (files.length == 0) {
			String error = "Yo, get some video files first";
			movieList.add(error);
			say(error);
		}
		else {
			ArrayList<String> filenames = new ArrayList<String>();
			for (File file : files) {
				filenames.add(file.getName());
			}
			java.util.Collections.sort(filenames);
			for (String filename : filenames) {
				movieList.add(filename);
			}
			listView.setSelection(0);
		}

		Touchpad touchpad = new Touchpad(listView, this);
		mGestureDetector = new GestureDetector(this);
		mGestureDetector.setBaseListener(touchpad);
		mGestureDetector.setFingerListener(touchpad);
		mGestureDetector.setScrollListener(touchpad);
	}

	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}

	public void say(String filename) {
		int dot = filename.lastIndexOf(".");
		if (dot != -1) {
			filename = filename.substring(0, dot);
		}
		mSpeech.speak(filename, TextToSpeech.QUEUE_FLUSH, null);
		Log.d("stefan", "Saying: "+filename);
	}

	public void launchVideo(String filename) {
		say(filename);
		String path = mMovieDirectory+"/"+filename;
		File file = new File(path);
		if (!file.exists()) {
			return;
		}

		Intent i = new Intent();
		i.setAction("com.google.glass.action.VIDEOPLAYER");
		i.putExtra("video_url", path);
		startActivity(i);
	}

	protected void onDestroy() {
		Log.d("stefan", "Exiting app");
		super.onDestroy();
		mSpeech.shutdown();
		unregisterReceiver(mIntentBlocker);
	}

}
