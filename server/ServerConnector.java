
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

    private static AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public void run() {
        ExecutorService pool = Executors.newCachedThreadPool(); // Thread pool for handling client connections
        try (ServerSocket connector = new ServerSocket(6834)) {
            System.out.println("Server is running and waiting for connections on port 6834...");
            while (true) {
                try {
                    Socket socket = connector.accept();
                    new Thread(() -> {
                        System.out.println("New client connected: " + socket.getInetAddress());
                        try {
                            PlayerConnection pc = new PlayerConnection(null, socket);
                            Command response = (Command) pc.readCommand();
                            if (response.getType() == Command.Type.NEW) {
                                pc.sendCommand(Command.Type.SERVER_CLIENT_ID,
                                        new ClientServerId(nextId.getAndIncrement()));
                                response = (Command) pc.readCommand();
                                Player player = (Player) response.getPayload();
                                pc.updatePlayer(player);
                                pool.submit(new ServerTableManager(pc));
                            } else if (response.getType() == Command.Type.RECONNECT) {
                                response = (Command) pc.readCommand();
                                ServerLamportClock.getInstance().receievedEvent(response.getLamportTS());
                                if (response.getType() == Command.Type.PLAYER_INFO) {
                                    Player player = (Player) response.getPayload();
                                    pc.updatePlayer(player);
                                    pool.submit(new ServerTableManager(pc));
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Error assigning player information: " + e.getMessage()
                                    + "in ServerConnector line 46.");
                        }
                    }).start();
                } catch (IOException e) {
                    System.err
                            .println("Error opening client socket: " + e.getMessage() + " in ServerConnector line 50.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting the server: " + e.getMessage());
        } finally {
            pool.shutdown(); // Shutdown the thread pool when the server stops
        }
    }
}