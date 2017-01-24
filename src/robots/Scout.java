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

import static thecat.RobotPlayer.rc;
import static gamemechanics.Broadcast.ENEMY_LOCATION;
import static gamemechanics.Util.*;
import static gamemechanics.Debug.*;

public strictfp class Scout {

	static MapLocation protect = null;
	// Hope for a Positive outcome
	static boolean positive = rand.nextBoolean();
	static int increaseRadius = 0;
	static float bulletAwarnesRadius = rc.getType().bodyRadius + rc.getType().strideRadius + RobotType.TANK.bulletSpeed;
	static RobotInfo scout = null;

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
				debug_colorBulletTrees();
				RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
				if (robots.length > 0 && tryToKillEnemyScout(robots)) {
					debug_println("Fight!");
				} else if (robots.length > 0) {
					if (robots.length >= 4 && rc.readBroadcast(ENEMY_LOCATION) == 0) {
						rc.broadcast(ENEMY_LOCATION, encode(robots[0].getLocation()));
					}

					MapLocation myLocation = rc.getLocation();
					MapLocation enemyLocation = robots[0].getLocation();
					Direction toEnemy = myLocation.directionTo(enemyLocation);
					int shootAt = 0;
					switch (robots[0].type) {
					case LUMBERJACK:
						if (rc.getLocation().distanceTo(robots[0].getLocation()) < RobotType.LUMBERJACK.bodyRadius
								+ RobotType.LUMBERJACK.strideRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS
								+ rc.getType().strideRadius + 0.01f) {
							tryMove(toEnemy.opposite());
						} else {
							tryMove(toEnemy);
						}
						break;
					case GARDENER:
					case SCOUT:
					case ARCHON:
						tryMove(toEnemy);
						break;
					case SOLDIER:
					case TANK:
						dodge();
						break;
					default:
						break;
					}
					while (shootAt < robots.length) {
						Direction shootTo = rc.getLocation().directionTo(robots[shootAt].location);
						if (rc.canFirePentadShot() && canShootPentandTo(shootTo)) {
							rc.firePentadShot(shootTo);
							break;
						}
						if (rc.canFireTriadShot() && canShootTriadTo(shootTo)) {
							rc.fireTriadShot(shootTo);
							break;
						}
						if (rc.canFireSingleShot() && canShootBulletTo(shootTo)) {
							rc.fireSingleShot(shootTo);
							break;
						}
						shootAt++;
					}
				} else if (tryShakeBulletTree()) {
					debug_println("Moved to Tree");
				} else if(Math.random() < 0.5 && tryMove(protect.directionTo(rc.getLocation()))){
					debug_println("Increased radius");
				}else{
					// try to Move a Circle around protect the point
					if (increaseRadius > 0) {
						if (tryMove(rc.getLocation().directionTo(protect).opposite())) {
							debug_println("Move away from protect");
							increaseRadius++;
							if (increaseRadius == 4) {
								increaseRadius = 0;
							}
						} else {
							increaseRadius = 0;
							// Try Moving counter clock whise
							MapLocation target = moveCircleAround(protect, rc.getLocation(), positive);
							Direction toTarget = rc.getLocation().directionTo(target);
							if (tryMove(toTarget)) {
								debug_println("-Circular Move");
							} else {
								// give up
								debug_println("I give up im stuck D:");
							}
						}
					} else {
						MapLocation target = moveCircleAround(protect, rc.getLocation(), positive);
						Direction toTarget = rc.getLocation().directionTo(target);
						if (rc.canSenseLocation(target) && rc.onTheMap(target)) {
							if (tryMove(toTarget)) {
								debug_println("Circular Move");
							} else {
								// Stuck change Direction and try again
								positive = !positive;
								target = moveCircleAround(protect, rc.getLocation(), positive);
								toTarget = rc.getLocation().directionTo(target);
								if (tryMove(toTarget)) {
									debug_println("-Circular Move");
									// Try to move to protect
								} else if (tryMove(rc.getLocation().directionTo(protect))) {
									debug_println("Move to protect");
									// Try to move away from protect
								} else if (tryMove(rc.getLocation().directionTo(protect).opposite())) {
									debug_println("Move away from protect");
								} else {
									// give up
									debug_println("I give up im stuck D:");
								}
							}
						} else {
							positive = !positive;
							if (tryMove(rc.getLocation().directionTo(protect).opposite())) {
								debug_println("Move away from protect");
								increaseRadius = 1;
							} else {
								// Try Moving counter clock whise
								target = moveCircleAround(protect, rc.getLocation(), positive);
								toTarget = rc.getLocation().directionTo(target);
								if (tryMove(toTarget)) {
									debug_println("-Circular Move");
								} else {
									// give up
									debug_println("I give up im stuck D:");
								}
							}
						}
					}
				}

				// Trees after Movement
				debug_colorBulletTrees();

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
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
			TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
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
			TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
			for (TreeInfo tree : trees) {
				// Trees without Bullets are not interesting
				if (tree.getContainedBullets() > 0) {
					// If we can reach and shake the Tree just shake it
					if (rc.canShake(tree.getID())) {
						rc.shake(tree.getID());
						debug_println("Shake Tree for " + tree.getContainedBullets() + " Bullets");
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
				debug_println("Shake Tree for " + bulletTree.getContainedBullets() + " Bullets");
				bulletTree = null;
			}
		}
		return moved;
	}

	/**
	 * Color Trees within sight radius that Contain Bullets golden
	 */
	public static void debug_colorBulletTrees() {
		TreeInfo[] bulletTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
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
