package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Accessor;
import me.zed_0xff.zombie_buddy.Patch;

import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.CollisionManager;
import zombie.core.Rand;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
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
 * 3. Local Variable Caching: Caches self.getZ(), getNextX(), getWidth() etc. to avoid repeated method calls.
 * 4. Identity Skip: Quickly skips self-comparison.
 * 5. Lazy Type Casting: Moves expensive Type.tryCastTo() checks inside the proximity check.
 * 6. Forward Direction Optimization: Uses direct X/Y components to avoid Vector2 object allocations.
 */
@Patch(className = "zombie.iso.IsoMovingObject", methodName = "separate")
public class Patch_IsoMovingObject {
    public static final Vector2 tempo = initTempo();

    public static Vector2 initTempo() {
        Vector2 v = Accessor.tryGet(null, Accessor.findField(IsoMovingObject.class, "tempo"), (Vector2) null);
        return v != null ? v : new Vector2();
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

    public static void optimized_separate(Object selfObj) {
        IsoMovingObject self = (IsoMovingObject) selfObj;

        if (!self.isSolidForSeparate() || !self.isPushableForSeparate()) {
            return;
        }

        IsoGameCharacter isoGameCharacter = (IsoGameCharacter) Type.tryCastTo(self, IsoGameCharacter.class);
        IsoPlayer isoPlayer = (IsoPlayer) Type.tryCastTo(self, IsoPlayer.class);
        if (self.z < 0.0f) {
            self.z = 0.0f;
        }

        // Cache per-self values (avoids repeated getters in hot loop)
        final float selfWidth = self.getWidth();
        final float selfZ = self.z;
        final float selfNx = self.nx;
        final float selfNy = self.ny;
        final IsoGridSquare currentSquare = self.getCurrentSquare();

        // Max range depends only on self; compute once
        final float maxRange = (isoPlayer != null && (isoPlayer.getPrimaryHandItem() instanceof HandWeapon))
                ? ((HandWeapon) isoPlayer.getPrimaryHandItem()).getMaxRange()
                : 0.3f;

        for (int i = 0; i <= 8; i++) {
            IsoGridSquare isoGridSquare = (i == 8) ? currentSquare : currentSquare.nav[i];
            if (isoGridSquare == null || isoGridSquare.getMovingObjects().isEmpty()) {
                continue;
            }
            if (isoGridSquare != currentSquare && currentSquare.isBlockedTo(isoGridSquare)) {
                continue;
            }

            java.util.ArrayList<IsoMovingObject> movingObjects = isoGridSquare.getMovingObjects();
            final int size = movingObjects.size();
            for (int i2 = 0; i2 < size; i2++) {
                IsoMovingObject isoMovingObject = movingObjects.get(i2);
                if (isoMovingObject == self || !isoMovingObject.isSolidForSeparate()) {
                    continue;
                }
                if (Math.abs(selfZ - isoMovingObject.z) > 0.3f) {
                    continue;
                }

                float f2 = selfWidth + isoMovingObject.getWidth();
                float dx = selfNx - isoMovingObject.nx;
                float dy = selfNy - isoMovingObject.ny;
                float lengthSq = dx * dx + dy * dy;
                float f2Sq = f2 * f2;

                boolean otherIsCharacter = isoMovingObject instanceof IsoGameCharacter;
                boolean otherIsVehicle = isoMovingObject instanceof BaseVehicle;
                // Self not character, or other is neither character nor vehicle: simple contact then return
                if (isoGameCharacter == null || (!otherIsCharacter && !otherIsVehicle)) {
                    if (lengthSq < f2Sq) {
                        CollisionManager.instance.AddContact(self, isoMovingObject);
                    }
                    return;
                }

                if (!otherIsCharacter) {
                    continue;
                }
                IsoGameCharacter isoGameCharacter2 = (IsoGameCharacter) isoMovingObject;
                IsoPlayer isoPlayer2 = (isoMovingObject instanceof IsoPlayer) ? (IsoPlayer) isoMovingObject : null;

                // Need real length only when in range for charge or collision
                float rangeSq = (f2 + maxRange) * (f2 + maxRange);
                if (lengthSq >= rangeSq) {
                    continue;
                }
                Vector2 vector2 = tempo;
                vector2.x = dx;
                vector2.y = dy;
                float length = (float) Math.sqrt(lengthSq);

                if (isoPlayer != null && isoPlayer.getBumpedChr() != isoMovingObject && length < f2 + maxRange
                        && isoPlayer.getForwardDirection().angleBetween(vector2) > 2.6179938155736564d
                        && isoPlayer.getBeenSprintingFor() >= 70.0f && WeaponType.getWeaponType(isoPlayer) == WeaponType.spear) {
                    isoPlayer.reportEvent("ChargeSpearConnect");
                    isoPlayer.setAttackType("charge");
                    isoPlayer.attackStarted = true;
                    isoPlayer.setVariable("StartedAttackWhileSprinting", true);
                    isoPlayer.setBeenSprintingFor(0.0f);
                    return;
                }
                if (length < f2) {
                                boolean z = false;
                                if (isoPlayer != null && isoPlayer.getVariableFloat("WalkSpeed", 0.0f) > 0.2f && isoPlayer.runningTime > 0.5f && isoPlayer.getBumpedChr() != isoMovingObject) {
                                    z = true;
                                }
                                if (GameClient.bClient && isoPlayer != null && (isoGameCharacter2 instanceof IsoPlayer) && !ServerOptions.getInstance().PlayerBumpPlayer.getValue()) {
                                    z = false;
                                }
                                if (z && !"charge".equals(isoPlayer.getAttackType())) {
                                    boolean z2 = !self.isOnFloor() && (isoGameCharacter.getBumpedChr() != null || (System.currentTimeMillis() - isoPlayer.getLastBump()) / 100 < 15 || isoPlayer.isSprinting()) && (isoPlayer2 == null || !isoPlayer2.isNPC());
                                    if (z2) {
                                        isoGameCharacter.bumpNbr++;
                                        int perkLevel = (10 - (isoGameCharacter.bumpNbr * 3)) + isoGameCharacter.getPerkLevel(PerkFactory.Perks.Fitness) + isoGameCharacter.getPerkLevel(PerkFactory.Perks.Strength);
                                        if (isoGameCharacter.Traits.Clumsy.isSet()) {
                                            perkLevel -= 5;
                                        }
                                        if (isoGameCharacter.Traits.Graceful.isSet()) {
                                            perkLevel += 5;
                                        }
                                        if (isoGameCharacter.Traits.VeryUnderweight.isSet()) {
                                            perkLevel -= 8;
                                        }
                                        if (isoGameCharacter.Traits.Underweight.isSet()) {
                                            perkLevel -= 4;
                                        }
                                        if (isoGameCharacter.Traits.Obese.isSet()) {
                                            perkLevel -= 8;
                                        }
                                        if (isoGameCharacter.Traits.Overweight.isSet()) {
                                            perkLevel -= 4;
                                        }
                                        BodyPart bodyPart = isoGameCharacter.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
                                        if (bodyPart.getAdditionalPain(true) > 20.0f) {
                                            perkLevel = (int) (perkLevel - ((bodyPart.getAdditionalPain(true) - 20.0f) / 20.0f));
                                        }
                                        if (Rand.Next(Math.max(1, Math.min(80, perkLevel))) == 0 || isoGameCharacter.isSprinting()) {
                                            isoGameCharacter.setVariable("BumpDone", false);
                                            isoGameCharacter.setBumpFall(true);
                                            isoGameCharacter.setVariable("TripObstacleType", "zombie");
                                        }
                                    } else {
                                        isoGameCharacter.bumpNbr = 0;
                                    }
                                    isoGameCharacter.setLastBump(System.currentTimeMillis());
                                    isoGameCharacter.setBumpedChr(isoGameCharacter2);
                                    isoGameCharacter.setBumpType(self.getBumpedType(isoGameCharacter2));
                                    boolean zIsBehind = isoGameCharacter.isBehind(isoGameCharacter2);
                                    String bumpType = isoGameCharacter.getBumpType();
                                    if (zIsBehind) {
                                        if (bumpType.equals("left")) {
                                            bumpType = "right";
                                        } else {
                                            bumpType = "left";
                                        }
                                    }
                                    isoGameCharacter2.setBumpType(bumpType);
                                    isoGameCharacter2.setHitFromBehind(zIsBehind);
                                    if (z2 | GameClient.bClient) {
                                        isoGameCharacter.actionContext.reportEvent("wasBumped");
                                    }
                                }
                                if (GameServer.bServer || self.distToNearestCamCharacter() < 60.0f) {
                                    if (self.isPushedByForSeparate(isoMovingObject)) {
                                        vector2.setLength((length - f2) / 8.0f);
                                        self.nx -= vector2.x;
                                        self.ny -= vector2.y;
                                    }
                                    self.collideWith(isoMovingObject);
                                }
                            }
                        }
            }
    }
}
