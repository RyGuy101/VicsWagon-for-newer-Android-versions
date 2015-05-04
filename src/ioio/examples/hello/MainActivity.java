package ioio.examples.hello;

/******************************************************************************************
 * Happy version 150228B...IntelliJ version
 * Added comments for Full and Half Step modes
 * minor tweaks
 ********************************************************************************************/

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.content.pm.ActivityInfo;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends IOIOActivity {//
	public UltraSonicSensor sonar;
	private TextView mText;
	private ScrollView mScroller;
	private TextToSpeech mTts;
	private SensorManager sensorManager;
	private DigitalOutput led;// The on-board LED
	private Accelerometer accelerometer;
	private VicsWagon vw;
	private boolean powerOn = false;
	private double defaultSpeed = 2.5;

	public static MainActivity activity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mText = (TextView) findViewById(R.id.logText);
		mScroller = (ScrollView) findViewById(R.id.scroller);
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		activity = this;
		VicsWagon.calculateTurn(38);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!powerOn) {
			item.setIcon(R.drawable.power_on);
			powerOn = true;
		} else {
			item.setIcon(R.drawable.power_off);
			powerOn = false;
		}
		return true;
	}

	class Looper extends BaseIOIOLooper {
		@Override
		protected void setup() throws ConnectionLostException {
			accelerometer = new Accelerometer(sensorManager, ioio_);
			vw = new VicsWagon(ioio_);
			sonar = new UltraSonicSensor(ioio_);
			vw.sonar = sonar;
			led = ioio_.openDigitalOutput(0, true);
			vw.configureVicsWagonStandard();
		}

		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			if (powerOn) {
				led.write(false);
				vw.goForward(2000);
				SystemClock.sleep(5000);
			} else {
				led.write(true);
			}
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	public void log(final String msg) {
		runOnUiThread(new Runnable() {
			public void run() {
				mText.append(msg);
				mText.append("\n");
				mScroller.smoothScrollTo(0, mText.getBottom());
			}
		});
	}

	public void onSensorChanged(SensorEvent event) {
		accelerometer.onSensorChanged(event);
	}
}
