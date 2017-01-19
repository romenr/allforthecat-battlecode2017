package robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;
import gamemechanics.Broadcast;

import static thecat.RobotPlayer.rc;
import static gamemechanics.Util.*;

public strictfp class Lumberjack {
	static TreeInfo best = null;
	static boolean checkFreeWay = false;

	public static void run() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		int lumberjackCount = rc.readBroadcast(Broadcast.LUMBERJACK_COUNT);
		if (lumberjackCount == 5 || lumberjackCount == 6) {
			checkFreeWay = true;
		}

		// The code you want your robot to perform every round should be in this
		// loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your
			// robot to explode
			try {
				
				checkWinCondition();

				if (!checkFreeWay) {
					if (best != null) {
						try {
							best = rc.senseTree(best.getID());
						} catch (Exception e) {
							best = null;
						}
					}

					if (best == null || best.getHealth() <= 0) {
						best = null;
						TreeInfo[] trees = rc.senseNearbyTrees();
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
				}

				// See if there are any enemy robots within striking range
				// (distance 1 from lumberjack's radius)
				RobotInfo[] robots = rc.senseNearbyRobots(
						RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS - 0.01f, enemy);
				RobotInfo[] allies = rc.senseNearbyRobots(
						RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS - 0.01f, rc.getTeam());
				if (robots.length > 0 && !rc.hasAttacked() && allies.length < robots.length) {
					// Use strike() to hit all nearby robots!
					tryMove(rc.getLocation().directionTo(robots[0].getLocation()),
							rc.getLocation().distanceTo(robots[0].getLocation()) - 0.1f);
					rc.strike();
				} else {
					// No close robots, so search for robots within sight radius
					robots = rc.senseNearbyRobots(-1, enemy);

					// If there is a robot, move towards it
					if (robots.length > 0) {
						MapLocation myLocation = rc.getLocation();
						MapLocation enemyLocation = robots[0].getLocation();
						Direction toEnemy = myLocation.directionTo(enemyLocation);
						tryMove(toEnemy);
					} else if (best == null) {
						if (!tryMove(getGeneralEnemyDirection())) {
							TreeInfo[] trees = rc.senseNearbyTrees();
							for (TreeInfo t : trees) {
								if (t.getTeam() != rc.getTeam()) {
									if (rc.canChop(t.getID())) {
										rc.chop(t.getID());
										break;
									}
								}
							}
						}

					} else {
						if (rc.canChop(best.getID())) {
							rc.chop(best.getID());
							if (best.getHealth() <= 5) {
								best = null;
							}
						} else {
							if (!tryMove(rc.getLocation().directionTo(best.getLocation()))) {
								best = null;
							}
						}
					}
				}

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Lumberjack Exception");
				e.printStackTrace();
			}
		}
	}

}
