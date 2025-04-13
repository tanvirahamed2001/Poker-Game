import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The BackupConnection class handles the communication between a 
 * primary server and its backup server.
 */
public class BackupConnection {
    private Socket backupSocket;
    private ObjectOutputStream backupOut;
    private ObjectInputStream backupIn;

    /**
     * Constructs a BackupConnection using the provided socket.
     * Initializes object input and output streams for object communication.
     *
     * @param backupSocket the socket connected to the primary server
     */
    public BackupConnection(Socket backupSocket) {
        try {
            this.backupSocket = backupSocket;
            this.backupOut = new ObjectOutputStream(this.backupSocket.getOutputStream());
            this.backupOut.flush();
            this.backupIn = new ObjectInputStream(this.backupSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("Error in primary to backup connection...");
        }
    }

    /**
     * Sets the socket read timeout for receiving data.
     *
     * @param timeout the timeout value in milliseconds
     */
    public void setTimeout(int timeout) {
        try {
            backupSocket.setSoTimeout(timeout);
        } catch (IOException e) {
            System.out.println("Error in primary to backup connection timeout...");
        }
    }

    /**
     * Sends a object to the primary server.
     *
     * @param obj the object to send
     */
    public void write(Object obj) {
        try {
            backupOut.writeObject(obj);
            backupOut.flush();
        } catch (IOException e) {
            System.out.println("Error in primary to backup connection writing...");
        }
    }

    /**
     * Reads a object sent by the primary server.
     *
     * @return the received object, or null if an error occurs
     */
    public Object read() {
        try {
            return backupIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error in primary to backup connection reading...");
            return null;
        }
    }

    /**
     * Closes the input and output streams as well as the socket connection 
     * to the primary server.
     */
    public void closeConnections() {
        try {
            if (backupIn != null) {
                backupIn.close();
                System.out.println("Closed backup input..");
            }
            if (backupOut != null) {
                backupOut.close();
                System.out.println("Closed backup output...");
            }
            if (backupSocket != null) {
                backupSocket.close();
                System.out.println("Closed backup listener...");
            }
        } catch (IOException e) {
            System.out.println("Error in primary to backup connection closing...");
        }
    }
}
