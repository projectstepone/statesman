package io.appform.statesman.server.dao.message;

import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;


/**
 * Entity that represents the message config that needs to stored and fetched from the DB
 */
@Entity
@Table(name = "message_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = "message_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoredMessageConfig {

    @Id
    @Column(name = "message_id")
    @LookupKey
    private String messageId;

    @Column(name = "message_config_body")
    private byte[] messageConfigBody;
}
