import java.util.ArrayList;
import java.util.List;

import org.opencv.core.*;

import org.opencv.imgproc.*;

public class PowerPortFinder extends PowerPort {

    private Mat m_lastImage;
    private Mat m_prototypePowerPortContour;
    private double m_maxMatchThreshold = 500.0;

    public PowerPortFinder() {

        m_prototypePowerPortContour = new Mat(20, 1, CvType.CV_32FC2);
        /*
         * This is a simplified 20 point representation of a Power Port target. The
         * first two numbers are the index of the matrix that's being initialized: 0, 0
         * The following numbers are coordinate pairs of x and y, e.g. (39, 40) to (132,
         * 197) are two points on the contour. They have a line segment between them.
         */
        m_prototypePowerPortContour.put(0, 0, 39, 40, 132, 197, 148, 230, 161, 247, 182, 287, 189, 289, 477, 287, 494,
                263, 509, 231, 515, 226, 576, 115, 582, 110, 620, 44, 620, 39, 592, 39, 462, 266, 199, 266, 200, 262,
                194, 257, 70, 39);

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

    private ArrayList<PowerPortTarget> processPowerPortContours(List<MatOfPoint> contours) {

        ArrayList<PowerPortTarget> PowerPortTargets = new ArrayList<>();

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

                    PowerPortTargets.add(new PowerPortTarget(contour, new Point(x, y), matchStrength));
                } catch (Exception ex) {

                }
            }
        }

        return PowerPortTargets;
    }

    public ArrayList<PowerPortTarget> getPowerPorts() {

		ArrayList<PowerPortTarget> PowerPorts = new ArrayList<PowerPortTarget>();

		PowerPorts.addAll(processPowerPortContours(filterContoursOutput()));

		return PowerPorts;
	}

}