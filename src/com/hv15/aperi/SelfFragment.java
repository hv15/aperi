
package com.hv15.aperi;


import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListFragment;
import android.database.Cursor;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.hv15.aperi.database.DatabaseHelper;

public class SelfFragment extends ListFragment implements GroupInfoListener,
        ConnectionInfoListener
{
    private View mContentView = null;
    private WifiP2pDevice mDevice = null;
    private WifiP2pDevice mOwner = null;
    private SimpleAdapter mAdaptor = null;
    private ArrayList<HashMap<String, String>> mMap = new ArrayList<HashMap<String, String>>(
            10);

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
        mContentView = inflater.inflate(R.layout.self_layout, container, false);
        mAdaptor = new SimpleAdapter(getActivity(), mMap, R.layout.row_macip,
                DatabaseHelper.DATABASE_FIELDS, new int[] {
                        R.id.my_macip_mac, R.id.my_macip_ip
                });
        setListAdapter(mAdaptor);
        return mContentView;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        ((AperiMainActivity) getActivity()).loaded();
        ((AperiMainActivity) getActivity()).register();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.macip_list_menu, menu);
    }

    /**
     * Get the actual device object
     * 
     * @return this device
     */
    protected WifiP2pDevice getDevice()
    {
        return mDevice;
    }

    /**
     * Get the owner device object
     */
    protected WifiP2pDevice getOwner()
    {
        return mOwner;
    }

    /**
     * Remove owner information
     */
    protected void clearDetails()
    {
        // Clear the ListView
        mMap.clear();
        mAdaptor.notifyDataSetChanged();
        // Removed owner information
        mOwner = null;
        TextView view = (TextView) mContentView.findViewById(R.id.my_g_name);
        view.setText("");
        view = (TextView) mContentView.findViewById(R.id.my_g_status);
        view.setText("");
        view = (TextView) mContentView.findViewById(R.id.my_g_is_group_owner);
        view.setText("");
    }

    /**
     * Update UI for this device.
     */
    public void updateDeviceView(WifiP2pDevice device)
    {
        mDevice = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(mDevice.deviceName + " [" + mDevice.deviceAddress + "]");
        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText("Status: "
                + AperiMainActivity.getDeviceStatus(mDevice.status));
        view = (TextView) mContentView.findViewById(R.id.my_is_group_owner);
        view.setText(mDevice.isGroupOwner() ? "Is: Group Owner" : "Is: Client");
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group)
    {
        mOwner = group.getOwner();
        String owner;
        if (mOwner.deviceAddress.equals(mDevice.deviceAddress)) {
            owner = "YOU ARE THE GROUP OWNER";
        } else {
            owner = mOwner.deviceName + " [" + mOwner.deviceAddress + "]";
        }
        TextView view = (TextView) mContentView.findViewById(R.id.my_g_name);
        view.setText(owner);
        view = (TextView) mContentView.findViewById(R.id.my_g_status);
        view.setText("Status: "
                + AperiMainActivity.getDeviceStatus(mOwner.status));
        view = (TextView) mContentView.findViewById(R.id.my_g_is_group_owner);
        view.setText(mOwner.isGroupOwner() ? "Is: Group Owner" : "Is: Client");
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info)
    {
        Log.i(AperiMainActivity.TAG,
                "Connection info recieved\n" + info.toString());
        ((AperiMainActivity) getActivity()).mSocketService.sendHandShake(info,
                mDevice);
    }

    protected void getData()
    {
        mMap.clear();
        Cursor data = DatabaseHelper
                .getHelper(getActivity())
                .getReadableDatabase()
                .query(DatabaseHelper.DATABASE_TABLE,
                        DatabaseHelper.DATABASE_FIELDS, null, null, null, null,
                        null);
        if (data.moveToFirst()) {
            do {
                HashMap<String, String> row = new HashMap<String, String>(2);
                row.put(DatabaseHelper.DATABASE_MAC, data.getString(0));
                row.put(DatabaseHelper.DATABASE_IP, data.getString(1));
                mMap.add(row);
            } while (data.moveToNext());
        }

        if (data != null && !data.isClosed()) {
            data.close();
        }

        mAdaptor.notifyDataSetChanged();
    }
}
