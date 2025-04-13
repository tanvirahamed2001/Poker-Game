import java.util.HashMap;
import java.util.ArrayList;
import shared.communication_objects.*;

/**
 * Manages individual player choices when it comes to creating, or joining poker
 * tables
 */
public class ServerTableManager implements Runnable {
    private PlayerConnection connection;
    private static HashMap<Integer, ArrayList<PlayerConnection>> games = ServerMain.getGames();
    private static boolean recon;

    /**
     * Constructor for table manager
     * 
     * @param pc PlayerConnection
     */
    public ServerTableManager(PlayerConnection pc) {
        this.connection = pc;
    }

    /**
     * Thread for running the table manager
     */
    @Override
    public void run() {
        int gameId = getInput();
        if (gameId > 0) {
            ServerMain.waitforGame(gameId, connection, recon);
        } else if (gameId == 0) {
            gameId = ServerMain.addNewGame();
            connection.sendCommand(Command.Type.TABLE_INFO, new TableInfo(gameId));
            ServerMain.waitforGame(gameId, connection, recon);
        } else {
            System.err.println("Invalid input or error occurred...");
        }
    }

    /**
     * Gets the players choice when it comes to a table.
     * New -> Puts the player into a new table with an new id
     * Join -> Joins a table with a given tablei d
     * Reconnect -> Happens on Primary Server Death, allows the player to reconnect
     * to the old table
     * 
     * @return int table choice
     */
    private int getInput() {
        try {
            recon = false;
            Command response = (Command) connection.readCommand();
            if (response.getType() == Command.Type.INITIAL_CONN) {
                String message = "Games Available: ";
                Integer[] keys = games.keySet().toArray(new Integer[] {});
                for (int i = 0; i < games.size(); i++) {
                    message += "Game " + keys[i] + ": " + games.get(keys[i]).size() + "/"
                            + ServerMain.maxplayers + " Players\n";
                }
                connection.sendCommand(Command.Type.GAMES_LIST, new GameList(message));
                response = (Command) connection.readCommand();
                GameChoice gc = (GameChoice) response.getPayload();
                if (gc.getChoice() == GameChoice.Choice.NEW) {
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
                int id = (int) response.getPayload();
                recon = true;
                return id;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
