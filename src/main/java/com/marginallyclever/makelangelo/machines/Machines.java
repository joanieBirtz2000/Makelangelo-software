package com.marginallyclever.makelangelo.machines;

import com.marginallyclever.makelangelo.plotter.plotterRenderer.*;

import java.util.Arrays;

public enum Machines {
    MAKELANGELO_5("Makelangelo 5", new Makelangelo5()),
    MAKELANGELO_3_3("Makelangelo 3.3", new Makelangelo3_3()),
    MAKELANGELO_CUSTOM("Makelangelo (Custom)", new MakelangeloCustom()),
    CARTESIAN("Cartesian", new Cartesian()),
    ZARPLOTTER("Zarplotter", new Zarplotter());

    private final PlotterRenderer plotterRenderer;

    private final String name;

    Machines(String name, PlotterRenderer plotterRenderer) {
        this.name = name;
        this.plotterRenderer = plotterRenderer;
    }

    public PlotterRenderer getPlotterRenderer() {
        return plotterRenderer;
    }

    public String getName() {
        return name;
    }

    public static Machines findByName(String name) {
        return Arrays.stream(values())
                .filter(enumValue -> enumValue.getName().contains(name))
                .findFirst()
                .orElseThrow();
    }
}