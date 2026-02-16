package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;

import zombie.AttackType;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraits;
import zombie.CollisionManager;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

/**
 * This patch provides an optimized implementation of IsoMovingObject.separate().
 *
 * Separate is the core "pushing" and collision detection logic for every character in the game.
 * It runs for every zombie and player against every other nearby object, making it a major bottleneck.
 *
 * Optimizations:
 * 1. Early Z-Axis Exit: Immediately skips collision checks if objects are on different heights.
 * 2. Squared Distance Math: Replaces expensive Math.sqrt() with dx*dx + dy*dy comparisons.
 * 3. Local Variable Caching: Caches self position, getWidth() etc. to avoid repeated method calls.
 * 4. Identity Skip: Quickly skips self-comparison.
 * 5. Lazy Type Casting: Moves expensive Type.tryCastTo() checks inside the proximity check.
 * 6. Forward Direction Optimization: Uses direct X/Y components to avoid Vector2 object allocations.
 */
@Patch(className = "zombie.iso.IsoMovingObject", methodName = "separate")
public class Patch_IsoMovingObject_B42_13 {
    public static final boolean ALL_FIELDS_FOUND = true; // for uniformity, used in tests

    @Patch.RuntimeType
    @Patch.OnEnter(skipOn = true)
    public static boolean separate(@Patch.This Object selfObj) {
        if (!ZBBetterFPS.g_OptimizeIsoMovingObject) {
            return false;
        }
        optimized_separate(selfObj);
        return true;
    }

