package com.marginallyclever.makelangeloRobot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import com.jogamp.opengl.GL2;
import com.marginallyclever.artPipeline.ArtPipeline;
import com.marginallyclever.artPipeline.ArtPipelineListener;
import com.marginallyclever.artPipeline.loadAndSave.LoadAndSaveGCode;
import com.marginallyclever.communications.NetworkConnection;
import com.marginallyclever.communications.NetworkConnectionListener;
import com.marginallyclever.convenience.ColorRGB;
import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.convenience.log.LogPanelListener;
import com.marginallyclever.convenience.turtle.DefaultTurtleRenderer;
import com.marginallyclever.convenience.turtle.Turtle;
import com.marginallyclever.convenience.turtle.TurtleRenderer;
import com.marginallyclever.makelangelo.CommandLineOptions;
import com.marginallyclever.makelangelo.Makelangelo;
import com.marginallyclever.makelangelo.SoundSystem;
import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.preview.PreviewListener;
import com.marginallyclever.makelangeloRobot.machineStyles.MachineStyle;
import com.marginallyclever.makelangeloRobot.settings.MakelangeloRobotSettings;

/**
 * MakelangeloRobot is the Controller for a physical robot, following a
 * Model-View-Controller design pattern. It also contains non-persistent Model
 * data. MakelangeloRobotPanel is one of the Views. MakelangeloRobotSettings is
 * the persistent Model data (machine configuration).
 * 
 * @author dan
 * @since 7.2.10
 */
public class MakelangeloRobot implements NetworkConnectionListener, ArtPipelineListener, PreviewListener, LogPanelListener {
	// Firmware check
	private final String VERSION_CHECK_MESSAGE = new String("Firmware v");
	private final long EXPECTED_FIRMWARE_VERSION = 11; // must match the version in the the firmware EEPROM
	
	private MakelangeloRobotSettings settings = null;
	private MakelangeloRobotPanel myPanel = null;

	private TurtleRenderer turtleRenderer;
	
	// Talking to the real machine
	private NetworkConnection connection = null;
	private boolean identityConfirmed = false;
	private boolean portConfirmed = false;
	private boolean firmwareVersionChecked = false;
	private boolean hardwareVersionChecked = false;

	// misc state
	private boolean areMotorsEngaged;
	private boolean isRunning;
	private boolean isPaused;
	private boolean penIsUp;
	private boolean penJustMoved;
	private boolean penIsUpBeforePause;
	private boolean didSetHome;
	private double penX;
	private double penY;

	private Turtle turtleToRender;
	private Turtle TurtleLoaded;
	
	private ArtPipeline myPipeline;

	// this list of gcode commands is store separate from the Turtle.
	private ArrayList<String> drawingCommands;
	// what line in drawingCommands is going to be sent next?
	protected int drawingProgress;

	// rendering stuff
	private MakelangeloRobotDecorator decorator = null;

	// Listeners which should be notified of a change to the percentage.
	private ArrayList<MakelangeloRobotListener> listeners = new ArrayList<MakelangeloRobotListener>();

	public MakelangeloRobot() {
		super();
		settings = new MakelangeloRobotSettings();
		myPipeline = new ArtPipeline();
		myPipeline.addListener(this);
		portConfirmed = false;
		areMotorsEngaged = true;
		isRunning = false;
		isPaused = false;
		penIsUp = false;
		penJustMoved = false;
		penIsUpBeforePause = false;
		didSetHome = false;
		setPenX(0);
		setPenY(0);
		turtleToRender = new Turtle();
		drawingCommands = new ArrayList<String>();
		drawingProgress = 0;
	}

