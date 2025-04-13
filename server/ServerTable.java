import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import shared.Player;
import shared.card_based.*;
import shared.card_based.Card.*;
import shared.card_based.Poker_Hands.winners;
import shared.communication_objects.*;

/**
 * This is a game in progress.
 * 
 * Has basic poker logic and functionality.
 * 
 * After all turns are done, calculates who has the best hand and gives them the
 * pot.
 */
public class ServerTable implements Runnable {

    private int currentbet, lastActive, currentplayer, pot, currentTurn, numPlayers, gameId;
    private boolean activePlayers[];
    private boolean roundCompleted, activeCheck, inprogress;
    private ArrayList<PlayerConnection> connections;
    private ArrayList<Player> players;
    private ArrayList<Card> tablecards;
    private ArrayList<Integer> currentBets;
    private Deck deck;

    private static Map<Integer, ServerTable> gameInstances = new HashMap<>();

    /**
     * ServerTable constructor
     * 
     * @param gameId      int for Game Id
     * @param connections List of player connections
     */
    public ServerTable(int gameId, ArrayList<PlayerConnection> connections) {
        this.gameId = gameId;
        this.connections = connections;
        this.players = new ArrayList<>();
        this.inprogress = false;
        for (PlayerConnection pc : connections) {
            players.add(pc.getPlayer());
            try {
                pc.getSocket().setSoTimeout(600000); // 60-minute timeout.
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        gameInstances.put(gameId, this);
    }

    /**
     * Gets the specified instance of a table
     * 
     * @param gameId the table id requested
     * @return ServerTable
     */
    public static ServerTable getInstance(int gameId) {
        return gameInstances.get(gameId);
    }

    /**
     * Attempts to reconnect players
     * 
     * @param pcs List of player connections
     */
    public void reconnectPlayers(ArrayList<PlayerConnection> pcs) {
        connections = pcs;
    }

    /**
     * Gets the table id
     * 
     * @return int table id
     */
    public int getTableID() {
        return gameId;
    }

    /**
     * Called to update the game state (via replication).
     * 
     * @param state a game state
     */
    public synchronized void updateState(GameState state) {
        this.pot = state.getPot();
        this.currentTurn = state.getCurrentTurn();
        this.lastActive = state.getLastActive();
        this.numPlayers = state.getNumPlayers();
        this.currentbet = state.getCurrentBet();
        this.inprogress = state.getProgress();
        this.activePlayers = state.getActivePlayers();
        this.currentplayer = state.getCurrentPlayer();
        this.roundCompleted = state.getRoundCompleted();
        this.players = state.getPlayers();
        this.currentBets = state.getCurrentBets();
        this.tablecards = state.getTableCards();
        this.deck = state.getSavedDeck();
        System.out.println("Table #" + gameId + " state updated..");
    }

    /**
     * Runs the actual poker game and contains all the logic necessary to do so.
     */
    @Override
    public void run() {
        if (!inprogress) {
            System.out.println("Game Started...");
            sendAllPlayers(Command.Type.MESSAGE, new Message("The Game has Begun..."));
            assignSeats();
            numPlayers = connections.size();
            activePlayers = new boolean[numPlayers];
            pot = 0;
            currentTurn = 1;
            currentbet = 0;
            deck = new Deck();
            currentplayer = 0;
            currentBets = new ArrayList<>();
            tablecards = new ArrayList<>();
            inprogress = true;
            // Initialize players: mark all active, deal two cards each, and set initial
            // bets.
            for (int i = 0; i < numPlayers; i++) {
                activePlayers[i] = true;
                players.get(i).new_card(deck.deal_card());
                players.get(i).new_card(deck.deal_card());
                sendPlayer(Command.Type.MESSAGE, new Message("Your cards are: " + players.get(i).show_all_cards()), i);
                currentBets.add(0);
            }
            lastActive = currentplayer;
        } else {
            System.out.println("Table #" + this.gameId + " is resuming...");
            connections = organizeSeats();
            sendAllPlayers(Command.Type.MESSAGE, new Message("Resuming Table #" + this.gameId + "..."));
            for (int i = 0; i < numPlayers; i++) {
                //printTableCards();
                sendPlayer(Command.Type.MESSAGE, new Message("Your cards are: " + players.get(i).show_all_cards()), i);
            }
        }

        // Main game loop for each street until showdown.
        while (currentTurn <= 5) {
            replicateGameState();
            boolean bettingStart = true; // used to check if it's the first turn
            roundCompleted = false;
            activeCheck = false;
            do {
                if (activePlayers[currentplayer]) {
                    getPlayerInput();
                }
                if ((!activePlayers[currentplayer]) && currentplayer == lastActive) {
                    activeCheck = true;
                }
                incrementPlayer();
                // End betting round when we have looped back to the start and the current
                // player is also the last who bet.
                if (!bettingStart && currentplayer == lastActive && activeCheck) {
                    roundCompleted = true;
                }
                if (bettingStart) {// will skip over the first turn
                    bettingStart = false;
                }
            } while (!roundCompleted);

            sendAllPlayers(Command.Type.MESSAGE, new Message("Betting round over. Moving to next turn...."));
            currentTurn++;
            clearBets();

            switch (currentTurn) {
                case 2: // Flop.
                    tablecards.add(deck.deal_card());
                    tablecards.add(deck.deal_card());
                    tablecards.add(deck.deal_card());
                    replicateGameState();
                    sendAllPlayers(Command.Type.MESSAGE,
                            new Message("Turn 2: The Flop\nTable Cards: " + tablecards.toString()));
                    break;
                case 3: // Turn.
                    tablecards.add(deck.deal_card());
                    replicateGameState();
                    sendAllPlayers(Command.Type.MESSAGE,
                            new Message("Turn 3: The Turn\nTable Cards: " + tablecards.toString()));
                    break;
                case 4: // River.
                    tablecards.add(deck.deal_card());
                    replicateGameState();
                    sendAllPlayers(Command.Type.MESSAGE,
                            new Message("Turn 4: The River\nTable Cards: " + tablecards.toString()));
                    break;
                case 5: // Showdown.
                    sendAllPlayers(Command.Type.MESSAGE, new Message("Turn 5: The Showdown!"));
                    ArrayList<Poker_Hands> winners = determine_winner();
                    if (winners.size() == 1) {
                        int winnum = winners.get(0).playernumber;
                        sendAllPlayers(Command.Type.MESSAGE,
                                new Message("Player " + winnum + " has won the round! They earn $" + pot + "..."));
                        players.get(winnum).deposit_funds(pot);
                    } else {
                        sendAllPlayers(Command.Type.MESSAGE, new Message("Players tied! Splitting pot..."));
                        int share = pot / winners.size();
                        for (Poker_Hands ph : winners) {
                            players.get(ph.playernumber).deposit_funds(share);
                        }
                    }
                    // Reset for new hand.
                    for (int i = 0; i < players.size(); i++) {
                        players.get(i).clear_hand();
                        if (players.get(i).view_funds() > 0) {
                            activePlayers[i] = true;
                            players.get(i).new_card(deck.deal_card());
                            players.get(i).new_card(deck.deal_card());
                            sendPlayer(Command.Type.MESSAGE,
                                    new Message("New hand: " + players.get(i).show_all_cards()), i);
                        } else {
                            activePlayers[i] = false;
                        }
                    }
                    pot = 0;
                    currentTurn = 1;
                    clearBets();
                    tablecards.clear();
                    currentplayer = 0;
                    lastActive = currentplayer;
                    sendAllPlayers(Command.Type.MESSAGE, new Message("New hand started..."));
                    break;
            }
            lastActive = currentplayer;
            bettingStart = true;
        }
    }

    private void printTableCards() {
        switch (currentTurn) {
            case 2: // Flop.
                sendAllPlayers(Command.Type.MESSAGE,
                        new Message("Turn 2: The Flop\nTable Cards: " + tablecards.toString()));
                break;
            case 3: // Turn.
                sendAllPlayers(Command.Type.MESSAGE,
                        new Message("Turn 3: The Turn\nTable Cards: " + tablecards.toString()));
                break;
            case 4: // River.
                sendAllPlayers(Command.Type.MESSAGE,
                        new Message("Turn 4: The River\nTable Cards: " + tablecards.toString()));
                break;
        }
    }

    /**
     * Clears all the best on the table
     */
    private void clearBets() {
        currentbet = 0;
        for (int i = 0; i < currentBets.size(); i++) {
            currentBets.set(i, 0);
        }
    }

    /**
     * Assigns the seating arrangment to the players
     */
    private void assignSeats() {
        for (int i = 0; i < connections.size(); i++) {
            connections.get(i).getPlayer().set_seat(i);
            sendPlayer(Command.Type.CLIENT_UPDATE_PLAYER, players.get(i), i);
        }
    }

    /**
     * Upon reconnecting players, try and reorganize them based on the seatings
     * 
     * @return List of Player Connections
     */
    private ArrayList<PlayerConnection> organizeSeats() {
        System.out.println("Reorganizing player seats...");
        // Map seat numbers to corresponding connections
        Map<Integer, PlayerConnection> seatToConnectionMap = new HashMap<>();
        for (int i = 0; i < connections.size(); i++) {
            seatToConnectionMap.put((Integer) connections.get(i).getPlayer().get_seat(), connections.get(i));
        }
        // Create a sorted list based on seat numbers
        ArrayList<PlayerConnection> sortedConnections = new ArrayList<>();
        for (int i = 0; i < connections.size(); i++) {
            sortedConnections.add(seatToConnectionMap.get(i));
        }
        System.out.println("Finished organizing players...");
        return sortedConnections;
    }

    /**
     * Gets player input from the player connections socket.
     * Sends a turn token to the player whos turn it is
     */
    private void getPlayerInput() {
        try {
            // send the player the token to signal their turn
            sendPlayer(Command.Type.TURN_TOKEN, new Token(), currentplayer);

            // Read a command response from the connection
            Command command = (Command) connections.get(currentplayer).readCommand();

            if (command.getType() == Command.Type.TURN_CHOICE) {

                TurnChoice playerChoice = (TurnChoice) command.getPayload();

                switch (playerChoice.getChoice()) {

                    case CHECK:
                        if ((currentBets.get(currentplayer) >= currentbet
                                && players.get(currentplayer).view_funds() != 0)) { // if funds are == 0 then they're
                                                                                    // all in and they're allowed to
                                                                                    // continue playing until the end of
                                                                                    // the round
                            sendAllPlayers(Command.Type.MESSAGE, new Message("Player " + currentplayer + " checks..."));
                            if (currentplayer == lastActive) {
                                activeCheck = true;
                            }
                        } else {
                            sendPlayer(Command.Type.MESSAGE, new Message("You must call, fold, or raise!"),
                                    currentplayer);
                            getPlayerInput();
                        }
                        break;

                    case FUNDS:
                        sendPlayer(Command.Type.MESSAGE,
                                new Message("Your funds: $" + players.get(currentplayer).view_funds()), currentplayer);
                        getPlayerInput();
                        break;

                    case CARD:
                        sendPlayer(Command.Type.MESSAGE,
                                new Message("Your cards: " + players.get(currentplayer).view_cards() + "\n" +
                                        "Table cards: " + tablecards),
                                currentplayer);
                        getPlayerInput();
                        break;

                    case FOLD:
                        int numac = 0;
                        for (int i = 0; i < activePlayers.length; i++) {
                            if (activePlayers[i]) {
                                numac++;
                            }
                        }
                        if (numac == 1) {// i.e only 1 player left on the table
                            sendPlayer(Command.Type.MESSAGE,
                                    new Message("You're the last player, please check until the final round..."),
                                    currentplayer);
                            getPlayerInput();
                            break;
                        }
                        activePlayers[currentplayer] = false;
                        sendAllPlayers(Command.Type.MESSAGE, new Message("Player " + currentplayer + " folds..."));
                        break;

                    case BET:
                        int amount = playerChoice.getBet();
                        if (amount <= 0 || amount > players.get(currentplayer).view_funds()
                                || amount <= (currentbet - currentBets.get(currentplayer))) {
                            sendPlayer(Command.Type.MESSAGE, new Message("Invalid bet amount. Try again..."),
                                    currentplayer);
                            getPlayerInput();
                        } else {
                            players.get(currentplayer).deposit_funds(-amount);
                            sendAllPlayers(Command.Type.MESSAGE,
                                    new Message("Player " + currentplayer + " bets $" + amount + "..."));
                            currentbet = amount;
                            pot += amount;
                            currentBets.set(currentplayer, amount);
                            lastActive = currentplayer;
                        }
                        break;

                    case CALL:
                        if (currentbet > players.get(currentplayer).view_funds()) {
                            sendPlayer(Command.Type.MESSAGE, new Message("Insufficient funds to call. You must fold..."),
                                    currentplayer);
                            activePlayers[currentplayer] = false;
                        } else {
                            players.get(currentplayer).deposit_funds(-currentbet);
                            pot += currentbet;
                            currentBets.set(currentplayer, currentbet);
                            sendAllPlayers(Command.Type.MESSAGE, new Message("Player " + currentplayer + " calls..."));
                        }
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Increment player count
     */
    private void incrementPlayer() {
        currentplayer = (currentplayer + 1) % connections.size();
    }

    /**
     * Sends a Command object to all players at the table
     * 
     * @param type Command.Type
     * @param obj  Object to send
     */
    private void sendAllPlayers(Command.Type type, Object obj) {
        for (PlayerConnection pc : connections) {
            pc.sendCommand(type, obj);
        }
    }

    /**
     * Sends a command to a singular player
     * 
     * @param type  Command.Type
     * @param obj   Object to send
     * @param index The player id to send it to
     */
    private void sendPlayer(Command.Type type, Object obj, int index) {
        connections.get(index).sendCommand(type, obj);
    }

    /**
     * Replicates the game state across all backups using the Replication Manager
     */
    private void replicateGameState() {
        System.out.println("Beginning Game #" + gameId + " replication!");
        ArrayList<Card> tablecardsSnapshot = new ArrayList<>(tablecards);
        GameState currentState = new GameState(gameId, pot, currentTurn, currentplayer, lastActive, numPlayers,
                currentbet, ServerLamportClock.getInstance().sendEvent(), inprogress, activePlayers, roundCompleted,
                players, currentBets, tablecardsSnapshot, deck);
        ReplicationManager.getInstance(true).sendStateUpdate(currentState);
        System.out.println("Finished Game #" + gameId + " replication!");
        replicatePlayer();
    }

    /**
     * Replicates new player data to the players
     */
    private void replicatePlayer() {
        for (PlayerConnection pc : connections) {
            pc.sendCommand(Command.Type.CLIENT_UPDATE_PLAYER, pc.getPlayer());
        }
    }

    /**
     * The determine_winner(), check_other(), check_flush(), and check_straight
     * methods remain unchanged.
     * 
     * @return
     */
    private ArrayList<Poker_Hands> determine_winner() {
        // ... existing logic remains unchanged ...
        ArrayList<Poker_Hands> wins = new ArrayList<>();
        ArrayList<ArrayList<Card>> hands = new ArrayList<>();
        ArrayList<Integer> player = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (activePlayers[i]) {
                hands.add(players.get(i).show_all_cards());
                player.add(i);
            }
        }
        ArrayList<Card> combined = new ArrayList<>();
        for (int j = 0; j < hands.size(); j++) {
            for (int i = 0; i < tablecards.size(); i++) {
                combined.add(tablecards.get(i));
            }
            combined.add(hands.get(j).get(0));
            combined.add(hands.get(j).get(1));
            ArrayList<Card> flush = check_flush(combined);
            if (!flush.isEmpty()) {
                if (!check_straight(flush).isEmpty()) {
                    flush.sort(Comparator.comparing(Card::get_rank));
                    wins.add(new Poker_Hands(winners.STRAIGHTFLUSH, flush.get(flush.size() - 1), player.get(j)));
                    combined.clear();
                    continue;
                }
            }
            wins.add(check_other(combined, j, player));
            if (wins.get(j).result == winners.FOURKIND || wins.get(j).result == winners.FULLHOUSE) {
                combined.clear();
                continue;
            } else {
                if (!flush.isEmpty()) {
                    flush.sort(Comparator.comparing(Card::get_rank));
                    wins.set(j, new Poker_Hands(winners.FLUSH, flush.get(flush.size() - 1), player.get(j)));
                    combined.clear();
                    continue;
                }
                ArrayList<Card> straight = check_straight(combined);
                if (!straight.isEmpty()) {
                    wins.set(j, new Poker_Hands(winners.STRAIGHT, straight.get(0), player.get(j)));
                    combined.clear();
                    continue;
                } else {
                    combined.clear();
                    continue;
                }

            }
        }
        ArrayList<Poker_Hands> firstcheck = new ArrayList<>();
        wins.sort(Comparator.comparing(Poker_Hands::getResult));
        firstcheck.add(wins.get(wins.size() - 1));
        wins.remove(wins.size() - 1);
        for (int i = wins.size() - 1; i > -1; i--) {
            if (wins.get(i).result == firstcheck.get(0).result) {
                firstcheck.add(wins.get(i));
            } else {
                break;
            }
        }
        if (firstcheck.size() == 1) {
            return firstcheck;
        } else {
            firstcheck.sort(Comparator.comparing(Poker_Hands::gethighrank));
            ArrayList<Poker_Hands> secondcheck = new ArrayList<>();
            secondcheck.add(firstcheck.get(firstcheck.size() - 1));
            firstcheck.remove(firstcheck.size() - 1);
            for (int i = firstcheck.size() - 1; i > -1; i--) {
                if (firstcheck.get(i).highcard == secondcheck.get(0).highcard) {
                    secondcheck.add(firstcheck.get(i));
                } else {
                    break;
                }
            }
            if (firstcheck.size() == 1 || firstcheck.get(0).secondhigh == null) {
                return firstcheck;
            } else {
                secondcheck.sort(Comparator.comparing(Poker_Hands::getsecondrank));
                ArrayList<Poker_Hands> thirdcheck = new ArrayList<>();
                thirdcheck.add(secondcheck.get(secondcheck.size() - 1));
                secondcheck.remove(secondcheck.size() - 1);
                for (int i = secondcheck.size() - 1; i > -1; i--) {
                    if (secondcheck.get(i).secondhigh == thirdcheck.get(0).secondhigh) {
                        thirdcheck.add(secondcheck.get(i));
                    } else {
                        break;
                    }
                }
                return secondcheck;
            }
        }
    }

    /**
     * Checks other possible hands
     * 
     * @param combined List of combined cards
     * @param j        is a player representation?
     * @param player   List of player integers
     * @return the list of poker hands
     */
    private Poker_Hands check_other(ArrayList<Card> combined, int j, ArrayList<Integer> player) {
        ArrayList<Card> pairOne = new ArrayList<>();
        ArrayList<Card> pairTwo = new ArrayList<>();
        ArrayList<Card> pairThree = new ArrayList<>();
        ArrayList<Card> compair = new ArrayList<>();

        // Loop through the combined list to extract pairs.
        // We assume combined is sorted by rank.
        for (int i = 0; i < combined.size() - 1; i++) {
            if (combined.get(i).get_rank().equals(combined.get(i + 1).get_rank())) {
                if (pairOne.isEmpty() || pairOne.get(0).get_rank().equals(combined.get(i).get_rank())) {
                    pairOne.add(combined.get(i));
                    pairOne.add(combined.get(i + 1));
                    i++; // skip the next element since it's part of the pair
                } else if (pairTwo.isEmpty() || pairTwo.get(0).get_rank().equals(combined.get(i).get_rank())) {
                    pairTwo.add(combined.get(i));
                    pairTwo.add(combined.get(i + 1));
                    i++;
                } else if (pairThree.isEmpty() || pairThree.get(0).get_rank().equals(combined.get(i).get_rank())) {
                    pairThree.add(combined.get(i));
                    pairThree.add(combined.get(i + 1));
                    i++;
                }
            }
        }

        // If no pair is found, return a HIGH hand using the two highest cards.
        if (pairOne.isEmpty()) {
            int size = combined.size();
            return new Poker_Hands(Poker_Hands.winners.HIGH, combined.get(size - 1), combined.get(size - 2),
                    player.get(j));
        }

        // Check for three-of-a-kind (or potential full house)
        int maxPairSize = Math.max(Math.max(pairOne.size(), pairTwo.size()), pairThree.size());
        if (maxPairSize == 3) {
            // Only one pair exists? Then it is a three-of-a-kind.
            if (pairTwo.isEmpty()) {
                return new Poker_Hands(Poker_Hands.winners.THREEKIND, pairOne.get(0), combined.get(combined.size() - 1),
                        player.get(j));
            } else if (pairThree.isEmpty()) {
                // Full house: one three-of-a-kind plus a pair.
                if (pairOne.size() == 3) {
                    return new Poker_Hands(Poker_Hands.winners.FULLHOUSE, pairOne.get(0), pairTwo.get(0),
                            player.get(j));
                } else {
                    return new Poker_Hands(Poker_Hands.winners.FULLHOUSE, pairTwo.get(0), pairOne.get(0),
                            player.get(j));
                }
            } else {
                // If three pairs exist, collect one card from each for tie-breaking.
                compair.add(pairOne.get(0));
                compair.add(pairTwo.get(0));
                compair.add(pairThree.get(0));
                compair.sort(Comparator.comparing(Card::get_rank));
                if (compair.size() >= 3) {
                    return new Poker_Hands(Poker_Hands.winners.TWOPAIR, compair.get(2), compair.get(1), player.get(j));
                } else {
                    return new Poker_Hands(Poker_Hands.winners.ONEPAIR, compair.get(0), compair.get(0), player.get(j));
                }
            }
        }

        // If only one pair exists, return ONEPAIR.
        if (pairTwo.isEmpty()) {
            return new Poker_Hands(Poker_Hands.winners.ONEPAIR, pairOne.get(0), combined.get(combined.size() - 1),
                    player.get(j));
        } else if (pairThree.isEmpty()) {
            compair.add(pairOne.get(0));
            compair.add(pairTwo.get(0));
            compair.sort(Comparator.comparing(Card::get_rank));
            return new Poker_Hands(Poker_Hands.winners.TWOPAIR, compair.get(1), compair.get(0), player.get(j));
        } else {
            // When three distinct pairs exist, ensure compair has at least three elements.
            compair.add(pairOne.get(0));
            compair.add(pairTwo.get(0));
            compair.add(pairThree.get(0));
            compair.sort(Comparator.comparing(Card::get_rank));
            if (compair.size() >= 3) {
                return new Poker_Hands(Poker_Hands.winners.TWOPAIR, compair.get(2), compair.get(1), player.get(j));
            } else {
                return new Poker_Hands(Poker_Hands.winners.ONEPAIR, compair.get(0), compair.get(0), player.get(j));
            }
        }
    }

    /**
     * Checks for a flush
     * 
     * @param hand The Player Hand
     * @return list of cards
     */
    private ArrayList<Card> check_flush(ArrayList<Card> hand) {
        ArrayList<Card> cards = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            cards.add(hand.get(i));
        }
        ArrayList<Card> flusher = new ArrayList<>();
        cards.sort(Comparator.comparing(Card::get_suit));
        Suit suit = cards.get(3).get_suit();
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).get_suit() == suit) {
                flusher.add(cards.get(i));
            } else {
                cards.remove(i);
                i--;
            }
        }
        if (flusher.size() < 5) {
            flusher.clear();
        } else {
            flusher.sort(Comparator.comparing(Card::get_rank));
        }
        return flusher;
    }

    /**
     * Checks for straight
     * 
     * @param combined List of cards
     * @return list of cards
     */
    private ArrayList<Card> check_straight(ArrayList<Card> combined) {
        ArrayList<Card> straighter = new ArrayList<>();
        combined.sort(Comparator.comparing(Card::get_rank));
        Collections.reverse(combined); // high to low, so as to get the best straight possible
        int consecutive = 0;
        for (int i = 0; i < combined.size() - 1; i++) {
            if (combined.get(i).get_rank().ordinal() - 1 == combined.get(i + 1).get_rank().ordinal()) {
                if (consecutive == 0) {
                    consecutive += 2;
                    straighter.add(combined.get(i));
                    straighter.add(combined.get(i + 1));
                } else {
                    consecutive++;
                    straighter.add(combined.get(i + 1));
                }
                if (consecutive == 5) {
                    return straighter;
                }
            } else {
                straighter.clear();
                consecutive = 0;
                if (i == 2) {
                    return straighter;
                }
            }
        }
        // unreachable?
        straighter.clear();
        return straighter;
    }
}
