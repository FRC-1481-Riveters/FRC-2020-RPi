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

public class PowerPortAnnotator extends PowerPortFinder {

	private Mat drawPowerPortAnnotations(Mat matCamera, ArrayList<PowerPortTarget> powerPorts, Scalar annotationColor) {

		for (int index = 0; index < powerPorts.size(); ++index) {

			Imgproc.drawMarker(matCamera, powerPorts.get(index).center, annotationColor);

			Imgproc.putText(matCamera, String.format("%.2f", powerPorts.get(index).confidence), powerPorts.get(index).center,
					Core.FONT_HERSHEY_SIMPLEX, 0.75, annotationColor);

		}

		return matCamera;
	}

	private Mat drawPowerPortContours(Mat matCamera, ArrayList<PowerPortTarget> powerPorts, Scalar annotationColor) {

		try {
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			for (int index = 0; index < powerPorts.size(); ++index) {

				contours.add(powerPorts.get(index).contour);
				Imgproc.drawContours(matCamera, contours, 0, annotationColor);
				contours.clear();
			}
		} catch (Exception ex) {
			System.out.print("Couldn't draw debug powerport contours:" + ex.toString());
		}
		return matCamera;
	}

	public Mat annotateImage(Mat image, ArrayList<PowerPortTarget> powerPorts) {

		/* Draw all the contours we found in red. */
		drawPowerPortContours(image, powerPorts, new Scalar(0, 0, 255));
		drawPowerPortAnnotations(image, powerPorts, new Scalar(0, 0, 255));

		return image;
	}
}