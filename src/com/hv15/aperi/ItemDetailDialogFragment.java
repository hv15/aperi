package com.hv15.aperi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * @author Hans
 * 
 */
public class ItemDetailDialogFragment extends DialogFragment
{
    private View mContentView = null;
    private WifiP2pDevice mDevice;
    private String mAddr;

    /**
     * Create a new instance
     * <p>
     * This is dialog with show the contents of the passed in
     * {@link WifiP2pDevice} contents using its {@link WifiP2pDevice#toString()
     * toString()} method.
     * <p>
     * <em>
     * If there is a {@link WifiP2pInfo} object associated with this device, this will be displayed as well.
     * </em>
     * 
     * @param device
     *            the {@link WifiP2pDevice} object for the device to show
     * @return the dialog fragment
     * @see AperiMainActivity#mDatabaseH
     */
    public static ItemDetailDialogFragment newInstance(WifiP2pDevice device)
    {
        ItemDetailDialogFragment frag = new ItemDetailDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("device", device);
        frag.setArguments(bundle);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        Bundle args = getArguments();
        mDevice = (WifiP2pDevice) args.getParcelable("device");

        mAddr = ((AperiMainActivity) getActivity())
                .getDeviceIP(mDevice.deviceAddress);

        getDialog().setTitle(mDevice.deviceName + " Info");

        mContentView = inflater.inflate(R.layout.item_dialog_layout, container,
                false);

        TextView view = (TextView) mContentView
                .findViewById(R.id.item_dialog_debug);
        view.setText(mDevice.toString());

        // /* Called when the user selects a file to send */
        // mContentView.findViewById(R.id.item_dialog_btn_start_client).setOnClickListener(
        // new View.OnClickListener() {
        //
        // @Override
        // public void onClick(View v) {
        // // Allow user to pick an image from Gallery or other
        // // registered apps
        // //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // //intent.setType("image/*");
        // //startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
        // }
        // });

        if (mAddr != null) {
            view = (TextView) mContentView.findViewById(R.id.item_dialog_info);
            view.setText("IP: " + mAddr);
            view.setVisibility(View.VISIBLE);
        }
        return mContentView;
    }

    // private void updateView()
    // {
    // if (mProgressDialog != null && mProgressDialog.isShowing()) {
    // mProgressDialog.dismiss();
    // }
    // // The owner IP is now known.
    // TextView view = (TextView)
    // mContentView.findViewById(R.id.item_dialog_owner);
    // view.setText(mConnectionInfo.isGroupOwner ? "Group Owner" : "Client");
    // // InetAddress from WifiP2pInfo struct.
    // view = (TextView) mContentView.findViewById(R.id.item_dialog_ip);
    // view.setText("gIP: "
    // + mConnectionInfo.groupOwnerAddress.getHostAddress());
    // ((AperiMainActivity) getActivity()).mDatabase.put(
    // mDevice.deviceAddress, mConnectionInfo.groupOwnerAddress);
    // // After the group negotiation, we assign the group owner as the file
    // // server. The file server is single threaded, single connection server
    // // socket.
    // if (mConnectionInfo.groupFormed && mConnectionInfo.isGroupOwner) {
    // //new FileServerAsyncTask(getActivity(),
    // mContentView.findViewById(R.id.status_text)).execute();
    // } else if (mConnectionInfo.groupFormed) {
    // // The other device acts as the client. In this case, we enable the
    // // get file button.
    // mContentView.findViewById(R.id.item_dialog_btn_start_client).setVisibility
    // (View.VISIBLE);
    // }
    // // hide the connect button
    // mContentView.findViewById(R.id.item_dialog_btn_connect).setVisibility(View
    // .GONE);
    // mContentView.findViewById(R.id.item_dialog_btn_disconnect).setVisibility(View
    // .VISIBLE);
    // }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends
            AsyncTask<Void, Void, String>
    {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
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
                statusText.setText("File copied - " + result);
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
            statusText.setText("Opening a server socket");
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
