package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Item;

import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.WorldListener;
import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

import net.minecraft.server.Entity;
import net.minecraft.server.WorldServer;

public class EntityManager {

	private static Set<Entity> entities = new HashSet<Entity>();
	private static Map<World, WorldListener> listeners = new HashMap<World, WorldListener>();

	public static void init() {
		for (WorldServer world : WorldUtil.getWorlds()) {
			EntityWorldWatcher listener = new EntityWorldWatcher(world);
			listener.enable();
			listeners.put(world.getWorld(), listener);
		}
	}

	public static void deinit() {
		entities = new HashSet<Entity>();
		for (WorldListener listener : listeners.values()) {
			listener.disable();
		}
		listeners = new HashMap<World, WorldListener>();
	}

	public static void deinit(World world) {
		WorldListener listener = listeners.remove(world);
		if (listener != null) {
			listener.disable();
		}
		new Operation(world) {
			@Override
			public void run() {
				this.doEntities();
			}

			@Override
			public void handle(Entity entity) {
				removeEntity(entity);
			}
		};
	}

	public static void init(World world) {
		if (WorldListener.isValid()) {
			WorldServer ws = WorldUtil.getNative(world);
			EntityWorldWatcher listener = new EntityWorldWatcher(ws);
			listener.enable();
			listeners.put(world, listener);
		}
	}

	public static boolean addEntity(Entity entity) {
		if (entities.add(entity)) {
			org.bukkit.entity.Entity bentity = entity.getBukkitEntity();
			if (!SpawnHandler.isIgnored(bentity)) {
				if (bentity instanceof Item && ItemUtil.isIgnored(bentity)) {
					SpawnHandler.ignoreSpawn(bentity);
				} else {
					if (SpawnHandler.getEntityLimits(bentity).handleSpawn()) {
						return true;
					} else {
						entities.remove(entity);
						return false;
					}
				}
			}
		}
		return true;
	}

	public static void removeEntity(Entity entity) {
		if (entities.remove(entity)) {
			SpawnHandler.handleDespawn(entity.getBukkitEntity());
		}
	}
}
