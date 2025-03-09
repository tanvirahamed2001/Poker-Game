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

import java.net.Socket;
import java.util.ArrayList;
import games.Player;
import games.card_based.Card;
import games.card_based.Deck;

public class ServerTable implements Runnable {
	ArrayList<Socket> playerlist;
	public ServerTable(ArrayList<Socket> plist) {
		this.playerlist = plist;
	}
	public void run() {
		boolean activePlayers[] = new boolean[playerlist.size()];
		int funds[] = new int[playerlist.size()];
		Deck deck = new Deck();
		Player players[] = new Player[playerlist.size()];
		for(int i = 0; i < activePlayers.length; i++) {
			activePlayers[i] = true; //all players are active at the start of the game
			players[i].deposit_funds(1000); //arbitrary number
		}
		
	}
}