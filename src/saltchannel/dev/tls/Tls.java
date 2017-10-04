package saltchannel.dev.tls;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import saltchannel.ByteChannel;
import saltchannel.SocketChannel;
import saltchannel.util.Hex;

/**
 * Executes an example TLS session to gather data about
 * how TLS and Salt Channel compares. No real error handling is implemented.
 * 
 * @author Frans Lundberg
 */
public class Tls {
    private static int port = 2040;
    private static String host = "localhost";
    private SSLContext serverContext;
    private SSLContext clientContext;

    public static void main(String[] args) throws Exception {
        new Tls().go();
    }
    
    private void go() throws Exception {
        context();
        startServer();
        Thread.sleep(500);
        client();
    }
    
    private void context() throws Exception {
        serverContext = SSLContext.getInstance("TLSv1.2");
        serverContext.init(serverKeyManagers(), trustAll(), new SecureRandom());
        
        clientContext = SSLContext.getInstance("TLSv1.2");
        clientContext.init(clientKeyManagers(), trustAll(), new SecureRandom());
    }
    
    private void client() throws Exception {
        SSLSocketFactory factory = clientContext.getSocketFactory();        
        byte[] response;
        
        System.out.println("CLIENT, connecting to server on port " + port + ".");
        
        try (Socket socket = factory.createSocket(host, port)) {
            printClientSocketInfo((SSLSocket) socket);
            SocketChannel channel = new SocketChannel(socket);
            channel.write(new byte[]{1, 5, 5, 5, 5, 5});
            response = channel.read();

            System.out.println("CLIENT, response: " + Hex.create(response));
        }
    }
    
    private void server() throws Exception {
        SSLServerSocketFactory factory = serverContext.getServerSocketFactory();
        SSLServerSocket ss = (SSLServerSocket) factory.createServerSocket(port);
        ss.setNeedClientAuth(true);
        
        try (Socket socket = ss.accept()) {
            printServerSocketInfo((SSLSocket) socket);
            ByteChannel channel = new SocketChannel(socket);
            
            byte[] bytes = channel.read();
            channel.write(true, bytes);  // echo back        
        }
    }

    @SuppressWarnings("unused")
    private void printEnabledSuites(String[] enabledCipherSuites) {
        System.out.println("SERVER, cipher suites: ");
        for (String s : enabledCipherSuites) {
            System.out.println("  " + s);
        }
    }
    
    private void printClientSocketInfo(SSLSocket socket) throws Exception {
        SSLSession session = socket.getSession();
        System.out.println("CLIENT, protocol: " + session.getProtocol());
        System.out.println("CLIENT, cipher suite: " + session.getCipherSuite());
        System.out.println("CLIENT, peer cert count: " + session.getPeerCertificates().length);
    }
    
    private void printServerSocketInfo(SSLSocket socket) throws Exception {
        SSLSession session = socket.getSession();
        
        System.out.println("SERVER, protocol: " + session.getProtocol());
        
        try {
            System.out.println("SERVER, peer cert count: " + session.getPeerCertificates().length);
        } catch (Exception e) {
            System.out.println("SERVER, got exception when asking about peer certs: " + e.toString());
        }        
    }
    
    private void startServer() {
        Thread serverThread = new Thread(new Runnable() {
            public void run() {
                try {
                    server();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
        serverThread.start();
    }
    
    private static TrustManager[] trustAll() throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert;
        
        try (InputStream in = Tls.class.getResourceAsStream("cert-client-ec.cer")) {
            cert = (X509Certificate)cf.generateCertificate(in);
        }
        
        final X509Certificate caCert = cert;
        
        return new TrustManager[] {
            new X509TrustManager() {
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{caCert};
                }
            }
        };
    }
    
    private KeyManager[] serverKeyManagers() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream in = this.getClass().getResourceAsStream("keystore-ec");
        //Alt: InputStream in = this.getClass().getResourceAsStream("keystore-rsa");
        
        if (in == null) {
            throw new Error("cannot find keystore resource");
        }
        
        try {
            ks.load(in, "password".toCharArray());
        } finally {
            in.close();
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());
        
        return kmf.getKeyManagers();
    }
    
    private KeyManager[] clientKeyManagers() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        String name = "keystore-client-ec";
        InputStream in = this.getClass().getResourceAsStream(name);
        
        if (in == null) {
            throw new Error("cannot find resource, " + name);
        }
        
        try {
            ks.load(in, "password".toCharArray());
        } finally {
            in.close();
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());
        
        return kmf.getKeyManagers();
    }
}

/*

WIRESHARK ANALYSIS 2017-05-10

EC case
-------

Protocol: TLSv1.2
Ciphersuite: TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256
Curve: NIST P-256

Client cert used (cert-client-ec.cer from keystore-client-ec).
It is "CA cert".

Total: 2299 bytes, 7 turns.


Notes
-----

* For more info, use: java -Djavax.net.debug=all -cp out/classes saltchannel.dev.Tls.
* Why does EC case require 7 turns? 5 turns (3 round-trips should be enough).

*/