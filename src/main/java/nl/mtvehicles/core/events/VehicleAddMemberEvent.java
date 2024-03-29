package nl.mtvehicles.core.events;

import nl.mtvehicles.core.events.interfaces.CanEditLicensePlate;
import nl.mtvehicles.core.events.interfaces.IsCancellable;
import nl.mtvehicles.core.infrastructure.models.MTVEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * On vehicle add rider (/vehicle addmember command). Members are players who may sit in the vehicle.
 */

public class VehicleAddMemberEvent extends MTVEvent implements IsCancellable, Cancellable, CanEditLicensePlate {

    private String licensePlate;
    private Player addedPlayer;

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public String getLicensePlate(){
        return licensePlate;
    }

    @Override
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    /**
     * Get the player who used /vehicle addmember command
     * @see #getPlayer()
     */
    public Player getAdder(){
        return super.getPlayer();
    }

    /**
     * Get the player who is being added as a member
     */
    public Player getAdded(){
        return addedPlayer;
    }

    /**
     * Set the player who is being added as a member
     */
    public void setAdded(Player player){
        this.addedPlayer = player;
    }

}
