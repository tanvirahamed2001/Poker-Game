
import java.io.*;
import java.net.*;
import java.util.*;
import shared.communication_objects.*;
import shared.*;

/**
 * The ReplicationManager class handles the core logic for maintaining
 * consistent replicated state across primary and backup servers.
 */
public class ReplicationManager {

    /** Singleton instance of the replication manager */
    private static ReplicationManager instance;

    /** Indicates whether this server is the current primary */
    private boolean isPrimary;

    /** Connections to all backups (only active in primary mode) */
    private List<BackupConnection> backupConns = new ArrayList<>();

    /** Listens for replication updates in backup mode */
    private ServerSocket replicationListener;

    /** Active connection to primary (only used by backups) */
    private PrimaryConnection primaryConn;

    /** Heartbeat timer to detect primary failure */
    private Timer heartbeatTimer;

    /** Timestamp of the last received heartbeat/update */
    private long lastUpdateTimestamp;

    /** ID of the current server (used in elections) */
    private int serverId;

    /** List of all participating server IDs */
    private List<Integer> allServerIds;

    /** Local Lamport clock instance for event ordering */
    private LamportClock lamportClock;

    /** Internal map storing replicated game state data by game ID */
    private Map<Integer, GameState> replicatedGameStates = new HashMap<>();

    /** Static nested helper class defining a connection endpoint (host + port) */
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

    /** Default backup endpoints */
    private final List<Endpoint> backupEndpoints = Arrays.asList(
            new Endpoint("localhost", 6836),
            new Endpoint("localhost", 6837),
            new Endpoint("localhost", 6838),
            new Endpoint("localhost", 6839)
    );

    private final int DEFAULT_REPLICATION_PORT = 6836;
    private final int CLIENT_PORT = 6834;
    private final long HEARTBEAT_THRESHOLD = 5000;

    /**
     * Private constructor that initializes primary or backup-specific components
     * based on the  isPrimary flag.
     *
     * @param isPrimary true if this instance is the primary server
     */
    private ReplicationManager(boolean isPrimary) {
        this.lamportClock = new LamportClock();
        this.isPrimary = isPrimary;

        if (isPrimary) {
            this.serverId = Integer.getInteger("serverId", 4);
            for (Endpoint ep : backupEndpoints) {
                try {
                    backupConns.add(new BackupConnection(new Socket(ep.host, ep.port)));
                    System.out.println("Primary connected to backup at " + ep);
                } catch (IOException e) {
                    System.err.println("Primary failed to connect to backup at " + ep);
                }
            }
        } else {
            this.serverId = Integer.getInteger("backupId", 1);
            int replicationPort = Integer.getInteger("replicationPort", DEFAULT_REPLICATION_PORT);
            waitForPrimaryConnection(replicationPort);
        }

        this.allServerIds = Arrays.asList(1, 2, 3, 4, 5);
        startElectionListener();
    }

    /**
     * Returns the singleton instance of ReplicationManager.
     *
     * @param isPrimary true if the server should initialize as primary
     * @return the singleton instance
     */
    public static synchronized ReplicationManager getInstance(boolean isPrimary) {
        if (instance == null) {
            instance = new ReplicationManager(isPrimary);
        }
        return instance;
    }

    /**
     * Called by the primary to send a GameState update to all backups.
     * Waits for acknowledgments and handles potential replication failures.
     *
     * @param state the game state to replicate
     */
    public synchronized void sendStateUpdate(GameState state) {
        if (isPrimary) {
            for (int i = 0; i < backupConns.size(); i++) {
                BackupConnection backup = backupConns.get(i);
                try {
                    backup.setTimeout(10000);
                    backup.write(state);
                    System.out.println("Game state sent...");
                    Command response = (Command) backup.read();
                    lamportRecieve(response.getLamportTS());
                    if (response.getType() != Command.Type.REPLICATION_ACK) {
                        throw new ReplicationExecption("No Ack From Backup...");
                    } else {
                        System.out.println("Received replication ACK...");
                    }
                } catch (ReplicationExecption e) {
                    System.err.println("Error sending state to a backup, removing connection...");
                    backupConns.remove(i);
                }
            }
        }
    }

    /**
     * Exception class used to flag replication issues.
     */
    class ReplicationExecption extends Exception {
        public ReplicationExecption(String msg) {
            super(msg);
        }
    }

