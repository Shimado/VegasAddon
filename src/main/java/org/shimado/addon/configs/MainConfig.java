package org.shimado.addon.configs;

import com.github.Shimado.api.CasinoGameModeRegister;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.shimado.addon.VegasAddon;
import org.shimado.addon.modes.Drums;
import org.shimado.basicutils.utils.ColorUtil;
import org.shimado.basicutils.utils.CreateItemUtil;
import org.shimado.basicutils.utils.NumberUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class MainConfig {

    private final VegasAddon plugin;
    private final CasinoGameModeRegister casinoGameModeRegister;
    private YamlConfiguration config;
    private YamlConfiguration messages;

    public MainConfig(VegasAddon plugin) {
        this.plugin = plugin;
        this.casinoGameModeRegister = plugin.getCasinoGameModeRegister();
        reload();
    }


    /**
     * Creates config.yml file from the /resources/config.yml
     * **/

    private void initConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        if (!configFile.exists()) {
            try {
                Files.copy(plugin.getResource("config.yml"), configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }


    /**
     * Creates en.yml file from the /resources/en.yml
     * **/

    private void initMessageConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "en.yml");

        if (!messagesFile.exists()) {
            try {
                Files.copy(plugin.getResource("en.yml"), messagesFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }


    /**
     * Loads all data from the config, checks them for correctness and then registers them in the Vegas plugin as a mode.
     * The standard config fields are checked in the Vegas plugin itself, there is no need to check them here!
     */

    private void loadDrums(){
        try {
            Map<String, Object> mapConfig = loadSettings(config, "Drums-settings");
            Map<String, Object> mapMessages = loadSettings(messages, "Drums-settings");
            if(mapConfig.isEmpty() || mapMessages.isEmpty()) return;

            Drums drums = (Drums) plugin.getCasinoGameModeRegister().getGameModeFromMaps(mapConfig, mapMessages, Drums.class);
            if(drums == null) return;

            int figuresMaxAmount = (int) mapConfig.getOrDefault("figures-amount-max", 1);
            if(NumberUtil.inRangeInt(figuresMaxAmount, 1, 3)){
                drums.setFiguresMaxAmount(figuresMaxAmount);
            }else{
                Bukkit.getConsoleSender().sendMessage(ColorUtil.getColor("&c[VEGAS] The maximum number of combinations for Drums mode is incorrect! Must be between 1 and 3!"));
                return;
            }


            if(mapConfig.containsKey("combinations")){
                List<Drums.DrumCombination> combinations = new ArrayList<>();

                for(Map.Entry<String, Map<String, Object>> a : ((Map<String, Map<String, Object>>) mapConfig.get("combinations")).entrySet()){
                    double chance = (double) a.getValue().getOrDefault("chance", 5.0);
                    double multiplier = (double) a.getValue().getOrDefault("multiplier", 1.0);
                    List<List<String>> types = (List<List<String>>) a.getValue().get("types");

                    for(List<String> l : types){
                        if(l.size() != 5 || l.stream().anyMatch(it -> it.length() != 5 || !it.matches("[01]+"))){
                            Bukkit.getConsoleSender().sendMessage(ColorUtil.getColor("&c[VEGAS] One of the combination variations (type) for Drums mode is specified incorrectly! Contains an incorrect number of characters, or an invalid character!"));
                            return;
                        }
                    }

                    if(mapConfig.containsKey("victory") && ((Map<String, Object>) mapConfig.get("victory")).containsKey("combinations") && ((List<String>) ((Map<String, Object>) mapConfig.get("victory")).get("combinations")).contains(a.getKey())){
                        combinations.add(new Drums.DrumCombination(chance, multiplier, false, types));
                    }
                    else if(mapConfig.containsKey("bonus") && ((Map<String, Object>) mapConfig.get("bonus")).containsKey("combinations") && ((List<String>) ((Map<String, Object>) mapConfig.get("bonus")).get("combinations")).contains(a.getKey())){
                        combinations.add(new Drums.DrumCombination(chance, multiplier, true, types));
                    }
                }

                if(combinations.isEmpty()) {
                    Bukkit.getConsoleSender().sendMessage(ColorUtil.getColor("&c[VEGAS] The combination number for the Drums game mode cannot be 0!"));
                    return;
                }

                drums.setCombinations(combinations);
            }else{
                Bukkit.getConsoleSender().sendMessage(ColorUtil.getColor("&c[VEGAS] The integrity of the Drums game mode config has been compromised! [combinations]"));
                return;
            }


            if(mapConfig.containsKey("rolling-items")){
                List<Drums.RollingItem> rollingItems = new ArrayList<>();

                for(Map<String, Object> a : (List<Map<String, Object>>) mapConfig.get("rolling-items")){
                    ItemStack item = CreateItemUtil.create(a.get("material"), " ", new ArrayList<>(), false, (int) a.get("custom-model-data"), true);
                    if(item == null) continue;
                    rollingItems.add(new Drums.RollingItem(
                            item,
                            (double) a.getOrDefault("chance", 10.0),
                            (double) a.getOrDefault("multiplier", 1.0)
                    ));
                }

                if(rollingItems.size() <= 6){
                    Bukkit.getConsoleSender().sendMessage(ColorUtil.getColor("&c[VEGAS] The number of materials to scroll must be more than 6!"));
                    return;
                }

                drums.setRollingItems(rollingItems);
            }else{
                Bukkit.getConsoleSender().sendMessage(ColorUtil.getColor("&c[VEGAS] The integrity of the Drums game mode config has been compromised! [rolling-items]"));
                return;
            }

            if(mapConfig.containsKey("background")){
                Map<String, Object> backgroundMap = (Map<String, Object>) mapConfig.get("background");
                if(backgroundMap.containsKey("victory-placeholder")){
                    Map<String, Object> placeholderMap = (Map<String, Object>) backgroundMap.get("victory-placeholder");
                    ItemStack placeholderItem = CreateItemUtil.create(placeholderMap.get("material"), " ", new ArrayList<>(), false, (int) placeholderMap.get("custom-model-data"), true);
                    if(placeholderItem != null){
                        drums.setVictoryPlaceholderItem(placeholderItem);
                    }else{
                        Bukkit.getConsoleSender().sendMessage(ColorUtil.getColor("&c[VEGAS] There is no victory placeholder item in Drums mode!"));
                        return;
                    }
                }else{
                    Bukkit.getConsoleSender().sendMessage(ColorUtil.getColor("&c[VEGAS] The integrity of the Drums game mode config has been compromised! [victory-placeholder]"));
                    return;
                }
            }else{
                Bukkit.getConsoleSender().sendMessage(ColorUtil.getColor("&c[VEGAS] The integrity of the Drums game mode config has been compromised! [background]"));
                return;
            }

            casinoGameModeRegister.register(drums);

        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }


    /**
     * Gets config data by key as a non-flat map. If the field is missing, it returns an empty map.
     *
     * @param config - config or messages config instance
     * @param key - config or messages config key value. In this addon its "Drums-settings"
     * @return map with all keys - values or empty map
     */

    @Nonnull
    private static Map<String, Object> loadSettings(YamlConfiguration config, String key) {
        ConfigurationSection drumsSection = config.getConfigurationSection(key);
        if (drumsSection == null) {
            return Collections.emptyMap();
        }
        return toMap(drumsSection);
    }


    /**
     * Converts the section into a ready-made non-flat map of all config fields,
     * which can then be used to obtain data and specify it for the mode.
     */

    @Nonnull
    private static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                map.put(key, toMap((ConfigurationSection) value));
            } else if (value instanceof List<?>) {
                List<?> list = (List<?>) value;
                List<Object> newList = new ArrayList<>();
                for (Object element : list) {
                    if (element instanceof ConfigurationSection) {
                        newList.add(toMap((ConfigurationSection) element));
                    } else {
                        newList.add(element);
                    }
                }
                map.put(key, newList);
            } else {
                map.put(key, value);
            }
        }
        return map;
    }


    /**
     * Loads/reloads configs, then loads game mode.
     */

    public void reload() {
        initConfig();
        initMessageConfig();
        loadDrums();
    }

}
