package me.dmk.app.giveaway;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.Getter;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.util.EmojiUtil;
import me.dmk.app.util.GiveawayUtil;
import org.bson.conversions.Bson;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.mongodb.client.model.Filters.*;

/**
 * Created by DMK on 29.03.2023
 */

@Getter
public class GiveawayManager extends GiveawayMap {

    private final MongoCollection<Giveaway> giveawayMongoCollection;
    private final ExecutorService executorService;

    public GiveawayManager(MongoDatabase mongoDatabase, ExecutorService executorService) {
        this.giveawayMongoCollection = mongoDatabase.getCollection("giveaways", Giveaway.class);
        this.executorService = executorService;
    }

    public CompletableFuture<Void> insertOne(Giveaway giveaway) {
        return CompletableFuture.supplyAsync(() -> {
            this.giveawayMongoCollection.insertOne(giveaway);
            this.put(giveaway);

            return null;
        }, this.executorService);
    }

    public CompletableFuture<Void> addUserToGiveaway(Giveaway giveaway, long userId) {
        return CompletableFuture.supplyAsync(() -> {
            UpdateResult updateResult = this.giveawayMongoCollection.updateOne(
                    this.getFilter(giveaway.getMessageId()),
                    Updates.push("participants", userId)
            );

            if (updateResult.wasAcknowledged()) {
                giveaway.addParticipant(userId);
            }

            return null;
        }, this.executorService);
    }

    public CompletableFuture<Void> removeUserFromGiveaway(Giveaway giveaway, long userId) {
        return CompletableFuture.supplyAsync(() -> {
            UpdateResult updateResult = this.giveawayMongoCollection.updateOne(
                    this.getFilter(giveaway.getMessageId()),
                    Updates.pull("participants", userId)
            );

            if (updateResult.wasAcknowledged()) {
                giveaway.removeParticipant(userId);
            }

            return null;
        }, this.executorService);
    }

    public void updateOne(Giveaway giveaway, Bson update) {
        CompletableFuture.supplyAsync(() -> {
            this.giveawayMongoCollection.updateOne(
                    this.getFilter(giveaway.getMessageId()),
                    update
            );

            return null;
        }, this.executorService);
    }

    public void deleteOne(Giveaway giveaway) {
        CompletableFuture.supplyAsync(() -> {
            DeleteResult deleteResult = this.giveawayMongoCollection.deleteOne(
                    this.getFilter(giveaway.getMessageId())
            );

            if (deleteResult.wasAcknowledged()) {
                this.remove(giveaway);
            }

            return null;
        }, this.executorService);
    }

    public Optional<Giveaway> getOrElseFind(long messageId) {
        if (this.get(messageId).isPresent()) {
            return this.get(messageId);
        }

        return Optional.ofNullable(
                this.giveawayMongoCollection.find(this.getFilter(messageId)).first()
        );
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

            message.createUpdater()
                    .setEmbed(embedMessage)
                    .removeAllComponents()
                    .applyChanges();

            this.deleteOne(giveaway);
            return;
        }

        EmbedBuilder embedBuilder = new EmbedMessage(server).giveaway()
                .setTitle(EmojiUtil.getPartyEmoji() + " Zakończony konkurs")
                .addField("Zwycięzcy", String.join("\n", selectedWinners))
                .addField("Nagroda", giveaway.getAward())
                .addField("Wzięło udział", String.valueOf(giveaway.getParticipants().size()));

        message.createUpdater()
                .setEmbed(embedBuilder)
                .removeAllComponents()
                .setContent(String.join(", ", selectedWinners))
                .applyChanges()
                .thenAcceptAsync(msg -> {
                    this.updateOne(giveaway, Updates.set("ended", true));
                    giveaway.setEnded(true);
                }).exceptionallyAsync(throwable -> {
                    this.deleteOne(giveaway);
                    throwable.printStackTrace();
                    return null;
                });
    }

    public List<Giveaway> getExpiredGiveaways() {
        long currentTime = Instant.now().plusSeconds(1).toEpochMilli();
        Bson filter = and(lt("expireAt", currentTime), eq("ended", false));

        return this.giveawayMongoCollection
                .find(filter)
                .into(new ArrayList<>());
    }

    public Bson getFilter(long messageId) {
        return Filters.eq("messageId", messageId);
    }
}
