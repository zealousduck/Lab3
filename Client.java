/* Client.java
 * COMP 4320 - LAB
 * GROUP 1:
 *      Olivia Murphy   - ONM0002
 *      Stacy Pickens   - SEP0020
 *      Patrick Stewart - PCS0012
 *
 * 11 - 16 - 15
 */

import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;

public class Client {
    public static void main (String args[]) {
        if (args.length == 1 && args[0].equals("test")) {
            TestSuite.runTests();
        }
        // Command line input: Client ServerName ServerPort MyPort
        if (args.length != 3) {
            System.out.println("Usage: Client ServerName ServerPort MyPort");
            return;
        }
        String serverName = args[0];
        int serverPort = 0, myPort = 0;
        try {
            serverPort = Integer.parseInt(args[1]);
            myPort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("Port numbers must be integer values");
            return;
        } // end parsing command line output
        
        UDPPacket packet = null;
        try {
            packet = Requestor.request(serverName, serverPort, myPort);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
        if (packet == null) {
            System.err.println("Uh. Shit. Something went wrong...");
            return;
        }
        //Printer.parsePacketHex(packet.getRawPacket());
        Chatter chatter = null; 
        try {
        switch (packet.getRequestType()) {
            case UDPPacket.ERROR_CODE:
                System.out.println("Received an Error Packet:");
                Printer.parseErrorValue(packet.getErrorValue());
                break;
            case UDPPacket.WAIT_CODE:
                chatter = new ChatServer(packet.getPortNumber());
                break;
            case UDPPacket.CONNECT_CODE:
                chatter = new ChatClient(packet.getIPAddress(), 
                                         packet.getPortNumber());
                break;
            default:
                System.out.println("Unidentified packet type");
        }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
        if (chatter != null) {
            try { chatter.chat(); }
            catch (IOException e) { System.out.println(e.getMessage()); }
        }
        System.out.println(); // Nice formatting
    }

} // End Client

/*================== HELPER CLASSES BELOW THIS LINE ==========================*/

/*================== UDP CLIENT ==============================================*/
class Requestor {
    public static final byte OUR_GID = 1;
    public static final int MAX_BUFFER_SIZE = 9; // more?
    public static final int REQUEST_MAGIC1 = 0;
    public static final int REQUEST_MAGIC2 = 1;
    public static final int REQUEST_PORT1 = 2;
    public static final int REQUEST_PORT2 = 3;
    public static final int REQUEST_GID = 4;

    static UDPPacket request(String serverName, int serverPort, int myPort) 
            throws Exception{
        byte[] data = buildRequest(OUR_GID, myPort);
        DatagramPacket packetToSend = 
            new DatagramPacket(data, data.length, 
                InetAddress.getByName(serverName), serverPort);
        DatagramSocket sock = new DatagramSocket();
        sock.send(packetToSend);
        DatagramPacket packetToReceive = 
            new DatagramPacket(new byte[MAX_BUFFER_SIZE], MAX_BUFFER_SIZE);
        sock.receive(packetToReceive);
        return UDPPacketFactory.getUDPPacket(packetToReceive.getData(), 
                                             packetToReceive.getLength());
    }
    
    static byte[] buildRequest(byte GID, int port) {
        byte[] request = new byte[UDPPacket.WAIT_LENGTH];
        request[REQUEST_MAGIC1] = (byte)(UDPPacket.MAGIC_NUM >> 8);
        request[REQUEST_MAGIC2] = (byte)(UDPPacket.MAGIC_NUM);
        request[REQUEST_GID] = GID;
        request[REQUEST_PORT1] = (byte)(port >> 8);
        request[REQUEST_PORT2] = (byte)port;
        return request;
    }
}


/*================== UDP PACKETS =============================================*/
class UDPPacketFactory {
    static UDPPacket getUDPPacket(byte[] packetIn, int length) throws Exception{
        int requestIn; // Analyze type of packet by contents!
        if (length != UDPPacket.CONNECT_LENGTH && 
                length != UDPPacket.WAIT_LENGTH &&
                length != UDPPacket.ERROR_LENGTH)
            throw new Exception("UDPPacketFactory: Invalid length:" + length);
        else if ((packetIn[UDPPacket.MAGIC1_LOCATION] & 0x0FF) != 0x0A5 &&
                (packetIn[UDPPacket.MAGIC2_LOCATION] & 0x0FF) != 0x0A5) 
            throw new Exception("UDPPacketFactory: Invalid magic number!");
        else if (length == UDPPacket.CONNECT_LENGTH)
            requestIn = UDPPacket.CONNECT_CODE;
        else if (packetIn[ErrorPacket.ZERO_LOCATION] == 0x00)
            requestIn = UDPPacket.ERROR_CODE;
        else if (packetIn[ErrorPacket.ZERO_LOCATION] != 0x00)
            requestIn = UDPPacket.WAIT_CODE;
        else
            throw new Exception("UDPPacketFactory: Invalid packetIn!");
        switch (requestIn) {
            case UDPPacket.ERROR_CODE:
                return new ErrorPacket(requestIn, packetIn);
            case UDPPacket.WAIT_CODE:
                return new WaitPacket(requestIn, packetIn);
            case UDPPacket.CONNECT_CODE:
                return new ConnectPacket(requestIn, packetIn);
            default: // Should never happen because we throw earlier?
                throw new Exception("UDPPacketFactory: Invalid request Type!");
        }
    }
}

abstract class UDPPacket { 
    static final int MAGIC_NUM = 0xA5A5;
    static final int MAGIC1_LOCATION = 0;
    static final int MAGIC2_LOCATION = 1;
    static final int ERROR_CODE = -13;
    static final int ERROR_LENGTH = 5;
    static final int WAIT_CODE = 0;
    static final int WAIT_LENGTH = 5;
    static final int CONNECT_CODE = 1;
    static final int CONNECT_LENGTH = 9;
    int requestType;
    int packetLength;
    byte[] packet;
    UDPPacket(int requestIn, byte[] packetIn) {
        requestType = requestIn;
        packet = packetIn;
    }

    int getRequestType() { return requestType; }

    byte[] getRawPacket() { return packet; }

    abstract byte getGID();
    abstract String getIPAddress() throws Exception; 
    abstract int getPortNumber() throws Exception;
    abstract byte getErrorValue() throws Exception; 
}

class ErrorPacket extends UDPPacket {
    static final int GID_LOCATION = 2;
    static final int ZERO_LOCATION = 3;
    static final int ERROR_LOCATION = 4;

    ErrorPacket(int requestIn, byte[] packetIn) {
        super(requestIn, packetIn);
        packetLength = UDPPacket.ERROR_LENGTH;
    }
    // insert relevant info extracts here
    byte getGID() {
        return packet[GID_LOCATION];
    }

    String getIPAddress() throws Exception {
        throw new Exception("ErrorPacket has no IP address field!");
    }

    int getPortNumber() throws Exception {
        throw new Exception("ErrorPacket has no Port number field!");
    }

    byte getErrorValue() {
        return packet[ERROR_LOCATION];
    }
}

class ConnectPacket extends UDPPacket {
    static final int IP1 = 2;
    static final int IP2 = 3;
    static final int IP3 = 4;
    static final int IP4 = 5;
    static final int PORT1 = 6;
    static final int PORT2 = 7;
    static final int GID_LOCATION = 8;

    ConnectPacket(int requestIn, byte[] packetIn) {
        super(requestIn, packetIn);
        packetLength = UDPPacket.CONNECT_LENGTH;
    }
    // insert relevant info extracts here
    byte getGID() {
        return packet[GID_LOCATION];
    }

    String getIPAddress() {
        return ((packet[IP1]&0x0FF) + "." +
                (packet[IP2]&0x0FF) + "." +
                (packet[IP3]&0x0FF) + "." +
                (packet[IP4]&0x0FF));
    }

    int getPortNumber() {
        int portNum = 0;
        portNum += (packet[PORT1] & 0x0FF) << 8;
        portNum += (packet[PORT2] & 0x0FF);
        portNum &= 0x0FFFF;
        return portNum;
    }

    byte getErrorValue() throws Exception {
        throw new Exception("ConnectPacket has no Error value field!");
    }
}

class WaitPacket extends UDPPacket {
    static final int GID_LOCATION = 2;
    static final int PORT1 = 3;
    static final int PORT2 = 4;

    WaitPacket(int requestIn, byte[] packetIn) {
        super(requestIn, packetIn);
        packetLength = UDPPacket.WAIT_LENGTH; 
    }
    // insert relevant info extracts here
    byte getGID() {
        return packet[GID_LOCATION];
    }

    String getIPAddress() throws Exception {
        throw new Exception("WaitPacket has no IP address field!");
    }

    int getPortNumber() {
        int portNum = 0;
        portNum += (packet[PORT1] & 0x0FF) << 8;
        portNum += (packet[PORT2] & 0x0FF);
        portNum &= 0x0FFFF;
        return portNum;
    }

    byte getErrorValue() throws Exception {
        throw new Exception("WaitPacket has no Error value field!");
    }
}


/*================== TCP CHATTERS ========================================*/
abstract class Chatter { 
    abstract void chat() throws IOException; 
}

class ChatState { boolean open; ChatState() { open = true; } }

class ChatReadThread extends Thread {  
    BufferedReader in;
    ChatState state;
    ChatReadThread(BufferedReader readerIn, ChatState s) { in = readerIn; state = s; }
    public void run() {
        String input = "";
        String terminateString = "bye bye birdie";
        while (!(input.toLowerCase().equals(terminateString))) {
            try { 
                input = in.readLine();
                if (state.open == false) return; // we're done
            } catch (IOException e) {
                break;
            }
            System.out.println(input);
        }
        state.open = false;
    }
}

class ChatWriteThread extends Thread {  
    PrintWriter out;
    ChatState state;
    BufferedReader userInput;
    ChatWriteThread(PrintWriter writerIn, BufferedReader readerIn, ChatState s) { 
        out = writerIn; userInput = readerIn; state = s; }
    public void run() {
        String message = "";
        String terminateString = "bye bye birdie";
        while (!(message.toLowerCase().equals(terminateString))) {
            try { 
                if (userInput.ready()) {
                    message = userInput.readLine(); 
                    out.println(message);
                } else try { Thread.sleep(500); } catch (InterruptedException i) {}
                if (state.open == false) return; // we're done
            } catch (IOException e) {
                break;
            }
        }
        state.open = false;
    }
}

class ChatServer extends Chatter {
    ServerSocket serverSocket;
    Socket socket;
    BufferedReader in;
    PrintWriter out;
    BufferedReader userInput;

    ChatServer(int port) throws IOException {
        System.err.println("Creating new ChatServer...");
        System.err.println("Opening port " + port);
        try { serverSocket = new ServerSocket(port); }
        catch (IOException e) { 
            System.err.println(e.getMessage());
            throw new IOException("Failed to create " + 
                this.getClass().getSimpleName() + "!");
        }
        System.err.println("new ChatServer created!");
    }

    void chat() throws IOException {
        try { socket = serverSocket.accept(); }
        catch (IOException e) { 
            System.err.println(e.getMessage());
            throw new IOException("Failed to accept connection.");
        }
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            userInput = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new IOException("Failed to get socket streams");
        }
        String connectedMsg = ("\n=== Connected to " + 
            socket.getInetAddress().getHostAddress() + 
            " ========================================\n");
        connectedMsg += "Enter a message to begin chatting!";
        System.out.println(connectedMsg);
        String prompt = 
            ("\n=== Welcome to the Group 1's Chat Room! ===========================\n");
        prompt += "Enter a message to begin chatting!";
        out.println(prompt);
        
        //runThreads(in, out);
        ChatState state = new ChatState();
        ChatWriteThread outThread = new ChatWriteThread(out, userInput, state);
        ChatReadThread inThread = new ChatReadThread(in, state);
        Thread t = new Thread(outThread);
        t.start();
        inThread.run();

        System.out.println("Closing " + this.getClass().getSimpleName() + "...");
        //userInput.close();
        //in.close();
        socket.close();
    }
}

class ChatClient extends Chatter {
    Socket socket;
    BufferedReader in;
    PrintWriter out;
    BufferedReader userInput;

