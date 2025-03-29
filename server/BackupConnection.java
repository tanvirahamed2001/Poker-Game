import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class BackupConnection {
    private Socket backupSocket;
    private ObjectOutputStream backupOut;
    private ObjectInputStream backupIn;

public BackupConnection(Socket backupSocket) {
        try {
            this.backupSocket = backupSocket;
            this.backupOut = new ObjectOutputStream(this.backupSocket.getOutputStream());
            this.backupOut.flush();
            this.backupIn = new ObjectInputStream(this.backupSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTimeout(int timeout) {
        try {
            backupSocket.setSoTimeout(timeout);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void write(Object obj) {
        try {
            backupOut.writeObject(obj);
            backupOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object read() {
        try {
            return backupIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void closeConnections() {
        try {
            if (backupIn != null) {
                backupIn.close();
                System.out.println("Closed backup input..");
            }
            if (backupOut != null) {
                backupOut.close();
                System.out.println("Closed backup output...");
            }
            if (backupSocket != null) {
                backupSocket.close();
                System.out.println("Closed backup listener...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
