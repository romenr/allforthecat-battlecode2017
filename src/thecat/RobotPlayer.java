package thecat;

import battlecode.common.*;
import robots.*;

public strictfp class RobotPlayer {
	public static RobotController rc;

	/**
	 * run() is the method that is called when a robot is instantiated in the
	 * Battlecode world. If this method returns, the robot dies!
	 **/
	public static void run(RobotController rc) throws GameActionException {

		// This is the RobotController object. You use it to perform actions
		// from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;

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
