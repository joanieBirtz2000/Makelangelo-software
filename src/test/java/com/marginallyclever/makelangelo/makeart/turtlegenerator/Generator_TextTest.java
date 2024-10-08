package com.marginallyclever.makelangelo.makeart.turtlegenerator;

import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.paper.Paper;
import com.marginallyclever.makelangelo.turtle.Turtle;
import com.marginallyclever.util.PreferencesHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;


public class Generator_TextTest {
    @BeforeAll
    public static void setup() {
        PreferencesHelper.start();
        Translator.start();
    }

    private Generator_Text generator;
    private Paper paper;
    private Turtle captureTurtle;

    // Test la fonction setupTransform()
    @Test
    public void testSetupTransform(){
        // Set up
        paper = new Paper();
        generator = new Generator_Text();
        paper.setPaperSize(210, 297, 0, 0);
        generator.setPaper(paper);
        Rectangle2D.Double rect = paper.getMarginRectangle();

        // Test la fonction sans grandeur
        generator.setupTransform();
        double width = rect.getWidth();
        double expectedChars = (int) Math.floor((float) (width * 10.0f - 5.0f * 2.0f) / (10.0f + 5.0f));
        assertEquals(expectedChars, generator.getCharsPerLine());

        // Test la fonction avec width plus grand que celui du papier
        width = 300;
        double height = 200;
        generator.setupTransform(width, height);
        width = rect.getWidth();
        double expectedCharsPerLine = (int) Math.floor((float) (width * 10.0f - 5.0f * 2.0f) / (10.0f + 5.0f));
        assertEquals(expectedCharsPerLine, generator.getCharsPerLine());

        // Test la fonction avec height plus grand que celui du papier
        width = 200;
        height = 300;
        generator.setupTransform(width, height);
        width *= (float) rect.getHeight() / (float) height;
        double expected = (int) Math.floor((float) (width * 10.0f - 5.0f * 2.0f) / (10.0f + 5.0f));
        assertEquals(expected, generator.getCharsPerLine());
    }

    // Test la fonction generate()
    @Test
   public void testGenerate(){
        // Set up
        paper = new Paper();
        generator = new Generator_Text();
        paper.setPaperSize(216, 279, 0, 0);
        generator.setPaper(paper);
        generator.addListener(turtle -> captureTurtle = turtle);

        generator.setFont(1);
        generator.setSize(20);
        generator.setMessage("Hello Makelangelo");

        // Test que la turtle génère le message voulu
        generator.generate();

        String[] font = generator.getFontNames();

        assertNotNull(captureTurtle);
        assertEquals(font[1], font[generator.getLastFont()]);
        assertEquals(20, generator.getLastSize());
        assertEquals("Hello Makelangelo", generator.getLastMessage());
    }
}