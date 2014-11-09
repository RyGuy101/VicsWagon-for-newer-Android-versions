package ioio.examples.hello;

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

public class VicsWagon
{
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
	private DigitalOutput rightMotorClockPulse;
	private DigitalOutput leftMotorClockPulse;
	private DigitalOutput motorEnable; // Both motors
	private DigitalOutput rightMotorClock; // Step right motor
	private DigitalOutput leftMotorClock; // Step left motor
	private DigitalOutput motorControllerReset;
	private DigitalOutput rightMotorDirection;
	private DigitalOutput leftMotorDirection;
	private DigitalOutput motorControllerControl;// Decay mode high => slow
	private DigitalOutput halfFull;// High => half step
	private DigitalOutput reset; // Must be true for motors to run.
	final ChannelConfigSteps stepperStepConfig = new ChannelConfigSteps(new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfigFmSpeed stepperRightFMspeedConfig = new ChannelConfigFmSpeed(Clock.CLK_62K5, 2, new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
	final ChannelConfigFmSpeed stepperLeftFMspeedConfig = new ChannelConfigFmSpeed(Clock.CLK_62K5, 2, new DigitalOutput.Spec(MOTOR_CLOCK_LEFT_PIN));
	final ChannelConfigBinary stepperRightDirConfig = new Sequencer.ChannelConfigBinary(false, false, new DigitalOutput.Spec(MOTOR_RIGHT_DIRECTION_PIN));
	final ChannelConfigBinary stepperLeftDirConfig = new Sequencer.ChannelConfigBinary(false, false, new DigitalOutput.Spec(MOTOR_LEFT_DIRECTION_PIN));
	final ChannelConfig[] channelConfigList = new ChannelConfig[] { stepperRightFMspeedConfig, stepperLeftFMspeedConfig };// stepperFMspeedConfig//stepperStepConfig
	private Sequencer sequencer;
	private Sequencer.ChannelCueBinary stepperDirCue = new ChannelCueBinary();
	private Sequencer.ChannelCueFmSpeed stepperRightFMspeedCue = new ChannelCueFmSpeed();
	private Sequencer.ChannelCueFmSpeed stepperLeftFMspeedCue = new ChannelCueFmSpeed();
	private Sequencer.ChannelCue[] cueList = new Sequencer.ChannelCue[] { stepperRightFMspeedCue, stepperLeftFMspeedCue };// stepperStepCue//stepperFMspeedCue
	
	public VicsWagon(IOIO ioio_)
	{
		this.ioio_ = ioio_;
	}
	public void configureVicsWagonStandard()
	{
		try
		{
			rightMotorDirection = ioio_.openDigitalOutput(MOTOR_RIGHT_DIRECTION_PIN, true);
			leftMotorDirection = ioio_.openDigitalOutput(MOTOR_LEFT_DIRECTION_PIN, false);
			motorControllerReset = ioio_.openDigitalOutput(MOTOR_RESET, true);
			motorEnable = ioio_.openDigitalOutput(MOTOR_ENABLE_PIN, true);// enable
			motorControllerControl = ioio_.openDigitalOutput(MOTOR_CONTROLLER_CONTROL_PIN, true);// slow
			halfFull = ioio_.openDigitalOutput(MOTOR_HALF_FULL_STEP_PIN, false);// Full
																				// step
			sequencer = ioio_.openSequencer(channelConfigList);
			sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
			stepperRightFMspeedCue.period = 600;
			stepperLeftFMspeedCue.period = 600;
			while (sequencer.available() > 0) // fill cue 
			{
				{
					sequencer.push(cueList, 60000);
					//log("step");
				}
			}

			sequencer.start();

		} catch (Exception e)
		{
		}
	}
}
