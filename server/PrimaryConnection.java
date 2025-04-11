import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The PrimaryConnection class manages a socket connection between a backup server
 * and the primary server.
 */
public class PrimaryConnection {
    private Socket primarySocket;
    private ObjectInputStream primaryIn;
    private ObjectOutputStream primaryOut;

    /**
     * Constructs a PrimaryConnection with the specified socket.
     * Initializes object streams for sending and receiving data.
     *
     * @param primarySocket the socket connected to the primary server
     */
    public PrimaryConnection(Socket primarySocket) {
        try {
            this.primarySocket = primarySocket;
            this.primaryOut = new ObjectOutputStream(this.primarySocket.getOutputStream());
            this.primaryOut.flush();
            this.primaryIn = new ObjectInputStream(this.primarySocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a object to the primary server.
     *
     * @param obj the object to be sent
     */
    public void write(Object obj) {
        try {
            primaryOut.writeObject(obj);
            primaryOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a object sent from the primary server.
     *
     * @return the object received, or null if an error occurs
     */
    public Object read() {
        try {
            return primaryIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Closes the input stream, output stream, and socket connection to the primary server.
     * Ensures that all resources are properly released.
     */
    public void closeConnections() {
        try {
            if (primaryIn != null) {
                primaryIn.close();
                System.out.println("Closed primary input..");
            }
            if (primaryOut != null) {
                primaryOut.close();
                System.out.println("Closed primary output...");
            }
            if (primarySocket != null) {
                primarySocket.close();
                System.out.println("Closed primary replication listener...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
