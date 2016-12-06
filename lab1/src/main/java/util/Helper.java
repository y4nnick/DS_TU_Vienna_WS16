package util;

import java.net.DatagramPacket;

public class Helper {

    /**
     * Reads out the String response from a DatagramPacket
     * @param packet the packet
     * @return the response string from the packet
     */
    public static String getResponseFromPacket(DatagramPacket packet){

        // Load real response (see: https://stackoverflow.com/questions/8229064/how-to-get-rid-of-the-empty-remaining-of-the-buffer)
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return new String(data);

    }

}
