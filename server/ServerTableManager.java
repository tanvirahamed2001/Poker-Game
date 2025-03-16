import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.ArrayList;
import shared.Player;
import shared.PlayerConnection;

public class ServerTableManager implements Runnable {
    private PlayerConnection connection;
    // Update the type of the games map to use PlayerConnection.
    private static HashMap<Integer, ArrayList<PlayerConnection>> games = ServerMain.getGames();

    public ServerTableManager(Socket socket, Player player) {
        try {
            // Create a PlayerConnection that couples the Player with its socket/streams.
            this.connection = new PlayerConnection(player, socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        int gameId = getInput();
        if (gameId > 0) {
            // Now pass the connection instead of connection.getPlayer()
            ServerMain.waitforGame(gameId, connection);
        } else if (gameId == 0) {
            gameId = ServerMain.addNewGame();
            ServerMain.waitforGame(gameId, connection);
        } else {
            System.err.println("Invalid input or error occurred.");
        }
    }
    
    private int getInput() {
        try {
            String message = "Games Available:\n";
            String response;
            // Get the keys from the games map.
            Integer[] keys = games.keySet().toArray(new Integer[]{});
            for (int i = 0; i < games.size(); i++) {
                message += "Game " + keys[i] + ": " + games.get(keys[i]).size() + "/" 
                           + ServerMain.maxplayers + " Players\n";
            }
            message += "Please input the game number you would like to join, or type 'new' to create a new game\nDone\n";
            connection.sendMessage(message);
            response = connection.readMessage(); 
            
            try {
                int table = Integer.parseInt(response);
                return table;
            } catch (NumberFormatException e) {
                if(response.equalsIgnoreCase("new")) {
                    return 0;
                } else {
                    connection.sendMessage("Invalid Input! Please Try Again\nDone\n");
                    return getInput(); 
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
