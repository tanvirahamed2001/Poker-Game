//Accepts connections from clients and passes it over to a table
//Theoretically this could be scrapped and Main could be this, but I'm not sure if we need to do anything else in main yet
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