import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import shared.Player;
import shared.communication_objects.*;

// Accepts connections from clients and passes them to the ServerTableManager
// Only one instance of this class should run at a time
public class ServerConnector implements Runnable {

    @Override
    public void run() {
        ExecutorService pool = Executors.newCachedThreadPool(); // Thread pool for handling client connections

        try (ServerSocket connector = new ServerSocket(6834)) {
            System.out.println("Server is running and waiting for connections on port 6834...");

            while (true) {
                try {
                    // Accept a new client connection
                    Socket socket = connector.accept();
                    System.out.println("New client connected: " + socket.getInetAddress());

                    // Read the Player object sent by the client
                    ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                    Command cmd = (Command) input.readObject();
                    Player player = (Player) cmd.getPayload();

                    // Pass the connection and player to the ServerTableManager
                    pool.submit(new ServerTableManager(socket, player));
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    System.err.println("Error reading Player object from client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting the server: " + e.getMessage());
        } finally {
            pool.shutdown(); // Shutdown the thread pool when the server stops
        }
    }
}