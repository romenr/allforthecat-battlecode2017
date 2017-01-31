package gamemechanics;

import static thecat.RobotPlayer.rc;

import java.util.HashMap;

import battlecode.common.Clock;
import battlecode.common.RobotType;

public class Debug {

	private static boolean verbose = false;
	private static HashMap<String, Integer> byteCodeMap = new HashMap<>();

	public static void debug_startCountingBytecode(String key) {
		byteCodeMap.put(key, Clock.getBytecodesLeft());
	}

	public static void debug_printUsedBytecode(String key) {
		if (!verbose)
			return;
		Integer s = byteCodeMap.get(key);
		if(s == null){
			System.out.println("You never started to count bytecode with the key " + key);
		}else{
			System.out.println(key+": " + (s-Clock.getBytecodesLeft()));
		}
	}

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
	public static void debug_productionInfo() {
		if (!verbose)
			return;
		System.out.println("=== Production Info ===");
		System.out.println("Round: " + rc.getRoundNum() + "/" + rc.getRoundLimit());
		System.out.println("Bullets: " + rc.getTeamBullets());
		System.out.println("Build Cooldown: " + rc.getBuildCooldownTurns());
		if(rc.getType() == RobotType.ARCHON){
			System.out.println("Robot count: " + rc.getRobotCount());
			System.out.println("Tree count: " + rc.getTreeCount());
		}
	}
	
	private static int monitorByteCodeLimitLastCallRoundNum = -1;
	/**
	 * Check if the robot went past its ByteCode limit by checking round numbers
	 */
	public static void debug_monitorRobotByteCodeLimit() {
		if (monitorByteCodeLimitLastCallRoundNum == -1) {
			monitorByteCodeLimitLastCallRoundNum = rc.getRoundNum();
		}
		if(monitorByteCodeLimitLastCallRoundNum != rc.getRoundNum()){
			System.out.println("ERROR: exeeded Bytecode limit by " + Clock.getBytecodeNum());
		}else{
			if (verbose){
				System.out.println("Bytecode left " + Clock.getBytecodesLeft());
			}
		}
		monitorByteCodeLimitLastCallRoundNum = rc.getRoundNum() + 1;
	}

	public static void debug_printBulletsFired() {
		if (!verbose)
			return;
		System.out.println("Bullets fired " + rc.hasAttacked());
	}
}
