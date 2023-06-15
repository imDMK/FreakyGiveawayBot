package me.dmk.app.giveaway;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import lombok.Getter;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.util.EmojiUtil;
import me.dmk.app.util.GiveawayUtil;
import org.bson.conversions.Bson;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;

/**
 * Created by DMK on 29.03.2023
 */

@Getter
public class GiveawayManager {

    private final MongoCollection<Giveaway> giveawayMongoCollection;
    private final ExecutorService executorService;

    public GiveawayManager(MongoDatabase mongoDatabase, ExecutorService executorService) {
        this.giveawayMongoCollection = mongoDatabase.getCollection("giveaways", Giveaway.class);
        this.executorService = executorService;
    }

    public CompletableFuture<Optional<Giveaway>> find(long messageId) {
        return CompletableFuture.supplyAsync(() ->
                        Optional.ofNullable(
                                this.giveawayMongoCollection.find(this.getGiveawayFilter(messageId)).first()
                        ),
                this.executorService
        );
    }

    public CompletableFuture<Void> add(Giveaway giveaway) {
        return CompletableFuture.runAsync(() ->
                        this.giveawayMongoCollection.insertOne(giveaway),
                this.executorService
        );
    }

    public CompletableFuture<Void> update(Giveaway giveaway, Bson update) {
        return CompletableFuture.runAsync(() ->
                        this.giveawayMongoCollection.updateOne(this.getGiveawayFilter(giveaway), update),
                this.executorService
        );
    }

    public CompletableFuture<Void> delete(Giveaway giveaway) {
        return CompletableFuture.runAsync(() ->
                        this.giveawayMongoCollection.deleteOne(this.getGiveawayFilter(giveaway)),
                this.executorService
        );
    }

    public CompletableFuture<Void> addUserToGiveaway(Giveaway giveaway, long userId) {
        Bson updates = Updates.push("participants", userId);

        return CompletableFuture.runAsync(() ->
                        this.giveawayMongoCollection.updateOne(this.getGiveawayFilter(giveaway), updates),
                this.executorService
        );
    }

    public CompletableFuture<Void> removeUserFromGiveaway(Giveaway giveaway, long userId) {
        Bson updates = Updates.pull("participants", userId);

        return CompletableFuture.runAsync(() ->
                        this.giveawayMongoCollection.updateOne(this.getGiveawayFilter(giveaway), updates),
                this.executorService
        );
    }

    public List<Giveaway> getExpiredGiveaways() {
        long time = Instant.now().plusSeconds(1).plusMillis(500).toEpochMilli();

        Bson filter = and(lt("expireAt", time), eq("ended", false));

        return this.giveawayMongoCollection
                .find(filter)
                .into(new ArrayList<>());
    }

    public void endGiveaway(Giveaway giveaway, Server server, Message message) {
        List<String> selectedWinners = GiveawayUtil.selectWinners(
                giveaway.getParticipants(),
                giveaway.getWinners()
        );

        if (selectedWinners.isEmpty()) {
            EmbedMessage embedMessage = new EmbedMessage(server).error();
            embedMessage.setDescription(
                    "Zbyt mało osób wzięło udział w konkursie, aby rozlosować zwycięzców.",
                    "Wzięło udział: " + giveaway.getParticipants().size(),
                    "Wymagane: " + giveaway.getWinners()
            );

            new MessageBuilder()
                    .setEmbed(embedMessage)
                    .replyTo(message)
                    .send(message.getChannel())
                    .exceptionally(ExceptionLogger.get());

            this.delete(giveaway);
            return;
        }

        Bson updates = Updates.set("ended", true);

        this.update(giveaway, updates)
                .thenAcceptAsync(unused -> {
                    EmbedBuilder embedBuilder = new EmbedMessage(server).giveaway()
                            .setTitle(EmojiUtil.getPartyEmoji() + " Zakończony konkurs")
                            .addField("Zwycięzcy", String.join("\n", selectedWinners))
                            .addField("Nagroda", giveaway.getAward())
                            .addField("Wzięło udział", String.valueOf(giveaway.getParticipants().size()));

                    message.createUpdater()
                            .setEmbed(embedBuilder)
                            .setContent(String.join(", ", selectedWinners))
                            .removeAllComponents()
                            .applyChanges()
                            .exceptionally(ExceptionLogger.get());
                })
                .exceptionallyAsync(throwable -> {
                    EmbedBuilder embedBuilder = new EmbedMessage(server).error();

                    embedBuilder.setDescription("Wystąpił błąd podczas rozlosowywania zwycięzców.");
                    embedBuilder.addField("Szczegóły", throwable.getMessage());

                    new MessageBuilder()
                            .setEmbed(embedBuilder)
                            .replyTo(message)
                            .send(message.getChannel())
                            .exceptionally(ExceptionLogger.get());

                    throwable.printStackTrace();
                    return null;
                });
    }

    public Bson getGiveawayFilter(Giveaway giveaway) {
        return Filters.eq("messageId", giveaway.getMessageId());
    }

    public Bson getGiveawayFilter(long messageId) {
        return Filters.eq("messageId", messageId);
    }
}
