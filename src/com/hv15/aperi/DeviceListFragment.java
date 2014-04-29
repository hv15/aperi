
package com.hv15.aperi;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.hv15.aperi.interfaces.DeviceActionListener;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends ListFragment implements
        PeerListListener
{
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private ProgressDialog mProgressDialog = null;
    private View mContentView = null;
    private WifiP2pDevice mDevice;
    private int mGroupyness = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(),
                R.layout.row_devices, peers));
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        mContentView = inflater.inflate(R.layout.peer_list_layout, container,
                false);
        return mContentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.peer_list_menu, menu);
    }

    /**
     * @return this mDevice
     */
    public WifiP2pDevice getDevice()
    {
        return mDevice;
    }

    protected void setDevice(WifiP2pDevice device)
    {
        mDevice = device;
    }

    /**
     * Initiate a connection with the peer.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        ItemDetailDialogFragment frag = ItemDetailDialogFragment
                .newInstance((WifiP2pDevice) getListAdapter().getItem(position));
        frag.show(getFragmentManager(), "dialog");
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice>
    {
        private List<WifiP2pDevice> devices;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                List<WifiP2pDevice> objects)
        {
            super(context, textViewResourceId, objects);
            devices = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity()
                        .getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }

            final WifiP2pDevice device = devices.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v
                        .findViewById(R.id.device_details);
                Button connect = (Button) v
                        .findViewById(R.id.row_device_connect);
                Button disconnect = (Button) v
                        .findViewById(R.id.row_device_disconnect);
                Button send = (Button) v.findViewById(R.id.row_send_file);
                ToggleButton toggle = (ToggleButton) v
                        .findViewById(R.id.row_device_connection_type);
                top.setText(device.deviceName);
                bottom.setText(AperiMainActivity.getDeviceStatus(device.status));
                toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked)
                    {
                        if (isChecked) {
                            mGroupyness = 15;
                        } else {
                            mGroupyness = 0;
                        }

                    }
                });
                connect.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v)
                    {
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;
                        config.groupOwnerIntent = mGroupyness; // I want to be
                                                               // groupOwner
                        config.wps.setup = WpsInfo.PBC;
                        if (mProgressDialog != null
                                && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                        mProgressDialog = ProgressDialog.show(getActivity(),
                                "Press back to cancel", "Connecting to "
                                        + device.deviceName + " ("
                                        + device.deviceAddress + ")", true,
                                true
                                // new DialogInterface.OnCancelListener()
                                //
                                // @Override
                                // public void onCancel(DialogInterface dialog)
                                // {
                                // ((DeviceActionListener)
                                // getActivity()).cancelDisconnect();
                                // }
                                // }
                                );
                        ((DeviceActionListener) getActivity()).connect(config);
                        // Danger
                        mDevice = device;
                    }
                });
                disconnect.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v)
                    {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });
                send.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v)
                    {
                        // TODO Auto-generated method stub

                    }
                });
                if (device.status == WifiP2pDevice.CONNECTED) {
                    connect.setVisibility(View.GONE);
                    toggle.setVisibility(View.GONE);
                    disconnect.setVisibility(View.VISIBLE);
                    send.setVisibility(View.VISIBLE);
                } else {
                    // Little trick to ensure that the user isn't present with
                    // a strange list
                    connect.setVisibility(View.VISIBLE);
                    toggle.setVisibility(View.VISIBLE);
                    disconnect.setVisibility(View.GONE);
                    send.setVisibility(View.GONE);
                }
            }
            return v;
        }
    }

    /**
     * Clear the peer list.
     * <p>
     * This should only be called when the peer list needs to <em>only</em> be
     * cleared. Otherwise use the
     * {@link WifiP2pManager#requestPeers(android.net.wifi.p2p.WifiP2pManager.Channel, PeerListListener)}
     * method using this class ({@link DeviceListFragment}) as a callback.
     * </p>
     * 
     * @see PeerListListener
     */
    public void clearPeers()
    {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     * 
     */
    public void onInitiateDiscovery()
    {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = ProgressDialog.show(getActivity(),
                "Press back to cancel", "Looking for Peers", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog)
                    {

                    }
                });
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList)
    {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        List<WifiP2pDevice> old = peers;
        peers.clear();
        peers.addAll(peerList.getDeviceList());

        // BEGIN NASTY
        // This is a bit of a nasty hack to ensure that the database isn't
        // littered with tons of invalid entries...

        if (old.size() != peers.size()) {
            for (WifiP2pDevice device : old) {
                ((AperiMainActivity) getActivity())
                        .delClient(device.deviceAddress);
            }
        }

        for (WifiP2pDevice device : peers) {
            if (device.status != WifiP2pDevice.CONNECTED) {
                ((AperiMainActivity) getActivity())
                        .delClient(device.deviceAddress);
            }
        }

        // END NASTY

        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.isEmpty()) {
            Log.i(AperiMainActivity.TAG, "No devices found");
        }
    }
}
