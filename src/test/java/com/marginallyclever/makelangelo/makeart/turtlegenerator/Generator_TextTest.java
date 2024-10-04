package com.marginallyclever.makelangelo.makeart.turtlegenerator;

import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.util.PreferencesHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class Generator_TextTest {
    @BeforeAll
    public static void setup() {
        PreferencesHelper.start();
        Translator.start();
    }
    @Test
    public void generateText() {
        Generator_Text generator = new Generator_Text();
        generator.setFont(-6);
        assertEquals(0, generator.getLastFont());
        generator.setFont(1);
        assertEquals(1, generator.getLastFont());
        assertEquals("", generator.getLastMessage());
    }
}