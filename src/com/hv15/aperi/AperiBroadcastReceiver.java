package com.hv15.aperi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

import com.hv15.aperi.adaptors.TabsPagerAdaptor;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class AperiBroadcastReceiver extends BroadcastReceiver
{
    private WifiP2pManager mManager;
    private Channel mChannel;
    private AperiMainActivity mActivity;

    /**
     * @param manager
     *            WifiP2pManager system service
     * @param channel
     *            WifiP2pChannel being listened on
     * @param activity
     *            Activity associated with the receiver
     */
    public AperiBroadcastReceiver(WifiP2pManager manager, Channel channel,
            AperiMainActivity activity)
    {
        super();
        if (manager == null) {
            throw new IllegalArgumentException("manager is null");
        } else if (channel == null) {
            throw new IllegalArgumentException("channel is null");
        } else if (activity == null) {
            throw new IllegalArgumentException("activity is null");
        } else {
            mManager = manager;
            mChannel = channel;
            mActivity = activity;
        }
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action))
        {
            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                mActivity.setIsWifiP2pEnabled(true);
            } else {
                mActivity.setIsWifiP2pEnabled(false);
                mActivity.clearPeers();
            }
            Log.d(AperiMainActivity.TAG, "P2P state changed - " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p mManager. This is an
            // asynchronous call and the calling mActivity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            mManager.requestPeers(mChannel, (PeerListListener) mActivity
                    .getFragment(TabsPagerAdaptor.LIST_FRAG));
            Log.d(AperiMainActivity.TAG, "P2P peers changed");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
                .equals(action)) {
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            DeviceListFragment list = (DeviceListFragment) mActivity
                    .getFragment(TabsPagerAdaptor.LIST_FRAG);
            SelfFragment self = (SelfFragment) mActivity
                    .getFragment(TabsPagerAdaptor.SELF_FRAG);
            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection
                // info to find group owner IP

                mManager.requestPeers(mChannel, list);
                mManager.requestConnectionInfo(mChannel, self);
                mManager.requestGroupInfo(mChannel, self);
                Log.d(AperiMainActivity.TAG, "Retireved connection info");
            } else {
                // It's a disconnect
                mManager.requestPeers(mChannel, list);
                self.clearDetails();
                Log.d(AperiMainActivity.TAG, "Disconnected!");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
                .equals(action)) {
            SelfFragment fragment = (SelfFragment) mActivity
                    .getFragment(TabsPagerAdaptor.SELF_FRAG);
            // This device
            WifiP2pDevice self = (WifiP2pDevice) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            fragment.updateDeviceView(self);
            Log.d(AperiMainActivity.TAG, "This device was updated...");
        }
    }
}
