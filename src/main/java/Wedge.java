
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;

public class Wedge {
		MatOfPoint contour;
        public double angle;
        public double confidence;
        public Scalar color;

        public Wedge(MatOfPoint contour, double angle, double confidence, Scalar color) {
        	this.contour = contour;
            this.angle = angle;
            this.confidence = confidence;
            this.color = color;
        }
    }