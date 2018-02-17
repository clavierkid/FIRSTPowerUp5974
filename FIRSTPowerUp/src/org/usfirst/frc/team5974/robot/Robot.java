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

/** TODO list:
 * 
 * Encoder
 * Simulation
 * **Dashboard
 * Vision
 * Lift code
 * AI/Autonomous
 */

//import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;		//Dashboard
//import edu.wpi.first.wpilibj.Joystick;							//Controller
//import edu.wpi.first.wpilibj.Timer;								//Timer
//import edu.wpi.first.wpilibj.Spark;								//Motor Controller
//import edu.wpi.first.wpilibj.VictorSP;
//import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.*;									//everything tbh
import org.usfirst.frc.team5974.robot.ADIS16448_IMU;			//IMU
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;

import java.util.ArrayList;		//arraylist

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update th-e build.properties file in the
 * project.
 */

public class Robot extends IterativeRobot {
	Command autonomousCommand;
	SendableChooser<Object> autoChooser;
	//public static OI oi;
	

	//Also, sometimes one side is inverted. If it is, we need to change our drive code to reflect that.
	/**Note that we have something along the lines of six VictorSP motor controllers and four Sparks. Also note that the ports start at 0 not 1. - Thomas*/
	VictorSP motorRB = new VictorSP(0); //motor right back
	VictorSP motorRF = new VictorSP(1); //motor right front
	VictorSP motorLB = new VictorSP(3); //motor left back // THIS IS INVERTED; USE NEGATIVES TO GO FORWARDS
	VictorSP motorLF = new VictorSP(2); //motor left front // THIS IS INVERTED; USE NEGATIVES TO GO FORWARDS
	
	Spark motorGL = new Spark(4);
	Spark motorGR = new Spark(5);
	
	Remote remote = new Remote();
	
	ADIS16448_IMU IMU = new ADIS16448_IMU();		//imu: accelerometer and gyro
	
	
	double angleToForward = 0;
	
	double angleCache = 0;
	
	//double robotSpeed;	//robot speed (fast/slow mode)
	double GameTime;
	double forkliftHeight;
	
	int autoStep = 0; //which step of the process we're on in autonomous
	
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
	
	boolean check = false;				//this should be deleted once the tests have been conducted
	
	ArrayList avgX = new ArrayList();	//List of X accelerations to be averaged
	ArrayList avgY = new ArrayList();	//List of Y accelerations to be averaged
	ArrayList avgZ = new ArrayList();	//List of Z accelerations to be averaged
	
	int sumX = 0;	//Sum of avgX
	int sumY = 0;	//Sum of avgY
	int sumZ = 0;	//Sum of avgZ
	
	double exX = 0;	//Excess X acceleration
	double exY = 0;	//Excess X acceleration
	double exZ = 0;	//Excess X acceleration

	//time variables [see updateTimer()]
	Timer timer = new Timer();
	Timer timerTest = new Timer();
	double dT = 0; //time difference (t1-t0)
	double t0 = 0; //time start
	double t1 = 0; //time end
	double startX = posX;
	double startY = posY;
	
	//this is the variable that gives us switch and scale sides in format LRL or RRL, etc
	String gameData;
	String robotStartPosition;
	/* robot starting position
	 * L: left-most robot
	 * M: middle robot
	 * R: right-most robot
	 */
	
	double counter = 0;
	
	
	
