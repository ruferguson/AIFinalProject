/* Ru Ferguson
 * 24 November 2020
 * This project creates and prints prediction suffix trees based on ArrayList<T> inputs and
 * also implements Pmin elimination.
 * 
 */

import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.*; 

//importing the JMusic stuff
import jm.music.data.*;
import jm.JMC;
import jm.util.*;
import jm.midi.*;

import java.io.UnsupportedEncodingException;
import java.net.*;

//Import OSC
import oscP5.*;
import netP5.*;

public class AIFinalProject extends PApplet {
	
	String runwayHost = "127.0.0.1";   // Runway Host
	int runwayPort = 57100;	  // Runway Port
	
	OscP5 oscP5;
	NetAddress myBroadcastLocation;
	
	// This array will hold all the humans detected
	JSONObject data;
	JSONArray humans;
	
	PVector curPos, prevPos;
	
	float curXPos;
	float curYPos;
	float prevXPos;
	float prevYPos;
	float curVel;
	float curAccel;
	float prevVel;
	float prevAccel;
	float jerk;
	float jerkThresh;
	
	
	MelodyPlayer player; //play a midi sequence
	MidiFileToNotes midiNotes; //read a midi file
	String filePath;

	Tree<Integer> pitchTree;
	Tree<Double> rhythmTree;
	
	Snake snake;
	
	public static void main(String[] args) {
		PApplet.main("AIFinalProject"); 
	}

	//setting the window size
	public void settings() {
		size(600, 600);
	}

	public void setup() {						
		// returns a url
		//filePath = getPath("mid/Super_Mario_Bros_Theme.mid"); // locate midi file
		//filePath = getPath("mid/pirates.mid"); // locate midi file
		filePath = getPath("mid/holst_jupiter.mid"); // locate midi file
		//filePath = getPath("mid/gardel_por.mid"); // locate midi file
		//filePath = getPath("mid/Mii_Channel.mid"); // locate midi file

		midiNotes = new MidiFileToNotes(filePath); //creates a new MidiFileToNotes

	    // which line to read in --> this object only reads one line (or ie, voice or ie, one instrument)'s worth of data from the file
		midiNotes.setWhichLine(0);

		player = new MelodyPlayer(this, 100.0f);
		
		player.setup();	
		
		// create the trees
		pitchTree = new Tree<Integer>(3, 0.1, 1.5);
		rhythmTree = new Tree<Double>(3, 0.1, 1.5);
		// train the trees
		pitchTree.train(midiNotes.getPitchArray());
		rhythmTree.train(midiNotes.getRhythmArray());
		// generate a new melody and play it
		player.setMelody(pitchTree.generate(30));
		player.setRhythm(rhythmTree.generate(30));
		
		//player.setMelody(midiNotes.getPitchArray());
		//player.setRhythm(midiNotes.getRhythmArray());

		curPos = new PVector(0, 0);
		prevPos = new PVector(0, 0);
		prevVel = 0;
		prevAccel = 0;
		jerk = 0;
		jerkThresh = (float) 0.40;
		
		snake = new Snake(this, curXPos, curYPos);
		
		frameRate(25);
		OscProperties properties = new OscProperties();
		properties.setRemoteAddress("127.0.0.1", 57200);
		properties.setListeningPort(57200);
		properties.setDatagramSize(99999999);
		properties.setSRSP(OscProperties.ON);
		oscP5 = new OscP5(this, properties);
	
		// Use the localhost and the port 57100 that we define in Runway
		myBroadcastLocation = new NetAddress(runwayHost, runwayPort);
		connect();

		fill(255);
		noStroke();
	}
	

	public void draw() {
	    player.play();		//play each note in the sequence -- the player will determine whether is time for a note onset
	    background(0);
	    showInstructions();
	    calcJerk();
	    motionBang();
	}

	//this finds the absolute path of a file
	String getPath(String path) {

		String filePath = "";
		try {
			filePath = URLDecoder.decode(getClass().getResource(path).getPath(), "UTF-8");

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return filePath;
	}

	// this starts & restarts the melody and runs unit tests
	public void keyPressed() {
		if (key == ' ') {
			player.reset();
			player.setMelody(midiNotes.getPitchArray());
			player.setRhythm(midiNotes.getRhythmArray());
			player.hasMelody = true; // starts the player
			println("Melody started!");
		} else if (key == 'p') {
			System.out.println("Generating notes . . . enjoy!");
		} else if (key == '1') {
		} else if (key == 'o') {		
			player.hasMelody = false; // stops the player
		} 
	}
	
	
	void connect() {
		OscMessage m = new OscMessage("/server/connect");
		oscP5.send(m, myBroadcastLocation);
	}
	
	// OSC Event: listens to data coming from Runway
	void oscEvent(OscMessage message) {
		if (!message.addrPattern().equals("/data")) return;
		String dataString = message.get(0).stringValue();	// The data is in a JSON string, so first we get the string value
		data = parseJSONObject(dataString);	// We then parse it as a JSONObject
	}
	
	void updatePosition() {
		if (data != null) {
		    humans = data.getJSONArray("poses");
		    for(int h = 0; h < humans.size(); h++) {
		      JSONArray keypoints = humans.getJSONArray(h);
		      JSONArray point = keypoints.getJSONArray(10);
			  prevXPos = curXPos;
			  prevYPos = curYPos;
			  curXPos = point.getFloat(0);
			  curYPos = point.getFloat(1);
		      drawControlPoint();
		    }
		}
	}
	
	void calcJerk() {
		updatePosition();
		curPos.set(curXPos, curYPos);
		prevPos.set(prevXPos, prevYPos);
		
	    if (!prevPos.equals(curPos)) { // if the position has changed
			prevVel = curVel; // save current velocity as previous velocity
			curVel = PVector.dist(curPos, prevPos); // calculate current velocity
			
			prevAccel = curAccel; // save current acceleration as previous acceleration
			curAccel = prevVel - curVel; // calculate current acceleration
			
			jerk = prevAccel - curAccel; // calculate current jerk

			System.out.println("curPos: " + curPos + " prevPos: " + prevPos + " curVel: " + curVel + " prevVel: " + prevVel + " curAccel: " + curAccel + " prevAccel: " + prevAccel + " jerk: " + jerk);                              
	    }
	}
	
	// A function to draw humans body parts as circles
	void drawControlPoint() {
		// right wrist ID = 10
	    snake.addPoint(curXPos * width, curYPos * height);
	    snake.drawPoints();
	    snake.removePoints();
	    
	    //ellipse(curXPos * width, curYPos * height, 10, 10);
	}
	
	void motionBang() {
		jerk = abs(jerk);
		if (jerk > jerkThresh) {
			player.reset();
		}
	}
	
	// display instructions to the user
	public void showInstructions() {
		fill(255, 100);
		rect(0, 500, 600, 100);
		textAlign(LEFT);
		textSize(30);
		fill(255, 75, 75);
		text("Welcome to the", width/2, height*2/10);
		text("PST Generator", width/2, height*3/10);
		textSize(18);
		fill(225, 50, 75);
		text("Now with Pmin Elimination 0.1 and 0.15", width/2, height*4/10);
		fill(225, 75, 90);
		text("Press 1 for Project 5: Unit Test 1", width/2, height*6/10);
		fill(225, 75, 105);
		text("Press 2 for Project 5: Unit Test 2", width/2, height*7/10); 
		fill(225, 75, 120);
		text("Press 3 for Project 5: Unit Test 3", width/2, height*8/10);
		fill(225, 75, 135);
		text("Press 4 for Project 5: Unit Test 4", width/2, height*9/10);
	}
	
}
