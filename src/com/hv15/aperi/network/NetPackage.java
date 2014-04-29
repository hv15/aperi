package com.hv15.aperi.network;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Pseudo-network-packet that contains some information from a
 * {@link WifiP2pDevice} object,
 * such as its name, MAC address, and the packet type.
 * <p>
 * The <i>types</i> are:
 * <ul>
 * <li>{@link NetPackage#REQUEST} - indicates that the packet is a request</li>
 * <li>{@link NetPackage#RESPOND} - indicates that the packet is a response</li>
 * <li>{@link NetPackage#FLOOD} - indicates that the packet containers an IP
 * address</li>
 * <li>{@link NetPackage#DISCONNECT} - indicates that a disconnection is about
 * to occur</li>
 * </ul>
 * 
 * @author Hans-Nikolai Viessmann
 * 
 */
public class NetPackage
{
    /**
     * Used to indicate that the packet is a request, meaning that the sender
     * wishes for the receiver to respond
     */
    public final static int REQUEST = 0;
    /**
     * Indicates that the packet is a response to a request.
     */
    public final static int RESPOND = 1;
    /**
     * The packet contains an IP address, and that this IP address should be
     * used for the response packet
     */
    public final static int FLOOD = -1;
    /**
     * Indicates that the sending device is about to disconnect from the group
     */
    public final static int DISCONNECT = 2;
    
    private String mName;
    private String mMac;
    private int mType;
    private String mIP = null;

    /**
     * Create a packet, having a {@link WifiP2pDevice} object parsed for
     * information
     * 
     * @param device
     *            {@link WifiP2pDevice} object
     * @param type
     *            The packet type
     * 
     * @see NetPackage#REQUEST
     * @see NetPackage#RESPOND
     */
    public NetPackage(WifiP2pDevice device, int type)
    {
        mName = device.deviceName;
        mMac = device.deviceAddress;
        mType = type;
    }

    /**
     * Create a packet
     * 
     * @param name
     *            name of a device
     * @param mac
     *            the devices MAC address
     * @param type
     *            The packet type
     * 
     * @see NetPackage#REQUEST
     * @see NetPackage#RESPOND
     */
    public NetPackage(String name, String mac, int type)
    {
        mName = name;
        mMac = mac;
        mType = type;
    }
    
    /**
     * This is the constructor to create a <strong>flood</strong> packet,
     * meaning that it contains meta data about another device such as its MAC
     * and IP address.
     * 
     * @param mac
     *            A device's MAC address
     * @param ip
     *            A device's IP address
     * 
     * @see NetPackage#FLOOD
     */
    public NetPackage(String mac, String ip)
    {
        mName = null;
        mMac = mac;
        mIP = ip;
        mType = FLOOD;
    }

    /**
     * This is the constructor to create a <strong>delete</strong> request
     * packet.
     * 
     * @param mac
     *            A device's MAC address
     * @see NetPackage#DISCONNECT
     */
    public NetPackage(String mac)
    {
        mName = null;
        mMac = mac;
        mIP = null;
        mType = DISCONNECT;
    }

    /**
     * Get the name of the device
     * 
     * @return device name
     */
    public String getName()
    {
        return mName;
    }

    /**
     * Get the MAC address of the device
     * 
     * @return device MAC address
     */
    public String getMac()
    {
        return mMac;
    }
    
    /**
     * Get the IP address.
     * <p>
     * <strong>Returns <code>null</code> if the packet is <i>not</i> a flood
     * packet</strong>
     * 
     * @return The IP address of the device
     * 
     * @see NetPackage#FLOOD
     */
    public String getIP()
    {
        return mIP;
    }

    /**
     * Get the packet <i>type</i>
     * 
     * @return packet type
     * 
     * @see NetPackage#REQUEST
     * @see NetPackage#RESPOND
     * @see NetPackage#FLOOD
     * @see NetPackage#DISCONNECT
     */
    public int getType()
    {
        return mType;
    }
}