package org.shimado.addon.configs;

import com.github.Shimado.api.CasinoGameModeRegister;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.shimado.addon.VegasAddon;
import org.shimado.addon.modes.Drums;
import org.shimado.basicutils.utils.CreateItemUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class MainConfig {

    private VegasAddon plugin;
    private CasinoGameModeRegister casinoGameModeRegister;
    private YamlConfiguration config;
    private YamlConfiguration messages;

    public MainConfig(VegasAddon plugin) {
        this.plugin = plugin;
        this.casinoGameModeRegister = plugin.getCasinoGameModeRegister();
        reload();
    }


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


    public void loadDrums(){
        try {
            Map<String, Object> mapConfig = loadDrumsSettings(config);
            Map<String, Object> mapMessages = loadDrumsSettings(messages);
            if(mapConfig.isEmpty() || mapMessages.isEmpty()) return;

            Drums drums = (Drums) plugin.getCasinoGameModeRegister().getGameModeFromMaps(mapConfig, mapMessages, Drums.class);

            if(mapConfig.containsKey("figures-amount-max")){
                drums.setFiguresMaxAmount((int) mapConfig.get("figures-amount-max"));
            }

            if(mapConfig.containsKey("combinations")){
                List<Drums.DrumCombination> combinations = new ArrayList<>();
                for(Map.Entry<String, Map<String, Object>> a : ((Map<String, Map<String, Object>>) mapConfig.get("combinations")).entrySet()){
                    String combinationName = a.getKey();
                    if(mapConfig.containsKey("victory") && ((Map<String, Object>) mapConfig.get("victory")).containsKey("combinations") && ((List<String>) ((Map<String, Object>) mapConfig.get("victory")).get("combinations")).contains(combinationName)){
                        combinations.add(new Drums.DrumCombination(
                                (double) a.getValue().getOrDefault("chance", 0.0),
                                (double) a.getValue().getOrDefault("multiplier", 0.0),
                                false,
                                (List<List<String>>) a.getValue().get("types")
                        ));
                    }
                    else if(mapConfig.containsKey("bonus") && ((Map<String, Object>) mapConfig.get("bonus")).containsKey("combinations") && ((List<String>) ((Map<String, Object>) mapConfig.get("bonus")).get("combinations")).contains(combinationName)){
                        combinations.add(new Drums.DrumCombination(
                                (double) a.getValue().getOrDefault("chance", 0.0),
                                (double) a.getValue().getOrDefault("multiplier", 0.0),
                                true,
                                (List<List<String>>) a.getValue().get("types")
                        ));
                    }
                }
                drums.setCombinations(combinations);
            }

            if(mapConfig.containsKey("rolling-items")){
                List<Drums.RollingItem> rollingItems = new ArrayList<>();
                for(Map<String, Object> a : (List<Map<String, Object>>) mapConfig.get("rolling-items")){
                    rollingItems.add(new Drums.RollingItem(
                            CreateItemUtil.create(a.get("material"), " ", new ArrayList<>(), false, (int) a.get("custom-model-data"), true),
                            (double) a.getOrDefault("chance", 0.0),
                            (double) a.getOrDefault("multiplier", 0.0)
                    ));
                }
                drums.setRollingItems(rollingItems);
            }

            casinoGameModeRegister.register(drums);

        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }


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


    private static Map<String, Object> loadDrumsSettings(YamlConfiguration config) {
        ConfigurationSection drumsSection = config.getConfigurationSection("Drums-settings");
        if (drumsSection == null) {
            return Collections.emptyMap();
        }
        return toMap(drumsSection);
    }


    public void reload() {
        initConfig();
        initMessageConfig();
        loadDrums();
    }

}
