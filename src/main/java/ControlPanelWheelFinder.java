import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.*;
import org.opencv.core.Core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;

import visionhelper.contourHelper;

public class ControlPanelWheelFinder extends ControlPanelWheel {

	private Mat m_lastImage;
	private Mat m_prototypeWedgeContour;
	private double m_maxMatchThreshold = 0.6;

	private contourHelper m_contourHelper = new contourHelper();

	public ControlPanelWheelFinder() {

		m_prototypeWedgeContour = new Mat(13, 1, CvType.CV_32FC2);
		/*
		 * This is a simplified 13 point represenation of a Control Panel wedge. The
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

	private ArrayList<Wedge> processWedgeContours(List<MatOfPoint> contours, Scalar color) {

		ArrayList<Wedge> wedges = new ArrayList<>();

		for (MatOfPoint contour : contours) {
			double matchStrength = Imgproc.matchShapes(m_prototypeWedgeContour, contour, Imgproc.CONTOURS_MATCH_I3, 0.0);

			if (matchStrength < m_maxMatchThreshold) {
				wedges.add(
						new Wedge(contour, getAdjustedAngle(m_contourHelper.getRotatedRectangle(contour)), matchStrength, color));
			}
		}

		return wedges;
	}

	public ArrayList<Wedge> getWedges() {

		ArrayList<Wedge> wedges = new ArrayList<Wedge>();

		/* Draw all the red contours we found in red. */
		wedges.addAll(processWedgeContours(convexHulls0Output(), new Scalar(0,0,255)));

		/* Draw all the yellow contours we found in yellow. */
		wedges.addAll(processWedgeContours(convexHulls1Output(), new Scalar(0,255,255)));

		/* Draw all the green contours we found in green. */
		wedges.addAll(processWedgeContours(convexHulls2Output(), new Scalar(0,255,0)));

		/* Draw all the blue contours we found in blue. */
		wedges.addAll(processWedgeContours(convexHulls3Output(), new Scalar(255,0,0)));

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
