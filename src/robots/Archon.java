package robots;

import static thecat.RobotPlayer.rc;
import static gamemechanics.Util.*;
import static gamemechanics.NeutralTrees.*;
import static gamemechanics.Broadcast.*;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public strictfp class Archon {

	public static int numberGardeners = 6;

	public static void run() throws GameActionException {

		// The code you want your robot to perform every round should be in this
		// loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your
			// robot to explode
			try {

				checkWinCondition();
				
				// The Archon that dose this Broadcast is the Leader
				if (rc.readBroadcast(LEADER_CHANNEL) != rc.getRoundNum()) {
					rc.broadcast(LEADER_CHANNEL, rc.getRoundNum());
					// Do Leader Stuff
					// Count Gardeners
					if (isTurnEven()) {
						rc.broadcast(GARDENER_COUNT_EVEN_TURN_CHANNEL, 0);
						rc.broadcast(GARDENER_GARDEN_EVEN_TURN_CHANNEL, 0);
					} else {
						rc.broadcast(GARDENER_COUNT_ODD_TURN_CHANNEL, 0);
						rc.broadcast(GARDENER_GARDEN_ODD_TURN_CHANNEL, 0);
					}
					System.out.println(getGardenerAliveCount());
				}
				int gardenerAlive = getGardenerAliveCount();
				if ((getGardenerGardenCount() >= gardenerAlive||rc.getRoundNum()>=200) && gardenerAlive < numberGardeners) {
					if(tryBuildGardener(getGoodGardenerDirection(), 10, 18)){
						broadcastGardenerAliveMessage();
					}
				}

				RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
				if(robotInfos.length >= 2 && rc.readBroadcast(ENEMY_LOCATION) == 0){
					rc.broadcast(ENEMY_LOCATION, encode(robotInfos[0].getLocation()));
				}
				
				shakeBulletTree();

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Archon Exception");
				e.printStackTrace();
			}
		}
	}

	private static Direction getGoodGardenerDirection() throws GameActionException {
		Direction[] directions = getDirections(16);
		for (Direction dir : directions) {
			MapLocation mapLocation = rc.getLocation().add(dir, rc.getType().bodyRadius + Gardener.GARDEN_SIZE);
			if (rc.onTheMap(mapLocation, Gardener.GARDEN_SIZE)
					&& !rc.isCircleOccupied(mapLocation, Gardener.GARDEN_SIZE)) {
				return dir;
			}
		}
		return randomDirection();
	}

	/**
	 * Try to hire a Gardener in Direction dir, while avoiding small obstacles.
	 * 
	 * @param dir
	 *            Intended Direction
	 * @return true if a Gardener was hired false otherwise
	 * @throws GameActionException
	 */
	static boolean tryBuildGardener(Direction dir) throws GameActionException {
		return tryBuildGardener(dir, 20, 3);
	}

	/**
	 * Try to hire a Gardener in Direction dir, while avoiding small obstacles.
	 * 
	 * @param dir
	 *            Intended Direction
	 * @param degreeOffset
	 *            offset if first direction is impossible
	 * @param checksPerSide
	 *            Ammount of checks on each side
	 * @return true if a Gardener was hired false otherwise
	 * @throws GameActionException
	 */
	static boolean tryBuildGardener(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
		// First, try intended direction
		if (rc.canHireGardener(dir)) {
			rc.hireGardener(dir);
			return true;
		}

		// Now try a bunch of similar angles
		int currentCheck = 1;

		while (currentCheck <= checksPerSide) {
			// Try the offset of the left side
			if (rc.canHireGardener(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
				rc.hireGardener(dir.rotateLeftDegrees(degreeOffset * currentCheck));
				return true;
			}
			// Try the offset on the right side
			if (rc.canHireGardener(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
				rc.hireGardener(dir.rotateRightDegrees(degreeOffset * currentCheck));
				return true;
			}
			// No gardener hired, try slightly further
			currentCheck++;
		}

		// A gardener was never hired, so return false.
		return false;
	}

}
