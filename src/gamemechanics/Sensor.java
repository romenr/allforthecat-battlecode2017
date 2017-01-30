package gamemechanics;

import battlecode.common.BulletInfo;
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
	private static RobotInfo[][] enemyRobotInfos = new RobotInfo[NUM_ROUNDS][];
	private static RobotInfo[][] alliedRobotInfos = new RobotInfo[NUM_ROUNDS][];
	private static TreeInfo[][] treeInfos = new TreeInfo[NUM_ROUNDS][];
	private static BulletInfo[] bulletInfos;
	private static Team allied = rc.getTeam();
	private static Team enemy = allied.opponent();
	
	public static void updateSensorData(){
		index = (index + 1) % NUM_ROUNDS;
		enemyRobotInfos[index] = rc.senseNearbyRobots(SENSE_ALL, enemy);
		alliedRobotInfos[index] = rc.senseNearbyRobots(SENSE_ALL, allied);
		treeInfos[index] = rc.senseNearbyTrees();
		bulletInfos = rc.senseNearbyBullets();
	}
	
	public static TreeInfo[] getTreeInfos(){
		return treeInfos[index];
	}
	
	public static BulletInfo[] getBulletInfos(){
		return bulletInfos;
	}
	
	public static RobotInfo[] getAllied(){
		return alliedRobotInfos[index];
	}
	
	public static RobotInfo[] getEnemy(){
		return enemyRobotInfos[index];
	}
	
	public static RobotInfo[] getTeam(Team team){
		if(team == rc.getTeam()){
			return alliedRobotInfos[index];
		}else{
			return enemyRobotInfos[index];
		}
	}
	
	public static RobotInfo getAllied(int i){
		return alliedRobotInfos[index][i];
	}
	
	public static RobotInfo getEnemy(int i){
		return enemyRobotInfos[index][i];
	}
	
	public static TreeInfo getTree(int i){
		return treeInfos[index][i];
	}
	
}
