package it.ratlab.beamlights.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveSchedulerTest {
    private static List<Long> allowed(MoveScheduler<String> s) {
        List<Long> out = new ArrayList<>();
        for (int i = 0; i < s.size(); i++) if (s.allowed(i)) out.add(s.key(i));
        return out;
    }

    @Test
    void underBudgetAllowsAll() {
        MoveScheduler<String> s = new MoveScheduler<>();
        s.begin();
        s.offer(1, false, 100, 1, "a");
        s.offer(2, false, 4, 1, "b");
        assertEquals(0, s.schedule(5));
        assertEquals(2, allowed(s).size());
    }

    @Test
    void zeroBudgetIsUnlimited() {
        MoveScheduler<String> s = new MoveScheduler<>();
        s.begin();
        for (int i = 0; i < 50; i++) s.offer(i, false, i, 1, null);
        assertEquals(0, s.schedule(0));
    }

    @Test
    void priorityThenNearestThenDisplacement() {
        MoveScheduler<String> s = new MoveScheduler<>();
        s.begin();
        s.offer(1, false, 1, 1, null);     // near
        s.offer(2, true, 900, 1, null);    // local central ray, far
        s.offer(3, false, 1, 50, null);    // as near, bigger move
        s.offer(4, false, 400, 99, null);  // far
        assertEquals(2, s.schedule(2));
        assertEquals(List.of(2L, 3L), allowed(s));
    }

    @Test
    void payloadFollowsCandidate() {
        MoveScheduler<String> s = new MoveScheduler<>();
        s.begin();
        s.offer(7, false, 9, 1, "far");
        s.offer(8, false, 1, 1, "near");
        s.schedule(1);
        for (int i = 0; i < s.size(); i++) {
            assertEquals(s.key(i) == 8 ? "near" : "far", s.payload(i));
            assertEquals(s.key(i) == 8, s.allowed(i));
        }
    }

    @Test
    void agedCandidateIsNotStarved() {
        MoveScheduler<String> s = new MoveScheduler<>();
        int ticksUntilAllowed = -1;
        for (int t = 0; t < 10 && ticksUntilAllowed < 0; t++) {
            s.begin();
            s.offer(1, false, 1, 1, null);   // always nearer
            s.offer(2, false, 100, 1, null); // starved without aging
            s.schedule(1);
            if (allowed(s).contains(2L)) ticksUntilAllowed = t;
        }
        assertEquals(MoveScheduler.MAX_AGE, ticksUntilAllowed);
    }

    @Test
    void ageResetsWhenNotOffered() {
        MoveScheduler<String> s = new MoveScheduler<>();
        for (int t = 0; t < MoveScheduler.MAX_AGE; t++) {
            s.begin();
            s.offer(1, false, 1, 1, null);
            s.offer(2, false, 100, 1, null);
            s.schedule(1);
        }
        s.begin();
        s.offer(1, false, 1, 1, null);
        s.schedule(1);
        s.begin();
        s.offer(1, false, 1, 1, null);
        s.offer(2, false, 100, 1, null);
        s.schedule(1);
        assertEquals(List.of(1L), allowed(s));
    }

    @Test
    void poolIsReused() {
        MoveScheduler<String> s = new MoveScheduler<>();
        s.begin();
        s.offer(1, false, 1, 1, "x");
        s.offer(2, false, 2, 1, "y");
        s.schedule(1);
        s.begin();
        s.offer(3, false, 1, 1, "z");
        assertEquals(0, s.schedule(1));
        assertEquals(1, s.size());
        assertEquals(3L, s.key(0));
        assertEquals("z", s.payload(0));
    }
}
