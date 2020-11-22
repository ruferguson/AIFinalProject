/* Ru Ferguson
 * 24 November 2020
 * 
 * This class creates a "snake." Used in the main to show where the body points are. */

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;

public class Snake {
   
	PApplet p;
	
	ArrayList<PVector> points;
	float xPos;
	float yPos;
	
	Snake(PApplet p, float curXPos, float curYPos) {
        this.p = p;
		xPos = curXPos;
		yPos = curYPos;
		points = new ArrayList<PVector>();
	}
	
	void addPoint(float curXPos, float curYPos) {
	    PVector point = new PVector(curXPos, curYPos);
	    points.add(0, point);
	}

	void drawPoints() {
	    float d = 50;
	    int f = 50;
	    int f2 = 50;
	    int t = 255;

	    for (int i = 0; i < points.size(); i++) {
	        PVector point = points.get(i);
	        p.fill(255, t);
	        p.ellipse(point.x, point.y, d, d);
	        d -= 1;
	        f += 1;
	        f2 += 2;
	        t -= 5;
	    }
	}

	void removePoints() {
	    while (points.size() > 50) {
	        int lastIndex = points.size() - 1;
	        points.remove(lastIndex);
	    }
	}
}
