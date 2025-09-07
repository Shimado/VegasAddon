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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.shimado.addon.VegasAddon;
import org.shimado.basicutils.BasicUtils;
import org.shimado.basicutils.inventory.InventoryUtil;
import org.shimado.basicutils.nms.IInvHandler;
import org.shimado.basicutils.utils.NumberUtil;
import org.shimado.basicutils.utils.PermissionUtil;
import org.shimado.basicutils.utils.SoundUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Drums extends CasinoGameMode implements CasinoGameModeMethods, Listener {

    private final int maxRaw = 35;
    private final int maxCol = 5;
    private final int[][] field = {  //[1][0] = 11    [0][3] = 5
            {2, 3, 4, 5, 6},
            {11,12,13,14,15},
            {20,21,22,23,24},
            {29,30,31,32,33},
            {38,39,40,41,42}
    };

    private int figuresMaxAmount = 1;
    private List<DrumCombination> possibleCombinations = new ArrayList<>();
    private List<RollingItem> possibleRollingItems = new ArrayList<>();
    private ItemStack victoryPlaceholderItem;

    private final VegasAddon plugin;
    private final IInvHandler invHandler = BasicUtils.getVersionControl().getInvHandler();
    private final CasinoGameModeUtil casinoGameModeUtil;
    private final ChipUtil chipUtil;
    private final VictoryUtil victoryUtil;
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

    public void setCombinations(List<DrumCombination> possibleCombinations){
        this.possibleCombinations = possibleCombinations;
    }

    public void setRollingItems(List<RollingItem> possibleRollingItems){
        this.possibleRollingItems = possibleRollingItems;
    }

    public void setVictoryPlaceholderItem(ItemStack victoryPlaceholderItem){
        this.victoryPlaceholderItem = victoryPlaceholderItem;
    }


    /**
     * Method for opening GUI.
     * The Vegas plugin itself implements the installation of standard background items.
     * Here, the inventory update and installation of items in it are implemented.
     * **/

    @Override
    public void openGame(Player player, Inventory inv){
        GameSession gameSession = new GameSession();
        sessions.put(player.getUniqueId(), gameSession);
        refreshInventory(inv, gameSession);

        // Sets the first 5x5 items on display. 0-5
        ItemStack[][] board = gameSession.getBoard();
        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < 5; col++) {
                board[raw][col] = possibleRollingItems.get(NumberUtil.randomInt(0, possibleRollingItems.size())).getRollingItem();
                inv.setItem(field[raw][col], board[raw][col]);
            }
        }
        gameSession.setBoard(board);
    }


    /**
     * We update the inventory, set inactive levers (when the bet is not yet set).
     * Resets the game session data for this mode.
     * Sets up slots 5 to the final row. This is where winning combinations are determined.
     * **/

    private void refreshInventory(Inventory inv, GameSession gameSession){
        InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemInactive());
        InventoryUtil.setItemToGUI(inv, getSpotSlots(), getSpotItem());
        gameSession.setBet(new CasinoBet());
        gameSession.setCycleIndex(0);
        gameSession.setVictoryCombinations(new ArrayList<>());

        ItemStack[][] board = gameSession.getBoard();

        // Sets the scroll slots that are just for the scroll background. 5 - (maxRow - 5)
        for (int raw = 5; raw < maxRaw - 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                board[raw][col] = possibleRollingItems.get(NumberUtil.randomInt(0, possibleRollingItems.size())).getRollingItem();
            }
        }

        // Selects the combinations to be shown
        List<ResultCombination> combinationsToSet = getCombinationsByChance();
        gameSession.setVictoryCombinations(combinationsToSet);

        // If it will be a defeat
        if(combinationsToSet.isEmpty()){
            setDefeatBoard(board);
        }

        // If this is a victory
        else{
            setVictoryBoard(board, combinationsToSet);
        }

        gameSession.setBoard(board);
    }


    /**
     * Gets a list of combinations that will be counted as a win or a bonus.
     * The list may be empty if the combination did not fall out.
     * **/

    private List<ResultCombination> getCombinationsByChance(){
        List<ResultCombination> combinationsToSet = new ArrayList<>();

        for (int i = 0; i < figuresMaxAmount; i++) {
            double chance = NumberUtil.randomDouble(0.0, 100.0);
            double maxRange = 0.0;
            double minRange = 0.0;

            for(DrumCombination drumCombination : possibleCombinations){
                maxRange += drumCombination.getChance();
                if(maxRange >= chance && chance >= minRange){

                    List<Integer> indexes = new ArrayList<>(IntStream.range(0, drumCombination.getCombinations().size()).boxed().collect(Collectors.toList()));
                    Collections.shuffle(indexes);

                    for (int index = 0; i < indexes.size(); index++) {
                        int[][] candidate = drumCombination.getCombinations().get(indexes.get(index));

                        if (!checkIfCombinationHoverAnother(candidate, combinationsToSet)) {
                            ResultCombination resultCombination = new ResultCombination();
                            resultCombination.setDrumCombination(drumCombination);
                            resultCombination.setCombination(candidate);
                            combinationsToSet.add(resultCombination);
                            break;
                        }
                    }

                    break;
                }
                minRange = maxRange;
            }
        }

        return combinationsToSet;
    }


    /**
     * Checks that the combination is not completely its active blocks in other combinations.
     * That is, if you have a double diagonal, then a regular diagonal can no longer fall out.
     * If the combination is 5 by 5, then any other combination cannot fall out.
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


    /**
     * If there are no winning combinations, it sets a losing field.
     * A check is made to make sure that none of the winning combinations are built.
     * **/

    private void setDefeatBoard(ItemStack[][] board){
        Map<ItemStack, int[][]> placedUnUsedItems = new HashMap<>();
        List<ItemStack> items = possibleRollingItems.stream().map(it -> it.getRollingItem()).collect(Collectors.toList());

        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                Collections.shuffle(items);

                board[raw + (maxRaw - 5)][col] = victoryPlaceholderItem;

                for (int index = 0; index < items.size(); index++) {
                    ItemStack itemToPlace = items.get(index);
                    int[][] arr = placedUnUsedItems.getOrDefault(itemToPlace, new int[5][5]);

                    if(!detectAnotherCombinations(arr)){
                        arr[raw][col] = 1;
                        placedUnUsedItems.put(itemToPlace, arr);
                        board[raw + (maxRaw - 5)][col] = itemToPlace;
                        break;
                    }
                }
            }
        }
    }


    /**
     * Finds another combination of active points in a given array.
     * **/

    private boolean detectAnotherCombinations(int[][] arr){
        for(DrumCombination drumCombination : possibleCombinations){
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


    /**
     *
     * **/

    private void setVictoryBoard(ItemStack[][] board, List<ResultCombination> combinationsToSet){
        boolean isSameMaterials = isSameMaterials(combinationsToSet);

        // Selects the material for installation
        RollingItem commonRollingItem = getRollingItemByChance();
        List<RollingItem> usedRollingItems = new ArrayList<>();

        for(ResultCombination resultCombination : combinationsToSet){
            RollingItem toUse = isSameMaterials ? commonRollingItem : getRollingItemByChance();
            usedRollingItems.add(toUse);
            resultCombination.setRollingItem(toUse);
        }

        // Selects the slots to be filled with background + sets the combinations themselves
        int[][] unUsedSlots = new int[5][5];
        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                boolean isPlacedByCombination = false;
                for(ResultCombination resultCombination : combinationsToSet){
                    if(resultCombination.getCombination()[raw][col] == 1){
                        board[raw + (maxRaw - 5)][col] = resultCombination.getRollingItem().getRollingItem(); // Here sets the winning combination
                        isPlacedByCombination = true;
                        break;
                    }
                }
                if(!isPlacedByCombination){
                    unUsedSlots[raw][col] = 1;
                }
            }
        }

        // List of items that can be used to fill the background
        List<RollingItem> unUsedRollingItems = possibleRollingItems.stream().filter(it -> {
            for(RollingItem usedRollingItem : usedRollingItems){
                if(usedRollingItem.getRollingItem().equals(it.getRollingItem())){
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        // Setting up inactive background objects. Setting up active ones is done a little higher
        Map<ItemStack, int[][]> placedUnUsedItems = new HashMap<>();

        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                if(unUsedSlots[raw][col] == 1){
                    Collections.shuffle(unUsedRollingItems);

                    board[raw + (maxRaw - 5)][col] = victoryPlaceholderItem;

                    for (int index = 0; index < unUsedRollingItems.size(); index++) {
                        ItemStack itemToPlace = unUsedRollingItems.get(index).getRollingItem();
                        int[][] arr = placedUnUsedItems.getOrDefault(itemToPlace, new int[5][5]);

                        if(!detectAnotherCombinations(arr)){
                            arr[raw][col] = 1;
                            placedUnUsedItems.put(itemToPlace, arr);
                            board[raw + (maxRaw - 5)][col] = itemToPlace;
                            break;
                        }
                    }
                }
            }
        }
    }


    /**
     * If suddenly active combinations intersect, then they must be of the same material.
     * If not, then they can be different, or the same.
     * **/

    private boolean isSameMaterials(List<ResultCombination> combinationsToSet){
        if(combinationsToSet.size() > 1){
            for (int raw = 0; raw < 5; raw++) {
                for (int col = 0; col < maxCol; col++) {
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


    /**
     * Receives an item for a winning combination.
     * **/

    private RollingItem getRollingItemByChance(){
        double totalSum = possibleRollingItems.stream().mapToDouble(RollingItem::getChance).sum();
        double chance = NumberUtil.randomDouble(0.0, totalSum);
        double minRange = 0.0;
        double maxRange = 0.0;

        for(RollingItem rollingItem : possibleRollingItems){
            maxRange += rollingItem.getChance();
            if(maxRange >= chance && chance >= minRange){
                return rollingItem;
            }
            minRange = maxRange;
        }

        return possibleRollingItems.get(NumberUtil.randomInt(0, possibleRollingItems.size()));
    }


    /**
     * Event that handles closing of the game GUI.
     * Checks if this is a game GUI by the title. And the method implements
     * the return of the token if the game has not yet started.
     * Removes the player from the session list.
     * **/

    @Override
    @EventHandler
    public void closeGUI(InventoryCloseEvent e){
        if (invHandler.checkIfPluginInventory(e.getView(), List.of(getGuiTitle()))) {
            Player player = (Player) e.getPlayer();
            GameSession gameSession = sessions.get(player.getUniqueId());
            if(gameSession == null) return;

            // Refund of the bet
            if(!gameSession.isSessionActive() && gameSession.getBet() != null){
                casinoGameModeUtil.refundBet(player, gameSession.getBet());
            }

            gameSession.cancelCycleID();
            sessions.remove(player.getUniqueId());
        }
    }


    /**
     * Handles clicking in the GUI.
     * The Vegas plugin itself handles music inclusion/selection if you have the BoomBox plugin on the server.
     * This is where bets are processed, the game is launched, and various checks are performed.
     * **/

    @Override
    @EventHandler
    public void clickGUI(InventoryClickEvent e){
        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (invHandler.checkIfClickInventory(e.getView(), getGuiTitle(), slot, getGuiSize() * 9)) {
            e.setCancelled(true);
            if (!getPermission().isEmpty() && !PermissionUtil.hasAccessAndSendMessage(player, "&cYOU DON''T HAVE THE PERMISSION!", "casino.*", "casino.all", getPermission())) return;
            GameSession gameSession = sessions.get(player.getUniqueId());
            ISession invSession = plugin.getInvSession().getSession(player);
            if(invSession == null || gameSession == null){
                player.closeInventory();
                return;
            }

            Inventory inv = invSession.getInv();
            ItemStack itemOnCursor = e.getCursor();
            CasinoBet bet = gameSession.getBet();

            // If the player is already walking
            if(gameSession.isSessionActive() || inv.getItem(slot) == null) return;

            // Optional. Checking if the table owner has the money to pay out in case of a win
            if(invSession.getCasinoTable() != null && !casinoGameModeUtil.checkIfBusinessTableHasMoney(player, invSession.getCasinoTable().getOwner())) return;

            // Bets
            if(getSpotSlots().contains(slot)){

                // New rate
                if(e.isLeftClick()){
                    casinoGameModeUtil.placeBet(player, bet, itemOnCursor, gameSession.getDefaultMoneyBet(), this,
                            () -> {
                                InventoryUtil.setItemToGUI(inv, slot, itemOnCursor.clone());
                                InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemActive());
                                SoundUtil.bet(player);
                            },
                            () -> {
                                InventoryUtil.setItemToGUI(inv, slot, chipUtil.getChip(bet.getMoneyBet()));
                                InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemActive());
                                SoundUtil.bet(player);
                            },
                            () -> {
                                InventoryUtil.setItemToGUI(inv, slot, itemOnCursor.clone());
                                InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemActive());
                                SoundUtil.bet(player);
                            }
                    );
                }

                // Refund of the bet
                else if(e.isRightClick() && bet.isAnyBet()){
                    casinoGameModeUtil.refundBet(player, bet);
                    InventoryUtil.setItemToGUI(inv, getSpotSlots(), getSpotItem());
                    InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemInactive());
                    SoundUtil.rollBack(player);
                }
            }

            // Launching the game
            else if(getLeverSlots().contains(slot) && inv.getItem(slot) != null && bet.isAnyBet()){
                InventoryUtil.setItemToGUI(inv, getSpotSlots(), getSpotItem());
                InventoryUtil.setItemToGUI(inv, getLeverSlots(), getLeverItemRolling());
                SoundUtil.activate(player);
                start(player, inv, gameSession, invSession);
            }

        }
    }


    /**
     * Starts a rotation cycle of the specified objects and after
     * reaching the maximum line stops the cycle and processes the result.
     * **/

    private void start(Player player, Inventory inv, GameSession gameSession, ISession invSession){
        int cycleID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int index = gameSession.getCycleIndex();
            for (int raw = 0; raw < 5; raw++) {
                for (int col = 0; col < maxCol; col++) {
                    inv.setItem(field[raw][col], gameSession.getBoard()[raw + index][col]);
                }
            }
            index++;
            if(index >= maxRaw){
                gameSession.cancelCycleID();
                checkEnd(player, gameSession, invSession);
            }
        }, 0, 5);
        gameSession.setCycleID(cycleID);
    }


    /**
     * Processes victory and defeat and after all, with a small delay, updates the GUI
     * **/

    private void checkEnd(Player player, GameSession gameSession, ISession invSession){
        Location tableLoc = invSession.getCasinoTable() == null ? player.getLocation() : invSession.getCasinoTable().getLoc();
        UUID tableOwnerUUID = invSession.getCasinoTable() == null ? null : invSession.getCasinoTable().getOwner();

        ItemStack[][] board = gameSession.getBoard();

        // Index change. Last (maxRow - 5) - maxRow items become first 0 - 5
        for (int raw = 0; raw < 5; raw++) {
            for (int col = 0; col < maxCol; col++) {
                board[raw][col] = board[raw + (maxRaw - 5)][col];
            }
        }

        // Defeat
        if(gameSession.getVictoryCombinations().isEmpty()){
            victoryUtil.defeat(player, gameSession.getBet(), this, tableLoc, tableOwnerUUID);
            int delayID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                gameSession.cancelCycleID();
                refreshInventory(invSession.getInv(), gameSession);
            }, 40);
            gameSession.setCycleID(delayID);
        }

        // Victory
        else{
            List<ResultCombination> victoryCombinations = gameSession.getVictoryCombinations();

            for(ResultCombination resultCombination : victoryCombinations){
                DrumCombination combination = resultCombination.getDrumCombination();
                double finalMultiplier = combination.getMultiplier() * resultCombination.getRollingItem().getMultiplier();
                if(!combination.isBonus()){
                    victoryUtil.victory(player, gameSession.getBet(), this, finalMultiplier, tableLoc, tableOwnerUUID);
                }else{
                    victoryUtil.bonus(player, gameSession.getBet(), this, finalMultiplier, tableLoc, tableOwnerUUID);
                }
            }

            for (int raw = 0; raw < 5; raw++) {
                for (int col = 0; col < maxCol; col++) {
                    invSession.getInv().setItem(field[raw][col], victoryPlaceholderItem);
                    for(ResultCombination r : victoryCombinations){
                        if(r.getCombination()[raw][col] == 1){
                            invSession.getInv().setItem(field[raw][col], r.getRollingItem().getRollingItem());
                        }
                    }
                }
            }

            int delayID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                gameSession.cancelCycleID();

                for (int raw = 0; raw < 5; raw++) {
                    for (int col = 0; col < maxCol; col++) {
                        invSession.getInv().setItem(field[raw][col], board[raw][col]);
                    }
                }

                refreshInventory(invSession.getInv(), gameSession);
            }, 40);
            gameSession.setCycleID(delayID);
        }

    }


    /**
     * Necessary for the Vegas plugin, to update data.
     * In this case, it is needed to return chips and clear the session.
     * **/

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