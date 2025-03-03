//manages games in progress and starts/stops them as needed
//TODO: watch the active_games ArrayList corresponding to the key, notify everyone once the size of the list reaches maxPlayers, start the game
public class ServerTable implements Runnable {
	int key;
	public ServerTable(int newkey) {
		this.key = newkey;
	}
	public void run() {
		
	}
}