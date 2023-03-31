package me.dmk.app.database;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dmk.app.configuration.DatabaseConfiguration;
import me.dmk.app.database.codec.DateCodec;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.*;

/**
 * Created by DMK on 29.03.2023
 */

@Getter
@Slf4j
@RequiredArgsConstructor
public class MongoClientService {

    private final DatabaseConfiguration databaseConfiguration;

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

    public void connect() {
        boolean auth = this.databaseConfiguration.isAuthentication();
        String userName = this.databaseConfiguration.getUserName();
        String password = this.databaseConfiguration.getPassword();
        String hostName = this.databaseConfiguration.getHostName();
        String databaseName = this.databaseConfiguration.getDatabaseName();
        int port = this.databaseConfiguration.getPort();

        String connectUrl = "mongodb://" + (auth ? userName + ":" + password + "@" : "") + hostName + ":" + port;

        PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder()
                .automatic(true)
                .build();

        CodecRegistry codecRegistry = fromRegistries(
                fromCodecs(new DateCodec()),
                getDefaultCodecRegistry(),
                fromProviders(pojoCodecProvider)
        );

        this.mongoClient = MongoClients.create(connectUrl);
        this.mongoDatabase = this.mongoClient.getDatabase(databaseName)
                .withCodecRegistry(codecRegistry);

        try {
            //Send ping to confirm a successful connection
            Bson ping = new BsonDocument("ping", new BsonInt64(1));
            this.mongoDatabase.runCommand(ping);
        } catch (MongoException exception) {
            log.error("Error while trying to connect database", exception);
        }
    }
}
