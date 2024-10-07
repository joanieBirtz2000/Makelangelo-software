package com.marginallyclever.makelangelo.rangeslider;




import org.junit.Before;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import java.awt.*;

import static org.junit.Assert.*;

class RangeSliderUITest {

    private TestableRangeSliderUI testableRangeSliderUI;
    private JSlider slider;


    @Test
    public void testCalculateThumbLocationWithoutSnapToTicks() {
        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(20);
        slider.setExtent(30);
        slider.setSnapToTicks(false);

        testableRangeSliderUI = new TestableRangeSliderUI(new RangeSlider());
        testableRangeSliderUI.installUI(slider);

        testableRangeSliderUI.calculateThumbLocation();

        // Vérification sans ajustement
        int expectedUpperThumbX = testableRangeSliderUI.getXPositionForValue(50);
        Rectangle upperThumbRect = testableRangeSliderUI.getUpperThumbRect();

        assertEquals(expectedUpperThumbX - (testableRangeSliderUI.getThumbSize().width / 2), upperThumbRect.x);
    }

    @Test
    public void testCalculateThumbLocationWithMajorTickSpacing() {
        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(22);
        slider.setExtent(38);
        slider.setMajorTickSpacing(10);
        slider.setSnapToTicks(true);

        testableRangeSliderUI = new TestableRangeSliderUI(new RangeSlider());
        testableRangeSliderUI.installUI(slider);

        testableRangeSliderUI.calculateThumbLocation();

        // Vérification d'alignement sur le tick 60
        int expectedUpperThumbX = testableRangeSliderUI.getXPositionForValue(60);
        Rectangle upperThumbRect = testableRangeSliderUI.getUpperThumbRect();
        assertEquals(expectedUpperThumbX - (testableRangeSliderUI.getThumbSize().width / 2), upperThumbRect.x);

        // Vérification du snappedValue
        assertEquals(60, slider.getValue() + slider.getExtent());
        assertEquals(60 - slider.getValue(), slider.getExtent());
    }
    @Test
    public void testCalculateThumbLocationWithMinorTickSpacing() {
        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(25);
        slider.setExtent(47);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(5);
        slider.setSnapToTicks(true);

        testableRangeSliderUI = new TestableRangeSliderUI(new RangeSlider());
        testableRangeSliderUI.installUI(slider);

        testableRangeSliderUI.calculateThumbLocation();

        // Calcul des valeurs intermédiaires pour vérification
        int upperValue = slider.getValue() + slider.getExtent();
        int tickSpacing = slider.getMinorTickSpacing();
        float temp = (float)(upperValue - slider.getMinimum()) / (float)tickSpacing;
        int whichTick = Math.round(temp);
        int snappedValue = slider.getMinimum() + (whichTick * tickSpacing);

        System.out.println("Upper Value: " + upperValue);
        System.out.println("Tick Spacing: " + tickSpacing);
        System.out.println("Snapped Value: " + snappedValue);

        // Vérifiez la position attendue et actuelle du curseur supérieur
        int expectedUpperThumbX = testableRangeSliderUI.getXPositionForValue(snappedValue);
        Rectangle upperThumbRect = testableRangeSliderUI.getUpperThumbRect();

        assertEquals(expectedUpperThumbX - (testableRangeSliderUI.getThumbSize().width / 2), upperThumbRect.x);
        assertEquals(snappedValue, slider.getValue() + slider.getExtent());
    }


    @Test
    public void testCalculateThumbLocationWithZeroTickSpacing() {
        slider = new JSlider();
        slider.setMinimum(0);
        slider.setMaximum(100);
        slider.setValue(20);
        slider.setExtent(30);
        slider.setSnapToTicks(true); // snapToTicks activé mais pas de major/minor tick spacing

        testableRangeSliderUI = new TestableRangeSliderUI(new RangeSlider());
        testableRangeSliderUI.installUI(slider);

        testableRangeSliderUI.calculateThumbLocation();

        // Vérifier qu'aucun ajustement n'a été fait
        int expectedUpperThumbX = testableRangeSliderUI.getXPositionForValue(50);
        Rectangle upperThumbRect = testableRangeSliderUI.getUpperThumbRect();

        assertEquals(expectedUpperThumbX - (testableRangeSliderUI.getThumbSize().width / 2), upperThumbRect.x);
        assertEquals(50, slider.getValue() + slider.getExtent()); // Pas de modification de la valeur
    }

    @Test
    public void testCalculateThumbLocationForVerticalSlider() {
        // Configuration du slider vertical
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
    

    @Test
    public void testScrollByUnitPositive(){
        slider = new JSlider();
        RangeSliderUI rangeSliderUI = new RangeSliderUI(new RangeSlider());
        rangeSliderUI.installUI(slider);
        int oldValue = slider.getValue();
        rangeSliderUI.scrollByUnit(1);
        assertEquals(oldValue + 1, slider.getValue());
    }

    @Test
    public void testScrollByUnitNegative(){
        slider = new JSlider();
        RangeSliderUI rangeSliderUI = new RangeSliderUI(new RangeSlider());
        rangeSliderUI.installUI(slider);
        int oldValue = slider.getValue();
        rangeSliderUI.scrollByUnit(-1);
        assertEquals(oldValue - 1, slider.getValue());
    }
}