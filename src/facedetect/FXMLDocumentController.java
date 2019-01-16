/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facedetect;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.face.BasicFaceRecognizer;
import org.opencv.face.Face;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.Facemark;
/**
 *
 * @author rfrysiak
 */
public class FXMLDocumentController implements Initializable {
       
    @FXML
    private Button button;
    @FXML
    private ImageView currentFrame;
    @FXML
    private DirectoryChooser directoryChooser;
    
    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that realizes the video capture
    private VideoCapture capture = new VideoCapture();
    // a flag to change the button behavior
    private boolean cameraActive = false;
    // the id of the camera to be used
    private static int cameraId = 0;

    private static int imgNr = 1;
    private static boolean saveImg = false;
    private static String saveDir;
   
    private CascadeClassifier faceCascade = new CascadeClassifier();

    @FXML
    private void handlebazaAction(ActionEvent event) {
   
                    loadDB();
                  
                }
    @FXML
    private void handleButtonAction(ActionEvent event) {
        if (!this.faceCascade.load("src\\facedetect\\haarcascade_frontalface_default.xml"))
            System.out.println("Nie udalo sie zaladowac klasyfikatora");
   
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

    @FXML
    private void handleSaveAction(ActionEvent event) {
        saveImg = true;
    }

    @FXML
    private void handleFolderAction(ActionEvent event) {
        directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Wybierz folder");
        File selectedDir = directoryChooser.showDialog(null);
        if (selectedDir != null) {
            saveDir = selectedDir.getAbsolutePath();
            imgNr = 1;
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
            Mat grayFrame = new Mat();
            MatOfRect faces = new MatOfRect();
                
                
            // check if the capture is open
            if (this.capture.isOpened())
            {
                    try
                    {
                            // read the current frame
                            this.capture.read(frame);

                            // if the frame is not empty, process it
                            if (!frame.empty())
                            {
                                    Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                                    //Imgproc.equalizeHist(grayFrame, grayFrame);
                                    // detect faces
                                    this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE, new Size(50, 50), new Size());
                                    // each rectangle in faces is a face: draw them!
                                    Rect[] facesArray = faces.toArray();
                                    for (int i = 0; i < facesArray.length; i++)
                                        Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);
                                    
                                    if ( saveImg ) {
                                        Mat face = grayFrame.submat(facesArray[0]);
                                        Imgcodecs img = new Imgcodecs();
                                        img.imwrite(saveDir+"\\"+imgNr+".pgm", face);
                                        imgNr++;
                                        saveImg = false;
                                    }
                            
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
private void loadDB () {
  
        File baza = new File("src\\\\facedetect\\\\face.csv");
       
        try{
            // inicjacja skanera
            Scanner inputStream = new Scanner(baza);
            String [] doRozdzialu= new String[100];
            ArrayList<Mat> images = new ArrayList<>();
            ArrayList<Mat> labels = new ArrayList<>();
           
            String data;
            
            // jesli nastepna linia = true
            
            while(inputStream.hasNext()){
                //czytaj linie
                
                 data= inputStream.nextLine();
               // System.out.println("#### czytam linie" + data);
                ArrayList aList= new ArrayList(Arrays.asList(data.split(" ; ")));
             
                
                for(int j=0;j<aList.size();j++)
                    {
                        if(j==0)
                        {
                           
                          // tutaj wkładam do obrazy
                        }
                        else
                        { 
                            
                            // tutaj wkładam do indexy
                        }
                        System.out.println("iterator "+j+" "+aList.get(j));
                    }
                
                
            }
            
               
            System.out.println("pliki dodane");
           /* 
            for(int y=0;y>=images.size();y++){
                System.out.println("index "+y+" = "+images.get(y));
            }
            */
            inputStream.close();


        }catch (FileNotFoundException e){

            System.out.println("błąd z bazą");
        }
                }
  
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
