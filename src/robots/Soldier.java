package robots;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import gamemechanics.Broadcast;
import gamemechanics.Debug;
import gamemechanics.NeutralTrees;
import gamemechanics.Sensor;
import gamemechanics.Util;

import static thecat.RobotPlayer.rc;
import static gamemechanics.Util.*;

public strictfp class Soldier {

	static MapLocation goal;
	static boolean positive = rand.nextBoolean();
	static boolean handleEnemys;

	// 1v1 Dodge
	static float x;
	static float d;
	static float sinAlpha;
	static float Y_OFFSET;
	static float distanceToEnemy;
	static float sideStep;

	static MapLocation dodgeSpot;
	static boolean dodging = false;
	static boolean stop = false;

	static boolean shootWithAngle = false;

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
				} else if (bulletInfos.length > 0 && (dodge(null) || Util.dodge())) {
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
		Broadcast.broadcastEnemySeen();
		shootWithAngle = false;

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
				if (rc.getLocation().distanceTo(enemyLocation) < 3.9f) {
					shootWithAngle = true;
				}
				if (!dodge(robots[0])) {
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
			float distance = rc.getLocation().distanceTo(robots[shootAt].location);
			MapLocation enemysNextLocation;
			if(distance <= 5){
				enemysNextLocation = Sensor.predictEnemyMovement(robots[shootAt],1);
			}else{
				enemysNextLocation = Sensor.predictEnemyMovement(robots[shootAt],2);
			}
			Direction shootTo;
			if (enemysNextLocation != null) {
				shootTo = rc.getLocation().directionTo(enemysNextLocation);
			} else {
				shootTo = rc.getLocation().directionTo(robots[shootAt].location);
			}
			if (shootWithAngle) {
				shootWithAngle = false;
				Direction left = shootTo.rotateLeftDegrees(10);
				if (shoot(left, shootMoreThanNeeded)) {
					break;
				}
				Direction right = shootTo.rotateRightDegrees(10);
				if (shoot(right, shootMoreThanNeeded)) {
					break;
				}
			}
			if (shoot(shootTo, shootMoreThanNeeded)) {
				break;
			}
			shootAt++;
		}
	}

	public static boolean shoot(Direction shootTo, boolean shootMoreThanNeeded) throws GameActionException {
		if (rc.canFirePentadShot() && canShootPentandTo(shootTo)) {
			rc.firePentadShot(shootTo);
			return true;
		}
		if (rc.canFireTriadShot() && (canShootTriadTo(shootTo, shootMoreThanNeeded ? 0 : 1))) {
			rc.fireTriadShot(shootTo);
			return true;
		}
		if (rc.canFireSingleShot() && canShootBulletTo(shootTo)) {
			rc.fireSingleShot(shootTo);
			return true;
		}
		return false;
	}

	public static boolean dodge(RobotInfo enemy) throws GameActionException {
		if (dodging) {
			if (stop) {
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
		} else if (enemy != null) {
			if (doseEnemyUsePentad(enemy)) {
				calculateDodgeVariables((float) Math.toRadians(GameConstants.PENTAD_SPREAD_DEGREES));
			} else {
				calculateDodgeVariables((float) Math.toRadians(GameConstants.TRIAD_SPREAD_DEGREES));
			}

			Direction toMe = enemy.location.directionTo(rc.getLocation());
			MapLocation dist = enemy.location.add(toMe, distanceToEnemy);
			MapLocation dogeLeft = dist.add(toMe.rotateRightDegrees(90), sideStep);
			MapLocation dogeRight = dist.add(toMe.rotateLeftDegrees(90), sideStep);
			if (canMoveTo(dogeRight)) {
				rc.move(rc.getLocation().directionTo(dogeRight), rc.getLocation().distanceTo(dogeRight) / 2);
				dodgeSpot = dogeRight;
				dodging = true;
				return true;
			} else {
				if (canMoveTo(dogeLeft)) {
					rc.move(rc.getLocation().directionTo(dogeLeft), rc.getLocation().distanceTo(dogeLeft) / 2);
					dodgeSpot = dogeLeft;
					dodging = true;
					return true;
				}
			}
		}
		return false;
	}

	private static boolean doseEnemyUsePentad(RobotInfo enemy) {
		float r = enemy.type.bodyRadius + GameConstants.BULLET_SPAWN_OFFSET + 0.01f;
		return rc.senseNearbyBullets(enemy.location, r).length == 5;
	}

	public static boolean canMoveTo(MapLocation location) throws GameActionException {
		if (rc.canSenseAllOfCircle(location, x) && rc.onTheMap(location, x) && rc.isCircleOccupied(location, x)) {
			if (rc.canMove(rc.getLocation().directionTo(location), rc.getLocation().distanceTo(location) / 2)) {
				return true;
			}
		}
		return false;
	}

	public static void calculateDodgeVariables(float alpha) {
		x = 2 * RobotType.SOLDIER.bodyRadius;
		d = (float) (x / Math.tan(alpha));
		sinAlpha = (float) (Math.sin(alpha));
		Y_OFFSET = 0.2f;
		distanceToEnemy = d + sinAlpha + Y_OFFSET;
		sideStep = 1.03f;
	}

}
