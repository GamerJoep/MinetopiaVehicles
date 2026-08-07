package nl.mtvehicles.core.movement;

import com.google.common.collect.Sets;
import nl.mtvehicles.core.Main;
import nl.mtvehicles.core.events.HornUseEvent;
import nl.mtvehicles.core.events.TankShootEvent;
import nl.mtvehicles.core.events.VehicleRegionEnterEvent;
import nl.mtvehicles.core.events.VehicleRegionLeaveEvent;
import nl.mtvehicles.core.infrastructure.annotations.ToDo;
import nl.mtvehicles.core.infrastructure.annotations.VersionSpecific;
import nl.mtvehicles.core.infrastructure.dataconfig.DefaultConfig;
import nl.mtvehicles.core.infrastructure.dataconfig.VehicleDataConfig;
import nl.mtvehicles.core.infrastructure.enums.*;
import nl.mtvehicles.core.infrastructure.modules.ConfigModule;
import nl.mtvehicles.core.infrastructure.modules.DependencyModule;
import nl.mtvehicles.core.infrastructure.utils.BossBarUtils;
import nl.mtvehicles.core.infrastructure.vehicle.VehicleData;
import nl.mtvehicles.core.infrastructure.vehicle.VehicleUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static nl.mtvehicles.core.Main.instance;
import static nl.mtvehicles.core.Main.schedulerRun;
import static nl.mtvehicles.core.infrastructure.modules.VersionModule.getServerVersion;
import static nl.mtvehicles.core.movement.PacketHandler.isObjectPacket;

/**
 * Class concerning the movement of vehicles
 */
public class VehicleMovement {
    /**
     * Given steering packet, checked.
     * @see PacketHandler#isObjectPacket(Object)
     */
    protected Object packet;

    /**
     * Player who is steering the vehicle.
     */
    protected Player player;
    /**
     * The type of vehicle. Uses an enum (not a String as it used to be).
     * @see VehicleType
     */
    protected VehicleType vehicleType;
    /**
     * The vehicle's license plate.
     */
    protected String license;

    /**
     * Vehicle's main armor stand (moved)
     */
    protected ArmorStand standMain;
    /**
     * Vehicle's armor stand with its texture
     */
    protected ArmorStand standSkin;
    /**
     * Vehicle's armor stand for the main seat (where the driver is seated)
     */
    protected ArmorStand standMainSeat;
    /**
     * Vehicle's armor stand for helicopter blades
     */
    protected @Nullable ArmorStand standRotors;

    /**
     * True if a flying vehicle is falling from the air.
     */
    protected boolean isFalling = false;
    /**
     * True if extreme falling is turned on in config.yml and 'isFalling' is true.
     * This makes falling faster, vehicles break when they land, etc...
     */
    protected boolean extremeFalling = false;

    protected boolean headlightsEnabled = false;


