import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import shared.Player;
import shared.PlayerConnection;
import shared.communication_objects.*;

public class ServerMain {
    private static ExecutorService gamepool = Executors.newFixedThreadPool(100);
    private static HashMap<Integer, ArrayList<PlayerConnection>> matching_games = new HashMap<>();
    public static final int maxplayers = 2;
    private static int id = 1;
    
    public static HashMap<Integer, ArrayList<PlayerConnection>> getGames() {
        return matching_games;
    }
    
    // NEW: Update game list from replicated game states.
    public static synchronized void updateGames(Map<Integer, GameState> replicatedGames) {
        for (Integer gameId : replicatedGames.keySet()) {
            if (!matching_games.containsKey(gameId)) {
                matching_games.put(gameId, new ArrayList<>());
                System.out.println("Restored Game " + gameId + " from replication state.");
            }
        }
    }
    
    public static int waitforGame(int key, PlayerConnection connection, boolean reconnect) {
        ArrayList<PlayerConnection> gameConnections = matching_games.get(key);
        if (gameConnections == null) {
            gameConnections = new ArrayList<>();
            matching_games.put(key, gameConnections);
        }
        if (gameConnections.size() < maxplayers) {
            gameConnections.add(connection);
            System.out.println("A player has been added to Game " + key + "! Currently " + gameConnections.size() + "/" + maxplayers);
            if (gameConnections.size() == maxplayers) { 
                if(reconnect) {
                    ServerTable oldTable = ServerTable.getInstance(key);
                    oldTable.reconnectPlayers(gameConnections);
                    Message msg = new Message("Rejoined table " + oldTable.getTableID()+ " waiting for game start...");
                    connection.sendCommand(Command.Type.MESSAGE, msg);
                    gamepool.submit(oldTable);
                } else {
                    gamepool.submit(new ServerTable(key, gameConnections));
                }
            }
            return 0;
        } else {
            return -1;
        }
    }
    
    public static synchronized int addNewGame() {
        matching_games.put(id, new ArrayList<>());
        System.out.println("A player has created Game " + id + "!");
        id++;
        return id - 1;
    }
    
    public static void main(String[] args) {
        boolean isPrimary = true;
        if (args.length > 0 && args[0].equalsIgnoreCase("backup")) {
            isPrimary = false;
        }
        // When running as primary, use "serverId" (e.g., -DserverId=4).
        // When running as backup, use "backupId" (e.g., -DbackupId=1).
        ReplicationManager replicationManager = ReplicationManager.getInstance(isPrimary);
        System.out.println("Starting server in " + (isPrimary ? "PRIMARY" : "BACKUP") + " mode...");
        if (isPrimary) {
            ServerConnector connector = new ServerConnector();
            Thread connectorThread = new Thread(connector);
            connectorThread.start();
        } else {
            System.out.println("Running in backup mode; client listener not started.");
        }
    }
}
