/* Ru Ferguson
 * 4 November 2020
 * This project creates and prints prediction suffix trees based on ArrayList<T> inputs and
 * also implements Pmin elimination.
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

//import javax.sound.midi.*;

//Import OSC
import oscP5.*;
import netP5.*;


//make sure this class name matches your file name, if not fix.
public class AIFinalProject extends PApplet {
	
	// Runway Host
	String runwayHost = "127.0.0.1";

	// Runway Port
	int runwayPort = 57100;
	
	OscP5 oscP5;
	NetAddress myBroadcastLocation;
	
	// This array will hold all the humans detected
	JSONObject data;
	JSONArray humans;

	// This are the pair of body connections we want to form. 
	// Try creating new ones!
	String[][] connections = {
	  {"nose", "leftEye"},
	  {"leftEye", "leftEar"},
	  {"nose", "rightEye"},
	  {"rightEye", "rightEar"},
	  {"rightShoulder", "rightElbow"},
	  {"rightElbow", "rightWrist"},
	  {"leftShoulder", "leftElbow"},
	  {"leftElbow", "leftWrist"}, 
	  {"rightHip", "rightKnee"},
	  {"rightKnee", "rightAnkle"},
	  {"leftHip", "leftKnee"},
	  {"leftKnee", "leftAnkle"}
	};
	
	PVector xPosVect, yPosVect;

	float xPos;
	float yPos;
	float lastXPos;
	float lastYPos;
	float vel;
	float accel;
	float jerk;

	
	
	MelodyPlayer player; //play a midi sequence
	MidiFileToNotes midiNotes; //read a midi file
	String filePath;

	Tree<Integer> pitchTree;
	Tree<Double> rhythmTree;
	
	public static void main(String[] args) {
		PApplet.main("AIFinalProject"); 
	}

	//setting the window size
	public void settings() {
		size(500, 500);
	}

	public void setup() {						
		// returns a url
		filePath = getPath("mid/Super_Mario_Bros_Theme.mid"); // locate midi file
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
		
		System.out.println("generated pitches: " + pitchTree.generate(10));
		System.out.println("generated rhythms: " + rhythmTree.generate(10));

		
		// frameRate(25);
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
		stroke(255);
	}
	

	public void draw() {
	    player.play();		//play each note in the sequence -- the player will determine whether is time for a note onset
	    //background(250);
	    //showInstructions();
	    
	    
	    background(0);
	    // Choose between drawing just an ellipse
	    // over the body parts or drawing connections
	    // between them. or both!
	    drawParts();
	    calcJerk();
	    //System.out.println("x: " + xPos + " y: " + yPos);
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

	// this function is not currently called. you may call this from setup() if you want to test
	// this just plays the midi file -- all of it via your software synth. You will not use this
	// function in upcoming projects but it could be a good debug tool.
	void playMidiFile(String filename) {
		Score theScore = new Score("Temporary score");
		Read.midi(theScore, filename);
		Play.midi(theScore);
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
	
	// A function to draw humans body parts as circles
	void drawParts() {
	  // Only if there are any humans detected
	  if (data != null) {
	    humans = data.getJSONArray("poses");
	    for(int h = 0; h < humans.size(); h++) {
	      JSONArray keypoints = humans.getJSONArray(h);
	      // Now that we have one human, let's draw its body parts
	      for (int k = 0; k < keypoints.size(); k++) {
	        // Body parts are relative to width and weight of the input
	        JSONArray point = keypoints.getJSONArray(k);
	        float x = point.getFloat(0);
	        float y = point.getFloat(1);
	        ellipse(x * width, y * height, 10, 10);
	      }
	    }
	  }
	}
	
	void connect() {
		  OscMessage m = new OscMessage("/server/connect");
		  oscP5.send(m, myBroadcastLocation);
	}
	
	// OSC Event: listens to data coming from Runway
	void oscEvent(OscMessage theOscMessage) {
	  if (!theOscMessage.addrPattern().equals("/data")) return;
	  // The data is in a JSON string, so first we get the string value
	  String dataString = theOscMessage.get(0).stringValue();

	  // We then parse it as a JSONObject
	  data = parseJSONObject(dataString);
	}
	
	void calcJerk() {
		if (data != null) {
		    humans = data.getJSONArray("poses");
		    for(int h = 0; h < humans.size(); h++) {
		      JSONArray keypoints = humans.getJSONArray(h);
		      
		      // 0 = nose
		      JSONArray point = keypoints.getJSONArray(0);
		      lastXPos = xPos;
		      lastYPos = yPos;
		      xPos = point.getFloat(0);
		      yPos = point.getFloat(1);
		     // xPosVect.set(lastXPos, xPos);
		     // yPosVect.set(lastYPos, yPos);
		      vel = (yPos - lastYPos) / (xPos - lastXPos);
		    }
		}
	}
	
	
	// display instructions to the user
	public void showInstructions() {
		textAlign(CENTER);
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
