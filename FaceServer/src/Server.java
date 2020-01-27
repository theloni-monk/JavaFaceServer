import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
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
    //TODO: add classifier
    ServerThread(Socket s, int[] imgDim){
        this.sock = s;
        this.imgDim = imgDim;
        this.frame = new Mat(imgDim[0],imgDim[1], CvType.CV_8UC3);
    }
    public void run() {
        try{
            BufferedInputStream inputStream = new BufferedInputStream(sock.getInputStream());
            BufferedOutputStream outputStream = new BufferedOutputStream(sock.getOutputStream());
            while(true) {

                byte[] ready = new byte[1];
                inputStream.read(ready); //checks for ready byte in stream

                if(ready[0] == 0x01) {
                    byte[] imageAr = new byte[921600]; //TEMP 480p
                    inputStream.read(imageAr);
                    frame.put(0, 0, imageAr); //put image matrix data from stream in bytearray to local frame field
                    System.out.println("Ingested frame");
                    //TODO: detect faces in frame and send back string with location
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
}
