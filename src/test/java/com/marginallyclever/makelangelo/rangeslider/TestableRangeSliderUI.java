package com.marginallyclever.makelangelo.rangeslider;


import java.awt.*;

public class TestableRangeSliderUI extends RangeSliderUI {

    public TestableRangeSliderUI(RangeSlider slider) {
        super(slider);
    }

    // Méthode publique pour exposer xPositionForValue
    public int getXPositionForValue(int value) {
        return xPositionForValue(value); // Appelle la méthode protégée
    }
    // Méthode publique pour exposer yPositionForValue
    public int getYPositionForValue (int value) {
        return yPositionForValue(value);
    }

    public Rectangle getTrackRect() {
        return this.trackRect;
    }

    // Setter pour trackRect
    public void setTrackRect(Rectangle trackRect) {
        this.trackRect = trackRect;
    }
}
