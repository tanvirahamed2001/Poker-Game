import java.io.*;
import java.net.Socket;

import shared.Colors;
import shared.Player;
import shared.communication_objects.*;

/**
 * The PlayerConnection class manages the connection between the server
 * and player.
 * It maintains communication using object streams, allowing for
 * the sending and receiving of Command objects. This class
 * also incorporates Lamport clock.
 */
public class PlayerConnection {
    private Player player;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    /**
     * Constructs a new PlayerConnection with the given player and socket.
     * Initializes object input and output streams.
     *
     * @param player the player associated with this connection
     * @param socket the socket connected to the player's client
     * @throws IOException if an I/O error occurs during stream setup
     */
    public PlayerConnection(Player player, Socket socket) {
        try {
            this.player = player;
            this.socket = socket;

            // Initialize output stream first to prevent stream deadlock
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();

            // Then initialize input stream
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.out.println("Error in server to client connection...");
        }
    }

    /**
     * Returns the player associated with this connection.
     *
     * @return the Player object
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Updates the player associated with this connection.
     *
     * @param player the new Player object
     */
    public void updatePlayer(Player player) {
        this.player = player;
    }

    /**
     * Returns the socket associated with this connection.
     *
     * @return the Socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Returns the input stream used for receiving objects.
     *
     * @return the ObjectInputStream
     */
    public ObjectInputStream getReader() {
        return in;
    }

    /**
     * Returns the output stream used for sending objects.
     *
     * @return the ObjectOutputStream
     */
    public ObjectOutputStream getWriter() {
        return out;
    }

    /**
     * Sends a Command object to the player, with the type and payload.
     * Attaches the current Lamport timestamp.
     *
     * @param type the command type
     * @param obj  the object payload to send
     */
    public void sendCommand(Command.Type type, Object obj) {
        int ts = ServerLamportClock.getInstance().sendEvent();
        Command cmd = new Command(type, obj);
        cmd.setLamportTS(ts);
        try {
            out.writeObject(cmd);
            out.flush();
        } catch (Exception e) {
            System.out.println("Error in server to client connection sending...");
        }
    }

    /**
     * Reads a Command object sent by the client and updates the Lamport clock.
     *
     * @return the Command received or null if an error occurred
     */
    public Object readCommand() {
        try {
            Object obj = in.readObject();
            Command cmd = (Command) obj;
            int senderTS = cmd.getLamportTS();
            ServerLamportClock.getInstance().receievedEvent(senderTS);
            return cmd;
        } catch (IOException e) {
            System.out.println("Error in server to client connection reading...");
            return new Command(Command.Type.DISCONNECT, null);
        } catch (ClassNotFoundException e2) {
            return null;
        }
    }

    /**
     * Closes the socket connection to the player.
     *
     * @throws IOException if an error occurs while closing the socket
     */
    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("Error in server to player connection closing...");
        }
    }

    /**
     * Sends the current player object back to the client to update its state.
     */
    public void updateClientPlayer() {
        sendCommand(Command.Type.CLIENT_UPDATE_PLAYER, this.player);
    }
}
