package nl.mtvehicles.core.listeners;

import de.tr7zw.changeme.nbtapi.NBTItem;
import nl.mtvehicles.core.events.VehiclePlaceEvent;
import nl.mtvehicles.core.infrastructure.enums.Message;
import nl.mtvehicles.core.infrastructure.enums.RegionAction;
import nl.mtvehicles.core.infrastructure.models.MTVListener;
import nl.mtvehicles.core.infrastructure.modules.ConfigModule;
import nl.mtvehicles.core.infrastructure.utils.TextUtils;
import nl.mtvehicles.core.infrastructure.vehicle.Vehicle;
import nl.mtvehicles.core.infrastructure.vehicle.VehicleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * On place of a vehicle
 */
public class VehiclePlaceListener extends MTVListener {

    public VehiclePlaceListener(){
        super(new VehiclePlaceEvent());
    }

    @EventHandler
    public void onVehiclePlace(final PlayerInteractEvent event) {
        this.event = event;
        player = event.getPlayer();

        final Action action = event.getAction();
        final ItemStack item = event.getItem();
        final Block clickedBlock = event.getClickedBlock();

        if (action != Action.RIGHT_CLICK_BLOCK || item == null || item.getType() == Material.AIR || item.getAmount() == 0 || !item.hasItemMeta() || clickedBlock == null) return;

        NBTItem nbtItem = new NBTItem(item);
        if (!nbtItem.hasTag("mtvehicles.kenteken")) return;

        String license = VehicleUtils.getLicensePlate(item);
        if (license == null || !VehicleUtils.existsByLicensePlate(license)) {
            ConfigModule.messagesConfig.sendMessage(player, Message.VEHICLE_NOT_FOUND);
            event.setCancelled(true);
            return;
        }

        VehiclePlaceEvent api = (VehiclePlaceEvent) getAPI();
        Location spawnLoc = clickedBlock.getLocation();

        if (ConfigModule.vehicleDataConfig.getType(license).isBoat()) { //placing boats (on top of a water body)
            while (spawnLoc.getBlock().getType().toString().contains("WATER")) {
                spawnLoc.add(0, 1, 0);
                if (spawnLoc.getY() >= clickedBlock.getLocation().getY() + 512) break;
            }
        }

        api.setLocation(spawnLoc);
        api.setLicensePlate(license);
        callAPI();
        if (isCancelled()) return;

        Location loc = api.getLocation();
        Vehicle vehicle = VehicleUtils.getVehicle(license);
        if (vehicle == null) return;

        if (event.getHand() != EquipmentSlot.HAND) {
            event.setCancelled(true);
            player.sendMessage(TextUtils.colorize(ConfigModule.messagesConfig.getMessage(Message.WRONG_HAND)));
            return;
        }

        if (ConfigModule.defaultConfig.isBlockWhitelistEnabled() && !ConfigModule.defaultConfig.blockWhiteList().contains(clickedBlock.getType())) {
            ConfigModule.messagesConfig.sendMessage(player, Message.BLOCK_NOT_IN_WHITELIST);
            return;
        }

        if (!ConfigModule.defaultConfig.canProceedWithAction(RegionAction.PLACE, vehicle.getVehicleType(), loc, player)) {
            ConfigModule.messagesConfig.sendMessage(player, Message.CANNOT_DO_THAT_HERE);
            return;
        }

        Location location = loc.clone().add(0, 1, 0);

        if (!vehicle.getVehicleType().isBoat() && !VehicleUtils.hasSpawnSpace(location)) {
            ConfigModule.messagesConfig.sendMessage(player, Message.CANNOT_DO_THAT_HERE);
            return;
        }

        VehicleUtils.spawnVehicle(license, location);
        player.getInventory().remove(player.getEquipment().getItemInHand());
        player.sendMessage(TextUtils.colorize(ConfigModule.messagesConfig.getMessage(Message.VEHICLE_PLACE).replace("%p%", vehicle.getOwnerName())));
        event.setCancelled(true);
    }
}
