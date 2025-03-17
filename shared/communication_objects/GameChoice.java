/**
 * this class is to mimic the client making a choice of creating a 
 * NEW table or to JOIN a table in progress
 */

package shared.communication_objects;

import java.io.Serializable;

public class GameChoice implements Serializable{

    public enum Choice {NEW, JOIN}

    private Choice choice;
    private int tableId;
    
    public GameChoice(Choice choice) {
        this.choice = choice;
    }

    public void setId(int id) {
        this.tableId = id;
    }

    public int getId() {
        return this.tableId;
    }

    public Choice getChoice() {
        return this.choice;
    }
    
}
