package robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;
import gamemechanics.Broadcast;

import static gamemechanics.Broadcast.*;
import static thecat.RobotPlayer.rc;
import static gamemechanics.Util.*;
import static gamemechanics.NeutralTrees.*;
import static gamemechanics.Debug.*;

public strictfp class Gardener {

	static int turnSinceSpawn = 0;
	static RobotInfo archon = null;
	static boolean inGarden = false;
	static Direction unitBuildDirection = null;
	static final float GARDEN_SIZE = 3 + 4 * GameConstants.GENERAL_SPAWN_OFFSET;
	public static final int MIN_LUMBERJACKS = 2;
	public static final int MAX_LUMBERJACKS = 3 * MIN_LUMBERJACKS;
	static int treesSinceSoldier = 0;
	static int defenseScouts = 0;
	static boolean hasBuildSoldier = false;
	static boolean soonInGarden = false;

	public static void run() throws GameActionException {

		// The code you want your robot to perform every round should be in this
		// loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your
			// robot to explode
			try {
				checkWinCondition();
				
				soonInGarden = false;
				turnSinceSpawn++;
				broadcastGardenerAliveMessage();
				shakeBulletTree();
				if(defenseScouts < 2){
					RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
					if(robotInfos.length >= 2 && rc.readBroadcast(ENEMY_LOCATION) == 0){
						rc.broadcast(ENEMY_LOCATION, encode(robotInfos[0].getLocation()));
					}
					for(RobotInfo robot: robotInfos){
						if(robot.getType() == RobotType.SCOUT){
							if(unitBuildDirection != null){
								if (tryBuildRobot(unitBuildDirection, RobotType.SCOUT, 10, 18)) {
									defenseScouts++;
								}
							}else if (tryBuildRobot(randomDirection(), RobotType.SCOUT, 10, 18)) {
								defenseScouts++;
							}
							break;
						}
					}
				}

				if (!inGarden && rc.onTheMap(rc.getLocation(), GARDEN_SIZE)
						&& !rc.isCircleOccupiedExceptByThisRobot(rc.getLocation(), GARDEN_SIZE)) {
					Direction[] directions = getDirections(12);
					for (Direction dir : directions) {
						MapLocation mapLocation = rc.getLocation().add(dir, RobotType.GARDENER.strideRadius);
						if (rc.onTheMap(mapLocation, GARDEN_SIZE)
								&& !rc.isCircleOccupiedExceptByThisRobot(mapLocation, GARDEN_SIZE)) {
							if (tryMove(rc.getLocation().directionTo(mapLocation))) {
								break;
							}
						}
					}
					inGarden = true;
					unitBuildDirection = rc.getLocation()
							.directionTo(getNearestInitialArchonLocation(rc.getTeam().opponent()));
				}

				if (inGarden) {
					if (rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 2 * GameConstants.GENERAL_SPAWN_OFFSET,
							rc.getTeam()).length == 1 && !hasBuildSoldier) {
						if (rc.canBuildRobot(RobotType.SOLDIER, unitBuildDirection)) {
							rc.buildRobot(RobotType.SOLDIER, unitBuildDirection);
							treesSinceSoldier = 0;
							hasBuildSoldier = true;
						}
					}
					if(treesSinceSoldier < 2){
						for (int i = 1; i < 6; i++) {
							if (rc.canPlantTree(unitBuildDirection.rotateLeftDegrees(60 * i))) {
								rc.plantTree(unitBuildDirection.rotateLeftDegrees(60 * i));
								treesSinceSoldier++;
							}
						}
					}
					rc.setIndicatorDot(rc.getLocation().add(unitBuildDirection), 255, 0, 0);
					if (rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 2 * GameConstants.GENERAL_SPAWN_OFFSET,
							rc.getTeam()).length >= 3) {
						broadcastGardenerGardenMessage();
					}
				} else {
					Direction[] directions = getDirections(12);
					for (Direction dir : directions) {
						float offset = 0.5f;
						for(int i = 1; i < 7; i++){
							MapLocation mapLocation = rc.getLocation().add(dir, i*offset);
							if (rc.onTheMap(mapLocation, GARDEN_SIZE)
									&& !rc.isCircleOccupiedExceptByThisRobot(mapLocation, GARDEN_SIZE)) {
								if (tryMove(rc.getLocation().directionTo(mapLocation))) {
									soonInGarden = true;
									break;
								}
							}
						}
						if(rc.hasMoved())
							break;
					}
					if (!rc.hasMoved()) {
						tryMove(randomDirection());
					}
				}

				TreeInfo[] trees = rc.senseNearbyTrees(-1, rc.getTeam());
				TreeInfo lowest = null;
				for (TreeInfo t : trees) {
					if (rc.canWater(t.getID())) {
						if (lowest == null || lowest.getHealth() > t.getHealth()) {
							lowest = t;
						}
					}
				}
				if (lowest != null) {
					rc.water(lowest.getID());
				}

				if (unitBuildDirection != null) {
					if (rc.readBroadcast(SCOUT_IS_BUILD_CHANNEL) == 0) {
						if (tryBuildRobot(unitBuildDirection, RobotType.SCOUT, 10, 18)) {
							rc.broadcast(SCOUT_IS_BUILD_CHANNEL, 1);
						}
					}
					if(hasBuildSoldier || treesSinceSoldier < 2){
						if (rc.readBroadcast(Broadcast.LUMBERJACK_COUNT) < MIN_LUMBERJACKS
								&& rc.canBuildRobot(RobotType.LUMBERJACK, unitBuildDirection)) {
							rc.buildRobot(RobotType.LUMBERJACK, unitBuildDirection);
							rc.broadcast(Broadcast.LUMBERJACK_COUNT, rc.readBroadcast(Broadcast.LUMBERJACK_COUNT) + 1);
						}
					}					
					if (rc.canBuildRobot(RobotType.SOLDIER, unitBuildDirection)) {
						rc.buildRobot(RobotType.SOLDIER, unitBuildDirection);
						treesSinceSoldier = 0;
					}
				}else{
					if(!soonInGarden){
						Direction dir = randomDirection();
						if (rc.readBroadcast(Broadcast.LUMBERJACK_COUNT) < MIN_LUMBERJACKS
								&& rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
							if(tryBuildRobot(dir, RobotType.LUMBERJACK)){
								rc.broadcast(Broadcast.LUMBERJACK_COUNT, rc.readBroadcast(Broadcast.LUMBERJACK_COUNT) + 1);
							}
						}
					}
				}
				
				debug_productionInfo();

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Gardener Exception");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Try to build a Robot in Direction dir, while avoiding small obstacles.
	 * 
	 * @param dir
	 *            Intended Direction
	 * @return true if a Robot was build false otherwise
	 * @throws GameActionException
	 */
	public static boolean tryBuildRobot(Direction dir, RobotType robotType) throws GameActionException {
		return tryBuildRobot(dir, robotType, 20, 3);
	}

	/**
	 * Try to build a Robot in Direction dir, while avoiding small obstacles.
	 * 
	 * @param dir
	 *            Intended Direction
	 * @param RobotType
	 *            the Type of the Robot
	 * @param degreeOffset
	 *            offset if first direction is impossible
	 * @param checksPerSide
	 *            Amount of checks on each side
	 * @return true if a Gardener was hired false otherwise
	 * @throws GameActionException
	 */
	public static boolean tryBuildRobot(Direction dir, RobotType robotType, float degreeOffset, int checksPerSide)
			throws GameActionException {
		// First, try intended direction
		if (rc.canBuildRobot(robotType, dir)) {
			rc.buildRobot(robotType, dir);
			return true;
		}

		// Now try a bunch of similar angles
		int currentCheck = 1;

		while (currentCheck <= checksPerSide) {
			// Try the offset of the left side
			if (rc.canBuildRobot(robotType, dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
				rc.buildRobot(robotType, dir.rotateLeftDegrees(degreeOffset * currentCheck));
				return true;
			}
			// Try the offset on the right side
			if (rc.canBuildRobot(robotType, dir.rotateRightDegrees(degreeOffset * currentCheck))) {
				rc.buildRobot(robotType, dir.rotateRightDegrees(degreeOffset * currentCheck));
				return true;
			}
			// No gardener hired, try slightly further
			currentCheck++;
		}

		// A gardener was never hired, so return false.
		return false;
	}

}
