/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

/*
Controls by Action:

	Arcade Drive:
		-Left joystick:
			Rotate
			Forward
			Backward
		
	Tank Drive:
		-Left joystick:
			Left wheels
				Front
				Back
		-Right joystick
			Right wheels
				Front
				Back
				
	Toggle Speed:
		-B button
	
	Toggle Drive Style: (arcade/tank)
		-X button
				
	Toggle Grabber In/Out:
		-Y button
	
	Climb:
		-Left trigger:
			Down
		-Right trigger:
			Up
		
	Grabber Wheels:
		-Left bumper:
			Spin left side
		-Right bumper:
			Spin right side
			
	Quick Turns:
		-D-pad:
			Turn to an angle relative to the field
*/

/*
 Controls by Buttons:
 
 	Left Trigger: Climb down
 	Right Trigger: Climb up
 	
 	Left Bumper: Grabber spin out
 	Right Bumper: Grabber spin in
 	
 	Left Joystick: Tank/arcade drive
 	Right Joystick: Tank drive
 	
 	D-Pad: Quick turns relative to the field
 	
 	A Button: None
 	B Button: Toggle speed
 	X Button: Toggle drive style
 	Y Button: Toggle grabber in/out
 	
 	Back Button: None
 	Select Button: None
 */

package org.usfirst.frc.team5974.robot;

/** To-do list:
 * 
 * Encoder
 * Simulation
 * Dashboard
 * Vision
 * Lift code
 * AI/Autonomous
 */

import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;		//Dashboard
import edu.wpi.first.wpilibj.Joystick;		//Controller
import edu.wpi.first.wpilibj.Timer;		//Timer
import edu.wpi.first.wpilibj.Spark;		//Motor Controller
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.*;		//everything tbh
import org.usfirst.frc.team5974.robot.ADIS16448_IMU;		//IMU
//import java.util.ArrayList;		//arraylist

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.properties file in the
 * project.
 */

public class Robot extends IterativeRobot {
	private static final String kDefaultAuto = "Default"; //any idea what these are for??
	private static final String kCustomAuto = "My Auto";
	private String m_autoSelected;
	private SendableChooser<String> m_chooser = new SendableChooser<>();
	

	//Also, sometimes one side is inverted. If it is, we need to change our drive code to reflect that.
	/**Note that we have something along the lines of six VictorSP motor controllers and four Sparks. Also note that the ports start at 0 not 1. - Thomas*/
	VictorSP motorRB = new VictorSP(0); //motor right back
	VictorSP motorRF = new VictorSP(1); //motor right front
	VictorSP motorLB = new VictorSP(3); //motor left back // THIS IS INVERTED USE NEGATIVES TO GO FORWARDS
	VictorSP motorLF = new VictorSP(2); //motor left front // THIS IS INVERTED USE NEGATIVES TO GO FORWARDS
	
	Spark motorGL = new Spark(4);
	Spark motorGR = new Spark(5);
	
	//Variables we're using
	Joystick controller = new Joystick(0);			//controller
	ADIS16448_IMU IMU = new ADIS16448_IMU();		//imu: accelerometer and gyro

	
	double joystickLXAxis;			//left joystick x-axis
	double joystickLYAxis;			//left joystick y-axis
	double joystickRXAxis;			//right joystick x-axis
	double joystickRYAxis;			//right joystick y-axis
	double triggerL;				//left trigger
	double triggerR;				//right trigger
	boolean bumperL;				//left bumper
	boolean bumperR;				//right bumper
	boolean buttonX;				//x button
	boolean buttonY;				//y button
	boolean buttonA;				//a button
	boolean buttonB;				//b button
	int dPad;					    //d-pad
	boolean joystickLPress;		    //left joystick button press
	boolean joystickRPress;		    //right joystick button press
	boolean buttonStart;			//start button
	boolean buttonBack;			    //back button
	
	/**Button ports, however, do start at 1 with one being the 'A' button. - Thomas*/
	int portButtonX = 3;
	int portButtonY = 4;
	int portButtonA = 1;
	int portButtonB = 2;
	
	int portJoystickLPress = 9;
	int portJoystickRPress = 10;
	
	int portJoystickLXAxis = 0;
	int portJoystickLYAxis = 1;
	int portJoystickRXAxis = 4;
	int portJoystickRYAxis = 5;
	
	int portTriggerL = 2;
	int portTriggerR = 3;
	int portBumperL = 5;
	int portBumperR = 6;
	
	int portButtonBack = 7;
	int portButtonStart = 8;
	
	int portDPad = 0;
	
	double angleToForward = 0;
	
