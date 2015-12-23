package com.rei.chairlift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class ChairliftScriptTest {

    @Test
    public void canGetParams() {
        ChairliftScript script = new ChairliftScript();
        Map<String, String> suppliedParams = Maps.newHashMap(ImmutableMap.of("bool", "true", "num", "123"));
        script.setConfig(new TemplateConfig(new ChairliftConfig(false, false, suppliedParams)));
        assertEquals(true, script.param("bool", "", false));
        suppliedParams.put("bool", "false");
        assertEquals(false, script.param("bool", "", true));
        assertTrue(script.param("num", "", 456) instanceof Number);
    }

}
