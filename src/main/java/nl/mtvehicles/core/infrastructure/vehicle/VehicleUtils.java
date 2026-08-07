package nl.mtvehicles.core.infrastructure.vehicle;

import de.tr7zw.changeme.nbtapi.NBTItem;
import nl.mtvehicles.core.Main;
import nl.mtvehicles.core.infrastructure.annotations.ToDo;
import nl.mtvehicles.core.infrastructure.dataconfig.DefaultConfig;
import nl.mtvehicles.core.infrastructure.dataconfig.VehicleDataConfig;
import nl.mtvehicles.core.infrastructure.enums.InventoryTitle;
import nl.mtvehicles.core.infrastructure.enums.Message;
import nl.mtvehicles.core.infrastructure.enums.RegionAction;
import nl.mtvehicles.core.infrastructure.enums.VehicleType;
import nl.mtvehicles.core.infrastructure.models.MTVConfig;
import nl.mtvehicles.core.infrastructure.modules.ConfigModule;
import nl.mtvehicles.core.infrastructure.utils.*;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Useful methods for vehicles
 * @see Vehicle
 */
public final class VehicleUtils {

    /**
     * A private constructor - makes this a "static class"
     */
    private VehicleUtils(){}

    /**
     * HashMap containing information about which trunk a player has opened (determined by vehicle's license plate)
     * @see VehicleData#getTrunkViewers(String) 
     */
    public static HashMap<Player, String> openedTrunk = new HashMap<>();

