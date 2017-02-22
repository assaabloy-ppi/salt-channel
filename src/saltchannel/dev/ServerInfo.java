package saltchannel.dev;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerInfo {
    private ServerSocket ss;
    
    public void doIt(String[] args) throws IOException {
        try {
            reallyDoIt();
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    System.out.println("Could not close ss");
                    return;
                }
            }
        }
    }
    
    private void reallyDoIt() throws IOException {
        ss = new ServerSocket(2001);
        Socket socket = ss.accept();
        
        System.out.println("Client connected");
        
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        
        while (true) {
            int b = in.read();
            System.out.println("b: " + (int) b + ", char: " + (char) b);
            
            if (b == -1) {
                break;
            }
            
            if (b == 'p') {
                out.write('p');
                out.write(0);
                out.write(1);
                out.write(1);
                
                out.flush();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new ServerInfo().doIt(args);
    }
}
