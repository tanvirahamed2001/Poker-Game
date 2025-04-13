import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import shared.communication_objects.*;

/**
 * The ServerMain class serves as the central entry point for the multiplayer
 * server system.
 * It handles game creation, player matchmaking, reconnection logic, and
 * delegates execution
 * of active game tables using a thread pool. It also initializes replication
 * behavior
 * depending on whether the server is launched in primary or backup mode.
 */
public class ServerMain {

    /** ID counter for assigning new game IDs */
    private static int id = 1;

    /** Maximum number of players per game */
    public static final int maxplayers = 2;

    /** Thread pool used to run active game sessions */
    private static ExecutorService gamepool = Executors.newFixedThreadPool(100);

    /** Maps game IDs to the list of connected {@link PlayerConnection}s */
    private static HashMap<Integer, ArrayList<PlayerConnection>> matching_games = new HashMap<>();

    /**
     * Returns the current map of active or forming games.
     *
     * @return a mapping from game ID to player connections
     */
    public static HashMap<Integer, ArrayList<PlayerConnection>> getGames() {
        return matching_games;
    }

    /**
     * Updates the server’s list of games with data restored from replication state.
     * This is typically called when a backup is promoted to primary.
     *
     * @param replicatedGames a map of replicated game states keyed by game ID
     */
    public static synchronized void updateGames(Map<Integer, GameState> replicatedGames) {
        for (Integer gameId : replicatedGames.keySet()) {
            if (!matching_games.containsKey(gameId)) {
                matching_games.put(gameId, new ArrayList<>());
                System.out.println("Restored Game #" + gameId);
            }
        }
    }

    /**
     * Attempts to match a player to an existing game, or reconnects them to a
     * resumed session.
     * Starts the game once the required number of players is met.
     *
     * @param key        the game ID to join
     * @param connection the player's connection object
     * @param reconnect  whether the player is reconnecting to a running game
     * @return 0 if the player was added successfully, or -1 if the game is full
     */
    public static int waitforGame(int key, PlayerConnection connection, boolean reconnect) {
        ArrayList<PlayerConnection> gameConnections = matching_games.get(key);
        if (gameConnections == null) {
            gameConnections = new ArrayList<>();
            matching_games.put(key, gameConnections);
        }

        if (gameConnections.size() < maxplayers) {
            gameConnections.add(connection);
            System.out.println("A player has been added to table #" + key + "! Currently " + gameConnections.size() + "/"
                    + maxplayers + "...");

            if (gameConnections.size() == maxplayers) {
                if (reconnect) {
                    ServerTable oldTable = ServerTable.getInstance(key);
                    oldTable.reconnectPlayers(gameConnections);
                    Message msg = new Message("Rejoined table #" + oldTable.getTableID() + " waiting for game start...");
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

    /**
     * Creates a new game entry with a unique game ID.
     *
     * @return the newly created game ID
     */
    public static synchronized int addNewGame() {
        matching_games.put(id, new ArrayList<>());
        System.out.println("A player has created Table #" + id + "...");
        return id++;
    }

    /**
     * Primary servers start a ServerConnector to handle client connections,
     * while backups only listen for replication updates.
     *
     * @param args optional command-line argument to indicate backup mode
     */
    public static void main(String[] args) {
        boolean isPrimary = true;
        if (args.length > 0 && args[0].equalsIgnoreCase("backup")) {
            isPrimary = false;
        }

        ReplicationManager replicationManager = ReplicationManager.getInstance(isPrimary);
        System.out.println("Starting server in " + (isPrimary ? "PRIMARY" : "BACKUP") + " mode...");

        if (isPrimary) {
            ServerConnector connector = new ServerConnector();
            Thread connectorThread = new Thread(connector);
            connectorThread.start();
        } else {
            System.out.println("Running in backup mode; client listener not started...");
        }
    }
}
