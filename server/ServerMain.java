import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain  {
	private static ExecutorService gamepool = Executors.newFixedThreadPool(100);//arbitrary number, can be changed as needed
	private static HashMap<Integer, ArrayList<Socket>> active_games = new HashMap<Integer, ArrayList<Socket>>(); //needs to be a map to maintain unique game IDs, ArrayList contains a list of the sockets currently waiting for that game to start
	public static final int maxplayers = 2;//also arbitrary
	private static int id = 1;//keeps track of the current unique id that will be assigned to the next game
	public static void main(String[] args) {
		ServerConnector x = new ServerConnector();
		Thread thread = new Thread(x);
		thread.start();
	}
	public static HashMap<Integer, ArrayList<Socket>> getGames() {
		return ServerMain.active_games;
	}
	//returning  -1 means the room was full, otherwise it got added successfully
	public static int waitforGame(int key, Socket socket) {
		if(active_games.get(key).size() < maxplayers) {
			active_games.get(key).add(socket);
			if(active_games.get(key).size() == maxplayers) {//start the game cause there's max players
				gamepool.submit(new ServerTable(active_games.get(key)));
			}
			return 0;
		}
		else {
			return -1;
		}
	}
	public static synchronized int addNewGame() {
		active_games.put(id, new ArrayList<Socket>());
		id++; //if this was IRL this would be a potential risk due to integer overflows
		return id;
	}
}