package io.appform.statesman.server.dao.message;

public interface IMessageConstructor {
    String constructMessage(String messageId,String language,String state);
}
