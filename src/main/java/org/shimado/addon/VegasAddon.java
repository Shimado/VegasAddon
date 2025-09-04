package org.shimado.addon;

import com.github.Shimado.VegasAPI;
import com.github.Shimado.api.*;
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
        chipUtil = VegasAPI.getChipUtil();
        victoryUtil = VegasAPI.getVictoryUtil();
        invSession = VegasAPI.getInvSession();
        new MainConfig(this);
    }


    @Override
    public void onDisable() {

    }


    private CasinoGameModeRegister casinoGameModeRegister;
    private CasinoGameModeUtil casinoGameModeUtil;
    private ChipUtil chipUtil;
    private VictoryUtil victoryUtil;
    private InvSession invSession;


    public CasinoGameModeRegister getCasinoGameModeRegister(){
        return casinoGameModeRegister;
    }
    public CasinoGameModeUtil getCasinoGameModeUtil(){
        return casinoGameModeUtil;
    }
    public ChipUtil getChipUtil(){
        return chipUtil;
    }
    public VictoryUtil getVictoryUtil(){
        return victoryUtil;
    }
    public InvSession getInvSession(){
        return invSession;
    }


}