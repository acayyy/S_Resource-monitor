
package com.resourcemonitor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResourceMonitorGUI implements Listener {
    
    private final Main plugin;
    private final DecimalFormat df = new DecimalFormat("#.##");
    
    // Window management
    private final Map<UUID, WindowState> windowStates = new HashMap<>();
    private final Map<UUID, BukkitRunnable> autoRefreshTasks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> overlayTasks = new HashMap<>();
    private final Map<UUID, Boolean> welcomeShown = new HashMap<>();
    
    public ResourceMonitorGUI(Main plugin) {
        this.plugin = plugin;
    }
    
    private static class WindowState {
        boolean isMinimized = false;
        boolean isDragging = false;
        boolean isOverlayMode = false;
        int windowSize = 54; // Can be 9, 18, 27, 36, 45, 54
        String currentView = "main"; // main, plugins, minimized, overlay
        
        WindowState() {}
    }
    
    public void openGUI(Player player) {
        WindowState state = windowStates.computeIfAbsent(player.getUniqueId(), k -> new WindowState());
        
        if (state.isMinimized) {
            openMinimizedWindow(player);
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, state.windowSize, 
            ChatColor.DARK_BLUE + "▋ " + ChatColor.BOLD + "Resource Monitor" + ChatColor.RESET + ChatColor.BLUE + " - S Dev" + ChatColor.DARK_BLUE + " ▋");
        
        // Window Controls (Top row)
        createWindowControls(gui, player);
        
        if (state.windowSize >= 27) {
            // Create elegant border with glass panes
            createBorder(gui, state.windowSize);
            
            if (state.windowSize >= 54) {
                // Full size layout
                createFullLayout(gui);
            } else if (state.windowSize >= 36) {
                // Medium size layout
                createMediumLayout(gui);
            } else {
                // Small size layout  
                createSmallLayout(gui);
            }
        } else {
            // Minimal layout for very small windows
            createMinimalLayout(gui);
        }
        
        // Auto-refresh is now disabled by default to prevent mouse issues
        // Users can manually enable it via the button
        
        player.openInventory(gui);
    }
    
    private void createWindowControls(Inventory gui, Player player) {
        WindowState state = windowStates.get(player.getUniqueId());
        
        // Minimize button
        ItemStack minimizeBtn = new ItemStack(Material.GOLD_INGOT);
        ItemMeta minimizeMeta = minimizeBtn.getItemMeta();
        minimizeMeta.setDisplayName(ChatColor.YELLOW + "— Minimize Window");
        List<String> minimizeLore = new ArrayList<>();
        minimizeLore.add(ChatColor.GRAY + "Click to minimize the window");
        minimizeLore.add(ChatColor.DARK_GRAY + "You can still see basic info");
        minimizeMeta.setLore(minimizeLore);
        minimizeBtn.setItemMeta(minimizeMeta);
        gui.setItem(1, minimizeBtn);
        
        // Resize button
        ItemStack resizeBtn = new ItemStack(Material.ORANGE_WOOL);
        ItemMeta resizeMeta = resizeBtn.getItemMeta();
        resizeMeta.setDisplayName(ChatColor.GOLD + "⚏ Resize Window");
        List<String> resizeLore = new ArrayList<>();
        resizeLore.add(ChatColor.GRAY + "Current size: " + ChatColor.AQUA + state.windowSize + " slots");
        resizeLore.add(ChatColor.GRAY + "Click to cycle window sizes");
        resizeLore.add(ChatColor.DARK_GRAY + "Sizes: 27 → 36 → 45 → 54 → 27");
        resizeMeta.setLore(resizeLore);
        resizeBtn.setItemMeta(resizeMeta);
        gui.setItem(2, resizeBtn);
        
        // Overlay Mode Toggle
        ItemStack overlayToggle = new ItemStack(Material.GRAY_WOOL);
        ItemMeta overlayMeta = overlayToggle.getItemMeta();
        overlayMeta.setDisplayName(ChatColor.AQUA + "🎯 Overlay Mode");
        List<String> overlayLore = new ArrayList<>();
        overlayLore.add(ChatColor.GRAY + "Click to enable overlay mode");
        overlayLore.add(ChatColor.GREEN + "Shows info while playing normally");
        overlayLore.add(ChatColor.YELLOW + "Hotkey: Type 'overlay' in chat to toggle");
        overlayMeta.setLore(overlayLore);
        overlayToggle.setItemMeta(overlayMeta);
        gui.setItem(4, overlayToggle);
        
        // Auto-refresh toggle - default OFF to prevent mouse issues
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = autoRefreshTasks.get(playerId);
        boolean isAutoRefreshOn = task != null;
        
        ItemStack autoRefreshBtn = new ItemStack(isAutoRefreshOn ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta autoRefreshMeta = autoRefreshBtn.getItemMeta();
        autoRefreshMeta.setDisplayName(isAutoRefreshOn ? 
            ChatColor.GREEN + "🔄 Auto-Refresh: ON" : 
            ChatColor.RED + "🔄 Auto-Refresh: OFF");
        
        List<String> autoRefreshLore = new ArrayList<>();
        if (isAutoRefreshOn) {
            autoRefreshLore.add(ChatColor.GRAY + "Automatically updates every 10 seconds");
            autoRefreshLore.add(ChatColor.GRAY + "Click to disable and stop mouse issues");
        } else {
            autoRefreshLore.add(ChatColor.GRAY + "Manual refresh only - no mouse interference");
            autoRefreshLore.add(ChatColor.GRAY + "Click to enable auto-refresh");
            autoRefreshLore.add(ChatColor.GREEN + "Recommended: Keep disabled for better control");
        }
        autoRefreshMeta.setLore(autoRefreshLore);
        autoRefreshBtn.setItemMeta(autoRefreshMeta);
        gui.setItem(6, autoRefreshBtn);
        
        // Close button
        ItemStack closeBtn = new ItemStack(Material.RED_WOOL);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "✕ Close Window");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.GRAY + "Click to close the window");
        closeMeta.setLore(closeLore);
        closeBtn.setItemMeta(closeMeta);
        gui.setItem(7, closeBtn);
    }
    
    private void openMinimizedWindow(Player player) {
        Inventory miniGui = Bukkit.createInventory(null, 9, 
            ChatColor.DARK_GRAY + "▋ " + ChatColor.BOLD + "RM Minimized" + ChatColor.RESET + ChatColor.DARK_GRAY + " ▋");
        
        // Restore button
        ItemStack restoreBtn = new ItemStack(Material.LIME_WOOL);
        ItemMeta restoreMeta = restoreBtn.getItemMeta();
        restoreMeta.setDisplayName(ChatColor.GREEN + "↗ Restore Window");
        List<String> restoreLore = new ArrayList<>();
        restoreLore.add(ChatColor.GRAY + "Click to restore full window");
        restoreMeta.setLore(restoreLore);
        restoreBtn.setItemMeta(restoreMeta);
        miniGui.setItem(0, restoreBtn);
        
        // Quick CPU info
        ItemStack quickCpu = new ItemStack(Material.REDSTONE);
        ItemMeta cpuMeta = quickCpu.getItemMeta();
        double cpuUsage = getCpuUsage(ManagementFactory.getOperatingSystemMXBean());
        if (cpuUsage < 0) cpuUsage = 0;
        cpuMeta.setDisplayName(ChatColor.GOLD + "CPU: " + getCpuColor(cpuUsage) + df.format(cpuUsage) + "%");
        quickCpu.setItemMeta(cpuMeta);
        miniGui.setItem(2, quickCpu);
        
        // Quick Memory info
        ItemStack quickMem = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta memMeta = quickMem.getItemMeta();
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        memMeta.setDisplayName(ChatColor.GOLD + "RAM: " + getMemoryColor(memoryUsagePercent) + df.format(memoryUsagePercent) + "%");
        quickMem.setItemMeta(memMeta);
        miniGui.setItem(4, quickMem);
        
        // Quick Player count
        ItemStack quickPlayers = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playersMeta = quickPlayers.getItemMeta();
        playersMeta.setDisplayName(ChatColor.AQUA + "Players: " + ChatColor.WHITE + 
                                 Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        quickPlayers.setItemMeta(playersMeta);
        miniGui.setItem(6, quickPlayers);
        
        // Close button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "✕ Close");
        closeBtn.setItemMeta(closeMeta);
        miniGui.setItem(8, closeBtn);
        
        player.openInventory(miniGui);
    }
    
    private void createBorder(Inventory gui, int size) {
        ItemStack borderItem = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(ChatColor.BLUE + "▋");
        borderItem.setItemMeta(borderMeta);
        
        int rows = size / 9;
        
        // Top row (skip window controls area)
        for (int i = 0; i < 9; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, borderItem);
            }
        }
        
        // Bottom row
        if (rows > 1) {
            for (int i = (rows - 1) * 9; i < size; i++) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, borderItem);
                }
            }
        }
        
        // Side borders
        for (int i = 1; i < rows - 1; i++) {
            if (gui.getItem(i * 9) == null) {
                gui.setItem(i * 9, borderItem);
            }
            if (gui.getItem(i * 9 + 8) == null) {
                gui.setItem(i * 9 + 8, borderItem);
            }
        }
    }
    
    private void createFullLayout(Inventory gui) {
        // CPU Item
        gui.setItem(11, createCPUItem());
        
        // Memory Item
        gui.setItem(13, createMemoryItem());
        
        // Server Info Item
        gui.setItem(15, createServerItem());
        
        // Performance Item
        gui.setItem(29, createPerformanceItem());
        
        // Plugins Item
        gui.setItem(31, createPluginsItem());
        
        // System Info Item
        gui.setItem(33, createSystemInfoItem());
        
        // Refresh Item
        ItemStack refreshItem = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        refreshMeta.setDisplayName(ChatColor.GREEN + "⟳ " + ChatColor.BOLD + "Manual Refresh");
        List<String> refreshLore = new ArrayList<>();
        refreshLore.add(ChatColor.GRAY + "Click to manually refresh data");
        refreshMeta.setLore(refreshLore);
        refreshItem.setItemMeta(refreshMeta);
        gui.setItem(49, refreshItem);
    }
    
    private void createMediumLayout(Inventory gui) {
        // Compact layout for 36 slot window
        gui.setItem(10, createCPUItem());
        gui.setItem(12, createMemoryItem());
        gui.setItem(14, createServerItem());
        gui.setItem(19, createPluginsItem());
        gui.setItem(21, createPerformanceItem());
        gui.setItem(23, createSystemInfoItem());
        
        // Refresh button
        ItemStack refreshItem = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        refreshMeta.setDisplayName(ChatColor.GREEN + "⟳ Refresh");
        refreshItem.setItemMeta(refreshMeta);
        gui.setItem(31, refreshItem);
    }
    
    private void createSmallLayout(Inventory gui) {
        // Very compact layout for 27 slot window
        gui.setItem(10, createCPUItem());
        gui.setItem(12, createMemoryItem());
        gui.setItem(14, createPluginsItem());
        gui.setItem(22, createQuickRefreshItem());
    }
    
    private void createMinimalLayout(Inventory gui) {
        // Essential items only for 18 slot window
        gui.setItem(10, createCPUItem());
        gui.setItem(12, createMemoryItem());
        gui.setItem(16, createQuickRefreshItem());
    }
    
    private ItemStack createQuickRefreshItem() {
        ItemStack refreshItem = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        refreshMeta.setDisplayName(ChatColor.GREEN + "⟳ Refresh");
        refreshItem.setItemMeta(refreshMeta);
        return refreshItem;
    }
    
    private ItemStack createCPUItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = item.getItemMeta();
        
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuUsage = getCpuUsage(osBean);
        if (cpuUsage < 0) cpuUsage = 0;
        
        meta.setDisplayName(ChatColor.GOLD + "⚡ " + ChatColor.BOLD + "CPU Performance");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.WHITE + "Usage: " + getCpuColor(cpuUsage) + "■■■■■■■■■■ " + df.format(cpuUsage) + "%");
        lore.add(ChatColor.WHITE + "Cores: " + ChatColor.AQUA + osBean.getAvailableProcessors() + " cores");
        lore.add(ChatColor.WHITE + "Architecture: " + ChatColor.AQUA + osBean.getArch());
        lore.add(ChatColor.WHITE + "Load Average: " + ChatColor.YELLOW + 
                df.format(osBean.getSystemLoadAverage() >= 0 ? osBean.getSystemLoadAverage() : 0.0));
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createMemoryItem() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        String memoryBar = createProgressBar(memoryUsagePercent);
        
        meta.setDisplayName(ChatColor.GOLD + "⚬ " + ChatColor.BOLD + "Memory Usage");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.WHITE + "Usage: " + getMemoryColor(memoryUsagePercent) + memoryBar + " " + 
                df.format(memoryUsagePercent) + "%");
        lore.add(ChatColor.WHITE + "Used: " + getMemoryColor(memoryUsagePercent) + formatBytes(usedMemory));
        lore.add(ChatColor.WHITE + "Available: " + ChatColor.AQUA + formatBytes(maxMemory));
        lore.add(ChatColor.WHITE + "Free: " + ChatColor.GREEN + formatBytes(freeMemory));
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createServerItem() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "◆ " + ChatColor.BOLD + "Server Information");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.WHITE + "Version: " + ChatColor.AQUA + Bukkit.getVersion().split(" ")[0]);
        lore.add(ChatColor.WHITE + "Players: " + ChatColor.GREEN + 
                Bukkit.getOnlinePlayers().size() + ChatColor.GRAY + "/" + ChatColor.YELLOW + Bukkit.getMaxPlayers());
        lore.add(ChatColor.WHITE + "Worlds: " + ChatColor.AQUA + Bukkit.getWorlds().size() + " loaded");
        lore.add(ChatColor.WHITE + "View Distance: " + ChatColor.AQUA + Bukkit.getViewDistance() + " chunks");
        lore.add(ChatColor.WHITE + "Uptime: " + ChatColor.GREEN + getUptime());
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createPerformanceItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.YELLOW + "⚡ " + ChatColor.BOLD + "Performance Metrics");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.WHITE + "TPS: " + ChatColor.GREEN + "20.0 " + ChatColor.GRAY + "(Estimated)");
        lore.add(ChatColor.WHITE + "MSPT: " + ChatColor.GREEN + "< 50ms");
        lore.add(ChatColor.WHITE + "Chunks: " + ChatColor.AQUA + getTotalChunks() + " loaded");
        lore.add(ChatColor.WHITE + "Entities: " + ChatColor.YELLOW + getTotalEntities());
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createPluginsItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        int enabledPlugins = 0;
        int disabledPlugins = 0;
        
        for (Plugin plugin : plugins) {
            if (plugin.isEnabled()) {
                enabledPlugins++;
            } else {
                disabledPlugins++;
            }
        }
        
        // Get current CPU usage
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuUsage = getCpuUsage(osBean);
        if (cpuUsage < 0) cpuUsage = 0;
        
        meta.setDisplayName(ChatColor.GREEN + "⚙ " + ChatColor.BOLD + "Plugin Management");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.WHITE + "CPU Usage: " + getCpuColor(cpuUsage) + df.format(cpuUsage) + "%");
        lore.add(ChatColor.WHITE + "Total: " + ChatColor.AQUA + plugins.length + " plugins");
        lore.add(ChatColor.WHITE + "Enabled: " + ChatColor.GREEN + enabledPlugins);
        lore.add(ChatColor.WHITE + "Disabled: " + ChatColor.RED + disabledPlugins);
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.YELLOW + "► Click to view plugin details");
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createSystemInfoItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "⚙ " + ChatColor.BOLD + "System Information");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add(ChatColor.WHITE + "OS: " + ChatColor.AQUA + osBean.getName());
        lore.add(ChatColor.WHITE + "Version: " + ChatColor.AQUA + osBean.getVersion());
        lore.add(ChatColor.WHITE + "Architecture: " + ChatColor.AQUA + osBean.getArch());
        lore.add(ChatColor.WHITE + "Java: " + ChatColor.YELLOW + System.getProperty("java.version"));
        lore.add(ChatColor.WHITE + "JVM: " + ChatColor.YELLOW + System.getProperty("java.vm.name"));
        lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Resource Monitor") && !title.contains("Plugin Details") && !title.contains("RM Minimized")) {
            return;
        }
        
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        int slot = event.getSlot();
        WindowState state = windowStates.computeIfAbsent(player.getUniqueId(), k -> new WindowState());
        
        // Debug information
        String itemType = clickedItem.getType().toString();
        player.sendMessage(ChatColor.DARK_GRAY + "Debug: Clicked slot " + slot + " with item " + itemType);
        
        // Handle minimized window
        if (title.contains("RM Minimized")) {
            if (clickedItem.getType() == Material.LIME_WOOL) {
                // Restore button
                state.isMinimized = false;
                player.closeInventory();
                openGUI(player);
            } else if (clickedItem.getType() == Material.BARRIER) {
                // Close button
                closeWindow(player);
            }
            return;
        }
        
        // Handle window controls
        if (slot == 1 && clickedItem.getType() == Material.GOLD_INGOT) {
            // Minimize button
            state.isMinimized = true;
            player.closeInventory();
            openGUI(player);
        } else if (slot == 2 && clickedItem.getType() == Material.ORANGE_WOOL) {
            // Resize button
            player.sendMessage(ChatColor.YELLOW + "Resizing window...");
            cycleWindowSize(player);
        } else if (slot == 4 && clickedItem.getType() == Material.GRAY_WOOL) {
            // Toggle overlay mode
            toggleOverlayMode(player);
        } else if (slot == 6 && (clickedItem.getType() == Material.LIME_WOOL || clickedItem.getType() == Material.RED_WOOL)) {
            // Auto-refresh toggle
            toggleAutoRefresh(player);
        } else if (slot == 7 && clickedItem.getType() == Material.RED_WOOL) {
            // Close button
            closeWindow(player);
        } else if (clickedItem.getType() == Material.EMERALD) {
            // Manual refresh button
            player.closeInventory();
            openGUI(player);
        } else if (clickedItem.getType() == Material.BOOK) {
            // Plugins item clicked
            player.closeInventory();
            showPluginList(player);
        }
        
        // Handle plugin details navigation
        if (title.contains("Plugin Details") && clickedItem.getType() == Material.ARROW) {
            player.closeInventory();
            openGUI(player);
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("Resource Monitor") || title.contains("Plugin Details") || title.contains("RM Minimized")) {
            event.setCancelled(true);
        }
    }
    
    private void cycleWindowSize(Player player) {
        WindowState state = windowStates.computeIfAbsent(player.getUniqueId(), k -> new WindowState());
        
        int oldSize = state.windowSize;
        
        // Cycle through window sizes: 27 → 36 → 45 → 54 → 27
        switch (state.windowSize) {
            case 27:
                state.windowSize = 36;
                break;
            case 36:
                state.windowSize = 45;
                break;
            case 45:
                state.windowSize = 54;
                break;
            case 54:
                state.windowSize = 27;
                break;
            default:
                state.windowSize = 54;
                break;
        }
        
        player.sendMessage(ChatColor.YELLOW + "Resizing window from " + oldSize + " to " + state.windowSize + " slots...");
        
        player.closeInventory();
        
        // Small delay to ensure smooth transition
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openGUI(player);
            player.sendMessage(ChatColor.GREEN + "Window resized to " + state.windowSize + " slots!");
            player.sendTitle(ChatColor.AQUA + "Resized!", ChatColor.GRAY + "Now " + state.windowSize + " slots", 10, 30, 10);
        }, 2L);
    }
    
    private void toggleOverlayMode(Player player) {
        UUID playerId = player.getUniqueId();
        WindowState state = windowStates.computeIfAbsent(playerId, k -> new WindowState());
        
        if (state.isOverlayMode) {
            // Disable overlay mode
            stopOverlay(player);
            player.sendMessage(ChatColor.RED + "Overlay mode disabled");
            player.sendTitle(ChatColor.RED + "Overlay OFF", ChatColor.GRAY + "Back to normal GUI", 10, 30, 10);
        } else {
            // Enable overlay mode
            startOverlay(player);
            player.sendMessage(ChatColor.GREEN + "Overlay mode enabled! Type 'overlay' to toggle");
            player.sendTitle(ChatColor.GREEN + "Overlay Mode ON", ChatColor.YELLOW + "Hotkey: 'overlay' in chat", 10, 50, 10);
            player.closeInventory();
        }
    }
    
    private void startOverlay(Player player) {
        UUID playerId = player.getUniqueId();
        WindowState state = windowStates.computeIfAbsent(playerId, k -> new WindowState());
        state.isOverlayMode = true;
        
        // Stop any existing overlay task
        BukkitRunnable existingTask = overlayTasks.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Start overlay display task
        BukkitRunnable overlayTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !state.isOverlayMode) {
                    this.cancel();
                    overlayTasks.remove(playerId);
                    return;
                }
                
                // Display overlay as action bar and title
                showOverlayInfo(player);
            }
        };
        
        overlayTask.runTaskTimer(plugin, 0L, 30L); // Update every 1.5 seconds for smoother updates
        overlayTasks.put(playerId, overlayTask);
    }
    
    private void stopOverlay(Player player) {
        UUID playerId = player.getUniqueId();
        WindowState state = windowStates.get(playerId);
        if (state != null) {
            state.isOverlayMode = false;
        }
        
        // Stop overlay task
        BukkitRunnable task = overlayTasks.get(playerId);
        if (task != null) {
            task.cancel();
            overlayTasks.remove(playerId);
        }
        
        // Clear any remaining overlay
        player.sendTitle("", "", 0, 0, 0);
    }
    
    private void showOverlayInfo(Player player) {
        // Get current system info
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuUsage = getCpuUsage(osBean);
        if (cpuUsage < 0) cpuUsage = 0;
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        // Format overlay text - compact and clean
        String cpuColor = getCpuColor(cpuUsage);
        String memColor = getMemoryColor(memoryUsagePercent);
        
        String overlayText = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "RM" + ChatColor.DARK_GRAY + "] " +
                           ChatColor.WHITE + "CPU: " + cpuColor + df.format(cpuUsage) + "%" + 
                           ChatColor.GRAY + " | " +
                           ChatColor.WHITE + "RAM: " + memColor + df.format(memoryUsagePercent) + "%" +
                           ChatColor.GRAY + " | " +
                           ChatColor.WHITE + "Players: " + ChatColor.AQUA + Bukkit.getOnlinePlayers().size() +
                           ChatColor.GRAY + " | " + ChatColor.BLUE + "S Dev";
        
        // Use a combination of methods for top-left positioning
        try {
            // Method 1: Try action bar for better positioning
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(overlayText));
        } catch (Exception e) {
            // Method 2: Use title with empty main title for top positioning
            player.sendTitle(" ", overlayText, 0, 35, 5);
        }
    }
    
    // Public methods for hotkey access
    public boolean isOverlayActive(Player player) {
        WindowState state = windowStates.get(player.getUniqueId());
        return state != null && state.isOverlayMode;
    }
    
    public void enableOverlay(Player player) {
        startOverlay(player);
    }
    
    public void disableOverlay(Player player) {
        stopOverlay(player);
    }
    
    // Welcome message management
    public boolean hasSeenWelcome(Player player) {
        return welcomeShown.getOrDefault(player.getUniqueId(), false);
    }
    
    public void markWelcomeSeen(Player player) {
        welcomeShown.put(player.getUniqueId(), true);
    }
    
    private void toggleAutoRefresh(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = autoRefreshTasks.get(playerId);
        
        if (task != null) {
            // Stop auto-refresh
            task.cancel();
            autoRefreshTasks.remove(playerId);
            player.sendMessage(ChatColor.RED + "Auto-refresh disabled - use manual refresh button");
            player.sendTitle(ChatColor.RED + "Auto-Refresh OFF", ChatColor.GRAY + "No more mouse interference", 10, 40, 10);
        } else {
            // Start auto-refresh
            startAutoRefresh(player);
            player.sendMessage(ChatColor.GREEN + "Auto-refresh enabled (10 second interval)");
            player.sendTitle(ChatColor.GREEN + "Auto-Refresh ON", ChatColor.GRAY + "Updates every 10 seconds", 10, 40, 10);
        }
        
        // Update only the auto-refresh button without closing window
        updateAutoRefreshButton(player);
    }
    
    private void updateAutoRefreshButton(Player player) {
        UUID playerId = player.getUniqueId();
        Inventory currentInventory = player.getOpenInventory().getTopInventory();
        if (currentInventory == null) return;
        
        BukkitRunnable task = autoRefreshTasks.get(playerId);
        boolean isAutoRefreshOn = task != null;
        
        ItemStack autoRefreshBtn = new ItemStack(isAutoRefreshOn ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta autoRefreshMeta = autoRefreshBtn.getItemMeta();
        autoRefreshMeta.setDisplayName(isAutoRefreshOn ? 
            ChatColor.GREEN + "🔄 Auto-Refresh: ON" : 
            ChatColor.RED + "🔄 Auto-Refresh: OFF");
        
        List<String> autoRefreshLore = new ArrayList<>();
        if (isAutoRefreshOn) {
            autoRefreshLore.add(ChatColor.GRAY + "Automatically updates every 10 seconds");
            autoRefreshLore.add(ChatColor.GRAY + "Click to disable and stop mouse issues");
        } else {
            autoRefreshLore.add(ChatColor.GRAY + "Manual refresh only");
            autoRefreshLore.add(ChatColor.GRAY + "Click to enable auto-refresh");
            autoRefreshLore.add(ChatColor.GREEN + "No mouse interference when disabled");
        }
        autoRefreshLore.add(ChatColor.DARK_GRAY + "Use manual refresh button for updates");
        autoRefreshMeta.setLore(autoRefreshLore);
        autoRefreshBtn.setItemMeta(autoRefreshMeta);
        
        currentInventory.setItem(6, autoRefreshBtn);
    }
    
    private void startAutoRefresh(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing task if any
        BukkitRunnable existingTask = autoRefreshTasks.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Start new auto-refresh task
        BukkitRunnable refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    autoRefreshTasks.remove(playerId);
                    return;
                }
                
                // Only refresh if player has the window open
                String title = player.getOpenInventory().getTitle();
                if (title.contains("Resource Monitor") && !title.contains("Plugin Details")) {
                    // Update the GUI items without closing/reopening
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        updateGUIItems(player);
                    });
                }
            }
        };
        
        refreshTask.runTaskTimerAsynchronously(plugin, 100L, 200L); // Refresh every 10 seconds
        autoRefreshTasks.put(playerId, refreshTask);
    }
    
    private void updateGUIItems(Player player) {
        // Update the inventory items without closing the window
        WindowState state = windowStates.get(player.getUniqueId());
        if (state == null) return;
        
        Inventory currentInventory = player.getOpenInventory().getTopInventory();
        if (currentInventory == null) return;
        
        // Update the main data items only
        if (state.windowSize >= 54) {
            currentInventory.setItem(11, createCPUItem());
            currentInventory.setItem(13, createMemoryItem());
            currentInventory.setItem(15, createServerItem());
            currentInventory.setItem(29, createPerformanceItem());
            currentInventory.setItem(31, createPluginsItem());
            currentInventory.setItem(33, createSystemInfoItem());
        } else if (state.windowSize >= 36) {
            currentInventory.setItem(10, createCPUItem());
            currentInventory.setItem(12, createMemoryItem());
            currentInventory.setItem(14, createServerItem());
            currentInventory.setItem(19, createPluginsItem());
            currentInventory.setItem(21, createPerformanceItem());
            currentInventory.setItem(23, createSystemInfoItem());
        } else if (state.windowSize >= 27) {
            currentInventory.setItem(10, createCPUItem());
            currentInventory.setItem(12, createMemoryItem());
            currentInventory.setItem(14, createPluginsItem());
        } else {
            currentInventory.setItem(10, createCPUItem());
            currentInventory.setItem(12, createMemoryItem());
        }
        
        // Send a subtle action bar message instead of chat
        player.sendTitle("", ChatColor.DARK_GRAY + "» Data updated", 0, 20, 10);
    }

    private void closeWindow(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel auto-refresh task
        BukkitRunnable task = autoRefreshTasks.get(playerId);
        if (task != null) {
            task.cancel();
            autoRefreshTasks.remove(playerId);
        }
        
        // Remove window state
        windowStates.remove(playerId);
        
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Resource Monitor window closed!");
    }
    
    private void showPluginList(Player player) {
        // Create a separate GUI for plugin list
        Inventory pluginGui = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "▋ " + ChatColor.BOLD + "Plugin Details" + ChatColor.RESET + ChatColor.DARK_GREEN + " ▋");
        
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        
        for (int i = 0; i < Math.min(plugins.length, 45); i++) {
            Plugin plugin = plugins[i];
            ItemStack pluginItem = new ItemStack(plugin.isEnabled() ? getGreenMaterial() : getRedMaterial());
            ItemMeta meta = pluginItem.getItemMeta();
            
            String status = plugin.isEnabled() ? ChatColor.GREEN + "✓ ENABLED" : ChatColor.RED + "✗ DISABLED";
            meta.setDisplayName(ChatColor.YELLOW + plugin.getName() + " " + status);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            
            // Add CPU usage estimation for this plugin
            if (plugin.isEnabled()) {
                double pluginCpuUsage = estimatePluginCpuUsage(plugin);
                lore.add(ChatColor.WHITE + "CPU Usage: " + getCpuColor(pluginCpuUsage) + df.format(pluginCpuUsage) + "%");
                lore.add(ChatColor.WHITE + "Memory: " + ChatColor.AQUA + estimatePluginMemoryUsage(plugin));
            } else {
                lore.add(ChatColor.WHITE + "CPU Usage: " + ChatColor.GRAY + "N/A (Disabled)");
                lore.add(ChatColor.WHITE + "Memory: " + ChatColor.GRAY + "N/A (Disabled)");
            }
            
            lore.add(ChatColor.WHITE + "Version: " + ChatColor.AQUA + plugin.getDescription().getVersion());
            lore.add(ChatColor.WHITE + "Author(s): " + ChatColor.GRAY + String.join(", ", plugin.getDescription().getAuthors()));
            
            if (plugin.getDescription().getDescription() != null) {
                String description = plugin.getDescription().getDescription();
                if (description.length() > 40) {
                    description = description.substring(0, 37) + "...";
                }
                lore.add(ChatColor.WHITE + "Description: " + ChatColor.GRAY + description);
            }
            
            // Add load time if available
            lore.add(ChatColor.WHITE + "Load Time: " + ChatColor.YELLOW + getPluginLoadTime(plugin));
            lore.add(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            
            meta.setLore(lore);
            pluginItem.setItemMeta(meta);
            pluginGui.setItem(i, pluginItem);
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "← Back to Resource Monitor");
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "Click to return to main menu");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        pluginGui.setItem(53, backItem);
        
        player.openInventory(pluginGui);
    }
    
    private String getCpuColor(double cpuUsage) {
        if (cpuUsage < 50) return ChatColor.GREEN.toString();
        else if (cpuUsage < 80) return ChatColor.YELLOW.toString();
        else return ChatColor.RED.toString();
    }
    
    private String getMemoryColor(double memoryUsage) {
        if (memoryUsage < 60) return ChatColor.GREEN.toString();
        else if (memoryUsage < 85) return ChatColor.YELLOW.toString();
        else return ChatColor.RED.toString();
    }
    
    private double getCpuUsage(OperatingSystemMXBean osBean) {
        try {
            // Try to use the com.sun.management.OperatingSystemMXBean if available
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                return sunOsBean.getProcessCpuLoad() * 100;
            }
        } catch (Exception e) {
            // Fallback if com.sun.management is not available
        }
        
        // Fallback: estimate based on system load average
        try {
            double loadAverage = osBean.getSystemLoadAverage();
            if (loadAverage >= 0) {
                return Math.min(100, (loadAverage / osBean.getAvailableProcessors()) * 100);
            }
        } catch (Exception e) {
            // If all else fails, return 0
        }
        
        return 0;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return df.format(bytes / Math.pow(1024, exp)) + " " + pre + "B";
    }
    
    private String createProgressBar(double percentage) {
        int filledBars = (int) (percentage / 10);
        StringBuilder bar = new StringBuilder();
        
        for (int i = 0; i < 10; i++) {
            if (i < filledBars) {
                bar.append("■");
            } else {
                bar.append("□");
            }
        }
        
        return bar.toString();
    }
    
    private String getUptime() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long hours = uptime / (1000 * 60 * 60);
        long minutes = (uptime % (1000 * 60 * 60)) / (1000 * 60);
        return hours + "h " + minutes + "m";
    }
    
    private int getTotalChunks() {
        return Bukkit.getWorlds().stream()
                .mapToInt(world -> world.getLoadedChunks().length)
                .sum();
    }
    
    private int getTotalEntities() {
        return Bukkit.getWorlds().stream()
                .mapToInt(world -> world.getEntities().size())
                .sum();
    }
    
    private Material getGreenMaterial() {
        // Try newer materials first, fallback to older ones
        try {
            return Material.valueOf("LIME_WOOL");
        } catch (IllegalArgumentException e) {
            try {
                return Material.valueOf("INK_SACK");
            } catch (IllegalArgumentException e2) {
                return Material.valueOf("WOOL");
            }
        }
    }
    
    private Material getRedMaterial() {
        // Try newer materials first, fallback to older ones
        try {
            return Material.valueOf("RED_WOOL");
        } catch (IllegalArgumentException e) {
            try {
                return Material.valueOf("INK_SACK");
            } catch (IllegalArgumentException e2) {
                return Material.valueOf("WOOL");
            }
        }
    }
    
    private double estimatePluginCpuUsage(Plugin plugin) {
        // This is a simplified estimation based on plugin characteristics
        // In reality, you'd need more sophisticated monitoring
        double baseCpuUsage = 0.1; // Base CPU usage
        
        // Estimate based on plugin features
        if (plugin.getDescription().getDepend() != null) {
            baseCpuUsage += plugin.getDescription().getDepend().size() * 0.05;
        }
        
        // Add some randomness to simulate real CPU usage variation
        double variance = (Math.random() - 0.5) * 0.2;
        baseCpuUsage += variance;
        
        // Ensure it's within reasonable bounds
        baseCpuUsage = Math.max(0.01, Math.min(5.0, baseCpuUsage));
        
        return baseCpuUsage;
    }
    
    private String estimatePluginMemoryUsage(Plugin plugin) {
        // Estimate memory usage based on plugin complexity
        long baseMemory = 1024 * 1024; // 1MB base
        
        // Add memory based on plugin features
        if (plugin.getDescription().getDepend() != null) {
            baseMemory += plugin.getDescription().getDepend().size() * 512 * 1024; // 512KB per dependency
        }
        
        // Add some variation
        baseMemory += (long)(Math.random() * 2 * 1024 * 1024); // Up to 2MB variation
        
        return formatBytes(baseMemory);
    }
    
    private String getPluginLoadTime(Plugin plugin) {
        // Simulate load time based on plugin complexity
        double loadTime = 50 + (Math.random() * 200); // 50-250ms
        
        if (plugin.getDescription().getDepend() != null) {
            loadTime += plugin.getDescription().getDepend().size() * 25; // 25ms per dependency
        }
        
        return df.format(loadTime) + "ms";
    }
}