    /**
     * Listens for game state updates from the primary and sends acknowledgments.
     * Only used in backup mode.
     */
    private void listenForUpdates() {
        while (true) {
            Object obj = primaryConn.read();
            if (obj instanceof GameState) {
                GameState receivedState = (GameState) obj;
                lamportRecieve(receivedState.getLamportTS());
                updateLocalGameState(receivedState);
                lastUpdateTimestamp = System.currentTimeMillis();
                System.out.println("Received and applied GameState update for game " + receivedState.getGameId());

                Command ack = new Command(Command.Type.REPLICATION_ACK, "ack");
                ack.setLamportTS(ServerLamportClock.getInstance().sendEvent());
                primaryConn.write(ack);
            } else if (obj == null) {
                System.err.println("Replication connection lost...");
                primaryConn.closeConnections();
                primaryConn = null;
                reinitializeReplicationListener();
                break;
            } else {
                System.out.println("Received non-GameState object: " + obj.getClass().getName());
            }
        }
    }

/**
 * Waits for the primary server to establish a replication connection.
 * <p>
 * This method is used by backup servers to listen for a connection from the primary,
 * set up the {@link PrimaryConnection}, start listening for updates,
 * and initialize the heartbeat monitoring.
 *
 * @param port the port to listen on for the primary's replication connection
 */
private void waitForPrimaryConnection(int port) {
    while (true) {
        try {
            if (replicationListener == null || replicationListener.isClosed()) {
                replicationListener = new ServerSocket(port);
                System.out.println("Backup " + serverId + " waiting for primary replication connection on port " + port);
            }
            Socket s = replicationListener.accept();
            primaryConn = new PrimaryConnection(s);
            new Thread(() -> listenForUpdates()).start();
            lastUpdateTimestamp = System.currentTimeMillis();
            startHeartbeat();
            break;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * Updates the local Lamport clock based on a received timestamp.
 * <p>
 * This helps maintain a consistent logical ordering of events across distributed servers.
 *
 * @param ts the Lamport timestamp received from another server
 */
private void lamportRecieve(int ts) {
    ServerLamportClock.getInstance().receievedEvent(ts);
}

/**
 * Reinitializes the replication listener when the connection to the primary is lost.
 * <p>
 * This method will call {@link #waitForPrimaryConnection(int)} with the configured port,
 * reestablishing communication with the new or recovered primary.
 */
private void reinitializeReplicationListener() {
    int replicationPort = Integer.getInteger("replicationPort", DEFAULT_REPLICATION_PORT);
    waitForPrimaryConnection(replicationPort);
}


    /**
     * Updates the internal map of replicated game states and updates or creates game tables accordingly.
     *
     * @param state the replicated {@link GameState} received from the primary
     */
    private void updateLocalGameState(GameState state) {
        replicatedGameStates.put(state.getGameId(), state);
        ServerTable currentTable = ServerTable.getInstance(state.getGameId());
        if (currentTable != null) {
            currentTable.updateState(state);
        } else {
            System.err.println("No active game for game ID " + state.getGameId() + ". Creating new instance from replication state.");
            ArrayList<PlayerConnection> dummyConnections = new ArrayList<>();
            ServerTable newTable = new ServerTable(state.getGameId(), dummyConnections);
            newTable.updateState(state);
        }
    }

    /**
     * Starts a heartbeat timer that checks whether the primary has failed.
     * If a timeout occurs, the server initiates an election.
     */
    private void startHeartbeat() {
        if (heartbeatTimer != null) heartbeatTimer.cancel();
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

/**
 * Initiates the Bully Election algorithm when the primary is suspected to have failed.
 * <p>
 * This method checks for any higher-ID servers that are still alive.
 * If none respond, this server promotes itself to primary.
 */
private void startElection() {
    System.out.println("Server " + serverId + " starting election.");
    boolean higherServerAlive = false;
    for (Integer id : allServerIds) {
        if (id > serverId) {
            try {
                if (sendElectionMessage(id)) {
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

/**
 * Sends an election message to a target server with a higher ID to determine if it's alive.
 *
 * @param targetId the ID of the target server
 * @return true if the target server responded and is alive; false otherwise
 * @throws IOException if a socket connection error occurs
 */
private boolean sendElectionMessage(int targetId) throws IOException {
    String targetHost = "localhost";
    int baseElectionPort = 7000;
    int targetPort = baseElectionPort + targetId;
    try (Socket socket = new Socket(targetHost, targetPort)) {
        socket.setSoTimeout(3000);
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

        Command electionCmd = new Command(Command.Type.ELECTION, new Election(serverId, targetId));
        oos.writeObject(electionCmd);
        oos.flush();

        Command responseCmd = (Command) ois.readObject();
        return responseCmd.getType() == Command.Type.ELECTION && (Boolean) responseCmd.getPayload();
    } catch (IOException | ClassNotFoundException e) {
        System.err.println("Error sending election message to server " + targetId + ": " + e.getMessage());
    }
    return false;
}

/**
 * Listens for incoming election messages on a port determined by the server ID.
 * <p>
 * When an election message is received and this server is the intended target,
 * it responds with an acknowledgment indicating it is alive.
 */
private void startElectionListener() {
    new Thread(() -> {
        int baseElectionPort = 7000;
        int myElectionPort = baseElectionPort + serverId;
        try (ServerSocket electionSocket = new ServerSocket(myElectionPort)) {
            System.out.println("Server " + serverId + " listening for election messages on port " + myElectionPort);
            while (true) {
                Socket socket = electionSocket.accept();
                new Thread(() -> {
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        oos.flush();
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                        Command electionCmd = (Command) ois.readObject();
                        if (electionCmd.getType() == Command.Type.ELECTION &&
                            ((Election) electionCmd.getPayload()).get_target_id() == serverId) {

                            Command response = new Command(Command.Type.ELECTION, Boolean.TRUE);
                            oos.writeObject(response);
                            oos.flush();
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


    /**
     * Promotes the current server to primary, reconnects to backups,
     * and resumes client handling responsibilities.
     */
    private void promoteToPrimary() {
        isPrimary = true;
        System.out.println("Server " + serverId + " is now promoted to primary.");
        try {
            if (replicationListener != null) replicationListener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ServerMain.updateGames(replicatedGameStates);
        connectBackupsToNewPrimary();
        if (heartbeatTimer != null) heartbeatTimer.cancel();

        ServerConnector connector = new ServerConnector();
        new Thread(connector).start();
    }

    /**
     * Called after promotion to primary. Reconnects to all backups to resume state replication.
     */
    private void connectBackupsToNewPrimary() {
        for (Endpoint ep : backupEndpoints) {
            try {
                backupConns.add(new BackupConnection(new Socket(ep.host, ep.port)));
                System.out.println("New primary connected to backup at " + ep);
            } catch (IOException e) {
                System.err.println("New primary failed to connect to backup at " + ep);
            }
        }
    }
}
