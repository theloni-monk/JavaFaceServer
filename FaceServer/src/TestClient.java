import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.Socket;
import java.util.Vector;


//entry point
public class TestClient{
    public static void main(String[] args) {
        new ClientFrame();
    }
}

//JFrame wrapper
class ClientFrame extends JFrame{
    public ClientFrame()
    {
        Frame f= new Frame("Canvas Example");
        f.add(new Client());
        f.setLayout(null);
        f.setSize(400, 400);
        f.setVisible(true);
    }
    /*
    public static void main(String args[])
    {
        new ClientFrame();
    }
    */
}


class Client extends JPanel {
    static VideoCapture capture;
    static Socket socket;
    static BufferedOutputStream outputStream;
    static DataInputStream inputStream;
    static BufferedImage currFrame;
    static Vector<FaceRect> faces;

    // Load OpenCV native library
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public Client(){
        setSize(480, 640);
        capture = new VideoCapture();
        capture.open(0);
        try {
            socket = new Socket("localhost", 5050);
            socket.setSendBufferSize(921601);
            outputStream = new BufferedOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());
        }
        catch (Exception e) { e.printStackTrace(); }
        faces = new Vector<FaceRect>();
        // grab a frame every 33 ms (30 frames/sec)
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        //System.out.println("Paint called");
        try {
            pLoop(g);
        } catch (Exception e) {
            e.printStackTrace();
            try{socket.close();}catch(Exception e1){}
            System.exit(0);
        }
    }

    private void pLoop(Graphics g) throws Exception{
        Mat image = grabFrame();

        sendFrame(image);
        //System.out.println("sent frame");

        currFrame = toBuffImg(image);

        g.drawImage(currFrame, 0,0,this);

        //ingests input stream to read if there are faces
        getFaces();

        // draw faces as rectangles

        for(FaceRect face: faces){
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(10));
            g2.setColor(Color.yellow);

            g2.drawRect(face.x,face.y,face.width,face.height);
        }

        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    //helpers
    private static Mat grabFrame() {
        Mat frame = new Mat();
        if (capture.isOpened()) {
            try {
                capture.read(frame);
            }
            catch (Exception e) {
                // log the (full) error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }
        //System.out.println("frame size: "+frame.size());
        return frame;
    }

    private static void sendFrame(Mat image) throws IOException{
        outputStream.write(0x01); //sends ready byte

        //byte[] size = ByteBuffer.allocate(4).putInt((int) (image.total() * image.elemSize())).array();
        //outputStream.write(size);

        byte[] imgDat = new byte[921600];//480p //(int) (image.total() * image.elemSize())];
        image.get(0,0, imgDat);
        outputStream.write(imgDat);
        outputStream.flush();
    }

    private static void getFaces(){
        //faces.clear(); //clear old face data
        try {
            byte[] ready = new byte[1];
            inputStream.read(ready); //checks for ready byte in stream to ingest the rest of the frame
            //socket streams don't operate on a message system, so ready byte is necessary to signal message
            if ((ready[0] & 0x01) != 0) { // check last bit
                faces.clear(); //clear old face data
                System.out.println("recieved face(s)");
                int numFaces = inputStream.readInt(); //read number of faces to read for
                System.out.println(numFaces + " face(s)");
                for(int i=0; i<numFaces; i++){
                    FaceRect f = new FaceRect();
                    f.x = inputStream.readInt();
                    f.y = inputStream.readInt();
                    f.width = inputStream.readInt();
                    f.height = inputStream.readInt();
                    faces.add(f); //add face to static collection
                }

            }

        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private static BufferedImage toBuffImg(Mat m) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if ( m.channels() > 1 ) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte [] b = new byte[bufferSize];
        m.get(0,0,b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }
}




