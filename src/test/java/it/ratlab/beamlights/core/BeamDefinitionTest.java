package it.ratlab.beamlights.core;

import com.google.gson.JsonParser;
import it.ratlab.beamlights.core.BeamDefinition.Slot;
import it.ratlab.beamlights.core.math.V3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeamDefinitionTest {
    private static BeamDefinition parse(String json) {
        return BeamDefinition.parse(JsonParser.parseString(json));
    }

    private static void invalid(String json) {
        assertThrows(IllegalArgumentException.class, () -> parse(json));
    }

    @Test
    void defaults() {
        BeamDefinition d = parse("{\"items\":[\"lantern\"],\"luminance\":12}");
        assertEquals(List.of("minecraft:lantern"), d.items());
        assertEquals(List.of(Slot.MAINHAND, Slot.OFFHAND), d.slots());
        assertEquals(12, d.luminance());
        assertEquals(16f, d.range());
        assertEquals(25f, d.cone());
        assertEquals(0xFFFFFF, d.rgb());
        assertNull(d.condition());
        assertEquals(0, d.priority());
    }

    @Test
    void fullFile() {
        BeamDefinition d = parse("{\"items\":[\"mymod:lamp\",\"#c:flashlights\"],\"slots\":[\"head\",\"Curios\",\"head\"],"
                + "\"luminance\":15,\"range\":40,\"cone\":10.5,\"color\":\"#FFE8B0\",\"priority\":3,"
                + "\"origin\":{\"forward\":0.3,\"down\":0.2},"
                + "\"condition\":{\"component\":\"mymod:enabled\",\"equals\":true}}");
        assertEquals(List.of("mymod:lamp", "#c:flashlights"), d.items());
        assertTrue(BeamDefinition.isTag(d.items().get(1)));
        assertEquals(List.of(Slot.HEAD, Slot.CURIOS), d.slots());
        assertEquals(0xFFE8B0, d.rgb());
        assertEquals(3, d.priority());
        assertEquals("mymod:enabled", d.condition().component());
        assertTrue(d.condition().expected().getAsBoolean());
    }

    @Test
    void presentCondition() {
        BeamDefinition d = parse("{\"items\":\"a:b\",\"luminance\":5,\"condition\":{\"component\":\"a:on\",\"present\":true}}");
        assertNull(d.condition().expected());
    }

    @Test
    void rejectsBadFiles() {
        invalid("[]");
        invalid("{\"luminance\":5}");
        invalid("{\"items\":[],\"luminance\":5}");
        invalid("{\"items\":[\"a:b\"]}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":16}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":2.5}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":5,\"range\":0.5}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":5,\"range\":200}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":5,\"cone\":90}");
        invalid("{\"items\":[\"A B\"],\"luminance\":5}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":5,\"slots\":[\"chest\"]}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":5,\"color\":\"red\"}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":5,\"condition\":{\"component\":\"a:on\"}}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":5,\"condition\":{\"component\":\"a:on\",\"present\":true,\"equals\":1}}");
        invalid("{\"items\":[\"a:b\"],\"luminance\":5,\"condition\":{\"component\":\"#a:on\",\"present\":true}}");
    }

    @Test
    void colors() {
        assertEquals(0x00FF10, BeamDefinition.parseColor("#00ff10"));
        assertEquals(0x123456, BeamDefinition.parseColor("123456"));
        assertEquals(0xABCDEF, BeamDefinition.parseColor("0xABCDEF"));
        assertThrows(IllegalArgumentException.class, () -> BeamDefinition.parseColor("#12345"));
        assertThrows(IllegalArgumentException.class, () -> BeamDefinition.parseColor("#GGGGGG"));
    }

    @Test
    void originOffset() {
        BeamDefinition d = parse("{\"items\":[\"a:b\"],\"luminance\":5,\"origin\":{\"forward\":2,\"down\":0.5}}");
        V3 o = d.origin(new V3(0, 10, 0), new V3(0, 0, 4));
        assertEquals(0, o.x(), 1e-9);
        assertEquals(9.5, o.y(), 1e-9);
        assertEquals(2, o.z(), 1e-9);
    }
}
