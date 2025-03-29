
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
    private static ClientServerConnection serverConnection;
    private static Player player;
    private static int id;
    private static InTable table_info = new InTable(false, 0);

    public static void main(String[] args) {
        // Create a scanner for getting player input
        Scanner scanner = new Scanner(System.in);

        // Display welcome message for the client
        System.out.println("Welcome to Poker Game!");

        // Begin connection to server
        if (connectToServer()) {
            id = getIDFromServer();
            player = getPlayerInfo(scanner, id);
            System.out.println(String.format("Connected to the game server with name %s and funds %d!", player.get_name(), player.view_funds()));
            sendCommand(Command.Type.PLAYER_INFO, player);
            handleGameSelection(scanner);
            playGame(scanner);

        } else {
            System.out.println("Failed to connect to the server. Please try again later.");
        }
        serverConnection.closeConnections();
    }

    /**
     * Waits to get an ID assignment from the server, should have after connection is established
     * @return int ID
     */
    private static int getIDFromServer() {
        Command cmd = (Command) serverConnection.read();
        if(cmd.getPayload() instanceof ClientServerId) {
            ClientServerId id = (ClientServerId) cmd.getPayload();
            return id.getID();
        } else {
            return 0;
        }
    }

    /**
     * Gets the current players information
     * @param scanner
     * @return Player Object
     */
    private static Player getPlayerInfo(Scanner scanner, int id) {
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
        Player player = new Player(playerName, depositAmount, id);
        return player;
    }

    /**
     * Update the player with the new server variation
     * @param updatedPlayer sent from server
     */
    private static void updatePlayer(Player updatedPlayer) {
        player = updatedPlayer;
        System.out.println("Updated player information!]\n");
    }

    /**
     * Attempts to connect to the given server IP and port
     * Sets up the necessary streams for input and output
     * @return true or false
     */
    private static boolean connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to primary server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            serverConnection = new ClientServerConnection(socket);
            monitorConnection();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to primary server: " + e.getClass().getSimpleName() + " in ClientMain Line 119.");
            return false;
        }
    }

    /**
     * Handles sending commands to server
     */
    private static void sendCommand(Command.Type type, Object obj) {
        Command cmd = new Command(type, obj);
        try {
            serverConnection.write(cmd);
        } catch(Exception e) {
            System.err.println("Error sending command object: " + e.getClass().getSimpleName() + " in ClientMain Line 149.");
        }
    }

    /**
     * Handles receiving a GAME_LIST Command from the server
     * Proceeds to build a GAME_CHOICE Command and send to the server
     */
    private static void handleGameSelection(Scanner scanner) {
        sendCommand(Command.Type.INITIAL_CONN, null);
        System.out.println("Waiting for available games..."); 
        // build the server response command
        Command serverResponse = (Command) serverConnection.read();
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
        sendCommand(Command.Type.GAME_CHOICE, gc);
        serverResponse = (Command) serverConnection.read();
        System.out.println(((Message)serverResponse.getPayload()).getMsg());
        System.out.println("Waiting for server table information!");
        serverResponse = (Command) serverConnection.read();
        table_info = new InTable(true, ((TableInfo)serverResponse.getPayload()).getTableID());
    }

    /**
     * Client side game playing logic.
     * Waits for a Command object from the server and decides based on the payload
     * [Game Over -> Game has ended, should return to table browser]
     * [Message -> Print plain text]
     * [Turn Token -> Its our turn to play, decide on a move for the turn]
     */
    private static void playGame(Scanner scanner) {
        try {
            serverConnection.setTimeout(10000);
            while(true) {
                Command serverResponse = (Command) serverConnection.read();
                if(serverResponse.getType() == Command.Type.GAME_OVER) {
                    System.out.println("Game Over! Exiting!");
                    break;
                }
                if(serverResponse.getType() == Command.Type.MESSAGE) {
                    System.out.println(((Message)serverResponse.getPayload()).getMsg());
                    continue;
                }
                if(serverResponse.getType() == Command.Type.CLIENT_UPDATE_PLAYER) {
                    player = (Player)serverResponse.getPayload();
                    System.out.println("Updating player from server");
                    continue;
                }
                if(serverResponse.getType() == Command.Type.TURN_TOKEN) {
                    String allChoices = "It's your turn! Please enter a command! Available Commands Are: ";
                    for(TurnChoice.Choice c : TurnChoice.Choice.values()) {
                        allChoices += c.name() + ", ";
                    }
                    if (allChoices.endsWith(", ")) {
                        allChoices = allChoices.substring(0, allChoices.length() - 2);
                    }
                    System.out.println(allChoices);
                    String input;
                    while(true) {
                        try {
                        	input = scanner.nextLine().toUpperCase();
                            TurnChoice.Choice choice = TurnChoice.Choice.valueOf(input);
                            System.out.println("You chose: " + choice);
                            TurnChoice tc = new TurnChoice(choice);
                            if(choice == TurnChoice.Choice.BET) {
                                System.out.println("Enter bet amount: ");
                                int betAmount = Integer.parseInt(scanner.nextLine());
                                System.out.println("You bet: $" + betAmount);
                                tc.betAmount(betAmount);
                            }
                            sendCommand(Command.Type.TURN_CHOICE, tc);
                            break;
                        } catch (IllegalArgumentException e) {
                            System.out.println("Invalid choice. Please enter CHECK, CALL, BET, FOLD, FUNDS, CARD.");
                        }
                    }
                }
            }
        }catch (Exception e) {
            //System.out.println("Error during game communication: " + e.getMessage());
            //System.out.println("Attempting to reconnect...");
            //reconnectToServer();
            // Optionally, re-enter the game loop or reinitialize state.
        }
    }

    /**
     * Thread for monitoring the current establishes connection to the server.
     * Attemps to reconnect on detected failure
     */
    private static void monitorConnection() {
        new Thread(() -> {
            while (true) {
                try {
                    // Sleep a bit before checking the connection status.
                    Thread.sleep(5000);
                    // A simple check: if the socket is closed or not connected, trigger a reconnect.
                    if (serverConnection.connected()) {
                        throw new IOException("Connection lost");
                    }
                } catch (Exception e) {
                    System.out.println("Attempting to reconnect...");
                    reconnectToServer();
                    break;
                }
            }
        }).start();
    }
    
    /**
     * Reconnect to server logic for when the connection is lost.
     * Closes connections to start then calls connectToServer to establish new connection.
     */
    private static void reconnectToServer() {
        // Close any existing resources.
        serverConnection.closeConnections();
        serverConnection = null;
        boolean reconnected = connectToServer();
        if (reconnected) {
            System.out.println("Reconnected successfully!");
            sendCommand(Command.Type.PLAYER_INFO, player);
            Scanner scanner = new Scanner(System.in);
            if(table_info.getIn()) {
                System.out.println("In table before disconnect...attempting to join table " + table_info.getTableID());
                sendCommand(Command.Type.RECONNECT, table_info.getTableID());
                playGame(scanner);
            } else {
                System.out.println("Client was not in a table, getting table list");
                handleGameSelection(scanner);
                playGame(scanner);
            }
        } else {
            System.out.println("Reconnect attempt failed. Retrying...");
            try { Thread.sleep(3000); } catch (InterruptedException ie) {}
            reconnectToServer();
        }
    }
}
