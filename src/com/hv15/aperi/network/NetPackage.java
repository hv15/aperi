package com.hv15.aperi.network;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Pseudo-network-packet that contains some information from a
 * {@link WifiP2pDevice} object, such as its name, MAC address, and the packet
 * type.
 * 
 * @author Hans-Nikolai Viessmann
 * @version 1.0.3
 * 
 */
public class NetPackage
{
    /**
     * Indicates that the sender has just connected to the Wi-Fi Direct Group
     * and would like it's presence to be broadcasted to all the other members
     * of the group.
     */
    public final static int CONNECT = 0;
    /**
     * Indicates that the receiver should send a {@linkplain NetPackage#HELLO}
     * packet to the device indicated in the packet, thus providing its own
     * information to the recipient.
     */
    public final static int RESPOND = 1;
    /**
     * Indicates that the receiver should not reply, instead it should store the
     * contained information in its database
     */
    public final static int HELLO = 2;
    /**
     * Indicates that the sending device is about to disconnect from the group;
     * causes the group owner to send this packet further to all other group
     * members.
     */
    public final static int DISCONNECT = 3;
    /**
     * Array of strings containing the human-readable packet types.
     */
    public final static String[] type = {
            "CONNECT", "RESPOND", "HELLO", "DISCONNECT"
    };

    /**
     * The name of the sender
     */
    public String deviceName;
    /**
     * The MAC address of the sender
     */
    public String deviceAddress;
    /**
     * The packet type
     */
    public int packetType;
    /**
     * The IP address of the sender
     */
    public String deviceIP;
    
    private final int[] types = {
            CONNECT, RESPOND, HELLO, DISCONNECT
    };

    /**
     * Main constructor to create a <i>generic</i> pseudo-network-packet.
     * 
     * @param name
     *            If known, the name of the sender <i>(may be null)</i>
     * @param mac
     *            The MAC address of the sender
     * @param ip
     *            The IP address of the device <i>(may be null)</i>
     * @param type
     *            The packet type (e.g. {@link NetPackage#CONNECT},
     *            {@link NetPackage#RESPOND}, {@link NetPackage#HELLO}, or
     *            {@link NetPackage#DISCONNECT})
     */
    public NetPackage(String name, String mac, String ip, int type)
    {
        deviceName = name;
        deviceAddress = mac;
        deviceIP = ip;
        packetType = types[type];
    }

    @Override
    public String toString()
    {
        return deviceName + " [" + deviceAddress + "]: " + deviceIP
                + ", type: " + packetType;
    }
}