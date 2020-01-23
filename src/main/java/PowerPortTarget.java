
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;


public class PowerPortTarget implements Comparable<PowerPortTarget> {
	public MatOfPoint contour;
	public Point center;
	public Point aimPoint;
	public double confidence;


	public PowerPortTarget(MatOfPoint contour, Point center, Point aimPoint, double confidence) {
		this.contour = contour;
		this.center = center;
		this.confidence = confidence;
		this.aimPoint = aimPoint;
	}


	@Override
	public int compareTo(PowerPortTarget o) {
		if (this.confidence > o.confidence) {
			return 1;
		}
		if (this.confidence < o.confidence) {
			return -1;
		}
		return 0;
	}

}
