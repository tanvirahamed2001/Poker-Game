import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import shared.Player;
import shared.PlayerConnection;

public class ServerMain {
    // A thread pool for running game tables.
    private static ExecutorService gamepool = Executors.newFixedThreadPool(100);
    // Map to maintain unique game IDs and their associated waiting players (connections).
    private static HashMap<Integer, ArrayList<PlayerConnection>> matching_games = new HashMap<>();
    public static final int maxplayers = 2; // Maximum players per game.
    private static int id = 1; // Unique ID generator for new games.

    public static void main(String[] args) {
        // Determine role based on command-line argument ("backup" means backup mode).
        boolean isPrimary = true; // Default to primary mode.
        if (args.length > 0 && args[0].equalsIgnoreCase("backup")) {
            isPrimary = false;
        }
        
        // Initialize the replication manager.
        ReplicationManager replicationManager = ReplicationManager.getInstance(isPrimary);
        
        System.out.println("Starting server in " + (isPrimary ? "PRIMARY" : "BACKUP") + " mode...");
        
        // Only start accepting client connections if we are primary.
        if (isPrimary) {
            ServerConnector connector = new ServerConnector();
            Thread connectorThread = new Thread(connector);
            connectorThread.start();
        } else {
            System.out.println("Running in backup mode; client listener not started.");
        }
    }
    
    // Returns the mapping of game IDs to their waiting PlayerConnection objects.
    public static HashMap<Integer, ArrayList<PlayerConnection>> getGames() {
        return matching_games;
    }
    
    public static int waitforGame(int key, PlayerConnection connection) {
        ArrayList<PlayerConnection> gameConnections = matching_games.get(key);
        if (gameConnections == null) {
            gameConnections = new ArrayList<>();
            matching_games.put(key, gameConnections);
        }
        if (gameConnections.size() < maxplayers) {
            gameConnections.add(connection);
            System.out.println("A player has been added to Game " + key + "! Currently " 
                               + gameConnections.size() + "/" + maxplayers);
            if (gameConnections.size() == maxplayers) { 
                // If maximum players have joined, start the game.
                gamepool.submit(new ServerTable(key, gameConnections));
            }
            return 0;
        } else {
            return -1;
        }
    }
    // Creates a new game and returns its unique game ID.
    public static synchronized int addNewGame() {
        matching_games.put(id, new ArrayList<PlayerConnection>());
        System.out.println("A player has created Game " + id + "!");
        id++; // Increment for the next game.
        return id - 1;
    }
}
