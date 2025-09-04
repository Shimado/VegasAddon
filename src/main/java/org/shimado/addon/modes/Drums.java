package org.shimado.addon.modes;

import com.github.Shimado.api.CasinoGameModeMethods;
import com.github.Shimado.api.CasinoGameModeUtil;
import com.github.Shimado.api.ChipUtil;
import com.github.Shimado.api.VictoryUtil;
import com.github.Shimado.instances.CasinoBet;
import com.github.Shimado.instances.CasinoGameMode;
import com.github.Shimado.interfaces.ISession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.stream.IntStream;

public class Drums extends CasinoGameMode implements CasinoGameModeMethods, Listener {

    private int figuresMaxAmount = 1;
    private List<DrumCombination> combinations = new ArrayList<>();
    private List<RollingItem> rollingItems = new ArrayList<>();
    private ItemStack victoryPlaceholderItem;
    private final int[][] field = {  //[1][0] = 11    [0][3] = 5
            {2,3,4,5,6},
            {11,12,13,14,15},
            {20,21,22,23,24},
            {29,30,31,32,33},
            {38,39,40,41,42}
    };
    private final int maxRaw = 35;
    private final int maxCol = 5;

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

    public void setVictoryPlaceholderItem(ItemStack victoryPlaceholderItem){
        this.victoryPlaceholderItem = victoryPlaceholderItem;
    }


    /**
     * ОСНОВНАЯ МЕХМНИКА
     * **/

