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
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

import java.lang.reflect.Field;

@Patch(className = "zombie.iso.IsoMovingObject", methodName = "separate")
public class Patch_IsoMovingObject {
    public static Vector2 tempo;

    static {
        try {
            Field field = IsoMovingObject.class.getDeclaredField("tempo");
            field.setAccessible(true);
            tempo = (Vector2) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            tempo = new Vector2();
        }
    }

    @Patch.RuntimeType
    @Patch.OnEnter(skipOn = true)
    public static boolean separate(@Patch.This Object selfObj) {
        if (!ZBBetterFPS.g_OptimizeIsoMovingObject) {
            return false;
        }

        optimized_separate(selfObj);
        return true;
    }

    /**
     * Optimized version of IsoMovingObject.separate()
     * 
     * Optimizations:
     * 1. Identity & Early Z-check: Skipped objects early based on identity and Z-axis difference.
     * 2. Squared Distance: Used squared distance (dx*dx + dy*dy) for proximity checks to avoid expensive Math.sqrt() calls.
     * 3. Attribute Caching: Cached self.getZ(), self.getNextX(), self.getNextY(), and self.getWidth() in local variables.
     * 4. Lazy Casting: Moved Type.tryCastTo() calls inside the distance check to avoid casting objects that are far away.
     * 5. Scope Hoisting: Calculated maxWeaponRange once outside the loop instead of every iteration.
     * 6. Loop Clarification: Refactored the while loop into a standard for loop.
     */
    public static void optimized_separate(Object selfObj) {
        IsoMovingObject self = (IsoMovingObject) selfObj;

        // Optimization 3: Attribute Caching
        if (!self.isSolidForSeparate() || !self.isPushableForSeparate()) {
            return;
        }

        float selfZ = self.getZ();
        float selfNextX = self.getNextX();
        float selfNextY = self.getNextY();
        float selfWidth = self.getWidth();

        IsoGameCharacter thisChr = (IsoGameCharacter) Type.tryCastTo(self, IsoGameCharacter.class);
        IsoPlayer thisPlyr = (IsoPlayer) Type.tryCastTo(self, IsoPlayer.class);
        IsoZombie thisZombie = (IsoZombie) Type.tryCastTo(self, IsoZombie.class);

        // Optimization 5: Scope Hoisting
        float maxWeaponRange = (thisPlyr == null || !(thisPlyr.getPrimaryHandItem() instanceof HandWeapon)) ? 0.3f
                : ((HandWeapon) thisPlyr.getPrimaryHandItem()).getMaxRange();

        IsoGridSquare currentSquare = self.getCurrentSquare();
        if (currentSquare == null)
            return;

        // Optimization 6: Loop Clarification
        for (int i = 0; i <= 8; i++) {
            IsoGridSquare sq = i == 8 ? currentSquare : currentSquare.getSurroundingSquares()[i];

            if (sq != null && !sq.getMovingObjects().isEmpty()
                    && (sq == currentSquare || !currentSquare.isBlockedTo(sq))) {
                int size = sq.getMovingObjects().size();
                for (int n = 0; n < size; n++) {
                    IsoMovingObject obj = sq.getMovingObjects().get(n);

                    // Optimization 1: Identity & Early Z-check
                    if (obj == self)
                        continue;

                    // Fail fast on Z-axis difference
                    float dz = selfZ - obj.getZ();
                    if (dz < -0.3f || dz > 0.3f)
                        continue;

                    if (!obj.isSolidForSeparate())
                        continue;

                    float dx = selfNextX - obj.getNextX();
                    float dy = selfNextY - obj.getNextY();

                    // Optimization 2: Squared Distance
                    float distSq = dx * dx + dy * dy;
                    float twidth = selfWidth + obj.getWidth();
                    float twidthSq = twidth * twidth;

                    // Optimization 4: Lazy Casting
                    // Only cast if the distance is within range for either simple collision or weapon range
                    float range = twidth + maxWeaponRange;
                    if (distSq < range * range) {
                        IsoGameCharacter objChr = (IsoGameCharacter) Type.tryCastTo(obj, IsoGameCharacter.class);

                        if (thisChr == null || (objChr == null && !(obj instanceof BaseVehicle))) {
                            if (distSq < twidthSq) {
                                CollisionManager.instance.AddContact(self, obj);
                                return;
                            }
                            continue;
                        }

                        if (objChr == null)
                            continue;

                        IsoPlayer objPlyr = (IsoPlayer) Type.tryCastTo(obj, IsoPlayer.class);
                        IsoZombie objZombie = (IsoZombie) Type.tryCastTo(obj, IsoZombie.class);

                        // Spear charge logic
                        if (thisPlyr != null && thisPlyr.getBumpedChr() != obj) {
                            if (distSq < range * range) {
                                float len = (float) Math.sqrt(distSq);
                                tempo.x = dx;
                                tempo.y = dy;
                                if (thisPlyr.getForwardDirection().angleBetween(tempo) > 2.6179938155736564d
                                        && thisPlyr.getBeenSprintingFor() >= 70.0f
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
                                boolean wasBumped = !self.isOnFloor() && (thisChr.getBumpedChr() != null
                                        || (System.currentTimeMillis() - thisPlyr.getLastBump()) / 100 < 15
                                        || thisPlyr.isSprinting()) && (objPlyr == null || !objPlyr.isNPC());
                                if (wasBumped) {
                                    thisChr.bumpNbr++;
                                    int baseChance = (((10 - (thisChr.bumpNbr * 3))
                                            + thisChr.getPerkLevel(PerkFactory.Perks.Fitness))
                                            + thisChr.getPerkLevel(PerkFactory.Perks.Strength))
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
                                    if (Rand.Next(Math.max(1, Math.min(80, baseChance))) == 0
                                            || thisChr.isSprinting()) {
                                        thisChr.setVariable("BumpDone", false);
                                        thisChr.setBumpFall(true);
                                        thisChr.setVariable("TripObstacleType", "zombie");
                                    }
                                } else {
                                    thisChr.bumpNbr = 0;
                                }
                                thisChr.setLastBump(System.currentTimeMillis());
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
