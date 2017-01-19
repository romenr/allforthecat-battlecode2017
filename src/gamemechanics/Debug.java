package gamemechanics;

import static thecat.RobotPlayer.rc;

import battlecode.common.RobotType;

public class Debug {

	/**
	 * Print a Message to the Console
	 * 
	 * @param message
	 *            the Message
	 */
	public static void debug_println(String message) {
		System.out.println(message);
	}
	
	/**
	 * Print Production Info about this Unit
	 */
	public static void debug_productionInfo(){
		System.out.println("=== Production Info ===");
		System.out.println("Round: " + rc.getRoundNum() + "/" + rc.getRoundLimit());
		System.out.println("Bullets: " + rc.getTeamBullets());
		System.out.println("Build Cooldown: " + rc.getBuildCooldownTurns());
		if(rc.getType() == RobotType.ARCHON){
			System.out.println("Robot count: " + rc.getRobotCount());
			System.out.println("Tree count: " + rc.getTreeCount());
		}
	}
	
}
