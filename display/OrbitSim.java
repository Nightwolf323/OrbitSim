package orbitsim.display;

import java.util.ArrayList;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import orbitsim.engine.Body;


//Questions to answer:
// Slider for timescale? Faster = less accurate, slower = more accurate
// Add buttons and text fields to change stats around bodies
// Eventually, display velocity vectors, maybe force vectors, and history path for objects


// Movement will be with a new KeyFrame for every single movement, with duration set based on timescale slider
// Each "tick" (every time the current animation is done) each body calculates where it should be next "tick"
// all of those positions are sent to the timeline via KeyFrames, and then the timeline is run until animation is completed
// Basically, GUI runs as fast as JavaFX does (because animations) but calculations are done depending on length of timeline
public class OrbitSim extends Application {
	ArrayList<Body> bodies = new ArrayList<Body>();
	Timeline timeline = new Timeline();

	public static void main(String[] args) {
		launch(args);
	}
	
	public void start(Stage stage) {
		
		Screen screen = Screen.getPrimary();
		Rectangle2D bounds = screen.getVisualBounds();

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
		
		Group root = new Group();
		root.getChildren().add(new Rectangle(bounds.getWidth(), bounds.getHeight(), Color.BLACK));
		
		Group canvas = new Group();
		root.getChildren().add(canvas);
		
		BodyControlManager bodyControlManager = new BodyControlManager(root);
		
		bodyControlManager.recreateGrid(2);
		
		// New bodies are added to the ArrayList
		bodies.add(new Body(10, 20, 20));
		bodies.add(new Body(20, 100, 100));
		
		// Display engine displays them at the beginning. Will probably want it to add the body's circle to the canvas any time one is created.
		for (int i = 0; i < bodies.size(); i++) {
			canvas.getChildren().add(bodies.get(i).getCircle());
		}
		
		canvas.setEffect(new BoxBlur(1, 1, 1));
		stage.setScene(new Scene(root, 1200, 800));
		
		
		// Create keyframes, then play it, and onFinished of last KeyFrame is function to clear KeyFrames, 
		// Stop timeline, and prompts recreation of next KeyFrame set
		timeline.getKeyFrames().addAll(
				new KeyFrame(Duration.ZERO, new KeyValue(bodies.get(0).getCircle().translateXProperty(), bodies.get(0).getCircle().getCenterX()), new KeyValue(bodies.get(0).getCircle().translateYProperty(), bodies.get(0).getCircle().getCenterY())),
				new KeyFrame(new Duration(2000), new clearTimeline(), new KeyValue(bodies.get(0).getCircle().translateXProperty(), 250), new KeyValue(bodies.get(0).getCircle().translateYProperty(), 250))
			);
		
		timeline.play();
		
		stage.show();
	}

	class clearTimeline implements EventHandler<ActionEvent> {

		public void handle(ActionEvent event) {
			timeline.stop();
			timeline.getKeyFrames().clear();


			timeline.getKeyFrames().addAll(
					new KeyFrame(Duration.ZERO, new KeyValue(bodies.get(0).getCircle().translateXProperty(), bodies.get(0).getCircle().getCenterX()), new KeyValue(bodies.get(0).getCircle().translateYProperty(), bodies.get(0).getCircle().getCenterY())),
					new KeyFrame(new Duration(5000), new KeyValue(bodies.get(0).getCircle().translateXProperty(), 400), new KeyValue(bodies.get(0).getCircle().translateYProperty(), 500))
				);
			
			timeline.play();
		}
		
	}
}