	public void rotateTo(double angle) {		//rotates robot to angle based on IMU and d-pad
		
		angleCache = angle;
		
		//int goTo = angleCache; //lazy programming at its finest lmao //okay yeah no I'm fixing this
		//clockwise degrees to goTo angle
		double ccw = (angleCache - angleToForward < 0) ? (angleCache - angleToForward + 360) : (angleCache - angleToForward);
		
		//counter-clockwise degrees to goTo angle
		double cw = (angleToForward - angleCache < 0) ? (angleToForward - angleCache + 360) : (angleToForward - angleCache);
		
		//rotates the fastest way until within +- 5 of goTo angle
		
		if (angleCache >= angleToForward + 5 || angleCache <= angleToForward - 5) { //TODO Breaks when any button is pressed (continues spinning indefinitely)
			updateGyro();
			if (cw <= ccw) {
				updateGyro();
				motorRB.set(-0.25);
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
	
	public void moveDistance(double distance, double angle) {		// move "distance" pointing along "angle" (in respect to forward)
		double startX = posX;
		double startY = posY;
		rotateTo(angle);
		while(Math.sqrt((startX * startX) + (startY * startY)) < distance) {	
		//I thought while loops broke things? Do we need to fix this? [Yes. -Thomas]
			if (angleToForward < angle) {
				//right greater
				motorRB.set(0.25);
				motorRF.set(0.25);
				motorLB.set(-0.1);
				motorLF.set(-0.1);
			} else if (angleToForward > angle) {
				motorRB.set(0.1);
				motorRF.set(0.1);
				motorLB.set(-0.25);
				motorLF.set(-0.25);
			} else {
				motorRB.set(0.25);
				motorRF.set(0.25);
				motorLB.set(-0.25);
				motorLF.set(-0.25);
			}
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
		accelX = (IMU.getAccelX() - exX) * 9.8 * Math.cos(angleToForward * (Math.PI / 180.0)); //convert from g's
		accelY = (IMU.getAccelY() - exY) * 9.8 * Math.sin(angleToForward * (Math.PI / 180.0));
		accelZ = (IMU.getAccelZ() - exZ) * 9.8;
		
		//velocity updated by acceleration integral
		velX += accelX * dT;
		velY += accelY * dT;
		velZ += accelZ * dT;
		
		//position updated by velocity integral and adjusted for robot rotation
		posX += velX * dT;
		posY += velY * dT;
		posZ += velZ * dT;
	}
	public void calibrate(int num) { //Calibrates gyro and creates excess acceleration values
		updateGyro();
		
		avgX.clear();
		avgY.clear();
		avgZ.clear();
		
		for (int i=0; i < num; i++) {
			avgX.add(IMU.getAccelX());
			avgY.add(IMU.getAccelY());
			avgZ.add(IMU.getAccelZ());
		}
		
		for (int i=0; i < avgX.size(); i++) {
			sumX += (double)avgX.get(i);
			sumY += (double)avgY.get(i);
			sumZ += (double)avgZ.get(i);
		}
		
		exX = sumX / avgX.size();
		exY = sumY / avgY.size();
		exZ = sumZ / avgZ.size();
		
	}
	
	
	public void update() {	//updates all update functions tee
		remote.updateController();
		updateTimer();
		updateTrifecta();
		updateGyro();
		updateGameTime();
	}
	
	public void dashboardOutput() {			//sends and displays data on dashboard
		SmartDashboard.putNumber("Time Remaining", GameTime);
		SmartDashboard.putNumber("x-position", posX);
		SmartDashboard.putNumber("y-position", posY);
		SmartDashboard.putNumber("z-position", posZ);
		SmartDashboard.putNumber("x-vel", velX);
		SmartDashboard.putNumber("y-vel", velY);
		SmartDashboard.putNumber("z-vel", velZ);
		SmartDashboard.putNumber("x-accel", accelX);
		SmartDashboard.putNumber("y-accel", accelY);
		SmartDashboard.putNumber("z-accel", accelZ);
		SmartDashboard.putNumber("dT", dT);
		SmartDashboard.putNumber("Speed", velY);
		SmartDashboard.putNumber("Angle to Forwards", angleToForward);
		SmartDashboard.putNumber("Angle to Forwards Graph", angleToForward);
		SmartDashboard.putBoolean("Tank Drive Style", remote.tankDriveBool);
		SmartDashboard.putBoolean("Fast Mode", remote.fastBool);
		SmartDashboard.putNumber("Team Number", 5974);
		//SmartDashboard.putString("Switch Scale Switch", gameData);
		//Data from calibrate()
		SmartDashboard.putNumber("test x", exX * 9.8);
		SmartDashboard.putNumber("test y", exY * 9.8);
		SmartDashboard.putNumber("test z", exZ * 9.8);
	
	}
	
	public void tankDrive() {	//tank drive: left joystick controls left wheels, right joystick controls right wheels
		//right motors = right joystick y-axis
		//left motors = left joystick y-axis
		if (remote.fastBool) {
			motorRB.set(remote.joystickRYAxis);
			motorRF.set(remote.joystickRYAxis);
			motorLB.set(-remote.joystickLYAxis);
			motorLF.set(-remote.joystickLYAxis);
		} else {
			motorRB.set(remote.joystickRYAxis/2);
			motorRF.set(remote.joystickRYAxis/2);
			motorLB.set(-remote.joystickLYAxis/2);
			motorLF.set(-remote.joystickLYAxis/2);
		}
	}
	
	public void arcadeDrive() {	//arcade drive: left joystick controls all driving
		//right wheels have less power the farther right the left joystick is and more power the farther left
		//left wheels have less power the farther left the left joystick is and more power the farther right
		//X-axis input is halved
		if (remote.fastBool) {
			motorRB.set((remote.joystickLYAxis + remote.joystickLXAxis/2));
			motorRF.set((remote.joystickLYAxis + remote.joystickLXAxis/2));
			motorLB.set(-(remote.joystickLYAxis - remote.joystickLXAxis/2));
			motorLF.set(-(remote.joystickLYAxis - remote.joystickLXAxis/2));
		} else {
			motorRB.set((remote.joystickLYAxis + remote.joystickLXAxis/2)/2);
			motorRF.set((remote.joystickLYAxis + remote.joystickLXAxis/2)/2);
			motorLB.set(-(remote.joystickLYAxis - remote.joystickLXAxis/2)/2);
			motorLF.set(-(remote.joystickLYAxis - remote.joystickLXAxis/2)/2);
		}
	}
	
	/*public void grab() {	//grabbers in/out based on bumper bools  
		//move left grabber wheels
		if (remote.bumperL) {
			if (remote.grabberInBool) {
				motorGL.set(1);
			} else {
				motorGL.set(-1);
			}
		} else {
			motorGL.set(0);
		}
		
		//move right grabber wheels
		if (remote.bumperR) {
			if (remote.grabberInBool) {
				motorGR.set(-1);
			} else {
				motorGR.set(1);
			}
		} else {
			motorGR.set(0);
		}
	}*/
	
	//this function is to break in the gear box
	public void gearBoxTest(){
		if (counter < 6) {
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
		autoChooser = new SendableChooser<>();
		autoChooser.addDefault("Start Left", new leftAuto()); //This lets us choose which auto mode we're doing
		autoChooser.addObject("Start Middle", new middleAuto());
		autoChooser.addObject("Start Right", new rightAuto());
		SmartDashboard.putData("Auto choices", autoChooser);
		//Our code
		CameraServer.getInstance().startAutomaticCapture().setResolution(1200, 900); //camera
		IMU.calibrate();
		IMU.reset();
		calibrate(10);
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
		autonomousCommand = (Command) autoChooser.getSelected();
		autonomousCommand.start();
		
		String gameData = "LRL";
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
		Scheduler.getInstance().run();
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
		

	
	public void teleopInit() {
		//Rumble controller for half a second
		remote.controller.setRumble(Joystick.RumbleType.kRightRumble, 0.5);
		remote.controller.setRumble(Joystick.RumbleType.kLeftRumble, 0.5);
		Timer.delay(0.5);
		remote.controller.setRumble(Joystick.RumbleType.kRightRumble, 0);
		remote.controller.setRumble(Joystick.RumbleType.kLeftRumble, 0);
		
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
		
		if (remote.tankDriveBool) {
			tankDrive();
		} 
		else {
			arcadeDrive();
		}
		if (remote.buttonA) {
			calibrate(10);
		}
	}

	/**
	 * This function is called periodically during test mode.
	 */
	//This function is not in use. We could use it to test individual mechanisms. It functions like a second teleop. - Thomas
	@Override
	public void testPeriodic() {
		gearBoxTest();
	}	
}