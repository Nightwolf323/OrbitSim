package orbitsim.engine;

import java.util.ArrayList;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import orbitsim.display.OrbitSim;
import orbitsim.display.TimelineManager;

public class Universe {
	ArrayList<Body> bodies = new ArrayList<Body>();
	TimelineManager timelineManager;
	OrbitSim main;
	static final double CONSTANT_G = 5000;
	int cyclesPerAnim;
	
	public Universe(OrbitSim sim) {
		main = sim;
		bodies.add(new Body(0.01, 700, 300, 20, 20));
		bodies.add(new Body(200, 600, 300, 0, 0));
		bodies.add(new Body(100, 100, 300, 10, 40));
		
		// Calculate how many integrations occur between each animation creation
		cyclesPerAnim = OrbitSim.animScale/OrbitSim.universeTick;
		System.out.println(cyclesPerAnim + " Cycles per animation");
	}

	public ArrayList<Body> getBodies() {
		return bodies;
	}
	
	public void setTLMgr(TimelineManager tl) {
		timelineManager = tl;
	}
	
	public void cycle() {
		// cycle through calculations until an animation
		for (int b = 1; b <= cyclesPerAnim; b++) {
			
			for (Body i : bodies) {
				for (Body a : bodies) {
					if (a != i) {
						if (checkIntersect(a.getCircle(),i.getCircle())) {
							handleCollision(a, i);
						}
					}
				}
			}
			
			for (Body i : bodies) {
				
				ArrayList<Vector> vecs = new ArrayList<Vector>();
				for (Body a:bodies) {
					if (a != i) {
						// Method for calculating based on total distance then splitting it to a vector
						double dx = i.getX() - a.getX();
						double dy = i.getY() - a.getY();
						double theta = Math.atan2(dy, dx);
						double distsq = Math.pow(dx, 2) + Math.pow(dy, 2);
						
						// g = -Gmm/r^2
						double f = -(Universe.CONSTANT_G * i.getMass() * a.getMass()) / distsq;
						double fx = f * Math.cos(theta);
						double fy = f * Math.sin(theta);
							
						// Calculation to calculate force vector is here
						vecs.add(new Vector(fx, fy));
					}
				}
				
				Vector finalVec = new Vector(0,0);
				
				// Basically just sum vectors
				for (Vector c : vecs) {
					finalVec = Vector.addVec(finalVec, c);
				}
				
				vecs.clear();

				//Basic euler integration for the new velocity and position of the body after one physics tick
				Vector eulerVel = new Vector(i.getVelX() + (finalVec.getX()/i.getMass())*(OrbitSim.universeTick/1000.0), i.getVelY() + (finalVec.getY()/i.getMass())*(OrbitSim.universeTick/1000.0));
				Vector eulerPos = new Vector(i.getX() + (eulerVel.getX()*(OrbitSim.universeTick/1000.0)), i.getY() + (eulerVel.getY()*(OrbitSim.universeTick/1000.0)));
				
				finalVec = new Vector(0,0);
				
				// Now calculate velocity again at the new position
				for (Body a:bodies) {
					if (a != i) {
						// Method for calculating based on total distance then splitting it to a vector
						double dx = eulerPos.getX() - a.getX();
						double dy = eulerPos.getY() - a.getY();
						double theta = Math.atan2(dy, dx);
						double distsq = Math.pow(2*dx, 2) + Math.pow(2*dy, 2);
						
						// g = -Gmm/r^2
						double f = -(Universe.CONSTANT_G * i.getMass() * a.getMass()) / distsq;
						double fx = f * Math.cos(theta);
						double fy = f * Math.sin(theta);
							
						// Calculation to calculate force vector is here
						vecs.add(new Vector(fx, fy));
					}
				}
				for (Vector c : vecs) {
					finalVec = Vector.addVec(finalVec, c);
				}
				
				Vector secondVel = new Vector(eulerVel.getX() + (finalVec.getX()/i.getMass())*(OrbitSim.universeTick/1000.0), eulerVel.getY() + (finalVec.getY()/i.getMass())*(OrbitSim.universeTick/1000.0));
				
				// Take the average of the two velocities to get the velocity the body will actually have
				Vector avgVel = new Vector((eulerVel.getX()+secondVel.getX())/2, (eulerVel.getY()+secondVel.getY())/2);
				
				i.queueVel(avgVel.getX(), avgVel.getY());
				// queue the position changes based on the new velocity (v*t)
				i.queuePos(i.getX() + (avgVel.getX()*(OrbitSim.universeTick/1000.0)), i.getY() + (avgVel.getY()*(OrbitSim.universeTick/1000.0)));
			}
			
			// tell all bodies to push the queued changes to their actual data
			for (Body i: bodies) {
				i.updateQueuedData();
				
				// Check if each body is intersecting with any other body
			}
			
		}
		// Once required number of cycles have run, animation needs to be created
		for (Body i : bodies) {
			// Add keyframes based on old and new positions (old pos is updated when new pos is created)

			timelineManager.getTimeline().getKeyFrames().addAll(
					new KeyFrame(Duration.ZERO, new KeyValue(i.getCircle().centerXProperty(), i.getOldX()), new KeyValue(i.getCircle().centerYProperty(), i.getOldY())),
					new KeyFrame(Duration.millis(OrbitSim.animScale), timelineManager, new KeyValue(i.getCircle().centerXProperty(), i.getX()), new KeyValue(i.getCircle().centerYProperty(), i.getY()))
				);
			
			// Draw line for previous step
			//TODO: Maybe animate it too so it draws along with the body moving animation
			// (not really necessary with such small calculation distances it's not noticable)
			
			Line l = new Line(i.getOldX(), i.getOldY(), i.getX(), i.getY());
			l.setStroke(Color.WHITESMOKE);
			main.getCanvas().getChildren().add(l);
			i.resetOldPos();
		}
		// Play the timeline
		timelineManager.getTimeline().play();
	}
	
	// Checker if two circles are intersecting (checking if something has collided)
	boolean checkIntersect(Circle a, Circle b) {
		return (Math.sqrt(Math.pow(a.getCenterX()-b.getCenterX(), 2) + Math.pow(a.getCenterY()-b.getCenterY(), 2)) < a.getRadius() + b.getRadius());
	}
	
	// Method to handle collisions, with conservation of momentum, combining bodies, etc
	void handleCollision(Body a, Body b) {
		double momentumAX = a.getMass() * a.getVelX();
		double momentumAY = a.getMass() * a.getVelY();
		double momentumBX = b.getMass() * b.getVelX();
		double momentumBY = b.getMass() * b.getVelY();
		
		double combinedMass = a.getMass() + b.getMass();
		
		double cVelX = (momentumAX + momentumBX)/combinedMass;
		double cVelY = (momentumAY + momentumBY)/combinedMass;
		Body c = new Body(combinedMass, (int)(a.getX()+b.getX())/2, (int)(a.getY() +b.getY())/2, cVelX, cVelY);
		bodies.add(c);
		
		queueRemoval(a);
		queueRemoval(b);
	}

//TODO: set it to build a list of bodies to remove, so that it can be run once collisions are all checked
	void queueRemoval(Body a) {
	}
}
