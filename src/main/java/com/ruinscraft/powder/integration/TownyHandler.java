package com.ruinscraft.powder.integration;

import java.util.Map.Entry;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.PreDeleteTownEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.event.TownUnclaimEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.ruinscraft.powder.PowderPlugin;
import com.ruinscraft.powder.model.Powder;
import com.ruinscraft.powder.model.PowderTask;
import com.ruinscraft.powder.model.tracker.Tracker;

/**
 * Handles Towny-related features and events
 *
 */
public class TownyHandler implements Listener {

	private TownyAPI townyAPI;
	private int maxPerTown;

	public TownyHandler(int maxPerTown) {
		this.townyAPI = TownyAPI.getInstance();
		this.maxPerTown = maxPerTown;
	}

	/**
	 * Max amount of Powders a player can place per town
	 * @return int
	 */
	public int getMaxPerTown() {
		return this.maxPerTown;
	}

	/**
	 * Checks if location is safe with Towny to put a Powder in
	 * @param player
	 * @param location
	 * @return if safe to place Powder
	 */
	public boolean checkLocation(Player player, Location location) {
		TownBlock block = townyAPI.getTownBlock(location);

		if (block == null || !block.hasTown()) return false;
		Town town;
		Resident resident;
		try {
			town = block.getTown();
			resident = townyAPI.getDataSource().getResident(player.getName());
		} catch (NotRegisteredException e) {
			return false;
		}
		if (!town.hasResident(player.getName())) return false;
		if (!townyAPI.isActiveResident(resident)) return false;

		// check if above player limit
		List<PowderTask> userCreatedPowders = PowderPlugin.get().getPowderHandler().getCreatedPowderTasks(player);
		if (userCreatedPowders.size() > PowderPlugin.get().getMaxCreatedPowders()) return false;

		int amntInTown = 0;
		for (PowderTask powderTask : userCreatedPowders) {
			for (Entry<Powder, Tracker> entry : powderTask.getPowders().entrySet()) {
				Tracker tracker = entry.getValue();

				Location trackerLocation = tracker.getCurrentLocation();
				if (townyAPI.getTownUUID(trackerLocation) != null) {
					if (townyAPI.getTownUUID(trackerLocation).equals(town.getUuid())) {
						amntInTown++;
					}
				}
				
			}
		}
		if (amntInTown > this.maxPerTown) return false; 

		return true;
	}

	/**
	 * Check whether a player has permission to place Powders in this Town
	 * @param town
	 * @param player
	 * @return if a player has permission to place Powders in this Town
	 */
	public boolean hasPermissionForPowder(Town town, Player player) {
		// check if they have build perm
		return true;
	}

	/**
	 * Checks if Powder is too close to the edge of the Town
	 * @param town
	 * @param location
	 * @return if the Powder is too close to the edge of the Town
	 */
	public boolean canPlacePowdersInTown(Town town, Location location) {
		// check if too close to side, like in P2Handler
		return true;
	}

	// removes Powders that end up in the wilderness after a Town unclaims an area
	@EventHandler
	public void onTownUnclaimEvent(TownUnclaimEvent event) {
		WorldCoord worldCoord = event.getWorldCoord();
		Chunk chunk = Bukkit.getWorlds().get(0).getChunkAt(worldCoord.getX(), worldCoord.getZ());

		for (PowderTask powderTask : PowderPlugin.get().getPowderHandler().getCreatedPowderTasks()) {
			for (Entry<Powder, Tracker> entry : powderTask.getPowders().entrySet()) {
				Tracker tracker = entry.getValue();
				Location trackerSpot = tracker.getCurrentLocation();
				if (townyAPI.getTownUUID(trackerSpot) == null) {
					powderTask.removePowder(entry.getKey());
					continue;
				}
				if (trackerSpot.getChunk().equals(chunk)) {
					powderTask.removePowder(entry.getKey());
				}
			}
		}
	}

	// removes Powders that were in a Town that is now being deleted
	@EventHandler
	public void onPreDeleteTownEvent(PreDeleteTownEvent event) {
		Town town = event.getTown();

		for (PowderTask powderTask : PowderPlugin.get().getPowderHandler().getCreatedPowderTasks()) {
			for (Entry<Powder, Tracker> entry : powderTask.getPowders().entrySet()) {
				Powder powder = entry.getKey();
				Tracker tracker = entry.getValue();

				UUID townUUID = townyAPI.getTownUUID(tracker.getCurrentLocation());

				if (town.getUuid().equals(townUUID)) {
					powderTask.removePowder(powder);
				}
			}
		}
	}

	// removes Powders from a Town that removes the owner of those Powders
	@EventHandler
	public void onTownRemoveResidentEvent(TownRemoveResidentEvent event) {
		Town town = event.getTown();
		Player player = townyAPI.getPlayer(event.getResident());

		for (PowderTask powderTask : PowderPlugin.get().getPowderHandler().getCreatedPowderTasks(player)) {
			for (Entry<Powder, Tracker> entry : powderTask.getPowders().entrySet()) {
				Powder powder = entry.getKey();
				Tracker tracker = entry.getValue();

				UUID townUUID = townyAPI.getTownUUID(tracker.getCurrentLocation());

				if (town.getUuid().equals(townUUID)) {
					powderTask.removePowder(powder);
				}
			}
		}
	}

}
