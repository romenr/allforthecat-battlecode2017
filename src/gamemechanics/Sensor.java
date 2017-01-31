package gamemechanics;

import battlecode.common.BulletInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.TreeInfo;
import thecat.RobotPlayer;

public strictfp class Sensor {

	private static RobotController rc = RobotPlayer.rc;
	private static final int NUM_ROUNDS = 2;
	private static final int SENSE_ALL = -1;
	private static int index = 0;
	private static int lasti = 0;
	private static RobotInfo[][] enemyRobotInfos = new RobotInfo[NUM_ROUNDS][];
	private static RobotInfo[][] alliedRobotInfos = new RobotInfo[NUM_ROUNDS][];
	private static TreeInfo[][] treeInfos = new TreeInfo[NUM_ROUNDS][];
	private static BulletInfo[] bulletInfos;
	private static Team allied = rc.getTeam();
	private static Team enemy = allied.opponent();

	public static void updateSensorData() {
		lasti = index;
		index = (index + 1) % NUM_ROUNDS;
		enemyRobotInfos[index] = rc.senseNearbyRobots(SENSE_ALL, enemy);
		alliedRobotInfos[index] = rc.senseNearbyRobots(SENSE_ALL, allied);
		treeInfos[index] = rc.senseNearbyTrees();
		bulletInfos = rc.senseNearbyBullets();
	}

	/**
	 * Try to Predict the next move of the enemy based on his last move Used to
	 * win these important Soldier 1v1's
	 * 
	 * @param enemy
	 *            the enemy this turn
	 * @return null if no prediction is possible the enemy location next turn
	 *         otherwise
	 */
	public static MapLocation predictEnemyMovement(RobotInfo enemy) {
		if (enemyRobotInfos[lasti] != null){
			for (RobotInfo oldData : enemyRobotInfos[lasti]) {
				if (enemy.getID() == oldData.getID()) {
					return enemy.location.add(oldData.location.directionTo(enemy.location),
							oldData.location.distanceTo(enemy.location));
				}
			}
		}
		return null;
	}

	public static void updateEnemyInfo() {
		enemyRobotInfos[index] = rc.senseNearbyRobots(SENSE_ALL, enemy);
	}

	public static TreeInfo[] getTreeInfos() {
		return treeInfos[index];
	}

	public static BulletInfo[] getBulletInfos() {
		return bulletInfos;
	}

	public static RobotInfo[] getAllied() {
		return alliedRobotInfos[index];
	}

	public static RobotInfo[] getEnemy() {
		return enemyRobotInfos[index];
	}

	public static RobotInfo[] getTeam(Team team) {
		if (team == rc.getTeam()) {
			return alliedRobotInfos[index];
		} else {
			return enemyRobotInfos[index];
		}
	}

	public static RobotInfo getAllied(int i) {
		return alliedRobotInfos[index][i];
	}

	public static RobotInfo getEnemy(int i) {
		return enemyRobotInfos[index][i];
	}

	public static TreeInfo getTree(int i) {
		return treeInfos[index][i];
	}

	public static boolean isScoutGood() throws GameActionException {
		return rc.readBroadcast(Broadcast.SCOUT_GOOD_CHANNEL) == 1;
	}

}
