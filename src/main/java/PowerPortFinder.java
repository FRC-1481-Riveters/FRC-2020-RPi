import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.core.*;

import org.opencv.imgproc.*;

public class PowerPortFinder extends PowerPort {

    private Mat m_lastImage;
    private MatOfPoint m_prototypePowerPortContour;
    private double m_maxMatchThreshold = 15.0;


    public PowerPortFinder() {

        /*
         * This is a simplified 8 point representation of a Power Port target. The first
         * two numbers are the index of the matrix that's being initialized: 0, 0 The
         * following numbers are coordinate pairs of x and y, e.g. (55, 39) to (99, 39)
         * are two points on the contour. They have a line segment between them.
         */
        m_prototypePowerPortContour = new MatOfPoint(new Point(55, 39), new Point(99, 39), new Point(260, 320),
                new Point(585, 320), new Point(745, 39), new Point(790, 39), new Point(605, 357), new Point(236, 357),
                new Point(55, 39));

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

    private List<PowerPortTarget> processPowerPortContours(List<MatOfPoint> contours) {

        List<PowerPortTarget> PowerPortTargets = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            double matchStrength = Imgproc.matchShapes(m_prototypePowerPortContour, contour, Imgproc.CONTOURS_MATCH_I3,
                    0.0);

            if (matchStrength < m_maxMatchThreshold) {
                try {
                    Moments p = Imgproc.moments(contour, false);

                    /*
                     * Determine the centroid (center) of the contour from the first order moments
                     */
                    double x = (p.get_m10() / p.get_m00());
                    double y = (p.get_m01() / p.get_m00());
                    
                    // TODO: Make the aimPoint better by targeting the middle of the line between the tips of the top of the contour.

                    double x_aimPoint = x;
                    double y_aimPoint = y;
                                       
                    
                    PowerPortTargets.add(new PowerPortTarget(contour, new Point(x, y), new Point (x_aimPoint, y_aimPoint), matchStrength));
                } catch (Exception ex) {

                }
            }
        }
        
        Collections.sort(PowerPortTargets);

        return PowerPortTargets;
    }

    public ArrayList<PowerPortTarget> getPowerPorts() {

        ArrayList<PowerPortTarget> PowerPorts = new ArrayList<PowerPortTarget>();

        PowerPorts.addAll(processPowerPortContours(filterContoursOutput()));

        return PowerPorts;
    }
    
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