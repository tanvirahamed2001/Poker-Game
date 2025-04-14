import java.awt.Color;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Arrays;
import java.util.List;
import shared.Player;
import shared.communication_objects.*;
import shared.Colors;

/**
 * The main client interface for the players
 * Will ask the player for their names and possible amount of funds to begin
 * playing with
 * Might need to make fund authorization a server side thing as well at some
 * point
 * After getting this information, attempts to connect to the servers.
 * These servers will be hosted on the linux systems of the UofC to keep them
 * consistent
 */
public class ClientMain {
    // Change SERVER_IPS when needed to do actual testing
    private static final List<String> SERVER_IPS = Arrays.asList("localhost");
    private static final int SERVER_PORT = 6834;
    private static ClientServerConnection serverConnection;
    private static Player player;
    private static int id;
    private static InTable table_info = new InTable(false, 0);
    private static boolean playing = false;
    private static LamportClock lamportClock;

    /**
     * Main function. Houses the initial connection logic and steps.
     * 
     * @param args
     */
    public static void main(String[] args) {
        lamportClock = new LamportClock();
        Scanner scanner = new Scanner(System.in);
        printWelcomeMessage();
        if (connectToServer()) {
            sendCommand(Command.Type.NEW, null);
            id = getIDFromServer();
            player = getPlayerInfo(scanner, id);
            sendCommand(Command.Type.PLAYER_INFO, player);
            handleGameSelection(scanner);
            Thread gameThread = new Thread(() -> playGame(scanner));
            gameThread.start();
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                printTerminalMessage(e.getLocalizedMessage());
            }
        } else {
            printTerminalMessage(Colors.RED + "Failed to connect to game servers..." + Colors.RESET);
        }
        scanner.close();
        serverConnection.closeConnections();
    }

    /**
     * Prints welcome message
     */
    private static void printWelcomeMessage() {
        System.out.println(Colors.PURPLE + Colors.BOLD + "*************************" + Colors.RESET);
        System.out.println(Colors.PURPLE + Colors.BOLD + "* Welcome to HoldemNet! *" + Colors.RESET);
        System.out.println(Colors.PURPLE + Colors.BOLD + "*************************" + Colors.RESET);
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
            int senderTS = cmd.getLamportTS();
            lamportClock.receievedEvent(senderTS);
            return id.getID();
        } else {
            return 0;
        }
    }

    /**
     * Gets the current players information
     * 
     * @param scanner for player input
     * @return Player Object
     */
    private static Player getPlayerInfo(Scanner scanner, int id) {
        printTerminalMessage("Enter your name...");
        String playerName = scanner.nextLine();
        int depositAmount;
        while (true) {
            printTerminalMessage("Enter initial deposit amount in dollars...");
            try {
                depositAmount = Integer.parseInt(scanner.nextLine());
                break;
            } catch (NumberFormatException e) {
                printTerminalMessage(Colors.RED + "Wrong input type, please deposit again..." + Colors.RESET);
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
        int failures = 0;
        for (String ip : SERVER_IPS) {
            try {
                Socket socket = new Socket(ip, SERVER_PORT);
                serverConnection = new ClientServerConnection(socket);
                printTerminalMessage(Colors.GREEN + "Connected to: " + ip + Colors.RESET);
                break;
            } catch (IOException e) {
                failures++;
            }
        }
        if (failures == SERVER_IPS.size()) {
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
            int ts = lamportClock.sendEvent();
            cmd.setLamportTS(ts);
            serverConnection.write(cmd);
        } catch (Exception e) {
            printTerminalMessage(Colors.RED + "Error sending command..." + Colors.RESET);
        }
    }

    /**
     * Handles reading in commands and seting lamport clocks
     * 
     * @return
     */
    private static Command readCommand() {
        Command cmd = (Command) serverConnection.read();
        if (cmd == null) {
            return null;
        }
        int senderTS = cmd.getLamportTS();
        lamportClock.receievedEvent(senderTS);
        return cmd;
    }

    /**
     * Handles receiving a GAME_LIST Command from the server
     * Proceeds to build a GAME_CHOICE Command and send to the server
     */
    private static void handleGameSelection(Scanner scanner) {
        sendCommand(Command.Type.INITIAL_CONN, null);
        printTerminalMessage("Waiting for available games...");
        Command serverResponse = readCommand();
        if (serverResponse.getType() == Command.Type.GAMES_LIST) {
            GameList games = (GameList) serverResponse.getPayload();
            printTerminalMessage(games.getGames());
        }
        printTerminalMessage("Enter game number or type 'new'...");
        GameChoice gc;
        if (scanner.hasNextInt()) {
            gc = new GameChoice(GameChoice.Choice.JOIN);
            gc.setId(scanner.nextInt());
        } else {
            gc = new GameChoice(GameChoice.Choice.NEW);
        }
        scanner.nextLine();
        sendCommand(Command.Type.GAME_CHOICE, gc);
        serverResponse = readCommand();
        printTerminalMessage(((Message) serverResponse.getPayload()).getMsg());
        serverResponse = readCommand();
        table_info = new InTable(true, ((TableInfo) serverResponse.getPayload()).getTableID());
        playing = true;
    }

    /**
     * Client side game playing logic. Runs on thread.
     * Waits for a Command object from the server and decides based on the payload
     * If null is received, error with input stream
     */
    private static void playGame(Scanner scanner) {
        try {
            while (playing) {
                Command serverResponse = readCommand();
                if (serverResponse == null) {
                    Thread.sleep(5000);
                    printTerminalMessage(
                            Colors.RED + "Server connection lost. Attempting to reconnect..." + Colors.RESET);
                    reconnectToServer();
                } else {
                    handleServerGameResponse(serverResponse, scanner);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                printTerminalMessage(Colors.RED + "Game Over!" + Colors.RESET);
                playing = false;
                break;
            case MESSAGE:
                printTerminalMessage(((Message) response.getPayload()).getMsg());
                break;
            case CLIENT_UPDATE_PLAYER:
                player = (Player) response.getPayload();
                break;
            case TURN_TOKEN:
                handleTurn(scanner);
                break;
            case REFUND:
                printTerminalMessage(Colors.RED + "A player left the table, you have been refunded. Join a new table..." + Colors.RESET);
                player = (Player) response.getPayload();
                handleGameSelection(scanner);
                break;
        }
    }

    /**
     * Once turn token is recieved, handle player input for that turn
     * 
     * @param scanner for player input
     */
    private static void handleTurn(Scanner scanner) {
        printTerminalMessage("Your turn! Available commands: Check, Call, Bet, Fold, Funds, Card.");
        while (true) {
            try {
                String input = scanner.nextLine().toUpperCase();
                TurnChoice.Choice choice = TurnChoice.Choice.valueOf(input);
                TurnChoice turnChoice = new TurnChoice(choice);
                if (choice == TurnChoice.Choice.BET) {
                    printTerminalMessage("Enter bet amount in dollars...");
                    turnChoice.betAmount(Integer.parseInt(scanner.nextLine()));
                }
                sendCommand(Command.Type.TURN_CHOICE, turnChoice);
                break;
            } catch (IllegalArgumentException e) {
                printTerminalMessage(Colors.RED + "Wrong input type, please bet again..." + Colors.RESET);
            }
        }
    }

    /**
     * Reconnect to server logic for when the connection is lost.
     * Closes connections to start then calls connectToServer to establish new
     * connection.
     */
    private static void reconnectToServer() {
        serverConnection.closeConnections();
        while (!connectToServer()) {
            printTerminalMessage(Colors.RED + "Reconnect attempt failed. Retrying..." + Colors.RESET);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
            }
        }
        printTerminalMessage(Colors.GREEN + "Reconnected successfully..." + Colors.RESET);
        sendCommand(Command.Type.RECONNECT, null);
        sendCommand(Command.Type.PLAYER_INFO, player);
        if (table_info.getIn()) {
            sendCommand(Command.Type.RECONNECT, table_info.getTableID());
        } else {
            handleGameSelection(new Scanner(System.in));
        }
    }
}
