import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.ArrayList;
import shared.Player;
import shared.PlayerConnection;

public class ServerTableManager implements Runnable {
    private PlayerConnection connection;
    private static HashMap<Integer, ArrayList<PlayerConnection>> games = ServerMain.getGames();

    public ServerTableManager(Socket socket, Player player) {
        try {
            this.connection = new PlayerConnection(player, socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        int gameId = getInput();
        if (gameId > 0) {
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
            Integer[] keys = games.keySet().toArray(new Integer[]{});
            for (int i = 0; i < games.size(); i++) {
                message += "Game " + keys[i] + ": " + games.get(keys[i]).size() + "/" 
                           + ServerMain.maxplayers + " Players\n";
            }
            message += "Please input the game number you would like to join, or type 'new' to create a new game\nDone\n";
            connection.sendMessage(message);
            response = (String) connection.readMessage();
            
            try {
                int table = Integer.parseInt(response);
                return table;
            } catch (NumberFormatException e) {
                if(response.equalsIgnoreCase("new")) {
                    message = "Started new table, waiting for another player!\n";
                    connection.sendMessage(message);
                    return 0;
                } else {
                    connection.sendMessage("Invalid Input! Please Try Again\nDone\n");
                    return getInput();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
