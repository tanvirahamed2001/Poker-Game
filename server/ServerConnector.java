//Accepts connections from clients and passes it over to the table manager to find or create a table
//should only have one of these running at a time
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import shared.Player;
public class ServerConnector implements Runnable {
	public void run() {
		ExecutorService pool = Executors.newCachedThreadPool();
		try 
		{ 
			ServerSocket connector = new ServerSocket(6834); 
			System.out.print("Waiting for connection...\n");
			while(true) {
				try {//accepts a new connection then hands it over to the ServerTableManager
					Socket socket = connector.accept();
					System.out.print("Someone connected...\n");
					ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
					Player info = (Player)input.readObject();
					pool.submit(new ServerTableManager(socket, info));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}