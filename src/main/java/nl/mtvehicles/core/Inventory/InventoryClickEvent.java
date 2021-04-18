package nl.mtvehicles.core.Inventory;

import nl.mtvehicles.core.Commands.VehiclesSubs.VehicleEdit;
import nl.mtvehicles.core.Commands.VehiclesSubs.VehicleMenu;
import nl.mtvehicles.core.Events.VehicleEntityEvent;
import nl.mtvehicles.core.Infrastructure.Helpers.MenuUtils;
import nl.mtvehicles.core.Infrastructure.Helpers.NBTUtils;
import nl.mtvehicles.core.Infrastructure.Helpers.TextUtils;
import nl.mtvehicles.core.Infrastructure.Helpers.ItemUtils;
import nl.mtvehicles.core.Infrastructure.Models.Vehicle;
import nl.mtvehicles.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryClickEvent implements Listener {

    public HashMap<UUID, ItemStack> vehicleMenu = new HashMap<>();
    public HashMap<UUID, Inventory> skinMenu = new HashMap<>();
    public HashMap<UUID, Integer> intSave = new HashMap<>();

    @EventHandler
    public void onClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        if (!e.getCurrentItem().hasItemMeta()) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getView().getTitle().contains("Vehicle Menu")) {
            e.setCancelled(true);
            List<Map<?, ?>> vehicles = Main.vehiclesConfig.getConfig().getMapList("voertuigen");
            List<Map<?, ?>> skins = (List<Map<?, ?>>) vehicles.get(e.getRawSlot()).get("cars");
            intSave.put(p.getUniqueId(), e.getRawSlot());
            Inventory inv = Bukkit.createInventory(null, 54, "Choose your vehicle");
            for (int i = 36; i <= 44; i++) {
                inv.setItem(i, ItemUtils.mItem("STAINED_GLASS_PANE", 1, (short) 0, "&c", "&c"));
            }
            inv.setItem(47, ItemUtils.mItem("BARRIER", 1, (short) 0, "&4Sluiten", "&cDruk hier om het menu te sluiten!"));
            inv.setItem(51, ItemUtils.mItem("WOOD_DOOR", 1, (short) 0, "&6Terug", "&eDruk hier om terug te gaan!"));
            for (Map<?, ?> skin : skins) {
                inv.addItem(ItemUtils.carItem2((Integer) skin.get("itemDamage"), ((String) skin.get("name")), (String) skin.get("SkinItem")));
            }
            skinMenu.put(p.getUniqueId(), inv);
            p.openInventory(inv);
            return;
        }
        if (e.getView().getTitle().contains("Choose your vehicle")) {
            if (e.getCurrentItem().equals(ItemUtils.mItem("BARRIER", 1, (short) 0, "&4Sluiten", "&cDruk hier om het menu te sluiten!"))) {
                e.setCancelled(true);
                p.closeInventory();
                return;
            }
            if (e.getCurrentItem().equals(ItemUtils.mItem("WOOD_DOOR", 1, (short) 0, "&6Terug", "&eDruk hier om terug te gaan!"))) {
                p.openInventory(VehicleMenu.beginMenu.get(p.getUniqueId()));
                e.setCancelled(true);
                return;
            }
            if (e.getCurrentItem().equals(ItemUtils.mItem("STAINED_GLASS_PANE", 1, (short) 0, "&c", "&c"))) {
                e.setCancelled(true);
                return;
            }
            e.setCancelled(true);
            vehicleMenu.put(p.getUniqueId(), e.getCurrentItem());
            Inventory inv = Bukkit.createInventory(null, 27, "Confirm getting vehicle");
            inv.setItem(11, ItemUtils.woolItem("WOOL", "RED_WOOL", 1, (short) 14, "&4Annuleren", "&7Druk hier om het te annuleren."));
            inv.setItem(15, ItemUtils.woolItem("WOOL", "LIME_WOOL", 1, (short) 5, "&aCreate Vehicle", "&7Druk hier als je het voertuigen wilt aanmaken en op je naam wilt zetten"));
            p.openInventory(inv);
        }
        if (e.getView().getTitle().contains("Confirm getting vehicle")) {
            e.setCancelled(true);
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Annuleren")) {
                p.openInventory(skinMenu.get(p.getUniqueId()));
            }
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Create Vehicle")) {
                List<Map<?, ?>> vehicles = Main.vehiclesConfig.getConfig().getMapList("voertuigen");
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("completedvehiclegive")));
                p.getInventory().addItem(vehicleMenu.get(p.getUniqueId()));
                String kenteken = NBTUtils.getString(vehicleMenu.get(p.getUniqueId()), "mtvehicles.kenteken");
                String naam = NBTUtils.getString(vehicleMenu.get(p.getUniqueId()), "mtvehicles.naam");
                Vehicle vehicle = new Vehicle();
                List<String> members = Main.vehicleDataConfig.getConfig().getStringList("voertuig." + kenteken + ".members");
                List<String> riders = Main.vehicleDataConfig.getConfig().getStringList("voertuig." + kenteken + ".riders");
                List<String> kof = Main.vehicleDataConfig.getConfig().getStringList("voertuig." + kenteken + ".kofferbakData");
                vehicle.setLicensePlate(kenteken);
                vehicle.setName(naam);
                vehicle.setVehicleType((String) vehicles.get(intSave.get(p.getUniqueId())).get("vehicleType"));
                vehicle.setSkinDamage(vehicleMenu.get(p.getUniqueId()).getDurability());
                vehicle.setSkinItem(vehicleMenu.get(p.getUniqueId()).getType().toString());
                vehicle.setGlow(false);
                vehicle.setBenzineEnabled((Boolean) vehicles.get(intSave.get(p.getUniqueId())).get("benzineEnabled"));
                vehicle.setBenzine(100);
                vehicle.setKofferbak((Boolean) vehicles.get(intSave.get(p.getUniqueId())).get("kofferbakEnabled"));
                vehicle.setKofferbakRows(1);
                vehicle.setBenzineVerbruik(0.01);
                vehicle.setKofferbakData(kof);
                vehicle.setAcceleratieSpeed((Double) vehicles.get(intSave.get(p.getUniqueId())).get("acceleratieSpeed"));
                vehicle.setMaxSpeed((Double) vehicles.get(intSave.get(p.getUniqueId())).get("maxSpeed"));
                vehicle.setBrakingSpeed((Double) vehicles.get(intSave.get(p.getUniqueId())).get("brakingSpeed"));
                vehicle.setAftrekkenSpeed((Double) vehicles.get(intSave.get(p.getUniqueId())).get("aftrekkenSpeed"));
                vehicle.setRotateSpeed((Integer) vehicles.get(intSave.get(p.getUniqueId())).get("rotateSpeed"));
                vehicle.setMaxSpeedBackwards((Double) vehicles.get(intSave.get(p.getUniqueId())).get("maxSpeedBackwards"));
                vehicle.setOwner(p.getUniqueId().toString());
                vehicle.setRiders(riders);
                vehicle.setMembers(members);
                vehicle.save();
                p.closeInventory();
            }
        }

        if (e.getView().getTitle().contains("Vehicle Restore")) {
            if (e.getCurrentItem().equals(ItemUtils.mItem("STAINED_GLASS_PANE", 1, (short) 0, "&c", "&c"))) {
                e.setCancelled(true);
                return;
            }
            if (e.getCurrentItem().equals(ItemUtils.mItem("SPECTRAL_ARROW", 1, (short) 0, "&cVolgende Pagina", "&c"))){
                e.setCancelled(true);
                MenuUtils.restoreCMD(p , Integer.parseInt(e.getView().getTitle().replace("Vehicle Restore ", ""))+1, MenuUtils.restoreUUID.get("uuid"));
                return;
            }
            if (e.getCurrentItem().equals(ItemUtils.mItem("SPECTRAL_ARROW", 1, (short) 0, "&cVorige Pagina", "&c"))){
                e.setCancelled(true);
                if (!(Integer.parseInt(e.getView().getTitle().replace("Vehicle Restore ", ""))-1 < 1)) {
                    MenuUtils.restoreCMD(p, Integer.parseInt(e.getView().getTitle().replace("Vehicle Restore ", "")) - 1, MenuUtils.restoreUUID.get("uuid"));
                }
                return;
            }
            e.setCancelled(true);
            ItemStack car = e.getCurrentItem();
            p.getInventory().addItem(car);
        }
        if (e.getView().getTitle().contains("Vehicle Edit")) {
            e.setCancelled(true);
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Vehicle Settings")) {
                MenuUtils.menuEdit(p);
            }
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Benzine Settings")) {
                MenuUtils.benzineEdit(p);
            }
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Kofferbak Settings")) {
                MenuUtils.kofferbakEdit(p);
            }
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Member Settings")) {
                MenuUtils.membersEdit(p);
            }
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Speed Settings")) {
                MenuUtils.speedEdit(p);
            }
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Delete Vehicle")) {
                String ken = NBTUtils.getString(p.getInventory().getItemInMainHand(), "mtvehicles.kenteken");
                Main.vehicleDataConfig.getConfig().set("vehicle." + ken, null);
                Main.vehicleDataConfig.save();
                p.getInventory().getItemInMainHand().setAmount(0);
                p.closeInventory();
            }
        }

        if (e.getView().getTitle().contains("Vehicle Settings")) {
            e.setCancelled(true);
            String ken = NBTUtils.getString(p.getInventory().getItemInMainHand(), "mtvehicles.kenteken");
            if (e.getCurrentItem().equals(ItemUtils.mItem("BARRIER", 1, (short) 0, "&4Sluiten", "&cDruk hier om het menu te sluiten!"))) {
                e.setCancelled(true);
                p.closeInventory();
                return;
            }
            if (e.getCurrentItem().equals(ItemUtils.mItem("WOOD_DOOR", 1, (short) 0, "&6Terug", "&eDruk hier om terug te gaan!"))) {
                VehicleEdit.editMenu(p, p.getInventory().getItemInMainHand());
                e.setCancelled(true);
                return;
            }

            if (e.getCurrentItem().equals(ItemUtils.glowItem("BOOK", "&6Glow Aanpassen", "&7Huidige: &e" + Main.vehicleDataConfig.getConfig().getString("vehicle." + ken + ".isGlow")))) {
                Main.vehicleDataConfig.getConfig().set("vehicle." + ken + ".isGlow", false);
                ItemMeta im = p.getInventory().getItemInMainHand().getItemMeta();
                im.removeEnchant(Enchantment.ARROW_INFINITE);
                im.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
                p.getInventory().getItemInMainHand().setItemMeta(im);
                Main.vehicleDataConfig.save();
                MenuUtils.menuEdit(p);
            }

            if (e.getCurrentItem().equals(ItemUtils.mItem("BOOK", 1, (short) 0, "&6Glow Aanpassen", "&7Huidige: &e" + Main.vehicleDataConfig.getConfig().getString("vehicle." + ken + ".isGlow")))) {
                Main.vehicleDataConfig.getConfig().set("vehicle." + ken + ".isGlow", true);
                ItemMeta im = p.getInventory().getItemInMainHand().getItemMeta();
                im.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
                im.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
                p.getInventory().getItemInMainHand().setItemMeta(im);
                Main.vehicleDataConfig.save();
                MenuUtils.menuEdit(p);
            }

            if (e.getCurrentItem().equals(ItemUtils.mItem("PAPER", 1, (short) 0, "&6Kenteken Aanpassen", "&7Huidige: &e" + ken))) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeLicenseInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".kenteken", true);
            }

            if (e.getCurrentItem().getDurability() == (short) Main.vehicleDataConfig.getConfig().getInt("vehicle." + ken + ".skinDamage")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeNameInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".naam", true);
            }
        }
        if (e.getView().getTitle().contains("Vehicle Benzine")) {
            e.setCancelled(true);
            String ken = NBTUtils.getString(p.getInventory().getItemInMainHand(), "mtvehicles.kenteken");
            if (e.getCurrentItem().equals(ItemUtils.mItem("BARRIER", 1, (short) 0, "&4Sluiten", "&cDruk hier om het menu te sluiten!"))) {
                e.setCancelled(true);
                p.closeInventory();
                return;
            }
            if (e.getCurrentItem().equals(ItemUtils.mItem("WOOD_DOOR", 1, (short) 0, "&6Terug", "&eDruk hier om terug te gaan!"))) {
                VehicleEdit.editMenu(p, p.getInventory().getItemInMainHand());
                e.setCancelled(true);
                return;
            }
            String menuitem = NBTUtils.getString(e.getCurrentItem(), "mtvehicles.item");
            if (menuitem.contains("1")) {
                if (Main.vehicleDataConfig.getConfig().getBoolean("vehicle."+ken+".benzineEnabled")){
                    Main.vehicleDataConfig.getConfig().set("vehicle." + ken + ".benzineEnabled", false);
                    Main.vehicleDataConfig.save();
                } else {
                    Main.vehicleDataConfig.getConfig().set("vehicle." + ken + ".benzineEnabled", true);
                    Main.vehicleDataConfig.save();
                }
                MenuUtils.benzineEdit(p);
                Main.vehicleDataConfig.save();
                MenuUtils.benzineEdit(p);
            }
            if (menuitem.contains("2")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeNewBenzineInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".benzine", true);
            }
            if (menuitem.contains("3")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeNewBenzineInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".benzineverbruik", true);
            }
        }
        if (e.getView().getTitle().contains("Vehicle Kofferbak")) {
            e.setCancelled(true);
            if (e.getCurrentItem().equals(ItemUtils.mItem("BARRIER", 1, (short) 0, "&4Sluiten", "&cDruk hier om het menu te sluiten!"))) {
                e.setCancelled(true);
                p.closeInventory();
                return;
            }
            if (e.getCurrentItem().equals(ItemUtils.mItem("WOOD_DOOR", 1, (short) 0, "&6Terug", "&eDruk hier om terug te gaan!"))) {
                VehicleEdit.editMenu(p, p.getInventory().getItemInMainHand());
                e.setCancelled(true);
                return;
            }
            String ken = NBTUtils.getString(p.getInventory().getItemInMainHand(), "mtvehicles.kenteken");
            String menuitem = NBTUtils.getString(e.getCurrentItem(), "mtvehicles.item");
            if (menuitem.contains("1")) {
                if (Main.vehicleDataConfig.getConfig().getBoolean("vehicle."+ken+".kofferbak")){
                    Main.vehicleDataConfig.getConfig().set("vehicle." + ken + ".kofferbak", false);
                    Main.vehicleDataConfig.save();
                } else {
                    Main.vehicleDataConfig.getConfig().set("vehicle." + ken + ".kofferbak", true);
                    Main.vehicleDataConfig.save();
                }
                MenuUtils.kofferbakEdit(p);
                Main.vehicleDataConfig.save();
                MenuUtils.kofferbakEdit(p);
            }
            if (menuitem.contains("2")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeNewRowsInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".kofferbakRows", true);
            }
            if (menuitem.contains("3")) {
                p.closeInventory();
                VehicleEntityEvent.kofferbak(p, ken);
            }
        }
        if (e.getView().getTitle().contains("Vehicle Members")) {
            e.setCancelled(true);
            if (e.getCurrentItem().equals(ItemUtils.mItem("BARRIER", 1, (short) 0, "&4Sluiten", "&cDruk hier om het menu te sluiten!"))) {
                e.setCancelled(true);
                p.closeInventory();
                return;
            }
            if (e.getCurrentItem().equals(ItemUtils.mItem("WOOD_DOOR", 1, (short) 0, "&6Terug", "&eDruk hier om terug te gaan!"))) {
                VehicleEdit.editMenu(p, p.getInventory().getItemInMainHand());
                e.setCancelled(true);
                return;
            }
        }
        if (e.getView().getTitle().contains("Vehicle Speed")) {
            e.setCancelled(true);
            if (e.getCurrentItem().equals(ItemUtils.mItem("BARRIER", 1, (short) 0, "&4Sluiten", "&cDruk hier om het menu te sluiten!"))) {
                e.setCancelled(true);
                p.closeInventory();
                return;
            }
            if (e.getCurrentItem().equals(ItemUtils.mItem("WOOD_DOOR", 1, (short) 0, "&6Terug", "&eDruk hier om terug te gaan!"))) {
                VehicleEdit.editMenu(p, p.getInventory().getItemInMainHand());
                e.setCancelled(true);
                return;
            }
            String menuitem = NBTUtils.getString(e.getCurrentItem(), "mtvehicles.item");
            if (menuitem.contains("1")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeSpeedInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".acceleratieSpeed", true);

            }
            if (menuitem.contains("2")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeSpeedInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".maxSpeed", true);

            }
            if (menuitem.contains("3")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeSpeedInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".brakingSpeed", true);
            }
            if (menuitem.contains("4")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeSpeedInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".aftrekkenSpeed", true);

            }
            if (menuitem.contains("5")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeSpeedInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".rotateSpeed", true);
            }
            if (menuitem.contains("6")) {
                p.closeInventory();
                p.sendMessage(TextUtils.colorize(Main.messagesConfig.getMessage("typeSpeedInChat")));
                ItemUtils.edit.put(p.getUniqueId() + ".maxSpeedBackwards", true);
            }
        }
        if (e.getView().getTitle().contains("Benzine menu")) {
            e.setCancelled(true);
            p.getInventory().addItem(e.getCurrentItem());

        }
        if (e.getView().getTitle().contains("Voucher Redeem Menu")) {
            e.setCancelled(true);
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Ja")){
                p.sendMessage(Main.messagesConfig.getMessage(TextUtils.colorize("voucherRedeem")));
                String ken = NBTUtils.getString(p.getInventory().getItemInMainHand(), "mtvehicles.damage");
                Vehicle.getByDamage(Integer.parseInt(ken), p);
                p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount()-1);
                p.closeInventory();
            }
            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Nee")){
                p.closeInventory();
            }

        }

    }

}
