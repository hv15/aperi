
package com.hv15.aperi;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
        ((AperiMainActivity) getActivity()).mSocketService
                .sendHandShakeConnect(info, mDevice);
        if (info.groupFormed) {
            new FileServerAsyncTask(getActivity()).execute();
        }
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

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends
            AsyncTask<Void, Void, String>
    {

        private Context context;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... params)
        {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(AperiMainActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(AperiMainActivity.TAG, "Server: connection done");
                final File f = new File(
                        Environment.getExternalStorageDirectory() + "/"
                                + context.getPackageName() + "/wifip2pshared-"
                                + System.currentTimeMillis()
                                + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(AperiMainActivity.TAG,
                        "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(AperiMainActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result)
        {
            if (result != null) {
                // statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute()
        {
            // statusText.setText("Opening a server socket");
        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out)
    {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(AperiMainActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
}