	//double robotSpeed;	//robot speed (fast/slow mode)
	double GameTime;
	boolean tankDriveBool = true;	//drive mode: true = tank drive, false = arcade drive
	boolean fastBool = false;		//speed mode: true = fast mode, false = slow mode
	double forkliftHeight;
	boolean grabberInBool = true;	//grabber: true = in, false = out
	
	//position arrays
	double posX = 0;
	double posY = 0;
	double posZ = 0;
	
	//velocity arrays
	double velX = 0;
	double velY = 0;
	double velZ = 0;
	
	//acceleration arrays
	double accelX = 0;
	double accelY = 0;
	double accelZ = 0;
	
	//time variables [see updateTimer()]
	Timer timer = new Timer();
	Timer timerTest = new Timer();
	double dT = 0; //time difference (t1-t0)
	double t0 = 0; //time start
	double t1 = 0; //time end
	
	//this is the variable that gives us switch and scale sides in format LRL or RRL, etc
	String gameData;
	
	double counter = 0;
	
	public boolean checkButton(boolean button, boolean toggle, int port) {		//When the button is pushed, once it is released, its toggle is changed
		if (button) {
			toggle = !toggle;
			while (button) {
				button = controller.getRawButton(port);
			}
		}
		return toggle;
	}
	
	public void rotateTo(int goTo) {		//rotates robot to angle based on IMU and d-pad
		//clockwise degrees to goTo angle
		double cw = (goTo - angleToForward < 0) ? (goTo - angleToForward + 360) : (goTo - angleToForward);
		
		//counter-clockwise degrees to goTo angle
		double ccw = (angleToForward - goTo < 0) ? (angleToForward - goTo + 360) : (angleToForward - goTo);
		
		//rotates the fastest way until within +- 5 of goTo angle
		while (goTo >= angleToForward + 5 || goTo <= angleToForward - 5) {
			updateGyro();
			if (cw <= ccw) {
				updateGyro();
				motorRB.set(-0.25); //
				motorRF.set(-0.25);
				motorLB.set(-0.25);
				motorLF.set(-0.25);
			} else {
				updateGyro();
				motorRB.set(0.25);
				motorRF.set(0.25);
				motorLB.set(0.25);
				motorLF.set(0.25);
			}
			updateGyro();
		}
		
	}
	
	public double withIn(double input, double upperBound, double lowerBound) {		//returns the inputed value if inside the bounds. If it is past a bound, returns that bound
		if (input > 0) {
			return java.lang.Math.min(upperBound, input);
		} else if (input < 0) {
			return java.lang.Math.max(lowerBound, input);
		} else {
			return 0;
		}
	}
	
	public void joystickDeadZone() {		//sets dead zone for joysticks
		if (joystickLXAxis <= 0.1 && joystickLXAxis >= -0.1) {
			joystickLXAxis = 0;
		} if (joystickLYAxis <= 0.1 && joystickLYAxis >= -0.1) {
			joystickLYAxis = 0;
		}
		if (joystickRXAxis <= 0.1 && joystickRXAxis >= -0.1) {
			joystickRXAxis = 0;
		} if (joystickRYAxis <= 0.1 && joystickRYAxis >= -0.1) {
			joystickRYAxis = 0;
		}
	}
	
	public void updateGyro() {		//set IMU.getAngle() (-inf,inf) output to a non-looping value [0,360)
		angleToForward = IMU.getAngleZ();
		if (angleToForward >= 360) {
			angleToForward -= 360;
		} else if (angleToForward < 0) {
			angleToForward += 360;
		}
	}
	
	
	public void updateTimer() {		//sets change in time between the current running of a periodic function and the previous running
		t0 = t1;
		t1 = timer.get();
		dT = t1 - t0;
	}
	public void updateGameTime() {   //Sets time remaining in match(approximation)
		GameTime = Timer.getMatchTime();
	}
	
	public void updateTrifecta() {	//updates pos, vel, and accel
		//accel variables updated from IMU
		accelX = IMU.getAccelX() * 9.8 * Math.cos(angleToForward * (Math.PI / 180.0)); //convert from g's
		accelY = IMU.getAccelY() * 9.8 * Math.sin(angleToForward * (Math.PI / 180.0));
		accelZ = IMU.getAccelZ() * 9.8;
		
		//velocity updated by acceleration integral
		velX += accelX * dT;
		velY += accelY * dT;
		velZ += accelZ * dT;
		
		//position updated by velocity integral and adjusted for robot rotation
		posX += velX * dT;
		posY += velY * dT;
		posZ += velZ * dT;
	}
	
