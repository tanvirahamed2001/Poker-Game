//Accepts connections from clients and passes it over to the table manager to find or create a table
//should only have one of these running at a time
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
public class ServerConnector implements Runnable {
	public void run() {
		ExecutorService pool = Executors.newCachedThreadPool();
		while(true) {
			
			try {//accepts a new connection then hands it over to the ServerTableManager
				ServerSocket connector = new ServerSocket(6834); //arbitrary port
				Socket socket = connector.accept();
				pool.submit(new ServerTableManager(socket));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}