package ioio.examples.hello;

/******************************************************************************************
 * Happy version 150228B...IntelliJ version
 * Added Duane's code for wave drive ... commented out
 ********************************************************************************************/
import java.util.ArrayList;
import java.util.HashMap;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.Sequencer;
import ioio.lib.api.Sequencer.ChannelConfig;
import ioio.lib.api.Sequencer.ChannelConfigBinary;
import ioio.lib.api.Sequencer.ChannelConfigFmSpeed;
import ioio.lib.api.Sequencer.ChannelConfigSteps;
import ioio.lib.api.Sequencer.ChannelCueBinary;
import ioio.lib.api.Sequencer.ChannelCueFmSpeed;
import ioio.lib.api.Sequencer.Clock;
import ioio.lib.api.Sequencer.Event;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;
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
import android.R.dimen;
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

public class VicsWagon {
	private IOIO ioio_;
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
	private DigitalOutput motorEnable; // Both motors
	private DigitalOutput rightMotorClock; // Step right motor
	private DigitalOutput leftMotorClock; // Step left motor
	private DigitalOutput motorControllerReset;
	private DigitalOutput rightMotorDirection;
	private DigitalOutput leftMotorDirection;
	private DigitalOutput motorControllerControl;// Decay mode high => slow
	private DigitalOutput halfFull;// High => half step
	private Sequencer sequencer;
	final ChannelConfigSteps stepperStepConfig = new ChannelConfigSteps(new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfigBinary stepperRightDirConfig = new Sequencer.ChannelConfigBinary(false, false, new DigitalOutput.Spec(MOTOR_RIGHT_DIRECTION_PIN));
	final ChannelConfigBinary stepperLeftDirConfig = new Sequencer.ChannelConfigBinary(false, false, new DigitalOutput.Spec(MOTOR_LEFT_DIRECTION_PIN));
	private Sequencer.ChannelCueBinary stepperDirCue = new ChannelCueBinary();
	final ChannelConfigFmSpeed stepperRightFMspeedConfig = new ChannelConfigFmSpeed(Clock.CLK_62K5, 2, new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfigFmSpeed stepperLeftFMspeedConfig = new ChannelConfigFmSpeed(Clock.CLK_62K5, 2, new DigitalOutput.Spec(MOTOR_CLOCK_LEFT_PIN));
	final ChannelConfig[] channelConfigList = new ChannelConfig[] { stepperRightFMspeedConfig, stepperLeftFMspeedConfig };// stepperFMspeedConfig
	private Sequencer.ChannelCueFmSpeed stepperRightFMspeedCue = new ChannelCueFmSpeed();
	private Sequencer.ChannelCueFmSpeed stepperLeftFMspeedCue = new ChannelCueFmSpeed();
	private Sequencer.ChannelCue[] cueList = new Sequencer.ChannelCue[] { stepperRightFMspeedCue, stepperLeftFMspeedCue };// stepperStepCue//stepperFMspeedCue
	private int MAX_FM_SPEED_PERIOD = 60000;
	private int MIN_FM_SPEED_PERIOD = 600;

	private boolean FORWARD_RIGHT = false;
	private boolean FORWARD_LEFT = true;
	private boolean BACKWARD_RIGHT = true;
	private boolean BACKWARD_LEFT = false;

	public MainActivity main;

	public void setMain(MainActivity main) {
		this.main = main;
	}

	public VicsWagon(IOIO ioio_) {
		this.ioio_ = ioio_;
	}

	public void runRobotTest() {
		int sinePeriod = 0;
		try {
			stepperRightFMspeedCue.period = 400;
			stepperLeftFMspeedCue.period = 400;
			sequencer = ioio_.openSequencer(channelConfigList);
			sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
			sequencer.start();
			while (sequencer.available() > 0) // fill cue
			{
				{
					/* Untested */
					// for (int i = 0; i < 314; i++) {
					// sinePeriod = (int) ((MAX_FM_SPEED_PERIOD
					// * (1 + Math.cos(1 / 100)) + MIN_FM_SPEED_PERIOD));
					// stepperRightFMspeedCue.period = sinePeriod;
					// stepperLeftFMspeedCue.period = sinePeriod;
					// sequencer.push(cueList, 600);
					// }
					sequencer.push(cueList, 1000);
				}
			}

		} catch (Exception e) {
		}
	}

	public void goBackward(double speed, int duration) {
		try {
			stepperRightFMspeedCue.period = (int) (1000 / speed);
			stepperLeftFMspeedCue.period = (int) (1000 / speed);
			backwardCue(duration);
			sequencer.start();
			waitToFinish();
			sequencer.pause();
		} catch (Exception e) {
		}
	}

	private void backwardCue(int duration) throws ConnectionLostException, InterruptedException {
		setDirection(BACKWARD_LEFT, BACKWARD_RIGHT);
		sequencer.push(cueList, duration);
	}

	public void goForward(double speed, int duration) {
		try {
			stepperRightFMspeedCue.period = (int) (1000 / speed);
			stepperLeftFMspeedCue.period = (int) (1000 / speed);
			forwardCue(duration);
			sequencer.start();
			waitToFinish();
			sequencer.pause();
		} catch (Exception e) {
		}
	}

	public void goForwardAndCheckForWall(double speed, int duration, int distanceFromWall) {
		try {
			stepperRightFMspeedCue.period = (int) (1000 / speed);
			stepperLeftFMspeedCue.period = (int) (1000 / speed);
			forwardCue(duration);
			sequencer.start();
			waitToFinishOrForWall(distanceFromWall);
			sequencer.stop();
		} catch (Exception e) {
		}
	}

	public void goForwardUntilWall(double speed, int distanceFromWall) {
		try {
			stepperRightFMspeedCue.period = (int) (1000 / speed);
			stepperLeftFMspeedCue.period = (int) (1000 / speed);
			setDirection(FORWARD_LEFT, FORWARD_RIGHT);
			sequencer.manualStart(cueList);
			waitUntilFrontWall(distanceFromWall);
			sequencer.manualStop();
		} catch (Exception e) {
		}
	}

	private void forwardCue(int duration) throws ConnectionLostException, InterruptedException {
		setDirection(FORWARD_LEFT, FORWARD_RIGHT);
		sequencer.push(cueList, duration);
	}

	public void spinLeft(double speed, int duration) {
		try {
			stepperRightFMspeedCue.period = (int) (1000 / speed);
			stepperLeftFMspeedCue.period = (int) (1000 / speed);
			spinLeftCue(duration);
			sequencer.start();
			waitToFinish();
			sequencer.pause();
		} catch (Exception e) {
		}
	}

	private void spinLeftCue(int duration) throws ConnectionLostException, InterruptedException {
		setDirection(BACKWARD_LEFT, FORWARD_RIGHT);
		sequencer.push(cueList, duration);
	}

	public void spinRight(double speed, int duration) {
		try {
			stepperRightFMspeedCue.period = (int) (1000 / speed);
			stepperLeftFMspeedCue.period = (int) (1000 / speed);
			spinRightCue(duration);
			sequencer.start();
			waitToFinish();
			sequencer.pause();
		} catch (Exception e) {
		}
	}

	public void spinRightForever(double speed) {
		try {
			stepperRightFMspeedCue.period = (int) (1000 / speed);
			stepperLeftFMspeedCue.period = (int) (1000 / speed);
			setDirection(FORWARD_LEFT, BACKWARD_RIGHT);
			sequencer.manualStart(cueList);
		} catch (Exception e) {
		}
	}

	private void spinRightCue(int duration) throws ConnectionLostException, InterruptedException {
		setDirection(FORWARD_LEFT, BACKWARD_RIGHT);
		sequencer.push(cueList, duration);
	}

	private void waitToFinish() throws ConnectionLostException {
		SystemClock.sleep(100);
		// while
		// (sequencer.getLastEvent().type.equals(Sequencer.Event.Type.STALLED))
		// {
		// }
		while (!sequencer.getLastEvent().type.equals(Sequencer.Event.Type.STALLED)) {
		}
	}

	private void waitUntilFrontWall(int distanceFromWall) throws ConnectionLostException, InterruptedException {
		SystemClock.sleep(100);
		sonar.read();
		while (sonar.getFrontDistance() > distanceFromWall) {
			SystemClock.sleep(100);
			MainActivity.activity.log(String.valueOf(sonar.getFrontDistance()));
			sonar.read();
			SystemClock.sleep(100);
		}
	}

	private boolean wallInFront(int distanceFromWall) throws ConnectionLostException, InterruptedException {
		sonar.read();
		return sonar.getFrontDistance() <= distanceFromWall;
	}

	private void waitToFinishOrForWall(int distanceFromWall) throws ConnectionLostException, InterruptedException {
		while (sequencer.getLastEvent().type.equals(Sequencer.Event.Type.STALLED)) {
		}
		SystemClock.sleep(100);
		sonar.read();
		while (sonar.getFrontDistance() > distanceFromWall && !sequencer.getLastEvent().type.equals(Sequencer.Event.Type.STALLED)) {
			SystemClock.sleep(100);
			MainActivity.activity.log(String.valueOf(sonar.getFrontDistance()));
			sonar.read();
		}
	}

	private void setDirection(boolean leftDirection, boolean rightDirection) throws ConnectionLostException {
		rightMotorDirection.close();
		leftMotorDirection.close();
		rightMotorDirection = ioio_.openDigitalOutput(MOTOR_RIGHT_DIRECTION_PIN, rightDirection);
		leftMotorDirection = ioio_.openDigitalOutput(MOTOR_LEFT_DIRECTION_PIN, leftDirection);
	}

	public void goMM(int mm) throws ConnectionLostException { // Go Millimeters

		double StepsPerMM = 1.000;
		int steps = (int) Math.abs((double) (mm) * StepsPerMM);
		int StepsPerPush = 16; // magic so that the queue duration same as
								// period
		// MaxFreq based on what you motor can handle without slipping...
		double maxFreq = 1000.0; // steps per second ran at 3000 some glitches
									// 2500 was good
		double minFreq = 16.0;
		double maxDelFreq = 4.0; // steps per second ran at 5.0 some glitches 4
									// was good
		int numPushes = steps / StepsPerPush;
		if (mm >= 0) {
			rightMotorDirection.write(FORWARD_RIGHT);
			leftMotorDirection.write(FORWARD_LEFT);
		} else {
			rightMotorDirection.write(BACKWARD_RIGHT);
			leftMotorDirection.write(BACKWARD_LEFT);
		}

		double freq;
		double maxfrac;
		double delfreq;
		double duration;
		int period;
		int i;
		// log("In goSteps");
		freq = minFreq;
		try {
			sequencer.start();
			for (i = 0; i < numPushes; i++) {
				maxfrac = (maxFreq - freq) / maxFreq;
				delfreq = (maxfrac * maxfrac) * maxDelFreq;
				if (i < numPushes / 2) {
					freq += delfreq;
				} else if (i > numPushes / 2) {
					freq -= delfreq;
				}

				// log("i = " + i + " freq = " + freq + " numPushes " +
				// numPushes);

				// translate freq steps/sec into period (usec) period = 1/freq
				// 1/2000 = 0.0005 but translate to usec divide by 0.000001
				// gives 500
				// period = 1000000/freq;
				if (freq <= minFreq)
					freq = minFreq;
				period = (int) (50000.0 / freq);
				if (period < 1)
					period = 1; // limits for the period
				if (period > 65535)
					period = 65535;
				stepperRightFMspeedCue.period = period; // period is in micro
														// seconds
				stepperLeftFMspeedCue.period = period;
				duration = (20 * period / 60.0 * StepsPerPush);
				// MainActivity.activity.log("i = " + i + " freq = " + freq +
				// " period " + period + " duration " + duration);
				// second parameter in the push(cue, duration)
				// 62500 cue duration 62500 * 16us = 1s
				// using period gives 16 steps per push
				sequencer.push(cueList, (int) duration);
				if (wallInFront(1000)) {
					sequencer.stop();
					sequencer.close();
					sequencer = ioio_.openSequencer(channelConfigList);
					sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
					break;
				}
			}
			waitToFinishOrForWall(1000);
			sequencer.stop();
			sequencer.close();
			sequencer = ioio_.openSequencer(channelConfigList);
			sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			MainActivity.activity.log("EXCEPTION" + e.getMessage());
		}
		MainActivity.activity.log("Finished going forward");
	}

	public void turn(int deg) throws ConnectionLostException {
		// zero radius turn of deg degrees
		if (deg >= 0) {
			rightMotorDirection.write(FORWARD_RIGHT);
			leftMotorDirection.write(BACKWARD_LEFT);
		} else {
			rightMotorDirection.write(BACKWARD_RIGHT);
			leftMotorDirection.write(FORWARD_LEFT);
		}
		int period;
		double duration;
		int StepsPerPush = 16;
		int steps = (int) (2.75 * (double) deg);
		period = (int) (50000.0 / 30.0);
		int numPushes = steps / StepsPerPush;
		try {
			sequencer.start();
			for (int i = 0; i < numPushes; i++) {
				stepperRightFMspeedCue.period = period; // period is in micro
														// seconds
				stepperLeftFMspeedCue.period = period;
				duration = (int) ((double) 20 * period / 60.0 * (double) StepsPerPush);
				MainActivity.activity.log("Turn " + period + " duration " + duration);
				sequencer.push(cueList, (int) duration);
			}
			waitToFinish();
			sequencer.pause();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			MainActivity.activity.log("EXCEPTION" + e.getMessage());
		}
	}

	public void configureVicsWagonStandard() {
		try {
			halfFull = ioio_.openDigitalOutput(MOTOR_HALF_FULL_STEP_PIN, false);// Full
			rightMotorDirection = ioio_.openDigitalOutput(MOTOR_RIGHT_DIRECTION_PIN, true);// forward
			leftMotorDirection = ioio_.openDigitalOutput(MOTOR_LEFT_DIRECTION_PIN, false);
			motorControllerControl = ioio_.openDigitalOutput(MOTOR_CONTROLLER_CONTROL_PIN, true);// slow
			motorEnable = ioio_.openDigitalOutput(MOTOR_ENABLE_PIN, true);// enable
			motorControllerReset = ioio_.openDigitalOutput(MOTOR_RESET, true);
			motorControllerReset.write(false);
			motorControllerReset.write(true);

			setUpMotorControllerChipForWaveDrive();

			sequencer = ioio_.openSequencer(channelConfigList);
			sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
		} catch (Exception e) {
		}
	}

	/*********************************************************************************
	 * Wave drive mode (full step one phase on) A LOW level on the pin HALF/FULL
	 * input selects the full step mode. When the low level is applied when the
	 * state machine is at an EVEN numbered state the wave drive mode is
	 * selected. To enter the wave drive mode the state machine must be in an
	 * EVEN numbered state. The most direct method to select the wave drive mode
	 * is to first apply a RESET, then while keeping the HALF/FULL input high
	 * apply one pulse to the clock input then take the HALF/FULL input low.
	 * This sequence first forces the state machine to state 1. The clock pulse,
	 * with the HALF/FULL input high advances the state machine from state 1 to
	 * either state 2 or 8 depending on the CW/CCW input. Starting from this
	 * point, after each clock pulse (rising edge) will advance the state
	 * machine following the sequence 2, 4, 6, 8, etc. if CW/CCW is high
	 * (clockwise movement) or 8, 6, 4, 2, etc. if CW/CCW is low
	 * (counterclockwise movement).
	 **********************************************************************************/
	public void setUpMotorControllerChipForWaveDrive() {
		try {
			leftMotorClock = ioio_.openDigitalOutput(MOTOR_CLOCK_LEFT_PIN, true);
			rightMotorClock = ioio_.openDigitalOutput(MOTOR_CLOCK_RIGHT_PIN, true);
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

		} catch (ConnectionLostException e) {
		}
	}

	/***********************************************************************************************************
	 * A HIGH logic level on the HALF/FULL input selects Half Step Mode. At
	 * Start-Up or after a RESET the Phase Sequencer is at state 1. After each
	 * clock pulse the state changes following the sequence 1,2,3,4,5,6,7,8,…
	 * if CW/ CCW is high (Clockwise movement) or 1,8,7,6,5,4,3,2,… if CW/CCW
	 * is low (Counterclockwise movement).
	 *************************************************************************************************************/
	public void setUpMotrollerChipForHalfStepDrive() {

	}

	/***********************************************************************************************************
	 * A LOW level on the HALF/FULL input selects the Full Step mode. When the
	 * low level is applied when the state machine is at an ODD numbered state
	 * the Normal Drive Mode is selected. The Normal Drive Mode can easily be
	 * selected by holding the HALF/FULL input low and applying a RESET. AT
	 * start -up or after a RESET the State Machine is in state1. While the
	 * HALF/FULL input is kept low, state changes following the sequence
	 * 1,3,5,7,… if CW/CCW is high (Clockwise movement) or 1,7,5,3,… if
	 * CW/CCW is low (Counterclockwise movement).
	 *************************************************************************************************************/
	public void setUpMotrollerChipForFullStepDrive() {

	}

	// private void duanesCode()
	// {
	// @Override
	// protected void setup() throws ConnectionLostException {
	// sonar = new UltraSonicSensor(ioio_);
	// led = ioio_.openDigitalOutput(0, true);
	//
	// // motor setup
	// rightMotorDirection = ioio_.openDigitalOutput(
	// MOTOR_RIGHT_DIRECTION_PIN, true);
	// leftMotorDirection = ioio_.openDigitalOutput(
	// MOTOR_LEFT_DIRECTION_PIN, false);
	// motorControllerReset = ioio_.openDigitalOutput(MOTOR_RESET, true);
	// motorEnable = ioio_.openDigitalOutput(MOTOR_ENABLE_PIN, true);// enable
	// motorControllerControl = ioio_.openDigitalOutput(
	// MOTOR_CONTROLLER_CONTROL_PIN, true);// true = slow fast
	// halfFull = ioio_.openDigitalOutput(MOTOR_HALF_FULL_STEP_PIN, false);//
	// high
	// // is
	// // halfstep
	// // configure to wave drive
	// leftMotorClock = ioio_
	// .openDigitalOutput(MOTOR_CLOCK_LEFT_PIN, true);
	// rightMotorClock = ioio_.openDigitalOutput(MOTOR_CLOCK_RIGHT_PIN,
	// true);
	// motorControllerReset.write(false);
	// motorControllerReset.write(true);
	// halfFull.write(true);// Half
	// rightMotorClock.write(false);
	// rightMotorClock.write(true);
	// leftMotorClock.write(false);
	// leftMotorClock.write(true);
	// halfFull.write(false);// Full
	// leftMotorClock.close();
	// rightMotorClock.close();
	//
	// try {
	// sequencer = ioio_.openSequencer(channelConfigList);
	// sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
	//
	// // while (sequencer.available() > 0)
	// // {
	// // addCueToCueList();
	// // }
	// sequencer.start();
	//
	// } catch (Exception e) {
	// }
	// log("sequencer started .");
	// }
	//
	// @Override
	// public void loop() throws ConnectionLostException {
	// if (button.isChecked()) {
	// led.write(false);
	// try {
	// rightMotorDirection.write(true);
	// leftMotorDirection.write(false);
	// // addCueToCueList();
	// goSteps(2000);
	// Thread.sleep(4000);
	// rightMotorDirection.write(false);
	// leftMotorDirection.write(true);
	// // addCueToCueList();
	// goSteps(2000);
	// Thread.sleep(4000);
	//
	// } catch (Exception e) {
	// }
	// } else {
	// led.write(true);
	// }
	// }
	// }
	//
	// private void goSteps(int steps) {
	// int StepsPerPush = 10;
	// double maxFreq = 3000.0; // steps per second
	// double maxDelFreq = 5.0; // steps per second
	// int numPushes = steps / StepsPerPush;
	// double freq = 0.0;
	// int period;
	// int i;
	// log("In goSteps");
	// for (i = 0; i < numPushes; i++) {
	// if (i < numPushes / 2) {
	// freq += ((maxFreq - freq) / (maxFreq)) * ((maxFreq - freq) / (maxFreq)) *
	// maxDelFreq;
	// } else if (i > numPushes / 2) {
	// freq -= ((maxFreq - freq) / (maxFreq)) * ((maxFreq - freq) / (maxFreq)) *
	// maxDelFreq;
	// }
	//
	// //log("i = " + i + " freq = " + freq + " numPushes " + numPushes);
	//
	// try {
	// // translate freq steps/sec into period (usec) period = 1/freq
	// // 1/2000 = 0.0005 but translate to usec divide by 0.000001
	// // gives 500
	// // period = 1000000/freq;
	// if(freq <= 0.0) freq = 1.0;
	// period = (int) (1000000.0 / freq);
	// if(period < 0) period = 0;
	// if(period > 65535) period = 65535;
	// stepperRightFMspeedCue.period = period; // period is in micro seconds
	// stepperLeftFMspeedCue.period = period;
	// sequencer.push(cueList, 625);
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// log("EXCEPTION" + e.getMessage());
	// }
	// }
	// }
	//
	// private void addCueToCueList() {
	// int minperiod = 5000;
	// int maxperiod = 50000;
	// int frac;
	//
	// try {
	//
	// /*
	// * for(frac = 0; frac <= 10; frac ++) {
	// * stepperRightFMspeedCue.period = (minperiod * frac + maxperiod *
	// * (10 - frac)) / 10; stepperLeftFMspeedCue.period = (minperiod *
	// * frac + maxperiod * (10 - frac)) / 10; sequencer.push(cueList,
	// * 6250); // 62500 cue duration 62500 * 16us = 1s }
	// *
	// * stepperRightFMspeedCue.period = minperiod;
	// * stepperLeftFMspeedCue.period = minperiod; sequencer.push(cueList,
	// * 62500); // 62500 cue duration 62500 * 16us = 1s
	// *
	// * for(frac = 10; frac >= 0; frac --) {
	// * stepperRightFMspeedCue.period = (minperiod * frac + maxperiod *
	// * (10-frac)) / 10; stepperLeftFMspeedCue.period = (minperiod * frac
	// * + maxperiod * (10-frac)) / 10; sequencer.push(cueList, 6250); //
	// * 62500 cue duration 62500 * 16us = 1s }
	// */
	// int sineperiod;
	//
	// for (int i = 0; i < 20; i++) {
	// sineperiod = (int) ((double) maxperiod
	// * ((Math.cos((double) i / 40.0 * 6.28) + 1.0) * 0.5) + (double)
	// minperiod);
	// stepperRightFMspeedCue.period = sineperiod;
	// stepperLeftFMspeedCue.period = sineperiod;
	// sequencer.push(cueList, 6250); // 62500 cue duration 62500 *
	// // 16us = 1s
	// log("sin period = " + sineperiod);
	// }
	//
	// for (int i = 19; i >= 0; i--) {
	// sineperiod = (int) ((double) maxperiod
	// * ((Math.cos((double) i / 40.0 * 6.28) + 1.0) * 0.5) + (double)
	// minperiod);
	// stepperRightFMspeedCue.period = sineperiod;
	// stepperLeftFMspeedCue.period = sineperiod;
	// sequencer.push(cueList, 6250); // 62500 cue duration 62500 *
	// // 16us = 1s
	// log("sin period = " + sineperiod);
	// }
	// // period of 5000 give 200 steps in 1 sec
	//
	// // log("AddCueToQueue " + i++);
	// } catch (Exception e) {
	// }
	// }
	// }

}