	public void updateController() {		//updates all controller features
		/**I don't know why you made a whole bunch of port variables when numbers are faster, but hey! You do you. - Thomas*/
		//i concur --Carter
		//should we merge this part with the 'variables we're using' section starting with Thomas' blue comment ("Button ports, however...")? --Muneo
		
		//left joystick update
		joystickLXAxis = controller.getRawAxis(portJoystickLXAxis);		//returns a value [-1,1]
		joystickLYAxis = controller.getRawAxis(portJoystickLYAxis);		//returns a value [-1,1]
		joystickLPress = controller.getRawButton(portJoystickLPress);	//returns a value {0,1}
		
		//right joystick update
		joystickRXAxis = controller.getRawAxis(portJoystickRXAxis);		//returns a value [-1,1]
		joystickRYAxis = controller.getRawAxis(portJoystickRYAxis);		//returns a value [-1,1]
		joystickRPress = controller.getRawButton(portJoystickRPress);	//returns a value {0,1}
		
		//trigger updates
		triggerL = controller.getRawAxis(portTriggerL);		//returns a value [0,1]
		triggerR = controller.getRawAxis(portTriggerR);		//returns a value [0,1]
		
		//bumper updates
		bumperL = controller.getRawButton(portBumperL);		//returns a value {0,1}
		bumperR = controller.getRawButton(portBumperR);		//returns a value {0,1}
		
		//button updates
		buttonX = controller.getRawButton(portButtonX);		//returns a value {0,1}
		buttonY = controller.getRawButton(portButtonY);		//returns a value {0,1}
		buttonA = controller.getRawButton(portButtonA);		//returns a value {0,1}
		buttonB = controller.getRawButton(portButtonB);		//returns a value {0,1}
		
		buttonBack = controller.getRawButton(portButtonBack);	//returns a value {0,1}
		buttonStart = controller.getRawButton(portButtonStart);	//returns a value {0,1}
		
		//toggle checks
		tankDriveBool = checkButton(buttonX, tankDriveBool, portButtonX);		//toggles boolean if button is pressed
		fastBool = checkButton(buttonB, fastBool, portButtonB);					//toggles boolean if button is pressed
		grabberInBool = checkButton(buttonY, grabberInBool, portButtonY);		//toggles boolean if button is pressed
		
		//d-pad/POV updates
		dPad = controller.getPOV(portDPad);		//returns a value {-1,0,45,90,135,180,225,270,315}

		//d-pad/POV turns
		if (dPad != -1) {
			dPad = 360 - dPad; //Converts the clockwise dPad rotation into a Gyro-readable counterclockwise rotation.
			rotateTo(dPad);
		}
		
		joystickDeadZone();
	}
	
	public void update() {	//updates all update functions tee
		updateController();
		updateTimer();
		updateTrifecta();
		updateGyro();
		updateGameTime();
	}
	
	public void dashboardOutput() {			//sends and displays data on dashboard
		SmartDashboard.putNumber("Time Remaining", GameTime);
		SmartDashboard.putNumber("x-position", posX);
		SmartDashboard.putNumber("y-position", posY);
		SmartDashboard.putNumber("x-vel", velX);
		SmartDashboard.putNumber("y-vel", velY);
		SmartDashboard.putNumber("x-accel", accelX);
		SmartDashboard.putNumber("y-accel", accelY);
		SmartDashboard.putNumber("dT", dT);
		SmartDashboard.putNumber("Speed", velY);
		SmartDashboard.putNumber("Angle to Forwards", angleToForward);
		SmartDashboard.putNumber("Angle to Forwards", angleToForward);
		SmartDashboard.putBoolean("Tank Drive Style", tankDriveBool);
		SmartDashboard.putBoolean("Fast Mode", fastBool);
		//SmartDashboard.putBoolean("Grabber In", grabberInBool);
	}
	
	public void tankDrive() {	//tank drive: left joystick controls left wheels, right joystick controls right wheels
		//right motors = right joystick y-axis
		//left motors = left joystick y-axis
		if (fastBool) {
			motorRB.set(joystickRYAxis);
			motorRF.set(joystickRYAxis);
			motorLB.set(-joystickLYAxis);
			motorLF.set(-joystickLYAxis);
		} else {
			motorRB.set(joystickRYAxis/2);
			motorRF.set(joystickRYAxis/2);
			motorLB.set(-joystickLYAxis/2);
			motorLF.set(-joystickLYAxis/2);
		}
	}
	
	public void arcadeDrive() {	//arcade drive: left joystick controls all driving
		//right wheels have less power the farther right the left joystick is and more power the farther left
		//left wheels have less power the farther left the left joystick is and more power the farther right
		//X-axis input is halved
		if (fastBool) {
			motorRB.set((joystickLYAxis + joystickLXAxis/2));
			motorRF.set((joystickLYAxis + joystickLXAxis/2));
			motorLB.set(-(joystickLYAxis - joystickLXAxis/2));
			motorLF.set(-(joystickLYAxis - joystickLXAxis/2));
		} else {
			motorRB.set((joystickLYAxis + joystickLXAxis/2)/2);
			motorRF.set((joystickLYAxis + joystickLXAxis/2)/2);
			motorLB.set(-(joystickLYAxis - joystickLXAxis/2)/2);
			motorLF.set(-(joystickLYAxis - joystickLXAxis/2)/2);
		}
	}
	
