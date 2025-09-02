package org.shimado.addon;

import com.github.Shimado.VegasAPI;
import com.github.Shimado.api.CasinoGameModeRegister;
import com.github.Shimado.api.CasinoGameModeUtil;
import com.github.Shimado.api.VictoryUtil;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimado.addon.configs.MainConfig;
import org.shimado.basicutils.BasicUtils;

public class VegasAddon extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        new BasicUtils(this);
        casinoGameModeRegister = VegasAPI.getCasinoGameModeRegister();
        casinoGameModeUtil = VegasAPI.getCasinoGameModeUtil();
        victoryUtil = VegasAPI.getVictoryUtil();
        new MainConfig(this);
    }


    @Override
    public void onDisable() {

    }


    private CasinoGameModeRegister casinoGameModeRegister;
    private CasinoGameModeUtil casinoGameModeUtil;
    private VictoryUtil victoryUtil;


    public CasinoGameModeRegister getCasinoGameModeRegister(){
        return casinoGameModeRegister;
    }
    public CasinoGameModeUtil getCasinoGameModeUtil(){
        return casinoGameModeUtil;
    }
    public VictoryUtil getVictoryUtil(){
        return victoryUtil;
    }
}