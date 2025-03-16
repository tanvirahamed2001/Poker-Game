import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import shared.Player;

// Handles assigning players to tables and manages socket communication
public class ServerTableManager implements Runnable {
    private Socket conn;
    private BufferedWriter out;
    private BufferedReader in;
    private static HashMap<Integer, ArrayList<Player>> games = ServerMain.getGames();
    private Player player;

    public ServerTableManager(Socket newConn, Player player) {
        this.conn = newConn;
        this.player = player;
        try {
            out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        int game = getInput();
        if (game > 0) {
            ServerMain.waitforGame(game, player);
        } else if (game == 0) {
            game = ServerMain.addNewGame();
            ServerMain.waitforGame(game, player);
        } else {
            // Handle error case
            System.err.println("Invalid input or error occurred.");
        }
    }

    private int getInput() {
        try {
            String message = "Games Available:\n";
            String response;
            Integer[] keys = games.keySet().toArray(new Integer[]{}); // Get the keys within the map
            for (int i = 0; i < games.size(); i++) {
                message += "Game " + keys[i] + ": " + games.get(keys[i]).size() + "/" + ServerMain.maxplayers + " Players\n";
            }
            message += "Please input the game number you would like to join, or type 'new' to create a new game\nDone\n";
            out.write(message);
            out.flush();
            response = in.readLine(); // Read client response

            try {
                int table = Integer.parseInt(response);
                return table;
            } catch (NumberFormatException e) {
                if (response.equalsIgnoreCase("new")) {
                    return 0; // Create a new game
                } else {
                    out.write("Invalid Input! Please Try Again\nDone\n");
                    out.flush();
                    return getInput(); // Retry input
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1; // Error case
    }
}