package robots;

import static gamemechanics.Util.canShootBulletTo;
import static gamemechanics.Util.canShootPentandTo;
import static gamemechanics.Util.canShootTriadTo;
import static gamemechanics.Util.checkWinCondition;
import static gamemechanics.Util.dodge;
import static gamemechanics.Util.getWanderMapDirection;
import static gamemechanics.Util.tryMove;
import static thecat.RobotPlayer.rc;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import gamemechanics.Broadcast;
import gamemechanics.Debug;
import gamemechanics.NeutralTrees;
import gamemechanics.Sensor;
import gamemechanics.Util;

public strictfp class Tank {

	public static void run() throws GameActionException {

		// The code you want your robot to perform every round should be in this
		// loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your
			// robot to explode
			try {
				checkWinCondition();
				
				Sensor.updateSensorData();

				// See if there are any nearby enemy robots
				RobotInfo[] robots = Sensor.getEnemy();
				BulletInfo[] bulletInfos = rc.senseNearbyBullets();

				// If there are some...
				if (robots.length > 0) {
					Broadcast.broadcastEnemySeen();
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
						float distance = rc.getLocation().distanceTo(robots[shootAt].location);
						MapLocation enemysNextLocation;
						if(distance <= 5){
							enemysNextLocation = Sensor.predictEnemyMovement(robots[shootAt],1);
						}else{
							enemysNextLocation = Sensor.predictEnemyMovement(robots[shootAt],2);
						}
						Direction shootTo;
						if(enemysNextLocation != null){
							shootTo  = rc.getLocation().directionTo(enemysNextLocation);
						}else{
							shootTo  = rc.getLocation().directionTo(robots[shootAt].location);
						}
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
					if (!Util.moveTo(Util.getGeneralEnemyLocation())) {
						if(!tryMove(getWanderMapDirection())){
							System.out.println("I did not Move");
						}

					}
				}

				NeutralTrees.shakeBulletTree();

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Debug.debug_monitorRobotByteCodeLimit();
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

}
