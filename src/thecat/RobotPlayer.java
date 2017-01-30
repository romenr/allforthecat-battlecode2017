package thecat;

import battlecode.common.*;
import robots.*;

public strictfp class RobotPlayer {
	public static RobotController rc;
	public static MapLocation[][] initialArchonLocations;
	public static Team ally;
	public static Team opponent;
	public static int numberOfArchons;

	/**
	 * run() is the method that is called when a robot is instantiated in the
	 * Battlecode world. If this method returns, the robot dies!
	 **/
	public static void run(RobotController rc) throws GameActionException {

		// This is the RobotController object. You use it to perform actions
		// from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;
		
		// Save the Team of the Robot and the Opponents
		ally = rc.getTeam();
		opponent = ally.opponent();
		
		// Get the Initial Archon Locations
		initialArchonLocations = new MapLocation[2][];
		initialArchonLocations[ally.ordinal()] = rc.getInitialArchonLocations(ally);
		initialArchonLocations[opponent.ordinal()] = rc.getInitialArchonLocations(opponent);
		
		// Save the Number of Archons per Team
		numberOfArchons = initialArchonLocations[0].length;
		
		//Debug.debug_println("Startup Bytecosts: " + Clock.getBytecodeNum());

		// Here, we've separated the controls into a different method for each
		// RobotType.
		// You can add the missing ones or rewrite this into your own control
		// structure.
		switch (rc.getType()) {
		case ARCHON:
			Archon.run();
			break;
		case GARDENER:
			Gardener.run();
			break;
		case SOLDIER:
			Soldier.run();
			break;
		case LUMBERJACK:
			Lumberjack.run();
			break;
		case SCOUT:
			Scout.run();
			break;
		case TANK:
			Tank.run();
			break;
		default:
			break;
		}
	}

	


	
	
	
	
}
