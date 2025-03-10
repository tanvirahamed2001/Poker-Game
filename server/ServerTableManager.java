//takes threads that are accepted and shows them the options for tables
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import shared.Player;

public class ServerTableManager implements Runnable {
	Socket conn;
	BufferedWriter out;
	BufferedReader in;
	ObjectInputStream obj_in;
	ObjectOutputStream obj_out;
	static HashMap<Integer, ArrayList<Player>> games = ServerMain.getGames();
	Player player;

	public ServerTableManager(Socket newconn, Player player) {
		this.conn = newconn;
		this.player = player;
		this.player.socket = newconn;
		try {
			out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		int game = getInput();
		if(game > 0) {
			ServerMain.waitforGame(game, player);
		}
		else if(game == 0) {
			game = ServerMain.addNewGame();
			ServerMain.waitforGame(game, player);
		}
		else {
			//freak out cause there's an error, I dunno, TODO
		}
	}
	private int getInput() {//gets the input from the client, a non-zero number should be the game id, 0 should create a new game, and -1 means error
		try {
			String message ="Games Available:\n";
			String response;
			Integer[] keys = games.keySet().toArray(new Integer[]{}); //gets the keys within the map
			for(int i = 0; i < games.size(); i++) {
				message += "Game " + keys[i] + ": " + games.get(keys[i]).size() + "/" + ServerMain.maxplayers + " Players" + "\n"; //Should be something like "Game X: X/X Players"
			}
			message += "Please input the game number you would like to join, or type 'new' to create a new game\nDone\n"; //use Done to signal to the client that it's done sending stuff
			out.write(message);
			out.flush();
			response = in.readLine();//should be a number or "new"
			try {
				int table = Integer.parseInt(response);
				return table;
			} catch(NumberFormatException e) {
				if(response.equals("new")) {
					return 0;
				}
				else {
					out.write("Invalid Input! Please Try Again\nDone\n");
					return getInput();
				}	
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;//shouldn't reach here unless something went wrong, probably an IOException
	}
}