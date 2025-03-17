import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.ArrayList;
import shared.Player;
import shared.PlayerConnection;
import shared.communication_objects.*;

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
            Command response;
            Integer[] keys = games.keySet().toArray(new Integer[]{});
            for (int i = 0; i < games.size(); i++) {
                message += "Game " + keys[i] + ": " + games.get(keys[i]).size() + "/" 
                           + ServerMain.maxplayers + " Players\n";
            }

            message += "Please input the game number you would like to join, or type 'new' to create a new game\nDone\n";

            connection.sendCommand(Command.Type.GAMES_LIST, new GameList(message));

            response = (Command) connection.readMessage();

            GameChoice gc = (GameChoice) response.getPayload();
            
            if(gc.getChoice() == GameChoice.Choice.NEW) {

                Message msg = new Message("You have created a new table! Waiting for players...\n");
                connection.sendCommand(Command.Type.MESSAGE, msg);
                return 0;

            } else if (gc.getChoice() == GameChoice.Choice.JOIN) {

                Message msg = new Message("Joined table " + gc.getId() + " waiting for game start...\n");
                connection.sendCommand(Command.Type.MESSAGE, msg);
                return gc.getId();

            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
