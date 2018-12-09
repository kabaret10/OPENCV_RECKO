/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facedetect;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;

/**
 *
 * @author rfrysiak
 */
public class FXMLDocumentController implements Initializable {
    
    @FXML
    private Button button;
    @FXML
    private ImageView currentFrame;
    
    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that realizes the video capture
    private VideoCapture capture = new VideoCapture();
    // a flag to change the button behavior
    private boolean cameraActive = false;
    // the id of the camera to be used
    private static int cameraId = 0;

   
    private Object classifier;

    @FXML
    private void handleButtonAction(ActionEvent event) {
   
        if (!this.cameraActive)
        {
                // start the video capture
                this.capture.open(cameraId);

                // is the video stream available?
                if (this.capture.isOpened())
                {
                        this.cameraActive = true;

                        // grab a frame every 33 ms (30 frames/sec)
                        Runnable frameGrabber = new Runnable() {

                                @Override
                                public void run()
                                {
                                        // effectively grab and process a single frame
                                        Mat frame = grabFrame();
                                        // convert and show the frame
                                        Image imageToShow = Utils.mat2Image(frame);
                                        updateImageView(currentFrame, imageToShow);
                                }
                        };

                        this.timer = Executors.newSingleThreadScheduledExecutor();
                        this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                        // update the button content
                        this.button.setText("Stop Camera");
                }
                else
                {
                        // log the error
                        System.err.println("Impossible to open the camera connection...");
                }
        }
        else
        {
                // the camera is not active at this point
                this.cameraActive = false;
                // update again the button content
                this.button.setText("Start Camera");

                // stop the timer
                this.stopAcquisition();
        }
    }

    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Mat} to show
     */
    private Mat grabFrame()
    {
            // init everything
            Mat frame = new Mat();

            // check if the capture is open
            if (this.capture.isOpened())
            {
                    try
                    {
                            // read the current frame
                            this.capture.read(frame);
                             String file = "C:\\opencv\\build\\etc\\haarcascades\\haarcascade_frontalface_alt.xml";
                                CascadeClassifier classifier = new CascadeClassifier(file);
                            // if the frame is not empty, process it
                               MatOfRect faceDetections = new MatOfRect();
                                classifier.detectMultiScale(frame, faceDetections);
                                System.out.println(String.format("Detected %s faces",
                                faceDetections.toArray().length));
                                
                                // Drawing boxes
         for (Rect rect : faceDetections.toArray()) {
            Imgproc.rectangle(
               frame,                                   //where to draw the box
               new Point(rect.x, rect.y),                            //bottom left
               new Point(rect.x + rect.width, rect.y + rect.height), //top right
               new Scalar(0, 0, 255)                                 //RGB colour
            );
         }
                                
                            if (!frame.empty())
                            {
                                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                                    
                            }
                            

                    }
                    catch (Exception e)
                    {
                            // log the error
                            System.err.println("Exception during the image elaboration: " + e);
                    }
            }

            return frame;
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition()
    {
            if (this.timer!=null && !this.timer.isShutdown())
            {
                    try
                    {
                            // stop the timer
                            this.timer.shutdown();
                            this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
                    }
                    catch (InterruptedException e)
                    {
                            // log any exception
                            System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
                    }
            }

            if (this.capture.isOpened())
            {
                    // release the camera
                    this.capture.release();
            }
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     * 
     * @param view
     *            the {@link ImageView} to update
     * @param image
     *            the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image)
    {
            Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    protected void setClosed()
    {
            this.stopAcquisition();
    }

    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
}