	public NetworkConnection getConnection() {
		return connection;
	}

	
	// @param c the connection.
	public void openConnection(NetworkConnection c) {
		if (c == null) return;

		Log.message("Opening connection...");
		portConfirmed = false;
		didSetHome = false;
		firmwareVersionChecked = false;
		hardwareVersionChecked = false;
		this.connection = c;
		this.connection.addListener(this);
		try {
			this.connection.sendMessage("M100\n");
		} catch (Exception e) {
			Log.error(e.getMessage());
		}
	}

	
	public void closeConnection() {
		if (this.connection == null)
			return;

		this.connection.closeConnection();
		this.connection.removeListener(this);
		notifyListeners(new MakelangeloRobotEvent(MakelangeloRobotEvent.DISCONNECT,this));
		this.connection = null;
		this.portConfirmed = false;
		this.identityConfirmed=false;
	}

	
	@Override
	public void finalize() {
		if (this.connection != null) {
			this.connection.removeListener(this);
		}
	}

	
	@Override
	public void sendBufferEmpty(NetworkConnection arg0) {
		sendFileCommand();
	}

	
	@Override
	public void dataAvailable(NetworkConnection arg0, String data) {
		if (data.endsWith("\n")) data = data.substring(0, data.length() - 1);
		Log.message("Recv: "+data);
		
		if(!identityConfirmed) {
			identityConfirmed = whenTheIntroductionsFinishOK(data);
			if(identityConfirmed) {
				Log.message("Identity confirmed.");
				notifyListeners(new MakelangeloRobotEvent(MakelangeloRobotEvent.CONNECTION_READY,this));
			}
		}
	}


	private boolean whenTheIntroductionsFinishOK(String data) {
		if(!portConfirmed) portConfirmed = doISeeAHello(data);
		
		if(!firmwareVersionChecked) {
			boolean isGood=doIseeMyFavoriteFirmwareVersion(data);
			if(isGood) {
				firmwareVersionChecked=true;
				// request the hardware version of this robot
				sendLineToRobot("D10\n");
			}
		}
		
		if(!hardwareVersionChecked) {
			String version=doISeeAHardwareVersion(data);
			if(version!=null) {
				hardwareVersionChecked=true;
				settings.setHardwareVersion(version);
			}
		}
		
		return portConfirmed && firmwareVersionChecked && hardwareVersionChecked;
	}

	private String doISeeAHardwareVersion(String data) {
		if(data.lastIndexOf("D10") >= 0) {
			String[] pieces = data.split(" ");
			if (pieces.length > 1) {
				String last = pieces[pieces.length - 1];
				Log.message("Heard "+data);
				Log.message("Hardware version check "+last);
				last = last.replace("\r\n", "");
				if (last.startsWith("V")) {
					String versionFound = last.substring(1).trim();
					Log.message("OK");
					return versionFound;
				} else {
					Log.message("BAD");
					notifyListeners(new MakelangeloRobotEvent(MakelangeloRobotEvent.BAD_HARDWARE, this, last));
				}
			}
		}

		return null;
	}

	private boolean doIseeMyFavoriteFirmwareVersion(String data) {
		if (data.lastIndexOf(VERSION_CHECK_MESSAGE) >= 0) {
			Log.message("Heard "+VERSION_CHECK_MESSAGE);
			String afterV = data.substring(VERSION_CHECK_MESSAGE.length()).trim();
			Log.message("Software version check "+afterV);
			
			long versionFound = 0;
			try {
				versionFound = Long.parseLong(afterV);
			} finally {}
			
			if (versionFound == EXPECTED_FIRMWARE_VERSION) {
				Log.message("OK");
				return true;
			} else {
				Log.message("BAD");
				notifyListeners(new MakelangeloRobotEvent(MakelangeloRobotEvent.BAD_FIRMWARE, this, versionFound));
			}
		}
		
		return false;
	}

