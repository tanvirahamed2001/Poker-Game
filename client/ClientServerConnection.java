import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import shared.Colors;

/**
 * Our Client - Server connection object
 * Houses all the sockets and streames necessary for the Client to communicate
 * with the Server
 */
public class ClientServerConnection {
    private Socket primarySocket;
    private ObjectInputStream primaryIn;
    private ObjectOutputStream primaryOut;

    /**
     * Basic constructor for the Client Server Connection
     * 
     * @param primarySocket Takes the primary socket upon connection
     */
    public ClientServerConnection(Socket primarySocket) {
        try {
            this.primarySocket = primarySocket;
            this.primaryOut = new ObjectOutputStream(this.primarySocket.getOutputStream());
            this.primaryOut.flush();
            // I swear to fuck if this works
            this.primaryIn = new ObjectInputStream(this.primarySocket.getInputStream());
        } catch (IOException e) {
            System.out.println(Colors.RED + "Error in client server connection..." + Colors.RESET);
        }
    }

    /**
     * Sets the timeout for the Client Server Connection
     * 
     * @param timeout The set amount of timeout, in ms, that is wanted
     */
    public void setTimeout(int timeout) {
        try {
            primarySocket.setSoTimeout(timeout);
        } catch (IOException e) {
            System.out.println(Colors.RED + "Error in client server timout settings..." + Colors.RESET);
        }
    }

    /**
     * Function for checking if the socket is closed, connected or null
     * 
     * @return Boolean, true if connected, false if not
     */
    public boolean connected() {
        if (primarySocket.isClosed() || !primarySocket.isConnected() || primarySocket == null) {
            return false;
        }
        return true;
    }

    /**
     * Writes a object to the connections output stream.
     * Expects a Command Object.
     * 
     * @param obj The command, or anything else, being written to the stream
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
     * Reads objects that are in the input stream of the connection.
     * 
     * @return The object that was read.
     */
    public Object read() {
        try {
            return primaryIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(Colors.RED + "Error in client server reading..." + Colors.RESET);
            return null;
        }
    }

    /**
     * Closes the given sockets and streams.
     */
    public void closeConnections() {
        try {
            if (primaryIn != null) {
                primaryIn.close();
            }
            if (primaryOut != null) {
                primaryOut.close();
            }
            if (primarySocket != null) {
                primarySocket.close();
            }
        } catch (IOException e) {
            System.out.println(Colors.RED + "Error in closing client server sockets..." + Colors.RESET);
        }
    }
}
