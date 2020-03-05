/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionThread;

import org.opencv.core.Mat;

import java.lang.Runtime;
import java.util.Date;
/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // 
                              "FOV": <camera's horizontal field of view in degrees> // optional (150 degrees if not specified)
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ],
               "stream": {                              // optional
                   "properties": [
                       {
                           "name": <stream property name>
                           "value": <stream property value>
                       }
                   ]
               }
           }
       ]
       "switched cameras": [
           {
               "name": <virtual camera name>
               "key": <network table key used for selection>
               // if NT value is a string, it's treated as a name
               // if NT value is a double, it's treated as an integer index
           }
       ]
   }
 */

public final class Main {
  private static String configFile = "/boot/frc.json";

  @SuppressWarnings("MemberName")
  public static class CameraConfig {
    public String name;
    public String path;
    public JsonObject config;
    public JsonElement streamConfig;
  }

  @SuppressWarnings("MemberName")
  public static class SwitchedCameraConfig {
    public String name;
    public String key;
  };

  public static int team;
  public static boolean server;
  public static List<CameraConfig> cameraConfigs = new ArrayList<>();
  public static List<SwitchedCameraConfig> switchedCameraConfigs = new ArrayList<>();
  public static List<VideoSource> cameras = new ArrayList<>();

  static long fieldOfView = 60;

  static long autoAssistConnectionTestLastReceivedTimeStamp;

  private Main() {
  }

  /**
   * Report parse error.
   */
  public static void parseError(String str) {
    System.err.println("config error in '" + configFile + "': " + str);
  }

  /**
   * Read single camera configuration.
   */
  public static boolean readCameraConfig(JsonObject config) {
    CameraConfig cam = new CameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement pathElement = config.get("path");
    if (pathElement == null) {
      parseError("camera '" + cam.name + "': could not read path");
      return false;
    }
    cam.path = pathElement.getAsString();

    // stream properties
    cam.streamConfig = config.get("stream");

    cam.config = config;

    cameraConfigs.add(cam);
    return true;
  }

  /**
   * Read single switched camera configuration.
   */
  public static boolean readSwitchedCameraConfig(JsonObject config) {
    SwitchedCameraConfig cam = new SwitchedCameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read switched camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement keyElement = config.get("key");
    if (keyElement == null) {
      parseError("switched camera '" + cam.name + "': could not read key");
      return false;
    }
    cam.key = keyElement.getAsString();

    switchedCameraConfigs.add(cam);
    return true;
  }

  /**
   * Read configuration file.
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  public static boolean readConfig() {
    // parse file
    JsonElement top;
    try {
      top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
    } catch (IOException ex) {
      System.err.println("could not open '" + configFile + "': " + ex);
      return false;
    }

    // top level must be an object
    if (!top.isJsonObject()) {
      parseError("must be JSON object");
      return false;
    }
    JsonObject obj = top.getAsJsonObject();

    // team number
    JsonElement teamElement = obj.get("team");
    if (teamElement == null) {
      parseError("could not read team number");
      return false;
    }
    team = teamElement.getAsInt();

    // ntmode (optional)
    if (obj.has("ntmode")) {
      String str = obj.get("ntmode").getAsString();
      if ("client".equalsIgnoreCase(str)) {
        server = false;
      } else if ("server".equalsIgnoreCase(str)) {
        server = true;
      } else {
        parseError("could not understand ntmode value '" + str + "'");
      }
    }

    // cameras
    JsonElement camerasElement = obj.get("cameras");
    if (camerasElement == null) {
      parseError("could not read cameras");
      return false;
    }
    JsonArray cameras = camerasElement.getAsJsonArray();
    for (JsonElement camera : cameras) {
      if (!readCameraConfig(camera.getAsJsonObject())) {
        return false;
      }
    }

    if (obj.has("switched cameras")) {
      JsonArray switchedCameras = obj.get("switched cameras").getAsJsonArray();
      for (JsonElement camera : switchedCameras) {
        if (!readSwitchedCameraConfig(camera.getAsJsonObject())) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Start running the camera.
   */
  public static VideoSource startCamera(CameraConfig config) {
    System.out.println("Starting camera '" + config.name + "' on " + config.path);
    CameraServer inst = CameraServer.getInstance();
    UsbCamera camera = new UsbCamera(config.name, config.path);
    MjpegServer server = inst.startAutomaticCapture(camera);

    Gson gson = new GsonBuilder().create();

    camera.setConfigJson(gson.toJson(config.config));
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    if (config.streamConfig != null) {
      server.setConfigJson(gson.toJson(config.streamConfig));
    }

    return camera;
  }

