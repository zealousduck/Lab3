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
        int requestIn = -1; // Analyze type of packet by contents!
        switch (requestIn) {
            case UDPPacket.ERROR_CODE:
                return new ErrorPacket(requestIn, packetIn);
            case UDPPacket.SERVER_CODE:
                return new WaitPacket(requestIn, packetIn);
            case UDPPacket.CLIENT_CODE:
                return new ConnectPacket(requestIn, packetIn);
            default:
                throw new Exception("Invalid request Type!");
        }
    }
}

abstract class UDPPacket { 
    static final int MAGIC_NUM = 0xA5A5;
    static final int ERROR_CODE = -13;
    static final int SERVER_CODE = 0;
    static final int CLIENT_CODE = 1;
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

    abstract int getGID();
    abstract String getIPAddress() throws Exception; 
    abstract int getPortNumber() throws Exception;
    abstract int getErrorValue() throws Exception; 
}

class ErrorPacket extends UDPPacket {
    ErrorPacket(int requestIn, byte[] packetIn) {
        super(requestIn, packetIn);
        packetLength = -1;
    }
    // insert relevant info extracts here
    int getGID() {
        return -1;
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
    ConnectPacket(int requestIn, byte[] packetIn) {
        super(requestIn, packetIn);
        packetLength = -1;
    }
    // insert relevant info extracts here
    int getGID() {
        return -1;
    }

    String getIPAddress() throws Exception {
        return null;
    }

    int getPortNumber() throws Exception {
        return -1;
    }

    int getErrorValue() throws Exception {
        throw new Exception("Packet has no Error value field!");
    }
}

class WaitPacket extends UDPPacket {
    WaitPacket(int requestIn, byte[] packetIn) {
        super(requestIn, packetIn);
        packetLength = -1; 
    }
    // insert relevant info extracts here
    int getGID() {
        return -1;
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