    /**
     * Main method for vehicles' movement
     * @param player Player who is steering the vehicle
     * @param packet Packet used for steering (will be checked)
     *
     * @see PacketHandler#isObjectPacket(Object)
     */
    public void vehicleMovement(Player player, Object packet) {

        //Do not continue if the correct packet has not been given
        if (!isObjectPacket(packet)) return;
        this.packet = packet;

        this.player = player;
        AtomicLong lastUsed = new AtomicLong(0L);

        if (player.getVehicle() == null) return;
        final Entity vehicle = player.getVehicle();

        if (!(vehicle instanceof ArmorStand)) return;
        if (vehicle.getCustomName() == null) return;

        if (vehicle.getCustomName().replace("MTVEHICLES_MAINSEAT_", "").isEmpty()) return; //Not sure what this line is supposed to be doing here but I'm keeping it, just in case
        this.license = vehicle.getCustomName().replace("MTVEHICLES_MAINSEAT_", "");

        if (VehicleData.autostand.get("MTVEHICLES_MAIN_" + license) == null) return;

        if (VehicleData.speed.get(license) == null) {
            VehicleData.speed.put(license, 0.0);
            return;
        }

        vehicleType = VehicleData.type.get(license);
        if (vehicleType == null) return;

        if (VehicleData.fuel.get(license) < 1) {
            BossBarUtils.setBossBarValue(0 / 100.0D, license);
            if (vehicleType.canFly()) {
                isFalling = true;
                extremeFalling = vehicleType.isHelicopter() && (boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.EXTREME_HELICOPTER_FALL);
            }
            else return;
        }

        BossBarUtils.setBossBarValue(VehicleData.fuel.get(license) / 100.0D, license);

        standMain = VehicleData.autostand.get("MTVEHICLES_MAIN_" + license);
        standSkin = VehicleData.autostand.get("MTVEHICLES_SKIN_" + license);
        standMainSeat = VehicleData.autostand.get("MTVEHICLES_MAINSEAT_" + license);
        standRotors = VehicleData.autostand.get("MTVEHICLES_WIEKENS_" + license);

        if (ConfigModule.vehicleDataConfig.getHealth(license) == 0) { // The vehicle is broken
            standMain.getWorld().spawnParticle(Particle.SMOKE_NORMAL, standMain.getLocation(), 2);

            if (!VehicleData.isVehicleDestroyed(license)) {
                if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.EXPLODING_VEHICLE)) {
                    Bukkit.getScheduler().runTask(instance, () -> {
                        standMain.getLocation().add(0, 0,0).createExplosion(2);
                        VehicleData.markVehicleAsDestroyed(license);
                    });
                }
                if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.DESTRUCTIBLE_VEHICLE)) {
                    Bukkit.getScheduler().runTask(instance, () -> {
                        String l = license;
                        VehicleUtils.despawnVehicle(l);
                        VehicleUtils.getVehicle(l).delete();

                    });
                }
            }
            return;
        }

        if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.HEADLIGHTS_ENABLED) && getServerVersion().isNewerOrEqualTo(ServerVersion.v1_17_R1)){headlightsEnabled = true;}


        schedulerRun(() -> {
            standSkin.teleport(new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY(), standMain.getLocation().getZ(), standMain.getLocation().getYaw(), standMain.getLocation().getPitch()));
            if (DependencyModule.isDependencyEnabled(SoftDependency.WORLD_GUARD)){
                Set<String> newRegions = DependencyModule.worldGuard.getRegionNames(standMain.getLocation());
                if (VehicleData.lastRegions.containsKey(license)){
                    Set<String> lastRegions = VehicleData.lastRegions.get(license);

                    if (!ConfigModule.defaultConfig.canProceedWithAction(RegionAction.RIDE, vehicleType, standMain.getLocation(), player)) {
                        player.getVehicle().eject();
                        VehicleData.speed.put(license, 0.0);
                        ConfigModule.messagesConfig.sendMessage(player, Message.CANNOT_DO_THAT_HERE);
                        return;
                    }

                    for (String leftRegion: Sets.difference(lastRegions, newRegions)) {
                        VehicleRegionLeaveEvent event = new VehicleRegionLeaveEvent(license, leftRegion);
                        event.setPlayer(player);
                        event.call();
                        if (event.isCancelled()) {
                            player.getVehicle().eject();
                            VehicleData.speed.put(license, 0.0);;
                            Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
                                standMain.teleport(new Location(player.getWorld(), player.getLocation().getX(), standMain.getLocation().getY(), player.getLocation().getZ()));
                                standSkin.teleport(new Location(player.getWorld(), player.getLocation().getX(), standSkin.getLocation().getY(), player.getLocation().getZ()));
                            }, 5L);
                            return;
                        }

                    }

                    for (String enteredRegion : Sets.difference(newRegions, lastRegions)) {
                        VehicleRegionEnterEvent event = new VehicleRegionEnterEvent(license, enteredRegion);
                        event.setPlayer(player);
                        event.call();
                        if (event.isCancelled()) {
                            player.getVehicle().eject();
                            VehicleData.speed.put(license, 0.0);
                            Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
                                standMain.teleport(new Location(player.getWorld(), player.getLocation().getX(), standMain.getLocation().getY(), player.getLocation().getZ()));
                                standSkin.teleport(new Location(player.getWorld(), player.getLocation().getX(), standSkin.getLocation().getY(), player.getLocation().getZ()));
                            }, 5L);
                            return;
                        }
                    }

                }
                VehicleData.lastRegions.put(license, newRegions);
            }

            updateStand();
            if (!vehicleType.isHelicopter()) blockCheck();
            mainSeat();

            if (VehicleData.seatsize.get(license + "addon") != null) {
                for (int i = 1; i <= VehicleData.seatsize.get(license + "addon"); i++) {
                    ArmorStand standAddon = VehicleData.autostand.get("MTVEHICLES_ADDON" + i + "_" + license);
                    //schedulerRun(() -> standAddon.teleport(standMain.getLocation()));
                    standAddon.teleport(standMain.getLocation());
                }
            }


            if (vehicleType.isHelicopter()) rotors();

            // Horn
            if (ConfigModule.vehicleDataConfig.isHornEnabled(license) && steerIsJumping() && !isFalling) {
                if (VehicleData.lastUsage.containsKey(player.getName())) lastUsed.set(VehicleData.lastUsage.get(player.getName()));

                if (System.currentTimeMillis() - lastUsed.get() >= Long.parseLong(ConfigModule.defaultConfig.get(DefaultConfig.Option.HORN_COOLDOWN).toString()) * 1000L) {
                    HornUseEvent api = new HornUseEvent(license);
                    api.setPlayer(player);
                    schedulerRun(api::call);

                    if (!api.isCancelled()){
                        standMain.getWorld().playSound(standMain.getLocation(), Objects.requireNonNull(ConfigModule.defaultConfig.get(DefaultConfig.Option.HORN_TYPE).toString()), 0.9f, 1f);
                        VehicleData.lastUsage.put(player.getName(), System.currentTimeMillis());
                    }
                }
            }

            // Tank
            if (vehicleType.isTank() && steerIsJumping()) {
                if (VehicleData.lastUsage.containsKey(player.getName())) lastUsed.set(VehicleData.lastUsage.get(player.getName()));

                if (System.currentTimeMillis() - lastUsed.get() >= Long.parseLong(ConfigModule.defaultConfig.get(DefaultConfig.Option.TANK_COOLDOWN).toString()) * 1000L) {
                    standMain.getWorld().playEffect(standMain.getLocation(), Effect.BLAZE_SHOOT, 1, 1);
                    standMain.getWorld().playEffect(standMain.getLocation(), Effect.GHAST_SHOOT, 1, 1);
                    standMain.getWorld().playEffect(standMain.getLocation(), Effect.WITHER_BREAK_BLOCK, 1, 1);
                    double xOffset = 4;
                    double yOffset = 1.6;
                    double zOffset = 0;
                    Location locvp = standMain.getLocation().clone();
                    Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
                    float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(fbvp.getYaw())));
                    float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(fbvp.getYaw())));
                    Location loc = new Location(standMain.getWorld(), xvp, standMain.getLocation().getY() + yOffset, zvp, fbvp.getYaw(), fbvp.getPitch());
                    spawnParticles(standMain, loc);
                    tankShoot(standMain, loc);
                    VehicleData.lastUsage.put(player.getName(), System.currentTimeMillis());
                }
            }
            if(headlightsEnabled && vehicle.getWorld().getTime() >= 13000 && vehicleType.isCar() && !vehicle.isEmpty()){
                addAndRemoveLight();
            }

            rotation();
            move();
        });
    }

    /**
     * Check the rotation of the vehicle
     */
    protected void rotation(){
        int rotationSpeed = VehicleData.getRotationSpeed(license);
        final Location locBelow = new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY() - 0.2, standMain.getLocation().getZ(), standMain.getLocation().getYaw(), standMain.getLocation().getPitch());

        final Material blockTypeBelow = locBelow.getBlock().getType();

        if (isFalling && vehicleType.isHelicopter()) return;
        if (vehicleType.isHelicopter() && !blockTypeBelow.equals(Material.AIR)) return;

        if (ConfigModule.defaultConfig.isIceSlippery() && blockTypeBelow.toString().contains("ICE"))
            rotationSpeed = rotationSpeed * 2;

        if (ConfigModule.defaultConfig.usePlayerFacingDriving()){
            rotateVehicle(player.getLocation().getYaw());
        } else {
            final int rotation = (VehicleData.speed.get(license) < 0.1) ? rotationSpeed / 3 : rotationSpeed;
            if (steerGetXxa() > 0) rotateVehicle(standMain.getLocation().getYaw() - rotation);
            else if (steerGetXxa() < 0) rotateVehicle(standMain.getLocation().getYaw() + rotation);
        }
    }

    /**
     * Rotate the vehicle to a specified yaw
     */
    protected void rotateVehicle(float yaw){
        schedulerRun(() -> {
            standMain.setRotation(yaw, standMain.getLocation().getPitch());
            standMainSeat.setRotation(yaw, standMain.getLocation().getPitch());
            standSkin.setRotation(yaw, standMain.getLocation().getPitch());
        });
    }

    /**
     * Check the movement of the vehicle
     */
    protected void move(){ // Forwards × Backwards
        final double maxSpeed = VehicleData.getSpeed(VehicleData.DataSpeed.MAXSPEED, license);
        final double accelerationSpeed = VehicleData.getSpeed(VehicleData.DataSpeed.ACCELERATION, license);
        final double brakingSpeed = VehicleData.getSpeed(VehicleData.DataSpeed.BRAKING, license);
        final double maxSpeedBackwards = VehicleData.getSpeed(VehicleData.DataSpeed.MAXSPEEDBACKWARDS, license);
        final Location locBelow = new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY() - 0.2, standMain.getLocation().getZ(), standMain.getLocation().getYaw(), standMain.getLocation().getPitch());

        if (steerGetZza() == 0.0 && !locBelow.getBlock().getType().equals(Material.AIR)) {
            putFrictionSpeed();
        }

        if (steerGetZza() > 0.0) {
            VehicleData.frictionBlocked.remove(license); //remove friction block if applicable
            if (VehicleData.speed.get(license) < 0) {
                VehicleData.speed.put(license, VehicleData.speed.get(license) + brakingSpeed);
                return;
            }
            putFuelUsage();

            if (VehicleData.speed.get(license) > maxSpeed - accelerationSpeed) return;
            VehicleData.speed.put(license, VehicleData.speed.get(license) + accelerationSpeed);
        }
        if (steerGetZza() < 0.0) {
            VehicleData.frictionBlocked.remove(license); //remove friction block if applicable
            if (VehicleData.speed.get(license) > 0) {
                VehicleData.speed.put(license, VehicleData.speed.get(license) - brakingSpeed);
                return;
            }
            putFuelUsage();

            if (VehicleData.speed.get(license) < -maxSpeedBackwards) return;
            VehicleData.speed.put(license, VehicleData.speed.get(license) - accelerationSpeed);
        }
    }

    /**
     * Slow down the vehicle due to friction
     */
    protected void putFrictionSpeed(){
        double frictionSpeed = VehicleData.getSpeed(VehicleData.DataSpeed.FRICTION, license);
        final String blockBelowName = new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY() - 0.2, standMain.getLocation().getZ(), standMain.getLocation().getYaw(), standMain.getLocation().getPitch()).getBlock().getType().toString();

        // Reduce friction on ice if slippery ice is set to true
        if (ConfigModule.defaultConfig.isIceSlippery() && blockBelowName.contains("ICE"))
            frictionSpeed = frictionSpeed * 0.5;

        BigDecimal round = BigDecimal.valueOf(VehicleData.speed.get(license)).setScale(1, BigDecimal.ROUND_DOWN);
        if (Double.parseDouble(String.valueOf(round)) == 0.0) {
            VehicleData.speed.put(license, 0.0);
            return;
        }
        if (Double.parseDouble(String.valueOf(round)) > 0.01) {
            VehicleData.speed.put(license, VehicleData.speed.get(license) - frictionSpeed);
            return;
        }
        if (Double.parseDouble(String.valueOf(round)) < 0.01) {
            VehicleData.speed.put(license, VehicleData.speed.get(license) + frictionSpeed);
        }
    }

    /**
     * Check the next block - carpets, slabs, snow - and do an appropriate action.
     * @return True if the vehicle is moving upwards (in any way)
     */
    @ToDo("Trapdoors")
    protected boolean blockCheck() {
        final Block currentBlock = standMain.getLocation().getBlock();
        final boolean isOnCarpet = currentBlock.getType().toString().contains("CARPET");
        if ((!isPassable(currentBlock) && !isOnCarpet) || !isPassable(standMain.getLocation().clone().add(0, 1, 0).getBlock())) {
            VehicleData.speed.put(license, 0.0);
            return false;
        }

        final Location loc = getLocationOfBlockAhead();
        final String locY = String.valueOf(standMain.getLocation().getY());
        final Location locBlockAbove = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ(), loc.getYaw(), loc.getPitch());
        final Location locBlockBelow = new Location(loc.getWorld(), loc.getX(), loc.getY() - 1, loc.getZ(), loc.getYaw(), loc.getPitch());
        final String drivingOnY = locY.substring(locY.length() - 2);

        final boolean isOnGround = drivingOnY.contains(".0");
        final boolean isOnSlab = drivingOnY.contains(".5");
        final boolean isPassable = isPassable(loc.getBlock());
        final boolean isAbovePassable = isPassable(locBlockAbove.getBlock());

        final double difference = Double.parseDouble("0." + locY.split("\\.")[1]);
        final BlockData blockData = loc.getBlock().getBlockData();
        final BlockData blockDataBelow = locBlockBelow.getBlock().getBlockData();


        if (standMain.getLocation().getBlock().getType().toString().contains("PATH") || standMain.getLocation().getBlock().getType().toString().contains("FARMLAND")){

            if (!isAbovePassable){
                VehicleData.speed.put(license, 0.0);
                return false;
            }

            if (!loc.getBlock().getType().toString().contains("PATH") && !loc.getBlock().getType().toString().contains("FARMLAND")) { //if block ahead isn't a path
                pushVehicleUp(0.0625);
                return true; //Vehicle will be pushed up
            }

            return false;

        }

        if (loc.getBlock().getType().toString().contains("CARPET")){ // If block ahead is a carpet
            if (!(boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.DRIVE_ON_CARPETS)){ // If carpets are turned off in config, stop
                VehicleData.speed.put(license, 0.0);
                return false;
            }

            if (!isAbovePassable) { // If there is a block above the carpet, stop
                VehicleData.speed.put(license, 0.0);
                return false;
            }

            if (isOnGround) pushVehicleUp(0.0625); // If the carpet is not placed on a slab, push the vehicle up
            return true;
        }

        if (blockData instanceof Snow){ // If block ahead is a snow layer (Does not include snow block - that's considered a full block.)
            final int layers = ((Snow) blockData).getLayers();
            double layerHeight = getLayerHeight(layers);
            if (VehicleData.speed.get(license) > 0.1) VehicleData.speed.put(license, 0.1); // Slow down on snow

            if (layerHeight == difference) return false; // If the player is already driving on a snow of the same height, vehicle will continue

            final double snowDifference = layerHeight - difference;
            pushVehicleUp(snowDifference); // Will push either up or down, depending on the difference

            return true;
        }

        // If the block ahead is a fence, wall or trapdoor, stop
        if (blockData instanceof Fence || loc.getBlock().getType().toString().contains("WALL") || blockData instanceof TrapDoor){
            VehicleData.speed.put(license, 0.0);
            return false;
        }

        // Slow down on Honey Blocks
        if (ConfigModule.defaultConfig.isHoneySlowdownEnabled()) {
            if (locBlockBelow.getBlock().getType() == Material.HONEY_BLOCK) {
                if (VehicleData.speed.get(license) > 0.05) VehicleData.speed.put(license, Math.max(VehicleData.speed.get(license) * 0.2, 0.05)); // Reduce speed to 20% of current speed or minimum 0.05
                return false;
            }
        }

        // Speed up on ice
        if (ConfigModule.defaultConfig.isIceSlippery()) {
            if (locBlockBelow.getBlock().getType().toString().contains("ICE")) {
                if (VehicleData.speed.get(license) > 0.05) VehicleData.speed.put(license, Math.max(VehicleData.speed.get(license) * 1.1, VehicleData.getSpeed(VehicleData.DataSpeed.MAXSPEED, license) * 1.2)); // Up speed by 10 %, maximum of 120 % of max speed
            }
        }

        // Pushing up on blocks/slabs
        if (ConfigModule.defaultConfig.driveUpSlabs().isSlabs()){ // If vehicles may only drive up slabs (and not blocks)
            if (isOnSlab) { // If the player is driving on a (bottom) slab

                if (isPassable) { // If the block ahead is air or another passable block (grass, flower, ...), vehicle will go down
                    pushVehicleDown(0.5);
                    return false;
                }

                if (blockData instanceof Slab) { // If the block ahead is a slab
                    Slab slab = (Slab) blockData;
                    if (slab.getType().equals(Slab.Type.BOTTOM)) { // If it is a bottom slab, continue
                        return false;
                    }
                    // Else (top/double slab) - treat it like a full block
                }

                if (!isAbovePassable) {
                    VehicleData.speed.put(license, 0.0);
                    return false; // Vehicle won't continue if there's a barrier above
                }


                pushVehicleUp(0.5); // Vehicle will go up if there's a full block or a top/double slab
                return true;
            }

            if (!isPassable) { // If block ahead is not air or another passable block (grass, flower, ...)

                if (blockData instanceof Slab) { // If it is a slab
                    Slab slab = (Slab) blockData;
                    if (slab.getType().equals(Slab.Type.BOTTOM)) {

                        if (!isAbovePassable) {
                            VehicleData.speed.put(license, 0.0);
                            return false; // Vehicle won't go up the slab if there's a barrier above
                        }

                        if (isOnGround) {
                            pushVehicleUp(0.5);
                        } else { // Maybe they're on a carpet
                            if ((0.5 - difference) > 0) pushVehicleUp(0.5 - difference);
                        }
                    }
                }

                VehicleData.speed.put(license, 0.0);
                return false; // If you're on the ground and there isn't bottom slab or a passable block, stop
            }

        } else if (ConfigModule.defaultConfig.driveUpSlabs().isBlocks()) { // If vehicles may only drive up blocks (and not slabs)

            if (!isOnSlab) { // If isn't placed on a slab
                if (!isPassable) { // If block ahead is not air or another passable block (grass, flower, ...)

                    if (blockData instanceof Slab){
                        Slab slab = (Slab) blockData;
                        if (slab.getType().equals(Slab.Type.BOTTOM)){
                            VehicleData.speed.put(license, 0.0);
                            return false; // If it's a bottom slab, stop.
                        }
                    }

                    if (!isAbovePassable) { // If more than 1 block high
                        VehicleData.speed.put(license, 0.0);
                        return false;
                    }

                    if (isOnGround) {
                        pushVehicleUp(1);
                    } else { // Maybe they're on a carpet
                        if ((1 - difference) > 0) pushVehicleUp(1 - difference);
                    }

                    return true;
                }
            }

            // If it's on a slab (might have been placed there)
            if (isPassable) { // If block ahead is air or another passable block (grass, flower, ...), vehicle will go down
                pushVehicleDown(0.5);
                return false;
            }

            if (blockData instanceof Slab) {
                Slab slab = (Slab) blockData;
                if (slab.getType().equals(Slab.Type.BOTTOM)) {
                    return false; // Vehicle will continue on the slabs
                }
            }

            if (!isAbovePassable) {
                VehicleData.speed.put(license, 0.0);
                return false; //Vehicle won't continue if there's a barrier above
            }

            pushVehicleUp(0.5); //Vehicle will go up if there's a full block or a top/double slab
            return true;

        } else if (ConfigModule.defaultConfig.driveUpSlabs().isBoth()) { // If vehicles may drive up both blocks and slabs

            if (isOnSlab) { // If vehicle is on a (bottom) slab

                if (isPassable) { // If block ahead is air or another passable block (grass, flower, ...), vehicle will go down
                    pushVehicleDown(0.5);
                    return false;
                }

                if (blockData instanceof Slab) {
                    Slab slab = (Slab) blockData;
                    if (slab.getType().equals(Slab.Type.BOTTOM)) {
                        return false; //Vehicle will continue on the slabs
                    }
                }

                if (!isAbovePassable) {
                    VehicleData.speed.put(license, 0.0);
                    return false; //Vehicle won't continue if there's a barrier above
                }

                pushVehicleUp(0.5); //Vehicle will go up if there's a full block or a top/double slab
                return true;
            }

            if (!isPassable) {

                if (!isAbovePassable) { //If more than 1 block high
                    VehicleData.speed.put(license, 0.0);
                    return false;
                }

                if (blockData instanceof Slab){
                    Slab slab = (Slab) blockData;
                    if (slab.getType().equals(Slab.Type.BOTTOM)){ //If it's a bottom slab
                        if (isOnGround) {
                            pushVehicleUp(0.5);
                        } else { //Maybe they're on a carpet
                            if ((0.5 - difference) > 0) pushVehicleUp(0.5 - difference);
                        }
                        return true;
                    }
                }

                //If it's another block or a top/double slab
                if (isOnGround) {
                    pushVehicleUp(1);
                } else { //Maybe they're on a carpet
                    if ((1 - difference) > 0) pushVehicleUp(1 - difference);
                }

                return true;

            }

            if (blockDataBelow instanceof Slab){ //If on a block, in front of it is air and below is bottom slab
                Slab slab = (Slab) blockDataBelow;
                if (slab.getType().equals(Slab.Type.BOTTOM)) { //If it's a bottom slab
                    pushVehicleDown(0.5);
                    return false; //Vehicle will go down
                }
            }


        }

        return false;
    }

    /**
     * Get the Y height of a snow layer
     * @param layers Number of layers
     * @return Height
     */
    protected double getLayerHeight(int layers){
        switch (layers){
            case 1:
                return 0.125;
            case 2:
                return 0.25;
            case 3:
                return 0.375;
            case 4:
                return 0.5;
            case 5:
                return 0.625;
            case 6:
                return 0.75;
            case 7:
                return 0.875;
            default:
                return 1;
        }
    }

    /**
     * Check the movement and teleport the main seat
     */
    protected void mainSeat() {
        if (VehicleData.seatsize.get(license) != null) {
            for (int i = 2; i <= VehicleData.seatsize.get(license); i++) {
                ArmorStand seatas = VehicleData.autostand.get("MTVEHICLES_SEAT" + i + "_" + license);
                double xOffset = VehicleData.seatx.get("MTVEHICLES_SEAT" + i + "_" + license);
                double yOffset = VehicleData.seaty.get("MTVEHICLES_SEAT" + i + "_" + license);
                double zOffset = VehicleData.seatz.get("MTVEHICLES_SEAT" + i + "_" + license);
                Location locvp = standMain.getLocation().clone();
                Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
                float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(fbvp.getYaw())));
                float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(fbvp.getYaw())));
                Location loc = new Location(standMain.getWorld(), xvp, standMain.getLocation().getY() + yOffset, zvp, fbvp.getYaw(), fbvp.getPitch());
                teleportSeat(seatas, loc);
            }
        }
        double xOffset = VehicleData.mainx.get("MTVEHICLES_MAINSEAT_" + license);
        double yOffset = VehicleData.mainy.get("MTVEHICLES_MAINSEAT_" + license);
        double zOffset = VehicleData.mainz.get("MTVEHICLES_MAINSEAT_" + license);
        Location locvp = standMain.getLocation().clone();
        Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
        float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(fbvp.getYaw())));
        float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(fbvp.getYaw())));
        Location loc = new Location(standMain.getWorld(), xvp, standMain.getLocation().getY() + yOffset, zvp, fbvp.getYaw(), fbvp.getPitch());
        teleportSeat(standMainSeat, loc);
    }

    /**
     * Teleport a seat to a specified location
     * @param seat ArmorStand of the seat
     * @param loc Location where the seat will be teleported to
     */
    @VersionSpecific
    protected void teleportSeat(ArmorStand seat, Location loc){
        try {
            ServerVersion version = getServerVersion();
            String craftEntityClassName = "org.bukkit.craftbukkit." + version.name() + ".entity.CraftEntity";
            if(version.isNewerOrEqualTo(ServerVersion.v26_1)) {
                craftEntityClassName = "org.bukkit.craftbukkit.entity.CraftEntity";
            }
            Class<?> craftEntityClass = Class.forName(craftEntityClassName);

            Method getHandleMethod = craftEntityClass.getMethod("getHandle");
            Object handle = getHandleMethod.invoke(seat);
            
            teleportSeat(handle, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Get the String name of the method for teleporting an ArmorStand. Changes between versions.
     * @return Teleport method's name as String.
     */
    @VersionSpecific
    protected static String getTeleportMethod(){
        if (getServerVersion().isNewerOrEqualTo(ServerVersion.v26_1)) return "absSnapTo";
        else if (getServerVersion().isNewerOrEqualTo(ServerVersion.v1_18_R1)) return "a";
        else return "setLocation";
    }

    /**
     * Teleport a seat to a desired location. The seat must already be specified as a CraftBukkit Entity.
     * @param seat Seat's ArmorStand as a CraftBukkit Entity
     * @param x X-coordinate of the location
     * @param y Y-coordinate of the location
     * @param z Z-coordinate of the location
     * @param yaw Yaw of the location
     * @param pitch Pitch of the location
     */
    protected void teleportSeat(Object seat, double x, double y, double z, float yaw, float pitch){
        schedulerRun(() -> {
            try {
                Method method = seat.getClass().getSuperclass().getSuperclass().getDeclaredMethod(getTeleportMethod(), double.class, double.class, double.class, float.class, float.class);
                method.invoke(seat, x, y, z, yaw, pitch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Teleport the armor stand to a correct location (based on speed, surroundings, fuel, etc...)
     */
    protected void updateStand() {
        final Location loc = standMain.getLocation();
        final Location locBlockAhead = getLocationOfBlockAhead();
        final Location locBlockAheadAndBelow = new Location(locBlockAhead.getWorld(), locBlockAhead.getX(), locBlockAhead.getY() - 1, locBlockAhead.getZ(), locBlockAhead.getPitch(), locBlockAhead.getYaw());
        final Location locBelow = new Location(loc.getWorld(), loc.getX(), loc.getY() - 0.2, loc.getZ(), loc.getYaw(), loc.getPitch());

        final Material block = locBelow.getBlock().getType();
        final String blockName = block.toString();

        boolean space = !isFalling && steerIsJumping();

        if (vehicleType.canFly()) {

            //Moving forwards when landing with no fuel - for more realistic movement.
            if (vehicleType.isAirplane() && isFalling && !block.equals(Material.AIR)){
                putFrictionSpeed();
                standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), 0.0, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }

            if ((vehicleType.isHelicopter() && !isPassable(locBelow.getBlock()))
                    || (vehicleType.isAirplane() && VehicleData.fuel.get(license) < 1 && !block.equals(Material.AIR))
            ) VehicleData.speed.put(license, 0.0);

            if (space) {
                final double takeOffSpeed = ((double) ConfigModule.defaultConfig.get(DefaultConfig.Option.TAKE_OFF_SPEED) > 0) ? (double) ConfigModule.defaultConfig.get(DefaultConfig.Option.TAKE_OFF_SPEED) : 0.4;
                if (vehicleType.isAirplane() && VehicleData.speed.get(license) < takeOffSpeed) {
                    double y = (isPassable(locBelow.getBlock())) ? -0.2 : 0;
                    standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), y, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                    return;
                }

                putFuelUsage();

                if (loc.getY() > (int) ConfigModule.defaultConfig.get(DefaultConfig.Option.MAX_FLYING_HEIGHT)) return;
                standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), 0.2, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }

            if (extremeFalling){
                if (standMain.isOnGround()){
                    if (VehicleData.fallDamage.get(license) == null){
                        double damageAmount = ((double) ConfigModule.defaultConfig.get(DefaultConfig.Option.HELICOPTER_FALL_DAMAGE) <= 0) ?
                                (double) DefaultConfig.Option.HELICOPTER_FALL_DAMAGE.getDefaultValue() : (double) ConfigModule.defaultConfig.get(DefaultConfig.Option.HELICOPTER_FALL_DAMAGE);
                        schedulerRun(() -> {
                            player.damage(damageAmount);
                            if (VehicleData.seatsize.get(license) != null) { //Damage all passengers too
                                for (int i = 2; i <= VehicleData.seatsize.get(license); i++) {
                                    ArmorStand seat = VehicleData.autostand.get("MTVEHICLES_SEAT" + i + "_" + license);
                                    List<Entity> passengers = seat.getPassengers();
                                    for (Entity p: passengers) {
                                        if (p instanceof LivingEntity){
                                            ((LivingEntity) p).damage(damageAmount);
                                        }
                                    }
                                }
                            }
                        });
                        VehicleData.fallDamage.put(license, true);
                    }
                }
                standMain.setGravity(true);
                return;
            }

            putFuelUsage();
            standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.2, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
            return;
        }

        if (vehicleType.isHover()) {
            if (block.equals(Material.AIR)) {
                standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.8, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }
            standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), 0.00001, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
            return;
        }

        if (vehicleType.isBoat()){
            if (!boatPassable(blockName) && !isPassable(locBelow.getBlock())){
                VehicleData.speed.put(license, 0.0);
            }

            if (isPassable(locBelow.getBlock()) && !boatPassable(blockName)){
                standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.8, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }

            standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), 0.01, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
            return;
        }

        if (blockName.contains("WATER")) {
            standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.8, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
            return;
        }

        if (isPassable(locBlockAhead.getBlock()) && isPassable(locBlockAheadAndBelow.getBlock())){
            if (isPassable(locBelow.getBlock())){
                standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.8, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }

            if (blockName.contains("CARPET")){
                standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), -0.7375, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
                return;
            }
        }

        standMain.setVelocity(new Vector(loc.getDirection().multiply(VehicleData.speed.get(license)).getX(), 0.0, loc.getDirection().multiply(VehicleData.speed.get(license)).getZ()));
    }

    /**
     * Remove fuel from vehicle (will use vehicle's fuel usage determined in VehicleData.yml)
     */
    protected void putFuelUsage() {
        if (!(boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.FUEL_ENABLED) || !(boolean) ConfigModule.vehicleDataConfig.get(license, VehicleDataConfig.Option.FUEL_ENABLED)) return;

        double fuelMultiplier = Double.parseDouble(ConfigModule.defaultConfig.get(DefaultConfig.Option.FUEL_MULTIPLIER).toString());
        if (fuelMultiplier < 0.1 || fuelMultiplier > 10) fuelMultiplier = 1; //Must be between 0.1 and 10. Default: 1
        final double newFuel = VehicleData.fuel.get(license) - (fuelMultiplier * VehicleData.fuelUsage.get(license));
        if (newFuel < 0) VehicleData.fuel.put(license, 0.0);
        else VehicleData.fuel.put(license, newFuel);
    }

    /**
     * Check whether a block is passable. Method used because 1.12 does not have this method natively.
     * @param block Checked block
     * @return True if the checked block is passable.
     */
    protected boolean isPassable(Block block){
        return block.isPassable();
    }

    /**
     * Rotate and move the rotors accordingly.
     */
    protected void rotors() {
        double xOffset = VehicleData.wiekenx.get("MTVEHICLES_WIEKENS_" + license);
        double yOffset = VehicleData.wiekeny.get("MTVEHICLES_WIEKENS_" + license);
        double zOffset = VehicleData.wiekenz.get("MTVEHICLES_WIEKENS_" + license);
        final Location locvp = standMain.getLocation().clone();
        final Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
        final float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(standRotors.getLocation().getYaw())));
        final float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(standRotors.getLocation().getYaw())));
        float yawAdd = (isFalling) ? 5 : 15;
        if (extremeFalling) yawAdd = 0;
        final Location loc = new Location(standMain.getWorld(), xvp, standMain.getLocation().getY() + yOffset, zvp, standRotors.getLocation().getYaw() + yawAdd, standRotors.getLocation().getPitch());
        schedulerRun(() -> standRotors.teleport(loc));
    }

    /**
     * Push vehicle up by a specified Y.
     * @param plus The height of which a vehicle is being pushed up.
     */
    protected void pushVehicleUp(double plus){
        final Location newLoc = new Location(standMain.getLocation().getWorld(), standMain.getLocation().getX(), standMain.getLocation().getY() + plus, standMain.getLocation().getZ(), standMain.getLocation().getYaw(), standMain.getLocation().getPitch());
        schedulerRun(() -> standMain.teleport(newLoc));
    }

    /**
     * Push vehicle down by a specified Y.
     * @param minus The height of which a vehicle is being pushed down.
     */
    protected void pushVehicleDown(double minus){
        pushVehicleUp(-minus);
    }

    /**
     * Get location of the block in front of the vehicle
     */
    protected Location getLocationOfBlockAhead(){
        double xOffset = 0.7;
        double yOffset = 0.4;
        double zOffset = 0.0;
        Location locvp = standMain.getLocation().clone();
        Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset));
        float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(fbvp.getYaw())));
        float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(fbvp.getYaw())));
        return new Location(standMain.getWorld(), xvp, standMain.getLocation().getY() + yOffset, zvp, fbvp.getYaw(), fbvp.getPitch());
    }

    /**
     * Checked whether a player is jumping (got from the steering packet)
     * @return True if player is jumping
     */
    protected boolean steerIsJumping(){
        try {
            ServerVersion version = getServerVersion();
            Class<?> packetClass = packet.getClass();

            if (version.isOlderOrEqualTo(ServerVersion.v1_21_R1)) {
                String declaredMethod = "d";

                if (version.isNewerOrEqualTo(ServerVersion.v1_20_R2) && version.isOlderThan(ServerVersion.v1_20_R4)) {
                    declaredMethod = "e";
                } else if (version.isNewerOrEqualTo(ServerVersion.v1_20_R4)) {
                    declaredMethod = "f";
                }
                Method method = packetClass.getDeclaredMethod(declaredMethod);
                return (Boolean) method.invoke(packet);
            }

            String inputMethod = "b";
            String jumpMethod = "e";

            if (version.isNewerOrEqualTo(ServerVersion.v26_1)) {
                inputMethod = "input";
                jumpMethod = "jump";
            }

            Object input = packetClass.getDeclaredMethod(inputMethod).invoke(packet);
            return (boolean) input.getClass().getDeclaredMethod(jumpMethod).invoke(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get steering packet's rotation
     * @return Rotation from the packet
     */
    protected float steerGetXxa(){
        try {
            ServerVersion version = getServerVersion();
            Class<?> packetClass = packet.getClass();

            if (version.isOlderOrEqualTo(ServerVersion.v1_21_R1)) {
                String declaredMethod = "b";

                if (version.isNewerOrEqualTo(ServerVersion.v1_19_R3) && version.isOlderThan(ServerVersion.v1_20_R4)) {
                    declaredMethod = "a";
                }

                Method method = packetClass.getDeclaredMethod(declaredMethod);
                return (float) method.invoke(packet);
            }

            String inputMethod = "b";
            String leftMethod = "c";
            String rightMethod = "d";

            if (version.isNewerOrEqualTo(ServerVersion.v26_1)) {
                inputMethod = "input";
                leftMethod = "left";
                rightMethod = "right";
            }

            Object input = packetClass.getDeclaredMethod(inputMethod).invoke(packet);
            if ((boolean) input.getClass().getDeclaredMethod(leftMethod).invoke(input)) {
                return 1;
            }
            if ((boolean) input.getClass().getDeclaredMethod(rightMethod).invoke(input)) {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get steering packet's movement (forwards × backwards)
     * @return Movement from the packet
     */
    protected float steerGetZza(){
        try {
            ServerVersion version = getServerVersion();
            Class<?> packetClass = packet.getClass();

            if (version.isOlderOrEqualTo(ServerVersion.v1_21_R1)) {
                String declaredMethod = "c";

                if (version.isNewerOrEqualTo(ServerVersion.v1_20_R2) && version.isOlderThan(ServerVersion.v1_20_R4)) {
                    declaredMethod = "d";
                } else if (version.isNewerOrEqualTo(ServerVersion.v1_20_R4)) {
                    declaredMethod = "e";
                }

                Method method = packetClass.getDeclaredMethod(declaredMethod);
                return (float) method.invoke(packet);
            }

            String inputMethod = "b";
            String forwardMethod = "a";
            String backwardMethod = "b";

            if (version.isNewerOrEqualTo(ServerVersion.v26_1)) {
                inputMethod = "input";
                forwardMethod = "forward";
                backwardMethod = "backward";
            }

            Object input = packetClass.getDeclaredMethod(inputMethod).invoke(packet);
            if ((boolean) input.getClass().getDeclaredMethod(forwardMethod).invoke(input)) {
                return 1;
            }
            if ((boolean) input.getClass().getDeclaredMethod(backwardMethod).invoke(input)) {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Spawn tank's shooting particles
     * @param stand The tank's main ArmorStand
     * @param loc Location of where the particles should be spawned
     */
    @VersionSpecific
    protected void spawnParticles(ArmorStand stand, Location loc){
        stand.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 2);
        stand.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2);
        stand.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 5);
        if (!getServerVersion().isOlderOrEqualTo(ServerVersion.v1_13_R2))
            stand.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 5);
    }

    /**
     * Spawn and shoot tank's TNT (must be enabled in config.yml), calls the {@link TankShootEvent}.
     * @param stand The tank's main ArmorStand
     * @param loc Location of where the TNT should be spawned
     */
    public void tankShoot(ArmorStand stand, Location loc){
        if (!(boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.TANK_TNT)) return;

        TankShootEvent api = new TankShootEvent(license);
        api.setPlayer(player);
        schedulerRun(api::call);
        if (api.isCancelled()) return;

        schedulerRun(() -> {
            TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);
            tnt.setFuseTicks(20);
            tnt.setVelocity(stand.getLocation().getDirection().multiply(3.0));
        });
    }

    /**
     * Check whether a block is passable for a boat.
     * @param blockName Name of checked block
     * @return True if the checked block is passable for a boat.
     */
    public boolean boatPassable(String blockName){
        return (blockName.contains("WATER") || blockName.contains("SEAGRASS") || blockName.contains("KELP") || blockName.contains("CORAL") || blockName.contains("PICKLE"));
    }

    /**
     * Spawn and move headlights, if turned on.
     * @warning Experimental feature. May cause lag.
     */
    private void addAndRemoveLight() {
        double xOffset = 0.7;
        double yOffset = 0.4;
        double zOffset = 0.0;
        Location locvp = standMain.getLocation().clone();
        Location fbvp = locvp.add(locvp.getDirection().setY(0).normalize().multiply(xOffset).multiply(2));
        float zvp = (float) (fbvp.getZ() + zOffset * Math.sin(Math.toRadians(fbvp.getYaw())));
        float xvp = (float) (fbvp.getX() + zOffset * Math.cos(Math.toRadians(fbvp.getYaw())));
        Location loc = new Location(standMain.getWorld(), xvp, standMain.getLocation().getY() + yOffset, zvp, fbvp.getYaw(), fbvp.getPitch());

        if(loc.getBlock().getType().equals(Material.AIR)){
            loc.getBlock().setType(Material.getMaterial("LIGHT"));
            Bukkit.getScheduler().runTaskLater(instance, () -> loc.getBlock().setType(Material.AIR), 10L);
        }
    }
}
