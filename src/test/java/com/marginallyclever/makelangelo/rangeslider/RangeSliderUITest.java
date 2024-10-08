package com.marginallyclever.makelangelo.rangeslider;




import org.junit.Before;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

class RangeSliderUITest {

    private TestableRangeSliderUI testableRangeSliderUI;
    private JSlider slider;


    // Test la fonction CalculateThumbLocation() without snap to ticks
    @Test
    public void testCalculateThumbLocationWithoutSnapToTicks() {
        //Set up
        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(20);
        slider.setExtent(30);
        slider.setSnapToTicks(false);

        // Utilisation de la classe TestableRangeSliderUI
        testableRangeSliderUI = new TestableRangeSliderUI(new RangeSlider());
        testableRangeSliderUI.installUI(slider);

        //Appel la fonction calculateThumbLocation()
        testableRangeSliderUI.calculateThumbLocation();

        // Vérification sans ajustement
        int expectedUpperThumbX = testableRangeSliderUI.getXPositionForValue(50);
        Rectangle upperThumbRect = testableRangeSliderUI.getUpperThumbRect();

        assertEquals(expectedUpperThumbX - (testableRangeSliderUI.getThumbSize().width / 2), upperThumbRect.x);
    }

    // Test la fonction CalculateThumbLocation() with major tick spacing
    @Test
    public void testCalculateThumbLocationWithMajorTickSpacing() {
        //Set up
        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(22);
        slider.setExtent(38);
        slider.setMajorTickSpacing(10);
        slider.setSnapToTicks(true);

        // Utilisation de la classe TestableRangeSliderUI
        testableRangeSliderUI = new TestableRangeSliderUI(new RangeSlider());
        testableRangeSliderUI.installUI(slider);

        //Appel la fonction calculateThumbLocation()
        testableRangeSliderUI.calculateThumbLocation();

        // Vérification d'alignement sur le tick 60
        int expectedUpperThumbX = testableRangeSliderUI.getXPositionForValue(60);
        Rectangle upperThumbRect = testableRangeSliderUI.getUpperThumbRect();
        assertEquals(expectedUpperThumbX - (testableRangeSliderUI.getThumbSize().width / 2), upperThumbRect.x);

        // Vérification du snappedValue
        assertEquals(60, slider.getValue() + slider.getExtent());
        assertEquals(60 - slider.getValue(), slider.getExtent());
    }
    // Test la fonction CalculateThumbLocation() with minor tick spacing
    @Test
    public void testCalculateThumbLocationWithMinorTickSpacing() {
        // Set up
        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(25);
        slider.setExtent(47);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(5);
        slider.setSnapToTicks(true);

        // Utilisation de la classe TestableRangeSliderUI
        testableRangeSliderUI = new TestableRangeSliderUI(new RangeSlider());
        testableRangeSliderUI.installUI(slider);

        //Appel la fonction calculateThumbLocation()
        testableRangeSliderUI.calculateThumbLocation();

        // Calcul des valeurs intermédiaires pour vérification
        int upperValue = slider.getValue() + slider.getExtent();
        int tickSpacing = slider.getMinorTickSpacing();
        float temp = (float)(upperValue - slider.getMinimum()) / (float)tickSpacing;
        int whichTick = Math.round(temp);
        int snappedValue = slider.getMinimum() + (whichTick * tickSpacing);

        //Affiche les valeurs pour debug
        System.out.println("Upper Value: " + upperValue);
        System.out.println("Tick Spacing: " + tickSpacing);
        System.out.println("Snapped Value: " + snappedValue);

        // Vérifiez la position attendue et actuelle du curseur supérieur
        int expectedUpperThumbX = testableRangeSliderUI.getXPositionForValue(snappedValue);
        Rectangle upperThumbRect = testableRangeSliderUI.getUpperThumbRect();

        assertEquals(expectedUpperThumbX - (testableRangeSliderUI.getThumbSize().width / 2), upperThumbRect.x);
        assertEquals(snappedValue, slider.getValue() + slider.getExtent());
    }

