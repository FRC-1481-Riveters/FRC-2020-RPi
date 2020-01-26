import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.core.*;

import org.opencv.imgproc.*;

public class PowerPortFinder extends PowerPort {

    private Mat m_lastImage;
    private MatOfPoint m_prototypePowerPortContour;
    private double m_maxMatchThreshold = 15.0;
    List<PowerPortTarget> m_powerPortTargets = new ArrayList<PowerPortTarget>();
    private double m_processingTime = 0;

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
        long startTime = System.currentTimeMillis();
        m_lastImage = source0;
        super.process(source0);

        m_powerPortTargets.clear();

        processPowerPortContours(filterContoursOutput());

        m_processingTime = System.currentTimeMillis() - startTime;
    }

    public Mat getLastImage() {
        return m_lastImage;
    }

    /*
     * Update the m_powerPortTargets list with the best match to the prototype power
     * port target prototype contour. Then sort the list of PowerPortTargets,
     * m_powerPortTargets such that the best match is the first element of the list.
     */
    private void processPowerPortContours(List<MatOfPoint> contours) {

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

                    // TODO: Make the aimPoint better by targeting the middle of the line between
                    // the tips of the top of the contour.

                    double x_aimPoint = x;
                    double y_aimPoint = y;

                    m_powerPortTargets.add(new PowerPortTarget(contour, new Point(x, y),
                            new Point(x_aimPoint, y_aimPoint), matchStrength));
                } catch (Exception ex) {

                }
            }
        }

        /*
         * Sort the List of PowerPortTargets such by ascending confidence value such
         * that the PowerPortTarget with the lowest value confidence (which is the best
         * match) is the first element of the list.
         */
        Collections.sort(m_powerPortTargets);
    }

    public List<PowerPortTarget> getPowerPorts() {
        return m_powerPortTargets;
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

    /*
     * Compute the best target's normalized heading. This heading is:
     * 
     * -1.0 if the target is all the way on the left side of the screen.
     * 
     * 0.0 if the target is in the center of the screen
     * 
     * 1.0 if the target is all the way on the right side if the screen.
     * 
     * and every number between if the target is somewhere between the left and
     * right side of the screen.
     */
    double getNormalizedCenter() {
        double targetInformation;
        try {
            targetInformation = getNormalizedCenter(m_powerPortTargets.get(0));
        } catch (Exception ex) {
            targetInformation = 0.0;
        }

        return targetInformation;
    }

    double getNormalizedCenter(PowerPortTarget powerPortTarget) {
        double targetInformation;
        try {
            targetInformation = 2.0f * ((powerPortTarget.aimPoint.x / (m_lastImage.cols() - 1)) - 0.5f);
        } catch (Exception ex) {
            targetInformation = 0.0;
        }
        return targetInformation;
    }

    double getProcessingTime() {
        return m_processingTime;
    }

    /*
     * Compute the distance to target using known features of the target, the
     * resolution and the FOV of the camera.
     * 
     * distance = Tin*FOVpixel/(2*Tpixel*tanΘ)
     * 
     * Where:
     * 
     * Θ is 1/2 of the FOV, in radians.
     * 
     * Tin is the actual width of the reference target, in inches.
     * 
     * FOVpixel is the width of the display in pixels (the horizontal resolution)
     * 
     * Tpixel is the width of the target the camera detected, in pixels
     * 
     * 
     * 
     * normalizedDistance = FOVPixel/(2*Tpixel*tanΘ)
     * 
     * Where:
     * 
     * Θ is 1/2 of the FOV, in radians.
     * 
     * FOVpixel is the width of the display in pixels (the horizontal resolution)
     * 
     * Tpixel is the width of the target the camera detected, in pixels
     * 
     * This gives you the distance from the target in units of target widths. Thus,
     * if this function returned 1.5, and the target is 10 inches across, you're 15
     * inches away from the target.
     */

    double getNormalizedTargetDistance(double fieldOfView) {
        double targetInformation;
        try {
            targetInformation = getNormalizedTargetDistance(fieldOfView, m_powerPortTargets.get(0));
        } catch (Exception ex) {
            targetInformation = Double.NaN;
        }

        return targetInformation;
    }

    double getNormalizedTargetDistance(double fieldOfView, PowerPortTarget powerPortTarget) {

        double targetDistance;

        try {
            /*
             * Determine the width of the target in pixels by creating a minAreaRectangle
             * around the target contour that we found. Then, use the width of this
             * minAreaRectangle as the width of the target in pixels.
             * 
             * This width is proportional to the actual width of the target in inches.
             */
            MatOfPoint2f dst = new MatOfPoint2f();
            powerPortTarget.contour.convertTo(dst, CvType.CV_32F);

            RotatedRect rRect = Imgproc.minAreaRect(dst);

            targetDistance = getLastImage().cols()
                    / (2.0 * rRect.size.width * Math.tan(Math.toRadians(fieldOfView / 2.0)));

        } catch (Exception ex) {
            targetDistance = Double.NaN;
        }

        return targetDistance;
    }
}