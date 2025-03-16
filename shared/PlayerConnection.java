package shared;

import java.io.*;
import java.net.Socket;
import shared.Player;

public class PlayerConnection {
    private Player player;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public PlayerConnection(Player player, Socket socket) throws IOException {
        this.player = player;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Socket getSocket() {
        return socket;
    }
    
    public BufferedReader getReader() {
        return in;
    }
    
    public BufferedWriter getWriter() {
        return out;
    }
    
    public void sendMessage(String message) throws IOException {
        out.write(message);
        out.flush();
    }
    
    public String readMessage() throws IOException {
        return in.readLine();
    }
    
    public void close() throws IOException {
        socket.close();
    }
}
