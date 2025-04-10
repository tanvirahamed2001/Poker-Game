
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import shared.*;
import shared.communication_objects.*;
import java.util.concurrent.atomic.AtomicInteger;

// Accepts connections from clients and passes them to the ServerTableManager
// Only one instance of this class should run at a time
public class ServerConnector implements Runnable {

    private static int nextId = 1;

    @Override
    public void run() {
        ExecutorService pool = Executors.newCachedThreadPool(); // Thread pool for handling client connections

        try (ServerSocket connector = new ServerSocket(6834)) {

            System.out.println("Server is running and waiting for connections on port 6834...");

            while (true) {
                try {
                    // Accept a new client connection
                    Socket socket = connector.accept();
                    new Thread(() -> {
                        System.out.println("New client connected: " + socket.getInetAddress());
                        try {
                            // initially setup the player connection object with the socket
                            PlayerConnection pc = new PlayerConnection(null, socket);

                            // We wait for the client to send the connection type - NEW, REJOIN OR RECONNECT
                            Command response = (Command) pc.readCommand();

                            if (response.getType() == Command.Type.NEW) { // A NEW CONNECTION
                                // Send the player his assigned ID
                                pc.sendCommand(Command.Type.SERVER_CLIENT_ID,
                                        new ClientServerId(nextId));
                                nextId++;
                                // Wait for response to get player information
                                response = (Command) pc.readCommand();
                                ServerLamportClock.getInstance().receievedEvent(response.getLamportTS());
                                Player player = (Player) response.getPayload();
                                pc.updatePlayer(player);
                                // Pass the connection and player to the ServerTableManager
                                pool.submit(new ServerTableManager(pc));
                            } else if (response.getType() == Command.Type.REJOIN) { // A CLIENT DISCONNECT
                                response = (Command) pc.readCommand();
                                ServerLamportClock.getInstance().receievedEvent(response.getLamportTS());
                                pool.submit(new ServerTableManager(pc));
                            } else if (response.getType() == Command.Type.RECONNECT) { // A SERVER DISCONNECT
                                response = (Command) pc.readCommand();
                                ServerLamportClock.getInstance().receievedEvent(response.getLamportTS());
                                if (response.getType() == Command.Type.PLAYER_INFO) {
                                    Player player = (Player) response.getPayload();
                                    if(player.get_client_id() > nextId){
                                        nextId = player.get_client_id();
                                        nextId++;
                                    }
                                    pc.updatePlayer(player);
                                    pool.submit(new ServerTableManager(pc));
                                }
                            }

                        } catch (IOException e) {
                            System.err.println("Error assigning player information: " + e.getMessage()
                                    + "in ServerConnector Line 46.");
                        } catch (ClassNotFoundException e2) {
                            System.err.println("Error getting class information: " + e2.getMessage()
                                    + "in ServerConnector Line 48.");
                        }
                    }).start();
                } catch (IOException e) {
                    System.err
                            .println("Error opening client socket: " + e.getMessage() + " in ServerConnector Line 50.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting the server: " + e.getMessage());
        } finally {
            pool.shutdown(); // Shutdown the thread pool when the server stops
        }
    }
}