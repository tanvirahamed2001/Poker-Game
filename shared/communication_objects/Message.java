/**
 * object to hande basic messages instead of using BufferedReaders and Writers
 */

package shared.communication_objects;

import java.io.Serializable; 

public class Message implements Serializable {

    private String text;

    public Message(String text) {
        this.text = text;
    }

    public String getMsg(){
        return this.text;
    }

}
