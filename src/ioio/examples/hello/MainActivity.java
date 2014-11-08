package ioio.examples.hello;

/**************************************************************************
 * Happy version 141103A Sequencer works with both wheels FM Speed
 * Changed decay mode to slow
 * added timing examples for PW spec = 10, cue duration = 60000, FMspeedCue.period = 60000
 * Time Base:
 * 1/16 microseconds = 62.5 nanoseconds.Clk_16M
 * 1/2 microseconds = 500 nanoseconds.Clk_2M
 * 4 microseconds...Clk_250K
 * 16 microseconds...Clk_62K5 
 * Pulse width = 10/62500 seconds = 160 microsecs
 * Period = 60000/62500 seconds = .96 secs
 * Cue duration = 60000 * 16us = .96 secs
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
import android.os.SystemClock;
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
	private static final int RIGHT_ULTRASONIC_INPUT_PIN = 11;
	private static final int LEFT_ULTRASONIC_INPUT_PIN = 13;
	private static final int MOTOR_ENABLE_PIN = 3;// Low turns both motors
	private static final int MOTOR_RIGHT_DIRECTION_PIN = 20;// High => cw
	private static final int MOTOR_LEFT_DIRECTION_PIN = 21;
	private static final int MOTOR_CONTROLLER_CONTROL_PIN = 6;// For both motors
	private static final int REAR_STROBE_ULTRASONIC_OUTPUT_PIN = 14;// ioio out
	private static final int MOTOR_HALF_FULL_STEP_PIN = 7;// For both motors
	private static final int MOTOR_RESET = 22;// For both motors
	private static final int MOTOR_CLOCK_LEFT_PIN = 27;
	private static final int MOTOR_CLOCK_RIGHT_PIN = 28;
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
	private DigitalOutput rightMotorClockPulse;
	private DigitalOutput leftMotorClockPulse;
	private DigitalOutput motorEnable; // Both motors
	private DigitalOutput rightMotorClock; // Step right motor
	private DigitalOutput leftMotorClock; // Step left motor
	private DigitalOutput motorControllerReset;
	private DigitalOutput rightMotorDirection;
	private DigitalOutput leftMotorDirection;
	private DigitalOutput motorControllerControl;// Decay mode high => slow
	private DigitalOutput halfFull;//High => half step
	private DigitalOutput reset; // Must be true for motors to run.
	final ChannelConfigSteps stepperStepConfig = new ChannelConfigSteps(new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfigFmSpeed stepperRightFMspeedConfig = new ChannelConfigFmSpeed(Clock.CLK_62K5, 10, new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfigFmSpeed stepperLeftFMspeedConfig = new ChannelConfigFmSpeed(Clock.CLK_62K5, 10, new DigitalOutput.Spec(MOTOR_CLOCK_LEFT_PIN));
	final ChannelConfigBinary stepperRightDirConfig = new Sequencer.ChannelConfigBinary(false, false, new DigitalOutput.Spec(MOTOR_RIGHT_DIRECTION_PIN));
	final ChannelConfigBinary stepperLeftDirConfig = new Sequencer.ChannelConfigBinary(false, false, new DigitalOutput.Spec(MOTOR_LEFT_DIRECTION_PIN));
	final ChannelConfig[] channelConfigList = new ChannelConfig[] { stepperRightFMspeedConfig, stepperLeftFMspeedConfig };// stepperFMspeedConfig//stepperStepConfig
	private Sequencer sequencer;
	private Sequencer.ChannelCueBinary stepperDirCue = new ChannelCueBinary();
	private Sequencer.ChannelCueFmSpeed stepperRightFMspeedCue = new ChannelCueFmSpeed();
	private Sequencer.ChannelCueFmSpeed stepperLeftFMspeedCue = new ChannelCueFmSpeed();
	private Sequencer.ChannelCue[] cueList = new Sequencer.ChannelCue[] { stepperRightFMspeedCue, stepperLeftFMspeedCue };// stepperStepCue//stepperFMspeedCue
	
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
			rightMotorDirection = ioio_.openDigitalOutput(MOTOR_RIGHT_DIRECTION_PIN, true);
			leftMotorDirection = ioio_.openDigitalOutput(MOTOR_LEFT_DIRECTION_PIN, false);
			motorControllerReset = ioio_.openDigitalOutput(MOTOR_RESET, true);
			motorEnable = ioio_.openDigitalOutput(MOTOR_ENABLE_PIN, true);// enable
			motorControllerControl = ioio_.openDigitalOutput(MOTOR_CONTROLLER_CONTROL_PIN, true);// slow
			halfFull = ioio_.openDigitalOutput(MOTOR_HALF_FULL_STEP_PIN, false);//Full step
			try
			{
				sequencer = ioio_.openSequencer(channelConfigList);
				sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
				stepperRightFMspeedCue.period = 60000;
				stepperLeftFMspeedCue.period = 60000;
				 while (sequencer.available() > 0)
				{
					// for (int i = 0; i < 10; i++)
						{
						 sequencer.push(cueList, 60000);
							log("step");
						}
				}
				
				sequencer.start();

			} catch (Exception e)
			{
			}
		}

		@Override
		public void loop() throws ConnectionLostException
		{
			if (button.isChecked())
			{
				led.write(false);
				try
				{
//					 if (sequencer.available() > 0)//pre load cue list
//					 {
//					 addCueToCueList();
//					 }
					// log(String.valueOf(sonar.getFrontDistance() + " " +
					// sonar.getLeftDistance() + " " + getAzimuth()));

				} catch (Exception e)
				{
				}
			} else
			{
				led.write(true);
			}
		}
	}

	private void addCueToCueList()
	{
		stepperRightFMspeedCue.period = 65535;
		stepperLeftFMspeedCue.period = 65535;
		try
		{
			for (int i = 0; i < 10; i++)
			{
				sequencer.push(cueList, 65535 / 4);// 1 step
				log("step" + i);
			}
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
