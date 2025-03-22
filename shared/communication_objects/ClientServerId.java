package shared.communication_objects;

import java.io.Serializable;

public class ClientServerId implements Serializable {

    private int id;

    public ClientServerId(int id) {
        this.id = id;
    }
    
    public int getID() {
        return this.id;
    }
    
}
