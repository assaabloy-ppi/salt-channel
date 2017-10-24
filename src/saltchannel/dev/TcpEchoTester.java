package saltchannel.dev;

import saltchannel.ByteChannel;
import saltchannel.SocketChannel;
import saltchannel.a1a2.A1Client;
import saltchannel.a1a2.A1Packet;
import saltchannel.a1a2.A2Packet;
import saltchannel.util.CryptoTestData;
import saltchannel.util.KeyPair;
import saltchannel.v2.NoSuchServer;
import saltchannel.v2.SaltClientSession;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
/**
 * Tests a salt-channel v2 host that runs the simplest case of the echo protocol.
 *
 * The Echo protocol is described as:
 * request to host      = { 0x01 , payload[n] }
 * response to client   = { 0x01 , payload[n] }
 *
 * The echo session is closed by sending a close request:
 * request to host      = { 0x03 }
 * response to client   = { 0x03 }
 *
 * Test assumptions:
 *
 * A server is running salt-channel v2 and running the echo protocol on top of that.
 * The server is running a TCP server on an IP address. We don't know anything about
 * the server except that it runs the echo server. Hence, if we do an A1 request the
 * server should response with A2 = "SC2-------","ECHO------".
 *
 * Test setup:
 *  1. Connect to TCP server
 *
 * Test teardown:
 *  1. Send close command.
 *      If response differ from request then fail.
 *      If response does not include LAST_FLAG then fail.
 *
 * Test Cases:
 *
 *  1. Send an A1 request:
 *
 *      If response does not include "SC2-------","ECHO------" then fail.
 *      If response does not include the LAST_FLAG then fail.
 *
 *  2. Do a normal handshake.
 *
 *      If response differs from request then fail.
 *      Save the public key of the server.
 *      Save a copy public key of the server that differs from the original.
 *
 *  3. Send an A1 request and include the public key received from (2).
 *
 *      If the response differ from the response in (1), then fail.
 *      If response does not include the LAST_FLAG then fail.
 *
 *  4. Send an A1 request and include the modified public key from (2).
 *
 *      If response does not include the NO_SUCH_SERVER and LAST_FLAG then fail.
 *
 *  5. Do a normal handshake and include the public key received from (3).
 *
 *      If handshake fails then fail.
 *
 *  6. Modify the public key received from (3). Do a normal handshake including the modified
 *     public key.
 *
 *      If response does not include the NO_SUCH_SERVER and LAST_FLAG then fail.
 *
 *  7. Do a normal handshake and send a single ECHO request in a single encrypted message package.
 *
 *      If response differs from request hen fail.
 *
 *  8. Do a normal handshake and send two ECHO requests in a multi message encrypted package.
 *
 *     If responses differ from requests then fail.
 *
 * @author Simon Johansson
 */
public class TcpEchoTester {

    static final String ECHO_PROT = "ECHO------";
    private String hostAddr = "localhost";
    private int port = 2033;

    private class TestEnvironment {
        Socket socket;
        ByteChannel clearChannel;
        ByteChannel saltChannel;
        SaltClientSession session;
        public void handshake(boolean bufferm4, byte[] expectedPub) {
            session.setEncKeyPair(CryptoTestData.bEnc);
            session.setBufferM4(bufferm4);
            if (expectedPub != null) {
                session.setWantedServer(expectedPub);
            }
            session.handshake();
            saltChannel = new LoggingByteChannel(session.getChannel());
        }
    }

