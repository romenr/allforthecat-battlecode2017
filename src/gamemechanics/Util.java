package gamemechanics;

import static thecat.RobotPlayer.rc;
import static gamemechanics.Debug.*;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.BodyInfo;
import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public strictfp class Util {

	public static final int NUMBER_DIRECTIONS = 8;
	public static MapLocation generalEnemyLocation = null;
	public static Boolean clockwise = null;
	static Direction dir = null;
	public static Random rand = new Random(rc.getID());
	static boolean wander = false;
	static boolean positive = rand.nextBoolean();
	static MapLocation center = null;
	static Direction explore = null;

	// this is the slugs "tail" imagine leaving a trail of sticky goo on the map
	// that you don't want to step in that slowly dissapates over time
	static ArrayList<MapLocation> oldLocations = new ArrayList<MapLocation>();

	public static boolean slugMoveToTarget(MapLocation target, float strideRadius) throws GameActionException {

		// when trying to move, let's look forward, then incrementing left and
		// right.
		float[] toTry = { 0, (float) Math.PI / 4, (float) -Math.PI / 4, (float) Math.PI / 2, (float) -Math.PI / 2,
				3 * (float) Math.PI / 4, -3 * (float) Math.PI / 4, -(float) Math.PI };

		MapLocation ourLoc = rc.getLocation();
		Direction toMove = ourLoc.directionTo(target);

		// let's try to find a place to move!
		for (int i = 0; i < toTry.length; i++) {
			Direction dirToTry = toMove.rotateRightDegrees(toTry[i]);
			if (rc.canMove(dirToTry, strideRadius)) {
				// if that location is free, let's see if we've already moved
				// there before (aka, it's in our tail)
				MapLocation newLocation = ourLoc.add(dirToTry, strideRadius);
				boolean haveWeMovedThereBefore = false;
				for (int j = 0; j < oldLocations.size(); j++) {
					if (newLocation.distanceTo(oldLocations.get(j)) < strideRadius * strideRadius) {
						haveWeMovedThereBefore = true;
						break;
					}
				}
				if (!haveWeMovedThereBefore) {
					oldLocations.add(newLocation);
					if (oldLocations.size() > 10) {
						// remove the head and chop the list down to size 10 (or
						// whatever you want to use)
						oldLocations.remove(0);
					}
					if (!rc.hasMoved() && rc.canMove(dirToTry, strideRadius)) {
						rc.move(dirToTry, strideRadius);
					}
					return (true);
				}

			}
		}
		// looks like we can't move anywhere
		return (false);

	}

	public static void checkWinCondition() throws GameActionException {
		// Check if we can win now
		if ((rc.getTeamBullets() / rc.getVictoryPointCost()) + rc.getTeamVictoryPoints() >= 1000) {
			// Buy the Win, erm... i mean donate for the good cause
			rc.donate(rc.getTeamBullets());
		}
		// Donate everything leftover the last round
		if (rc.getRoundNum() == rc.getRoundLimit() - 1) {
			rc.donate(rc.getTeamBullets());
		}
		// If your the last one standing donate as much as you can and hope for
		// the best
		if (rc.getRobotCount() == 1 && rc.getType() != RobotType.ARCHON && rc.getVictoryPointCost() >= rc.getTeamBullets()) {
			rc.donate(rc.getVictoryPointCost());
		}
	}

	private static final int NUM_CHECKS = 8;
	private static final float directionAngle = (float) (Math.PI / NUM_CHECKS);
	private static final int PATH_LENGTH = 16;
	private static MapLocation[] path = null;
	private static int lastPointer = 0;
	private static int lastBeforeBacktrack = 0;
	private static boolean backtrack = false;

	/**
	 * 
	 * This is my implementation of the slug Pathing algorithm. I use a Array as
	 * a ring to store the path the robot walked and added backtracking. That
	 * means if the robot gets stuck it follows its own path back, until it's
	 * not stuck anymore. TODO 1.) Add a Lookahead so that the robot dosen't
	 * need to go the path witch he will backtrack anyways. 2.) Check if the
	 * Robot has left a certain area in x Turns. Maybe with a second slug
	 * algorithm witch has a separate list with fewer points but a lot bigger
	 * radius.
	 * 
	 * @param goalLocation
	 * @return
	 * @throws GameActionException
	 */
	public static boolean moveTo(MapLocation goalLocation) throws GameActionException {
		if (rc.hasMoved() || rc.getLocation().equals(goalLocation))
			return false;

		if (path == null) {
			path = new MapLocation[PATH_LENGTH];
			for (int i = 0; i < PATH_LENGTH; i++) {
				path[i] = new MapLocation(1000, 1000);
			}
		}

		Direction dir = rc.getLocation().directionTo(goalLocation);
		float speed = rc.getType().strideRadius;
		float minDistance = speed * speed;

		for (int i = 0; i <= NUM_CHECKS; i++) {
			// 0, Pi and left rotations
			MapLocation locationAfterMove = rc.getLocation().add(dir.rotateLeftRads(i * directionAngle), speed);

			if (checkMove(locationAfterMove, minDistance)) {
				return true;
			}

			if (i == 0 || i == NUM_CHECKS)
				continue;

			// right rotations
			locationAfterMove = rc.getLocation().add(dir.rotateRightRads(i * directionAngle), speed);

			if (checkMove(locationAfterMove, minDistance)) {
				return true;
			}

		}

		// escape move
		if (rc.canMove(path[lastPointer])) {
			if (!backtrack) {
				lastBeforeBacktrack = lastPointer;
				backtrack = true;
			}
			rc.move(path[lastPointer]);
			lastPointer = (lastPointer - 1 + PATH_LENGTH) % PATH_LENGTH;
			return true;
		}

		return false;
	}

	public static boolean checkMove(MapLocation locationAfterMove, float minDistance) throws GameActionException {
		boolean letsMoveThere = true;
		if (rc.canMove(locationAfterMove)) {
			for (MapLocation onPath : path) {
				if (locationAfterMove.isWithinDistance(onPath, minDistance)) {
					letsMoveThere = false;
					break;
				}
			}
			if (letsMoveThere) {
				if (backtrack) {
					backtrack = false;
					lastPointer = lastBeforeBacktrack;
				}
				lastPointer = (lastPointer + 1) % PATH_LENGTH;
				path[lastPointer] = locationAfterMove;
				rc.move(locationAfterMove);
				return true;
			}
		}
		return false;
	}

	public static void debug_drawPath() {
		if (path == null)
			return;
		for (MapLocation onPath : path) {
			rc.setIndicatorDot(onPath, 255, 0, 0);
		}
	}

	public static void wander() throws GameActionException {
		Direction dir = randomDirection();
		tryMove(dir);
	}

	public static int encode(MapLocation location) {
		int x = Math.round(location.x);
		int y = Math.round(location.y);
		int encoded = (x << 10) + y;
		return encoded;
	}

	public static MapLocation decode(int code) {
		if (code == 0)
			return null;
		int x = code >> 10;
		int y = code & 0x3FF;
		return new MapLocation(x, y);
	}

	/**
	 * Ancient dark Math, use with caution! A chord of a circle is a straight
	 * line segment whose end points both lie on the circle.
	 * 
	 * @param alpha
	 *            angle in Radiant
	 * @return the chord of alpha
	 */
	public static float crd(float alpha) {
		return 2f * (float) Math.sin(alpha / 2f);
	}

	/**
	 * Inverse function of crd y=crd(alpha) acrd(y) = alpha
	 * 
	 * @param y
	 *            lenght of a line with both points on a unit circle
	 * @return alpha angle in Radiant
	 */
	public static float acrd(float y) {
		return 2f * (float) Math.asin(y / 2f);
	}

	/**
	 * Inverse function of crd for a circle different from the unit circle
	 * 
	 * @param y
	 *            lenght of a line with both points on a unit circle
	 * @param radius
	 *            Radius of the circle
	 * @return alpha angle in Radiant
	 */
	public static float acrd(float y, float radius) {
		return acrd(y / radius);
	}

	static MapLocation[][] initialArchonLocation = new MapLocation[2][];

	/**
	 * Get the nearest Archon start point
	 * 
	 * @param team
	 *            the Team of the Archon
	 * @return MapLocation of the nearest Archon Start Point
	 */
	public static MapLocation getNearestInitialArchonLocation(Team team) {
		int teamNr = team.ordinal();
		// Getting Locations is expensive do it only once
		if (initialArchonLocation[teamNr] == null) {
			initialArchonLocation[teamNr] = rc.getInitialArchonLocations(team);
		}
		// Find the nearest Archon
		MapLocation location = initialArchonLocation[teamNr][0];
		for (int i = 1; i < initialArchonLocation[teamNr].length; i++) {
			if (rc.getLocation().distanceTo(initialArchonLocation[teamNr][i]) < rc.getLocation().distanceTo(location)) {
				location = initialArchonLocation[teamNr][i];
			}
		}
		return location;
	}

	public static boolean broadcastEnemy = false;
	public static boolean attackBase = false;
	private static int wanderTime = 0;

	/**
	 * Get a direction in witch to find enemys
	 * 
	 * 1. listen to enemy broadcast 2. go to enemy base 3. explore
	 * 
	 * @return
	 * @throws GameActionException
	 */
	public static Direction getGeneralEnemyDirection() throws GameActionException {
		return rc.getLocation().directionTo(getGeneralEnemyLocation());
	}

	public static MapLocation[] enemyArchons = null;
	public static boolean[] destroyedBases = null;

	/**
	 * Return a enemy base that was not yet destroyed
	 * 
	 * @return
	 * @throws GameActionException 
	 */
	public static MapLocation getEnemyBase() throws GameActionException {
		if (enemyArchons == null) {
			enemyArchons = rc.getInitialArchonLocations(rc.getTeam().opponent());
			destroyedBases = new boolean[enemyArchons.length];
			
		}
		updateDestroyedBases();
		for (int i = 0; i < enemyArchons.length; i++) {
			if (!destroyedBases[i]) {
				return enemyArchons[i];
			}
		}
		return null;
	}
	
	public static void updateDestroyedBases() throws GameActionException{
		int destroyed = rc.readBroadcast(Broadcast.ENEMY_ARCHONS_DESTROYED_CHANNEL);
		for(int i = 0; i < destroyedBases.length; i++){
			int archoni = destroyed >> i;
			if(!destroyedBases[i] && (archoni & 1) == 1){
				destroyedBases[i] = true;
				last++;
			}
		}
	}

	public static int last = 0;

	public static void killedArchon() throws GameActionException {
		if (last < destroyedBases.length) {
			int data = rc.readBroadcast(Broadcast.ENEMY_ARCHONS_DESTROYED_CHANNEL);
			data = data | (1 << last);
			rc.broadcast(Broadcast.ENEMY_ARCHONS_DESTROYED_CHANNEL, data);
			destroyedBases[last++] = true;
		}
	}

	/**
	 * Decides what the next goal of this robot should be used by combat units.
	 * 
	 * @return A MapLocation where the unit should go to
	 * @throws GameActionException
	 */
	public static MapLocation getGeneralEnemyLocation() throws GameActionException {
		// If Enemy Location is set run there
		int code = rc.readBroadcast(Broadcast.ENEMY_LOCATION_CHANNEL);
		if (code != 0) {
			broadcastEnemy = true;
			attackBase = false;
			generalEnemyLocation = decode(code);
		}

		// Check if were at our destination
		if (generalEnemyLocation != null && rc.getLocation().distanceTo(generalEnemyLocation) <= 3) {
			generalEnemyLocation = null;
			// Broadcast that we checked the location out
			if (broadcastEnemy) {
				rc.broadcast(Broadcast.ENEMY_LOCATION_CHANNEL, 0);
				broadcastEnemy = false;
			}
			if (attackBase) {
				killedArchon();
				attackBase = false;
			}
		}

		// If enemy Location is not set try to run to an archon
		if (generalEnemyLocation == null) {
			// get a archon witch we didn't yet go to
			MapLocation enmarchon = getEnemyBase();
			if (enmarchon == null) {
				// No more archons go search for enemy's
				wander = true;
			} else {
				attackBase = true;
				generalEnemyLocation = enmarchon;
			}
		}
		if (wander) {
			wanderTime++;
			// Don't try to long
			if (wanderTime > 60) {
				generalEnemyLocation = null;
			}
		}
		// Search enemy's
		if (wander && generalEnemyLocation == null) {
			explore = randomDirection();
			generalEnemyLocation = rc.getLocation().add(explore, 20);
			wanderTime = 0;
		}
		rc.setIndicatorDot(generalEnemyLocation, 0x4b, 00, 0x82);
		return generalEnemyLocation;
	}

	public static Direction getWanderMapDirection() {
		if (clockwise == null) {
			clockwise = isTurnEven();
		}
		if (dir == null)
			dir = randomDirection();
		int round = rc.getRoundNum() % 60;
		if (clockwise) {
			return dir.rotateLeftDegrees(6 * round);
		} else {
			return dir.rotateRightDegrees(6 * round);
		}
	}

	/**
	 * Returns True if the current Turn is a Even turn
	 * 
	 * @return ture if turn even, false if odd
	 */
	public static boolean isTurnEven() {
		return (rc.getRoundNum() & 1) == 0;
	}

	/**
	 * Returns a Direction Array with length NUMBER_DIRECTIONS
	 * 
	 * The First Direction is North every other gets Rotated by
	 * 2PI/NUMBER_DIRECTIONS
	 * 
	 * @param numberDirections
	 *            NUMBER_DIRECTIONS = numberDirections
	 * @return Direction Array
	 */
	public static Direction[] getDirections(int numberDirections) {
		Direction[] directions = new Direction[numberDirections];
		for (int i = 0; i < numberDirections; i++) {
			directions[i] = new Direction((float) (-Math.PI) + (float) (i * 2 * Math.PI / numberDirections));
		}
		return directions;
	}

	/**
	 * Returns a Direction Array with length NUMBER_DIRECTIONS
	 * 
	 * The First Direction is North every other gets Rotated by
	 * 2PI/NUMBER_DIRECTIONS
	 * 
	 * @return Direction Array
	 */
	public static Direction[] getDirections() {
		return getDirections(NUMBER_DIRECTIONS);
	}

	/**
	 * Returns a random Direction
	 * 
	 * @return a random Direction
	 */
	public static Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * directly in the path.
	 *
	 * @param dir
	 *            The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	public static boolean tryMove(Direction dir) throws GameActionException {
		return tryMove(dir, rc.getType().strideRadius);
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * directly in the path.
	 *
	 * @param dir
	 *            The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	public static boolean tryMove(Direction dir, float dist) throws GameActionException {
		return tryMove(dir, 4f, 18, dist);
	}

	/**
	 * Search in a Rectangle around this robot for a Spot with a Circle free
	 * space
	 * 
	 * @param radius
	 *            Radius of the Circle
	 * @return The Map position of the free Space
	 * @throws GameActionException
	 */
	public static MapLocation getFreeMapPosition(float radius) throws GameActionException {
		MapLocation freeLocation = null;
		Direction[] directions = getDirections();
		for (Direction d : directions) {
			float dist;
			float sinD = (float) Math.sin(d.radians);
			if (sinD == 0) {
				dist = (rc.getType().bodyRadius + radius);
			} else {
				dist = (rc.getType().bodyRadius + radius) / sinD;
			}
			freeLocation = rc.getLocation().add(d, dist);
			if (rc.isCircleOccupied(freeLocation, radius)) {
				return freeLocation;
			}
		}
		return freeLocation;
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * direction in the path.
	 *
	 * @param dir
	 *            The intended direction of movement
	 * @param degreeOffset
	 *            Spacing between checked directions (degrees)
	 * @param checksPerSide
	 *            Number of extra directions checked on each side, if intended
	 *            direction was unavailable
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	public static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide, float distance)
			throws GameActionException {

		if (distance > rc.getType().strideRadius) {
			distance = rc.getType().strideRadius;
		}

		if (dir == null) {
			dir = Direction.NORTH;
		}

		// First, try intended direction
		if (rc.canMove(dir, distance)) {
			rc.move(dir, distance);
			return true;
		}

		// Now try a bunch of similar angles
		int currentCheck = 1;

		while (currentCheck <= checksPerSide) {
			// Try the offset of the left side
			if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance)) {
				rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance);
				return true;
			}
			// Try the offset on the right side
			if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck), distance)) {
				rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck), distance);
				return true;
			}
			// No move performed, try slightly further
			currentCheck++;
		}

		// A move never happened, so return false.
		return false;
	}

	/**
	 * A slightly more complicated example function, this returns true if the
	 * given bullet is on a collision course with the current robot. Doesn't
	 * take into account objects between the bullet and this robot.
	 *
	 * @param bullet
	 *            The bullet in question
	 * @return True if the line of the bullet's path intersects with this
	 *         robot's current position.
	 */
	public static boolean willCollideWithMe(BulletInfo bullet) {
		MapLocation myLocation = rc.getLocation();

		// Get relevant bullet information
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;

		// Calculate bullet relations to this robot
		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		float distToRobot = bulletLocation.distanceTo(myLocation);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		// If theta > 90 degrees, then the bullet is traveling away from us and
		// we can break early
		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}

		// distToRobot is our hypotenuse, theta is our angle, and we want to
		// know this length of the opposite leg.
		// This is the distance of a line that goes from myLocation and
		// intersects perpendicularly with propagationDirection.
		// This corresponds to the smallest radius circle centered at our
		// location that would intersect with the
		// line that is the path of the bullet.
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh
																					// cah
																					// toa
																					// :)

		return (perpendicularDist <= rc.getType().bodyRadius);
	}

	static boolean trySidestep(BulletInfo bullet) throws GameActionException {

		Direction towards = bullet.getDir();

		return (tryMove(towards.rotateRightDegrees(90)) || tryMove(towards.rotateLeftDegrees(90)));
	}

	public static boolean dodge() throws GameActionException {
		BulletInfo[] bullets = rc.senseNearbyBullets();
		for (BulletInfo bi : bullets) {
			if (willCollideWithMe(bi)) {
				if (trySidestep(bi)) {
					return true;
				}
			}
		}
		return false;

	}

	public static float willCollideWith(Direction propagationDirection, MapLocation bulletLocation, BodyInfo bodyInfo) {
		MapLocation location = bodyInfo.getLocation();

		// Calculate bullet relations to this robot
		Direction directionToRobot = bulletLocation.directionTo(location);
		float distToRobot = bulletLocation.distanceTo(location);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		// If theta > 90 degrees, then the bullet is traveling away from us and
		// we can break early
		if (Math.abs(theta) > Math.PI / 2) {
			return 0;
		}

		// distToRobot is our hypotenuse, theta is our angle, and we want to
		// know this length of the opposite leg.
		// This is the distance of a line that goes from myLocation and
		// intersects perpendicularly with propagationDirection.
		// This corresponds to the smallest radius circle centered at our
		// location that would intersect with the
		// line that is the path of the bullet.
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh

		if ((perpendicularDist <= rc.getType().bodyRadius)) {
			return bulletLocation.distanceTo(bodyInfo.getLocation());
		} else {
			return 0;
		}
	}
	
	public static float willCollideWith(Direction dir, MapLocation location, Team team) {
		if (Team.NEUTRAL == team) {
			TreeInfo[] trees = Sensor.getTreeInfos();
			Team enemyTeam = rc.getTeam().opponent();
			for (TreeInfo tree : trees) {
				if(tree.getTeam() == enemyTeam) continue;
				float when = willCollideWith(dir, location, tree);
				if (when > 0) {
					return when;
				}
			}
		} else {
			RobotInfo[] robotInfos = Sensor.getTeam(team);
			for (RobotInfo robot : robotInfos) {
				float when = willCollideWith(dir, location, robot);
				if (when > 0) {
					return when;
				}
			}
		}
		return 0;
	}

	public static float willCollideWithTree(Direction dir) {
		return willCollideWith(dir,
				rc.getLocation().add(dir, rc.getType().bodyRadius + GameConstants.BULLET_SPAWN_OFFSET), Team.NEUTRAL);
	}
	public static Boolean[] canShootBullet;
	public static int lastUpdateRound = -1;
	public static final int NUMBEROFDIRS = 360;
	
	public static void initCanShootBullet(){
		if(rc.getRoundNum() != lastUpdateRound){
			canShootBullet = new Boolean[NUMBEROFDIRS];
			lastUpdateRound = rc.getRoundNum();
		}
	}

	public static boolean canShootBulletTo(Direction dir) {
		initCanShootBullet();
		int direction = (int)(dir.getAngleDegrees()) + 180;
		if(canShootBullet[direction] != null) return canShootBullet[direction];
		MapLocation location = rc.getLocation().add(dir, rc.getType().bodyRadius + GameConstants.BULLET_SPAWN_OFFSET);
		float ally = willCollideWith(dir, location, rc.getTeam());
		float enemy = willCollideWith(dir, location, rc.getTeam().opponent());
		float tree = willCollideWith(dir, location, Team.NEUTRAL);
		if (enemy == 0){
			canShootBullet[direction] = false;
			return false;
		}
		if ((ally == 0 && tree == 0)||(ally == 0 && enemy <= tree)){
			canShootBullet[direction] = true;
			return true;
		}
		canShootBullet[direction] = enemy < ally;
		return enemy < ally;
	}

	public static boolean canShootTriadTo(Direction dir) {
		return canShootTriadTo(dir, 1);
	}
	
	public static boolean canShootTriadTo(Direction dir, int hits) {
		boolean a = canShootBulletTo(dir);
		boolean b = canShootBulletTo(dir.rotateLeftDegrees(GameConstants.TRIAD_SPREAD_DEGREES));
		boolean c = canShootBulletTo(dir.rotateRightDegrees(GameConstants.TRIAD_SPREAD_DEGREES));
		return (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) > hits;
	}
	
	public static boolean canShootPentandTo(Direction dir) {
		boolean a = canShootBulletTo(dir);
		boolean b = canShootBulletTo(dir.rotateLeftDegrees(GameConstants.PENTAD_SPREAD_DEGREES));
		boolean c = canShootBulletTo(dir.rotateLeftDegrees(2 * GameConstants.PENTAD_SPREAD_DEGREES));
		boolean d = canShootBulletTo(dir.rotateRightDegrees(GameConstants.PENTAD_SPREAD_DEGREES));
		boolean e = canShootBulletTo(dir.rotateRightDegrees(2 * GameConstants.PENTAD_SPREAD_DEGREES));
		return (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) + (d ? 1 : 0) + (e ? 1 : 0) > 3;
	}

	/**
	 * Calculate the Point you should walk to if you want to wander in a circle
	 * around center
	 * 
	 * @param center
	 *            The center you walk around
	 * @param pointOnCircle
	 *            Current position
	 * @param positive
	 *            Direction
	 * @return Position where you should move to
	 */
	public static MapLocation moveCircleAround(MapLocation center, MapLocation pointOnCircle, boolean positive) {
		// Line inside the Circle
		float y = rc.getType().strideRadius;
		// alpha angle of the triangle were moving along
		float alpha = acrd(y, center.distanceTo(pointOnCircle));
		// the sin of alpha ^^
		float x = (float) Math.sin(alpha);
		if (x == 0) {
			// Should never happen :D
			debug_println("moveCircleAround Division by Zero");
			// Hopefully ~Rene
			return new MapLocation(0, 0);
		}
		// The angle we need to move
		float beta = (float) Math.asin(x / y);
		// Direction between center and pointOnCircle
		Direction direction = center.directionTo(pointOnCircle);
		// Left right degrees Rads ...
		if (positive) {
			direction = direction.rotateLeftDegrees(90).rotateLeftRads(beta);
		} else {
			direction = direction.rotateRightDegrees(90).rotateRightRads(beta);
		}
		MapLocation moveTo = pointOnCircle.add(direction, y);
		return moveTo;
	}

}
