//Accepts connections from clients and passes it over to the table manager to find or create a table
//should only have one of these running at a time
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
					//TODO: Send the client a welcome message, with possible menu actions
					pool.submit(new ServerTableManager(socket));
				} catch (IOException e) {
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