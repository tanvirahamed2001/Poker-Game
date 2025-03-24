
import java.io.*;
import java.net.*;
import java.util.*;
import shared.communication_objects.*;
import shared.*;

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
    private int serverId;
    private List<Integer> allServerIds;
    
    // Helper class to define an endpoint.
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
    
    // List of backup endpoints.
    private final List<Endpoint> backupEndpoints = Arrays.asList(
        new Endpoint("localhost", 6836),
        new Endpoint("localhost", 6837),
        new Endpoint("localhost", 6838)
    );
    
    // Ports.
    private final int DEFAULT_REPLICATION_PORT = 6836;
    private final int CLIENT_PORT = 6834;
    
    // Heartbeat threshold in ms.
    private final long HEARTBEAT_THRESHOLD = 5000;
    
    // NEW: Map to store the latest replicated game states.
    private Map<Integer, GameState> replicatedGameStates = new HashMap<>();
    
    private ReplicationManager(boolean isPrimary) {
        this.isPrimary = isPrimary;
        if (isPrimary) {
            // Use system property "serverId" (default 4) for the primary.
            this.serverId = Integer.getInteger("serverId", 4);
            // Connect to each backup.
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
                }
            }
        } else {
            // Use system property "backupId" (default 1) for backups.
            this.serverId = Integer.getInteger("backupId", 1);
            try {
                int replicationPort = Integer.getInteger("replicationPort", DEFAULT_REPLICATION_PORT);
                replicationListener = new ServerSocket(replicationPort);
                System.out.println("Backup " + serverId + " waiting for primary replication connection on port " + replicationPort);
                // Block until a primary connects.
                Socket primarySocket = replicationListener.accept();
                primaryIn = new ObjectInputStream(primarySocket.getInputStream());
                new Thread(() -> listenForUpdates()).start();
                startHeartbeat();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.allServerIds = Arrays.asList(1, 2, 3, 4);
        startElectionListener();
    }
    
    public static synchronized ReplicationManager getInstance(boolean isPrimary) {
        if (instance == null) {
            instance = new ReplicationManager(isPrimary);
        }
        return instance;
    }
    
    // Called by the primary to broadcast GameState updates.
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
                    it.remove();
                }
            }
        }
    }
    
    // Backups listen for state updates from the primary.
    private void listenForUpdates() {
        try {
            while (true) {
                Object obj = primaryIn.readObject();
                if (obj instanceof GameState) {
                    GameState receivedState = (GameState) obj;
                    updateLocalGameState(receivedState);
                    lastUpdateTimestamp = System.currentTimeMillis();
                    System.out.println("Received and applied GameState update for game " +
                                       receivedState.getGameId());
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
    
    // NEW: Save the replicated game state and update the corresponding game table.
    private void updateLocalGameState(GameState state) {
        System.out.println("UPDATING LOCAL GAME STATE");
        System.out.println(state.toString());
        replicatedGameStates.put(state.getGameId(), state);
        ServerTable currentTable = ServerTable.getInstance(state.getGameId());
        if (currentTable != null) {
            currentTable.updateState(state);
        } else {
            System.err.println("No active game for game ID " + state.getGameId() + ". Creating new instance from replication state.");
            ArrayList<shared.PlayerConnection> dummyConnections = new ArrayList<>();
            ServerTable newTable = new ServerTable(state.getGameId(), dummyConnections);
            newTable.updateState(state);
        }
    }
    
    // Heartbeat mechanism to detect primary failure.
    private void startHeartbeat() {
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
                    lastUpdateTimestamp = now;
                }
            }
        }, HEARTBEAT_THRESHOLD, HEARTBEAT_THRESHOLD);
    }
    
    // Election-related methods (startElection, sendElectionMessage, startElectionListener) follow...
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
        String targetHost = "localhost";
        int baseElectionPort = 7000;
        int targetPort = baseElectionPort + targetId;
        try (Socket socket = new Socket(targetHost, targetPort)) {
            socket.setSoTimeout(3000);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Command electionCmd = new Command(Command.Type.ELECTION, new Election(serverId, targetId));
            oos.writeObject(electionCmd);
            oos.flush();
            Command responseCmd = (Command) ois.readObject();
            if (responseCmd.getType() == Command.Type.ELECTION) {
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
        // Update the game list using replicated game states.
        ServerMain.updateGames(replicatedGameStates);
        // Connect to backups so they resume receiving state updates.
        connectBackupsToNewPrimary();
        heartbeatTimer.cancel();
        // Start accepting client connections as the new primary.
        new Thread(() -> startClientListener()).start();
    }
    
    // Connect to all backups.
    private void connectBackupsToNewPrimary() {
        for (Endpoint ep : backupEndpoints) {
            try {
                Socket backupSocket = new Socket(ep.host, ep.port);
                backupSockets.add(backupSocket);
                ObjectOutputStream oos = new ObjectOutputStream(backupSocket.getOutputStream());
                oos.flush();
                backupOutputs.add(oos);
                System.out.println("New primary connected to backup at " + ep);
            } catch (IOException e) {
                System.err.println("New primary failed to connect to backup at " + ep);
            }
        }
    }
    
// Add a flag to ensure the client listener is only started once.
private volatile boolean clientListenerStarted = false;

private void startClientListener() {
    // Check if the client listener is already running.
    if (clientListenerStarted) {
        System.out.println("Client listener already running.");
        return;
    }
    
    try {
        // Try binding to the client port.
        ServerSocket clientSocket = new ServerSocket(CLIENT_PORT);
        clientSocket.setReuseAddress(true);
        clientListenerStarted = true;
        System.out.println("New primary now accepting client connections on port " + CLIENT_PORT);
        while (true) {
            Socket client = clientSocket.accept();
            new Thread(() -> handleClientConnection(client)).start();
        }
    } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Failed to bind to port " + CLIENT_PORT + ". Ensure the old primary is terminated or the port is free.");
    }
}

    // Handle a client connection post-failover.
    private void handleClientConnection(Socket client) {
        System.out.println("Accepted a new client connection post-failover.");
        try {
            PlayerConnection pc = new PlayerConnection(null, client);
            Command cmd = (Command) pc.readCommand();
            if (cmd.getType() == Command.Type.PLAYER_INFO) {
                Player player = (Player) cmd.getPayload();
                pc.updatePlayer(player);
                new Thread(new ServerTableManager(pc)).start();
            } else {
                System.out.println("Unexpected command type from client: " + cmd.getType());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
