
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class Wedge {
	public MatOfPoint contour;
	public Point center;
	public double angle;
	public double confidence;
	public Scalar color;

	public Wedge(MatOfPoint contour, Point center, double angle, double confidence, Scalar color) {
		this.contour = contour;
		this.center = center;
		this.angle = angle;
		this.confidence = confidence;
		this.color = color;
	}
}