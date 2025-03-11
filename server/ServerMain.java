import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import shared.Player;

public class ServerMain  {
	private static ExecutorService gamepool = Executors.newFixedThreadPool(100);//arbitrary number, can be changed as needed
	private static HashMap<Integer, ArrayList<Player>> matching_games = new HashMap<>(); //needs to be a map to maintain unique game IDs, ArrayList contains a list of the players currently waiting for that game to start
	public static final int maxplayers = 2;//also arbitrary
	private static int id = 1;//keeps track of the current unique id that will be assigned to the next game


	public static void main(String[] args) {
		System.out.print("Starting server...\n");
		ServerConnector x = new ServerConnector();
		Thread thread = new Thread(x);
		thread.start();
	}


	public static HashMap<Integer, ArrayList<Player>> getGames() {
		return ServerMain.matching_games;
	}
	//returning  -1 means the room was full, otherwise it got added successfully
	public static int waitforGame(int key, Player player) {
		if(matching_games.get(key).size() < maxplayers) {
			matching_games.get(key).add(player);
			System.out.println("A player has been added to Game " + key + "! Currently " + matching_games.get(key).size() + "/" + maxplayers);
			if(matching_games.get(key).size() == maxplayers) {//start the game cause there's max players
				gamepool.submit(new ServerTable(matching_games.get(key)));
			}
			return 0;
		}
		else {
			return -1;
		}
	}
	public static synchronized int addNewGame() {
		matching_games.put(id, new ArrayList<Player>());
		System.out.println("A player has created Game " + id + "!");
		id++; //if this was IRL this would be a potential risk due to integer overflows
		return id-1;
	}
}