	public void grab() {	//grabbers in/out based on bumper bools  
		//move left grabber wheels
		if (bumperL) {
			if (grabberInBool) {
				motorGL.set(1);
			} else {
				motorGL.set(-1);
			}
		} else {
			motorGL.set(0);
		}
		
		//move right grabber wheels
		if (bumperR) {
			if (grabberInBool) {
				motorGR.set(-1);
			} else {
				motorGR.set(1);
			}
		} else {
			motorGR.set(0);
		}
	}
	
	//this function is to break in the gear box
	public void gearBoxTest(){
		while (counter < 6) {
			timerTest.start();
			if (480 >= timerTest.get()) {
				motorRB.set(1);
				motorRF.set(1);
				motorLB.set(1);
				motorLF.set(1);
			}
			else if (timerTest.get() > 480 && 600 >= timerTest.get()) {
				motorRB.set(0);
				motorRF.set(0);
				motorLB.set(0);
				motorLF.set(0);
			}
			else if (timerTest.get() > 600) {
				timerTest.reset();
				counter++;
			}
		}
	}
	
	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */
	@Override
	public void robotInit() {
		m_chooser.addDefault("Default Auto", kDefaultAuto); //We sould probably figure out what this pre-generated code does at some point - Thomas
		m_chooser.addObject("My Auto", kCustomAuto);
		SmartDashboard.putData("Auto choices", m_chooser);
		CameraServer.getInstance().startAutomaticCapture();
		//Our code
		IMU.calibrate();
		IMU.reset();
	}

	/**
	 * This autonomous (along with the chooser code above) shows how to select
	 * between different autonomous modes using the dashboard. The sendable
	 * chooser code works with the Java SmartDashboard. If you prefer the
	 * LabVIEW Dashboard, remove all of the chooser code and uncomment the
	 * getString line to get the auto name from the text box below the Gyro
	 *
	 * <p>You can add additional auto modes by adding additional comparisons to
	 * the switch structure below with additional strings. If using the
	 * SendableChooser make sure to add them to the chooser code above as well.
	 */
	@Override
	public void autonomousInit() {
		m_autoSelected = m_chooser.getSelected();
		// autoSelected = SmartDashboard.getString("Auto Selector",
		// defaultAuto);
		System.out.println("Auto selected: " + m_autoSelected);
		gameData = DriverStation.getInstance().getGameSpecificMessage(); 
		/*Gives 3 characters telling your switch and scale sides.
		 *The first one is your switch.
		 *The second is the scale.
		 *The third one is your opponent's switch
		*/
	}

	/**
	 * This function is called periodically during autonomous.
	 */
	@Override
	public void autonomousPeriodic() {
		switch (m_autoSelected) {
			case kCustomAuto:
				// Put custom auto code here
				break;
			case kDefaultAuto:
			default:
				// Put default auto code here
				break;
		}
		/*To use gameData,example
		 * if(gameData.charAt(0) == 'L')     //if alliance switch is on left side at character 0 (The first character)
		 * { //blah blahblah what to do if switch is on left yeah
		 * }
		 * else{
		 * 		//what to do if switch is on right.
		 * }
		 * Repeat for character 1 (scale) and character 2 (opponent's switch) - Thomas
		 */
	}

	
	public void teleopInit() {
		//Rumble controller for half a second
		controller.setRumble(Joystick.RumbleType.kRightRumble, 0.5);
		controller.setRumble(Joystick.RumbleType.kLeftRumble, 0.5);
		Timer.delay(0.5);
		controller.setRumble(Joystick.RumbleType.kRightRumble, 0);
		controller.setRumble(Joystick.RumbleType.kLeftRumble, 0);
		
		timer.start();
	}
	
	/**
	 * This function is called periodically during operator control.
	 */
	@Override
	public void teleopPeriodic() {
		update();
		
		//grab();
		
		//dashboard outputs
		dashboardOutput();
		
		if (tankDriveBool) {
			tankDrive();
		} 
		else {
			arcadeDrive();
		}
	}

	/**
	 * This function is called periodically during test mode.
	 */
	//This funtion is not in use. We could use it to test individual mechanisms. It functions like a second teleop. - Thomas
	@Override
	public void testPeriodic() {
		gearBoxTest();
	}	
}