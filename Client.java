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

public class Client {
    public static void main (String args[]) {
        // parse command line input
        
        Requestor requestor; // new Requestor, connects via UDP
        UDPPacket packet; // gotten from request()
        Chatter chatter; // new Chatter based on response from requestor, see _CODE

    }

/******************* HELPER CLASSES BELOW THIS LINE ******************************/

/******************* UDP CLIENT **************************************************/
class Requestor {
    
    Requestor() {
        // Stub
    }

    UDPPacket request() {
        // stub
        // connect to UDP server
        // make request
        // get packet back
        // use factory to generate packet
        return null; // Not a valid
    }
}


/******************* UDP PACKETS *************************************************/
class UDPPacketFactory {
    UDPPacket getUDPPacket(byte[] packetIn) throws Exception {
        int requestIn; // Analyze type of packet by contents!
        if (packetIn.length == UDPPacket.CONNECT_LENGTH)
            requestIn = UDPPacket.CONNECT_CODE;
        else if (packetIn[ErrorPacket.ZERO_LOCATION] == 0x00)
            requestIn = UDPPacket.ERROR_CODE;
        else if (packetIn[ErrorPacket.ZERO_LOCATION] != 0x00)
            requestIn = UDPPacket.WAIT_CODE;
        else
            throw new Exception("Invalid packetIn length!!");
        switch (requestIn) {
            case UDPPacket.ERROR_CODE:
                return new ErrorPacket(requestIn, packetIn);
            case UDPPacket.WAIT_CODE:
                return new WaitPacket(requestIn, packetIn);
            case UDPPacket.CONNECT_CODE:
                return new ConnectPacket(requestIn, packetIn);
            default:
                throw new Exception("Invalid request Type!");
        }
    }
}

abstract class UDPPacket { 
    static final int MAGIC_NUM = 0xA5A5;
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

    int getRequestType() {
        return requestType;
    }

    abstract byte getGID();
    abstract String getIPAddress() throws Exception; 
    abstract int getPortNumber() throws Exception;
    abstract int getErrorValue() throws Exception; 
}

class ErrorPacket extends UDPPacket {
    static final int GID_LOCATION = 2;
    static final int ZERO_LOCATION = 3;
    ErrorPacket(int requestIn, byte[] packetIn) {
        super(requestIn, packetIn);
        packetLength = UDPPacket.ERROR_LENGTH;
    }
    // insert relevant info extracts here
    byte getGID() {
        return packet[GID_LOCATION];
    }

    String getIPAddress() throws Exception {
        throw new Exception("Packet has no IP address field!");
    }

    int getPortNumber() throws Exception {
        throw new Exception("Packet has no Port number field!");
    }

    int getErrorValue() {
        return -1;
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

    int getErrorValue() throws Exception {
        throw new Exception("Packet has no Error value field!");
    }
}

class WaitPacket extends UDPPacket {
    static final int GID_LOCATION = 2;
    WaitPacket(int requestIn, byte[] packetIn) {
        super(requestIn, packetIn);
        packetLength = UDPPacket.WAIT_LENGTH; 
    }
    // insert relevant info extracts here
    byte getGID() {
        return packet[GID_LOCATION];
    }

    String getIPAddress() throws Exception {
        throw new Exception("Packet has no IP address field!");
    }

    int getPortNumber() {
        return -1;
    }

    int getErrorValue() throws Exception {
        throw new Exception("Packet has no Error value field!");
    }
}


/******************* TCP CHATTER *************************************************/
abstract class Chatter { abstract void run(); }

class ChatServer extends Chatter {
    // Insert socket here

    ChatServer(int ip, int port) {
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

    ChatClient(int ip, int port) {
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

static void parseErrorValue(int error) {
    // Print out an error message based on type
    // magic problem = 1
    // length probelm = 2
    // port out of range = 4
}

} // End Client
