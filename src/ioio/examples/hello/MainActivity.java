package ioio.examples.hello;

/**************************************************************************
 * Happy version 141006A Single step works...trying to get sequencer working
 **************************************************************************/
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.Sequencer;
import ioio.lib.api.Sequencer.ChannelConfigBinary;
import ioio.lib.api.Sequencer.ChannelConfigSteps;
import ioio.lib.api.Sequencer.ChannelCueBinary;
import ioio.lib.api.Sequencer.ChannelCueFmSpeed;
import ioio.lib.api.Sequencer.ChannelCueSteps;
import ioio.lib.api.Sequencer.Clock;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.content.Intent;
import android.graphics.DashPathEffect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;
import ioio.lib.api.Sequencer.ChannelConfig;
import ioio.lib.api.Sequencer.ChannelConfigBinary;
import ioio.lib.api.Sequencer.ChannelConfigFmSpeed;
import ioio.lib.api.Sequencer.ChannelConfigSteps;

public class MainActivity extends IOIOActivity
{
	private static final int FRONT_STROBE_ULTRASONIC_OUTPUT_PIN = 16;
	private static final int LEFT_STROBE_ULTRASONIC_OUTPUT_PIN = 17;
	private static final int RIGHT_STROBE_ULTRASONIC_OUTPUT_PIN = 15;
	private static final int FRONT_ULTRASONIC_INPUT_PIN = 12;
	private static final int REAR_ULTRASONIC_INPUT_PIN = 10;// input to ioio
															// board
	private static final int RIGHT_ULTRASONIC_INPUT_PIN = 11;
	private static final int LEFT_ULTRASONIC_INPUT_PIN = 13;
	private static final int MOTOR_ENABLE_PIN = 3;// Low turns off all power to
	// both motors
	private static final int MOTOR_RIGHT_DIRECTION_OUTPUT_PIN = 20;// High means
	// clockwise
	private static final int MOTOR_LEFT_DIRECTION_OUTPUT_PIN = 21;
	private static final int MOTOR_CONTROLLER_CONTROL_PIN = 6;// For both motors
	private static final int REAR_STROBE_ULTRASONIC_OUTPUT_PIN = 14;// output
	// from ioio
	// board
	private static final int MOTOR_HALF_FULL_STEP_PIN = 7;// For both motors
	private static final int MOTOR_RESET = 22;// For both motors
	private static final int MOTOR_CLOCK_LEFT_PIN = 27;
	private static final int MOTOR_CLOCK_RIGHT_PIN = 28;
	final ChannelConfigSteps stepperStepConfig = new ChannelConfigSteps(new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfigFmSpeed stepperFMspeedConfig = new ChannelConfigFmSpeed(Clock.CLK_2M, 10,
			new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfig[] config = new ChannelConfig[] { stepperFMspeedConfig };
	private Sequencer sequencer;
	private Sequencer.ChannelCueBinary stepperDirCue = new ChannelCueBinary();
	private Sequencer.ChannelCueSteps stepperStepCue = new ChannelCueSteps();
	private Sequencer.ChannelCueFmSpeed stepperFMspeedCue = new ChannelCueFmSpeed();
	private Sequencer.ChannelCue[] queue = new Sequencer.ChannelCue[] { stepperFMspeedCue };
	final ChannelConfigBinary stepperDirConfig = new Sequencer.ChannelConfigBinary(false, false,
			new DigitalOutput.Spec(MOTOR_RIGHT_DIRECTION_OUTPUT_PIN));
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
	private int pulseWidth = 10;// microseconds
	private int rightStepperMotorPeriod = 60000;
	private DigitalOutput rightMotorClockPulse;
	private DigitalOutput leftMotorClockPulse;
	private DigitalOutput motorEnable; // Both motors
	private DigitalOutput rightMotorClock; // Step right motor
	private DigitalOutput leftMotorClock; // Step left motor
	private DigitalOutput motorCongtrollerReset;
	private DigitalOutput rightMotorControl; // Motor decay mode
	private DigitalOutput rightMotorDirection;
	private DigitalOutput leftMotorDirection;
	private DigitalOutput motorControllerControl;// Decay mode selector, high =
													// slow decay, low = fast
													// decay
	private DigitalOutput halfFull;
	private DigitalOutput reset; // Must be true for motors to run.
	private DigitalOutput control;// Decay mode selector high = slow, low = fast

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		button = (ToggleButton) findViewById(R.id.button);
		mText = (TextView) findViewById(R.id.logText);
		mScroller = (ScrollView) findViewById(R.id.scroller);
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		valuesAccelerometer = new float[3];
		valuesMagneticField = new float[3];
		matrixR = new float[9];
		matrixI = new float[9];
		matrixValues = new float[3];
	}

	class Looper extends BaseIOIOLooper
	{

		@Override
		protected void setup() throws ConnectionLostException
		{
			sonar = new UltraSonicSensor(ioio_);
			led = ioio_.openDigitalOutput(0, true);
			rightMotorDirection = ioio_.openDigitalOutput(MOTOR_RIGHT_DIRECTION_OUTPUT_PIN, true);
			leftMotorDirection = ioio_.openDigitalOutput(MOTOR_LEFT_DIRECTION_OUTPUT_PIN, false);
			motorCongtrollerReset = ioio_.openDigitalOutput(MOTOR_RESET, true);
			motorEnable = ioio_.openDigitalOutput(MOTOR_ENABLE_PIN, true);// enable
			rightMotorClock = ioio_.openDigitalOutput(MOTOR_CLOCK_RIGHT_PIN, false);// step
			leftMotorClock = ioio_.openDigitalOutput(MOTOR_CLOCK_LEFT_PIN, false);
			rightMotorControl = ioio_.openDigitalOutput(MOTOR_CONTROLLER_CONTROL_PIN, false);// fast
																								// decay
			try
			{
				sequencer = ioio_.openSequencer(config);
				sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
				while (sequencer.available() > 0)
				{
					push();
				}
				sequencer.start();

			} catch (Exception e)
			{
			}
			log("sequencer started");
		}

		@Override
		public void loop() throws ConnectionLostException
		{
			if (button.isChecked())
			{
				led.write(false);
				try
				{
					Thread.sleep(1000);
					rightMotorClock.write(true);
					rightMotorClock.write(false);
					leftMotorClock.write(true);
					leftMotorClock.write(false);
					// push();
					log(String.valueOf(sonar.getFrontDistance() + " " + sonar.getLeftDistance() + " " + getAzimuth()));

				} catch (Exception e)
				{
				}
			} else
			{
				led.write(true);
			}
		}
	}

	private void push()
	{
		stepperFMspeedCue.period = rightStepperMotorPeriod *= .9;
		try
		{
			sequencer.push(queue, 62500);
		} catch (Exception e)
		{
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
		switch (event.sensor.getType())
		{
		case Sensor.TYPE_ACCELEROMETER:
			for (int i = 0; i < 3; i++)
			{
				valuesAccelerometer[i] = event.values[i];
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			for (int i = 0; i < 3; i++)
			{
				valuesMagneticField[i] = event.values[i];
			}
			break;
		}

		boolean success = SensorManager.getRotationMatrix(matrixR, matrixI, valuesAccelerometer, valuesMagneticField);
		log(success + "  success");
		if (success)
		{
			SensorManager.getOrientation(matrixR, matrixValues);
			synchronized (this)
			{
				azimuth = Math.toDegrees(matrixValues[0]);
				pitch = Math.toDegrees(matrixValues[1]);
				roll = Math.toDegrees(matrixValues[2]);
			}
		}
	}

	public synchronized double getAzimuth()
	{
		return azimuth;
	}

	public synchronized double getPitch()
	{
		return pitch;
	}

	public synchronized double getRoll()
	{
		return roll;
	}
}