    private void go(String[] args) throws Throwable {

        if (args.length > 0) {
            hostAddr = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        /* Step 1 */
        a1RequestTest("A2 Server should response \"SCv2------\",\"ECHO------\" without public key included in A1", null, false);

        /* Step 2 */
        byte[] hostPub = handshakeTest("Normal handshake without public key included should work", null, null);

        byte[] dummyPub = Arrays.copyOf(hostPub, hostPub.length);
        dummyPub[4] += 1;

        /* Step 3 */
        a1RequestTest("A2 Server should response \"SCv2------\",\"ECHO------\" when public key is included in A1", hostPub, false);

        /* Step 4 */
        a1RequestTest("A2 Server should response NO_SUCH_SERVER when an invalid public key is included in A1", dummyPub, true);

        /* Step 5 */
        handshakeTest("Handshake should work when public key is included in M1", hostPub, null);

        /* Step 6 */
        handshakeTest("Handshake should should response NO_SUCH_SERVER when unexpected public key is included in M1", dummyPub, new NoSuchServer());

        /* Step 7 */
        singlePackageEchoTest();
        singlePackageEchoTest(hostPub);
        singlePackageEchoTest(hostPub, true);

        /* Step 8 */
        multiPackageEchoTest();
        multiPackageEchoTest(hostPub);
        multiPackageEchoTest(hostPub, true);

        /* Step 9 */
        versionTest();

        /* Step 10 */
        sendDataTest();

        /* Step 11 */
        closeTest();

    }

    public static void main(final String[] args) throws Throwable {

        int n = 1;
        if (args.length > 2) {
            n = Integer.parseInt(args[2]);
        }

        for (int i = 0; i < n; i++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        new TcpEchoTester().go(args);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }).start();
        }


    }

    private abstract class Test {

        TestEnvironment environment;
        String doc;
        Exception expectException = null;

        public Test(String doc) {
            this.doc = doc;
        }

        public void expectException(Exception e) {
            expectException = e;
        }

        abstract public void run() throws IOException;

        private TestEnvironment setup() throws IOException {
            KeyPair sigKeyPair = CryptoTestData.aSig; /* Client is Alice */
            Socket socket = new Socket(hostAddr, port);
            ByteChannel clear = new LoggingByteChannel(new SocketChannel(socket));
            SaltClientSession session = new SaltClientSession(sigKeyPair, clear);
            TestEnvironment environment = new TestEnvironment();
            environment.clearChannel = clear;
            environment.saltChannel = null;
            environment.session = session;
            environment.socket = socket;
            return environment;
        }

        private void teardown() throws IOException {
            environment.socket.close();
        }

        public void go() throws Throwable {
            System.out.println("Running test: " + doc);
            environment = setup();
            boolean passed = false;
            Throwable e = null;
            try {
                run();
                teardown();
                passed = true;
            } catch (Throwable ex) {
                if (expectException != null && ex.getClass().equals(expectException.getClass())) {
                    passed = true;
                }
                e = ex;
            }

            if (!passed && e != null) {
                System.out.println("Result: Failed");
                throw new Throwable(e);
            }

            System.out.println("Result: Passed");

        }
    }

    public void a1RequestTest(String doc, final byte[] hostPub, final boolean expectNoSuchServer) throws Throwable {
        new Test(doc) {
            @Override
            public void run() {
                A1Client client = new A1Client(environment.clearChannel);

                if (hostPub != null) {
                    A1Packet a1 = client.getA1();
                    a1.addressType = A1Packet.ADDRESS_TYPE_PUBKEY;
                    a1.address = hostPub;
                }

                A2Packet a2 = client.go();

                A2Packet.Prot[] prots = a2.getProts();

                boolean echoFound = false;

                for (A2Packet.Prot p : prots) {
                    if (p.p1().equals(A2Packet.SC2_PROT_STRING) && p.p2().equals(ECHO_PROT)) {
                        echoFound = true;
                        break;
                    }
                }

                if (expectNoSuchServer) {
                    if (a2.noSuchServer) {
                        return;
                    }
                    throw new AssertionError("Server did not respond with NO_SUCH_SERVER");
                }

                if (!echoFound) {
                    throw new AssertionError("Echo protocol was not found on host.");
                }

            }
        }.go();
    }

    public byte[] handshakeTest(String doc, final byte[] expectedPub, final Exception expectedException) throws Throwable {

        final byte[][] hostPub = {null};

        new Test(doc) {

            @Override
            public void run() {
                if (expectedException != null) {
                    expectException(expectedException);
                }
                environment.handshake(false, expectedPub);
                hostPub[0] = environment.session.getServerSigKey();
            }
        }.go();

        return hostPub[0];
    }

    public void singlePackageEchoTest() throws Throwable {
        singlePackageEchoTest(null, false);
    }
    public void singlePackageEchoTest(byte[] expectedPub) throws Throwable {
        singlePackageEchoTest(expectedPub, false);
    }
    public void singlePackageEchoTest(final byte[] expectedPub, final boolean bufferM4) throws Throwable {
        new Test("Echo request in single encrypted package should work") {

            @Override
            public void run() throws IOException {
                environment.handshake(bufferM4, expectedPub);

                byte[] request = new byte[]{0x01, 0x02, 0x03};

                environment.saltChannel.write(false, request);
                byte[] response = environment.saltChannel.read();

                if (!Arrays.equals(request, response)) {
                    throw new AssertionError("Response differs from request");
                }

            }
        }.go();
    }

    public void multiPackageEchoTest() throws Throwable {
        multiPackageEchoTest(null, false);
    }
    public void multiPackageEchoTest(final byte[] expectedPub) throws Throwable {
        multiPackageEchoTest(expectedPub, false);
    }
    public void multiPackageEchoTest(final byte[] expectedPub, final boolean bufferM4) throws Throwable {
        new Test("Multiple Echo requests in multi encrypted package should work") {

            @Override
            public void run() throws IOException {
                environment.handshake(bufferM4, expectedPub);

                byte[] request1 = new byte[]{0x01, 0x02, 0x03};
                byte[] request2 = new byte[]{0x01, 0x02, 0x03};

                environment.saltChannel.write(false, request1, request2);
                byte[] response1 = environment.saltChannel.read();
                byte[] response2 = environment.saltChannel.read();

                if (!Arrays.equals(request1, response1)) {
                    throw new AssertionError("Response 1 differs from request");
                }

                if (!Arrays.equals(request2, response2)) {
                    throw new AssertionError("Response 2 differs from request");
                }

            }
        }.go();
    }

    public void versionTest() throws Throwable {
        new Test("ECHO protocol version should work") {

            @Override
            public void run() throws IOException {
                environment.handshake(false, null);

                byte[] request = new byte[]{0x00};

                environment.saltChannel.write(false, request);
                byte[] response = environment.saltChannel.read();

                if (!Arrays.equals(response, new byte[]{0x00, 0x01})) {
                    throw new AssertionError("Response differs from request");
                }


            }
        }.go();
    }

    public void sendDataTest() throws Throwable {
        new Test("ECHO send data request should work") {

            @Override
            public void run() throws IOException {
                environment.handshake(false, null);

                byte[] request = new byte[]{0x02, 0x05, 0x00, 0x00, 0x00, 0x7f};

                environment.saltChannel.write(false, request);
                byte[] response = environment.saltChannel.read();

                byte[] expectedResponse = new byte[]{0x02, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f};

                if (!Arrays.equals(response, expectedResponse)) {
                    throw new AssertionError("Response 1 differs from request");
                }


            }
        }.go();
    }

    public void closeTest() throws Throwable {
        new Test("ECHO close should work") {

            @Override
            public void run() throws IOException {
                environment.handshake(false, null);

                byte[] request = new byte[]{0x03};

                environment.saltChannel.write(false, request);
                byte[] response = environment.saltChannel.read();

                if (!Arrays.equals(response, request)) {
                    throw new AssertionError("Response 1 differs from request");
                }

            }
        }.go();
    }

}