    public static void optimized_separate(Object selfObj) {
        IsoMovingObject self = (IsoMovingObject) selfObj;

        // Skip non-physical objects early
        if (!self.isSolidForSeparate() || !self.isPushableForSeparate()) {
            return;
        }

        // Cache self attributes
        float selfZ = self.getZ();
        float selfNextX = self.getNextX();
        float selfNextY = self.getNextY();
        float selfWidth = self.getWidth();

        IsoGameCharacter thisChr = (IsoGameCharacter) Type.tryCastTo(self, IsoGameCharacter.class);
        IsoPlayer thisPlyr = (IsoPlayer) Type.tryCastTo(self, IsoPlayer.class);
        IsoZombie thisZombie = (IsoZombie) Type.tryCastTo(self, IsoZombie.class);

        // Pre-calculate max weapon range once
        float maxWeaponRange = (thisPlyr == null || !(thisPlyr.getPrimaryHandItem() instanceof HandWeapon)) ? 0.3f
                : ((HandWeapon) thisPlyr.getPrimaryHandItem()).getMaxRange();

        IsoGridSquare currentSquare = self.getCurrentSquare();
        if (currentSquare == null) {
            return;
        }

        long now = 0;

        // Iterate through surrounding squares (including current)
        for (int i = 0; i <= 8; i++) {
            IsoGridSquare sq = (i == 8) ? currentSquare : currentSquare.getSurroundingSquares()[i];

            if (sq != null && !sq.getMovingObjects().isEmpty()
                    && (sq == currentSquare || !currentSquare.isBlockedTo(sq))) {

                int size = sq.getMovingObjects().size();
                for (int n = 0; n < size; n++) {
                    IsoMovingObject obj = sq.getMovingObjects().get(n);

                    if (obj == self) {
                        continue;
                    }

                    // Optimization 1: Fail fast on Z-axis difference
                    float dz = selfZ - obj.getZ();
                    if (dz < -0.3f || dz > 0.3f) {
                        continue;
                    }

                    if (!obj.isSolidForSeparate()) {
                        continue;
                    }

                    float dx = selfNextX - obj.getNextX();
                    float dy = selfNextY - obj.getNextY();

                    // Optimization 2: Use squared distance to avoid Math.sqrt()
                    float distSq = dx * dx + dy * dy;
                    float twidth = selfWidth + obj.getWidth();
                    float twidthSq = twidth * twidth;

                    float range = twidth + maxWeaponRange;
                    float rangeSq = range * range;

                    if (distSq < rangeSq) {
                        // Optimization 5: Lazy casting only when objects are actually close
                        IsoGameCharacter objChr = (IsoGameCharacter) Type.tryCastTo(obj, IsoGameCharacter.class);
                        boolean otherIsVehicle = obj instanceof BaseVehicle;

                        if (thisChr == null || (objChr == null && !otherIsVehicle)) {
                            if (distSq < twidthSq) {
                                CollisionManager.instance.AddContact(self, obj);
                            }
                            return;
                        }

                        if (objChr == null) {
                            continue;
                        }

                        IsoPlayer objPlyr = (IsoPlayer) Type.tryCastTo(obj, IsoPlayer.class);
                        IsoZombie objZombie = (IsoZombie) Type.tryCastTo(obj, IsoZombie.class);

                        // Spear charge logic
                        if (thisPlyr != null && thisPlyr.getBumpedChr() != obj) {
                            float fwdX = thisPlyr.getForwardDirectionX();
                            float fwdY = thisPlyr.getForwardDirectionY();
                            float dot = (fwdX * dx) + (fwdY * dy);
                            // cos(theta) = dot(A,B) / (|A|*|B|)
                            double cos = (double) dot / Math.sqrt(distSq);
                            if (cos < -0.866025d) { // > 150 degrees
                                if (thisPlyr.getBeenSprintingFor() >= 70.0f
                                        && WeaponType.getWeaponType(thisPlyr) == WeaponType.SPEAR) {
                                    thisPlyr.reportEvent("ChargeSpearConnect");
                                    thisPlyr.setAttackType(AttackType.CHARGE);
                                    thisPlyr.setAttackStarted(true);
                                    thisPlyr.setVariable("StartedAttackWhileSprinting", true);
                                    thisPlyr.setBeenSprintingFor(0.0f);
                                    return;
                                }
                            }
                        }

                        // Physical collision/bumping logic
                        if (distSq < twidthSq) {
                            boolean bump = false;
                            if (thisPlyr != null && thisPlyr.getVariableFloat("WalkSpeed", 0.0f) > 0.2f
                                    && thisPlyr.runningTime > 0.5f && thisPlyr.getBumpedChr() != obj) {
                                bump = true;
                            }
                            if (GameClient.client && thisPlyr != null && (objChr instanceof IsoPlayer)
                                    && !ServerOptions.getInstance().playerBumpPlayer.getValue()) {
                                bump = false;
                            }
                            if (thisZombie != null && thisZombie.isReanimatedForGrappleOnly()) {
                                bump = false;
                            }
                            if (objZombie != null && objZombie.isReanimatedForGrappleOnly()) {
                                bump = false;
                            }

                            if (bump && !thisPlyr.isAttackType(AttackType.CHARGE)) {
                                if (now == 0) now = System.currentTimeMillis();
                                boolean wasBumped = !self.isOnFloor() && (thisChr.getBumpedChr() != null
                                        || (now - thisPlyr.getLastBump()) / 100 < 15
                                        || thisPlyr.isSprinting()) && (objPlyr == null || !objPlyr.isNPC());
                                if (wasBumped) {
                                    thisChr.bumpNbr++;
                                    int baseChance = (10 - (thisChr.bumpNbr * 3))
                                            + thisChr.getPerkLevel(PerkFactory.Perks.Fitness)
                                            + thisChr.getPerkLevel(PerkFactory.Perks.Strength)
                                            - (thisChr.getMoodles().getMoodleLevel(MoodleType.DRUNK) * 2);
                                    CharacterTraits characterTraits = thisChr.getCharacterTraits();
                                    if (characterTraits.get(CharacterTrait.CLUMSY)) baseChance -= 5;
                                    if (characterTraits.get(CharacterTrait.GRACEFUL)) baseChance += 5;
                                    if (characterTraits.get(CharacterTrait.VERY_UNDERWEIGHT)) baseChance -= 8;
                                    if (characterTraits.get(CharacterTrait.UNDERWEIGHT)) baseChance -= 4;
                                    if (characterTraits.get(CharacterTrait.OBESE)) baseChance -= 8;
                                    if (characterTraits.get(CharacterTrait.OVERWEIGHT)) baseChance -= 4;
                                    BodyPart part = thisChr.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
                                    if (part.getAdditionalPain(true) > 20.0f) {
                                        baseChance = (int) (baseChance - ((part.getAdditionalPain(true) - 20.0f) / 20.0f));
                                    }
                                    if (Rand.Next(Math.max(1, Math.min(80, baseChance))) == 0 || thisChr.isSprinting()) {
                                        thisChr.setVariable("BumpDone", false);
                                        thisChr.setBumpFall(true);
                                        thisChr.setVariable("TripObstacleType", "zombie");
                                    }
                                } else {
                                    thisChr.bumpNbr = 0;
                                }
                                thisChr.setLastBump(now);
                                thisChr.setBumpedChr(objChr);
                                thisChr.setBumpType(self.getBumpedType(objChr));
                                boolean fromBehind = thisChr.isBehind(objChr);
                                String zombieBump = thisChr.getBumpType();
                                if (fromBehind) {
                                    zombieBump = zombieBump.equals("left") ? "right" : "left";
                                }
                                objChr.setBumpType(zombieBump);
                                objChr.setHitFromBehind(fromBehind);
                                if (wasBumped | GameClient.client) {
                                    thisChr.getActionContext().reportEvent("wasBumped");
                                }
                            }

                            if (GameServer.server || self.distToNearestCamCharacter() < 60.0f) {
                                if (thisZombie != null) {
                                    thisZombie.networkAi.wasSeparated = true;
                                }
                                if (self.isPushedByForSeparate(obj)) {
                                    float len = (float) Math.sqrt(distSq);
                                    if (len > 0) {
                                        float factor = (len - twidth) / (8.0f * len);
                                        self.setNextX(selfNextX - dx * factor);
                                        self.setNextY(selfNextY - dy * factor);
                                    }
                                }
                                self.collideWith(obj);
                            }
                        }
                    }
                }
            }
        }
    }
}
