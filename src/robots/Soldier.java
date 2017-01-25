package robots;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import gamemechanics.NeutralTrees;
import gamemechanics.Util;

import static thecat.RobotPlayer.rc;
import static gamemechanics.Broadcast.ENEMY_LOCATION;
import static gamemechanics.Util.*;

public strictfp class Soldier {

	static MapLocation goal;
	static boolean positive = rand.nextBoolean();

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
				BulletInfo[] bulletInfos = rc.senseNearbyBullets();

				// If there are some...
				if (robots.length > 0) {
					if (rc.readBroadcast(ENEMY_LOCATION) == 0) {
						rc.broadcast(ENEMY_LOCATION, encode(robots[0].getLocation()));
					}

					// And we have enough bullets, and haven't attacked yet this
					// turn...
					// If there is a robot, move towards it

					MapLocation myLocation = rc.getLocation();
					MapLocation enemyLocation = robots[0].getLocation();
					Direction toEnemy = myLocation.directionTo(enemyLocation);
					int shootAt = 0;

					switch (robots[0].type) {
					case LUMBERJACK:
						if (rc.getLocation().distanceTo(robots[0].getLocation()) < 5.6f) {
							tryMove(toEnemy.opposite());
						} else {
							tryMove(toEnemy);
						}
						break;
					case GARDENER:
					case SCOUT:
						tryMove(toEnemy);
						break;
					case SOLDIER:
					case TANK:
						if (rc.getLocation().distanceTo(robots[0].getLocation()) > 6) {
							tryMove(toEnemy);
						} else {
							if(!dodge()){
								tryMove(toEnemy);
							}
						}
						break;
					case ARCHON:
						if (robots.length > 1) {
							tryMove(rc.getLocation().directionTo(robots[1].getLocation()));
						}else{
							tryMove(toEnemy);
						}
					default:
						break;
					}
					while (shootAt < robots.length) {
						Direction shootTo = rc.getLocation().directionTo(robots[shootAt].location);
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
						shootAt++;
					}
				} else if(bulletInfos.length > 0 && dodge()){
					//Dodged bullet
				}else{
					if (!Util.moveToTarget(Util.getGeneralEnemyLocation())) {
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
