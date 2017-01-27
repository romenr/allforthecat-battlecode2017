package gamemechanics;

import static thecat.RobotPlayer.rc;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public strictfp class NeutralTrees {

	/**
	 * Shake a nearby Tree, but only if it contains Bullets
	 * @return ID of the shaken Tree, -1 otherwise
	 * @throws GameActionException
	 */
	public static int shakeBulletTree() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE, Team.NEUTRAL);
		for (TreeInfo tree : trees) {
			if (tree.containedBullets > 0) {
				rc.shake(tree.getID());
				return tree.getID();
			}
		}
		return -1;
	}
	
}
