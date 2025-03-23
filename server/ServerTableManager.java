
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

    public ServerTableManager(PlayerConnection pc) {
        this.connection = pc;
    }
    
    @Override
    public void run() {
        int gameId = getInput();
        if (gameId > 0) {
            ServerMain.waitforGame(gameId, connection);
        } else if (gameId == 0) {
            gameId = ServerMain.addNewGame();
            connection.sendCommand(Command.Type.TABLE_INFO, new TableInfo(gameId));
            ServerMain.waitforGame(gameId, connection);
        } else {
            System.err.println("Invalid input or error occurred.");
        }
    }
    
    private int getInput() {
        try {
            Command response = (Command)connection.readCommand();;
            if(response.getType() == Command.Type.INITIAL_CONN) {
                String message = "Games Available: ";
                Integer[] keys = games.keySet().toArray(new Integer[]{});
                for (int i = 0; i < games.size(); i++) {
                    message += "Game " + keys[i] + ": " + games.get(keys[i]).size() + "/" 
                            + ServerMain.maxplayers + " Players\n";
                }
                message += "Please input the game number you would like to join, or type 'new' to create a new game.";
                connection.sendCommand(Command.Type.GAMES_LIST, new GameList(message));
                response = (Command) connection.readCommand();
                GameChoice gc = (GameChoice) response.getPayload();
                if(gc.getChoice() == GameChoice.Choice.NEW) {
                    Message msg = new Message("You have created a new table! Waiting for players...");
                    connection.sendCommand(Command.Type.MESSAGE, msg);
                    return 0;
                } else if (gc.getChoice() == GameChoice.Choice.JOIN) {
                    Message msg = new Message("Joined table " + gc.getId() + " waiting for game start...");
                    connection.sendCommand(Command.Type.MESSAGE, msg);
                    connection.sendCommand(Command.Type.TABLE_INFO, new TableInfo(gc.getId()));
                    return gc.getId();
                }
            } else if (response.getType() == Command.Type.RECONNECT) {
                int id = (int)response.getPayload();
                Message msg = new Message("Rejoined table " + id + " waiting for game start...");
                connection.sendCommand(Command.Type.MESSAGE, msg);
                return (int)response.getPayload();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