	// introductions
	private boolean doISeeAHello(String data) {
		ServiceLoader<MachineStyle> knownHardware = ServiceLoader.load(MachineStyle.class);
		Iterator<MachineStyle> i = knownHardware.iterator();
		while (i.hasNext()) {
			MachineStyle ms = i.next();
			String myHello = ms.getHello();
			if (data.lastIndexOf(myHello) >= 0) {
				Log.message("Heard "+myHello+".  Port confirmed.");
				portConfirmed = true;
				// which machine GUID is this?
				String afterHello = data.substring(data.lastIndexOf(myHello) + myHello.length());
				settings.saveConfig();
				long id=findOrCreateA_UID(afterHello);
				settings.loadConfig(id);
				return true;
			}
		}
		
		return false;
	}

	
	public boolean isPortConfirmed() {
		return portConfirmed;
	}
	
 
	public long findOrCreateA_UID(String line) {
		long newUID = -1;
		
		// try to get the UID in the line
		String[] lines = line.split("\\r?\\n");
		if (lines.length > 0) {
			try {
				newUID = Long.parseLong(lines[0]);
				Log.message("UID found: "+newUID);
			} catch (NumberFormatException e) {
				Log.error("UID parsing failed.");
				Log.error("line="+line);
				Log.error(e.getMessage());
			}
		}

		// new robots have UID<=0
		if(newUID <= 0) newUID = getNewRobotUID();
		
		return newUID;
	}

	
	public void addListener(MakelangeloRobotListener listener) {
		listeners.add(listener);
	}

	public void removeListener(MakelangeloRobotListener listener) {
		listeners.remove(listener);
	}
	
	private void notifyListeners(MakelangeloRobotEvent e) {
		for (MakelangeloRobotListener listener : listeners) listener.makelangeloRobotUpdate(e);
	}

	
	@Override
	public void lineError(NetworkConnection arg0, int lineNumber) {
		drawingProgress = lineNumber;
	}

