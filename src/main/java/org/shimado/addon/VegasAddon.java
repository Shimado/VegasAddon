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
        new BasicUtils(this);  // Optional. Loads the BasicUtil library. If you don't use it, just don't use this method
        casinoGameModeRegister = VegasAPI.getCasinoGameModeRegister();
        casinoGameModeUtil = VegasAPI.getCasinoGameModeUtil();
        chipUtil = VegasAPI.getChipUtil();
        victoryUtil = VegasAPI.getVictoryUtil();
        invSession = VegasAPI.getInvSession();
        new MainConfig(this);  // Creates a config and a message config. Loads the game mode itself from the config and registers it in the Vegas plugin memory
    }


    @Override
    public void onDisable() {

    }


    private CasinoGameModeRegister casinoGameModeRegister;   // Class for registering game mode in Vegas plugin
    private CasinoGameModeUtil casinoGameModeUtil;           // Optional. A class with some methods to make it easier to implement the methods
    private ChipUtil chipUtil;                               // Method for getting chips. Needed if you want to get chips, for example for display in game mode
    private VictoryUtil victoryUtil;                         // Method of handling win/loss/jackpot etc. Processes the event itself, http requests, messages, sounds, commands
    private InvSession invSession;                           // Needed to get the inventory in which the game is running, information about the table on which the player is playing, the name of the game, etc.


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