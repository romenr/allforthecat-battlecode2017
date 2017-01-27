package robots;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.TreeInfo;
import gamemechanics.Debug;
import gamemechanics.Sensor;
import gamemechanics.Util;

import static thecat.RobotPlayer.rc;
import static gamemechanics.Util.*;

public strictfp class Lumberjack {
	static TreeInfo best = null;
	static boolean checkFreeWay = false;

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

				// Update the best tree
				// if we cant sense it set it to null
				if (best != null) {
					TreeInfo[] trees = Sensor.getTreeInfos();
					int id = best.getID();
					best = null;
					for (TreeInfo t : trees) {
						if(t.getID() == id){
							best = t;
							break;
						}
					}
				}

				// Search a new best tree
				if (best == null) {
					best = null;
					TreeInfo[] trees = Sensor.getTreeInfos();
					for (TreeInfo t : trees) {
						if (t.getTeam() != rc.getTeam()) {
							if (t.getContainedRobot() != null) {
								best = t;
								break;
							}
							if (best == null) {
								best = t;
							}
						}
					}
				}

				// See if there are any enemy robots within striking range
				// (distance 1 from lumberjack's radius)
				RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
				RobotInfo[] allies = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam());
				if (robots.length > 0 && !rc.hasAttacked()) {
					// Use strike() to hit all nearby robots!
					if(allies.length > 0){
						// Move away from allie
						tryMove(allies[0].location.directionTo(rc.getLocation()));
					}else{
						dodge();
					}
					rc.strike();
				} else {
					// No close robots, so search for robots within sight radius
					robots = Sensor.getEnemy();

					// If there is a robot, move towards it
					if (robots.length > 0) {
						MapLocation enemyLocation = robots[0].getLocation();

						switch (robots[0].type) {
						case LUMBERJACK:
						case GARDENER:
						case SCOUT:
						case ARCHON:
						case SOLDIER:
							Util.moveToTarget(enemyLocation);
							break;
						case TANK:
							// runaway aaaaa
							tryMove(enemyLocation.directionTo(rc.getLocation()));
							break;
						default:
							break;
						}
						
					} else if (best == null) {
						if (!Util.moveToTarget(getGeneralEnemyLocation())) {
						}

					} else {
						if (rc.canChop(best.getID())) {
							rc.chop(best.getID());
						} else {
							Util.moveToTarget(best.getLocation());
						}
					}
				}
				if(!rc.hasAttacked()){
					TreeInfo[] trees = Sensor.getTreeInfos();
					for (TreeInfo t : trees) {
						if (t.getTeam() != rc.getTeam()) {
							if (rc.canChop(t.getID())) {
								rc.chop(t.getID());
								break;
							}
						}
					}
				}

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Debug.debug_monitorRobotByteCodeLimit();
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Lumberjack Exception");
				e.printStackTrace();
			}
		}
	}

}
