package robots;

import static gamemechanics.Util.canShootBulletTo;
import static gamemechanics.Util.canShootPentandTo;
import static gamemechanics.Util.canShootTriadTo;
import static gamemechanics.Util.checkWinCondition;
import static gamemechanics.Util.dodge;
import static gamemechanics.Util.getGeneralEnemyDirection;
import static gamemechanics.Util.getWanderMapDirection;
import static gamemechanics.Util.tryMove;
import static gamemechanics.Util.willCollideWithTree;
import static thecat.RobotPlayer.rc;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import gamemechanics.NeutralTrees;

public strictfp class Tank {

	public static void run() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		

		// The code you want your robot to perform every round should be in this
		// loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your
			// robot to explode
			try {
				checkWinCondition();
				
				// See if there are any nearby enemy robots
				RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

				// If there are some...
				if (robots.length > 0) {
					MapLocation myLocation = rc.getLocation();
					MapLocation enemyLocation = robots[0].getLocation();
					Direction toEnemy = myLocation.directionTo(enemyLocation);
					int shootAt = 0;

					switch(robots[0].type){
					case LUMBERJACK:
						if(rc.getLocation().distanceTo(robots[0].getLocation()) < 4.6f){
							tryMove(toEnemy.opposite());
						}else{
							tryMove(toEnemy);
						}
						break;
					case SCOUT:
						if(rc.getLocation().distanceTo(robots[0].getLocation()) < 3f){
							tryMove(toEnemy.opposite());
						}else{
							tryMove(toEnemy);
						}
						break;
					case GARDENER:
					case ARCHON:
						tryMove(toEnemy);
						break;
					case SOLDIER:
					case TANK:
						dodge();
						break;
					default:
						break;
					}
					while(shootAt < robots.length){
						Direction shootTo = rc.getLocation().directionTo(robots[shootAt].location);
						if(!willCollideWithTree(shootTo)){
							if (rc.canFirePentadShot() && canShootPentandTo(shootTo)) {
								rc.firePentadShot(shootTo);
								break;
							}
							if (rc.canFireTriadShot() && canShootTriadTo(shootTo)) {
								rc.fireTriadShot(shootTo);
								break;
							}
							if (rc.canFireSingleShot() && canShootBulletTo(shootTo)) {
								rc.fireSingleShot(shootTo);
								break;
							}
						}
						shootAt++;
					}
				}else{
					if(!tryMove(getGeneralEnemyDirection())){
						if(!tryMove(getWanderMapDirection())){
							System.out.println("I did not Move");
						}
					}
				}

				NeutralTrees.shakeBulletTree();
				
				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}
	
}
