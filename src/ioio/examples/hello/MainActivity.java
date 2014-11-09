package ioio.examples.hello;

/******************************************************************************************
 * Happy version 141108B Refactoring
 * Changed decay mode to slow for lower power dissipation
 * added timing examples for PW spec = 2, cue duration = 60000, FMspeedCue.period = 600
 * default queue size is 32...to change queue size:
 * sequencer.setEventQueueSize(newSize);
 * Time Base:
 * 1/16 microseconds = 62.5 nanoseconds.Clk_16M
 * 1/2 microseconds = 500 nanoseconds.Clk_2M
 * 4 microseconds...Clk_250K
 * 16 microseconds...Clk_62K5 
 * Pulse width = 2/62500 seconds = 32 microsecs (multiple of clock period)
 * Period = 600/62500 seconds = .0096 secs (multiple of clock period)
 * Cue duration = 60000 * 16us = .96 secs (cue duration is always a multiple of 16 microsecs)
 ********************************************************************************************/
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.hardware.Sensor;
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
	private Sensor sensorAccelerometer;
	private Sensor sensorMagneticField;
	private float[] valuesAccelerometer;
	private float[] valuesMagneticField;
	private float[] matrixR;
	private float[] matrixI;
	private float[] matrixValues;
	private double azimuth;
	private double pitch;
	private double roll;
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
			vw.configureVicsWagonStandard();
			sonar = new UltraSonicSensor(ioio_);
			led = ioio_.openDigitalOutput(0, true);
		}

		@Override
		public void loop() throws ConnectionLostException
		{
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
