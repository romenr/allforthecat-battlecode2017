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
import gamemechanics.Debug;
import gamemechanics.Sensor;

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
	static boolean firstGardener = false;
	static boolean fistStep = false;
	
	public static void run() throws GameActionException {
		if(rc.getRoundNum() < 10){
			firstGardener = true;
		}

		// The code you want your robot to perform every round should be in this
		// loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your
			// robot to explode
			try {
				checkWinCondition();
				Sensor.updateSensorData();
				soonInGarden = false;
				turnSinceSpawn++;
				broadcastGardenerAliveMessage();
				shakeBulletTree();

				// Starting strat
				if (firstGardener && !fistStep && rc.isBuildReady()) {
					TreeInfo[] treeInfos = Sensor.getTreeInfos();
					if (treeInfos.length > 4) {
						if(tryBuildRobot(randomDirection(), RobotType.LUMBERJACK, 10, 18)){
							fistStep = true;
						}
					} else {
						if(tryBuildRobot(randomDirection(), RobotType.SOLDIER, 10, 18)){
							fistStep = true;
						}
					}
				}else{
					if (firstGardener && fistStep && rc.isBuildReady()) {
						if(tryBuildRobot(randomDirection(), RobotType.SOLDIER, 10, 18)){
							firstGardener = false;
						}
					}
				}

				if (!inGarden && isValidGardenPosition(rc.getLocation())) {
					Direction[] directions = getDirections(12);
					for (Direction dir : directions) {
						MapLocation mapLocation = rc.getLocation().add(dir, RobotType.GARDENER.strideRadius);
						if (isValidGardenPosition(mapLocation)) {
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
					if (getGardenSize() == 1 && !hasBuildSoldier) {
						if (rc.canBuildRobot(RobotType.SOLDIER, unitBuildDirection)) {
							rc.buildRobot(RobotType.SOLDIER, unitBuildDirection);
							treesSinceSoldier = 0;
							hasBuildSoldier = true;
						}
					}
					if (treesSinceSoldier < 2) {
						for (int i = 1; i < 6; i++) {
							if (rc.canPlantTree(unitBuildDirection.rotateLeftDegrees(60 * i))) {
								rc.plantTree(unitBuildDirection.rotateLeftDegrees(60 * i));
								treesSinceSoldier++;
							}
						}
					}
					rc.setIndicatorDot(rc.getLocation().add(unitBuildDirection), 255, 0, 0);
					if (getGardenSize() == 5) {
						broadcastGardenerGardenMessage();
					}
				} else {
					Direction[] directions = getDirections(12);
					for (Direction dir : directions) {
						float offset = 0.5f;
						for (int i = 1; i < 7; i++) {
							MapLocation mapLocation = rc.getLocation().add(dir, i * offset);
							if (isValidGardenPosition(mapLocation)) {
								if (tryMove(rc.getLocation().directionTo(mapLocation))) {
									soonInGarden = true;
									break;
								}
							}
						}
						if (rc.hasMoved())
							break;
					}
				}
				
				if (!inGarden && !rc.hasMoved()) {
					tryMove(randomDirection());
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
//					if (rc.readBroadcast(SCOUT_IS_BUILD_CHANNEL) == 0 && Sensor.isScoutGood() && rc.getRoundNum() >= 40) {
//						if (tryBuildRobot(unitBuildDirection, RobotType.SCOUT, 10, 18)) {
//							rc.broadcast(SCOUT_IS_BUILD_CHANNEL, 1);
//						}
//					}
					if (hasBuildSoldier || treesSinceSoldier < 2) {
						if (rc.readBroadcast(Broadcast.LUMBERJACK_COUNT_CHANNEL) < MIN_LUMBERJACKS
								&& rc.canBuildRobot(RobotType.LUMBERJACK, unitBuildDirection)) {
							rc.buildRobot(RobotType.LUMBERJACK, unitBuildDirection);
							rc.broadcast(Broadcast.LUMBERJACK_COUNT_CHANNEL, rc.readBroadcast(Broadcast.LUMBERJACK_COUNT_CHANNEL) + 1);
						}
					}
					if (rc.canBuildRobot(RobotType.SOLDIER, unitBuildDirection)) {
						rc.buildRobot(RobotType.SOLDIER, unitBuildDirection);
						treesSinceSoldier = 0;
					}
				} else {
					if (!soonInGarden) {
						Direction dir = randomDirection();
						if (rc.readBroadcast(Broadcast.LUMBERJACK_COUNT_CHANNEL) < MIN_LUMBERJACKS
								&& rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
							if (tryBuildRobot(dir, RobotType.LUMBERJACK)) {
								rc.broadcast(Broadcast.LUMBERJACK_COUNT_CHANNEL,
										rc.readBroadcast(Broadcast.LUMBERJACK_COUNT_CHANNEL) + 1);
							}
						}
					}
				}

				debug_productionInfo();

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Debug.debug_monitorRobotByteCodeLimit();
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

	// The Radius of a Hexagon Garden
	public static final float RADIUS = RobotType.GARDENER.bodyRadius + GameConstants.GENERAL_SPAWN_OFFSET
			+ 2 * GameConstants.BULLET_TREE_RADIUS;

	/**
	 * Check if location is a place where a Garden can be build
	 * 
	 * @param location
	 *            the map Location
	 * @return
	 * @throws GameActionException
	 */
	public static boolean isValidGardenPosition(MapLocation location) throws GameActionException {
		return rc.canSenseAllOfCircle(location, RADIUS) && rc.onTheMap(location, RADIUS)
				&& !rc.isCircleOccupiedExceptByThisRobot(location, RADIUS);
	}

	public static TreeInfo[] garden;
	public static int updated = -1;
	
	public static int getGardenSize(){
		if(updated != rc.getRoundNum()){
			garden = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 2 * GameConstants.GENERAL_SPAWN_OFFSET,
					rc.getTeam());
			updated = rc.getRoundNum();
		}
		return garden.length;
	}

}
