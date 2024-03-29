package net.problemzone.knockit.modules.maps;

import net.problemzone.knockit.Main;
import net.problemzone.knockit.exceptions.InvalidConfigException;
import net.problemzone.knockit.util.Countdown;
import net.problemzone.knockit.util.Language;
import net.problemzone.knockit.util.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class MapManager {

    public Queue<MapSpawnpoint> maps = new LinkedList<>();
    private static MapManager instance;
    private MapSpawnpoint currentMap;
    private final int ROTATION_TIME = 601;

    private MapManager() {

    }

    public static MapManager getInstance() {
        if (instance == null) instance = new MapManager();
        return instance;
    }

    public void addGameMaps() {
        ConfigurationSection mapsSection = ConfigManager.getInstance().getConfig().getConfigurationSection("Maps");
        if (mapsSection == null) throw new InvalidConfigException("The config.yml file is malformed. Please check that the map section is correct.");

        mapsSection.getKeys(false).forEach(key -> {
            MapSpawnpoint world = new MapSpawnpoint("Maps." + key);
            maps.offer(world);
            loadWorld(world.getWorld());
        });
    }

    private void loadWorld(String GAME_WORLD_NAME) {
        World gameWorld = Bukkit.getServer().createWorld(new WorldCreator(GAME_WORLD_NAME));
        Objects.requireNonNull(gameWorld).setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        gameWorld.setDifficulty(Difficulty.PEACEFUL);
        gameWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        gameWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    }

    public MapSpawnpoint getCurrentMap() {
        return currentMap;
    }

    public void startMapRotation() {
        currentMap = maps.poll();
        maps.offer(currentMap);

        MapManager.MapSpawnpoint spawn = MapManager.getInstance().getCurrentMap();
        Location spawnpoint = spawn.getCoordinates().toLocation(Objects.requireNonNull(Bukkit.getWorld(spawn.getWorld())), (float) spawn.getYaw(), (float) spawn.getPitch());

        Bukkit.getOnlinePlayers().forEach(player -> player.teleport(spawnpoint));
        Countdown.createChatCountdown(ROTATION_TIME, Language.MAP_CHANGE);

        new BukkitRunnable() {
            @Override
            public void run() {
                startMapRotation();
            }
        }.runTaskLater(Main.getInstance(), ROTATION_TIME * 20L);
    }

    public static class MapSpawnpoint {

        private final String world;
        private final Vector coordinates;

        private final double Pitch;
        private final double Yaw;
        private final double Deathheight;
        private final double Fightheight;


        public MapSpawnpoint(String path) {
            ConfigurationSection config = ConfigManager.getInstance().getConfig().getConfigurationSection(path);
            if (config == null) throw new InvalidConfigException("The config.yml file is malformed. Please check that the map section is correct.");

            this.world = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(config.getString("Welt")));
            this.coordinates = new Vector(config.getDouble("Spawn.X"), config.getDouble("Spawn.Y"), config.getDouble("Spawn.Z"));
            this.Pitch = config.isSet("Spawn.Pitch") ? config.getDouble("Spawn.Pitch") : 0;
            this.Yaw = config.isSet("Spawn.Yaw") ? config.getDouble("Spawn.Yaw") : 0;
            this.Deathheight = config.getDouble("Deathheight");
            this.Fightheight = config.getDouble("Fightheight");
        }

        public String getWorld() {
            return world;
        }

        public double getDeathheight() {
            return Deathheight;
        }

        public double getFightheight() {
            return Fightheight;
        }

        public Vector getCoordinates() {
            return coordinates;
        }

        public double getPitch() {
            return Pitch;
        }

        public double getYaw() {
            return Yaw;
        }
    }
}