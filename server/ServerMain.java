import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import shared.Player;

public class ServerMain {
    // A thread pool for running game tables
    private static ExecutorService gamepool = Executors.newFixedThreadPool(100);
    // Map to maintain unique game IDs and their associated waiting players
    private static HashMap<Integer, ArrayList<Player>> matching_games = new HashMap<>();
    public static final int maxplayers = 2; // Maximum players per game (arbitrary)
    private static int id = 1; // Unique ID generator for new games

    public static void main(String[] args) {
        // Determine role based on command-line argument ("backup" means backup mode)
        boolean isPrimary = true; // default to primary mode
        if (args.length > 0 && args[0].equalsIgnoreCase("backup")) {
            isPrimary = false;
        }
        
        // Initialize the replication manager
        ReplicationManager replicationManager = ReplicationManager.getInstance(isPrimary);
        
        System.out.println("Starting server in " + (isPrimary ? "PRIMARY" : "BACKUP") + " mode...");
        
        // Only start accepting client connections if we are primary
        if (isPrimary) {
            ServerConnector connector = new ServerConnector();
            Thread connectorThread = new Thread(connector);
            connectorThread.start();
        } else {
            System.out.println("Running in backup mode; client listener not started.");
        }
    }
    
    // Returns the games waiting to start.
    public static HashMap<Integer, ArrayList<Player>> getGames() {
        return matching_games;
    }
    
    // Adds a player to a game. Returns 0 if successful, or -1 if the room is full.
    public static int waitforGame(int key, Player player) {
        if (matching_games.get(key).size() < maxplayers) {
            matching_games.get(key).add(player);
            System.out.println("A player has been added to Game " + key + "! Currently " 
                               + matching_games.get(key).size() + "/" + maxplayers);
            if (matching_games.get(key).size() == maxplayers) { 
                // If maximum players have joined, start the game.
                gamepool.submit(new ServerTable(matching_games.get(key)));
            }
            return 0;
        } else {
            return -1;
        }
    }
    
    // Creates a new game and returns its unique ID.
    public static synchronized int addNewGame() {
        matching_games.put(id, new ArrayList<Player>());
        System.out.println("A player has created Game " + id + "!");
        id++; // increment for the next game
        return id - 1;
    }
}