	/**
	 * based on http://www.exampledepot.com/egs/java.net/Post.html
	 */
	private long getNewRobotUID() {
		long newUID = 0;

		boolean pleaseGetAGUID = !CommandLineOptions.hasOption("-noguid");
		if (!pleaseGetAGUID)
			return 0;

		Log.message("obtaining UID from server.");
		try {
			// Send request
			URL url = new URL("https://www.marginallyclever.com/drawbot_getuid.php");
			URLConnection conn = url.openConnection();
			// get results
			InputStream connectionInputStream = conn.getInputStream();
			Reader inputStreamReader = new InputStreamReader(connectionInputStream);
			BufferedReader rd = new BufferedReader(inputStreamReader);
			String line = rd.readLine();
			Log.message("Server says: '" + line + "'");
			newUID = Long.parseLong(line);
			// did read go ok?
			if (newUID != 0) {
				settings.createNewUID(newUID);

				try {
					// Tell the robot it's new UID.
					connection.sendMessage("UID " + newUID);
				} catch (Exception e) {
					// FIXME has this ever happened? Deal with it better?
					Log.error("UID to robot: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			Log.error("UID from server: " + e.getMessage());
			return 0;
		}

		return newUID;
	}

	public String generateChecksum(String line) {
		byte checksum = 0;

		for (int i = 0; i < line.length(); ++i) {
			checksum ^= line.charAt(i);
		}

		return "*" + Integer.toString(checksum);
	}

	// Send the machine configuration to the robot.
	public void sendConfig() {
		if(!identityConfirmed) return;

		Log.message("Sending config.");
		String config = settings.getGCodeConfig();
		String[] lines = config.split("\n");
		for( String line : lines ) sendLineToRobot(line + "\n");
		setHome();
		sendLineToRobot("G0"
				+" F" + StringHelper.formatDouble(settings.getTravelFeedRate()) 
				+" A" + StringHelper.formatDouble(settings.getAcceleration())
				+"\n");
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean isPaused() {
		return isPaused;
	}

	public void pause() {
		if (isPaused)
			return;

		isPaused = true;
		// remember for later if the pen is down
		penIsUpBeforePause = penIsUp;
		// raise it if needed.
		raisePen();
	}

	public void unPause() {
		if (!isPaused)
			return;

		// if pen was down before pause, lower it
		if (!penIsUpBeforePause) {
			lowerPen();
		}

		isPaused = false;
	}

	public void halt() {
		isRunning = false;
		isPaused = false;
		raisePen();
		notifyListeners(new MakelangeloRobotEvent(MakelangeloRobotEvent.STOP,this));
	}

	public void setRunning() {
		isRunning = true;
		notifyListeners(new MakelangeloRobotEvent(MakelangeloRobotEvent.START,this));
	}

	public void raisePen() {
		sendLineToRobot(settings.getPenUpString());
		rememberRaisedPen();
	}
	
	protected void rememberRaisedPen() {
		penJustMoved = !penIsUp;
		penIsUp = true;
	}
	
	protected void rememberLoweredPen() {
		penJustMoved = penIsUp;
		penIsUp = false;
	}

	public void lowerPen() {
		sendLineToRobot(settings.getPenDownString());
		rememberLoweredPen();
	}

	public void testPenAngle(double testAngle) {
		sendLineToRobot(MakelangeloRobotSettings.COMMAND_DRAW + " Z" + StringHelper.formatDouble(testAngle));
	}

	/**
	 * removes comments, processes commands robot doesn't handle, add checksum
	 * information.
	 *
	 * @param line command to send
	 */
	public void sendLineWithNumberAndChecksum(String line, int lineNumber) {
		if (getConnection() == null || !isPortConfirmed() || !isRunning())
			return;

		line = "N" + lineNumber + " " + line;
		if(!line.endsWith(";")) line += ';';
		line += generateChecksum(line);
		sendLineToRobot(line);
	}

	
	// Take the next line from the file and send it to the robot, if permitted.
	public void sendFileCommand() {
		int total = drawingCommands.size();

		if(!isRunning() || isPaused() || total == 0 || identityConfirmed==true)
			return;

		// are there any more commands?
		if (drawingProgress >= total) {
			// no!
			halt();
			// bask in the glory
			if (myPanel != null)
				myPanel.statusBar.setProgress(total, total);

			SoundSystem.playDrawingFinishedSound();
		} else {
			String line = drawingCommands.get(drawingProgress);
			sendLineWithNumberAndChecksum(line, drawingProgress);
			drawingProgress++;

			// TODO update the simulated position to match the real robot?
			if (line.contains("G0") || line.contains("G1")) {
				double px = this.getPenX();
				double py = this.getPenY();

				String[] tokens = line.split(" ");
				for (String t : tokens) {
					if (t.startsWith("X")) px = Float.parseFloat(t.substring(1));
					if (t.startsWith("Y")) py = Float.parseFloat(t.substring(1));
				}
				this.setPenX(px);
				this.setPenY(py);
			}

			if (myPanel != null)
				myPanel.statusBar.setProgress(drawingProgress, total);
			// loop until we find a line that gets sent to the robot, at which
			// point we'll
			// pause for the robot to respond. Also stop at end of file.
		}
	}

	public void startAt(int lineNumber) {
		if (drawingCommands.size() == 0)
			return;

		drawingProgress = lineNumber;
		setLineNumber(lineNumber);
		setRunning();
		sendFileCommand();
	}

	/**
	 * display a dialog asking the user to change the pen
	 * 
	 * @param toolNumber a 24 bit RGB color of the new pen.
	 */
	public void requestUserChangeTool(int toolNumber) {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(10, 10, 10, 10);

		JLabel fieldValue = new JLabel("");
		fieldValue.setOpaque(true);
		fieldValue.setMinimumSize(new Dimension(80, 20));
		fieldValue.setMaximumSize(fieldValue.getMinimumSize());
		fieldValue.setPreferredSize(fieldValue.getMinimumSize());
		fieldValue.setSize(fieldValue.getMinimumSize());
		fieldValue.setBackground(new Color(toolNumber));
		fieldValue.setBorder(new LineBorder(Color.BLACK));
		panel.add(fieldValue, c);

		JLabel message = new JLabel(Translator.get("ChangeToolMessage"));
		c.gridx = 1;
		c.gridwidth = 3;
		panel.add(message, c);

		notifyListeners(new MakelangeloRobotEvent(MakelangeloRobotEvent.TOOL_CHANGE, this, toolNumber));
		Component root = null;
		MakelangeloRobotPanel p = this.getControlPanel();
		if(p != null) root = p.getRootPane();
		JOptionPane.showMessageDialog(root, panel, Translator.get("ChangeToolTitle"), JOptionPane.PLAIN_MESSAGE);
	}
	
	@Override
	public void commandFromLogPanel(String msg) {
		sendLineToRobot(msg);
	}

	/**
	 * Sends a single command the robot. Could be anything.
	 *
	 * @param line command to send.
	 * @return <code>true</code> if command was sent to the robot;
	 *         <code>false</code> otherwise.
	 */
	public boolean sendLineToRobot(String line) {
		if (getConnection() == null || !isPortConfirmed())
			return false;

		String reportedline = line;
		// does it have a checksum? hide it from the log
		if (reportedline.contains(";")) {
			String[] lines = line.split(";");
			reportedline = lines[0];
		}
		if (reportedline.trim().isEmpty()) {
			// nothing to send
			return false;
		}

		// remember important status changes
		
		if (reportedline.startsWith(settings.getPenUpString())) {
			rememberRaisedPen();
		}
		if (reportedline.startsWith(settings.getPenDownString())) {
			rememberLoweredPen();
		}
		if (reportedline.startsWith("M17")) {
			if( myPanel != null ) {
				myPanel.motorsHaveBeenEngaged();
			}
		}
		if (reportedline.startsWith("M18")) {
			if( myPanel != null ) {
				myPanel.motorsHaveBeenDisengaged();
			}
		}

		Log.message(reportedline);
		
		// make sure the line has a return on the end
		if(!line.endsWith("\n")) {
			line += "\n";
		}

		try {
			getConnection().sendMessage(line);
		} catch (Exception e) {
			Log.error(e.getMessage());
			return false;
		}
		return true;
	}

	
	public void setCurrentFeedRate(float feedRate) {
		// remember it
		settings.setDrawFeedRate(feedRate);
		// get it again in case it was capped.
		feedRate = settings.getDrawFeedRate();
		// tell the robot
		sendLineToRobot("G00 F" + StringHelper.formatDouble(feedRate));
	}

	
	public double getCurrentFeedRate() {
		return penIsUp
				? settings.getTravelFeedRate()
				: settings.getDrawFeedRate();
	}
	

	public void goHome() {
		movePenAbsolute(settings.getHomeX(),settings.getHomeY());
		penX=settings.getHomeX();
		penY=settings.getHomeY();
	}

	
	public void findHome() {
		this.raisePen();
		sendLineToRobot("G28");
		setPenX((float) settings.getHomeX());
		setPenY((float) settings.getHomeY());
	}

	
	public void setHome() {
		sendLineToRobot(settings.getGCodeTeleportToHomePosition());
		// save home position
		sendLineToRobot("D6"
						+" X" + StringHelper.formatDouble(settings.getHomeX())
						+" Y" + StringHelper.formatDouble(settings.getHomeY()));
		setPenX((float) settings.getHomeX());
		setPenY((float) settings.getHomeY());
		didSetHome = true;
	}

	
	public boolean didSetHome() {
		return didSetHome;
	}

	
	/**
	 * @param x absolute position in mm
	 * @param y absolute position in mm
	 */
	public void movePenAbsolute(double x, double y) {
		String go = (penJustMoved ? getTravelOrMoveWithFeedrate() : "");
		penJustMoved=false;
		sendLineToRobot(go
						+ " X" + StringHelper.formatDouble(x)
						+ " Y" + StringHelper.formatDouble(y));
		penX = x;
		penY = y;
		penJustMoved=false;
	}

	
	/**
	 * @param dx relative position in mm
	 * @param dy relative position in mm
	 */
	public void movePenRelative(float dx, float dy) {
		sendLineToRobot("G91"); // set relative mode

		String go = (penJustMoved ? getTravelOrMoveWithFeedrate() : "");
		penJustMoved=false;
		
		sendLineToRobot(go
						+ " X" + StringHelper.formatDouble(dx)
						+ " Y" + StringHelper.formatDouble(dy));
		sendLineToRobot("G90"); // return to absolute mode
		penX += dx;
		penY += dy;
	}

	
	private String getTravelOrMoveWithFeedrate() {
		if(penIsUp) return MakelangeloRobotSettings.COMMAND_TRAVEL +" "+ settings.getTravelFeedrateString();
		else        return MakelangeloRobotSettings.COMMAND_DRAW   +" "+ settings.getDrawFeedrateString();
	}

	
	public boolean areMotorsEngaged() {
		return areMotorsEngaged;
	}

	
	public void movePenToEdgeLeft() {
		movePenAbsolute(settings.getPaperLeft(),penY);
	}
	

	public void movePenToEdgeRight() {
		movePenAbsolute(settings.getPaperRight(),penY);
	}

	
	public void movePenToEdgeTop() {
		movePenAbsolute(getPenX(),settings.getPaperTop());
	}

	
	public void movePenToEdgeBottom() {
		movePenAbsolute(getPenX(),settings.getPaperBottom());
	}

	
	public void disengageMotors() {
		sendLineToRobot("M18");
		areMotorsEngaged = false;
	}
	

	public void engageMotors() {
		sendLineToRobot("M17");
		areMotorsEngaged = true;
	}

	
	public void jogLeftMotorOut() {
		sendLineToRobot("D00 L400");
	}

	
	public void jogLeftMotorIn() {
		sendLineToRobot("D00 L-400");
	}
	

	public void jogRightMotorOut() {
		sendLineToRobot("D00 R400");
	}
	

	public void jogRightMotorIn() {
		sendLineToRobot("D00 R-400");
	}

	
	public void setLineNumber(int newLineNumber) {
		sendLineToRobot("M110 N" + newLineNumber);
	}

	
	public MakelangeloRobotSettings getSettings() {
		return settings;
	}

	
	public MakelangeloRobotPanel createControlPanel(Makelangelo gui) {
		myPanel = new MakelangeloRobotPanel(gui, this);
		return myPanel;
	}

	
	public MakelangeloRobotPanel getControlPanel() {
		return myPanel;
	}

	
	public void setTurtle(Turtle t) {
		TurtleLoaded = new Turtle(t);
		myPipeline.processTurtle(TurtleLoaded, settings);
	}

	
	public void saveTurtleToDrawing(Turtle turtle) {
		int lineCount=0;
		try (final OutputStream fileOutputStream = new FileOutputStream("currentDrawing.ngc")) {
			LoadAndSaveGCode exportForDrawing = new LoadAndSaveGCode();
			exportForDrawing.save(fileOutputStream, this);

			drawingCommands.clear();
			BufferedReader reader = new BufferedReader(new FileReader("currentDrawing.ngc"));
			String line;

			while ((line = reader.readLine()) != null) {
				drawingCommands.add(line.trim());
				++lineCount;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		MakelangeloFirmwareSimulation m = new MakelangeloFirmwareSimulation(settings);
		double eta= m.getTimeEstimate(turtle);
		String msg = "Run time estimate=" +Log.secondsToHumanReadable(eta);
		Log.message(msg);
		System.out.println(msg);
		myPanel.statusBar.setProgressEstimate(eta, lineCount);
	}

	
	public Turtle getTurtle() {
		return turtleToRender;
	}

	
	public void setDecorator(MakelangeloRobotDecorator arg0) {
		decorator = arg0;
	}

	
	@SuppressWarnings("unused")
	private MakelangeloFirmwareVisualizer visualizer = new MakelangeloFirmwareVisualizer();
	
	
	@Override
	public void render(GL2 gl2) {
		float[] lineWidthBuf = new float[1];
		gl2.glGetFloatv(GL2.GL_LINE_WIDTH, lineWidthBuf, 0);
		
		// outside physical limits
		paintLimits(gl2);
		paintPaper(gl2);
		paintMargins(gl2);
		
		// hardware features
		settings.getHardwareProperties().render(gl2, this);

		gl2.glLineWidth(lineWidthBuf[0]);
		
		if (decorator != null) {
			// filters can also draw WYSIWYG previews while converting.
			decorator.render(gl2);
		} else if (turtleToRender != null) {
			if(turtleRenderer==null) {
				turtleRenderer = new DefaultTurtleRenderer(gl2);
				//turtleRenderer = new BarberPoleTurtleRenderer(gl2);
			}
			if(turtleRenderer!=null) {
				turtleToRender.render(turtleRenderer);
			}
			
			//visualizer.render(gl2,turtleToRender,settings);
		}
	}
	
	
	private void paintLimits(GL2 gl2) {
		gl2.glLineWidth(1);

		gl2.glColor3f(0.7f, 0.7f, 0.7f);
		gl2.glBegin(GL2.GL_TRIANGLE_FAN);
		gl2.glVertex2d(settings.getLimitLeft(), settings.getLimitTop());
		gl2.glVertex2d(settings.getLimitRight(), settings.getLimitTop());
		gl2.glVertex2d(settings.getLimitRight(), settings.getLimitBottom());
		gl2.glVertex2d(settings.getLimitLeft(), settings.getLimitBottom());
		gl2.glEnd();
	}
	
	
	private void paintPaper(GL2 gl2) {
		ColorRGB c = settings.getPaperColor();
		gl2.glColor3d(
				(double)c.getRed() / 255.0, 
				(double)c.getGreen() / 255.0, 
				(double)c.getBlue() / 255.0);
		gl2.glBegin(GL2.GL_TRIANGLE_FAN);
		gl2.glVertex2d(settings.getPaperLeft(), settings.getPaperTop());
		gl2.glVertex2d(settings.getPaperRight(), settings.getPaperTop());
		gl2.glVertex2d(settings.getPaperRight(), settings.getPaperBottom());
		gl2.glVertex2d(settings.getPaperLeft(), settings.getPaperBottom());
		gl2.glEnd();
	}
	
	
	private void paintMargins(GL2 gl2) {
		gl2.glPushMatrix();
		gl2.glColor3f(0.9f, 0.9f, 0.9f);
		gl2.glBegin(GL2.GL_LINE_LOOP);
		gl2.glVertex2d(settings.getMarginLeft(), settings.getMarginTop());
		gl2.glVertex2d(settings.getMarginRight(), settings.getMarginTop());
		gl2.glVertex2d(settings.getMarginRight(), settings.getMarginBottom());
		gl2.glVertex2d(settings.getMarginLeft(), settings.getMarginBottom());
		gl2.glEnd();
		gl2.glPopMatrix();
	}

	
	// in mm
	public double getPenX() {
		return penX;
	}
	

	// in mm
	public void setPenX(double px) {
		this.penX = px;
	}

	
	// in mm
	public double getPenY() {
		return penY;
	}

	
	// in mm
	public void setPenY(double py) {
		this.penY = py;
	}

	
	public int findLastPenUpBefore(int startAtLine) {
		int total = drawingCommands.size();
		if (total == 0)
			return 0;

		String toMatch = settings.getPenUpString();

		int x = startAtLine;
		if (x >= total)
			x = total - 1;

		toMatch = toMatch.trim();
		while (x > 1) {
			String line = drawingCommands.get(x).trim();
			if (line.equals(toMatch)) {
				return x;
			}
			--x;
		}

		return x;
	}

	public ArtPipeline getPipeline() {
		return myPipeline;
	}

	public boolean isPenIsUp() {
		return penIsUp;
	}

	@Override
	public void turtleFinished(Turtle t) {
		turtleToRender = t;
		saveTurtleToDrawing(t);		
	}
}
