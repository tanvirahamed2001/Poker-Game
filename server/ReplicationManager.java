import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.TimerTask;

public class ReplicationManager {
    private static ReplicationManager instance;
    private boolean isPrimary;
    // For primary mode: a socket to each backup server
    private Socket backupSocket;
    private ObjectOutputStream backupOut;

    // For backup mode: a server socket to listen for replication updates
    private ServerSocket replicationListener;
    private ObjectInputStream primaryIn;

    // Heartbeat timer for detecting failure
    private Timer heartbeatTimer;
    private long lastUpdateTimestamp;
    
    // Replication port (for primary-to-backup communication)
    private final int REPLICATION_PORT = 6836;

    private ReplicationManager(boolean isPrimary) {
        this.isPrimary = isPrimary;
        if (isPrimary) {
            // In primary mode, connect to the backup replication listener
            try {
                backupSocket = new Socket("localhost", REPLICATION_PORT);
                backupOut = new ObjectOutputStream(backupSocket.getOutputStream());
            } catch (IOException e) {
                System.err.println("Primary could not connect to backup replication server.");
                e.printStackTrace();
            }
        } else {
            // In backup mode, listen for replication updates from the primary.
            try {
                replicationListener = new ServerSocket(REPLICATION_PORT);
                // This blocks until the primary connects
                Socket primarySocket = replicationListener.accept();
                primaryIn = new ObjectInputStream(primarySocket.getInputStream());
                // Start a thread to continuously listen for updates
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        listenForUpdates();
                    }
                }).start();
                startHeartbeat();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized ReplicationManager getInstance(boolean isPrimary) {
        if (instance == null) {
            instance = new ReplicationManager(isPrimary);
        }
        return instance;
    }

    // Called by the primary after each key game event to send a state update
    public synchronized void sendStateUpdate(GameState state) {
        if (isPrimary && backupOut != null) {
            try {
                backupOut.writeObject(state);
                backupOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Continuously listens for replication updates from the primary
    private void listenForUpdates() {
        try {
            while (true) {
                Object obj = primaryIn.readObject();
                if (obj instanceof GameState) {
                    GameState receivedState = (GameState) obj;
                    updateLocalGameState(receivedState);
                    lastUpdateTimestamp = System.currentTimeMillis();
                    System.out.println("Received and applied GameState update from primary.");
                } else {
                    System.out.println("Received non-GameState object: " + obj.getClass().getName());
                }
            }
        } catch (EOFException eof) {
            // This exception is expected when the replication stream
            System.out.println("Replication stream closed. Exiting update listener.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Updates the backup's local game state
    private void updateLocalGameState(GameState state) {
        int gameId = state.getGameId();
        ServerTable currentTable = ServerTable.getInstance(gameId);
        if (currentTable != null) {
            currentTable.updateState(state);
        } else {
            System.err.println("No active game found for game ID " + gameId + " on backup");
        }
    }

    //TODO: Find a good heartbeat timer and maybe rethink how we implement this (PLEASE REMOVE ONCE DONE)
    private final long HEARTBEAT_THRESHOLD = 500000; // milliseconds will need to probably change later

    // Starts a heartbeat timer
    private void startHeartbeat() {
        heartbeatTimer = new Timer(true);
        lastUpdateTimestamp = System.currentTimeMillis();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now - lastUpdateTimestamp > HEARTBEAT_THRESHOLD) {
                    System.out.println("No update received in threshold time");
                    promoteToPrimary();
                    heartbeatTimer.cancel();
                }
            }
        }, HEARTBEAT_THRESHOLD, HEARTBEAT_THRESHOLD);
    }

    // Promotes the backup to primary 
    private void promoteToPrimary() {
        isPrimary = true;
        System.out.println("Backup is now promoted to primary.");
        // Close the replication listener 
        if (replicationListener != null) {
            try {
                replicationListener.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Start accepting client connections on the client port
        new Thread(new Runnable() {
            @Override
            public void run() {
                startClientListener();
            }
        }).start();
    }

    // Starts a server socket to accept client connections
    private void startClientListener() {
        final int CLIENT_PORT = 6836;  // Change this if needed for testing
        try (ServerSocket clientSocket = new ServerSocket(CLIENT_PORT)) {
            clientSocket.setReuseAddress(true);
            System.out.println("New primary now accepting client connections on port " + CLIENT_PORT);
            while (true) {
                Socket client = clientSocket.accept();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handleClientConnection(client);
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * For when primary server crashes, this function will take over all the client connections
     * @param client
     */
    private void handleClientConnection(Socket client) {
        System.out.println("Accepted a new client connection post-failover.");
        // reuse your ServerConnector's logic
    }
}