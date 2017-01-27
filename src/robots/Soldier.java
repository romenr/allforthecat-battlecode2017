package robots;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import gamemechanics.Debug;
import gamemechanics.NeutralTrees;
import gamemechanics.Sensor;
import gamemechanics.Util;

import static thecat.RobotPlayer.rc;
import static gamemechanics.Broadcast.ENEMY_LOCATION;
import static gamemechanics.Util.*;

public strictfp class Soldier {

	static MapLocation goal;
	static boolean positive = rand.nextBoolean();

	public static void run() throws GameActionException {
		Team enemy = rc.getTeam().opponent();

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

				// If there are some...
				if (robots.length > 0) {
					if (rc.readBroadcast(ENEMY_LOCATION) == 0) {
						rc.broadcast(ENEMY_LOCATION, encode(robots[0].getLocation()));
					}

					// And we have enough bullets, and haven't attacked yet this
					// turn...
					// If there is a robot, move towards it

					MapLocation myLocation = rc.getLocation();
					MapLocation enemyLocation = robots[0].getLocation();
					Direction toEnemy = myLocation.directionTo(enemyLocation);
					int shootAt = 0;
					boolean shootMoreThanNeeded = false;

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
						if (rc.getLocation().distanceTo(robots[0].getLocation()) > 5.9) {
							tryMove(toEnemy);
						} else {
							if (!dodge()) {
								if(!tryMove(toEnemy.rotateLeftDegrees(135))){
									tryMove(toEnemy.rotateRightDegrees(135));
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
					while (shootAt < robots.length) {
						Direction shootTo = rc.getLocation().directionTo(robots[shootAt].location);
						if (rc.canFireTriadShot() && (canShootTriadTo(shootTo, shootMoreThanNeeded?0:1))) {
							rc.fireTriadShot(shootTo);
							break;
						}
						if (rc.canFireSingleShot() && canShootBulletTo(shootTo)) {
							rc.fireSingleShot(shootTo);
							break;
						}
						shootAt++;
					}
				} else if (bulletInfos.length > 0 && dodge()) {
					// Dodged bullet
				} else {
					if (!Util.moveToTarget(Util.getGeneralEnemyLocation())) {
						if (!tryMove(getWanderMapDirection())) {
							System.out.println("I did not Move");
						}

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

}
