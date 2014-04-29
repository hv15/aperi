/**
 * 
 */

package com.hv15.aperi.interfaces;

import android.net.wifi.p2p.WifiP2pConfig;

/**
 * @author Hans-Nikolai Viessmann
 * 
 */
public interface DeviceActionListener
{
    void cancelDisconnect();

    void connect(WifiP2pConfig config);

    void disconnect();
}
