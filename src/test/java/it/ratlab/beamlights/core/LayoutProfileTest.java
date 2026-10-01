package it.ratlab.beamlights.core;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutProfileTest {
    private static LayoutProfile parse(String s) {
        return LayoutProfile.parse(JsonParser.parseString(s));
    }

    @Test
    void fullProfile() {
        LayoutProfile p = parse("{\"providers\":[\"omegaflashlight\"],\"indoor\":\"center_only\","
                + "\"outdoor\":\"TRIANGLE\",\"cone\":true,\"priority\":5}");
        assertEquals(List.of("omegaflashlight"), p.providers());
        assertEquals(RayLayout.Pattern.CENTER_ONLY, p.indoor());
        assertEquals(RayLayout.Pattern.TRIANGLE, p.outdoor());
        assertTrue(p.coneOr(false));
        assertEquals(5, p.priority());
    }

    @Test
    void missingFieldsFallBack() {
        LayoutProfile p = parse("{\"providers\":[\"data\"],\"outdoor\":\"RING\"}");
        assertNull(p.indoor());
        assertEquals(RayLayout.Pattern.CROSS, p.indoorOr(RayLayout.Pattern.CROSS));
        assertEquals(RayLayout.Pattern.RING, p.outdoorOr(RayLayout.Pattern.CROSS));
        assertTrue(p.coneOr(true));
    }

    @Test
    void errors() {
        assertThrows(IllegalArgumentException.class, () -> parse("{\"indoor\":\"RING\"}"));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"providers\":[],\"indoor\":\"RING\"}"));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"providers\":[\"x\"],\"indoor\":\"SPIRAL\"}"));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"providers\":[\"x\"]}"));
    }
}
