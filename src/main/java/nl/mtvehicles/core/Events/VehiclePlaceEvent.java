package nl.mtvehicles.core.Events;

import nl.mtvehicles.core.Infrastructure.Helpers.ItemFactory;
import nl.mtvehicles.core.Infrastructure.Helpers.NBTUtils;
import nl.mtvehicles.core.Infrastructure.Helpers.TextUtils;
import nl.mtvehicles.core.Infrastructure.Models.Vehicle;
import nl.mtvehicles.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VehiclePlaceEvent implements Listener {
    @EventHandler
    public void onVehiclePlace(final PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        final Action action = e.getAction();
        final ItemStack item = e.getItem();

        if (e.getItem() == null
                || (!e.getItem().hasItemMeta()
                || !(NBTUtils.contains(item, "mtvehicles.kenteken")))
                || e.getClickedBlock() == null
        ) {return;
}
        if (e.getHand() != EquipmentSlot.HAND) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("wrongHand")));
            return;
        }
        String ken = NBTUtils.getString(item, "mtvehicles.kenteken");
        if (ken == null) {
            return;
        }
        if (!Vehicle.existsByPlate(ken)) {
            Main.messagesConfig.sendMessage(p, "vehicleNotFound");
            e.setCancelled(true);
            return;
        }
        if (!action.equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (Main.defaultConfig.isBlockWhitelistEnabled()
            && !Main.defaultConfig.blockWhiteList().contains(e.getClickedBlock().getType())) {
            Main.messagesConfig.sendMessage(p, "blockNotInWhitelist");
            return;
        }
        e.setCancelled(true);
        Location loc = e.getClickedBlock().getLocation();
        Location location = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
        ArmorStand as = location.getWorld().spawn(location, ArmorStand.class);
        as.setVisible(false);
        as.setCustomName("MTVEHICLES_SKIN_" + ken);
        as.setHelmet(item);
        ArmorStand as2 = location.getWorld().spawn(location, ArmorStand.class);
        as2.setVisible(false);
        as2.setCustomName("MTVEHICLES_MAIN_" + ken);
        Vehicle vehicle = Vehicle.getByPlate(ken);
        List<Map<String, Double>> seats = (List<Map<String, Double>>) vehicle.getVehicleData().get("seats");
        p.getInventory().remove(p.getItemInHand());
        p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("vehiclePlace").replace("%p%", Bukkit.getOfflinePlayer(UUID.fromString(Vehicle.getByPlate(ken).getOwner().toString())).getName())));
        for (int i = 1; i <= seats.size(); i++) {
            Map<String, Double> seat = seats.get(i - 1);
            if (i == 1) {
                Location location2 = new Location(location.getWorld(), location.getX() + seat.get("x"), location.getY() + seat.get("y"), location.getZ() + seat.get("z"));
                ArmorStand as3 = location2.getWorld().spawn(location2, ArmorStand.class);
                as3.setCustomName("MTVEHICLES_MAINSEAT_" + ken);
                as3.setGravity(false);
                as3.setVisible(false);
            }
        }
        List<Map<String, Double>> wiekens = (List<Map<String, Double>>) vehicle.getVehicleData().get("wiekens");
        if (Main.vehicleDataConfig.getConfig().getString("vehicle." + ken + ".vehicleType").contains("HELICOPTER")) {
            for (int i = 1; i <= wiekens.size(); i++) {
                Map<?, ?> seat = wiekens.get(i - 1);
                if (i == 1) {
                    Location location2 = new Location(location.getWorld(), location.getX() + (Double) seat.get("z"), (Double) location.getY() + (Double) seat.get("y"), location.getZ() + (Double) seat.get("x"));
                    ArmorStand as3 = location2.getWorld().spawn(location2, ArmorStand.class);
                    as3.setCustomName("MTVEHICLES_WIEKENS_" + ken);
                    as3.setGravity(false);
                    as3.setVisible(false);
                    if (Main.defaultConfig.getConfig().getBoolean("wiekens-always-on") == true) {
                        ItemStack car = (new ItemFactory(Material.getMaterial("DIAMOND_HOE"))).setDurability((short) 1058).setName(TextUtils.colorize("&6Wieken")).setNBT("mtvehicles.kenteken", ken).toItemStack();
                        ItemMeta im = car.getItemMeta();
                        List<String> itemlore = new ArrayList<>();
                        itemlore.add(TextUtils.colorize("&a"));
                        itemlore.add(TextUtils.colorize("&a" + ken));
                        itemlore.add(TextUtils.colorize("&a"));
                        im.setLore(itemlore);
                        im.setUnbreakable(true);
                        car.setItemMeta(im);
                        as3.setHelmet((ItemStack) seat.get("item"));
                    }
                }
            }
        }
    }
}
