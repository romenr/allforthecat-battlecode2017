package robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;
import gamemechanics.Debug;
import gamemechanics.Sensor;

import static thecat.RobotPlayer.rc;
import static gamemechanics.Util.*;
import static gamemechanics.Debug.*;
import static gamemechanics.NeutralTrees.shakeBulletTree;

public strictfp class Scout {

	static MapLocation protect = null;
	// Hope for a Positive outcome
	static boolean positive = rand.nextBoolean();
	static int increaseRadius = 0;
	static float bulletAwarnesRadius = rc.getType().bodyRadius + rc.getType().strideRadius + RobotType.TANK.bulletSpeed;
	static RobotInfo scout = null;
	private static int wanderTime;
	private static Direction explore;
	private static MapLocation exploreLocation;

	public static void run() {
		protect = getNearestInitialArchonLocation(rc.getTeam());
		// The code you want your robot to perform every round should be in this
		// loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your
			// robot to explode
			try {
				checkWinCondition();

				// Trees before Movement
				Sensor.updateSensorData();
				debug_colorBulletTrees();
				
				RobotInfo[] robots = Sensor.getEnemy();
				if(robots.length > 0 && robots[0].location.distanceTo(rc.getLocation()) < 8){
					tryMove(robots[0].location.directionTo(rc.getLocation()));
				}else if (tryShakeBulletTree()) {

				} else {
					wanderTime++;
					// Don't try to long
					if (wanderTime > 20) {
						exploreLocation = null;
					}
					// Search enemy's
					if (exploreLocation == null) {
						explore = randomDirection();
						exploreLocation = rc.getLocation().add(explore, 20);
						wanderTime = 0;
					}
					tryMove(rc.getLocation().directionTo(exploreLocation));
				}
				shakeBulletTree();

				// Trees after Movement
				debug_colorBulletTrees();

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Debug.debug_monitorRobotByteCodeLimit();
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Scout Exception");
				e.printStackTrace();
			}
		}
	}

	static TreeInfo bulletTree = null;
	static boolean updateBulletTreeInfoSucessfull = false;

	/**
	 * Search for Bullet Trees Move to the one With the most Bullets Shake trees
	 * with Bullets that come in range
	 * 
	 * @return true if the Robot moved towards a Tree, false otherwise
	 * @throws GameActionException
	 */
	public static boolean tryShakeBulletTree() throws GameActionException {
		// If there is already a Interesting tree update the Tree Info
		if (bulletTree != null) {
			updateBulletTreeInfoSucessfull = false;
			TreeInfo[] trees = Sensor.getTreeInfos();
			for (TreeInfo tree : trees) {
				if (tree.getID() == bulletTree.getID()) {
					bulletTree = tree;
					updateBulletTreeInfoSucessfull = true;
					break;
				}
			}
			// The Tree is not interesting if he is gone or already shaken
			if (!updateBulletTreeInfoSucessfull || bulletTree.getContainedBullets() == 0) {
				bulletTree = null;
			}
		}

		// If there is no bulletTree search for one
		if (bulletTree == null) {
			TreeInfo[] trees = Sensor.getTreeInfos();
			for (TreeInfo tree : trees) {
				// Trees without Bullets are not interesting
				if (tree.getContainedBullets() > 0) {
					// If we can reach and shake the Tree just shake it
					if (rc.canShake(tree.getID())) {
						rc.shake(tree.getID());
						continue;
					}
					// Write up the best Tree
					if (bulletTree == null || bulletTree.getContainedBullets() < tree.getContainedBullets()) {
						bulletTree = tree;
					}
				}
			}
		}

		boolean moved = false;
		// Move towards BulletTree and shake him if possible
		if (bulletTree != null && !rc.hasMoved()) {
			moved = tryMove(rc.getLocation().directionTo(bulletTree.getLocation()));
			if (rc.canShake(bulletTree.getID())) {
				rc.shake(bulletTree.getID());
				bulletTree = null;
			}
		}
		return moved;
	}

	/**
	 * Color Trees within sight radius that Contain Bullets golden
	 */
	public static void debug_colorBulletTrees() {
		TreeInfo[] bulletTrees = Sensor.getTreeInfos();
		for (TreeInfo tree : bulletTrees) {
			if (tree.getContainedBullets() > 0) {
				rc.setIndicatorDot(tree.getLocation(), 255, 215, 0);
			}
		}
	}

	static RobotInfo enemy = null;
	static boolean updateRobotInfoSucessfull = false;

	/**
	 * Kill em scouts
	 * 
	 * @param enemys
	 * @return
	 * @throws GameActionException
	 */
	public static boolean tryToKillEnemyScout(RobotInfo[] enemys) throws GameActionException {
		// If there is already a Interesting enemy update the Robot Info
		if (enemy != null) {
			updateRobotInfoSucessfull = false;
			for (RobotInfo robot : enemys) {
				if (robot.getID() == enemy.getID()) {
					enemy = robot;
					updateRobotInfoSucessfull = true;
					break;
				}
			}
			// The Robot is not interesting if he is gone
			if (!updateRobotInfoSucessfull) {
				enemy = null;
			}
		}

		// If there is no interesting enemy search for one
		if (enemy == null) {
			for (RobotInfo robot : enemys) {
				if (robot.getType() == RobotType.SCOUT) {
					enemy = robot;
					break;
				}
			}
		}

		if (enemy != null) {
			float dist = rc.getLocation().distanceTo(enemy.getLocation());
			Direction dir = rc.getLocation().directionTo(enemy.getLocation());
			if (dist - enemy.type.bodyRadius < rc.getType().strideRadius) {
				if (tryMove(dir, dist - enemy.type.bodyRadius - GameConstants.BULLET_SPAWN_OFFSET / 2)) {

				}
			} else {
				tryMove(dir);
			}
			if (rc.canFireSingleShot()) {
				// ...Then fire a bullet in the direction of the enemy.
				rc.fireSingleShot(rc.getLocation().directionTo(enemy.location));
			}
			return true;
		}

		return false;

	}

}
