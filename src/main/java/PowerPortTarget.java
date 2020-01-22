
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class PowerPortTarget {
	public MatOfPoint contour;
	public Point center;
	public double confidence;


	public PowerPortTarget(MatOfPoint contour, Point center, double confidence) {
		this.contour = contour;
		this.center = center;
		this.confidence = confidence;
	}
}