    /**
     * Check whether there is enough free space around a location to spawn a vehicle (so it won't get stuck between walls).
     * @param location Location to check
     * @return True if the location has enough free space
     */
    public static boolean hasSpawnSpace(Location location) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 1; y++) {
                    if (!location.clone().add(x, y, z).getBlock().isPassable()) return false;
                }
            }
        }
        return true;
    }

    /**
     * Spawn a vehicle
     * @param licensePlate Vehicle's license plate
     * @param location Location where the vehicle should be spawned
     *
     * @throws IllegalArgumentException If vehicle with given license plate does not exist
     */
    public static void spawnVehicle(String licensePlate, Location location) throws IllegalArgumentException {
        if (!existsByLicensePlate(licensePlate)) throw new IllegalArgumentException("Vehicle does not exists.");

        ArmorStand standSkin = location.getWorld().spawn(location, ArmorStand.class);
        allowTicking(standSkin);
        standSkin.setVisible(false);
        standSkin.setCustomName("MTVEHICLES_SKIN_" + licensePlate);
        standSkin.getEquipment().setHelmet(
                ItemUtils.getVehicleItem(
                        ItemUtils.getMaterial(ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.SKIN_ITEM).toString()),
                        (int) ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.SKIN_DAMAGE),
                        false,
                        ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.NAME).toString(),
                        licensePlate));

        ArmorStand standMain = location.getWorld().spawn(location, ArmorStand.class);
        standMain.setVisible(false);
        standMain.setCustomName("MTVEHICLES_MAIN_" + licensePlate);

        Vehicle vehicle = getVehicle(licensePlate);

        List<Map<String, Double>> seats = (List<Map<String, Double>>) vehicle.getVehicleData().get("seats");
        Map<String, Double> mainSeat = seats.get(0);
        Location locationMainSeat = new Location(location.getWorld(), location.getX() + mainSeat.get("x"), location.getY() + mainSeat.get("y"), location.getZ() + mainSeat.get("z"));
        ArmorStand standMainSeat = locationMainSeat.getWorld().spawn(locationMainSeat, ArmorStand.class);
        standMainSeat.setCustomName("MTVEHICLES_MAINSEAT_" + licensePlate);
        standMainSeat.setGravity(false);
        standMainSeat.setVisible(false);

        if (ConfigModule.vehicleDataConfig.getType(licensePlate).isBoat()){
            standMain.setGravity(false);
            standSkin.setGravity(false);
        }

        if (ConfigModule.vehicleDataConfig.getType(licensePlate).isHelicopter()) {
            List<Map<String, Double>> helicopterBlades = (List<Map<String, Double>>) vehicle.getVehicleData().get("wiekens");
            Map<?, ?> blade = helicopterBlades.get(0);
            Location locationBlade = new Location(location.getWorld(), location.getX() + (double) blade.get("z"), location.getY() + (double) blade.get("y"), location.getZ() + (double) blade.get("x"));
            ArmorStand standRotors = locationBlade.getWorld().spawn(locationBlade, ArmorStand.class);
            standRotors.setCustomName("MTVEHICLES_WIEKENS_" + licensePlate);
            standRotors.setGravity(false);
            standRotors.setVisible(false);

            if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.HELICOPTER_BLADES_ALWAYS_ON)) {
                ItemStack rotor = (new ItemFactory(Material.getMaterial("DIAMOND_HOE"))).setDurability((short) 1058).setName(TextUtils.colorize("&6Wieken")).setNBT("mtvehicles.kenteken", licensePlate).toItemStack();
                ItemMeta itemMeta = rotor.getItemMeta();
                List<String> lore = new ArrayList<>();
                lore.add(TextUtils.colorize("&a"));
                lore.add(TextUtils.colorize("&a" + licensePlate));
                lore.add(TextUtils.colorize("&a"));
                itemMeta.setLore(lore);
                itemMeta.setUnbreakable(true);
                rotor.setItemMeta(itemMeta);

                allowTicking(standRotors);
                standRotors.setHelmet((ItemStack) blade.get("item"));
            }
        }
    }

    /**
     * Get license plate from a vehicle item
     * @param item Vehicle as Item
     * @return Vehicle's license plate
     */
    public static String getLicensePlate(ItemStack item){
        NBTItem nbt = new NBTItem(item);
        return nbt.getString("mtvehicles.kenteken");
    }

    /**
     * Get Vehicle instance from a vehicle item
     * @param item Vehicle as Item
     * @return Vehicle
     * @see #getLicensePlate(ItemStack)
     */
    public static Vehicle getVehicle(ItemStack item){
        return getVehicle(getLicensePlate(item));
    }

    /**
     * Get the license plate of player's driven vehicle
     * @param p Player
     * @return Returns null if no vehicle is being driven
     * @see #getDrivenVehicle(Player)
     */
    @Nullable
    public static String getDrivenVehiclePlate(Player p){
        if (p.getVehicle() == null) return null;
        if (!p.getVehicle().getCustomName().contains("MTVEHICLES_")) return null;

        String[] name = p.getVehicle().getCustomName().split("_");
        return name[2];
    }

    /**
     * Get the player's driven vehicle
     * @param p Player
     * @return Returns null if no vehicle is being driven
     * @see #getDrivenVehiclePlate(Player)
     */
    public static Vehicle getDrivenVehicle(Player p){
        if (getDrivenVehiclePlate(p) == null) return null;
        return getVehicle(getDrivenVehiclePlate(p));
    }

    /**
     * Check if given UUID exists (to prevent further issues)
     * @since 2.5.1
     */
    public static boolean vehicleUUIDExists(String uuid){
        boolean exists = false;
        List<Map<?, ?>> vehicles = ConfigModule.vehiclesConfig.getVehicles();
        outerLoop:
        for (Map<?, ?> configVehicle : vehicles) {
            List<Map<?, ?>> skins = (List<Map<?, ?>>) configVehicle.get("cars");
            for (Map<?, ?> skin : skins) {
                if (skin.get("uuid") != null) {
                    if (skin.get("uuid").equals(uuid)) {
                        exists = true;
                        break outerLoop;
                    }
                }
            }
        }
        return exists;
    }

    /**
     * Create a vehicle and get its item by UUID (UUID may be found in vehicles.yml)
     * @param owner Vehicle's owner
     * @param uuid Vehicle's UUID (UUID may be found in vehicles.yml)
     * @return Null if vehicle was not found by given UUID; otherwise, vehicle item
     */
    public static ItemStack createAndGetItemByUUID(OfflinePlayer owner, String uuid) {
        List<Map<?, ?>> vehicles = ConfigModule.vehiclesConfig.getVehicles();
        for (Map<?, ?> configVehicle : vehicles) {
            List<Map<?, ?>> skins = (List<Map<?, ?>>) configVehicle.get("cars");
            for (Map<?, ?> skin : skins) {
                if (skin.get("uuid") != null) {
                    if (skin.get("uuid").equals(uuid)) {
                        String nbtVal;
                        if (skin.get("nbtValue") == null) {
                            nbtVal = "null";
                        } else {
                            nbtVal = skin.get("nbtValue").toString();
                        }

                        ItemStack item = ItemUtils.getVehicleItem(ItemUtils.getMaterial(skin.get("SkinItem").toString()), (int) skin.get("itemDamage"), ((String) skin.get("name")), "mtcustom", nbtVal);
                        NBTItem nbt = new NBTItem(item);
                        final String licensePlate = nbt.getString("mtvehicles.kenteken");

                        Vehicle vehicle = new Vehicle(
                                null,
                                licensePlate,
                                (String) skin.get("name"),
                                VehicleType.valueOf((String) configVehicle.get("vehicleType")),
                                false,
                                (int) skin.get("itemDamage"),
                                (String) skin.get("SkinItem"),
                                false,
                                (boolean) configVehicle.get("hornEnabled"),
                                (double) configVehicle.get("maxHealth"),
                                (boolean) configVehicle.get("benzineEnabled"),
                                100,
                                0.01,
                                (boolean) configVehicle.get("kofferbakEnabled"),
                                1,
                                null,
                                (double) configVehicle.get("acceleratieSpeed"),
                                (double) configVehicle.get("maxSpeed"),
                                (double) configVehicle.get("maxSpeedBackwards"),
                                (double) configVehicle.get("brakingSpeed"),
                                (double) configVehicle.get("aftrekkenSpeed"),
                                (int)configVehicle.get("rotateSpeed"),
                                owner.getUniqueId(),
                                null,
                                null,
                                (double) skin.get("price"),
                                (String) skin.get("nbtValue")
                        );
                        vehicle.save();
                        return item;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check whether horn is enabled on this vehicle.
     * @param damage The vehicle item's durability
     * @return True if horn is enabled
     */
    public static boolean getHornByDamage(int damage){
        List<Map<?, ?>> vehicles = ConfigModule.vehiclesConfig.getVehicles();
        for (Map<?, ?> configVehicle : vehicles) {
            List<Map<?, ?>> skins = (List<Map<?, ?>>) configVehicle.get("cars");
            for (Map<?, ?> skin : skins) {
                if (skin.get("itemDamage") != null) {
                    if (skin.get("itemDamage").equals(damage)) {
                        return (boolean) configVehicle.get("hornEnabled");
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check what is the max health of this vehicle.
     * @param damage The vehicle item's durability
     * @return Max health of the vehicle
     */
    public static double getMaxHealthByDamage(int damage){
        List<Map<?, ?>> vehicles = ConfigModule.vehiclesConfig.getVehicles();
        for (Map<?, ?> configVehicle : vehicles) {
            List<Map<?, ?>> skins = (List<Map<?, ?>>) configVehicle.get("cars");
            for (Map<?, ?> skin : skins) {
                if (skin.get("itemDamage") != null) {
                    if (skin.get("itemDamage").equals(damage)) {
                        return (double) configVehicle.get("maxHealth");
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Get a vehicle item by license plate. <b>Does not create a new vehicle.</b>
     * @param licensePlate Vehicle's license plate
     * @return The vehicle item - just aesthetic (null if license plate is not found)
     * @see #getItem(String)
     * @since 2.5.1
     */
    public static ItemStack getItemByLicensePlate(String licensePlate){
        return getItem(getUUID(licensePlate));
    }

    /**
     * Get a vehicle item by UUID. <b>Does not create a new vehicle - just for aesthetic purposes.</b> (Otherwise, use {@link #createAndGetItemByUUID(OfflinePlayer, String)})
     * @param carUUID Vehicle's UUID (UUID may be found in vehicles.yml)
     * @return The vehicle item - just aesthetic (null if UUID is not found)
     *
     */
    public static ItemStack getItem(String carUUID) {
        List<Map<?, ?>> vehicles = ConfigModule.vehiclesConfig.getVehicles();
        List<Map<?, ?>> matchedVehicles = new ArrayList<>();
        for (Map<?, ?> configVehicle : vehicles) {
            List<Map<?, ?>> skins = (List<Map<?, ?>>) configVehicle.get("cars");
            for (Map<?, ?> skin : skins) {
                if (skin.get("uuid") != null) {
                    if (skin.get("uuid").equals(carUUID)) {
                        if (skin.get("uuid") != null) {
                            ItemStack is = ItemUtils.getVehicleItem(ItemUtils.getMaterial(skin.get("SkinItem").toString()), (int) skin.get("itemDamage"), ((String) skin.get("name")));
                            matchedVehicles.add(configVehicle);
                            return is;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check whether an entity is a vehicle
     * @param entity Checked entity
     * @return True if the entity is a vehicle
     */
    public static boolean isVehicle(Entity entity){
        return entity.getCustomName() != null && entity instanceof ArmorStand && entity.getCustomName().contains("MTVEHICLES");
    }

    /**
     * Get the current driver of the vehicle.
     * @param licensePlate Vehicle's license plate
     * @return Returns null if the vehicle is not being driven by any player at the moment.
     * @since 2.5.1
     */
    @Nullable
    public static Player getCurrentDriver(String licensePlate){
        Player driver = null;
        for (World world : Bukkit.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getCustomName() != null && entity.getCustomName().contains("MAINSEAT_" + licensePlate)) {
                    driver = (Player) entity.getPassenger();
                }
            }
        }
        return driver;
    }

    /**
     * Get license plate of an entity (which should be a vehicle - see {@link #isVehicle(Entity)}.
     * @param entity Vehicle's main armor stand
     * @return Vehicle's license plate
     */
    public static String getLicensePlate(@Nullable Entity entity){
        if (entity == null) return null;
        final String name = entity.getCustomName();
        if (name.split("_").length > 1) {
            return name.split("_")[2];
        }
        return null;
    }

    /**
     * Get the UUID of a car by its license plate
     * @param licensePlate Vehicle's license plate
     * @return Vehicle's UUID
     */
    public static String getUUID(String licensePlate) {
        if (!existsByLicensePlate(licensePlate)) return null;

        Object skinItem = ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.SKIN_ITEM);
        Object skinDamage = ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.SKIN_DAMAGE);
        Object nbtValue = ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.NBT_VALUE);
        
        List<Map<?, ?>> vehicles = ConfigModule.vehiclesConfig.getVehicles();
        for (Map<?, ?> configVehicle : vehicles) {
            List<Map<?, ?>> skins = (List<Map<?, ?>>) configVehicle.get("cars");
            for (Map<?, ?> skin : skins) {
                if (skin.get("itemDamage").equals(skinDamage)) {
                    if (skin.get("SkinItem").equals(skinItem)) {
                        if (skin.get("nbtValue") != null) {
                            if (skin.get("nbtValue").equals(nbtValue)) {
                                return skin.get("uuid").toString();
                            }
                        } else {
                            return skin.get("uuid").toString();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the Vehicle instance by a vehicle's license plate
     * @param licensePlate Vehicle's license plate
     * @return Vehicle instance
     *
     * @see Vehicle
     */
    @ToDo("Beautify the code inside this method.")
    public static Vehicle getVehicle(String licensePlate) {
        if (!existsByLicensePlate(licensePlate)) return null;
        
        Map<String, Object> vehicleData = new HashMap<>();
        for (VehicleDataConfig.Option option : VehicleDataConfig.Option.values()) {
            Object value = ConfigModule.vehicleDataConfig.get(licensePlate, option);
            if (value != null) {
                vehicleData.put(option.getPath(), value);
            }
        }

        List<Map<?, ?>> vehicles = ConfigModule.vehiclesConfig.getVehicles();
        List<Map<?, ?>> matchedVehicles = new ArrayList<>();
        double price = 0.0;

        for (Map<?, ?> configVehicle : vehicles) {
            List<Map<?, ?>> skins = (List<Map<?, ?>>) configVehicle.get("cars");
            for (Map<?, ?> skin : skins) {
                if (skin.get("itemDamage").equals(vehicleData.get(VehicleDataConfig.Option.SKIN_DAMAGE.getPath()))) {
                    if (skin.get("SkinItem").equals(vehicleData.get(VehicleDataConfig.Option.SKIN_ITEM.getPath()))) {
                        if (skin.get("nbtValue") != null) {
                            if (skin.get("nbtValue").equals(vehicleData.get(VehicleDataConfig.Option.NBT_VALUE.getPath()))) {
                                matchedVehicles.add(configVehicle);
                                price = (double) skin.get("price");
                            }
                        } else {
                            matchedVehicles.add(configVehicle);
                            price = (double) skin.get("price");
                        }
                    }
                }
            }
        }

        if (matchedVehicles.isEmpty()) return null;
        if (matchedVehicles.size() > 1) return null;

        return new Vehicle(
                matchedVehicles.get(0),
                licensePlate,
                (String) vehicleData.get(VehicleDataConfig.Option.NAME.getPath()),
                VehicleType.valueOf((String) vehicleData.get(VehicleDataConfig.Option.VEHICLE_TYPE.getPath())),
                (boolean) vehicleData.get(VehicleDataConfig.Option.IS_OPEN.getPath()),
                (int) vehicleData.get(VehicleDataConfig.Option.SKIN_DAMAGE.getPath()),
                (String) vehicleData.get(VehicleDataConfig.Option.SKIN_ITEM.getPath()),
                (boolean) vehicleData.get(VehicleDataConfig.Option.IS_GLOWING.getPath()),
                ConfigModule.vehicleDataConfig.isHornSet(licensePlate) ? (boolean) vehicleData.get(VehicleDataConfig.Option.HORN_ENABLED.getPath()) : ConfigModule.vehicleDataConfig.isHornEnabled(licensePlate),
                ConfigModule.vehicleDataConfig.isHealthSet(licensePlate) ? (double) vehicleData.get(VehicleDataConfig.Option.HEALTH.getPath()) : ConfigModule.vehicleDataConfig.getHealth(licensePlate),
                (boolean) vehicleData.get(VehicleDataConfig.Option.FUEL_ENABLED.getPath()),
                (double) vehicleData.get(VehicleDataConfig.Option.FUEL.getPath()),
                (double) vehicleData.get(VehicleDataConfig.Option.FUEL_USAGE.getPath()),
                (boolean) vehicleData.get(VehicleDataConfig.Option.TRUNK_ENABLED.getPath()),
                (int) vehicleData.get(VehicleDataConfig.Option.TRUNK_ROWS.getPath()),
                ConfigModule.vehicleDataConfig.getTrunkData(licensePlate),
                (double) vehicleData.get(VehicleDataConfig.Option.ACCELERATION_SPEED.getPath()),
                (double) vehicleData.get(VehicleDataConfig.Option.MAX_SPEED.getPath()),
                (double) vehicleData.get(VehicleDataConfig.Option.MAX_SPEED_BACKWARDS.getPath()),
                (double) vehicleData.get(VehicleDataConfig.Option.BRAKING_SPEED.getPath()),
                (double) vehicleData.get(VehicleDataConfig.Option.FRICTION_SPEED.getPath()),
                (int) vehicleData.get(VehicleDataConfig.Option.ROTATION_SPEED.getPath()),
                UUID.fromString((String) vehicleData.get(VehicleDataConfig.Option.OWNER.getPath())),
                ConfigModule.vehicleDataConfig.getRiders(licensePlate),
                ConfigModule.vehicleDataConfig.getMembers(licensePlate),
                price,
                (String) vehicleData.get(VehicleDataConfig.Option.NBT_VALUE.getPath())
        );
    }


    /**
     * Check whether this vehicle exists in the database (vehicleData.yml)
     * @param licensePlate Vehicle's license plate
     * @return True if vehicle is in the database (vehicleData.yml)
     */
    public static boolean existsByLicensePlate(String licensePlate) {
        return ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.SKIN_ITEM) != null;
    }

    /**
     * Check whether a player can ride/drive the vehicle.
     * @param player Player
     * @param licensePlate Vehicle's license plate
     * @return True if player is the vehicle's set rider.
     */
    public static boolean canRide(Player player, String licensePlate) {
        return ConfigModule.vehicleDataConfig.getRiders(licensePlate).contains(player.getUniqueId().toString());
    }

    /**
     * Check whether a player can sit in the vehicle.
     * @param player Player
     * @param licensePlate Vehicle's license plate
     * @return True if player is the vehicle's set passenger/member.
     */
    public static boolean canSit(Player player, String licensePlate) {
        return ConfigModule.vehicleDataConfig.getMembers(licensePlate).contains(player.getUniqueId().toString());
    }

    /**
     * Get the UUID of the vehicle's owner
     * @param licensePlate Vehicle's license plate
     * @return UUID of vehicle's owner
     */
    public static UUID getOwnerUUID(String licensePlate) {
        Object owner = ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.OWNER);
        if(owner == null) {
            return null;
        }
        return UUID.fromString(owner.toString());
    }

    /**
     * Open a vehicle's trunk to a player
     * @param p Player who is opening the trunk
     * @param license Vehicle's license plate
     */
    public static void openTrunk(Player p, String license) {
        if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.TRUNK_ENABLED)) {
            if (VehicleUtils.getVehicle(license) == null) {
                ConfigModule.messagesConfig.sendMessage(p, Message.VEHICLE_NOT_FOUND);
                return;
            }

            if (VehicleUtils.getVehicle(license).isOwner(p) || p.hasPermission("mtvehicles.kofferbak")) {
                ConfigModule.configList.forEach(MTVConfig::reload);
                Inventory inv = Bukkit.createInventory(null, (int) ConfigModule.vehicleDataConfig.get(license, VehicleDataConfig.Option.TRUNK_ROWS) * 9, InventoryTitle.VEHICLE_TRUNK.getStringTitle());

                if (ConfigModule.vehicleDataConfig.get(license, VehicleDataConfig.Option.TRUNK_DATA) != null) {
                    List<ItemStack> chestContentsFromConfig = (List<ItemStack>) ConfigModule.vehicleDataConfig.get(license, VehicleDataConfig.Option.TRUNK_DATA);

                    for (ItemStack item : chestContentsFromConfig) {
                        if (item != null) inv.addItem(item);
                    }
                }

                openedTrunk.put(p, license);
                VehicleData.trunkViewerAdd(license, p);
                p.openInventory(inv);

            } else {
                p.sendMessage(TextUtils.colorize(ConfigModule.messagesConfig.getMessage(Message.VEHICLE_NO_RIDER_TRUNK).replace("%p%", VehicleUtils.getVehicle(license).getOwnerName())));
            }
        }
    }

    /**
     * Check if trunk of a vehicle is opened by a specified player
     * @param p Player
     * @param license Vehicle's license plate
     * @since 2.5.1
     */
    public static boolean isTrunkInventoryOpen(Player p, String license) {
        return openedTrunk.containsKey(p) && openedTrunk.get(p).equals(license);
    }

    /**
     * Check whether a player is inside a vehicle
     * @param p Player
     * @return True if player is inside any MTV vehicle
     */
    public static boolean isInsideVehicle(Player p){
        if (p == null) return false;
        if (!p.isInsideVehicle()) return false;
        return VehicleUtils.isVehicle(p.getVehicle());
    }

    /**
     * Check whether a vehicle is occupied
     * @param licensePlate Vehicle's license plate
     * @return True if the vehicle is occupied
     */
    public static boolean isOccupied(String licensePlate) {
        return getCurrentDriver(licensePlate) != null;
    }

    /**
     * Get all the vehicle's set drivers/riders.
     * @param licensePlate Vehicle's license plate
     * @return String of all the drivers/riders separated by commas
     *
     * @deprecated Use {@link #canRide(Player, String)} instead.
     */
    @Deprecated
    public static String getRidersAsString(String licensePlate) {
        StringBuilder sb = new StringBuilder();
        for (String s : ConfigModule.vehicleDataConfig.getRiders(licensePlate)) {
            if (!UUID.fromString(s).equals(getOwnerUUID(licensePlate))) {
                sb.append(Bukkit.getOfflinePlayer(UUID.fromString(s)).getName()).append(", ");
            }
        }
        if (sb.toString().isEmpty()) {
            sb.append("Niemand");
        }
        return sb.toString();
    }

    /**
     * Pick up a vehicle and put it to player's inventory
     * @param license Vehicle's license plate
     * @param player Player
     */
    public static void pickupVehicle(String license, Player player) {
        Vehicle vehicle = getVehicle(license);
        if (vehicle == null) {
            for (World world : Bukkit.getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getCustomName() != null && entity.getCustomName().contains(license)) {
                        entity.remove();
                    }
                }
            }
            ConfigModule.messagesConfig.sendMessage(player, Message.VEHICLE_NOT_FOUND);
            return;
        }

        if (vehicle.getOwnerName() == null) {
            ConfigModule.messagesConfig.sendMessage(player, Message.VEHICLE_NOT_FOUND);
            Main.logSevere("Could not find the owner of the vehicle " + license + "! The vehicleData.yml must be malformed!");
            return;
        }

        if (vehicle.isOwner(player) && !((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.CAR_PICKUP)) || player.hasPermission("mtvehicles.oppakken")) {
            for (World world : Bukkit.getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getCustomName() != null && entity.getCustomName().contains(license)) {
                        ArmorStand test = (ArmorStand) entity;
                        if (test.getCustomName().contains("MTVEHICLES_SKIN_" + license)) {
                            for (Player trunkViewer : VehicleData.getTrunkViewers(license)){
                                trunkViewer.closeInventory();
                            }
                            if (!TextUtils.checkInvFull(player)) {
                                player.getInventory().addItem(test.getHelmet());
                                player.sendMessage(TextUtils.colorize(ConfigModule.messagesConfig.getMessage(Message.VEHICLE_PICKUP).replace("%p%", vehicle.getOwnerName())));
                            } else {
                                ConfigModule.messagesConfig.sendMessage(player, Message.INVENTORY_FULL);
                                return;
                            }
                        }
                        test.remove();
                    }
                }
            }
        } else {
            if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.CAR_PICKUP)) {
                player.sendMessage(TextUtils.colorize(ConfigModule.messagesConfig.getMessage(Message.CANNOT_DO_THAT_HERE)));
                return;
            }
            player.sendMessage(TextUtils.colorize(ConfigModule.messagesConfig.getMessage(Message.VEHICLE_NO_OWNER_PICKUP).replace("%p%", vehicle.getOwnerName())));
            return;
        }
    }

    /**
     * Delete a vehicle from the database and despawn it from all worlds
     * @param licensePlates Vehicle's license plate
     * @throws IllegalArgumentException Thrown if given license plate is invalid.
     * @throws IllegalStateException Thrown if vehicle is already deleted
     * @since 2.5.4
     */
    public static void deleteVehicle(String... licensePlates) throws IllegalArgumentException, IllegalStateException {
        for (String licensePlate : licensePlates) {
            if (!existsByLicensePlate(licensePlate)) throw new IllegalArgumentException("Vehicle " + licensePlate + " does not exist.");
            despawnVehicle(licensePlate);
            ConfigModule.vehicleDataConfig.delete(licensePlate);
        }
    }

    /**
     * Teleport a vehicle to a location
     * @param licensePlate Vehicle's license plate
     * @param location Location where the vehicle should be teleported
     * @throws IllegalArgumentException Thrown if given license plate is invalid.
     */
    public static void teleportVehicle(String licensePlate, Location location) throws IllegalArgumentException {
        if (!existsByLicensePlate(licensePlate)) throw new IllegalArgumentException("Vehicle does not exists.");

        for (World world : Bukkit.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getCustomName() != null && entity.getCustomName().contains(licensePlate)) {
                    entity.teleport(location);
                }
            }
        }
    }

    /**
     * Despawn a vehicle specified by its license plate from all worlds
     * @param licensePlates Vehicle's license plate
     * @throws IllegalArgumentException Thrown if given license plate is invalid.
     * @since 2.5.1
     * @see #despawnVehicle(World, String...)
     * @return Number of vehicles despawned
     */
    public static int despawnVehicle(String... licensePlates) throws IllegalArgumentException {
        int despawned = 0;
        for (String licensePlate : licensePlates) {
            if (!existsByLicensePlate(licensePlate)) throw new IllegalArgumentException("Vehicle " + licensePlate + " does not exist.");
            for (Player trunkViewer : VehicleData.getTrunkViewers(licensePlate)){
                trunkViewer.closeInventory();
            }

            for (World world : Bukkit.getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getCustomName() != null && entity.getCustomName().contains(licensePlate) && entity.getCustomName().contains("MTVEHICLES")) {
                        entity.remove();
                        despawned++;
                    }
                }
            }
        }
        return despawned;
    }

    /**
     *  Despawn a vehicle specified by its license plate from a specified world
     * @param world World where the vehicle is being removed
     * @param licensePlates Vehicle's license plate
     * @throws IllegalArgumentException Thrown if given license plate is invalid.
     * @since 2.5.1
     * @see #despawnVehicle(String...)
     * @return Number of vehicles despawned
     */
    public static int despawnVehicle(World world, String... licensePlates) throws IllegalArgumentException {
        int despawned = 0;
        for (String licensePlate : licensePlates) {
            if (!existsByLicensePlate(licensePlate)) throw new IllegalArgumentException("Vehicle " + licensePlate + " does not exist.");

            for (Player trunkViewer : VehicleData.getTrunkViewers(licensePlate)){
                trunkViewer.closeInventory();
            }

            for (Entity entity : world.getEntities()) {
                if (entity.getCustomName() != null && entity.getCustomName().contains(licensePlate)) {
                    entity.remove();
                    despawned++;
                }
            }
        }
        return despawned;
    }

    /**
     * Get a list of all spawned vehicles' license plates in all worlds.
     * @return May return list with duplicates - if the same vehicle is spawned multiple times (see {@link #getUniqueSpawnedVehiclePlates()}).
     * @since 2.5.1
     * @see #getAllSpawnedVehiclePlates(World)
     */
    public static List<String> getAllSpawnedVehiclePlates(){
        List<String> list = new ArrayList<>();

        for (World world : Bukkit.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getCustomName() != null) {
                    String name = entity.getCustomName();
                    if (name.contains("MTVEHICLES_MAIN_")) list.add(name.split("_")[2]);
                }
            }
        }
        return list;
    }

    /**
     * Get a list of all spawned vehicles' license plates in a specified world.
     * @return May return list with duplicates - if the same vehicle is spawned multiple times (see {@link #getUniqueSpawnedVehiclePlates(World)}).
     * @since 2.5.1
     * @see #getAllSpawnedVehiclePlates()
     */
    public static List<String> getAllSpawnedVehiclePlates(World world){
        List<String> list = new ArrayList<>();

        for (Entity entity : world.getEntities()) {
            if (entity.getCustomName() != null) {
                String name = entity.getCustomName();
                if (name.contains("MTVEHICLES_MAIN_")) list.add(name.split("_")[2]);
            }
        }
        return list;
    }

    /**
     * Get a list of all spawned vehicles' license plates in all worlds.
     * @return Returns HashSet with no duplicates (see {@link #getAllSpawnedVehiclePlates()}).
     * @since 2.5.1
     * @see #getUniqueSpawnedVehiclePlates(World)
     */
    public static Set<String> getUniqueSpawnedVehiclePlates(){
        return new HashSet<>(getAllSpawnedVehiclePlates());
    }

    /**
     * Get a list of all spawned vehicles' license plates in a specified worlds.
     * @return Returns HashSet with no duplicates (see {@link #getAllSpawnedVehiclePlates(World)}).
     * @since 2.5.1
     * @see #getUniqueSpawnedVehiclePlates()
     */
    public static Set<String> getUniqueSpawnedVehiclePlates(World world){
        return new HashSet<>(getAllSpawnedVehiclePlates(world));
    }

    /**
     * Set vehicle's current fuel level
     * @param licensePlate Vehicle's license plate
     * @param fuel Fuel level (0–100)
     * @return True if fuel level was set successfully
     */
    public static boolean setFuel(String licensePlate, Double fuel){
        if (!existsByLicensePlate(licensePlate)) return false;
        if (!(fuel <= 100) || !(fuel >= 0)) return false;
        VehicleData.fuel.put(licensePlate, fuel);
        ConfigModule.vehicleDataConfig.set(licensePlate, VehicleDataConfig.Option.FUEL, fuel);
        return true;
    }

    /**
     * Create {@link VehicleData} (necessary for driving to work), helicopter blades, and make player enter a vehicle.
     * @param licensePlate Vehicle's license plate
     * @param p Player who is entering the vehicle
     */
    @ToDo("Beautify the code inside this method.")
    public static void enterVehicle(String licensePlate, Player p) {
        if (!(VehicleData.autostand2.get(licensePlate) == null)) {
            if (!VehicleData.autostand2.get(licensePlate).isEmpty()) {
                return;
            }
        }

        Vehicle vehicle = getVehicle(licensePlate);

        if (vehicle == null) {
            ConfigModule.messagesConfig.sendMessage(p, Message.VEHICLE_NOT_FOUND);
            return;
        }

        if (vehicle.getOwnerName() == null) {
            ConfigModule.messagesConfig.sendMessage(p, Message.VEHICLE_NOT_FOUND);
            Main.logSevere("Could not find the owner of vehicle " + licensePlate + "! The vehicleData.yml must be malformed!");
            return;
        }

        if (!vehicle.isPublic() && !vehicle.isOwner(p) && !vehicle.canRide(p) && !p.hasPermission("mtvehicles.ride")){
            p.sendMessage(ConfigModule.messagesConfig.getMessage(Message.VEHICLE_NO_RIDER_ENTER).replace("%p%", vehicle.getOwnerName()));
            return;
        }

        for (Entity entity : p.getWorld().getEntities()) {

            if (entity.getCustomName() != null && entity.getCustomName().contains(licensePlate)) {
                ArmorStand vehicleAs = (ArmorStand) entity;
                if (!entity.isEmpty()) {
                    return;
                }
                VehicleData.fuel.put(licensePlate, vehicle.getFuel());
                VehicleData.fuelUsage.put(licensePlate, (double) ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.FUEL_USAGE));
                VehicleData.type.put(licensePlate, VehicleType.valueOf(ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.VEHICLE_TYPE).toString().toUpperCase(Locale.ROOT)));

                VehicleData.setRotationSpeed(licensePlate, (int) ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.ROTATION_SPEED));
                VehicleData.setSpeed(VehicleData.DataSpeed.MAXSPEED, licensePlate, (double) ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.MAX_SPEED));
                VehicleData.setSpeed(VehicleData.DataSpeed.ACCELERATION, licensePlate, (double) ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.ACCELERATION_SPEED));
                VehicleData.setSpeed(VehicleData.DataSpeed.BRAKING, licensePlate, (double) ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.BRAKING_SPEED));
                VehicleData.setSpeed(VehicleData.DataSpeed.MAXSPEEDBACKWARDS, licensePlate, (double) ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.MAX_SPEED_BACKWARDS));
                VehicleData.setSpeed(VehicleData.DataSpeed.FRICTION, licensePlate, (double) ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.FRICTION_SPEED));

                Location location = new Location(entity.getWorld(), entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ(), entity.getLocation().getYaw(), entity.getLocation().getPitch());

                if (!ConfigModule.defaultConfig.canProceedWithAction(RegionAction.ENTER, vehicle.getVehicleType(), location, p)){
                    ConfigModule.messagesConfig.sendMessage(p, Message.CANNOT_DO_THAT_HERE);
                    return;
                }

                VehicleType vehicleType = ConfigModule.vehicleDataConfig.getType(licensePlate);
                if (vehicleAs.getCustomName().contains("MTVEHICLES_SKIN_" + licensePlate)) {
                    basicStandCreator(licensePlate, "SKIN", location, vehicleAs.getHelmet(), false);
                    basicStandCreator(licensePlate, "MAIN", location, null, true);
                    vehicle.saveSeats();
                    List<Map<String, Double>> seats = vehicle.getSeats();
                    VehicleData.seatsize.put(licensePlate, seats.size());
                    for (int i = 1; i <= seats.size(); i++) {
                        Map<String, Double> seat = seats.get(i - 1);
                        if (i == 1) {
                            mainSeatStandCreator(licensePlate, location, p, seat.get("x"), seat.get("y"), seat.get("z"));
                            BossBarUtils.addBossBar(p, licensePlate);
                            p.sendMessage(TextUtils.colorize(ConfigModule.messagesConfig.getMessage(Message.VEHICLE_ENTER_RIDER).replace("%p%", getVehicle(licensePlate).getOwnerName())));
                        }

                        if (i > 1) {

                            VehicleData.seatx.put("MTVEHICLES_SEAT" + i + "_" + licensePlate, seat.get("x"));
                            VehicleData.seaty.put("MTVEHICLES_SEAT" + i + "_" + licensePlate, seat.get("y"));
                            VehicleData.seatz.put("MTVEHICLES_SEAT" + i + "_" + licensePlate, seat.get("z"));
                            Location location2 = new Location(location.getWorld(), location.getX() + Double.valueOf(seat.get("x")), location.getY() + Double.valueOf(seat.get("y")), location.getZ() + Double.valueOf(seat.get("z")));

                            ArmorStand as = location2.getWorld().spawn(location2, ArmorStand.class);
                            allowTicking(as);
                            as.setVisible(false);
                            as.setCustomName("MTVEHICLES_SEAT" + i + "_" + licensePlate);
                            as.setGravity(false);

                            VehicleData.autostand.put("MTVEHICLES_SEAT" + i + "_" + licensePlate, as);
                        }
                    }
                    List<Map<String, Double>> wiekens = (List<Map<String, Double>>) vehicle.getVehicleData().get("wiekens");
                    if (vehicleType.isHelicopter()) {
                        VehicleData.maxheight.put(licensePlate, (int) ConfigModule.defaultConfig.get(DefaultConfig.Option.MAX_FLYING_HEIGHT));
                        for (int i = 1; i <= wiekens.size(); i++) {
                            Map<?, ?> seat = wiekens.get(i - 1);
                            if (i == 1) {
                                Location location2 = new Location(location.getWorld(), location.getX() + (Double) seat.get("z"), (Double) location.getY() + (Double) seat.get("y"), location.getZ() + (Double) seat.get("x"));
                                VehicleData.wiekenx.put("MTVEHICLES_WIEKENS_" + licensePlate, (Double) seat.get("x"));
                                VehicleData.wiekeny.put("MTVEHICLES_WIEKENS_" + licensePlate, (Double) seat.get("y"));
                                VehicleData.wiekenz.put("MTVEHICLES_WIEKENS_" + licensePlate, (Double) seat.get("z"));

                                ArmorStand as = location2.getWorld().spawn(location2, ArmorStand.class);
                                allowTicking(as);
                                as.setVisible(false);
                                as.setCustomName("MTVEHICLES_WIEKENS_" + licensePlate);
                                as.setGravity(false);
                                as.setHelmet((ItemStack) seat.get("item"));

                                VehicleData.autostand.put("MTVEHICLES_WIEKENS_" + licensePlate, as);
                            }
                        }
                    }
                }
                vehicleAs.remove();
            }
        }
    }

    /**
     * Used in {@link #enterVehicle(String, Player)}.
     */
    private static void basicStandCreator(String license, String type, Location location, ItemStack item, Boolean gravity) {
        ArmorStand as = location.getWorld().spawn(location, ArmorStand.class);
        allowTicking(as);
        as.setVisible(false);
        as.setCustomName("MTVEHICLES_" + type + "_" + license);
        as.setHelmet(item);
        as.setGravity(gravity);

        VehicleData.autostand.put("MTVEHICLES_" + type + "_" + license, as);
    }

    private static void allowTicking(ArmorStand armorStand) {
        if(PaperUtils.isRunningPaper) {
            armorStand.setCanTick(true);
        }
    }

    /**
     * Used in {@link #enterVehicle(String, Player)}.
     */
    private static void mainSeatStandCreator(String license, Location location, Player p, double x, double y, double z) {
        Location location2 = new Location(location.getWorld(), location.getX() + Double.valueOf(z), location.getY() + Double.valueOf(y), location.getZ() + Double.valueOf(z));
        ArmorStand as = location2.getWorld().spawn(location2, ArmorStand.class);
        allowTicking(as);
        as.setVisible(false);
        as.setCustomName("MTVEHICLES_MAINSEAT_" + license);
        as.setGravity(false);

        VehicleData.autostand.put("MTVEHICLES_MAINSEAT_" + license, as);
        VehicleData.speed.put(license, 0.0);
        VehicleData.speedhigh.put(license, 0.0);
        VehicleData.mainx.put("MTVEHICLES_MAINSEAT_" + license, x);
        VehicleData.mainy.put("MTVEHICLES_MAINSEAT_" + license, y);
        VehicleData.mainz.put("MTVEHICLES_MAINSEAT_" + license, z);

        as.setPassenger(p);
        VehicleData.autostand2.put(license, as);
    }

    /**
     * Shortcut for {@link Vehicle.Seat#getSeat(Player)}
     */
    public static Vehicle.Seat getSeat(Player player){
        return Vehicle.Seat.getSeat(player);
    }

    /**
     * Kick a player out of a vehicle; if the player is a driver, {@link #turnOff(Vehicle)} is called as well.
     * @return True if successful
     * @throws IllegalStateException If player is not seated in a (valid) vehicle
     */
    public static boolean kickOut(Player player) throws IllegalStateException {
        if (getSeat(player) == null) throw new IllegalStateException("Player is not seated in a vehicle!");

        Entity seat = player.getVehicle();
        if (!getSeat(player).isDriver()) {
            return seat.removePassenger(player);
        }

        final String license = getLicensePlate(seat);
        if (seat.removePassenger(player)){
            BossBarUtils.removeBossBar(player, license);
            return turnOff(license);
        }
        return false;
    }

    /**
     * Get the location of a vehicle
     * @param vehicle Vehicle
     * @return Vehicle's location
     * @since 2.5.4
     * @see #getLocation(String)
     */
    public static Location getLocation(Vehicle vehicle){
        return getLocation(vehicle.getLicensePlate());
    }

    /**
     * Get the location of a vehicle
     * @param licensePlate Vehicle's license plate
     * @return Vehicle's location
     * @since 2.5.4
     */
    public static Location getLocation(String licensePlate){
        if (VehicleData.autostand.get("MTVEHICLES_MAIN_" + licensePlate) == null) return null;
        return VehicleData.autostand.get("MTVEHICLES_MAIN_" + licensePlate).getLocation();
    }

    /**
     * Delete {@link VehicleData}, helicopter blades; save fuel, etc... <b>after a driver has left the vehicle</b>.
     * @param vehicle Vehicle
     * @return False if the driver is seated in the vehicle, or if the vehicle doesn't have {@link VehicleData} and thus is not created (see {@link #enterVehicle(String, Player)} -> the vehicle can't be turned off. Otherwise, true.
     *
     * @warn Do not call this method if a vehicle is being used! Use {@link #kickOut(Player)} instead.
     */
    public static boolean turnOff(@NotNull Vehicle vehicle){
        final String licensePlate = vehicle.getLicensePlate();

        if (VehicleData.autostand.get("MTVEHICLES_MAIN_" + licensePlate) == null) return false;

        final ArmorStand standMain = VehicleData.autostand.get("MTVEHICLES_MAIN_" + licensePlate);
        final ArmorStand standSkin = VehicleData.autostand.get("MTVEHICLES_SKIN_" + licensePlate);
        final ArmorStand standMainSeat = VehicleData.autostand.get("MTVEHICLES_MAINSEAT_" + licensePlate);
        VehicleType vehicleType = VehicleData.type.get(licensePlate);

        VehicleData.lastRegions.remove(licensePlate); // doesn't do anything if not set
        VehicleData.lastSafeLocation.remove(licensePlate);
        if(vehicleType == null) return true;

        if (vehicleType.isHelicopter()) {
            ArmorStand blades = VehicleData.autostand.get("MTVEHICLES_WIEKENS_" + licensePlate);
            Location locBelow = new Location(blades.getLocation().getWorld(), blades.getLocation().getX(), blades.getLocation().getY() - 0.2, blades.getLocation().getZ(), blades.getLocation().getYaw(), blades.getLocation().getPitch());
            blades.setGravity(locBelow.getBlock().getType().equals(Material.AIR)); // Blades should not fall if the helicopter is on the ground
        }

        // If a helicopter is 'extremely falling' and player manages to leave it beforehand
        if (vehicleType.isHelicopter() && (boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.EXTREME_HELICOPTER_FALL) && !standMainSeat.isOnGround()){
            VehicleData.fallDamage.put(licensePlate, true); // Do not damage when entering afterwards
        }

        if (!vehicleType.isBoat()) {
            standMain.setGravity(true);
            standSkin.setGravity(true);
        }
        List<Map<String, Double>> seats = vehicle.getSeats();
        for (int i = 2; i <= seats.size(); i++) {
            if (VehicleData.autostand.get("MTVEHICLES_SEAT" + i + "_" + licensePlate) != null)
                VehicleData.autostand.get("MTVEHICLES_SEAT" + i + "_" + licensePlate).remove();
        }
        VehicleData.type.remove(licensePlate); //.remove(license+"b") used to be here... why? maybe i'm missing something?

        if ((boolean) ConfigModule.defaultConfig.get(DefaultConfig.Option.FUEL_ENABLED) && (boolean) ConfigModule.vehicleDataConfig.get(licensePlate, VehicleDataConfig.Option.FUEL_ENABLED)) {
            double fuel = VehicleData.fuel.get(licensePlate);
            ConfigModule.vehicleDataConfig.set(licensePlate, VehicleDataConfig.Option.FUEL, fuel);
            ConfigModule.vehicleDataConfig.saveToDisk();
        }

        return true;
    }

    /**
     * @param licensePlate Vehicle's license plate
     * @see #turnOff(Vehicle)
     */
    public static boolean turnOff(@NotNull String licensePlate){
        if (getVehicle(licensePlate) == null) return false;
        return turnOff(getVehicle(licensePlate));
    }

    /**
     * Get list of seats for a vehicle (specified by license plate)
     * @see Vehicle#getSeats()
     */


    /**
     * Get vehicle's price
     * @param carUUID Vehicle's UUID
     * @return Price of the vehicle, null if UUID is not found
     */
    public static Double getPrice(String carUUID){
        List<Map<?, ?>> vehicles = ConfigModule.vehiclesConfig.getVehicles();
        for (Map<?, ?> configVehicle : vehicles) {
            List<Map<?, ?>> skins = (List<Map<?, ?>>) configVehicle.get("cars");
            for (Map<?, ?> skin : skins) {
                if (skin.get("uuid") != null) {
                    if (skin.get("uuid").equals(carUUID)) {
                        if (skin.get("uuid") != null) {
                            return (double) skin.get("price");
                        }
                    }
                }
            }
        }
        return null;
    }
}
