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

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Comparator;
import java.math.*;

import shared.Player;
import shared.card_based.*;
import shared.card_based.Card.*;
import shared.card_based.Poker_Hands.winners;

public class ServerTable implements Runnable {
	ArrayList<Socket> psocket;
	ArrayList<BufferedWriter> outlist;
	ArrayList<BufferedReader> inlist;
	ArrayList<Card> tablecards;
	int currentbet, lastactive, currentplayer, pot, currentturn;
	ArrayList<Player> players;
	boolean activePlayers[];
	boolean finalturn;
	
	public ServerTable(ArrayList<Player> plist) {
		this.players = plist;
		outlist = new ArrayList<>();
		inlist = new ArrayList<>();
		psocket = new ArrayList<>();
		for(int i = 0; i < players.size(); i++) {
			psocket.add(players.get(i).socket);
			try {
				psocket.get(i).setSoTimeout(600000); //60 minute timeout
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(int i = 0; i < psocket.size(); i++) {
			try {
				outlist.add(new BufferedWriter(new OutputStreamWriter(psocket.get(i).getOutputStream())));
				inlist.add(new BufferedReader(new InputStreamReader(psocket.get(i).getInputStream())));
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	public void run() {
		System.out.println("Game Started!");
		sendAllPlayers("The Game has Begun!\n");
		finalturn = false;
		activePlayers = new boolean[psocket.size()];
		pot = 0;
		currentturn = 1;
		Deck deck = new Deck();
		lastactive = players.size()-1;
		currentplayer = 0;
		currentbet = 0;
		tablecards = new ArrayList<Card>();
		for(int i = 0; i < activePlayers.length; i++) {
			activePlayers[i] = true; //all players are active at the start of the game
			players.get(i).new_card(deck.deal_card());
			players.get(i).new_card(deck.deal_card());
			sendPlayer("Your cards are " + players.get(i).show_all_cards() + "\n", i);
		}
		
		//turns: 0: pre-betting, 1: flop, 2: turn, 3: river, 4: game over
		while(currentturn < 5) {
			if(lastactive == currentplayer) {
				finalturn = true;
			}
			if(activePlayers[currentplayer] == true) {
				getPlayerInput();
			}
			if(finalturn) {
				sendAllPlayers("All players are done, moving to next turn!\n");
				currentturn++;
				currentbet = 0;
				lastactive = currentplayer;
				incrementplayer();
				switch(currentturn) {
				case 2:
					//the flop
					tablecards.add(deck.deal_card());
					tablecards.add(deck.deal_card());
					tablecards.add(deck.deal_card());
					sendAllPlayers("Turn 2: The Flop\n Card 1: " + tablecards.get(0).toString() + "\nCard 2: " + tablecards.get(1).toString() + "\nCard 3: " + tablecards.get(2).toString() + "\n");
					break;
				case 3:	
					//the turn
					tablecards.add(deck.deal_card());
					sendAllPlayers("Turn 3: The Turn\n Card 4: " + tablecards.get(3).toString() + "\n");
					break;
				case 4:
					tablecards.add(deck.deal_card());
					sendAllPlayers("Turn 4: The River\n Card 5: " + tablecards.get(4).toString() + "\n");
					break;
				case 5:
					ArrayList<Poker_Hands> winners = determine_winner();
					if(winners.size() == 1) {
						int winnum = winners.get(0).playernumber;
						sendAllPlayers("Player " + winnum + "has won the round! They earn $" + pot + "!\n");
						players.get(winnum).deposit_funds(pot);
					}
					else {
						sendAllPlayers("Players ");
						for(int i = 0; i < winners.size(); i++) {
							sendAllPlayers(winners.get(i).playernumber + " ");
							if(i != winners.size()-1) {
								sendAllPlayers("and ");
							}
						}
						sendAllPlayers("Have tied! The pot will be split among the winners!\n");
						pot /= winners.size();
						for(int i = 0; i < winners.size(); i++) {
							players.get(winners.get(i).playernumber).deposit_funds(pot);
						}
					}
					for(int i = 0; i < players.size(); i++) {
						players.get(i).clear_hand();
						if(players.get(i).view_funds() != 0) {
							activePlayers[i] = true;
							players.get(i).new_card(deck.deal_card());
							players.get(i).new_card(deck.deal_card());
						}
						
					}
					pot = 0;
					currentturn = 1;
					incrementplayer();
					currentbet = 0;
					finalturn = false;
					lastactive = currentplayer;
					tablecards.clear();
					int active = 0;
					for(int i = 0; i < activePlayers.length; i++) {
						if(activePlayers[i] == true) {
							active++;
						}
					}
					if(active < 2) {
						sendAllPlayers("The Game has Ended! Player " + winners.get(0).playernumber + "Has won!\nGoodbye!\n");
						System.exit(0);
					}
					winners.clear();
					break;
				}
			}
		}
	}
	/**
     * Determines who the winner is based on their cards
     * @return an int representing which player number won the game
     */
	private ArrayList<Poker_Hands> determine_winner() {
		ArrayList<Poker_Hands> wins = new ArrayList<>();
		ArrayList<ArrayList<Card>> hands = new ArrayList<>();
		boolean isflush;
		for(int i = 0; i < players.size(); i++) {
			hands.add(players.get(i).show_all_cards());
		}
		ArrayList<Card> combined = new ArrayList<>();//combined hand of player and table cards
		for(int j = 0; j < hands.size(); j++) {//j is current player
			for(int i = 0; i < tablecards.size(); i++) {
				combined.add(tablecards.get(i));
			}
			combined.add(hands.get(j).get(0));
			combined.add(hands.get(j).get(1));//add the player and table hands together
			combined.sort(Comparator.comparing(Card::get_rank)); //sort by rank
			ArrayList<Card> straight = check_straight(combined);
			if(!straight.isEmpty()) {
				if(!check_flush(straight).isEmpty()) {
					wins.add(new Poker_Hands(winners.STRAIGHTFLUSH, straight.get(straight.size()-1), j));
					combined.clear();
					continue;
				}
			}
			wins.add(check_other(combined, j));
			if(wins.get(j).result == winners.FOURKIND || wins.get(j).result == winners.FULLHOUSE) {
				combined.clear();
				continue;
			}
			else {
				if(!straight.isEmpty()) {
					wins.set(j, new Poker_Hands(winners.STRAIGHT, straight.get(0), j));
					combined.clear();
					continue;
				}
				else if(!check_flush(combined).isEmpty()) {
					ArrayList<Card> flusher = check_flush(combined);
					wins.set(j, new Poker_Hands(winners.FLUSH, flusher.get(flusher.size()-1), j));
					combined.clear();
					continue;
				}
				else{
					combined.clear();
					continue;
				}
			}
		}
		ArrayList<Poker_Hands> firstcheck = new ArrayList<>();
		wins.sort(Comparator.comparing(Poker_Hands::getResult));
		firstcheck.add(wins.get(wins.size()-1));
		wins.remove(wins.size()-1);
		for(int i = wins.size()-1; i > -1; i--) {//check for ties
			if(wins.get(i).result == firstcheck.get(0).result) {
				firstcheck.add(wins.get(i));
			}
			else {
				break;
			}
		}
		if(wins.size() == 1) {
			return wins;
		}
		else {
			firstcheck.sort(Comparator.comparing(Poker_Hands::gethighrank));
			ArrayList<Poker_Hands> secondcheck = new ArrayList<>();
			secondcheck.add(firstcheck.get(firstcheck.size()-1));
			firstcheck.remove(firstcheck.size()-1);
			for(int i = firstcheck.size()-1; i > -1; i--) {//check for ties
				if(firstcheck.get(i).highcard == secondcheck.get(0).highcard) {
					secondcheck.add(firstcheck.get(i));
				}
				else {
					break;
				}
			}
			if(firstcheck.size() == 1 || firstcheck.get(0).secondhigh == null) {
				return firstcheck;
			}
			else {
				secondcheck.sort(Comparator.comparing(Poker_Hands::getsecondrank));;
				ArrayList<Poker_Hands> thirdcheck = new ArrayList<>();
				thirdcheck.add(secondcheck.get(secondcheck.size()-1));
				secondcheck.remove(secondcheck.size()-1);
				for(int i = secondcheck.size()-1; i > -1; i--) {
					if(secondcheck.get(i).secondhigh == thirdcheck.get(0).secondhigh) {
						thirdcheck.add(secondcheck.get(i));
					}
					else {
						break;
					}
				}
				return secondcheck;
			}
		}
	}
	/**
     * determine_winner helper functions, checks for Four/Three of a kind, One/Two Pair, Full Houses, and high card if it can't find anything
     * @return an int representing which player number won the game
     */
	private Poker_Hands check_other(ArrayList<Card> combined, int j) {
		ArrayList<Card> pairone = new ArrayList<>();//there is 7 cards in the deck, so there can be up to 3 pairs when checking
		ArrayList<Card> pairtwo = new ArrayList<>();
		ArrayList<Card> pairthree = new ArrayList<>();
		ArrayList<Card> compair = new ArrayList<>();
		for(int i = 0; i < combined.size(); i++) {
			if(i != combined.size()-1) {
				if(combined.get(i).get_rank() == combined.get(i+1).get_rank()) {
					if(pairone.isEmpty()) {
						pairone.add(combined.get(i));
						pairone.add(combined.get(i+1));
						combined.remove(i);
						combined.remove(i);
						i -= 2;
					}
					else if(pairone.get(0).get_rank() == combined.get(i).get_rank()) {
						combined.remove(i);
						combined.remove(i);
						return new Poker_Hands(winners.FOURKIND, pairone.get(0), combined.get(combined.size()-1), j);
					}
					else if(pairtwo.isEmpty()) {
						pairtwo.add(combined.get(i));
						pairtwo.add(combined.get(i+1));
						combined.remove(i);
						combined.remove(i);
						i -= 2;
					}
					else if(pairtwo.get(0).get_rank() == combined.get(i).get_rank()) {
						combined.remove(i);
						combined.remove(i);
						combined.add(pairone.get(0));
						combined.sort(Comparator.comparing(Card::get_rank));//add the pairone pair back to check if it's the spare high card
						return new Poker_Hands(winners.FOURKIND, pairtwo.get(0), combined.get(combined.size()-1), j);
					}
					else {
						pairthree.add(combined.get(i));
						pairthree.add(combined.get(i+1));
						combined.remove(i);
						combined.remove(i);
						i -= 2;
					}
				}
			}
			if(!pairone.isEmpty()) {
				if(combined.get(i).get_rank() == pairone.get(0).get_rank()) {
					pairone.add(combined.get(i));
					combined.remove(i);
					i--;
				}
			}
			if(!pairtwo.isEmpty()) {
				if(combined.get(i).get_rank() == pairtwo.get(0).get_rank()) {
					pairtwo.add(combined.get(i));
					combined.remove(i);
					i--;
				}
			}
			if(!pairthree.isEmpty()) {
				if(combined.get(i).get_rank() == pairthree.get(0).get_rank()) {
					pairthree.add(combined.get(i));
					combined.remove(i);
					i--;
				}
			}
		}
		if(pairone.isEmpty()) {//no pairs at all found, return high card
			return new Poker_Hands(winners.HIGH, combined.get(combined.size()-1), combined.get(combined.size()-2), j);
		}
		if(Math.max(Math.max(pairone.size(), pairtwo.size()), pairthree.size()) == 3) {//three of a kind, possible full house
			if(pairtwo.isEmpty()) {//no other pairs, three of a kind
				return new Poker_Hands(winners.THREEKIND, pairone.get(0), combined.get(combined.size()-1), j);
			} //implicitly if this if statement fails then pairtwo isn't empty
			else if(pairthree.isEmpty()) {//no other pair to compare with, full house
				if(pairone.size() == 3) {
					return new Poker_Hands(winners.FULLHOUSE, pairone.get(0), pairtwo.get(0), j);
				}
				else {
					return new Poker_Hands(winners.FULLHOUSE, pairtwo.get(0), pairone.get(0), j);
				}
			}//same as before, this implicitly means pairthree isn't empty
			else {
				if(pairone.size() == 3) {
					compair.add(pairtwo.get(0));
					compair.add(pairthree.get(0));
					compair.sort(Comparator.comparing(Card::get_rank));
					return new Poker_Hands(winners.FULLHOUSE, pairone.get(0), compair.get(1), j);
				}
				else if(pairtwo.size() == 3) {
					compair.add(pairone.get(0));
					compair.add(pairthree.get(0));
					compair.sort(Comparator.comparing(Card::get_rank));
					return new Poker_Hands(winners.FULLHOUSE, pairtwo.get(0), compair.get(1), j);
				}
				else {
					compair.add(pairone.get(0));
					compair.add(pairtwo.get(0));
					compair.sort(Comparator.comparing(Card::get_rank));
					return new Poker_Hands(winners.FULLHOUSE, pairthree.get(0), compair.get(1), j);
				}
			}
		}
		if(pairtwo.isEmpty()) {//one pair
			return new Poker_Hands(winners.ONEPAIR, pairone.get(0), combined.get(combined.size()-1), j);
		}
		else if(pairthree.isEmpty()) {
			compair.add(pairone.get(0));
			compair.add(pairtwo.get(0));
			compair.sort(Comparator.comparing(Card::get_rank));
			return new Poker_Hands(winners.TWOPAIR, compair.get(1), compair.get(0), j);
		}
		else {
			compair.add(pairone.get(0));
			compair.add(pairtwo.get(0));
			compair.add(pairthree.get(0));
			compair.sort(Comparator.comparing(Card::get_rank));
			return new Poker_Hands(winners.TWOPAIR, compair.get(2), compair.get(1), j);
		}
		
	}
	/**
     * determine_winner helper functions, checks for flushes
     * @return an ArrayList of cards, empty if a flush was not found, sorted and filled with the flush cards if it was found
     */
	private ArrayList<Card> check_flush(ArrayList<Card> hand) {
		ArrayList<Card> cards = new ArrayList<>();
		for(int i = 0; i < hand.size(); i++){//iirc lists are passed by reference so we create and copy a new list so as not to edit the parameter
			cards.add(hand.get(i));
		}
		ArrayList<Card> flusher = new ArrayList<>();
		cards.sort(Comparator.comparing(Card::get_suit)); //sort by suit instead
		Suit suit = cards.get(3).get_suit(); //I may be incorrect, but my logic is in any combination of 7 cards flushes where the cards are organized by suit, card 3 MUST be a part of the flush
		for(int i = 0; i < cards.size(); i++) {
			if(cards.get(i).get_suit() == suit) {
				flusher.add(cards.get(i));
			}
			else{
			cards.remove(i);
			i--;
			}
		}
		if(flusher.size() < 5){
			flusher.clear();
		}
		else{
			flusher.sort(Comparator.comparing(Card::get_rank));
		}
		return flusher;
	}
	/**
     * determine_winner helper functions, checks for straights
     * @return the list of cards that create the straight, in ascending order
     */
	private ArrayList<Card> check_straight(ArrayList<Card> combined) {
		ArrayList<Card> straighter = new ArrayList<>();//keeps track of consecutive cards
		boolean consecutive = false;
		for(Card.Rank rank : Rank.values()) {//check for a straight
			for(int i = 0; i < combined.size(); i++) {
				if(combined.get(i).get_rank() == rank) {
					if(i < combined.size() -1) {
						if(combined.get(i).get_rank() == combined.get(i+1).get_rank()) {
							//if you have 2 cards with the same rank and a card is already in the flusher, then prioritize the suit being built (to correctly build straight flushes) and ditch the other one
							if(i < combined.size() -2) {
								if(combined.get(i).get_suit() == combined.get(i+2).get_suit()) {
									straighter.add(combined.get(i));
									straighter.remove(i+1);
								}
								else {
									straighter.add(combined.get(i+1));
									straighter.remove(i);
								}
							}
							if(!straighter.isEmpty()) {
								if(combined.get(i).get_suit() == straighter.get(0).get_suit()) {
									straighter.add(combined.get(i));
									straighter.remove(i+1);
								}
								else {
									straighter.add(combined.get(i+1));
									straighter.remove(i);
								}
							}
							else {
								straighter.add(combined.get(i));
								straighter.remove(i+1);
							}
						}
					}
					consecutive = true;
					break;
				}
			}
			if(!consecutive) {
				straighter.clear();
			}
			else {
				if(straighter.size() == 5) {
						return straighter;
				}
				consecutive = false;
			}
		}
		return straighter;
		
	}
	/**
     * get player input, will also update the game state depending on their actions
     */
	private void getPlayerInput() {
		try {
			sendPlayer("token\n", currentplayer);
			String response = inlist.get(currentplayer).readLine();
			if(response.equalsIgnoreCase("Check")) {
				if(currentbet == 0) {
					sendAllPlayers("Player " + currentplayer + " has checked!\n");
					incrementplayer();
				}
				else {
					sendPlayer("Someone else has opened! You must call, fold, or raise!\n", currentplayer);
				}
			}
			else if(response.contains("bet") || response.contains("Bet")) {
				try {
					int amount = Integer.parseInt(response.split(" ")[1]);
					if(amount <= 0) {
						throw new NumberFormatException();
					}
					else if(amount > players.get(currentplayer).view_funds()) {
						sendPlayer("You don't have the funds to do that! Please Try Again.\n", currentplayer);
						getPlayerInput();
					}
					else {
						players.get(currentplayer).deposit_funds(-amount);
						sendAllPlayers("Player " + currentplayer + " has bet $" + amount + "!\n" );
						lastactive = currentplayer;
						finalturn = false;
						currentbet = amount;
						pot += amount;
						incrementplayer();
					}
				}
				catch(ArrayIndexOutOfBoundsException e) {
					sendPlayer("Error: no bet amount entered! Please Try Again.\n", currentplayer);
					getPlayerInput();
				}
				catch(NumberFormatException e) {
					sendPlayer("Error: Please enter a valid number for a bet! Please Try Again.\n", currentplayer);
					getPlayerInput();
				}
			}
			else if(response.equalsIgnoreCase("fold")) {
				activePlayers[currentplayer] = false;
				sendAllPlayers("Player " + currentplayer + " has folded!\n");
				incrementplayer();
			}
			else if(response.equalsIgnoreCase("call")) {
				players.get(currentplayer).deposit_funds(-currentbet);
				pot += currentbet;
				sendAllPlayers("Player " + currentplayer + " has called!\n");
				incrementplayer();
			}
			else if(response.equalsIgnoreCase("funds")) {
				sendPlayer("You currently have $" + players.get(currentplayer).view_funds() + "\n", currentplayer);
				getPlayerInput();
			}
			else if(response.equalsIgnoreCase("cards")) {
				sendPlayer("Your cards are: " + players.get(currentplayer).view_cards() + "\nThe cards on the table are: ", currentplayer);
				for(int i = 0; i < tablecards.size(); i++) {
					sendPlayer(tablecards.get(i).toString() + " ", i);
				}
				sendPlayer("\n", currentplayer);
				getPlayerInput();
			}
			else {
				sendPlayer("Invalid command! Please Try Again!\n", currentplayer);
				getPlayerInput();
			}
		}catch(SocketTimeoutException e) {//player timed out
			
		} 
		catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
	/**
	 * increments the current player, 'flips' it back to 0 when needed
	 */
	private void incrementplayer() {
		currentplayer++;
		if(currentplayer == psocket.size()) {
			currentplayer = 0;
		}
	}
	/**
	 * tries to send a message to all players that exist
	 */
	private void sendAllPlayers(String string) {
		for(int i = 0; i < psocket.size(); i++) {
			try {
				outlist.get(i).write(string);
				outlist.get(i).flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	private void sendPlayer(String string, int playernumber) {
		try {
			outlist.get(playernumber).write(string);
			outlist.get(playernumber).flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}