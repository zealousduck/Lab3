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
        // Comand line input: Client ServerName ServerPort MyPort
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
        }
        
        Requestor requestor = new Requestor();
        UDPPacket packet = null;
        try {
            packet = requestor.request(serverName, serverPort, myPort);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
        if (packet == null) {
            System.err.println("Something went wrong with packet...");
            return;
        }
        Printer.parsePacketHex(packet.getRawPacket());
        Chatter chatter = null; 
        try {
        switch (packet.getRequestType()) {
            case UDPPacket.ERROR_CODE:
                System.out.println("Received an Error Packet:");
                Printer.parseErrorValue(packet.getErrorValue());
                break;
            case UDPPacket.WAIT_CODE:
                chatter = 
                    new ChatServer(packet.getIPAddress(), packet.getPortNumber());
                break;
            case UDPPacket.CONNECT_CODE:
                chatter = 
                    new ChatClient(packet.getIPAddress(), packet.getPortNumber());
                break;
        }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
        if (chatter != null) {
            chatter.run();
        }
    }

} // End Client

/******************* HELPER CLASSES BELOW THIS LINE ******************************/

/******************* UDP CLIENT **************************************************/
class Requestor {
    public static final byte OUR_GID = 1;
    public static final int MAX_BUFFER_SIZE = 32; // can't see why we'd need more

    UDPPacket request(String serverName, int serverPort, int myPort) throws Exception{
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
    
    byte[] buildRequest(byte GID, int port) {
        byte[] request = new byte[UDPPacket.WAIT_LENGTH];
        request[UDPPacket.MAGIC1_LOCATION] = (byte)(UDPPacket.MAGIC_NUM >> 8);
        request[UDPPacket.MAGIC2_LOCATION] = (byte)(UDPPacket.MAGIC_NUM);
        request[WaitPacket.GID_LOCATION] = GID;
        request[WaitPacket.PORT1] = (byte)(port >> 8);
        request[WaitPacket.PORT2] = (byte)port;
        return request;
    }
}


/******************* UDP PACKETS *************************************************/
class UDPPacketFactory {
    static UDPPacket getUDPPacket(byte[] packetIn, int length) throws Exception {
        int requestIn; // Analyze type of packet by contents!
        if (length == UDPPacket.CONNECT_LENGTH)
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
        return ((byte)packet[IP1] + "." +
                (byte)packet[IP2] + "." +
                (byte)packet[IP3] + "." +
                (byte)packet[IP4]);
    }

    int getPortNumber() {
        return (packet[PORT1] << 8) + (packet[PORT2]);
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
        return (packet[PORT1] << 8) + (packet[PORT2]);
    }

    byte getErrorValue() throws Exception {
        throw new Exception("WaitPacket has no Error value field!");
    }
}


/******************* TCP CHATTER *************************************************/
abstract class Chatter { abstract void run(); }

class ChatServer extends Chatter {
    // Insert socket here

    ChatServer(String ip, int port) {
        // Stub
    }

    void run() {
        // Stub
        // Wait for connections
        // prompt client
        // wait for message
        // print message
        // terminate?
        // loop
    }
}

class ChatClient extends Chatter {
    // Insert socket here

    ChatClient(String ip, int port) {
        // Stub
    }
    
    void run() {
        // Stub
        // connect
        // receive prompt
        // get user input
        // send message
        // terminate?
        // loop
    }
}

/******************* PRINT FUNCTIONS *********************************************/
class Printer {
    static void parsePacketHex(byte[] packet) {
        System.err.println("DEBUG: PARSING PACKET:");
        for (byte b : packet) {
            System.err.printf("0x%0X ", b);
        }
        System.err.println("");
    }

    static void parseErrorValue(byte error) {
        byte VOODOO_MAGIC = 0x1;
        byte LENGTH_ERROR = 0x2;
        byte PORT_NUM_OOR = 0x4;
        // Print out an error message based on type
        System.err.println("Errors:");
        if ((error & VOODOO_MAGIC) > 0)
            System.err.println("\tIncorrect magic number");
        if ((error & LENGTH_ERROR) > 0)
            System.err.println("\tIncorrect length");
        if ((error & PORT_NUM_OOR) > 0)
            System.err.println("\tPort number out of range");
    }
}

