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
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Banner;
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
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import static com.cavetale.mytems.util.Entities.setTransient;

public final class DefaultEntityChessPieceSet implements ChessPieceSet {
    @Override
    public boolean canSupport(WorldChessBoard board) {
        return board.getFacingAxis() == Axis.Y;
    }

    @Override
    public WorldChessPiece place(WorldChessBoard board, ChessSquare square, ChessPiece piece) {
        if (!canSupport(board)) {
            throw new IllegalArgumentException("Y axis facing required!");
        }
        final var location = getLocation(board, square, piece);
        final Color leatherColor = piece.getColor() == ChessColor.WHITE
            ? Color.WHITE
            : Color.BLACK;
        return switch (piece.type) {
        case PAWN -> {
            final Zombie zombie = location.getWorld().spawn(location, Zombie.class, false, z -> {
                    applyEntity(z);
                    z.setShouldBurnInDay(false);
                    z.setBaby();
                    z.getEquipment().setHelmet(leatherArmor(Material.LEATHER_HELMET, leatherColor));
                    z.getEquipment().setChestplate(leatherArmor(Material.LEATHER_CHESTPLATE, leatherColor));
                    z.getEquipment().setLeggings(leatherArmor(Material.LEATHER_LEGGINGS, leatherColor));
                    z.getEquipment().setBoots(leatherArmor(Material.LEATHER_BOOTS, leatherColor));
                });
            yield new DefaultPiece(piece, zombie, board);
        }
        case KNIGHT -> {
            final Horse horse = location.getWorld().spawn(location, Horse.class, false, h -> {
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
            Worlds.worlds().setAllowVehicleEnter(true);
            horse.addPassenger(rider);
            Worlds.worlds().setAllowVehicleEnter(false);
            rider.getEquipment().setHelmet(piece.color == ChessColor.WHITE
                                           ? new ItemStack(Material.IRON_HELMET)
                                           : new ItemStack(Material.NETHERITE_HELMET));
            rider.getEquipment().setItemInMainHand(piece.color == ChessColor.WHITE
                                                   ? new ItemStack(Material.IRON_SWORD)
                                                   : new ItemStack(Material.NETHERITE_SWORD));
            final var shield = new ItemStack(Material.SHIELD);
            shield.editMeta(meta -> {
                    if (!(meta instanceof BlockStateMeta blockStateMeta)) return;
                    if (!(blockStateMeta.getBlockState() instanceof Banner banner)) return;
                    banner.setBaseColor(piece.color == ChessColor.WHITE ? DyeColor.WHITE : DyeColor.BLACK);
                    blockStateMeta.setBlockState(banner);
                });
            rider.getEquipment().setItemInOffHand(shield);
            yield new MountedPiece(piece, rider, horse, board);
        }
        case BISHOP -> {
            final AbstractSkeleton skeleton = piece.color == ChessColor.WHITE
                ? spawnSkeleton(location)
                : spawnWitherSkeleton(location);
            skeleton.getEquipment().setHelmet(piece.color == ChessColor.WHITE
                                              ? Mytems.WHITE_WITCH_HAT.createItemStack()
                                              : Mytems.BLACK_WITCH_HAT.createItemStack());
            skeleton.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
            yield new DefaultPiece(piece, skeleton, board);
        }
        case ROOK -> {
            final Entity entity = piece.color == ChessColor.WHITE
                ? spawnPolarBear(location)
                : spawnPanda(location);
            yield new DefaultPiece(piece, entity, board);
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
            yield new DefaultPiece(piece, armorStand, board);
        }
        case KING -> {
            final Entity entity = piece.color == ChessColor.WHITE
                ? location.getWorld().spawn(location, IronGolem.class, false, e -> {
                        applyEntity(e);
                    })
                : location.getWorld().spawn(location, Warden.class, false, e -> {
                        applyEntity(e);
                    });
            yield new DefaultPiece(piece, entity, board);
        }
        default -> throw new IllegalStateException("piece.type=" + piece.type);
        };
    }

    private static ArmorStand spawnArmorStand(Location location, Color color) {
        return location.getWorld().spawn(location, ArmorStand.class, false, as -> {
                applyEntity(as);
                as.getEquipment().setChestplate(leatherArmor(Material.LEATHER_CHESTPLATE, color));
                as.getEquipment().setLeggings(leatherArmor(Material.LEATHER_LEGGINGS, color));
                as.getEquipment().setBoots(leatherArmor(Material.LEATHER_BOOTS, color));
                as.setBasePlate(false);
                as.setArms(true);
                as.setCanMove(false);
                as.setCanTick(false);
                as.setGravity(false);
                as.setDisabledSlots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
                                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
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
        return location.getWorld().spawn(location, Skeleton.class, false, s -> {
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
        return location.getWorld().spawn(location, WitherSkeleton.class, false, s -> {
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
        return location.getWorld().spawn(location, PolarBear.class, false, b -> {
                applyEntity(b);
            });
    }

    private static Panda spawnPanda(Location location) {
        return location.getWorld().spawn(location, Panda.class, false, b -> {
                applyEntity(b);
                b.setMainGene(Panda.Gene.NORMAL);
                b.setHiddenGene(Panda.Gene.NORMAL);
            });
    }

    private static ItemStack whiteQueenSkull() {
        return Skull.create("SnowQueen",
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
        setTransient(entity);
        entity.setSilent(true);
        if (entity instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false);
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
        private final ChessPiece chessPiece;
        private final Entity entity;
        private final WorldChessBoard board;

        @Override
        public List<Entity> getEntities() {
            return List.of(entity);
        }

        @Override
        public void move(ChessSquare square) {
            entity.teleport(getLocation(board, square, chessPiece));
        }

        @Override
        public void remove() {
            entity.remove();
        }

        @Override
        public void explode() {
            final double height = entity.getHeight();
            final var location = entity.getLocation().add(0.0, height * 0.5, 0.0);
            location.getWorld().spawnParticle(Particle.BLOCK, location, 128,
                                              0.25, height * 0.25, 0.25, 0.0,
                                              (chessPiece.color == ChessColor.WHITE
                                               ? Material.WHITE_CONCRETE.createBlockData()
                                               : Material.BLACK_CONCRETE.createBlockData()));
        }
    };

    @Data
    @RequiredArgsConstructor
    private static final class MountedPiece implements EntityChessPiece {
        private final ChessPiece chessPiece;
        private final Entity rider;
        private final Entity mount;
        private final WorldChessBoard board;

        @Override
        public List<Entity> getEntities() {
            return List.of(rider, mount);
        }

        @Override
        public void remove() {
            rider.remove();
            mount.remove();
        }

        @Override
        public void move(ChessSquare square) {
            mount.teleport(getLocation(board, square, chessPiece), TeleportFlag.EntityState.RETAIN_PASSENGERS);
        }

        @Override
        public void explode() {
            final double height = rider.getHeight();
            final var location = rider.getLocation().add(0.0, height * 0.5, 0.0);
            location.getWorld().spawnParticle(Particle.BLOCK, location, 128,
                                              0.25, height * 0.25, 0.25, 0.0,
                                              (chessPiece.color == ChessColor.WHITE
                                               ? Material.WHITE_CONCRETE.createBlockData()
                                               : Material.BLACK_CONCRETE.createBlockData()));
        }
    }
}