    // Test la fonction CalculateThumbLocation() with zero tick spacing
    @Test
    public void testCalculateThumbLocationWithZeroTickSpacing() {
        //Set up
        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(20);
        slider.setExtent(30);
        slider.setSnapToTicks(true); // snapToTicks activé mais pas de major/minor tick spacing

        // Utilisation de la classe TestableRangeSliderUI
        testableRangeSliderUI = new TestableRangeSliderUI(new RangeSlider());
        testableRangeSliderUI.installUI(slider);

        //Appel la fonction calculateThumbLocation()
        testableRangeSliderUI.calculateThumbLocation();

        // Vérifier qu'aucun ajustement n'a été fait
        int expectedUpperThumbX = testableRangeSliderUI.getXPositionForValue(50);
        Rectangle upperThumbRect = testableRangeSliderUI.getUpperThumbRect();

        assertEquals(expectedUpperThumbX - (testableRangeSliderUI.getThumbSize().width / 2), upperThumbRect.x);
        assertEquals(50, slider.getValue() + slider.getExtent()); // Pas de modification de la valeur
    }

    // Test la fonction CalculateThumbLocation() for vertical slider
    @Test
    public void testCalculateThumbLocationForVerticalSlider() {
        //Set up du slider vertical
        slider = new JSlider(JSlider.VERTICAL);
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(30);
        slider.setExtent(40);  // Le curseur supérieur sera à 30 + 40 = 70

        // Utilisation de la classe TestableRangeSliderUI
        testableRangeSliderUI = new TestableRangeSliderUI(new RangeSlider());
        testableRangeSliderUI.installUI(slider);

        // Définir manuellement la taille et la position de la piste (track) pour le test
        testableRangeSliderUI.setTrackRect(new Rectangle(0, 0, 10, 200));  // Largeur et hauteur arbitraires de la piste

        // Appeler la méthode à tester
        testableRangeSliderUI.calculateThumbLocation();

        // Calculer la position attendue du curseur supérieur
        int expectedUpperThumbY = testableRangeSliderUI.getYPositionForValue(slider.getValue() + slider.getExtent());  // Basé sur 70
        Rectangle upperThumbRect = testableRangeSliderUI.getUpperThumbRect();

        // Vérifier que la position X du curseur supérieur correspond à la piste
        assertEquals("Upper thumb X position should match track X position.", testableRangeSliderUI.getTrackRect().x, upperThumbRect.x);

        // Vérifier que la position Y du curseur supérieur est correcte
        assertEquals("Upper thumb Y position should be centered correctly.",
                expectedUpperThumbY - (upperThumbRect.height / 2), upperThumbRect.y);
    }
    
    // Test la fonction scrollByUnit
    @Test
    public void testScrollByUnit(){
        // Set up
        slider = new JSlider();
        RangeSliderUI rangeSliderUI = new RangeSliderUI(new RangeSlider());
        rangeSliderUI.installUI(slider);

        // Test que la valeur du slider augmente de 1
        int oldValue = slider.getValue();
        rangeSliderUI.scrollByUnit(1);      // Direction positive
        assertEquals(oldValue + 1, slider.getValue());

        // Test que la valeur du slider diminue de 1
        oldValue = slider.getValue();
        rangeSliderUI.scrollByUnit(-1);     // Direction négative
        assertEquals(oldValue - 1, slider.getValue());
    }

    // Test la fonction scrollByUnit avec le upperThumbSelected == true
    @Test
    public void testScrollByUnitWithUpperThumbSelected() throws NoSuchFieldException, IllegalAccessException {
        // Set up
        RangeSlider slider = new RangeSlider();
        RangeSliderUI rangeSliderUI = new RangeSliderUI(new RangeSlider());
        Field upperThumbSelectedField = rangeSliderUI.getClass().getDeclaredField("upperThumbSelected");
        upperThumbSelectedField.setAccessible(true);
        upperThumbSelectedField.setBoolean(rangeSliderUI, true);
        rangeSliderUI.installUI(slider);

        // Test que la valeur maximale du slider augmente de 1
        int oldValue = slider.getUpperValue();
        rangeSliderUI.scrollByUnit(1);
        assertEquals(oldValue + 1, slider.getUpperValue());
    }

    // Test la fonction scrollByBlock
    @Test
    public void testScrollByBlock(){
        // Set up
        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(100);
        RangeSliderUI rangeSliderUI = new RangeSliderUI(new RangeSlider());
        rangeSliderUI.installUI(slider);

        // Test que la valeur du slider augmente selon les valeurs maximum et minimum du slider
        int oldValue = slider.getValue();
        rangeSliderUI.scrollByBlock(1);     // Direction positive
        assertEquals(oldValue + 10, slider.getValue());

        // Test que la valeur du slider diminue selon les valeurs maximum et minimum du slider
        oldValue = slider.getValue();
        rangeSliderUI.scrollByBlock(-1);    // Direction négative
        assertEquals(oldValue - 10, slider.getValue());
    }
}