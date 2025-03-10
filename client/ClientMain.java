// The main client application for our players
// Will ask the player for their names and possible amount of funds to begin playing with
// Might need to make fund authorization a server side thing as well at some point
// After getting this information, attempts to connect to the servers.
// These servers will be hosted on the linux systems of the UofC to keep them consistent

import java.io.*;
import java.net.*;
import java.util.Scanner;
import shared.Player;

public class ClientMain {
    private static final String SERVER_ADDRESS = "localhost"; // Place blocker where we will put IP address UofC systems to keep it consistent
    private static final int SERVER_PORT = 6834; // Match server port
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static ObjectOutputStream obj_out_stream;
    private static ObjectInputStream obj_in_stream;
    private static Player player;

    public static void main(String[] args) {
        // Create a scanner for getting player input
        Scanner scanner = new Scanner(System.in);

        // Display welcome message for the client
        System.out.println("Welcome to Poker Game!\n");

        // Consume the newLine
        //scanner.nextLine(); 

        // Create the player
        player = getPlayerInfo(scanner);
        
        // Begin connection to server
        if (connectToServer()) {

            // Print connection completed message for the client
            System.out.println(String.format("Connected to the game server with name %s and funds %d!", player.get_name(), player.view_funds()));

            // Send player data to server
            sendPlayerData();

            // TODO: Tim -> interact with the table manager to create / join a table. Start a game of poker once William is finished the game logic on the server side. Implement any missing classes / logic.

            // Step 5: Choose or create a game
            handleGameSelection(scanner);
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
        
        
        return new Player(playerName, depositAmount, null);
    }

    /**
     * Sends the current state of the player to the server
     */
    private static void sendPlayerData() {
        try {
            obj_out_stream.writeObject(player);
            obj_out_stream.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get generic object from the server. 
     * Can be Players at this point.
     * Update as we expand project.
     * TODO: Thread this as a possible listener
     */
    private static void getObjectData() {
        try {
            Object obj = obj_in_stream.readObject();
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

    // Old implementation for when we used double
    private static double getValidDepositAmount(Scanner scanner) {
        double amount;
        while (true) {
            System.out.print("Enter initial deposit amount: $");
            try {
                amount = Double.parseDouble(scanner.nextLine().trim());
                if (amount >= 0) break;
                System.out.println("Deposit amount must be non-negative.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a valid number.");
            }
        }
        return amount;
    }

    /**
     * Attempts to connect to the given server IP and port
     * Sets up the necessary streams for input and output
     * @return
     */
    private static boolean connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            obj_out_stream = new ObjectOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        } catch (IOException e) {
            System.err.println("Error: Unable to connect to server - " + e.getMessage());
            return false;
        }
    }
    private static void sendMessage(String message) {
        //TODO: Finish sendMessage, actually send to the server
        if (out != null) {
            out.println(message);
        }
    }

    private static void startListenerThread() {
        Thread listener = new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println("Server: " + serverMessage);
                }
            } catch (IOException e) {
                System.err.println("Connection lost: " + e.getMessage());
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    private static void handleGameSelection(Scanner scanner) {
        System.out.println("Waiting for available games...");
        try {
            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                if (serverResponse.equals("Done")) break;
                System.out.println(serverResponse);
            }

            // Get user input for game selection
            System.out.print("Enter game number or type 'new': ");
            String choice = scanner.nextLine().trim();
            sendMessage(choice);

        } catch (IOException e) {
            System.err.println("Error receiving game selection: " + e.getMessage());
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
}
