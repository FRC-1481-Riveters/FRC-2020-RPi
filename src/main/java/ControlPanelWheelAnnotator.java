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

public class ControlPanelWheelAnnotator extends ControlPanelWheel {
    private Mat m_lastImage;

    public void process(Mat source0) {
        m_lastImage = source0;
        super.process(source0);
    }

    public Mat getLastImage() {
        return m_lastImage;
    }

    public Mat annotateImage(Mat image) {


        List<MatOfPoint> contours;
        
        /* Draw all the red contours we found in red. */
        contours = super.convexHulls0Output();
        for (int index = 0; index < contours.size(); ++index) {
            Imgproc.drawContours(image, contours, index, new Scalar(0, 0, 255));
        }

        /* Draw all the yellow contours we found in yellow. */
        contours = super.convexHulls1Output();
        for (int index = 0; index < contours.size(); ++index) {
            Imgproc.drawContours(image, contours, index, new Scalar(0, 255, 255));
        }

        /* Draw all the green contours we found in green. */
        contours = super.convexHulls2Output();
        for (int index = 0; index < contours.size(); ++index) {
            Imgproc.drawContours(image, contours, index, new Scalar(0, 255, 0));
        }

        /* Draw all the blue contours we found in blue. */
        contours = super.convexHulls3Output();
        for (int index = 0; index < contours.size(); ++index) {
            Imgproc.drawContours(image, contours, index, new Scalar(255, 0, 0));
        }

        return image;
    }
}
