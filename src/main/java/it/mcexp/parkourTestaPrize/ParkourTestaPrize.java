package it.mcexp.parkourTestaPrize;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ParkourTestaPrize extends JavaPlugin implements Listener {

    private File configFile;
    private File dataFile;
    private FileConfiguration config;
    private FileConfiguration data;

    private Set<Location> headLocations;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        loadConfig();
        loadData();
        loadHeadLocations();
    }

    @Override
    public void onDisable() {
        saveData();
        saveConfig();
    }

    private void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHeadLocations() {
        headLocations = new HashSet<>();
        List<String> savedLocations = config.getStringList("head-locations");

        for (String loc : savedLocations) {
            String[] parts = loc.split(",");
            if (parts.length == 4) {
                String worldName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);

                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                headLocations.add(location);
            }
        }
    }

    private void saveHeadLocations() {
        List<String> savedLocations = new ArrayList<>();
        for (Location loc : headLocations) {
            savedLocations.add(loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ());
        }
        config.set("head-locations", savedLocations);

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onHeadClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.PLAYER_HEAD) {
            return;
        }

        Location clickedLocation = event.getClickedBlock().getLocation();
        if (!headLocations.contains(clickedLocation)) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = player.getName();
        Set<String> clickedPlayers = new HashSet<>(data.getStringList("clicked-players"));

        if (clickedPlayers.contains(playerName)) {
            player.sendMessage("Hai già riscattato il premio!");
            return;
        }

        clickedPlayers.add(playerName);
        data.set("clicked-players", new ArrayList<>(clickedPlayers));
        saveData();

        List<String> commands = config.getStringList("commands");
        for (String command : commands) {
            command = command.replace("{player}", playerName);
            if (command.startsWith("msg:")) {
                String message = command.substring(4);
                message = ChatColor.translateAlternateColorCodes('&', message);
                player.sendMessage(message);
            } else {
                command = ChatColor.translateAlternateColorCodes('&', command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("setheadp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Questo comando può essere usato solo da un giocatore.");
                return true;
            } else if (sender.hasPermission("parkourtestaprize.sethead")) {

            }

            Player player = (Player) sender;
            Location location = player.getTargetBlockExact(5).getLocation();

            if (location == null || location.getBlock().getType() != Material.PLAYER_HEAD) {
                player.sendMessage("Devi guardare una testa per poterla impostare!");
                return true;
            }

            headLocations.add(location);
            saveHeadLocations();
            player.sendMessage("Testa impostata con successo!");
            return true;
        }
        return false;
    }
}