//This is a game in progress, need to run the game
/*The poker logic should look something like this: give token to first player -> set that player as last active player ->
set all players as active -> start loop on playerlist (ring loop, once at the end should loop back to the beginning
if player is active: give token -> listen for commands (until timeout)
	if command is check (do nothing, only available if nobody has bet this turn, so we need to either prevent the player from checking or throw an error if they do when someone has already bet): 
		if player is last active player: move to next turn
		else: continue loop
	else if command is open (initial bet) or raise (increase bet): 
		notify all players of open/raise
		add bet to pot
		set last active player to current player
		continue loop
	else if command is fold: set current player as inactive
	else if command is call: (match last bet, similar logic to check, similar errors to check)
		add bet to pot
		continue loop
	else if command is skip: (should only be available to the last active player IF they raised or opened, could be rolled into call/check theoretically)
		move to next turn
		

after all the turns are done, calculate who has the best hand and give them the pot, players who are out of cash are kicked, those who aren't restart play
*/

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import shared.Player;
import shared.PlayerConnection;
import shared.card_based.*;
import shared.card_based.Card.*;
import shared.card_based.Poker_Hands.winners;

public class ServerTable implements Runnable {
    private int gameId;
    private static Map<Integer, ServerTable> gameInstances = new HashMap<>();
    private ArrayList<PlayerConnection> connections;
    private ArrayList<Player> players;
    private ArrayList<Card> tablecards;
    private int currentbet, lastActive, currentplayer, pot, currentTurn;
    private ArrayList<Integer> currentBets;
    private boolean activePlayers[];
    GameState currentState;
    
