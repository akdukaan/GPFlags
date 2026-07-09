package me.ryanhamshire.GPFlags;

public class MessageSpecifier {

    public Messages messageID;
    public String[] messageParams;

    public MessageSpecifier(Messages messageID, String... messageParams) {
        this.messageID = messageID;
        this.messageParams = messageParams;
    }

    public Messages getMessageID() {
        return messageID;
    }

    public String[] getMessageParams() {
        return messageParams;
    }
}
