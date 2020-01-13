import java.util.ArrayList;
import java.util.List;

import org.opencv.core.*;

import org.opencv.imgproc.*;

public class ControlPanelWheelFinder extends ControlPanelWheel {

	private Mat m_lastImage;
	private Mat m_prototypeWedgeContour;
	private double m_maxMatchThreshold = 0.25;

	public ControlPanelWheelFinder() {

		m_prototypeWedgeContour = new Mat(13, 1, CvType.CV_32FC2);
		/*
		 * This is a simplified 13 point representation of a Control Panel wedge. The
		 * first two numbers are the index of the matrix that's being initialized: 0, 0
		 * The following numbers are coordinate pairs of x and y, e.g. (86, 12) to (11,
		 * 206) are two points on the contour. They have a line segment between them.
		 */
		m_prototypeWedgeContour.put(0, 0, 86, 12, 11, 206, 34, 215, 55, 220, 56, 219, 72, 222, 73, 221, 74, 222, 99, 222, 100, 221,
				101, 222, 134, 216, 161, 206);

	}

	public void setMaxMatchThreshold(double newMaxMatchThreshold) {
		m_maxMatchThreshold = newMaxMatchThreshold;
	}

	public void process(Mat source0) {
		m_lastImage = source0;
		super.process(source0);
	}

	public Mat getLastImage() {
		return m_lastImage;
	}

	private ArrayList<Wedge> processWedgeContours(List<MatOfPoint> contours, Scalar annotationColor) {

		ArrayList<Wedge> wedges = new ArrayList<>();

		for (MatOfPoint contour : contours) {
			double matchStrength = Imgproc.matchShapes(m_prototypeWedgeContour, contour, Imgproc.CONTOURS_MATCH_I3, 0.0);

			if (matchStrength < m_maxMatchThreshold) {
				try {
					Moments p = Imgproc.moments(contour, false);

					/*
					 * Determine the centroid (center) of the contour from the first order moments
					 */
					double x = (p.get_m10() / p.get_m00());
					double y = (p.get_m01() / p.get_m00());

					/*
					 * Determine the orientation of the contour. See
					 * https://en.wikipedia.org/wiki/Image_moment#Examples_2 for more details
					 * 
					 * This method uses the second order central moments to determine the major and
					 * minor axis of the contour that are closest to the x and y axis. This means
					 * that as the contour turns, the major and minor axis with swap every 90
					 * degrees. This makes the angle go from -45 to 0 to 45 degrees.
					 * 0 degrees is parallel to either the y axis or the x axis.
					 */
					double u20 = (p.get_m20() / p.get_m00()) - Math.pow(x, 2);
					double u02 = (p.get_m02() / p.get_m00()) - Math.pow(y, 2);
					double u11 = (p.get_m11() / p.get_m00()) - (x * y);

					double theta = 0.5 * Math.atan(2 * u11 / (u20 - u02));

					wedges.add(new Wedge(contour, new Point(x, y), Math.toDegrees(theta), matchStrength, annotationColor));
				} catch (Exception ex) {

				}
			}
		}

		return wedges;
	}

	public ArrayList<Wedge> getWedges() {

		ArrayList<Wedge> wedges = new ArrayList<Wedge>();

		/* Draw all the red contours we found in red. */
		wedges.addAll(processWedgeContours(convexHulls0Output(), new Scalar(0, 0, 255)));

		/* Draw all the yellow contours we found in yellow. */
		wedges.addAll(processWedgeContours(convexHulls1Output(), new Scalar(0, 255, 255)));

		/* Draw all the green contours we found in green. */
		wedges.addAll(processWedgeContours(convexHulls2Output(), new Scalar(0, 255, 0)));

		/* Draw all the blue contours we found in blue. */
		wedges.addAll(processWedgeContours(convexHulls3Output(), new Scalar(255, 0, 0)));

		return wedges;
	}

	// }

	/*
	 * Rotated rectangle angles are weird. See
	 * https://namkeenman.wordpress.com/2015/12/18/open-cv-determine-angle-of-
	 * rotatedrect-minarearect/ for more details
	 * 
	 * This normalizes it to a nominal polar framework, where the 0 degrees is
	 * parallel to the x axis poing to the right. 180 is parallel to the x axis
	 * pointing to the left.
	 */
	double getAdjustedAngle(RotatedRect rRect) {
		double angle;
		if (rRect.size.width < rRect.size.height) {
			angle = 90 - rRect.angle;
		} else {
			angle = -rRect.angle;
		}

		return angle;
	}

}
