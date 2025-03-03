import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain  {
	private static ExecutorService gamepool = Executors.newFixedThreadPool(100);//arbitrary number, can be changed as needed
	private static HashMap<Integer, ArrayList<Socket>> active_games = new HashMap<Integer, ArrayList<Socket>>(); //needs to be a map to maintain unique game IDs, ArrayList contains a list of the sockets currently waiting for that game to start
	public static final int maxplayers = 2;//also arbitrary
	private static int id = 0;//keeps track of the current unique id that will be assigned to the next game
	public static void main(String[] args) {
		ServerConnector x = new ServerConnector();
		Thread thread = new Thread(x);
		thread.start();
	}
	public static HashMap<Integer, ArrayList<Socket>> getGames() {
		return ServerMain.active_games;
	}
	public static void waitforGame(int key, Socket socket) {
		try {
			active_games.get(key).add(socket);
			active_games.get(key).wait();
		} catch (InterruptedException e) {
			System.out.println("I woke up!"); //for testing purposes
		}
	}
	public static synchronized int addNewGame() {
		gamepool.submit(new ServerTable(id));
		active_games.put(id, new ArrayList<Socket>());
		id++; //if this was IRL this would be a potential risk due to integer overflows
		return id-1;
	}
}