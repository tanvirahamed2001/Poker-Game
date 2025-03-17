/**
 * object to hande basic messages instead of using BufferedReaders and Writers
 */

package shared.communication_objects;

public class Message {

    private String text;

    public Message(String text) {
        this.text = text;
    }

    public String getMsg(){
        return this.text;
    }

}
