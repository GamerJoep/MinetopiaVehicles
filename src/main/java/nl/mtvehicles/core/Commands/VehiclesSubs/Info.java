package nl.mtvehicles.core.Commands.VehiclesSubs;

import nl.mtvehicles.core.Infrastructure.Models.MTVehicleSubCommand;
import nl.mtvehicles.core.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.Console;

public class Info extends MTVehicleSubCommand {
    @Override
    public boolean execute(CommandSender sender, Command cmd, String s, String[] args) {


        sendMessage("&2-=-=-=-=-=-=-=-=-=-=-");
        sendMessage("");
        sendMessage("&a MT-Vehicles &2is gemaakt door: &aGamerJoep_ &2en &aJeffrey. &2wil je meer weten? Ga dan naar &ahttps://mtvehicles.nl");
        sendMessage("");
        sendMessage("&2-=-=-=-=-=-=-=-=-=-=-");



        return false;

    }
}