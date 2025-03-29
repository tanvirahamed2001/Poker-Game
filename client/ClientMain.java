
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
    private static final String SERVER_ADDRESS = "localhost"; // Place blocker where we will put IP address UofC systems
                                                              // to keep it consistent
    private static final int SERVER_PORT = 6834; // Match server port
    private static ClientServerConnection serverConnection;
    private static Player player;
    private static int id;
    private static InTable table_info = new InTable(false, 0);
    private static boolean playing = false;

    public static void main(String[] args) {
        // Init a scanner
        Scanner scanner = new Scanner(System.in);
        // Display welcome message for the client
        printWelcomeMessage();
        // Begin connection to server
        if (connectToServer()) {
            // start monitoring the server connection with a monitor thread
            Thread monitorThread = new Thread(() -> monitorConnection());
            monitorThread.start();
            // get a id tag from the server
            id = getIDFromServer();
            // create the player object
            player = getPlayerInfo(scanner, id);
            printTerminalMessage(String.format("Connected to the game server with name %s and funds %d!",
                    player.get_name(), player.view_funds()));
            // send the player via a player info command
            sendCommand(Command.Type.PLAYER_INFO, player);
            // handle creating / selecting a table
            handleGameSelection(scanner);
            // main game thread
            Thread gameThread = new Thread(() -> playGame(scanner));
            gameThread.start();
            // wait for threads to be finished to close out the main thread
            try {
                gameThread.join();
                monitorThread.join();
            } catch (InterruptedException e) {
                printTerminalMessage(e.getLocalizedMessage());
            }
        } else {
            System.out.println("Failed to connect to the server. Please try again later.");
        }
        scanner.close();
        serverConnection.closeConnections();
    }

    /**
     * Prints welcome message
     */
    private static void printWelcomeMessage() {
        System.out.println("*********************************************");
        System.out.println("* Welcome to CPSC 559 Group 31s Poker Game! *");
        System.out.println("*********************************************");
    }

    /**
     * Prints a given string to the terminal for the client
     * 
     * @param msg
     */
    private static void printTerminalMessage(String msg) {
        System.out.println(msg);
    }

    /**
     * Waits to get an ID assignment from the server, should have after connection
     * is established
     * 
     * @return int ID
     */
    private static int getIDFromServer() {
        Command cmd = (Command) serverConnection.read();
        if (cmd.getPayload() instanceof ClientServerId) {
            ClientServerId id = (ClientServerId) cmd.getPayload();
            return id.getID();
        } else {
            return 0;
        }
    }

    /**
     * Gets the current players information
     * 
     * @param scanner
     * @return Player Object
     */
    private static Player getPlayerInfo(Scanner scanner, int id) {
        printTerminalMessage("Enter your name: ");
        String playerName = scanner.nextLine();
        int depositAmount;
        while (true) {
            printTerminalMessage("Enter initial deposit amount: $");
            try {
                depositAmount = Integer.parseInt(scanner.nextLine());
                break;
            } catch (NumberFormatException e) {
                printTerminalMessage("Invalid input! Please try again!");
            }

        }
        Player player = new Player(playerName, depositAmount, id);
        return player;
    }

    /**
     * Attempts to connect to the given server IP and port
     * Sets up the necessary streams for input and output
     * 
     * @return true or false
     */
    private static boolean connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to primary server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            serverConnection = new ClientServerConnection(socket);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to primary server: " + e.getClass().getSimpleName()
                    + " in ClientMain Line 119.");
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
        } catch (Exception e) {
            printTerminalMessage(String.format(
                    "Error sending command object: " + e.getClass().getSimpleName() + " in ClientMain Line 149."));
        }
    }

    /**
     * Handles receiving a GAME_LIST Command from the server
     * Proceeds to build a GAME_CHOICE Command and send to the server
     */
    private static void handleGameSelection(Scanner scanner) {
        sendCommand(Command.Type.INITIAL_CONN, null);
        printTerminalMessage("Waiting for available games...");
        // build the server response command
        Command serverResponse = (Command) serverConnection.read();
        // check if the response is of type GAMES_LIST
        if (serverResponse.getType() == Command.Type.GAMES_LIST) {
            GameList games = (GameList) serverResponse.getPayload();
            printTerminalMessage(games.getGames());
        }
        // create the game choice from the player
        printTerminalMessage("Enter game number or type 'new': ");
        GameChoice gc;
        if (scanner.hasNextInt()) {
            gc = new GameChoice(GameChoice.Choice.JOIN);
            gc.setId(scanner.nextInt());
        } else {
            gc = new GameChoice(GameChoice.Choice.NEW);
        }
        scanner.nextLine();
        sendCommand(Command.Type.GAME_CHOICE, gc);
        serverResponse = (Command) serverConnection.read();
        printTerminalMessage(((Message) serverResponse.getPayload()).getMsg());
        printTerminalMessage("Waiting for server table information!");
        serverResponse = (Command) serverConnection.read();
        table_info = new InTable(true, ((TableInfo) serverResponse.getPayload()).getTableID());
        playing = true;
    }

    /**
     * Client side game playing logic. Runs on thread.
     * Waits for a Command object from the server and decides based on the payload
     * If null is received, error with input stream
     */
    private static void playGame(Scanner scanner) {
        while (playing) {
            Command serverResponse = (Command) serverConnection.read();
            if (serverResponse != null) {
                printTerminalMessage("Recieved null from input stream, retrying");
            } else {
                handleServerGameResponse(serverResponse, scanner);
            }
        }
    }

    /**
     * If we get a non null server response, handle the command type
     * 
     * @param response non null server response
     * @param scanner  for playing input
     */
    private static void handleServerGameResponse(Command response, Scanner scanner) {
        switch (response.getType()) {
            case GAME_OVER:
                printTerminalMessage("Game Over!");
                playing = false;
                break;
            case MESSAGE:
                printTerminalMessage(((Message) response.getPayload()).getMsg());
                break;
            case CLIENT_UPDATE_PLAYER:
                player = (Player) response.getPayload();
                printTerminalMessage("Updaying Player Information");
                break;
            case TURN_TOKEN:
                handleTurn(scanner);
                break;
        }
    }

    /**
     * Once turn token is recieved, handle player input for that turn
     * 
     * @param scanner for player input
     */
    private static void handleTurn(Scanner scanner) {
        printTerminalMessage("Your turn! Available commands: CHECK, CALL, BET, FOLD, FUNDS, CARD.");
        while (true) {
            try {
                String input = scanner.nextLine().toUpperCase();
                TurnChoice.Choice choice = TurnChoice.Choice.valueOf(input);
                TurnChoice turnChoice = new TurnChoice(choice);
                if (choice == TurnChoice.Choice.BET) {
                    System.out.print("Enter bet amount: $");
                    turnChoice.betAmount(Integer.parseInt(scanner.nextLine()));
                }
                sendCommand(Command.Type.TURN_CHOICE, turnChoice);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid choice. Try again.");
            }
        }
    }

    /**
     * Thread for monitoring the current establishes connection to the server.
     * Attemps to reconnect on detected failure
     */
    private static void monitorConnection() {
        new Thread(() -> {
            while (playing) {
                try {
                    // Sleep a bit before checking the connection status.
                    Thread.sleep(5000);
                    // A simple check: if the socket is closed or not connected, trigger a
                    // reconnect.
                    if (!serverConnection.connected()) {
                        throw new IOException("Connection lost");
                    }
                } catch (Exception e) {
                    printTerminalMessage("Attempting to reconnect...");
                    reconnectToServer();
                }
            }
        }).start();
    }

    /**
     * Reconnect to server logic for when the connection is lost.
     * Closes connections to start then calls connectToServer to establish new
     * connection.
     */
    private static void reconnectToServer() {
        // Close any existing resources.
        serverConnection.closeConnections();
        // Loop till we manage to get a connection
        while (!connectToServer()) {
            printTerminalMessage("Reconnect attempt failed. Retrying...");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
            }
        }
        printTerminalMessage("Reconnected successfully!");
        sendCommand(Command.Type.PLAYER_INFO, player);
        if (table_info.getIn()) {
            printTerminalMessage("In table before disconnect...attempting to join table " + table_info.getTableID());
            sendCommand(Command.Type.RECONNECT, table_info.getTableID());
        } else {
            printTerminalMessage("Client was not in a table, getting table list");
            handleGameSelection(new Scanner(System.in));
        }
    }
}
