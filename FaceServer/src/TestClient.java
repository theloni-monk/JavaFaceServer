package test;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.Socket;

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
    static BufferedInputStream inputStream;
    static BufferedImage currFrame;

    // Load OpenCV native library
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public Client(){
        setSize(480, 640);
        capture = new VideoCapture();
        capture.open(0);
        try {
            Socket socket = new Socket("localhost", 5050);
            socket.setSendBufferSize(921601);
            outputStream = new BufferedOutputStream(socket.getOutputStream());
            inputStream = new BufferedInputStream(socket.getInputStream());
        }
        catch (Exception e) { e.printStackTrace(); }
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
        }
    }

    private void pLoop(Graphics g) throws Exception{
        Mat image = grabFrame();

        sendFrame(image);

        currFrame = toBuffImg(image);

        g.drawImage(currFrame, 0,0,this);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    //helpers
    private static void sendFrame(Mat image) throws IOException{
        outputStream.write(0x01); //sends ready byte

        //byte[] size = ByteBuffer.allocate(4).putInt((int) (image.total() * image.elemSize())).array();
        //outputStream.write(size);

        byte[] imgDat = new byte[921600];//480p //(int) (image.total() * image.elemSize())];
        image.get(0,0, imgDat);
        outputStream.write(imgDat);
        outputStream.flush();
    }

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