    ChatClient(String ip, int port) throws IOException {
        System.err.println("Creating new ChatClient...");
        System.err.print("Connecting to " + ip);
        System.err.println(" on port " + port);
        try {socket = new Socket(InetAddress.getByName(ip), port); }
        catch (IOException e) {
            System.err.println(e.getMessage());
            throw new IOException("Failed to create " +
                this.getClass().getSimpleName() + "!");
        }
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            userInput = new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new IOException("Failed to get socket streams");
        }
        System.err.println("New ChatClient created!");
    }
    
    void chat() throws IOException {
        try { System.out.println(in.readLine()); }
        catch (IOException e) {
            System.err.println(e.getMessage());
            throw new IOException("Failed to read from socket.");
        }
        
        //runThreads(in, out);
        ChatState state = new ChatState();
        ChatWriteThread outThread = new ChatWriteThread(out, userInput, state);
        ChatReadThread inThread = new ChatReadThread(in, state);
        Thread t = new Thread(outThread);
        t.start();
        inThread.run();

        System.out.println("Closing " + this.getClass().getSimpleName() + "...");
        //userInput.close();
        //in.close();
        socket.close();
    }
}

/*================== PRINT FUNCTIONS =====================================*/
class Printer {
    static void parsePacketHex(byte[] packet) {
        System.err.println("DEBUG: PARSING PACKET:");
        String temp = "";
        for (byte b : packet) {
            if (b != 0x00) {
                System.err.printf(temp + "0x%02X ", b);
                temp = "";
            } else {
                temp += "0x" + b + " "; // save bytes, only print if a non-zero appears
            }
        }
        System.err.println("");
    }

