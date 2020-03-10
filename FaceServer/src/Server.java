import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.dnn.*;
import org.opencv.dnn.Dnn.*;
import org.opencv.objdetect.Objdetect;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;



public class Server {
    // Compulsory
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
    final static int[] iDim = {640,480}; //TEMP

    public static void main(String[] args) throws Exception{
        ServerSocket serverSocket = new ServerSocket(5050);
        try{ serverSocket.setReceiveBufferSize(921601); }
        catch(Exception e){ e.printStackTrace(); }
        System.out.println("Listening");
        while(true){
            Socket s = serverSocket.accept();
            new Thread(new ServerThread(s, iDim)).start();
            System.out.println("Connected");
        }
    }
}

class ServerThread implements Runnable {
    Socket sock;
    int[] imgDim;
    Mat frame;
    BufferedInputStream inputStream;
    DataOutputStream outputStream;
    Net net;
    final float confidenceThreshold = 0.7f;

    ServerThread(Socket s, int[] imgDim){
        this.sock = s;
        this.imgDim = imgDim;
        this.frame = new Mat(imgDim[0],imgDim[1], CvType.CV_8UC3);

        String configFile = "C:\\Users\\TheoA\\Documents\\Github\\JavaFaceServer\\FaceServer\\src\\models\\opencv_face_detector.pbtxt"; //new File("./models/opencv_face_detector_uint8.pb").getAbsolutePath();
        String weightFile = "C:\\Users\\TheoA\\Documents\\Github\\JavaFaceServer\\FaceServer\\src\\models\\opencv_face_detector_uint8.pb";//new File("./models/opencv_face_detector.pbtxt").getAbsolutePath();
        net = Dnn.readNetFromTensorflow(weightFile, configFile);
    }
    public void run() {
        try{
            //initialize I/O streams
            inputStream = new BufferedInputStream(sock.getInputStream());
            outputStream = new DataOutputStream(sock.getOutputStream());

            while(true) {
                byte[] ready = new byte[1];
                inputStream.read(ready); //checks for ready byte in stream to ingest the rest of the frame
                //socket streams don't operate on a message system, so ready byte is necessary to signal message
                if(ready[0] == 0x01) {
                    byte[] imageAr = new byte[921600]; //TEMP allocate 480p byte buffer to write to
                    inputStream.read(imageAr);
                    frame.put(0, 0, imageAr); //put image matrix data from stream in bytearray to local frame field

                    //System.out.println("ingested frame");

                    Rect[] faces = findFaces();

                    //HACK gets around native array access issues
                    boolean faceFound = true;
                    try{Rect f = faces[0];}
                    catch(ArrayIndexOutOfBoundsException e){ faceFound = false; }

                    if(faceFound){
                        System.out.println("detected face(s)");
                        sendFaces(faces);
                    }
                    else{
                        outputStream.write(0x00); // tell client no faces were found
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                sock.close();
                System.out.println("...Stopped");
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
    private Rect[] findFaces(){
        Mat inputBlob = Dnn.blobFromImage(this.frame, 1, new Size(this.frame.width(), this.frame.height()), new Scalar(104.0, 177.0, 123.0), true, false);


        net.setInput(inputBlob, "data");
        //FIXME: wrangle output
        Mat detectionMat = net.forward("detection_out");

        System.out.println("dims: " + detectionMat.dims() + " ; size: " + detectionMat.size() + "x" + detectionMat.size(2) + "x" + detectionMat.size(3));

        detectionMat = detectionMat.reshape(1, (int) detectionMat.total() / 7);
        System.out.println(detectionMat.get(0,0));
        //this is fucked

        int numFaces = detectionMat.rows();
        Rect[] rects = new Rect[numFaces];
        for(int i = 0; i < numFaces; i++)
        {
            double confidence = detectionMat.get(i,2)[0];
            if(confidence > confidenceThreshold)
            {
                int x1 = (int)(detectionMat.get(i,3)[0] * this.frame.width());
                int y1 = (int)(detectionMat.get(i,4)[0] * this.frame.height());
                int x2 = (int)(detectionMat.get(i,5)[0] * this.frame.width());
                int y2 = (int)(detectionMat.get(i,6)[0] * this.frame.height());

                rects[i] = new Rect(new Point(x1, y1), new Point(x2, y2));
            }
        }
        return new Rect[0];//rects;
    }

    private void sendFaces(Rect[] faces){
        try {
            outputStream.write(0x01);
            System.out.println("Faces.length: "+faces.length);
            outputStream.writeInt(faces.length);
            for (Rect face : faces) {
                outputStream.writeInt(face.x);
                outputStream.writeInt(face.y);
                outputStream.writeInt(face.width);
                outputStream.writeInt(face.height);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
