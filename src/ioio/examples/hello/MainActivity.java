package ioio.examples.hello;

/******************************************************************************************
 * Happy version 141127A
 * Wave Stepping...not verified with scope
 ********************************************************************************************/
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends IOIOActivity
{
	private ToggleButton button;
	public UltraSonicSensor sonar;
	private TextView mText;
	private ScrollView mScroller;
	private TextToSpeech mTts;
	private SensorManager sensorManager;
	private DigitalOutput led;// The on-board LED
	Accelerometer accelerometer;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		button = (ToggleButton) findViewById(R.id.button);
		mText = (TextView) findViewById(R.id.logText);
		mScroller = (ScrollView) findViewById(R.id.scroller);
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	}

	class Looper extends BaseIOIOLooper
	{
		@Override
		protected void setup() throws ConnectionLostException
		{
			accelerometer = new Accelerometer(sensorManager, ioio_);
			VicsWagon vw = new VicsWagon(ioio_);
			sonar = new UltraSonicSensor(ioio_);
			led = ioio_.openDigitalOutput(0, true);
			vw.configureVicsWagonStandard();
		}

		@Override
		public void loop() throws ConnectionLostException
		{
			log("loop");
			if (button.isChecked())
			{
				led.write(false);
			} else
			{
				led.write(true);
			}
		}
	}


	@Override
	protected IOIOLooper createIOIOLooper()
	{
		return new Looper();
	}

	public void log(final String msg)
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				mText.append(msg);
				mText.append("\n");
				mScroller.smoothScrollTo(0, mText.getBottom());
			}
		});
	}

	public void onSensorChanged(SensorEvent event)
	{
		accelerometer.onSensorChanged(event);
	}
}