  /**
   * Start running the switched camera.
   */
  public static MjpegServer startSwitchedCamera(SwitchedCameraConfig config) {
    System.out.println("Starting switched camera '" + config.name + "' on " + config.key);
    MjpegServer server = CameraServer.getInstance().addSwitchedCamera(config.name);

    NetworkTableInstance.getDefault()
        .getEntry(config.key)
        .addListener(event -> {
              if (event.value.isDouble()) {
                int i = (int) event.value.getDouble();
                if (i >= 0 && i < cameras.size()) {
                  server.setSource(cameras.get(i));
                }
              } else if (event.value.isString()) {
                String str = event.value.getString();
                for (int i = 0; i < cameraConfigs.size(); i++) {
                  if (str.equals(cameraConfigs.get(i).name)) {
                    server.setSource(cameras.get(i));
                    break;
                  }
                }
              }
            },
            EntryListenerFlags.kImmediate | EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
         
    return server;
  }

  /**
   * Example pipeline.
   */
  public static class MyPipeline implements VisionPipeline {
    public int val;

    @Override
    public void process(Mat mat) {
      val += 1;
    }
  }

  /**
   * Main.
   */
  public static void main(String... args) {
    if (args.length > 0) {
      configFile = args[0];
    }

    // read configuration
    if (!readConfig()) {
      return;
    }

    // start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    if (server) {
      System.out.println("Setting up NetworkTables server");
      ntinst.startServer();
    } else {
      System.out.println("Setting up NetworkTables client for team " + team);
      ntinst.startClientTeam(team);
    }

    // start cameras
    for (CameraConfig config : cameraConfigs) {
      cameras.add(startCamera(config));
    }

    // start switched cameras
    for (SwitchedCameraConfig config : switchedCameraConfigs) {
      startSwitchedCamera(config);
    }
    /*
     * Set the update rate to slower than normal, and call the flush() instead to
     * send the target information with low latency.
     *
     * https://www.chiefdelphi.com/t/networking-a-raspberry-pi/335503/16
     */
    ntinst.setUpdateRate(1.0);

    NetworkTableEntry autoAssistConnectionTest = NetworkTableInstance.getDefault().getTable("Vision")
        .getEntry("autoAssistConnectionTest");
    /*
     * Get a timestamp that represents the last time we received something from the
     * Roborio. This particular signal is transmitted every 500 ms. Thus, this is a
     * good "Hi. I'm the roborio and I'm listening to you." message. Use this
     * timestamp later to see if the roborio has stopped listening to us.
     * 
     * Try to coordinate system times between the roborio and the raspberry pi so
     * that timestamps on annotated video is easy to combine with log entries.
     */
    autoAssistConnectionTest.addListener(event -> {

      double millisecondsSinceEpochOnRoboRIO = event.value.getDouble();

      /*
       * Check if the RPi's system time is more than 0.5 seconds different from the
       * RoboRIO's system time.
       * 
       * If it is, update the RPi's system clock to the RoboRIO's system clock.
       */
      if (Math.abs(System.currentTimeMillis() - (long) millisecondsSinceEpochOnRoboRIO) > 500) {

        try {
          Runtime runTime = Runtime.getRuntime();
          runTime.exec(String.format("sudo date -s @%.3f", millisecondsSinceEpochOnRoboRIO / 1000.0));
          System.out.format("Updated the RPi's system clock to %s%n", new Date().toString());
        } catch (Exception e) {
          System.out.format("Couldn't set system time from %.3f:%s%n", millisecondsSinceEpochOnRoboRIO / 1000.0,
              e.toString());
        }

      }

      autoAssistConnectionTestLastReceivedTimeStamp = System.currentTimeMillis();

    }, EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

    /*
     * Find a camera named "Target". If there's a camera named Target, start
     * PowerPort vision targeting on it and send the target results back to the
     * RoboRIO to help it aim toward the PowerPort target.
     */
    {
      String str = new String("Target");

      for (int cameraConfigsIndex = 0; cameraConfigsIndex < cameraConfigs.size(); cameraConfigsIndex++) {
        if (str.equals(cameraConfigs.get(cameraConfigsIndex).name)) {

          // start image processing on camera "Target" if present

          CvSource outputStream = CameraServer.getInstance().putVideo("Annotated PowerPort stream", 160, 120);

          NetworkTableEntry targetInformation = ntinst.getTable("Vision").getEntry("targetInformation");

          try {
            /*
             * Get the Target camera's configuration JSONElement "FOV" if it exists, then
             * render it as a long. If the element isn't in the JSON /boot/frc.json file,
             * the get() will return a null, which will cause the getAsLong() to throw an
             * exception. Just use the default, initially set, value instead of what's in the
             * file.
             */

            fieldOfView = cameraConfigs.get(cameraConfigsIndex).config.get("FOV").getAsLong();
            System.out.println(String.format("Set FOV to %d", fieldOfView));
          } catch (Exception e) {
            System.out.println(
                String.format("Couldn't understand camera's FOV configuration value (ex: FOV: 150 ). Using %d instead.",
                    fieldOfView));
          }

          VisionThread visionThread = new VisionThread(cameras.get(cameraConfigsIndex), new PowerPortAnnotator(),
              pipeline -> {

                double relativeHeading = pipeline.getNormalizedCenter() * (double) fieldOfView / 2.0f;

                double targetWidth = 39.26; /* Width of a real and ideal target in inches */
                double targetDistance = pipeline.getNormalizedTargetDistance(fieldOfView) * targetWidth;

                /*
                 * Send the information packet to the Roborio that contains an array of doubles.
                 * The doubles contain the values:
                 * 
                 * [0] the offset angle of the target in degrees relative to the current
                 * heading.
                 * 
                 * [1] the age of this target information in milliseconds.
                 * 
                 * [2] the computed distance to the target based on the known relative size of
                 * the target
                 * 
                 */
                targetInformation
                    .setDoubleArray(new double[] { relativeHeading, pipeline.getProcessingTime(), targetDistance });

                /*
                 * Flush the network table queue to quickly send this network table field to the
                 * roborio. This reduces the network latency of this information to almost
                 * nothing.
                 */
                targetInformation.getInstance().flush();

                /*
                 * Generate an annotated image stream with the annotateImage() function, which
                 * paints contours and text of all the targets that were found. When nobody is
                 * connected to the diagnostic, annotated outputStream, don't bother updating
                 * it, to reduce computer throughput.
                 */
                if (outputStream.isEnabled()) {
                  Mat matCamera = pipeline.getLastImage();

                  matCamera = pipeline.annotateImage(matCamera, pipeline.getPowerPorts());

                  outputStream.putFrame(matCamera);
                }

              });

          visionThread.start();
        }
      }
    }

    // loop forever
    for (;;) {
      try {
        Thread.sleep(1000);

        /*
         * Determine how long it's been since we last heard from the roborio. If it's
         * been too long, assume that something's gone amiss with the NetworkTables
         * connection to the roborio and do something about it.
         */
        long timeSinceLastRoborioEcho = System.currentTimeMillis() - autoAssistConnectionTestLastReceivedTimeStamp;

        try {
          if (timeSinceLastRoborioEcho > 1000) {
            /*
             * It's been too long since we last heard from the roborio. Assume that
             * something has gone wrong with the NetworkTables communication. Stop it and
             * restart it.
             * 
             * Don't do this too often as it'll increase the latency of the things we send
             * over network tables, but don't wait too long to try to fix communications
             * between the vision processor and the roborio if it's gone amiss. 1 second is
             * plenty long enough to wait for the roborio. Do something if it's been longer
             * than that since we've heard from the roborio.
             */
            System.out.println(
                String.format("Restarting networktables client because I haven't heard from the roborio for %d ms",
                    timeSinceLastRoborioEcho));
            ntinst.stopClient();
            ntinst.startClientTeam(team);
          }
        } catch (Exception ex) {
          System.out.println(String.format("Exception caught while testing roborio echo delay:%s", ex.toString()));
        }

      } catch (InterruptedException ex) {
        return;
      }
    }
  }
}