    @Override
    public void openGame(Player player, Inventory inv){
        GameSession gameSession = new GameSession();
        sessions.put(player.getUniqueId(), gameSession);
        refreshInventory(inv, gameSession);

        // Устанавливает первые 5х5 предметов напоказ
        ItemStack[][] board = gameSession.getBoard();
        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < 5; col++) {
                board[raw][col] = rollingItems.get(NumberUtil.randomInt(0, rollingItems.size())).getRollingItem();
                inv.setItem(field[raw][col], board[raw][col]);
            }
        }
        gameSession.setBoard(board);
    }


    /**
     *
     * **/

    private void refreshInventory(Inventory inv, GameSession gameSession){
        InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemInactive());
        InventoryUtil.setItemToGUI(inv, getSpotSlots(), getSpotItem());
        gameSession.setBet(new CasinoBet());
        gameSession.setCycleIndex(0);
        gameSession.setVictoryCombinations(new ArrayList<>());

        ItemStack[][] board = gameSession.getBoard();

        // Устанавливает слоты прокрутки, которые нужны просто для фона прокрутки
        for (int raw = 5; raw < maxRaw - 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                board[raw][col] = rollingItems.get(NumberUtil.randomInt(0, rollingItems.size())).getRollingItem();
            }
        }

        // Выбирает комбинации, которые будут показаны
        List<ResultCombination> combinationsToSet = getCombinationsByChance();
        gameSession.setVictoryCombinations(combinationsToSet);

        // Если это будет поражение
        if(combinationsToSet.isEmpty()){
            setDefeatBoard(board);
        }

        // Если это будет победа
        else{
            setVictoryBoard(board, combinationsToSet);
        }

        gameSession.setBoard(board);
    }


    /**
     *
     * **/

    private List<ResultCombination> getCombinationsByChance(){
        List<ResultCombination> combinationsToSet = new ArrayList<>();

        for (int i = 0; i < figuresMaxAmount; i++) {
            double chance = NumberUtil.randomDouble(0.0, 100.0);
            double maxRange = 0.0;
            double minRange = 0.0;

            for(DrumCombination drumCombination : combinations){
                maxRange += drumCombination.getChance();
                if(maxRange <= chance && chance >= minRange){
                    ResultCombination resultCombination = new ResultCombination();
                    resultCombination.setDrumCombination(drumCombination);

                    List<Integer> indexes = IntStream.range(0, drumCombination.getCombinations().size()).boxed().toList();
                    Object index = indexes.get(NumberUtil.randomInt(0, indexes.size()));
                    indexes.remove(index);
                    int[][] arrToPlace = drumCombination.getCombinations().get((int) index);

                    boolean notPlace = false;
                    
                    while (!indexes.isEmpty() && checkIfCombinationHoverAnother(arrToPlace, combinationsToSet)){
                        index = indexes.get(NumberUtil.randomInt(0, indexes.size()));
                        indexes.remove(index);
                        arrToPlace = drumCombination.getCombinations().get((int) index);
                        if(indexes.isEmpty()){
                            notPlace = true;
                        }
                    }

                    if(!notPlace){
                        resultCombination.setCombination(arrToPlace);
                        combinationsToSet.add(resultCombination);
                    }

                    break;
                }
                minRange = maxRange;
            }
        }

        return combinationsToSet;
    }


    /**
     *
     * **/

    private boolean checkIfCombinationHoverAnother(int[][] arrToPlace, List<ResultCombination> combinationsToSet){
        int[][] checkArr = new int[5][5];
        for(ResultCombination r : combinationsToSet){
            for (int raw = 0; raw < 5; raw++) {
                for (int col = 0; col < maxCol; col++) {
                    if(r.getCombination()[raw][col] == 1){
                        checkArr[raw][col] = 1;
                    }
                }
            }
        }

        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                if(arrToPlace[raw][col] == 1 && checkArr[raw][col] == 0){
                    return false;
                }
            }
        }

        return true;
    }


    private boolean isSameMaterials(List<ResultCombination> combinationsToSet){
        if(combinationsToSet.size() > 1){
            for (int raw = 0; raw < 5; raw++) {
                for (int col = 0; col < 5; col++) {
                    boolean isTrue = false;
                    for(ResultCombination resultCombination : combinationsToSet){
                        if(resultCombination.getCombination()[raw][col] == 1){
                            if(isTrue){
                                return true;
                            }else{
                                isTrue = true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }


    private boolean detectAnotherCombinations(int[][] arr){
        for(DrumCombination drumCombination : combinations){
            for(int[][] iArr : drumCombination.getCombinations()){
                boolean nextCombination = false;
                for (int raw = 0; raw < 5; raw++) {
                    if(nextCombination) break;
                    for (int col = 0; col < maxCol; col++) {
                        if(iArr[raw][col] != arr[raw][col]){
                            nextCombination = true;
                            break;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }


    private void setDefeatBoard(ItemStack[][] board){
        Map<ItemStack, int[][]> placedUnUsedItems = new HashMap<>();

        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                int maxCount = 0;
                ItemStack itemToPlace = rollingItems.get(NumberUtil.randomInt(0, rollingItems.size())).getRollingItem();
                int[][] arr = placedUnUsedItems.getOrDefault(itemToPlace, new int[5][5]);

                while (detectAnotherCombinations(arr) && maxCount < 20){
                    itemToPlace = rollingItems.get(NumberUtil.randomInt(0, rollingItems.size())).getRollingItem();
                    arr = placedUnUsedItems.getOrDefault(itemToPlace, new int[5][5]);
                    maxCount++;
                }

                arr[raw][col] = 1;
                placedUnUsedItems.put(itemToPlace, arr);
                board[raw + (maxRaw - 5)][col] = itemToPlace;
            }
        }
    }


    private void setVictoryBoard(ItemStack[][] board, List<ResultCombination> combinationsToSet){
        boolean isSameMaterials = isSameMaterials(combinationsToSet);

        RollingItem commonRollingItem = rollingItems.get(NumberUtil.randomInt(0, rollingItems.size()));
        List<RollingItem> usedRollingItems = new ArrayList<>();
        for(ResultCombination resultCombination : combinationsToSet){
            RollingItem toUse = isSameMaterials ? commonRollingItem : rollingItems.get(NumberUtil.randomInt(0, rollingItems.size()));
            usedRollingItems.add(toUse);
            resultCombination.setRollingItem(toUse);
        }

        int[][] unUsedSlots = new int[5][5];
        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                boolean isPlacedByCombination = false;
                for(ResultCombination resultCombination : combinationsToSet){
                    if(resultCombination.getCombination()[raw][col] == 1){
                        board[raw + (maxRaw - 5)][col] = resultCombination.getRollingItem().getRollingItem();
                        isPlacedByCombination = true;
                    }
                }
                if(!isPlacedByCombination){
                    unUsedSlots[raw][col] = 1;
                }
            }
        }

        List<RollingItem> unUsedRollingItems = rollingItems.stream().filter(it -> {
            for(RollingItem usedRollingItem : usedRollingItems){
                if(usedRollingItem.getRollingItem().equals(it.getRollingItem())){
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        Map<ItemStack, int[][]> placedUnUsedItems = new HashMap<>();

        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                if(unUsedSlots[raw][col] == 1){
                    int maxCount = 0;
                    ItemStack itemToPlace = unUsedRollingItems.get(NumberUtil.randomInt(0, unUsedRollingItems.size())).getRollingItem();
                    int[][] arr = placedUnUsedItems.getOrDefault(itemToPlace, new int[5][5]);

                    while (detectAnotherCombinations(arr) && maxCount < 20){
                        itemToPlace = unUsedRollingItems.get(NumberUtil.randomInt(0, unUsedRollingItems.size())).getRollingItem();
                        arr = placedUnUsedItems.getOrDefault(itemToPlace, new int[5][5]);
                        maxCount++;
                    }

                    arr[raw][col] = 1;
                    placedUnUsedItems.put(itemToPlace, arr);
                    board[raw + (maxRaw - 5)][col] = itemToPlace;
                }
            }
        }
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
                start(player, inv, gameSession, invSession);
            }

        }
    }


    private void start(Player player, Inventory inv, GameSession gameSession, ISession session){
        int cycleID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int index = gameSession.getCycleIndex();
            for (int raw = 0; raw < 5; raw++) {
                for (int col = 0; col < 5; col++) {
                    inv.setItem(field[raw][col], gameSession.getBoard()[raw + index][col]);
                }
            }
            index++;
            if(index >= 34){
                gameSession.cancelCycleID();
                checkEnd(player, gameSession, session);
            }
        }, 0, 5);
        gameSession.setCycleID(cycleID);
    }


    private void checkEnd(Player player, GameSession gameSession, ISession session){
        Location tableLoc = session.getCasinoTable() == null ? player.getLocation() : session.getCasinoTable().getLoc();
        UUID tableOwnerUUID = session.getCasinoTable() == null ? null : session.getCasinoTable().getOwner();

        ItemStack[][] board = gameSession.getBoard();

        // Смена индексов
        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                board[raw][col] = board[raw + (maxRaw - 5)][col];
            }
        }

        // Поражение
        if(gameSession.getVictoryCombinations().isEmpty()){
            victoryUtil.defeat(player, gameSession.getBet(), this, tableLoc, tableOwnerUUID);
            int delayID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                gameSession.cancelCycleID();
                refreshInventory(session.getInv(), gameSession);
            }, 40);
            gameSession.setCycleID(delayID);
        }
        // Победа
        else{
            List<ResultCombination> victoryCombinations = gameSession.getVictoryCombinations();

            for(ResultCombination resultCombination : victoryCombinations){
                DrumCombination combination = resultCombination.getDrumCombination();
                if(!combination.isBonus()){
                    victoryUtil.victory(player, gameSession.getBet(), this, combination.getMultiplier() * resultCombination.getRollingItem().getMultiplier(), tableLoc, tableOwnerUUID);
                }else{
                    victoryUtil.bonus(player, gameSession.getBet(), this, combination.getMultiplier() * resultCombination.getRollingItem().getMultiplier(), tableLoc, tableOwnerUUID);
                }
            }

            for (int raw = 0; raw < 5; raw++) {
                for (int col = 0; col < maxCol; col++) {
                    session.getInv().setItem(field[raw][col], victoryPlaceholderItem);
                    for(ResultCombination r : victoryCombinations){
                        if(r.getCombination()[raw][col] == 1){
                            session.getInv().setItem(field[raw][col], r.getRollingItem().getRollingItem());
                        }
                    }
                }
            }

            int delayID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                gameSession.cancelCycleID();

                for (int raw = 0; raw < 5; raw++) {
                    for (int col = 0; col < maxCol; col++) {
                        session.getInv().setItem(field[raw][col], board[raw][col]);
                    }
                }

                refreshInventory(session.getInv(), gameSession);
            }, 40);
            gameSession.setCycleID(delayID);
        }

    }


    @Override
    public void reload(){
        sessions.forEach((playerUUID, gameSession) -> {
            CasinoBet bet = gameSession.getBet();
            Player player = Bukkit.getPlayer(playerUUID);

            if(player != null && player.isOnline()){
                casinoGameModeUtil.refundBet(player, bet);
            }

            gameSession.cancelCycleID();
        });
        sessions.clear();
    }


    private class GameSession{

        private int cycleID = -1;
        private int cycleIndex = 0;
        private CasinoBet bet;
        private double defaultMoneyBet;
        private ItemStack[][] board = new ItemStack[maxRaw][maxCol];
        private List<ResultCombination> victoryCombinations = new ArrayList<>();


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


        public int getCycleIndex(){
            return cycleIndex;
        }

        public void setCycleIndex(int cycleIndex){
            this.cycleIndex = cycleIndex;
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


        public List<ResultCombination> getVictoryCombinations(){
            return victoryCombinations;
        }

        public void setVictoryCombinations(List<ResultCombination> victoryCombinations){
            this.victoryCombinations = victoryCombinations;
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