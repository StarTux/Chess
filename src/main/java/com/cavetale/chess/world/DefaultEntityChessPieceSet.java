package com.cavetale.chess.world;

import com.cavetale.chess.board.ChessColor;
import com.cavetale.chess.board.ChessPiece;
import com.cavetale.chess.board.ChessSquare;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Skull;
import io.papermc.paper.entity.TeleportFlag;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Panda;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Warden;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public final class DefaultEntityChessPieceSet {
    public WorldChessPiece place(WorldChessBoard board, ChessSquare square, ChessPiece piece) {
        if (board.getFacingAxis() != Axis.Y) {
            throw new IllegalArgumentException("Y axis facing required!");
        }
        final var location = getLocation(board, square, piece);
        final Color leatherColor = piece.getColor() == ChessColor.WHITE
            ? Color.WHITE
            : Color.BLACK;
        return switch (piece.type) {
        case PAWN -> {
            final Zombie zombie = location.getWorld().spawn(location, Zombie.class, z -> {
                    applyEntity(z);
                    z.setShouldBurnInDay(false);
                    z.setBaby();
                    z.getEquipment().setHelmet(leatherArmor(Material.LEATHER_HELMET, leatherColor));
                    z.getEquipment().setChestplate(leatherArmor(Material.LEATHER_CHESTPLATE, leatherColor));
                    z.getEquipment().setLeggings(leatherArmor(Material.LEATHER_LEGGINGS, leatherColor));
                    z.getEquipment().setBoots(leatherArmor(Material.LEATHER_BOOTS, leatherColor));
                });
            yield new DefaultPiece(zombie, board, piece);
        }
        case KNIGHT -> {
            final Horse horse = location.getWorld().spawn(location, Horse.class, h -> {
                    applyEntity(h);
                    h.setColor(piece.color == ChessColor.WHITE
                               ? Horse.Color.WHITE
                               : Horse.Color.BLACK);
                    h.setStyle(Horse.Style.NONE);
                    h.getInventory().setArmor(leatherArmor(Material.LEATHER_HORSE_ARMOR, leatherColor));
                });
            final AbstractSkeleton rider = piece.color == ChessColor.WHITE
                ? spawnSkeleton(location)
                : spawnWitherSkeleton(location);
            horse.addPassenger(rider);
            rider.getEquipment().setHelmet(piece.color == ChessColor.WHITE
                                           ? new ItemStack(Material.IRON_HELMET)
                                           : new ItemStack(Material.NETHERITE_HELMET));
            rider.getEquipment().setItemInMainHand(piece.color == ChessColor.WHITE
                                                   ? new ItemStack(Material.IRON_SWORD)
                                                   : new ItemStack(Material.NETHERITE_SWORD));
            rider.getEquipment().setItemInOffHand(piece.color == ChessColor.WHITE
                                                  ? new ItemStack(Material.SHIELD)
                                                  : new ItemStack(Material.SHIELD));
            yield new EntityChessPiece() {
                @Override
                public List<Entity> getEntities() {
                    return List.of(horse, rider);
                }

                @Override
                public void remove() {
                    horse.remove();
                    rider.remove();
                }

                @Override
                public void move(ChessSquare square) {
                    horse.teleport(getLocation(board, square, piece), TeleportFlag.EntityState.RETAIN_PASSENGERS);
                }
            };
        }
        case BISHOP -> {
            final AbstractSkeleton skeleton = piece.color == ChessColor.WHITE
                ? spawnSkeleton(location)
                : spawnWitherSkeleton(location);
            skeleton.getEquipment().setHelmet(piece.color == ChessColor.WHITE
                                              ? Mytems.WHITE_WITCH_HAT.createItemStack()
                                              : Mytems.BLACK_WITCH_HAT.createItemStack());
            skeleton.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
            yield new DefaultPiece(skeleton, board, piece);
        }
        case ROOK -> {
            final Entity entity = piece.color == ChessColor.WHITE
                ? spawnPolarBear(location)
                : spawnPanda(location);
            yield new DefaultPiece(entity, board, piece);
        }
        case QUEEN -> {
            final var armorStand = spawnArmorStand(location, leatherColor);
            armorStand.getEquipment().setItemInMainHand(piece.color == ChessColor.WHITE
                                                        ? Mytems.ICE_STAFF.createItemStack()
                                                        : Mytems.DR_ACULA_STAFF.createItemStack());
            stretchRightArm(armorStand);
            armorStand.getEquipment().setHelmet(piece.color == ChessColor.WHITE
                                                ? whiteQueenSkull()
                                                : blackQueenSkull());
            yield new DefaultPiece(armorStand, board, piece);
        }
        case KING -> {
            final Entity entity = piece.color == ChessColor.WHITE
                ? location.getWorld().spawn(location, IronGolem.class, e -> {
                        applyEntity(e);
                    })
                : location.getWorld().spawn(location, Warden.class, e -> {
                        applyEntity(e);
                    });
            yield new DefaultPiece(entity, board, piece);
        }
        default -> throw new IllegalStateException("piece.type=" + piece.type);
        };
    }

    private static ArmorStand spawnArmorStand(Location location, Color color) {
        return location.getWorld().spawn(location, ArmorStand.class, as -> {
                applyEntity(as);
                as.getEquipment().setChestplate(leatherArmor(Material.LEATHER_CHESTPLATE, color));
                as.getEquipment().setLeggings(leatherArmor(Material.LEATHER_LEGGINGS, color));
                as.getEquipment().setBoots(leatherArmor(Material.LEATHER_BOOTS, color));
                as.setBasePlate(false);
                as.setArms(true);
                as.setCanMove(false);
                as.setCanTick(false);
                as.setGravity(false);
                as.setDisabledSlots(EquipmentSlot.values());
            });
    }

    private static Location getLocation(WorldChessBoard board, ChessSquare square, ChessPiece piece) {
        final var result = board.getCenterLocation(square);
        float yaw = switch (board.getFaceBoardY()) {
        case NORTH -> 180f;
        case SOUTH -> 0f;
        case WEST -> 90f;
        case EAST -> 270f;
        default -> 0f;
        };
        if (piece.color == ChessColor.BLACK) yaw += 180f;
        result.setYaw(yaw);
        return result;
    }

    private static ItemStack leatherArmor(Material material, Color color) {
        ItemStack result = new ItemStack(material);
        result.editMeta(m -> {
                if (!(m instanceof LeatherArmorMeta meta)) return;
                meta.setColor(color);
            });
        return result;
    }

    private static void stretchRightArm(ArmorStand armorStand) {
        armorStand.setRightArmPose(armorStand.getRightArmPose().setX(Math.PI * -0.5));
    }

    private static Skeleton spawnSkeleton(Location location) {
        return location.getWorld().spawn(location, Skeleton.class, s -> {
                applyEntity(s);
                s.setShouldBurnInDay(false);
                s.getEquipment().setHelmet(null);
                s.getEquipment().setChestplate(null);
                s.getEquipment().setLeggings(null);
                s.getEquipment().setBoots(null);
                s.getEquipment().setItemInMainHand(null);
                s.getEquipment().setItemInOffHand(null);
            });
    }

    private static WitherSkeleton spawnWitherSkeleton(Location location) {
        return location.getWorld().spawn(location, WitherSkeleton.class, s -> {
                applyEntity(s);
                s.setShouldBurnInDay(false);
                s.getEquipment().setHelmet(null);
                s.getEquipment().setChestplate(null);
                s.getEquipment().setLeggings(null);
                s.getEquipment().setBoots(null);
                s.getEquipment().setItemInMainHand(null);
                s.getEquipment().setItemInOffHand(null);
            });
    }

    private static PolarBear spawnPolarBear(Location location) {
        return location.getWorld().spawn(location, PolarBear.class, b -> {
                applyEntity(b);
            });
    }

    private static Panda spawnPanda(Location location) {
        return location.getWorld().spawn(location, Panda.class, b -> {
                applyEntity(b);
                b.setMainGene(Panda.Gene.NORMAL);
                b.setHiddenGene(Panda.Gene.NORMAL);
            });
    }

    private static ItemStack whiteQueenSkull() {
        return Skull.create("Snow queen",
                            UUID.fromString("299db46a-638e-46b1-8027-d49b362dcc81"),
                            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2EzZGFmMjFhMGE3Njk0YmViYTg0ODU2NGRmNGM3ZmM1MjZmYjQ5NjY5NWZkNWE4Y2VmYWVmM2EwZWVkM2NjYSJ9fX0=");
    }

    private static ItemStack blackQueenSkull() {
        return Skull.create("Queen",
                            UUID.fromString("60e12380-5ae4-4650-a8b7-bd19c9b29642"),
                            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTdlY2QxMWRkN2UwYWVmNDZhZTQzMTE3NWE0OGQzMmVkYmU3ZmQzOTNkMTY5Mzc1NzUyOTA4NjI3M2YyZiJ9fX0=");
    }

    private static void applyEntity(Entity entity) {
        EntityChessPiece.markAsChessPiece(entity);
        entity.setPersistent(false);
        entity.setSilent(true);
        if (entity instanceof LivingEntity living) {
            living.setCollidable(false);
        }
        if (entity instanceof Mob mob) {
            mob.setAware(false);
            Bukkit.getMobGoals().removeAllGoals(mob);
        }
    }

    @Data
    @RequiredArgsConstructor
    private static final class DefaultPiece implements EntityChessPiece {
        private final Entity entity;
        private final WorldChessBoard board;
        private final ChessPiece piece;

        @Override
        public List<Entity> getEntities() {
            return List.of(entity);
        }

        @Override
        public void move(ChessSquare square) {
            entity.teleport(getLocation(board, square, piece));
        }

        @Override
        public void remove() {
            entity.remove();
        }
    };
}
