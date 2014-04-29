package com.hv15.aperi.interfaces;

import java.util.ArrayList;

import com.hv15.aperi.services.SocketService;

/**
 * Interface for the SocketServer to interact with the SQLite Database
 * 
 * @author Hans-Nikolai Viessmann <hv15@hw.ac.uk>
 * @see SocketService
 */
public interface DatabaseListener
{
    /**
     * Add a new client to the database, or update an existing entry
     * 
     * @param mac
     *            The device's MAC address
     * @param ip
     *            The device's IP address
     */
    public void addClient(String mac, String ip);

    /**
     * Delete a client from the database
     * 
     * @param mac
     *            MAC address of device to be removed
     */
    public void delClient(String mac);

    /**
     * Drop the table in the database
     * <p>
     * <strong>This should only be done when the session is over</strong>
     */
    public void clearClients();

    /**
     * Get a list of clients
     * 
     * @return {@link ArrayList} of String arrays, format <code>{mac, ip}</code>
     */
    public ArrayList<String[]> getClients();
}
