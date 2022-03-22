package nl.mtvehicles.core.listeners;

import de.tr7zw.changeme.nbtapi.NBTItem;
import nl.mtvehicles.core.infrastructure.helpers.ItemUtils;
import nl.mtvehicles.core.infrastructure.helpers.MenuUtils;
import nl.mtvehicles.core.infrastructure.helpers.TextUtils;
import nl.mtvehicles.core.Main;
import nl.mtvehicles.core.infrastructure.modules.ConfigModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onLicenseChat(AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".kenteken") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".kenteken")) {
            e.setCancelled(true);

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);

                if (!(ConfigModule.vehicleDataConfig.getConfig().get("vehicle." + e.getMessage() + ".skinItem") == null)) {
                    ConfigModule.messagesConfig.sendMessage(p, "actionFailedDupLicense");
                    MenuUtils.menuEdit(p);
                    ItemUtils.edit.put(p.getUniqueId() + ".kenteken", false);
                    return;
                }
                for (String s : ConfigModule.vehicleDataConfig.getConfig().getConfigurationSection("vehicle." + ken).getKeys(false)) {
                    ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + e.getMessage() + "." + s, ConfigModule.vehicleDataConfig.getConfig().get("vehicle." + ken + "." + s));
                }

                ConfigModule.vehicleDataConfig.save();
                p.getInventory().setItemInMainHand(ItemUtils.carItem2(ConfigModule.vehicleDataConfig.getConfig().getInt("vehicle." + ken + ".skinDamage"), ConfigModule.vehicleDataConfig.getConfig().getString("vehicle." + ken + ".name"), ConfigModule.vehicleDataConfig.getConfig().getString("vehicle." + ken + ".skinItem"), e.getMessage()));

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.menuEdit(p));

                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".kenteken", false);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken, null);
                ConfigModule.vehicleDataConfig.save();
                return;
            }

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.menuEdit(p));

            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".kenteken", false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNaamChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".naam") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".naam")) {
            e.setCancelled(true);

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".name", e.getMessage());
                ConfigModule.vehicleDataConfig.save();
                p.getInventory().setItemInMainHand(ItemUtils.carItem2(ConfigModule.vehicleDataConfig.getConfig().getInt("vehicle." + ken + ".skinDamage"), e.getMessage(), ConfigModule.vehicleDataConfig.getConfig().getString("vehicle." + ken + ".skinItem"), ken));
                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".naam", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.menuEdit(p));
                return;
            }

            MenuUtils.menuEdit(p);
            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".naam", false);

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.menuEdit(p));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBenzineChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".benzine") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".benzine")) {
            e.setCancelled(true);

            if (!isI(e.getMessage(), p)) {
                MenuUtils.benzineEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".benzine", false);
                return;
            }

            if (Integer.parseInt(e.getMessage()) > 100) {
                MenuUtils.benzineEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".benzine", false);
                p.sendMessage(TextUtils.colorize("&cLetop! Het cijfer moet onder de 100 zijn!"));
                return;
            }

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".benzine", Double.valueOf(e.getMessage()));
                ConfigModule.vehicleDataConfig.save();
                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".benzine", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.benzineEdit(p));
                return;
            }

            MenuUtils.benzineEdit(p);
            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".benzine", false);

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.menuEdit(p));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBenzineVerbruikChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".benzineverbruik") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".benzineverbruik")) {
            e.setCancelled(true);

            if (!isD(e.getMessage(), p)) {
                MenuUtils.benzineEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".benzineverbruik", false);
                return;
            }

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".benzineVerbruik", Double.valueOf(e.getMessage()));
                ConfigModule.vehicleDataConfig.save();
                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".benzineverbruik", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.benzineEdit(p));
                return;
            }

            MenuUtils.benzineEdit(p);
            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".benzine", false);

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.menuEdit(p));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKofferbakrowsChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".kofferbakRows") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".kofferbakRows")) {
            e.setCancelled(true);

            if (e.getMessage().toLowerCase().contains("annule")) {
                MenuUtils.kofferbakEdit(p);
                ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
                ItemUtils.edit.put(p.getUniqueId() + ".kofferbakRows", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.kofferbakEdit(p));
            }

            if (!isI(e.getMessage(), p)) {
                MenuUtils.kofferbakEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".kofferbakRows", false);
                return;
            }

            int input = Integer.parseInt(e.getMessage());
            if (input < 1 || input > 6) {
                MenuUtils.kofferbakEdit(p);
                ConfigModule.messagesConfig.sendMessage(p, "invalidInput");
                ItemUtils.edit.put(p.getUniqueId() + ".kofferbakRows", false);
                return;
            }

            String ken = getLicensePlate(p);
            ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".kofferbakRows", input);
            ConfigModule.vehicleDataConfig.save();
            ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
            ItemUtils.edit.put(p.getUniqueId() + ".kofferbakRows", false);

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.kofferbakEdit(p));

        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAcceleratieSpeedChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".acceleratieSpeed") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".acceleratieSpeed")) {
            e.setCancelled(true);

            if (!isD(e.getMessage(), p)) {
                MenuUtils.speedEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".acceleratieSpeed", false);
                return;
            }

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".acceleratieSpeed", Double.valueOf(e.getMessage()));
                ConfigModule.vehicleDataConfig.save();
                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".acceleratieSpeed", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
                return;
            }

            MenuUtils.benzineEdit(p);
            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".acceleratieSpeed", false);
            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMaxSpeedChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".maxSpeed") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".maxSpeed")) {
            e.setCancelled(true);

            if (!isD(e.getMessage(), p)) {
                MenuUtils.speedEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".maxSpeed", false);
                return;
            }

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".maxSpeed", Double.valueOf(e.getMessage()));
                ConfigModule.vehicleDataConfig.save();
                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".maxSpeed", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
                return;
            }

            MenuUtils.benzineEdit(p);
            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".maxSpeed", false);

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBrakingSpeedChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".brakingSpeed") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".brakingSpeed")) {
            e.setCancelled(true);

            if (!isD(e.getMessage(), p)) {
                MenuUtils.speedEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".brakingSpeed", false);
                return;
            }

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".brakingSpeed", Double.valueOf(e.getMessage()));
                ConfigModule.vehicleDataConfig.save();
                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".brakingSpeed", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
                return;
            }

            MenuUtils.benzineEdit(p);
            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".brakingSpeed", false);

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAftrekkenSpeedChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".aftrekkenSpeed") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".aftrekkenSpeed")) {
            e.setCancelled(true);

            if (!isD(e.getMessage(), p)) {
                MenuUtils.speedEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".aftrekkenSpeed", false);
                return;
            }

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".aftrekkenSpeed", Double.valueOf(e.getMessage()));
                ConfigModule.vehicleDataConfig.save();
                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".aftrekkenSpeed", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
                return;
            }

            MenuUtils.benzineEdit(p);
            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".aftrekkenSpeed", false);

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMaxSpeedBackwardsChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".maxSpeedBackwards") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".maxSpeedBackwards")) {
            e.setCancelled(true);

            if (!isD(e.getMessage(), p)) {
                MenuUtils.speedEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".maxSpeedBackwards", false);
                return;
            }

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".maxSpeedBackwards", Double.valueOf(e.getMessage()));
                ConfigModule.vehicleDataConfig.save();
                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".maxSpeedBackwards", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
                return;
            }

            MenuUtils.benzineEdit(p);
            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".maxSpeedBackwards", false);

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRotateSpeedChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (ItemUtils.edit.get(p.getUniqueId() + ".rotateSpeed") == null) return;

        if (ItemUtils.edit.get(p.getUniqueId() + ".rotateSpeed")) {
            e.setCancelled(true);

            if (!isI(e.getMessage(), p)) {
                MenuUtils.speedEdit(p);
                ItemUtils.edit.put(p.getUniqueId() + ".rotateSpeed", false);
                return;
            }

            if (!e.getMessage().toLowerCase().contains("annule")) {
                String ken = getLicensePlate(p);
                ConfigModule.vehicleDataConfig.getConfig().set("vehicle." + ken + ".rotateSpeed", Integer.parseInt(e.getMessage()));
                ConfigModule.vehicleDataConfig.save();
                ConfigModule.messagesConfig.sendMessage(p, "actionSuccessful");
                ItemUtils.edit.put(p.getUniqueId() + ".rotateSpeed", false);

                if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
                return;
            }

            MenuUtils.speedEdit(p);
            ConfigModule.messagesConfig.sendMessage(p, "actionCanceled");
            ItemUtils.edit.put(p.getUniqueId() + ".rotateSpeed", false);

            if (e.isAsynchronous()) Bukkit.getScheduler().runTask(Main.instance, () -> MenuUtils.speedEdit(p));
        }
    }

    public boolean isI(String str, Player p) {
        try {
            Integer.parseInt(str);
        } catch (Throwable e) {
            p.sendMessage(TextUtils.colorize("&cPay attention! It must be an integer. (For example: 7)"));
            return false;
        }
        return true;
    }

    public boolean isD(String str, Player p) {
        try {
            Double.valueOf(str);
        } catch (Throwable e) {
            p.sendMessage(TextUtils.colorize("&cPay attention! It must be a double. (For example: 0.02)"));
            return false;
        }
        return true;
    }

    /**
     * Get license plate of player's held vehicle
     * @param player Player
     * @return License plate of player's held vehicle
     */
    private String getLicensePlate(Player player){
        NBTItem nbt = new NBTItem(player.getInventory().getItemInMainHand());
        return nbt.getString("mtvehicles.kenteken");
    }
}