    static void parseErrorValue(byte error) {
        byte VOODOO_MAGIC = 0x1;
        byte LENGTH_ERROR = 0x2;
        byte PORT_NUM_OOR = 0x4;
        String errorStr = "Errors:";
        // Print out an error message based on type
        if ((error & VOODOO_MAGIC) > 0)
            errorStr += ("\tmagic number |");
        if ((error & LENGTH_ERROR) > 0)
            errorStr += ("| packet length |");
        if ((error & PORT_NUM_OOR) > 0)
            errorStr += ("| Port out of range");
        System.err.println(errorStr);
    }
}

/*================== TEST FUNCTIONS ======================================*/
class TestSuite {
    static void runTests() {
        System.err.println("TESTING UDPPackets:");
        Requestor r = new Requestor();
        System.err.println("Expected:\n0xA5 0xA5 0xAB 0xCD 0x0A");
        Printer.parsePacketHex(r.buildRequest((byte)0x0A, 0xABCD));
        UDPPacket pkt;
        byte[] invalid1 = {1};
        byte[] invalid2 = {0, 0, 1, 1, 1};
        byte[] invalid3 = {(byte)0xA5, (byte)0xA5, 1, 1, 1, 1};
        try {
            UDPPacketFactory.getUDPPacket(invalid1, invalid1.length);
            UDPPacketFactory.getUDPPacket(invalid2, invalid2.length);
            UDPPacketFactory.getUDPPacket(invalid3, invalid3.length);
            System.err.println("Invalid packet failed!");
        } catch (Exception e) {
            System.err.println("Invalid packet success!");
        }
        byte[] validError = {(byte)0xA5, (byte)0xA5, 10, 0, 7};
        try {
            pkt = UDPPacketFactory.getUDPPacket(validError, 
                                                validError.length);
            if (pkt.getErrorValue() != 7) 
                throw new Exception("getErrorValue()");
            if (pkt.getGID() != 10) 
                throw new Exception("getGID()");
            System.err.println("Valid error success!");
            Printer.parseErrorValue(pkt.getErrorValue());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Valid error failed!");
        }
        byte[] validWait = {(byte)0xA5, (byte)0xA5, 10, (byte)0xAB, 
            (byte)0xCD};
        try {
            pkt = UDPPacketFactory.getUDPPacket(validWait, 
                                                validWait.length);
            if (pkt.getGID() != 10) 
                throw new Exception("getGID()");
            if (pkt.getPortNumber() != 0x0ABCD) 
                throw new Exception("getPortNumber() = " + 
                    pkt.getPortNumber());
            System.err.println("Valid wait success!");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Valid wait failed!");
        }
        byte[] validConnect = {(byte)0xA5, (byte)0xA5, (byte)131, 
            (byte)204, (byte)14, (byte)199, (byte)0xAB, (byte)0xCD, 10};
        try {
            pkt = UDPPacketFactory.getUDPPacket(validConnect, 
                                                validConnect.length);
            if (pkt.getGID() != 10) throw new Exception("getGID()");
            if (!pkt.getIPAddress().equals("131.204.14.199")) 
                throw new Exception("getIPAddress" + pkt.getIPAddress());
            if (pkt.getPortNumber() != 0x0ABCD) 
                throw new Exception("getPortNumber() = " + 
                    pkt.getPortNumber());
            System.err.println("Valid connect success!");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Valid connect failed!");
        }

    }
}

