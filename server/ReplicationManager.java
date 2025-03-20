import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;
import shared.communication_objects.*;

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
    
    // Election fields.
    // Each server (primary or backup) has a unique ID.
    private int serverId;
    // List of all server IDs (for example, backups: 1,2,3 and primary: 4).
    private List<Integer> allServerIds;
    
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
    // Update these endpoints with the actual IP addresses/ports of your backup servers.
    private final List<Endpoint> backupEndpoints = Arrays.asList(
        new Endpoint("localhost", 6836),
        new Endpoint("localhost", 6837),
        new Endpoint("localhost", 6838)
    );
    
    // For backup mode: replication port and fixed client port.
    private final int DEFAULT_REPLICATION_PORT = 6836;
    private final int CLIENT_PORT = 6834;
    
    // Heartbeat threshold in milliseconds (adjust as needed).
    private final long HEARTBEAT_THRESHOLD = 10000; // e.g., 50 seconds
    
    private ReplicationManager(boolean isPrimary) {
        this.isPrimary = isPrimary;
        if (isPrimary) {
            // Primary mode: use the system property "serverId" (default 4).
            this.serverId = Integer.getInteger("serverId", 4);
            // Connect to all backup servers.
            for (Endpoint ep : backupEndpoints) {
                try {
                    Socket s = new Socket(ep.host, ep.port);
                    backupSockets.add(s);
                    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                    oos.flush();
                    backupOutputs.add(oos);
                    System.out.println("Primary connected to backup at " + ep);
                } catch (IOException e) {
                    System.err.println("Primary failed to connect to backup at " + ep);
                    // Continue with remaining endpoints.
                }
            }
        } else {
            // Backup mode: use the system property "backupId" (default 1).
            this.serverId = Integer.getInteger("backupId", 1);
            try {
                int replicationPort = Integer.getInteger("replicationPort", DEFAULT_REPLICATION_PORT);
                replicationListener = new ServerSocket(replicationPort);
                System.out.println("Backup " + serverId + " waiting for primary replication connection on port " + replicationPort);
                // Block until the primary connects.
                Socket primarySocket = replicationListener.accept();
                primaryIn = new ObjectInputStream(primarySocket.getInputStream());
                new Thread(() -> listenForUpdates()).start();
                startHeartbeat();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Set the list of all server IDs. For example: backups (1, 2, 3) and primary (4).
        this.allServerIds = Arrays.asList(1, 2, 3, 4);
        
        // Start election listener for every server (primary and backup).
        startElectionListener();
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
    
    // Start a heartbeat timer. If no update is received within the threshold, initiate leader election.
    private void startHeartbeat() {
        // If there's an existing timer, cancel it to avoid duplicate tasks.
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }
        heartbeatTimer = new Timer(true);
        lastUpdateTimestamp = System.currentTimeMillis();
        
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now - lastUpdateTimestamp > HEARTBEAT_THRESHOLD) {
                    System.out.println("No update received in threshold time. Initiating election.");
                    startElection();
                    // Reset the lastUpdateTimestamp to avoid triggering repeated elections immediately
                    lastUpdateTimestamp = now;
                    
                    // Alternatively, if your election process takes time,
                    // you could cancel the timer and reinitialize it once the election concludes.
                    // heartbeatTimer.cancel();
                    // startHeartbeat();
                }
            }
        }, HEARTBEAT_THRESHOLD, HEARTBEAT_THRESHOLD);
    }
    
    // Bully algorithm: send election messages to all servers with a higher ID.
    private void startElection() {
        System.out.println("Server " + serverId + " starting election.");
        boolean higherServerAlive = false;
        for (Integer id : allServerIds) {
            if (id > serverId) {
                try {
                    boolean response = sendElectionMessage(id);
                    if (response) {
                        higherServerAlive = true;
                        break;
                    }
                } catch (IOException e) {
                    System.err.println("No response from server with id " + id);
                }
            }
        }
        if (!higherServerAlive) {
            System.out.println("No higher server responded. Server " + serverId + " becomes the new primary.");
            promoteToPrimary();
        } else {
            System.out.println("A higher server is alive. Waiting for new leader announcement.");
        }
    }
    
    private boolean sendElectionMessage(int targetId) throws IOException {
        // Determine the target's host and election port.
        String targetHost = "localhost"; // Update as needed.
        int baseElectionPort = 7000; // Example base port.
        int targetPort = baseElectionPort + targetId; // For example, server with id 2 listens on port 7002.
        
        try (Socket socket = new Socket(targetHost, targetPort)) {
            socket.setSoTimeout(3000); // Set a timeout for the response.
            
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            
            // Create and send an election command.
            Command electionCmd = new Command(Command.Type.ELECTION, new Election(serverId, targetId));
            oos.writeObject(electionCmd);
            oos.flush();
            
            // Wait for and process the response.
            Command responseCmd = (Command) ois.readObject();
            if (responseCmd.getType() == Command.Type.ELECTION) {
                // Expecting a Boolean response indicating the target server is alive.
                Boolean isAlive = (Boolean) responseCmd.getPayload();
                return isAlive;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error sending election message to server " + targetId + ": " + e.getMessage());
        }
        return false;
    }
    
    private void startElectionListener() {
        new Thread(() -> {
            int baseElectionPort = 7000;
            int myElectionPort = baseElectionPort + serverId;
            try (ServerSocket electionSocket = new ServerSocket(myElectionPort)) {
                System.out.println("Server " + serverId + " listening for election messages on port " + myElectionPort);
                while (true) {
                    Socket socket = electionSocket.accept();
                    new Thread(() -> {
                        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
                            
                            Command electionCmd = (Command) ois.readObject();
                            if (electionCmd.getType() == Command.Type.ELECTION) {
                                Election election = (Election) electionCmd.getPayload();
                                // Ensure the message is intended for this server.
                                if (election.get_target_id() == serverId) {
                                    Command response = new Command(Command.Type.ELECTION, Boolean.TRUE);
                                    oos.writeObject(response);
                                    oos.flush();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            } catch (IOException e) {
                System.err.println("Election listener on server " + serverId + " failed: " + e.getMessage());
            }
        }).start();
    }
    
    // Promote this backup to primary.
    private void promoteToPrimary() {
        isPrimary = true;
        System.out.println("Server " + serverId + " is now promoted to primary.");
        if (replicationListener != null) {
            try {
                replicationListener.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Start client listener on the primary port.
        // Uncomment the line below to start accepting client connections post-promotion.
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
