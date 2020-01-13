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

public class ControlPanelWheelAnnotator extends ControlPanelWheelFinder {


    public Mat drawConfidenceValues(Mat matCamera, ArrayList<Wedge> wedges, Scalar annotationColor) {

        for (int index = 0; index < wedges.size(); ++index) {

            try {
                /*
                 * Find the center of the contour so we can write the confidence value as a
                 * number in the middle of the shape.
                 */
                Moments p = Imgproc.moments(wedges.get(index).contour, false);
                int x = (int) (p.get_m10() / p.get_m00());
                int y = (int) (p.get_m01() / p.get_m00());

                Imgproc.putText(matCamera, String.format("%.2f,%.0f", wedges.get(index).confidence,wedges.get(index).angle), new Point(x, y),
                        Core.FONT_HERSHEY_SIMPLEX, 0.75, annotationColor);
            } catch (Exception ex) {
                System.out.print("Couldn't find center of contour to annotate with matchStrength:" + ex.toString());
            }
        }

        return matCamera;
    }

    private Mat drawWedgeContours(Mat matCamera, List<MatOfPoint> contours, Scalar annotationColor) {

        try {
            for (int index = 0; index < contours.size(); ++index) {
                Imgproc.drawContours(matCamera, contours, index, annotationColor);
            }
        } catch (Exception ex) {
            System.out.print("Couldn't draw debug wedge contours:" + ex.toString());
        }
        return matCamera;
    }

    public Mat annotateImage(Mat image) {

        List<MatOfPoint> contours;

        /* Draw all the red contours we found in red. */
        contours = convexHulls0Output();
        drawWedgeContours(image, contours, new Scalar(0, 0, 255));

        /* Draw all the yellow contours we found in yellow. */
        contours = convexHulls1Output();
        drawWedgeContours(image, contours, new Scalar(0, 255, 255));

        /* Draw all the green contours we found in green. */
        contours = convexHulls2Output();
        drawWedgeContours(image, contours, new Scalar(0, 255, 0));

        /* Draw all the blue contours we found in blue. */
        contours = convexHulls3Output();
        drawWedgeContours(image, contours, new Scalar(255, 0, 0));

        return image;
    }
}
