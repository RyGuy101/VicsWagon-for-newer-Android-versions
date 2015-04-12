package ioio.examples.hello;

/**************************************************************************
 * Happy version 141013A Sequencer works with both wheels FM Speed
 * 
 * 
 * time base:
 * 1/16 microseconds = 62.5ns  Clk_16M
 * 1/2 microseconds = 500ns    Clk_2M
 * 4 microseconds              Clk_250K
 * 16 microseconds             Clk_62K5
 * 
 * 
 * 
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

public class MainActivity extends IOIOActivity {
	private static final int FRONT_STROBE_ULTRASONIC_OUTPUT_PIN = 16;
	private static final int LEFT_STROBE_ULTRASONIC_OUTPUT_PIN = 17;
	private static final int RIGHT_STROBE_ULTRASONIC_OUTPUT_PIN = 15;
	private static final int REAR_STROBE_ULTRASONIC_OUTPUT_PIN = 14;// ioio
	private static final int FRONT_ULTRASONIC_INPUT_PIN = 12;
	private static final int LEFT_ULTRASONIC_INPUT_PIN = 13;
	private static final int RIGHT_ULTRASONIC_INPUT_PIN = 11;
	private static final int REAR_ULTRASONIC_INPUT_PIN = 10;// input to ioio
	private static final int MOTOR_ENABLE_PIN = 3;// Low turns both motors
	private static final int MOTOR_RIGHT_DIRECTION_PIN = 20;// High => cw
	private static final int MOTOR_LEFT_DIRECTION_PIN = 21;
	private static final int MOTOR_CONTROLLER_CONTROL_PIN = 6;// For both motors
																	// output
	private static final int MOTOR_HALF_FULL_STEP_PIN = 7;// For both motors
	private static final int MOTOR_RESET = 22;// For both motors
	private static final int MOTOR_CLOCK_LEFT_PIN = 27;
	private static final int MOTOR_CLOCK_RIGHT_PIN = 28;
	private int i = 0;
	private ToggleButton button;
	public  UltraSonicSensor sonar;
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
	private DigitalOutput led;    // The on-board LED
	private int pulseWidth = 10;  // 10/62500 seconds = 160us
	private int rightStepperMotorPeriod = 10000; // this period at 62500 push
													// gives 1/2 revolution at
													// the wheel
	private int leftStepperMotorPeriod = 10000;  // therefore it is microseconds
	private DigitalOutput rightMotorClockPulse;
	private DigitalOutput leftMotorClockPulse;
	private DigitalOutput motorEnable; // Both motors
	private DigitalOutput rightMotorClock; // Step right motor
	private DigitalOutput leftMotorClock; // Step left motor
	private DigitalOutput motorControllerReset;
	private DigitalOutput rightMotorDirection;
	private DigitalOutput leftMotorDirection;
	private DigitalOutput motorControllerControl;// Decay mode selector, high =
													// slow decay, low = fast
	private DigitalOutput halfFull;
	private DigitalOutput reset; // Must be true for motors to run.
	private DigitalOutput control;// Decay mode selector high = slow, low = fast
	final ChannelConfigSteps stepperStepConfig = new ChannelConfigSteps(
			new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfigFmSpeed stepperRightFMspeedConfig = new ChannelConfigFmSpeed(
			Clock.CLK_2M, pulseWidth, new DigitalOutput.Spec(
					MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfigFmSpeed stepperLeftFMspeedConfig = new ChannelConfigFmSpeed(
			Clock.CLK_2M, pulseWidth, new DigitalOutput.Spec(
					MOTOR_CLOCK_LEFT_PIN));
	final ChannelConfigBinary stepperRightDirConfig = new Sequencer.ChannelConfigBinary(
			false, false, new DigitalOutput.Spec(MOTOR_RIGHT_DIRECTION_PIN));
	final ChannelConfigBinary stepperLeftDirConfig = new Sequencer.ChannelConfigBinary(
			false, false, new DigitalOutput.Spec(MOTOR_LEFT_DIRECTION_PIN));
	final ChannelConfig[] channelConfigList = new ChannelConfig[] {
			stepperRightFMspeedConfig, stepperLeftFMspeedConfig };// stepperFMspeedConfig//stepperStepConfig
	private Sequencer sequencer;
	private Sequencer.ChannelCueBinary  stepperDirCue = new ChannelCueBinary();
	private Sequencer.ChannelCueSteps   stepperStepCue = new ChannelCueSteps();
	private Sequencer.ChannelCueFmSpeed stepperRightFMspeedCue = new ChannelCueFmSpeed();
	private Sequencer.ChannelCueFmSpeed stepperLeftFMspeedCue = new ChannelCueFmSpeed();
	private Sequencer.ChannelCue[] cueList = new Sequencer.ChannelCue[] {
			stepperRightFMspeedCue, stepperLeftFMspeedCue };// stepperStepCue//stepperFMspeedCue

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		button = (ToggleButton) findViewById(R.id.button);
		mText = (TextView) findViewById(R.id.logText);
		mScroller = (ScrollView) findViewById(R.id.scroller);
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorAccelerometer = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorMagneticField = sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		valuesAccelerometer = new float[3];
		valuesMagneticField = new float[3];
		matrixR = new float[9];
		matrixI = new float[9];
		matrixValues = new float[3];
	}

	class Looper extends BaseIOIOLooper {

		/*********************************************************************************
		 * Wave drive mode (full step one phase on) A LOW level on the pin
		 * HALF/FULL input selects the full step mode. When the low level is
		 * applied when the state machine is at an EVEN numbered state the wave
		 * drive mode is selected. To enter the wave drive mode the state
		 * machine must be in an EVEN numbered state. The most direct method to
		 * select the wave drive mode is to first apply a RESET, then while
		 * keeping the HALF/FULL input high apply one pulse to the clock input
		 * then take the HALF/FULL input low. This sequence first forces the
		 * state machine to state 1. The clock pulse, with the HALF/FULL input
		 * high advances the state machine from state 1 to either state 2 or 8
		 * depending on the CW/CCW input. Starting from this point, after each
		 * clock pulse (rising edge) will advance the state machine following
		 * the sequence 2, 4, 6, 8, etc. if CW/CCW is high (clockwise movement)
		 * or 8, 6, 4, 2, etc. if CW/CCW is low (counterclockwise movement).
		 **********************************************************************************/
		@Override
		protected void setup() throws ConnectionLostException {
			sonar = new UltraSonicSensor(ioio_);
			led = ioio_.openDigitalOutput(0, true);

			// motor setup
			rightMotorDirection = ioio_.openDigitalOutput(
					MOTOR_RIGHT_DIRECTION_PIN, true);
			leftMotorDirection = ioio_.openDigitalOutput(
					MOTOR_LEFT_DIRECTION_PIN, false);
			motorControllerReset = ioio_.openDigitalOutput(MOTOR_RESET, true);
			motorEnable = ioio_.openDigitalOutput(MOTOR_ENABLE_PIN, true);// enable
			motorControllerControl = ioio_.openDigitalOutput(
					MOTOR_CONTROLLER_CONTROL_PIN, true);// true = slow fast
			halfFull = ioio_.openDigitalOutput(MOTOR_HALF_FULL_STEP_PIN, false);// high
																				// is
																				// halfstep
			// configure to wave drive
			leftMotorClock = ioio_
					.openDigitalOutput(MOTOR_CLOCK_LEFT_PIN, true);
			rightMotorClock = ioio_.openDigitalOutput(MOTOR_CLOCK_RIGHT_PIN,
					true);
			motorControllerReset.write(false);
			motorControllerReset.write(true);
			halfFull.write(true);// Half
			rightMotorClock.write(false);
			rightMotorClock.write(true);
			leftMotorClock.write(false);
			leftMotorClock.write(true);
			halfFull.write(false);// Full
			leftMotorClock.close();
			rightMotorClock.close();

			try {
				sequencer = ioio_.openSequencer(channelConfigList);
				sequencer.waitEventType(Sequencer.Event.Type.STOPPED);

				// while (sequencer.available() > 0)
				// {
				// addCueToCueList();
				// }
				sequencer.start();

			} catch (Exception e) {
			}
			log("sequencer started .");
		}

		@Override
		public void loop() throws ConnectionLostException {
			if (button.isChecked()) {
				led.write(false);
				try {
					// addCueToCueList();
					// wheel diameter 125mm
					// 400 steps per rev gives 0.981747 mm per step
					// or 1963.49 mm
					goMM(1000);  // 
					Thread.sleep(4000);
					turn(120);
					Thread.sleep(4000);
					// addCueToCueList();
					//goMM(-3000);
					//Thread.sleep(5000);

				} catch (Exception e) {
				}
				// joey's stuff
				//if(sonar.getFrontDistance()>3)
				//{
					
					//stop();
				//}
			} else {
				led.write(true);
				// test the ultrasonic sensor
				try {
					sonar.read();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int distance = sonar.getFrontDistance();
				log("front distance " + distance);
			}
		}
	}
	
	
    private void turn(int deg) throws ConnectionLostException {
    	// zero radius turn of deg degrees
		if(deg >= 0) {
			rightMotorDirection.write(true);
			leftMotorDirection.write(true);
		} else {
			rightMotorDirection.write(false);
			leftMotorDirection.write(false);
		}
		int period;
		double duration;
		int StepsPerPush = 16;
		int steps = (int)(2.75 * (double)deg);
		period = (int) (1000000.0 / 30.0);
		int numPushes = steps / StepsPerPush;
		for(int i = 0; i < numPushes; i++) {
			stepperRightFMspeedCue.period = period; // period is in micro seconds
			stepperLeftFMspeedCue.period = period;
			duration = (int)((double)period/60.0 * (double)StepsPerPush);
			log("Turn " + period + " duration " + duration);
			try {
				sequencer.push(cueList, (int)duration);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				log("EXCEPTION" + e.getMessage());
			}
		}
    }
    
	private void goMM(int mm) throws ConnectionLostException {  // Go Millimeters
		
		double StepsPerMM  = 1.000;
		int steps = (int)Math.abs((double)(mm) * StepsPerMM);
		int StepsPerPush = 16;  // magic so that the queue duration same as period
		// MaxFreq based on what you motor can handle without slipping...
		double maxFreq = 1000.0; // steps per second ran at 3000 some glitches 2500 was good
		double minFreq = 16.0;
		double maxDelFreq = 4.0; // steps per second ran at 5.0 some glitches  4 was good
		int numPushes = steps / StepsPerPush;
		if(mm >= 0) {
			rightMotorDirection.write(true);
			leftMotorDirection.write(false);
		} else {
			rightMotorDirection.write(false);
			leftMotorDirection.write(true);
		}
		
		double freq;
		double maxfrac;
		double delfreq;
		double duration;
		int    period;
		int    i;
		//log("In goSteps");
		freq = minFreq;
		for (i = 0; i < numPushes; i++) {
			maxfrac = (maxFreq - freq) / maxFreq;
			delfreq = (maxfrac * maxfrac) * maxDelFreq;
			if (i < numPushes / 2) {
				freq += delfreq;
			} else if (i > numPushes / 2) {
				freq -= delfreq;
			}

			//log("i = " + i + " freq = " + freq + " numPushes " + numPushes);

			try {
				// translate freq steps/sec into period (usec) period = 1/freq
				// 1/2000 = 0.0005 but translate to usec divide by 0.000001
				// gives 500
				// period = 1000000/freq;
				if(freq <= minFreq) freq = minFreq;
				period = (int) (1000000.0 / freq);
				if(period < 1) period = 1; // limits for the period
				if(period > 65535) period = 65535;
				stepperRightFMspeedCue.period = period; // period is in micro seconds
				stepperLeftFMspeedCue.period = period;
				duration = (period/60.0 * StepsPerPush);
				log("i = " + i + " freq = " + freq + " period " + period + " duration " + duration);
				// second parameter in the push(cue, duration)
				// 62500 cue duration 62500 * 16us = 1s  
				// using period gives 16 steps per push
				sequencer.push(cueList, (int)duration);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log("EXCEPTION" + e.getMessage());
			}
		}
	}

	private void addCueToCueList() {
		int minperiod = 5000;
		int maxperiod = 50000;
		//int frac;

		try {

			/*
			 * for(frac = 0; frac <= 10; frac ++) {
			 * stepperRightFMspeedCue.period = (minperiod * frac + maxperiod *
			 * (10 - frac)) / 10; stepperLeftFMspeedCue.period = (minperiod *
			 * frac + maxperiod * (10 - frac)) / 10; sequencer.push(cueList,
			 * 6250); // 62500 cue duration 62500 * 16us = 1s }
			 * 
			 * stepperRightFMspeedCue.period = minperiod;
			 * stepperLeftFMspeedCue.period = minperiod; sequencer.push(cueList,
			 * 62500); // 62500 cue duration 62500 * 16us = 1s
			 * 
			 * for(frac = 10; frac >= 0; frac --) {
			 * stepperRightFMspeedCue.period = (minperiod * frac + maxperiod *
			 * (10-frac)) / 10; stepperLeftFMspeedCue.period = (minperiod * frac
			 * + maxperiod * (10-frac)) / 10; sequencer.push(cueList, 6250); //
			 * 62500 cue duration 62500 * 16us = 1s }
			 */
			int sineperiod;

			for (int i = 0; i < 20; i++) {
				sineperiod = (int) ((double) maxperiod
						* ((Math.cos((double) i / 40.0 * 6.28) + 1.0) * 0.5) + (double) minperiod);
				stepperRightFMspeedCue.period = sineperiod;
				stepperLeftFMspeedCue.period = sineperiod;
				// 62500 cue duration 62500 * 16us = 1s
				sequencer.push(cueList, 6250); 
				log("sin period = " + sineperiod);
			}

			for (int i = 19; i >= 0; i--) {
				sineperiod = (int) ((double) maxperiod
						* ((Math.cos((double) i / 40.0 * 6.28) + 1.0) * 0.5) + (double) minperiod);
				stepperRightFMspeedCue.period = sineperiod;
				stepperLeftFMspeedCue.period = sineperiod;
				sequencer.push(cueList, 6250); // 62500 cue duration 62500 *
												// 16us = 1s
				log("sin period = " + sineperiod);
			}
			// period of 5000 give 200 steps in 1 sec

			// log("AddCueToQueue " + i++);
		} catch (Exception e) {
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
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			for (int i = 0; i < 3; i++) {
				valuesAccelerometer[i] = event.values[i];
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			for (int i = 0; i < 3; i++) {
				valuesMagneticField[i] = event.values[i];
			}
			break;
		}

		boolean success = SensorManager.getRotationMatrix(matrixR, matrixI,
				valuesAccelerometer, valuesMagneticField);
		log(success + "  success");
		if (success) {
			SensorManager.getOrientation(matrixR, matrixValues);
			synchronized (this) {
				azimuth = Math.toDegrees(matrixValues[0]);
				pitch = Math.toDegrees(matrixValues[1]);
				roll = Math.toDegrees(matrixValues[2]);
			}
		}
	}

	public synchronized double getAzimuth() {
		return azimuth;
	}

	public synchronized double getPitch() {
		return pitch;
	}

	public synchronized double getRoll() {
		return roll;
	}
}
