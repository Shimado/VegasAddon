package org.shimado.addon.modes;

import com.github.Shimado.api.CasinoGameModeMethods;
import com.github.Shimado.api.CasinoGameModeUtil;
import com.github.Shimado.api.ChipUtil;
import com.github.Shimado.api.VictoryUtil;
import com.github.Shimado.instances.CasinoBet;
import com.github.Shimado.instances.CasinoGameMode;
import com.github.Shimado.interfaces.ISession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.shimado.addon.VegasAddon;
import org.shimado.basicutils.BasicUtils;
import org.shimado.basicutils.instances.Pair;
import org.shimado.basicutils.inventory.InventoryUtil;
import org.shimado.basicutils.nms.IInvHandler;
import org.shimado.basicutils.utils.NumberUtil;
import org.shimado.basicutils.utils.PermissionUtil;
import org.shimado.basicutils.utils.SoundUtil;

import java.util.*;
import java.util.stream.Collectors;

public class Drums extends CasinoGameMode implements CasinoGameModeMethods, Listener {

    private int figuresMaxAmount = 1;
    private List<DrumCombination> combinations = new ArrayList<>();
    private List<RollingItem> rollingItems = new ArrayList<>();
    private final int[][] field = {  //[1][0] = 11    [0][3] = 5
            {2,3,4,5,6},
            {11,12,13,14,15},
            {20,21,22,23,24},
            {29,30,31,32,33},
            {38,39,40,41,42}
    };

    private VegasAddon plugin;
    private IInvHandler invHandler = BasicUtils.getVersionControl().getInvHandler();
    private CasinoGameModeUtil casinoGameModeUtil;
    private ChipUtil chipUtil;
    private VictoryUtil victoryUtil;
    private Map<UUID, GameSession> sessions = new HashMap<>();

    public Drums(VegasAddon plugin){
        super(Drums.class.getSimpleName());
        this.plugin = plugin;
        this.casinoGameModeUtil = plugin.getCasinoGameModeUtil();
        this.chipUtil = plugin.getChipUtil();
        this.victoryUtil = plugin.getVictoryUtil();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    public void setFiguresMaxAmount(int figuresMaxAmount){
        this.figuresMaxAmount = figuresMaxAmount;
    }

    public void setCombinations(List<DrumCombination> combinations){
        this.combinations = combinations;
    }

    public void setRollingItems(List<RollingItem> rollingItems){
        this.rollingItems = rollingItems;
    }


    /**
     * ОСНОВНАЯ МЕХМНИКА
     * **/

    @Override
    public void openGame(Player player, Inventory inv){
        GameSession gameSession = new GameSession();
        sessions.put(player.getUniqueId(), gameSession);

        refreshInventory(inv, gameSession);
        ItemStack[][] board = gameSession.getBoard();
        for (int raw = 0; raw < 35; raw++) {
            for (int col = 0; col < 5; col++) {
                board[raw][col] = rollingItems.get(NumberUtil.randomInt(0, rollingItems.size())).getRollingItem();
                if(raw < 5){
                    inv.setItem(field[raw][col], board[raw][col]);
                }
            }
        }
        gameSession.setBoard(board);
    }

    private void refreshInventory(Inventory inv, GameSession gameSession){
        InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemInactive());
        InventoryUtil.setItemToGUI(inv, getSpotSlots(), getSpotItem());
        gameSession.setBet(new CasinoBet());
        ItemStack[][] board = gameSession.getBoard();
        for (int raw = 5; raw < 35; raw++) {
            for (int col = 0; col < 5; col++) {
                board[raw][col] = rollingItems.get(NumberUtil.randomInt(0, rollingItems.size())).getRollingItem();
            }
        }
        gameSession.setBoard(board);
    }



