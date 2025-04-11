package shared.communication_objects;

import java.io.Serializable;

/**
 * Utility class for sending Client Server IDs over Socket.
 * Combined with the Command Object.
 */
public class ClientServerId implements Serializable {

    private int id;

    /**
     * Constructor
     * 
     * @param id for the client
     */
    public ClientServerId(int id) {
        this.id = id;
    }

    /**
     * Returns the ID
     * 
     * @return the id of the client
     */
    public int getID() {
        return this.id;
    }

}
