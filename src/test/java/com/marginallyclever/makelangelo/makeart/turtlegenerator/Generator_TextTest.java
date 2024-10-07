package com.marginallyclever.makelangelo.makeart.turtlegenerator;

import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.paper.Paper;
import com.marginallyclever.makelangelo.paper.PaperSettingsPanel;
import com.marginallyclever.makelangelo.turtle.Turtle;
import com.marginallyclever.util.PreferencesHelper;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPanelFixture;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


public class Generator_TextTest {
    @BeforeAll
    public static void setup() {
        PreferencesHelper.start();
        Translator.start();
    }
//    // il y a 1356 font dans le tableau fontNames
//    @Test
//    public void generateText() {
//        Generator_Text generator = new Generator_Text();
//        generator.setFont(-6);
//        assertEquals(0, generator.getLastFont());
//        generator.setFont(1);
//        assertEquals(1, generator.getLastFont());
//        generator.setFont(2000);
//        assertEquals(1355, generator.getLastFont());
//        assertEquals("", generator.getLastMessage());
//
//    }
    private Generator_Text generator;
    private Paper paper;
    private Turtle captureTurtle;


    @Test
    public void testGenerator_Text(){

    }

    @Test
    public void testSetupTransform(){
        paper = new Paper();
        generator = new Generator_Text();
        paper.setPaperSize(210, 297, 0, 0);
        generator.setPaper(paper);
        Rectangle2D.Double rect = paper.getMarginRectangle();

        generator.setupTransform();
        double width = rect.getWidth();
        double expectedChars = (int) Math.floor((float) (width * 10.0f - 5.0f * 2.0f) / (10.0f + 5.0f));
        assertEquals(expectedChars, generator.getCharsPerLine(),"transform");

        width = 300;
        double height = 200;
        generator.setupTransform(width, height);
        width = rect.getWidth();
        double expectedCharsPerLine = (int) Math.floor((float) (width * 10.0f - 5.0f * 2.0f) / (10.0f + 5.0f));
        assertEquals(expectedCharsPerLine, generator.getCharsPerLine());

        width = 200;
        height = 300;
        generator.setupTransform(width, height);
        width *= (float) rect.getHeight() / (float) height;
        double expected = (int) Math.floor((float) (width * 10.0f - 5.0f * 2.0f) / (10.0f + 5.0f));
        assertEquals(expected, generator.getCharsPerLine());
    }

    @Test
   public void testGenerate(){
        paper = new Paper();
        generator = new Generator_Text();
        paper.setPaperSize(216, 279, 0, 0);
        generator.setPaper(paper);
        generator.addListener(turtle -> captureTurtle = turtle);

        generator.setFont(1);
        generator.setSize(20);
        generator.setMessage("Hello Makelangelo");
        generator.generate();

        String[] font = generator.getFontNames();

        assertNotNull(captureTurtle);
        assertEquals("SansSerif", font[generator.getLastFont()]);
        assertEquals(20, generator.getLastSize());
    }
}