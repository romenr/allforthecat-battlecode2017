package robots;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import gamemechanics.Debug;
import gamemechanics.NeutralTrees;
import gamemechanics.Sensor;
import gamemechanics.Util;

import static thecat.RobotPlayer.rc;
import static gamemechanics.Broadcast.ENEMY_LOCATION_CHANNEL;
import static gamemechanics.Util.*;

public strictfp class Soldier {

	static MapLocation goal;
	static boolean positive = rand.nextBoolean();
	static boolean handleEnemys;

	// 1v1 Dodge Triad constants Pentad seems used little and its a bother
	// looking what the enemy is doing
	static final float ALPHA = (float) Math.toRadians(GameConstants.TRIAD_SPREAD_DEGREES);
	static final float X = 2 * RobotType.SOLDIER.bodyRadius;
	static final float D = (float) (X / Math.tan(ALPHA));
	static final float SIN_ALPHA = (float) (Math.sin(ALPHA));
	static final float Q = (float) (SIN_ALPHA * Math.tan(ALPHA));
	static final float Y_OFFSET = 0.2f;
	static final float X_OFFSET = Q / 2;
	static final float DISTANCE_TO_ENEMY = D + SIN_ALPHA + Y_OFFSET*2;
	static final float SIDESTEP = 1 + 0.03f;

	static MapLocation dodgeSpot;
	static boolean dodging = false;
	static boolean stop = false;

	public static void run() throws GameActionException {

		// The code you want your robot to perform every round should be in this
		// loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your
			// robot to explode
			try {

				checkWinCondition();

				Sensor.updateSensorData();

				// See if there are any nearby enemy robots
				RobotInfo[] robots = Sensor.getEnemy();
				BulletInfo[] bulletInfos = Sensor.getBulletInfos();
				handleEnemys = false;
				// If there are some...
				if (robots.length > 0) {
					handleEnemys();
					handleEnemys = true;
				} else if (bulletInfos.length > 0 && dodge()) {
					// Dodged bullet
				} else {
					if (!rc.hasMoved()) {
						if (!Util.moveTo(Util.getGeneralEnemyLocation())) {
							if (!tryMove(getWanderMapDirection())) {
								// System.out.println("I did not Move");
							}

						}
					}
				}
				if (!handleEnemys) {
					Sensor.updateEnemyInfo();
					robots = Sensor.getEnemy();
					if (robots.length > 0) {
						handleEnemys();
					}
				}

				debug_drawPath();

				NeutralTrees.shakeBulletTree();

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Debug.debug_monitorRobotByteCodeLimit();
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

	public static void handleEnemys() throws GameActionException {
		RobotInfo[] robots = Sensor.getEnemy();
		if (rc.readBroadcast(ENEMY_LOCATION_CHANNEL) == 0) {
			rc.broadcast(ENEMY_LOCATION_CHANNEL, encode(robots[0].getLocation()));
		}

		// And we have enough bullets, and haven't attacked yet this
		// turn...
		// If there is a robot, move towards it

		MapLocation myLocation = rc.getLocation();
		MapLocation enemyLocation = robots[0].getLocation();
		Direction toEnemy = myLocation.directionTo(enemyLocation);
		int shootAt = 0;
		boolean shootMoreThanNeeded = false;
		if (!rc.hasMoved()) {
			Debug.debug_startCountingBytecode("Movement");
			switch (robots[0].type) {
			case LUMBERJACK:
				if (rc.getLocation().distanceTo(robots[0].getLocation()) < 5.6f) {
					tryMove(toEnemy.opposite());
				} else {
					tryMove(toEnemy);
				}
				break;
			case GARDENER:
			case SCOUT:
				tryMove(toEnemy);
				break;
			case SOLDIER:
				shootMoreThanNeeded = true;
			case TANK:
				if(!dodgeTriad(enemyLocation)){
					if (!Util.moveTo(Util.getGeneralEnemyLocation())) {
						if (!tryMove(getWanderMapDirection())) {
							// System.out.println("I did not Move");
						}

					}
				}
				break;
			case ARCHON:
				if (robots.length > 1) {
					tryMove(rc.getLocation().directionTo(robots[1].getLocation()));
				} else {
					tryMove(toEnemy);
				}
			default:
				break;
			}
		}
		while (shootAt < robots.length) {
			MapLocation enemysNextLocation = Sensor.predictEnemyMovement(robots[shootAt]);
			Direction shootTo;
			if(enemysNextLocation != null){
				shootTo  = rc.getLocation().directionTo(enemysNextLocation);
			}else{
				shootTo  = rc.getLocation().directionTo(robots[shootAt].location);
			}
			if (rc.canFirePentadShot() && canShootPentandTo(shootTo)) {
				rc.firePentadShot(shootTo);
				break;
			}
			if (rc.canFireTriadShot() && (canShootTriadTo(shootTo, shootMoreThanNeeded ? 0 : 1))) {
				rc.fireTriadShot(shootTo);
				break;
			}
			if (rc.canFireSingleShot() && canShootBulletTo(shootTo)) {
				rc.fireSingleShot(shootTo);
				break;
			}
			shootAt++;
		}
	}

	public static boolean dodgeTriad(MapLocation enemy) throws GameActionException {
		if (dodging) {
			if(stop){
				stop = false;
				dodging = false;
				return true;
			}
			stop = true;
			if (rc.canMove(dodgeSpot)) {
				rc.move(dodgeSpot);
				return true;
			}
			return false;
		} else {
			dodging = true;
			Direction toMe = enemy.directionTo(rc.getLocation());
			MapLocation dist = enemy.add(toMe, DISTANCE_TO_ENEMY);
			MapLocation dogeLeft = dist.add(toMe.rotateRightDegrees(90), SIDESTEP);
			MapLocation dogeRight = dist.add(toMe.rotateLeftDegrees(90), SIDESTEP);
			if(canMoveTo(dogeRight)){
				rc.move(rc.getLocation().directionTo(dogeRight), rc.getLocation().distanceTo(dogeRight) / 2);
				dodgeSpot = dogeRight;
				dodging = true;
				return true;
			}else{
				if(canMoveTo(dogeLeft)){
					rc.move(rc.getLocation().directionTo(dogeLeft), rc.getLocation().distanceTo(dogeLeft) / 2);
					dodgeSpot = dogeLeft;
					dodging = true;
					return true;
				}
			}
		}
		return false;
	}

	public static boolean canMoveTo(MapLocation location) throws GameActionException {
		if (rc.canSenseAllOfCircle(location, X) && rc.onTheMap(location, X) && rc.isCircleOccupied(location, X)) {
			if (rc.canMove(rc.getLocation().directionTo(location), rc.getLocation().distanceTo(location) / 2)) {
				return true;
			}
		}
		return false;
	}

}