    @Override
    @EventHandler
    public void closeGUI(InventoryCloseEvent e){
        if (invHandler.checkIfPluginInventory(e.getView(), List.of(getGuiTitle()))) {
            Player player = (Player) e.getPlayer();
            GameSession gameSession = sessions.get(player.getUniqueId());
            if(gameSession == null) return;

            // Возврат ставки
            if(!gameSession.isSessionActive() && gameSession.getBet() != null){
                casinoGameModeUtil.refundBet(player, gameSession.getBet());
            }

            gameSession.cancelCycleID();
            sessions.remove(player.getUniqueId());
        }
    }


    @Override
    @EventHandler
    public void clickGUI(InventoryClickEvent e){
        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (invHandler.checkIfClickInventory(e.getView(), getGuiTitle(), slot, getGuiSize() * 9)) {
            e.setCancelled(true);
            if (!getPermission().isEmpty() && !PermissionUtil.hasAccessAndSendMessage(player, "&cYOU DON''T HAVE THE PERMISSION!", "casino.*", "casino.all", getPermission())) return;
            GameSession gameSession = sessions.get(player.getUniqueId());
            ISession invSession = (ISession) plugin.getInvSession().getSession(player);
            if(invSession == null || gameSession == null){
                player.closeInventory();
                return;
            }

            Inventory inv = invSession.getInv();
            ItemStack itemOnCursor = e.getCursor();
            CasinoBet bet = gameSession.getBet();

            // Если игрока уже идет
            if(gameSession.isSessionActive() || inv.getItem(slot) == null) return;

            // СИСТЕМА БИЗНЕСА
            if(invSession.getCasinoTable() != null && !casinoGameModeUtil.checkIfBusinessTableHasMoney(player, invSession.getCasinoTable().getOwner())) return;

            // Ставки
            else if(getSpotSlots().contains(slot)){

                // Новая ставка
                if(e.isLeftClick()){
                    casinoGameModeUtil.placeBet(player, bet, itemOnCursor, gameSession.getDefaultMoneyBet(), this,
                            () -> {
                                InventoryUtil.setItemToGUI(inv, slot, itemOnCursor.clone());
                                InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemActive());
                                SoundUtil.bet(player);

                                ItemStack movingItem = itemOnCursor.clone();
                                movingItem.setAmount(1);
                            },
                            () -> {
                                ItemStack visualChips = chipUtil.getChip(bet.getMoneyBet());
                                InventoryUtil.setItemToGUI(inv, slot, visualChips);
                                InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemActive());
                                SoundUtil.bet(player);

                                ItemStack movingItem = visualChips.clone();
                                movingItem.setAmount(1);
                            },
                            () -> {
                                InventoryUtil.setItemToGUI(inv, slot, itemOnCursor.clone());
                                InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemActive());
                                SoundUtil.bet(player);

                                ItemStack movingItem = chipUtil.getChip(100);
                                movingItem.setAmount(1);
                            }
                    );
                }

