import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;

public class ReplicationManager {
    private static ReplicationManager instance;
    private boolean isPrimary;
    
    // For primary mode: store backup sockets and their output streams.
    private List<Socket> backupSockets = new ArrayList<>();
    private List<ObjectOutputStream> backupOutputs = new ArrayList<>();
    
    // For backup mode: a ServerSocket to listen for replication updates and an ObjectInputStream for the primary.
    private ServerSocket replicationListener;
    private ObjectInputStream primaryIn;
    
    // Heartbeat timer for detecting failure.
    private Timer heartbeatTimer;
    private long lastUpdateTimestamp;
    
    // Leader election fields.
    // Each backup is assigned a unique ID. (Set via system property "backupId", default is 1)
    private int myBackupId;
    // A list of backup IDs that should include all backups (for example, 1,2,3).
    private List<Integer> backupIds;
    
    // Helper class to define an endpoint (hostname and port)
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
    
    // In primary mode, list the backup endpoints.
    // For three backups, update these with the actual IP addresses of the backup machines.
    private final List<Endpoint> backupEndpoints = Arrays.asList(
        new Endpoint("localhost", 6836),
        new Endpoint("localhost", 6837),
        new Endpoint("localhost", 6838)
    );
    
    // For backup mode: replication port and fixed client port.
    private final int DEFAULT_REPLICATION_PORT = 6836;
    private final int CLIENT_PORT = 6834;
    
    // Heartbeat threshold in milliseconds (adjust as needed).
    private final long HEARTBEAT_THRESHOLD = 5000; // e.g., 5 seconds
    
    private ReplicationManager(boolean isPrimary) {
        this.isPrimary = isPrimary;
        // Set backup IDs from configuration.
        this.myBackupId = Integer.getInteger("backupId", 1); // Each backup runs with a unique backupId
        this.backupIds = Arrays.asList(1, 2, 3); // Example: three backups
        
        if (isPrimary) {
            // Primary mode: connect to all backups.
            for (Endpoint ep : backupEndpoints) {
                try {
                    Socket s = new Socket(ep.host, ep.port);
                    backupSockets.add(s);
                    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                    backupOutputs.add(oos);
                    System.out.println("Primary connected to backup at " + ep);
                } catch (IOException e) {
                    System.err.println("Primary failed to connect to backup at " + ep);
                    // Continue with remaining endpoints.
                }
            }
        } else {
            // Backup mode: create a replication listener.
            try {
                int replicationPort = Integer.getInteger("replicationPort", DEFAULT_REPLICATION_PORT);
                replicationListener = new ServerSocket(replicationPort);
                System.out.println("Backup " + myBackupId + " waiting for primary replication connection on port " + replicationPort);
                // Block until the primary connects.
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
    
    // Primary calls this method to broadcast the GameState to all backups.
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
    
    // Backups continuously listen for updates from the primary.
    private void listenForUpdates() {
        try {
            while (true) {
                Object obj = primaryIn.readObject();
                if (obj instanceof GameState) {
                    GameState receivedState = (GameState) obj;
                    updateLocalGameState(receivedState);
                    lastUpdateTimestamp = System.currentTimeMillis();
                    System.out.println("Received and applied GameState update from primary. (Game ID: " +
                                       receivedState.getGameId() + ", Turn: " + receivedState.getCurrentTurn() + ")");
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
    
    // Update local game state via the ServerTable.
    private void updateLocalGameState(GameState state) {
        ServerTable currentTable = ServerTable.getInstance(state.getGameId());
        if (currentTable != null) {
            currentTable.updateState(state);
        } else {
            System.err.println("No active game found for game ID " + state.getGameId() + " on backup. Creating new instance from replication state.");
            ArrayList<shared.PlayerConnection> dummyConnections = new ArrayList<>();
            ServerTable newTable = new ServerTable(state.getGameId(), dummyConnections);
            newTable.updateState(state);
        }
    }
    
    // Start a heartbeat timer. If no update is received in threshold time, initiate leader election.
    private void startHeartbeat() {
        heartbeatTimer = new Timer(true);
        lastUpdateTimestamp = System.currentTimeMillis();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now - lastUpdateTimestamp > HEARTBEAT_THRESHOLD) {
                    System.out.println("No update received in threshold time.");
                    startElection();
                    heartbeatTimer.cancel();
                }
            }
        }, HEARTBEAT_THRESHOLD, HEARTBEAT_THRESHOLD);
    }
    
    // Bully algorithm: send election messages to all backups with a higher ID.
    private void startElection() {
        System.out.println("Backup " + myBackupId + " starting election.");
        boolean higherBackupAlive = false;
        for (Integer id : backupIds) {
            if (id > myBackupId) {
                try {
                    // In a complete implementation, send an election Command to the backup with target ID.
                    // Here, we simulate the process:
                    boolean response = sendElectionMessage(id);
                    if (response) {
                        higherBackupAlive = true;
                        break;
                    }
                } catch (IOException e) {
                    System.err.println("No response from backup with id " + id);
                }
            }
        }
        if (!higherBackupAlive) {
            System.out.println("No higher backup responded. Backup " + myBackupId + " becomes the new primary.");
            promoteToPrimary();
        } else {
            System.out.println("A higher backup is alive. Waiting for new leader announcement.");
        }
    }
    
    // Placeholder: send an election message to a backup with the given target ID.
    private boolean sendElectionMessage(int targetId) throws IOException {
        // In your final implementation, use your object streams to send an Election command and wait for an OK response.
        // For now, we simulate by returning false (i.e. no response).
        return false;
    }
    
    // Promote this backup to primary.
    private void promoteToPrimary() {
        isPrimary = true;
        System.out.println("Backup " + myBackupId + " is now promoted to primary.");
        if (replicationListener != null) {
            try {
                replicationListener.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Start client listener on the primary port.
       // new Thread(() -> startClientListener()).start();
    }
    
    // Starts a ServerSocket for client connections once this backup is promoted.
    private void startClientListener() {
        try (ServerSocket clientSocket = new ServerSocket(CLIENT_PORT)) {
            clientSocket.setReuseAddress(true);
            System.out.println("New primary now accepting client connections on port " + CLIENT_PORT);
            while (true) {
                Socket client = clientSocket.accept();
                // Handle each client connection (e.g., pass to ServerTableManager).
                new Thread(() -> handleClientConnection(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Handles incoming client connections post-failover.
    private void handleClientConnection(Socket client) {
        System.out.println("Accepted a new client connection post-failover.");
        try {
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            shared.Player info = (shared.Player) in.readObject();
            //new Thread(new ServerTableManager(client, info)).start();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
