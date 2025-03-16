package shared;

import java.io.*;
import java.net.Socket;
import shared.Player;

public class PlayerConnection {
    private Player player;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public PlayerConnection(Player player, Socket socket) throws IOException {
        this.player = player;
        this.socket = socket;
        // Create ObjectOutputStream first and flush its header.
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        // Then create ObjectInputStream.
        this.in = new ObjectInputStream(socket.getInputStream());
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Socket getSocket() {
        return socket;
    }
    
    public ObjectInputStream getReader() {
        return in;
    }
    
    public ObjectOutputStream getWriter() {
        return out;
    }
    
    // Now send a message as an object (e.g., a String or a Command object).
    public void sendMessage(Object message) throws IOException {
        out.writeObject(message);
        out.flush();
    }
    
    // Read a message and return it as an Object.
    public Object readMessage() throws IOException, ClassNotFoundException {
        return in.readObject();
    }
    
    public void close() throws IOException {
        socket.close();
    }
}
