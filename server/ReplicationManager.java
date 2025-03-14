import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;

public class ReplicationManager {
    private static ReplicationManager instance;
    private boolean isPrimary;
    
    // For primary mode: store a list of backup sockets and their output streams.
    private List<Socket> backupSockets = new ArrayList<>();
    private List<ObjectOutputStream> backupOutputs = new ArrayList<>();
    
    // For backup mode: a server socket to listen for replication updates and input stream from primary.
    private ServerSocket replicationListener;
    private ObjectInputStream primaryIn;
    
    // Heartbeat timer for detecting failure on backups.
    private Timer heartbeatTimer;
    private long lastUpdateTimestamp;
    
    // Helper class to define backup endpoints.
    private static class Endpoint {
        String host;
        int port;
        public Endpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }
        @Override
        public String toString() {
            return host + ":" + port;
        }
    }
    
    // In primary mode, list the backup endpoints. In this example, we assume you have two backups.
    private final List<Endpoint> backupEndpoints = Arrays.asList(
        new Endpoint("localhost", 6836),
        new Endpoint("localhost", 6837)
    );
    
    // For backup mode: use a system property to set the replication port; default to 6836.
    private final int DEFAULT_REPLICATION_PORT = 6836;
    
    // The port for client connections remains fixed.
    private final int CLIENT_PORT = 6834;
    
    private ReplicationManager(boolean isPrimary) {
        this.isPrimary = isPrimary;
        if (isPrimary) {
            // In primary mode, iterate over each backup endpoint and attempt to connect.
            for (Endpoint ep : backupEndpoints) {
                try {
                    Socket s = new Socket(ep.host, ep.port);
                    backupSockets.add(s);
                    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                    backupOutputs.add(oos);
                    System.out.println("Primary connected to backup at " + ep);
                } catch (IOException e) {
                    System.err.println("Primary failed to connect to backup at " + ep);
                    // Continue with other endpoints even if one fails.
                }
            }
        } else {
            // In backup mode, listen for replication updates from the primary.
            try {
                int replicationPort = Integer.getInteger("replicationPort", DEFAULT_REPLICATION_PORT);
                replicationListener = new ServerSocket(replicationPort);
                System.out.println("Backup waiting for primary replication connection on port " + replicationPort);
                // This call blocks until the primary connects.
                Socket primarySocket = replicationListener.accept();
                primaryIn = new ObjectInputStream(primarySocket.getInputStream());
                new Thread(() -> listenForUpdates()).start();
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
    
    // Called by the primary after each key game event to send the GameState to all backups.
    public synchronized void sendStateUpdate(GameState state) {
        if (isPrimary) {
            Iterator<ObjectOutputStream> it = backupOutputs.iterator();
            while (it.hasNext()) {
                ObjectOutputStream oos = it.next();
                try {
                    oos.writeObject(state);
                    oos.flush();
                } catch (IOException e) {
                    System.err.println("Error sending state to a backup; removing connection.");
                    e.printStackTrace();
                    it.remove();
                }
            }
        }
    }
    
    // In backup mode: continuously listen for updates from the primary.
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
            System.out.println("Replication stream closed. Exiting update listener.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    // Updates the backup's local game state by calling ServerTable.updateState() for the given game.
    private void updateLocalGameState(GameState state) {
        ServerTable currentTable = ServerTable.getInstance(state.getGameId());
        if (currentTable != null) {
            currentTable.updateState(state);
        } else {
            System.err.println("No active game found for game ID " + state.getGameId() + " on backup.");
        }
    }
    
    // Heartbeat threshold (in milliseconds).
    private final long HEARTBEAT_THRESHOLD = 5000000;
    
    // Starts a heartbeat timer to detect if no updates are received.
    private void startHeartbeat() {
        heartbeatTimer = new Timer(true);
        lastUpdateTimestamp = System.currentTimeMillis();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now - lastUpdateTimestamp > HEARTBEAT_THRESHOLD) {
                    System.out.println("No update received in threshold time.");
                    promoteToPrimary();
                    heartbeatTimer.cancel();
                }
            }
        }, HEARTBEAT_THRESHOLD, HEARTBEAT_THRESHOLD);
    }
    
    // Promotes the backup to primary if no updates are received.
    private void promoteToPrimary() {
        isPrimary = true;
        System.out.println("Backup is now promoted to primary.");
        if (replicationListener != null) {
            try {
                replicationListener.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new Thread(() -> startClientListener()).start();
    }
    
    // Starts a server socket to accept client connections once the backup is promoted.
    private void startClientListener() {
        try (ServerSocket clientSocket = new ServerSocket(CLIENT_PORT)) {
            clientSocket.setReuseAddress(true);
            System.out.println("New primary now accepting client connections on port " + CLIENT_PORT);
            while (true) {
                Socket client = clientSocket.accept();
                new Thread(() -> handleClientConnection(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Handles a new client connection after failover by passing it to ServerTableManager.
    private void handleClientConnection(Socket client) {
        System.out.println("Accepted a new client connection post-failover.");
        try {
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            shared.Player info = (shared.Player) in.readObject();
            new Thread(new ServerTableManager(client, info)).start();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
