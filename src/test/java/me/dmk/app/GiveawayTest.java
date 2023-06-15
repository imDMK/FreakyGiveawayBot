package me.dmk.app;

import me.dmk.app.util.GiveawayUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by DMK on 15.06.2023
 */
public class GiveawayTest {

    @Test
    void distributionTest() {
        this.selectWinners(1, 10);
        this.selectWinners(2, 5);
        this.selectWinners(5, 5);
    }

    @DisplayName("Select winners from set")
    private void selectWinners(int winners, int participants) {
        Set<Long> participantsSet = new HashSet<>();

        for (int i = 0; i < participants; i++) {
            participantsSet.add(ThreadLocalRandom.current().nextLong());
        }

        System.out.printf(
                "Selecting %s winners from %s participants", winners, participants
        ).println();

        List<String> selectedWinners = GiveawayUtil.selectWinners(participantsSet, winners);

        assertEquals(winners, selectedWinners.size());
    }
}
