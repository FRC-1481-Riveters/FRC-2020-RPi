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


	public Mat drawWedgeAnnotations(Mat matCamera, ArrayList<Wedge> wedges, Scalar annotationColor) {

		for (int index = 0; index < wedges.size(); ++index) {

			Imgproc.drawMarker(matCamera, wedges.get(index).center, annotationColor);

			Imgproc.putText(matCamera, String.format("%.2f,%.1f", wedges.get(index).confidence, wedges.get(index).angle),
					wedges.get(index).center, Core.FONT_HERSHEY_SIMPLEX, 0.75, annotationColor);

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

        /* Draw all the red contours we found in red. */
        drawWedgeContours(image, convexHulls0Output(), new Scalar(0, 0, 255));

        /* Draw all the yellow contours we found in yellow. */
        drawWedgeContours(image, convexHulls1Output(), new Scalar(0, 255, 255));

        /* Draw all the green contours we found in green. */
        drawWedgeContours(image, convexHulls2Output(), new Scalar(0, 255, 0));

        /* Draw all the blue contours we found in blue. */
        drawWedgeContours(image, convexHulls3Output(), new Scalar(255, 0, 0));

        return image;
    }
}
