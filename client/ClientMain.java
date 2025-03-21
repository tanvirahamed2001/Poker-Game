// The main client application for our players
// Will ask the player for their names and possible amount of funds to begin playing with
// Might need to make fund authorization a server side thing as well at some point
// After getting this information, attempts to connect to the servers.
// These servers will be hosted on the linux systems of the UofC to keep them consistent

import java.io.*;
import java.net.*;
import java.util.Scanner;
import shared.Player;
import shared.communication_objects.*;

public class ClientMain {
    private static final String SERVER_ADDRESS = "localhost"; // Place blocker where we will put IP address UofC systems to keep it consistent
    private static final int SERVER_PORT = 6834; // Match server port
    private static final int BACKUP_PORT = 6836; // BACKUP PORT
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static Player player;

    public static void main(String[] args) {
        // Create a scanner for getting player input
        Scanner scanner = new Scanner(System.in);

        // Display welcome message for the client
        System.out.println("Welcome to Poker Game!");

        // Create the player
        player = getPlayerInfo(scanner);
        
        // Begin connection to server
        if (connectToServer()) {

            // Print connection completed message for the client
            System.out.println(String.format("Connected to the game server with name %s and funds %d!", player.get_name(), player.view_funds()));

            // Send player data to server
            sendCommand(Command.Type.PLAYER_INFO, player);

            // Step 5: Choose or create a game
            handleGameSelection(scanner);

            // play the game
            playGame(scanner);

        } else {
            System.out.println("Failed to connect to the server. Please try again later.");
        }
        // Close resources when done
        closeConnection();
    }

    /**
     * Gets the current players information
     * @param scanner
     * @return Player
     */
    private static Player getPlayerInfo(Scanner scanner) {
        System.out.print("Enter your name: ");
        String playerName = scanner.nextLine();
        int depositAmount;
        while(true) {
        	System.out.print("Enter initial deposit amount: $");
        	try {
        		depositAmount = Integer.parseInt(scanner.nextLine());
        		break;
        	}catch(NumberFormatException e) {
        		System.out.println("Invalid input! Please try again!");
        	}
            
        }

        Player player = new Player(playerName, depositAmount);
        
        return player;
    }

    /**
     * Get generic object from the server. 
     * Can be Players at this point.
     * Update as we expand project.
     * TODO: Thread this as a possible listener
     */
    private static void getObjectData() {
        try {
            Object obj = in.readObject();
            if(obj instanceof Player) {
                updatePlayer((Player)obj);
            }
        } catch(IOException e) {
            //TODO: combine or keep seperate with generic expections
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the player with the new server variation
     * @param updatedPlayer
     */
    private static void updatePlayer(Player updatedPlayer) {
        player = updatedPlayer;
        System.out.println("Updated player information!]\n");
    }

    /**
     * Attempts to connect to the given server IP and port
     * Sets up the necessary streams for input and output
     * @return
     */
    private static boolean connectToServer() {
        try {
            // Attempt primary connection
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to primary server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
        } catch (IOException e) {
            System.err.println("Failed to connect to primary server: " + e.getMessage());
            try {
                // Attempt connection to backup
                socket = new Socket(SERVER_ADDRESS, BACKUP_PORT);
                System.out.println("Connected to backup server at " + SERVER_ADDRESS + ":" + BACKUP_PORT);
            } catch (IOException e2) {
                System.err.println("Failed to connect to backup server: " + e2.getMessage());
                return false;
            }
        }
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    /**
     * Handles sending commands to server
     */
    private static void sendCommand(Command.Type type, Object obj) {
        Command cmd = new Command(type, obj);
        try {
            out.writeObject(cmd);
            out.flush();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles receiving a GAME_LIST Command from the server
     * Proceeds to build a GAME_CHOICE Command and send to the server
     */
    private static void handleGameSelection(Scanner scanner) {

        System.out.println("Waiting for available games...");

        try {
            
            // build the server response command
            Command serverResponse = (Command)in.readObject();

            // check if the response is of type GAMES_LIST
            if(serverResponse.getType() == Command.Type.GAMES_LIST) {
                GameList games = (GameList) serverResponse.getPayload();
                System.out.println(games.getGames());
            }

            // create the game choice from the player
            System.out.print("Enter game number or type 'new': ");

            GameChoice gc;
            
            if(scanner.hasNextInt()) {
                gc = new GameChoice(GameChoice.Choice.JOIN);
                gc.setId(scanner.nextInt());
            } else {
                gc = new GameChoice(GameChoice.Choice.NEW);
            }

            scanner.nextLine();

            // send the command
            sendCommand(Command.Type.GAME_CHOICE, gc);

            serverResponse = (Command)in.readObject();

            System.out.println(((Message)serverResponse.getPayload()).getMsg());

        } catch (IOException e) {

            System.err.println("Error receiving game selection: " + e.getMessage());

        } catch (ClassNotFoundException e2) {

            System.err.println("Error with game list object: " + e2.getMessage());

        }
    }

    private static void closeConnection() {
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
            System.out.println("Disconnected from server.");
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    private static void playGame(Scanner scanner) {
        try {
            socket.setSoTimeout(600000);
            while (true) {
                Command serverResponse = (Command) in.readObject();
                // Process the command normally.
                if (serverResponse.getType() == Command.Type.GAME_OVER) {
                    System.out.println("Game Over! Exiting!");
                    break;
                }
                if (serverResponse.getType() == Command.Type.MESSAGE) {
                    System.out.println(((Message) serverResponse.getPayload()).getMsg());
                    continue;
                }
                // ... additional processing ...
            }
        } catch (Exception e) {
            System.out.println("Error during game communication: " + e.getMessage());
            System.out.println("Attempting to reconnect...");
            reconnectToServer();
            // Optionally, re-enter the game loop or reinitialize state.
        }
    }

    private static void monitorConnection() {
        new Thread(() -> {
            while (true) {
                try {
                    // Sleep a bit before checking the connection status.
                    Thread.sleep(5000);
                    // A simple check: if the socket is closed or not connected, trigger a reconnect.
                    if (socket == null || socket.isClosed() || !socket.isConnected()) {
                        throw new IOException("Connection lost");
                    }
                } catch (Exception e) {
                    System.out.println("Connection lost. Attempting to reconnect...");
                    reconnectToServer();
                }
            }
        }).start();
    }
    
    private static void reconnectToServer() {
        // Close any existing resources.
        closeConnection();
        boolean reconnected = connectToServer();
        if (reconnected) {
            System.out.println("Reconnected successfully!");
            // Optionally reinitialize or notify the game logic about reconnection.
        } else {
            System.out.println("Reconnect attempt failed. Retrying...");
            // Optionally wait before trying again.
            try { Thread.sleep(3000); } catch (InterruptedException ie) {}
            reconnectToServer();
        }
    }
}
