import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientServerConnection {
    private Socket primarySocket;
    private ObjectInputStream primaryIn;
    private ObjectOutputStream primaryOut;

    public ClientServerConnection(Socket primarySocket) {
        try {
            this.primarySocket = primarySocket;
            this.primaryOut = new ObjectOutputStream(this.primarySocket.getOutputStream());
            this.primaryOut.flush();
            //I swear to fuck if this works
            this.primaryIn = new ObjectInputStream(this.primarySocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTimeout(int timeout) {
        try {
            primarySocket.setSoTimeout(timeout);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean connected() {
        if(primarySocket.isClosed() || !primarySocket.isConnected() || primarySocket == null) {
            return false;
        }
        return true;
    }

    public void write(Object obj) {
        try {
            primaryOut.writeObject(obj);
            primaryOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object read() {
        try {
            return primaryIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void closeConnections() {
        try {
            if (primaryIn != null) {
                primaryIn.close();
                System.out.println("Closed primary input..");
            }
            if (primaryOut != null) {
                 primaryOut.close();
                System.out.println("Closed primary output...");
            }
            if (primarySocket != null) {
                primarySocket.close();
                System.out.println("Closed primary replication listener...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
