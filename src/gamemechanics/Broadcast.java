package gamemechanics;

import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import static thecat.RobotPlayer.rc;
import static gamemechanics.Util.isTurnEven;

public strictfp class Broadcast {
	
	/**
	 * Broadcast Channel Constants
	 */
	public static final int LEADER_CHANNEL = 0;
	public static final int GARDENER_COUNT_EVEN_TURN_CHANNEL = 1;
	public static final int GARDENER_COUNT_ODD_TURN_CHANNEL = 2;
	public static final int SCOUT_IS_BUILD_CHANNEL = 3;
	public static final int LUMBERJACK_COUNT = 4;
	public static final int GARDENER_GARDEN_EVEN_TURN_CHANNEL = 5;
	public static final int GARDENER_GARDEN_ODD_TURN_CHANNEL = 6;
	public static final int ENEMY_LOCATION = 7;
	
	/**
	 * Returns the Number of Gardeners who where alive last Turn.
	 * 
	 * @return number of living Gardeners
	 * @throws GameActionException
	 */
	public static int getGardenerAliveCount() throws GameActionException {
		if (isTurnEven()) {
			return rc.readBroadcast(GARDENER_COUNT_ODD_TURN_CHANNEL);
		} else {
			return rc.readBroadcast(GARDENER_COUNT_EVEN_TURN_CHANNEL);
		}
	}

	/**
	 * Only Called by Gardeners. Broadcast that this Gardener is alive
	 * 
	 * @throws GameActionException
	 */
	public static void broadcastGardenerAliveMessage() throws GameActionException {
		if (rc.getType() != RobotType.GARDENER)
			return;
		if (isTurnEven()) {
			rc.broadcast(GARDENER_COUNT_EVEN_TURN_CHANNEL, rc.readBroadcast(GARDENER_COUNT_EVEN_TURN_CHANNEL) + 1);
		} else {
			rc.broadcast(GARDENER_COUNT_ODD_TURN_CHANNEL, rc.readBroadcast(GARDENER_COUNT_ODD_TURN_CHANNEL) + 1);
		}
	}
	
	/**
	 * Returns the Number of Gardeners who where in a garden last Turn.
	 * 
	 * @return number of living Gardeners
	 * @throws GameActionException
	 */
	public static int getGardenerGardenCount() throws GameActionException {
		if (isTurnEven()) {
			return rc.readBroadcast(GARDENER_GARDEN_ODD_TURN_CHANNEL);
		} else {
			return rc.readBroadcast(GARDENER_GARDEN_EVEN_TURN_CHANNEL);
		}
	}

	/**
	 * Only Called by Gardeners. Broadcast that this Gardener is in his functioning garden
	 * 
	 * @throws GameActionException
	 */
	public static void broadcastGardenerGardenMessage() throws GameActionException {
		if (rc.getType() != RobotType.GARDENER)
			return;
		if (isTurnEven()) {
			rc.broadcast(GARDENER_GARDEN_EVEN_TURN_CHANNEL, rc.readBroadcast(GARDENER_GARDEN_EVEN_TURN_CHANNEL) + 1);
		} else {
			rc.broadcast(GARDENER_GARDEN_ODD_TURN_CHANNEL, rc.readBroadcast(GARDENER_GARDEN_ODD_TURN_CHANNEL) + 1);
		}
	}
	
}
