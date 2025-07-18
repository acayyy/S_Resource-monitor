package com.resourcemonitor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.List;

public class Main extends JavaPlugin implements Listener {
    
    private OperatingSystemMXBean osBean;
    private MemoryMXBean memoryBean;
    private DecimalFormat df;
    private ResourceMonitorGUI gui;
    
    @Override
    public void onEnable() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.df = new DecimalFormat("#.##");
        this.gui = new ResourceMonitorGUI(this);
        
        // Display introduction and branding
        getLogger().info("=====================================");
        getLogger().info("    ResourceMonitor Plugin v1.0.0    ");
        getLogger().info("         by S Development            ");
        getLogger().info("=====================================");
        getLogger().info("Real-time server resource monitoring");
        getLogger().info("Commands: /rm, /resourcemonitor");
        getLogger().info("Overlay Hotkey: Type 'overlay' in chat");
        getLogger().info("Plugin successfully enabled!");
        getLogger().info("=====================================");
        
        // Register command
        getCommand("resourcemonitor").setExecutor(this);
        
        // Register GUI event listener and main listener for hotkeys
        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        getLogger().info("=====================================");
        getLogger().info("  ResourceMonitor Plugin disabled    ");
        getLogger().info("         by S Development            ");
        getLogger().info("       Thank you for using!          ");
        getLogger().info("=====================================");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("resourcemonitor") || 
            command.getName().equalsIgnoreCase("rm")) {
            
            if (!sender.hasPermission("resourcemonitor.use")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }
            
            // Always open GUI for players, console gets basic info
            if (sender instanceof Player) {
                Player player = (Player) sender;
                
                // Welcome message for first-time users
                if (!gui.hasSeenWelcome(player)) {
                    player.sendMessage(ChatColor.GOLD + "==============================================");
                    player.sendMessage(ChatColor.GREEN + "       Welcome to ResourceMonitor!          ");
                    player.sendMessage(ChatColor.BLUE + "            by S Development                 ");
                    player.sendMessage(ChatColor.GOLD + "==============================================");
                    player.sendMessage(ChatColor.YELLOW + "Commands: " + ChatColor.WHITE + "/rm or /resourcemonitor");
                    player.sendMessage(ChatColor.YELLOW + "Overlay Hotkey: " + ChatColor.WHITE + "Type 'overlay' in chat");
                    player.sendMessage(ChatColor.GRAY + "Click the gray wool for overlay mode!");
                    gui.markWelcomeSeen(player);
                }
                
                gui.openGUI(player);
                return true;
            } else {
                // Console info
                sender.sendMessage(ChatColor.GREEN + "=== Server Resource Information ===");
                
                // CPU info
                double cpuUsage = getCpuUsage();
                String cpuColor = getCpuColor(cpuUsage);
                sender.sendMessage(ChatColor.YELLOW + "CPU Usage: " + cpuColor + df.format(cpuUsage) + "%");
                
                // Memory info
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long usedMemory = totalMemory - runtime.freeMemory();
                long maxMemory = runtime.maxMemory();
                double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
                String memColor = getMemoryColor(memoryUsagePercent);
                
                sender.sendMessage(ChatColor.YELLOW + "Memory Usage: " + memColor + df.format(memoryUsagePercent) + "%");
                sender.sendMessage(ChatColor.YELLOW + "Used Memory: " + ChatColor.WHITE + formatBytes(usedMemory));
                sender.sendMessage(ChatColor.YELLOW + "Max Memory: " + ChatColor.WHITE + formatBytes(maxMemory));
                sender.sendMessage(ChatColor.YELLOW + "Total Memory: " + ChatColor.WHITE + formatBytes(totalMemory));
                
                // Server info
                int onlinePlayers = Bukkit.getOnlinePlayers().size();
                int maxPlayers = Bukkit.getMaxPlayers();
                sender.sendMessage(ChatColor.YELLOW + "Players Online: " + ChatColor.AQUA + onlinePlayers + "/" + maxPlayers);
                
                // Plugin count
                List<Plugin> plugins = List.of(Bukkit.getPluginManager().getPlugins());
                sender.sendMessage(ChatColor.YELLOW + "Loaded Plugins: " + ChatColor.GREEN + plugins.size());
                
                return true;
            }
        }
        return false;
    }
    
    public double getCpuUsage() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
        }
        return osBean.getSystemLoadAverage();
    }
    
    public String getCpuColor(double cpuUsage) {
        if (cpuUsage < 30) return ChatColor.GREEN.toString();
        else if (cpuUsage < 70) return ChatColor.YELLOW.toString();
        else return ChatColor.RED.toString();
    }
    
    public String getMemoryColor(double memoryUsage) {
        if (memoryUsage < 50) return ChatColor.GREEN.toString();
        else if (memoryUsage < 80) return ChatColor.YELLOW.toString();
        else return ChatColor.RED.toString();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return df.format(bytes / Math.pow(1024, exp)) + " " + pre + "B";
    }
    
    // Hotkey system - F3+R simulation via chat commands
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage().toLowerCase();
        Player player = event.getPlayer();
        
        // Check for overlay toggle hotkey simulation
        if (message.equals("f3r") || message.equals("/f3r") || message.equals("rmtoggle") || message.equals("overlay")) {
            event.setCancelled(true);
            
            // Add debug message
            player.sendMessage(ChatColor.YELLOW + "Hotkey detected: " + message);
            
            // Run on main thread
            Bukkit.getScheduler().runTask(this, () -> {
                try {
                    if (gui.isOverlayActive(player)) {
                        gui.disableOverlay(player);
                        player.sendMessage(ChatColor.RED + "✕ Overlay disabled (Hotkey)");
                    } else {
                        gui.enableOverlay(player);
                        player.sendMessage(ChatColor.GREEN + "✓ Overlay enabled (Hotkey) - Check action bar!");
                    }
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error toggling overlay: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
    
    // Alternative hotkey via sneak+jump (experimental)
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is trying the shift+space combo for overlay
        if (event.isSneaking()) {
            // Store that player is sneaking for potential combo
            // This would need additional tracking for jump events
            // For now, we'll use the chat-based hotkey system
        }
    }
    
    // Public method to get GUI instance
    public ResourceMonitorGUI getGUI() {
        return gui;
    }
}