    public ServerTable(int gameId, ArrayList<PlayerConnection> connections) {
        this.gameId = gameId;
        this.connections = connections;
        this.players = new ArrayList<>();
        for (PlayerConnection pc : connections) {
            players.add(pc.getPlayer());
            try {
                pc.getSocket().setSoTimeout(600000);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        gameInstances.put(gameId, this);
    }

    public static ServerTable getInstance(int gameId) {
        return gameInstances.get(gameId);
    }

    public synchronized void updateState(GameState state) {
        this.players = state.getPlayers();
        this.pot = state.getPot();
        this.currentTurn = state.getCurrentTurn();
        this.tablecards = state.getTableCards();
        System.out.println("Game " + gameId + " state updated from snapshot.");
    }

    @Override
    public void run() {
        System.out.println("Game Started!");
        sendAllPlayers("The Game has Begun!\n");
    
        int numPlayers = connections.size();
        activePlayers = new boolean[numPlayers];
        pot = 0;
        currentTurn = 1;
        currentbet = 0;
        Deck deck = new Deck();
        currentplayer = 0;
        currentBets = new ArrayList<>();
        tablecards = new ArrayList<>();
    
        for (int i = 0; i < numPlayers; i++) {
            activePlayers[i] = true;
            players.get(i).new_card(deck.deal_card());
            players.get(i).new_card(deck.deal_card());
            sendPlayer("Your cards are: " + players.get(i).show_all_cards() + "\n", i);
            currentBets.add(0);
        }
        lastActive = currentplayer;
    
        while (currentTurn <= 5) {
            replicateGameState();
            int bettingStart = currentplayer;
            boolean roundCompleted = false;
            do {
                if (activePlayers[currentplayer]) {
                    getPlayerInput();
                }
                incrementPlayer();
                if (currentplayer == bettingStart && currentplayer == lastActive) {
                    roundCompleted = true;
                }
            } while (!roundCompleted);
    
            sendAllPlayers("Betting round over. Moving to next turn.\n");
            currentTurn++;
            clearBets();
    
            switch (currentTurn) {
                case 2: // Flop.
                    tablecards.add(deck.deal_card());
                    tablecards.add(deck.deal_card());
                    tablecards.add(deck.deal_card());
                    sendAllPlayers("Turn 2: The Flop\nCards: " + tablecards.toString() + "\n");
                    break;
                case 3: // Turn.
                    tablecards.add(deck.deal_card());
                    sendAllPlayers("Turn 3: The Turn\nCard: " + tablecards.get(3).toString() + "\n");
                    break;
                case 4: // River.
                    tablecards.add(deck.deal_card());
                    sendAllPlayers("Turn 4: The River\nCard: " + tablecards.get(4).toString() + "\n");
                    break;
                case 5: // Showdown.
                    ArrayList<Poker_Hands> winners = determine_winner();
                    if (winners.size() == 1) {
                        int winnum = winners.get(0).playernumber;
                        sendAllPlayers("Player " + winnum + " has won the round! They earn $" + pot + "!\n");
                        players.get(winnum).deposit_funds(pot);
                    } else {
                        sendAllPlayers("Players tied! Splitting pot.\n");
                        int share = pot / winners.size();
                        for (Poker_Hands ph : winners) {
                            players.get(ph.playernumber).deposit_funds(share);
                        }
                    }
                    for (int i = 0; i < players.size(); i++) {
                        players.get(i).clear_hand();
                        if (players.get(i).view_funds() > 0) {
                            activePlayers[i] = true;
                            players.get(i).new_card(deck.deal_card());
                            players.get(i).new_card(deck.deal_card());
                            sendPlayer("New hand: " + players.get(i).show_all_cards() + "\n", i);
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
                    sendAllPlayers("New hand started.\n");
                    break;
            }
            lastActive = currentplayer;
        }
    }
    
    private void clearBets() {
        currentbet = 0;
        for (int i = 0; i < currentBets.size(); i++) {
            currentBets.set(i, 0);
        }
    }

    private void getPlayerInput() {
        try {
            sendPlayer("token\n", currentplayer);
            // Cast the read object to String.
            String response = (String) connections.get(currentplayer).readMessage();
            if (response.equalsIgnoreCase("check")) {
                if (currentBets.get(currentplayer) >= currentbet) {
                    sendAllPlayers("Player " + currentplayer + " checks.\n");
                } else {
                    sendPlayer("You must call, fold, or raise!\n", currentplayer);
                    getPlayerInput();
                }
            } else if (response.toLowerCase().startsWith("bet")) {
                try {
                    int amount = Integer.parseInt(response.split(" ")[1]);
                    if (amount <= 0 || amount > players.get(currentplayer).view_funds()) {
                        sendPlayer("Invalid bet amount. Try again.\n", currentplayer);
                        getPlayerInput();
                    } else {
                        players.get(currentplayer).deposit_funds(-amount);
                        sendAllPlayers("Player " + currentplayer + " bets $" + amount + ".\n");
                        currentbet = amount;
                        pot += amount;
                        currentBets.set(currentplayer, amount);
                        lastActive = currentplayer;
                    }
                } catch (Exception e) {
                    sendPlayer("Error: Invalid bet command. Try again.\n", currentplayer);
                    getPlayerInput();
                }
            } else if (response.equalsIgnoreCase("fold")) {
                activePlayers[currentplayer] = false;
                sendAllPlayers("Player " + currentplayer + " folds.\n");
            } else if (response.equalsIgnoreCase("call")) {
                if (currentbet > players.get(currentplayer).view_funds()) {
                    sendPlayer("Insufficient funds to call. You must fold.\n", currentplayer);
                    activePlayers[currentplayer] = false;
                } else {
                    players.get(currentplayer).deposit_funds(-currentbet);
                    pot += currentbet;
                    currentBets.set(currentplayer, currentbet);
                    sendAllPlayers("Player " + currentplayer + " calls.\n");
                }
            } else if (response.equalsIgnoreCase("funds")) {
                sendPlayer("Your funds: $" + players.get(currentplayer).view_funds() + "\n", currentplayer);
                getPlayerInput();
            } else if (response.equalsIgnoreCase("cards")) {
                sendPlayer("Your cards: " + players.get(currentplayer).view_cards() + "\n" +
                           "Table cards: " + tablecards.toString() + "\n", currentplayer);
                getPlayerInput();
            } else {
                sendPlayer("Invalid command! Please try again.\n", currentplayer);
                getPlayerInput();
            }
        } catch (SocketTimeoutException e) {
            // Handle timeout if needed.
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void incrementPlayer() {
        currentplayer = (currentplayer + 1) % connections.size();
    }
    
    private void sendAllPlayers(String message) {
        for (PlayerConnection pc : connections) {
            try {
                pc.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void sendPlayer(String message, int index) {
        try {
            connections.get(index).sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void replicateGameState() {
        GameState currentState = new GameState(gameId, players, pot, currentTurn, tablecards);
        ReplicationManager.getInstance(true).sendStateUpdate(currentState);
    }
    
    // determine_winner(), check_other(), check_flush(), check_straight() remain unchanged.
    private ArrayList<Poker_Hands> determine_winner() {
        // ... existing logic remains unchanged ...
        ArrayList<Poker_Hands> wins = new ArrayList<>();
        ArrayList<ArrayList<Card>> hands = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            hands.add(players.get(i).show_all_cards());
        }
        ArrayList<Card> combined = new ArrayList<>();
        for (int j = 0; j < hands.size(); j++) {
            for (int i = 0; i < tablecards.size(); i++) {
                combined.add(tablecards.get(i));
            }
            combined.add(hands.get(j).get(0));
            combined.add(hands.get(j).get(1));
            combined.sort(Comparator.comparing(Card::get_rank));
            ArrayList<Card> straight = check_straight(combined);
            if (!straight.isEmpty()) {
                if (!check_flush(straight).isEmpty()) {
                    wins.add(new Poker_Hands(winners.STRAIGHTFLUSH, straight.get(straight.size()-1), j));
                    combined.clear();
                    continue;
                }
            }
            wins.add(check_other(combined, j));
            if (wins.get(j).result == winners.FOURKIND || wins.get(j).result == winners.FULLHOUSE) {
                combined.clear();
                continue;
            } else {
                if (!straight.isEmpty()) {
                    wins.set(j, new Poker_Hands(winners.STRAIGHT, straight.get(0), j));
                    combined.clear();
                    continue;
                } else if (!check_flush(combined).isEmpty()) {
                    ArrayList<Card> flusher = check_flush(combined);
                    wins.set(j, new Poker_Hands(winners.FLUSH, flusher.get(flusher.size()-1), j));
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
        firstcheck.add(wins.get(wins.size()-1));
        wins.remove(wins.size()-1);
        for (int i = wins.size()-1; i > -1; i--) {
            if (wins.get(i).result == firstcheck.get(0).result) {
                firstcheck.add(wins.get(i));
            } else {
                break;
            }
        }
        if (wins.size() == 1) {
            return wins;
        } else {
            firstcheck.sort(Comparator.comparing(Poker_Hands::gethighrank));
            ArrayList<Poker_Hands> secondcheck = new ArrayList<>();
            secondcheck.add(firstcheck.get(firstcheck.size()-1));
            firstcheck.remove(firstcheck.size()-1);
            for (int i = firstcheck.size()-1; i > -1; i--) {
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
                thirdcheck.add(secondcheck.get(secondcheck.size()-1));
                secondcheck.remove(secondcheck.size()-1);
                for (int i = secondcheck.size()-1; i > -1; i--) {
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
    
    private Poker_Hands check_other(ArrayList<Card> combined, int j) {
        ArrayList<Card> pairone = new ArrayList<>(); // first pair
        ArrayList<Card> pairtwo = new ArrayList<>(); // second pair
        ArrayList<Card> pairthree = new ArrayList<>(); // third pair
        ArrayList<Card> compair = new ArrayList<>();
        
        // Loop through the combined list to extract pairs.
        for (int i = 0; i < combined.size(); i++) {
            if (i != combined.size()-1) {
                if (combined.get(i).get_rank() == combined.get(i+1).get_rank()) {
                    if (pairone.isEmpty()) {
                        pairone.add(combined.get(i));
                        pairone.add(combined.get(i+1));
                        combined.remove(i);
                        combined.remove(i);
                        i -= 2;
                    } else if (pairone.get(0).get_rank() == combined.get(i).get_rank()) {
                        combined.remove(i);
                        combined.remove(i);
                        return new Poker_Hands(winners.FOURKIND, pairone.get(0), combined.get(combined.size()-1), j);
                    } else if (pairtwo.isEmpty()) {
                        pairtwo.add(combined.get(i));
                        pairtwo.add(combined.get(i+1));
                        combined.remove(i);
                        combined.remove(i);
                        i -= 2;
                    } else if (pairtwo.get(0).get_rank() == combined.get(i).get_rank()) {
                        combined.remove(i);
                        combined.remove(i);
                        combined.add(pairone.get(0));
                        combined.sort(Comparator.comparing(Card::get_rank));
                        return new Poker_Hands(winners.FOURKIND, pairtwo.get(0), combined.get(combined.size()-1), j);
                    } else {
                        pairthree.add(combined.get(i));
                        pairthree.add(combined.get(i+1));
                        combined.remove(i);
                        combined.remove(i);
                        i -= 2;
                    }
                }
            }
            if (!pairone.isEmpty() && combined.get(i).get_rank() == pairone.get(0).get_rank()) {
                pairone.add(combined.get(i));
                combined.remove(i);
                i--;
            }
            if (!pairtwo.isEmpty() && combined.get(i).get_rank() == pairtwo.get(0).get_rank()) {
                pairtwo.add(combined.get(i));
                combined.remove(i);
                i--;
            }
            if (!pairthree.isEmpty() && combined.get(i).get_rank() == pairthree.get(0).get_rank()) {
                pairthree.add(combined.get(i));
                combined.remove(i);
                i--;
            }
        }
        
        // If no pair is found, return HIGH.
        if (pairone.isEmpty()) {
            return new Poker_Hands(winners.HIGH, combined.get(combined.size()-1), combined.get(combined.size()-2), j);
        }
        
        // Now, if the maximum size among the pairs is 3 (i.e. three of a kind)
        if (Math.max(Math.max(pairone.size(), pairtwo.size()), pairthree.size()) == 3) {
            if (pairtwo.isEmpty()) {
                return new Poker_Hands(winners.THREEKIND, pairone.get(0), combined.get(combined.size()-1), j);
            } else if (pairthree.isEmpty()) {
                if (pairone.size() == 3) {
                    return new Poker_Hands(winners.FULLHOUSE, pairone.get(0), pairtwo.get(0), j);
                } else {
                    return new Poker_Hands(winners.FULLHOUSE, pairtwo.get(0), pairone.get(0), j);
                }
            } else {
                // Fall into the tie-break: ensure compair gets all three highest cards.
                if (!pairone.isEmpty() && !pairtwo.isEmpty() && !pairthree.isEmpty()) {
                    compair.add(pairone.get(0));
                    compair.add(pairtwo.get(0));
                    compair.add(pairthree.get(0));
                    compair.sort(Comparator.comparing(Card::get_rank));
                    if (compair.size() >= 3) {
                        return new Poker_Hands(winners.TWOPAIR, compair.get(2), compair.get(1), j);
                    } else {
                        // Fallback if not enough elements are present.
                        return new Poker_Hands(winners.ONEPAIR, compair.get(0), compair.get(0), j);
                    }
                } else {
                    // Fallback to ONEPAIR if one of the pairs is empty.
                    return new Poker_Hands(winners.ONEPAIR, pairone.get(0), pairone.get(0), j);
                }
            }
        }
        
        if (pairtwo.isEmpty()) {
            return new Poker_Hands(winners.ONEPAIR, pairone.get(0), combined.get(combined.size()-1), j);
        } else if (pairthree.isEmpty()) {
            compair.add(pairone.get(0));
            compair.add(pairtwo.get(0));
            compair.sort(Comparator.comparing(Card::get_rank));
            return new Poker_Hands(winners.TWOPAIR, compair.get(1), compair.get(0), j);
        } else {
            // This is the problematic block.
            if (!pairone.isEmpty() && !pairtwo.isEmpty() && !pairthree.isEmpty()) {
                compair.add(pairone.get(0));
                compair.add(pairtwo.get(0));
                compair.add(pairthree.get(0));
                compair.sort(Comparator.comparing(Card::get_rank));
                if (compair.size() >= 3) {
                    return new Poker_Hands(winners.TWOPAIR, compair.get(2), compair.get(1), j);
                } else {
                    // Fallback if compair doesn't have enough elements.
                    return new Poker_Hands(winners.ONEPAIR, compair.get(0), compair.get(0), j);
                }
            } else {
                return new Poker_Hands(winners.ONEPAIR, pairone.get(0), pairone.get(0), j);
            }
        }
    }
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

    private ArrayList<Card> check_straight(ArrayList<Card> combined) {
        ArrayList<Card> straighter = new ArrayList<>();
        boolean consecutive = false;
        for (Card.Rank rank : Card.Rank.values()) {
            for (int i = 0; i < combined.size(); i++) {
                if (combined.get(i).get_rank() == rank) {
                    if (i < combined.size() - 1) {
                        if (combined.get(i).get_rank() == combined.get(i+1).get_rank()) {
                            if (i < combined.size() - 2) {
                                if (combined.get(i).get_suit() == combined.get(i+2).get_suit()) {
                                    straighter.add(combined.get(i));
                                    straighter.remove(i+1);
                                } else {
                                    straighter.add(combined.get(i+1));
                                    straighter.remove(i);
                                }
                            }
                            if (!straighter.isEmpty()) {
                                if (combined.get(i).get_suit() == straighter.get(0).get_suit()) {
                                    straighter.add(combined.get(i));
                                    straighter.remove(i+1);
                                } else {
                                    straighter.add(combined.get(i+1));
                                    straighter.remove(i);
                                }
                            } else {
                                straighter.add(combined.get(i));
                                straighter.remove(i+1);
                            }
                        }
                    }
                    consecutive = true;
                    break;
                }
            }
            if (!consecutive) {
                straighter.clear();
            } else {
                if (straighter.size() == 5) {
                    return straighter;
                }
                consecutive = false;
            }
        }
        return straighter;
    }
}
