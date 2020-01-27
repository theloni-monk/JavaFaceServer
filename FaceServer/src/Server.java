import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;



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
    private CascadeClassifier faceCascade;
    private int absoluteFaceSize;

    ServerThread(Socket s, int[] imgDim){
        this.sock = s;
        this.imgDim = imgDim;
        this.frame = new Mat(imgDim[0],imgDim[1], CvType.CV_8UC3);
        this.absoluteFaceSize = 0;

        //load face detector
        //TODO: fix file path
        String haar = "C:\\Users\\TheoA\\Documents\\Github\\JavaFaceServer\\FaceServer\\src\\haarcascade.xml";
        String lbp =  "C:\\Users\\TheoA\\Documents\\Github\\JavaFaceServer\\FaceServer\\src\\lbp.xml";

        this.faceCascade = new CascadeClassifier(lbp);

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

                    //hack gets around native array access issues
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
                    //FIXME: hangs here
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
        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();

        // convert the frame in gray scale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        // equalize the frame histogram to improve the result
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // compute minimum face size (10% of the frame height, in our case)
        if (this.absoluteFaceSize == 0)
        {
            int height = grayFrame.rows();
            if (Math.round(height * 0.1f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }

        // detect faces
        //FIXME: fuck this shit
        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 1, Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

        //return the list of faces in Rect[] form
        Rect[] rects = faces.toArray();
        return rects;
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
