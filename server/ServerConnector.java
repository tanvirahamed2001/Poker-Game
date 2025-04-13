import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import shared.*;
import shared.communication_objects.*;

/**
 * TheServerConnector class listens for incoming client connections on a
 * designated port
 * and delegates each client to a new ServerTableManager instance for game
 * management.
 * It supports both new connections and reconnections, handling client ID
 * assignment and
 * player initialization. This class is intended to run as a single server-side
 * listener
 * and should be started in its own thread.
 */
public class ServerConnector implements Runnable {

    /** A counter for assigning unique client-server IDs */
    private static AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Starts the server on port 6834 and listens for client connections.
     * For each accepted socket, a new PlayerConnection is created.
     * Based on the initial Command Type either a new player is assigned
     * a unique ID or a reconnection is processed. All valid players are passed
     * to a ServerTableManager via a thread pool.
     */
    @Override
    public void run() {
        ExecutorService pool = Executors.newCachedThreadPool(); // Thread pool for managing multiple clients
        try (ServerSocket connector = new ServerSocket(6834)) {
            System.out.println("Server is running and waiting for connections...");
            while (true) {
                try {
                    Socket socket = connector.accept();
                    new Thread(() -> {
                        System.out.println("New client connected...");
                        try {
                            PlayerConnection pc = new PlayerConnection(null, socket);

                            // First message expected from client
                            Command response = (Command) pc.readCommand();

                            if (response.getType() == Command.Type.NEW) {
                                // Assign unique ID to the new client
                                pc.sendCommand(Command.Type.SERVER_CLIENT_ID,
                                        new ClientServerId(nextId.getAndIncrement()));

                                // Wait for the client to send its Player object
                                response = (Command) pc.readCommand();
                                Player player = (Player) response.getPayload();
                                pc.updatePlayer(player);

                                // Start game handling
                                pool.submit(new ServerTableManager(pc));

                            } else if (response.getType() == Command.Type.RECONNECT) {
                                // Handle client reconnection and player restoration
                                response = (Command) pc.readCommand();
                                ServerLamportClock.getInstance().receievedEvent(response.getLamportTS());

                                if (response.getType() == Command.Type.PLAYER_INFO) {
                                    Player player = (Player) response.getPayload();
                                    pc.updatePlayer(player);

                                    // Resume game handling
                                    pool.submit(new ServerTableManager(pc));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error assigning player information...");
                        }
                    }).start();
                } catch (IOException e) {
                    System.err.println("Error opening client socket...");
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting the server...");
        } finally {
            pool.shutdown(); // Gracefully shuts down the thread pool when server stops
        }
    }
}
