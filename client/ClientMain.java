// The main client application for our players
// Will ask the player for their names and possible amount of funds to begin playing with
// Might need to make fund authorization a server side thing as well at some point
// After getting this information, attempts to connect to the servers.
// These servers will be hosted on the linux systems of the UofC to keep them consistent

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientMain {
    private static final String SERVER_ADDRESS = "127.0.0.1"; // Change to actual server IP
    private static final int SERVER_PORT = 6834; // Match server port
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Poker Game!");
        
        // Step 1: Ask for player name
        System.out.print("Enter your name: ");
        String playerName = scanner.nextLine();
        
        // Step 2: Handle deposit logic (optional, discuss server-side handling)
        System.out.print("Enter initial deposit amount: $");
        double depositAmount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline
        
        //player to house data
        Player player = new Player(playerName, depositAmount);

        //TODO: compress player to send over network

        // Step 3: Attempt to connect to the server
        if (connectToServer()) {
            System.out.println("Connected to the game server!");
            sendMessage(playerName + " has joined with $" + depositAmount);
            
            // Step 4: Choose or create a game
            handleGameSelection(scanner);
        } else {
            System.out.println("Failed to connect to the server. Please try again later.");
        }
    }

    private static boolean connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
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
    
    private static void handleGameSelection(Scanner scanner) {
        try {
            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                if (serverResponse.equals("Done")) break;
                System.out.println(serverResponse);
            }
            System.out.print("Enter game number or type 'new': ");
            String choice = scanner.nextLine();
            sendMessage(choice);
        } catch (IOException e) {
            System.err.println("Error receiving game selection: " + e.getMessage());
        }
    }
}
