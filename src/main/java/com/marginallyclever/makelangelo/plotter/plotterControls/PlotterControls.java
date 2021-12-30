package com.marginallyclever.makelangelo.plotter.plotterControls;

import com.marginallyclever.convenience.CommandLineOptions;
import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.plotter.Plotter;
import com.marginallyclever.makelangelo.turtle.Turtle;
import com.marginallyclever.makelangelo.turtle.TurtleMove;
import com.marginallyclever.util.PreferencesHelper;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * {@link PlotterControls} brings together three separate panels and wraps all
 * the lower level features in a human friendly, intuitive interface. - The
 * {@link MarlinInterface}, which manages the two way network connection to a
 * robot running Marlin firmware. - The {@link JogInterface}, which is a
 * human-friendly way to drive a {@link Plotter} - The {@link ProgramInterface},
 * which is a buffer for queueing commands to a {@link Plotter}
 * 
 * @author Dan Royer
 * @since 7.28.0
 */
public class PlotterControls extends JPanel {
	private static final long serialVersionUID = -1201865024705737250L;
	private Plotter myPlotter;
	private Turtle myTurtle;
	private JogInterface jogInterface;
	private MarlinInterface marlinInterface;
	private ProgramInterface programInterface;

	// private JButton bSaveGCode = new
	// JButton(Translator.get("PlotterControls.SaveGCode"));
	private JButton bFindHome = new JButton(Translator.get("JogInterface.FindHome"));
	private JButton bRewind = new JButton(Translator.get("PlotterControls.Rewind"));
	private JButton bStart = new JButton(Translator.get("PlotterControls.Play"));
	private JButton bStep = new JButton(Translator.get("PlotterControls.Step"));
	private JButton bPause = new JButton(Translator.get("PlotterControls.Pause"));
	private JProgressBar progress = new JProgressBar(0, 100);

	private boolean isRunning = false;
	private boolean penIsUpBeforePause = false;

	public PlotterControls(Plotter plotter, Turtle turtle) {
		super();
		myPlotter = plotter;
		myTurtle = turtle;

		jogInterface = new JogInterface(plotter);
		marlinInterface = new MarlinPlotterInterface(plotter);
		programInterface = new ProgramInterface(plotter, turtle);

		JTabbedPane pane = new JTabbedPane();
		pane.addTab(JogInterface.class.getSimpleName(), jogInterface);
		pane.addTab(MarlinInterface.class.getSimpleName(), marlinInterface);
		pane.addTab(ProgramInterface.class.getSimpleName(), programInterface);

		this.setLayout(new BorderLayout());
		this.add(pane, BorderLayout.CENTER);
		this.add(getToolBar(), BorderLayout.NORTH);
		this.add(progress, BorderLayout.SOUTH);

		marlinInterface.addListener((e) -> {
			if (e.getActionCommand().contentEquals(MarlinInterface.IDLE)) {
				// logger.debug("PlotterControls heard idle");
				if (isRunning) {
					// logger.debug("PlotterControls is running");
					step();
				}
			}
			updateProgressBar();
		});
	}

	private JToolBar getToolBar() {
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		bar.add(bFindHome);
		bar.addSeparator();
		bar.add(bRewind);
		bar.add(bStart);
		bar.add(bPause);
		bar.add(bStep);

		bFindHome.addActionListener((e) -> findHome());
		bRewind.addActionListener((e) -> rewind());
		bStart.addActionListener((e) -> play());
		bPause.addActionListener((e) -> pause());
		bStep.addActionListener((e) -> step());

		updateButtonStatus();

		return bar;
	}

	private void findHome() {
		jogInterface.findHome();
		updateButtonStatus();
	}

	private void step() {
		programInterface.step();
		if (programInterface.getLineNumber() == -1) {
			// done
			pause();
		}
	}

	public void startAt(int lineNumber) {
		int count = programInterface.getMoveCount();
		if (lineNumber >= count)
			lineNumber = count;
		if (lineNumber < 0)
			lineNumber = 0;

		programInterface.setLineNumber(lineNumber);
		play();
	}

	private void updateProgressBar() {
		progress.setValue((int) (100.0 * programInterface.getLineNumber() / programInterface.getMoveCount()));
	}

	private void rewind() {
		programInterface.rewind();
		progress.setValue(0);
	}

	private void play() {
		isRunning = true;
		updateButtonStatus();
		if (!penIsUpBeforePause)
			myPlotter.lowerPen();
		rewindIfNoProgramLineSelected();
		step();
	}

	private void rewindIfNoProgramLineSelected() {
		if (programInterface.getLineNumber() == -1) {
			programInterface.rewind();
		}
	}

	private void pause() {
		isRunning = false;
		updateButtonStatus();
		penIsUpBeforePause = myPlotter.getPenIsUp();
		if (!penIsUpBeforePause)
			myPlotter.raisePen();
	}

	public boolean isRunning() {
		return isRunning;
	}

	private void updateButtonStatus() {
		boolean isHomed = myPlotter.getDidFindHome();
		bRewind.setEnabled(isHomed && !isRunning);
		bStart.setEnabled(isHomed && !isRunning);
		bPause.setEnabled(isHomed && isRunning);
		bStep.setEnabled(isHomed && !isRunning);
	}

	@SuppressWarnings("unused")
	private int findLastPenUpBefore(int startAtLine) {
		List<TurtleMove> history = myTurtle.history;
		int total = history.size();
		int x = startAtLine;
		if (x >= total)
			x = total - 1;
		if (x < 0)
			return 0;

		while (x > 1) {
			TurtleMove m = history.get(x);
			if (m.type == TurtleMove.TRAVEL)
				return x;
			--x;
		}

		return x;
	}

	public void closeConnection() {
		marlinInterface.closeConnection();
	}

	// TEST

	public static void main(String[] args) {
		PreferencesHelper.start();
		CommandLineOptions.setFromMain(args);
		Translator.start();

		JFrame frame = new JFrame(PlotterControls.class.getSimpleName());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new PlotterControls(new Plotter(), new Turtle()));
		frame.pack();
		frame.setVisible(true);
	}

}