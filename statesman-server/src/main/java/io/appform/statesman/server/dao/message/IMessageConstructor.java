package io.appform.statesman.server.dao.message;

/**
 *  Interface for constructing the message for a given
 *  message id,language and state from the config defined
 */
public interface IMessageConstructor {
    String constructMessage(String messageId,String language,String state);
}
