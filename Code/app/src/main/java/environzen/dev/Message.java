package environzen.dev;

import java.io.Serializable;

public class Message implements Serializable {

    public enum Type {
        WARNING,
        DELAY,
        NAVIGATION,
        ADD_STEPS,
        START_LOGGING,
        RESPONSE,
        INITIATE
    }

    private final Type type;
    private final int info;
    private final int subTask;

    public Message(Type type, int info) {
        this.type = type;
        this.info = info;
        this.subTask = 0;
    }

    public Message(Type type, int info, int subTask) {
        this.type = type;
        this.info = info;
        this.subTask = subTask;
    }

    public int getInfo() {
        return info;
    }

    public int getSubTask() {
        return subTask;
    }

    public Type getType() {
        return type;
    }
}
