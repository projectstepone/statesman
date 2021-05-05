package io.appform.statesman.server.dao.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.MessageConfig;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.server.utils.MapperUtils;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Optional;


/**
 * This class is mainly responsible for constructing the message for a given
 * message id,language and state from the config defined
 */
@Slf4j
public class MessageConstructor implements IMessageConstructor {

    private final MessageConfigStoreCommand messageConfigStoreCommand;
    private final String DEFAULT_FILED_NAME = "default";

    @Inject
    public MessageConstructor(MessageConfigStoreCommand messageConfigStoreCommand) {
        this.messageConfigStoreCommand = messageConfigStoreCommand;
    }


    /**
     *
     * @param messageId : id for the message config
     * @param language : language in which the message needs to be build
     * @param state : state for which the message is intended
     * Example Json :
     *{
     *    "messageId":"welcome",
     *    "messageBody":{
     *       "default":{
     *          "KA":"msg1",
     *          "default":"msg2"
     *       },
     *       "hindi":{
     *          "RAJ":"msg3",
     *          "UP":"msg4",
     *          "default":"msg5"
     *       },
     *       "marathi":{
     *          "MAH":"msg6",
     *          "default":"msg7"
     *       }
     *    }
     * }
     * @return : Completely constructed message
     */
    @Override
    public String constructMessage(String messageId, String language, String state) {

        Optional<MessageConfig> optionalMessageConfig = messageConfigStoreCommand.get(messageId);
        if(!optionalMessageConfig.isPresent()){
            throw new StatesmanError("No message config found", ResponseCode.NO_PROVIDER_FOUND);
        }

        MessageConfig storeOutput = optionalMessageConfig.get();
        JsonNode root = storeOutput.getMessageBody();
        JsonNode languageNode;

        if(root == null){
            throw new StatesmanError("No message config found", ResponseCode.NO_PROVIDER_FOUND);
        }else if((root.get(language) == null)){
            if(root.get(DEFAULT_FILED_NAME) == null) {
                throw new StatesmanError("No default language found", ResponseCode.NO_PROVIDER_FOUND);
            }else{
                languageNode = root.get(DEFAULT_FILED_NAME);
            }
        }else{
            languageNode = root.get(language);
        }

        if(languageNode.get(state) != null) {
            return languageNode.get(state).asText();
        }else if(languageNode.get(DEFAULT_FILED_NAME) != null) {
            return languageNode.get(DEFAULT_FILED_NAME).asText();
        }else{
            return root.get(DEFAULT_FILED_NAME).get(DEFAULT_FILED_NAME).asText();
        }
    }
}


