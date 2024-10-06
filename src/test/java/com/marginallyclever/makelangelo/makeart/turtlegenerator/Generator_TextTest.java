package com.marginallyclever.makelangelo.makeart.turtlegenerator;

import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.paper.Paper;
import com.marginallyclever.makelangelo.paper.PaperSettingsPanel;
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
//    @BeforeAll
//    public static void setup() {
//        PreferencesHelper.start();
//        Translator.start();
//    }
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
    private FrameFixture window;
    private Paper paper;

//    @Before
//    public void setUp() {
//        Robot robot = BasicRobot.robotWithNewAwtHierarchy();
//
//        PreferencesHelper.start();
//        Translator.start();
//
//    }

    @Test
    public void testGenerator_Text(){

    }

    @Test
    public void testSetupTransform(){
        PreferencesHelper.start();
        Translator.start();

        paper = new Paper();
        generator = new Generator_Text();
        paper.setPaperSize(210, 297, 0, 0);
        generator.setPaper(paper);
        Rectangle2D.Double rect = paper.getMarginRectangle();

        double width = 300;
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
    public void testWrapToLength(){

    }

    @Test
    public void testLongestLine(){


    }

    @Test
    public void testCalculateBounds(){

    }
}