                // Возврат ставки
                else if(e.isRightClick() && bet.isAnyBet()){
                    casinoGameModeUtil.refundBet(player, bet);
                    InventoryUtil.setItemToGUI(inv, getSpotSlots(), getSpotItem());
                    InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemInactive());
                    SoundUtil.rollBack(player);
                }
            }

            // Запуск игры
            else if(getLeverSlots().contains(slot) && inv.getItem(slot) != null && bet.isAnyBet()){
                InventoryUtil.setItemToGUI(inv, getSpotSlots(), getSpotItem());
                InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemRolling());
                SoundUtil.activate(player);
                start(player, inv, gameSession, invSession.getCasinoTable());
            }

        }
    }


    private void start(){
        List<ResultCombination> combinationsToSet = new ArrayList<>();

        for (int i = 0; i < figuresMaxAmount; i++) {
            double chance = NumberUtil.randomDouble(0.0, 100.0);
            double left = 0.0;

            for(DrumCombination drumCombination : combinations){
                left += drumCombination.getChance();
                if(left >= chance){
                    ResultCombination resultCombination = new ResultCombination();
                    resultCombination.setDrumCombination(drumCombination);
                    resultCombination.setCombination(drumCombination.getCombinations().get(NumberUtil.randomInt(0, drumCombination.getCombinations().size())));
                    combinationsToSet.add(resultCombination);
                    break;
                }
            }
        }

        boolean isSameMaterials = false;

        if(combinationsToSet.size() > 1){
            for (int raw = 0; raw < 5; raw++) {
                for (int col = 0; col < 5; col++) {
                    boolean isTrue = false;
                    for(ResultCombination resultCombination : combinationsToSet){
                        isTrue = resultCombination.getCombination()[raw][col] == 1;
                    }
                }
            }
        }




    }




    @Override
    public void reload(){
        sessions.forEach((playerUUID, session) -> {
            CasinoBet bet = session.getBet();
            Player player = Bukkit.getPlayer(playerUUID);

            if(player != null && player.isOnline()){
                casinoGameModeUtil.refundBet(player, bet);
            }

            session.cancelCycleID();
        });
        sessions.clear();
    }


    private class GameSession{

        private int cycleID = -1;
        private CasinoBet bet;
        private double defaultMoneyBet;
        private ItemStack[][] board = new ItemStack[35][5];


        public boolean isSessionActive(){
            return cycleID != -1;
        }

        public void setCycleID(int cycleID){
            this.cycleID = cycleID;
        }

        public void cancelCycleID(){
            if(cycleID != -1){
                Bukkit.getScheduler().cancelTask(cycleID);
                cycleID = -1;
            }
        }


        public CasinoBet getBet(){
            return bet;
        }

        public void setBet(CasinoBet bet){
            this.bet = bet;
        }


        public double getDefaultMoneyBet(){
            return defaultMoneyBet;
        }

        public void setDefaultMoneyBet(double defaultMoneyBet){
            this.defaultMoneyBet = defaultMoneyBet;
        }


        public ItemStack[][] getBoard(){
            return board;
        }

        public void setBoard(ItemStack[][] board){
            this.board = board;
        }

    }


    public static class DrumCombination{

        private double chance = 0.0;
        private double multiplier = 1.0;
        private boolean isBonus = false;
        private List<int[][]> combinations = new ArrayList<>();

        public DrumCombination(double chance, double multiplier, boolean isBonus, List<List<String>> combinations){
            this.chance = chance;
            this.multiplier = multiplier;
            this.isBonus = isBonus;
            this.combinations = combinations.stream()
                    .map(group -> group.stream()
                            .map(str -> str.chars()
                                    .map(c -> c - '0')
                                    .toArray())
                            .toArray(int[][]::new)
                    )
                    .collect(Collectors.toList());
        }


        public double getChance(){
            return chance;
        }

        public double getMultiplier(){
            return multiplier;
        }

        public boolean isBonus(){
            return isBonus;
        }

        public List<int[][]> getCombinations() {
            return combinations;
        }
    }


    public static class RollingItem{

        private ItemStack rollingItem;
        private double chance;
        private double multiplier;

        public RollingItem(ItemStack rollingItem, double chance, double multiplier){
            this.rollingItem = rollingItem;
            this.chance = chance;
            this.multiplier = multiplier;
        }


        public ItemStack getRollingItem(){
            return rollingItem;
        }

        public double getChance() {
            return chance;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }


    public static class ResultCombination{

        private DrumCombination drumCombination;
        private int[][] combination;
        private RollingItem rollingItem;


        public DrumCombination getDrumCombination(){
            return drumCombination;
        }

        public void setDrumCombination(DrumCombination drumCombination){
            this.drumCombination = drumCombination;
        }


        public int[][] getCombination(){
            return combination;
        }

        public void setCombination(int[][] combination){
            this.combination = combination;
        }


        public RollingItem getRollingItem(){
            return rollingItem;
        }

        public void setRollingItem(RollingItem rollingItem){
            this.rollingItem = rollingItem;
        }


    }

}