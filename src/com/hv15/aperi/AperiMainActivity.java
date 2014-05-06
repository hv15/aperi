package com.hv15.aperi;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.hv15.aperi.adaptors.TabsPagerAdaptor;
import com.hv15.aperi.database.DatabaseHelper;
import com.hv15.aperi.interfaces.DatabaseListener;
import com.hv15.aperi.interfaces.DeviceActionListener;
import com.hv15.aperi.interfaces.MacIpListener;
import com.hv15.aperi.services.LocalSocketBinder;
import com.hv15.aperi.services.SocketService;

/**
 * An activity that uses android.net.wifi.p2p API to discover and connect with
 * available devices. WifiP2p APIs are asynchronous and rely on callback
 * mechanism using interfaces to notify the application of operation success or
 * failure. The application makes use of a {@link BroadcastReceiver} for
 * notification of Wi-Fi state related events.
 * <p>
 * Furthermore the application makes use of the Android Support Library,
 * allowing for the use of the {@link TabsPagerAdaptor} to provide a
 * <i>swiping</i> UI effect.
 * 
 * @author Hans-Nikolai Viessmann
 * @version 1 (prototype 3798)
 */
public class AperiMainActivity extends Activity implements ChannelListener,
        DeviceActionListener, TabListener, MacIpListener
{
    public static final String TAG = "AperiMainActivity";

    // Variables for WiFi-Direct related logic
    private WifiP2pManager mManager;
    private boolean mIsWifiP2pEnabled = false;
    private boolean mRetryChannel = false;
    private boolean mIsReady = false;
    private final IntentFilter mIntentFilter = new IntentFilter();
    private Channel mChannel;
    private AperiBroadcastReceiver mReceiver = null;

    // Database and other logic
    protected DatabaseHelper mDatabaseH = DatabaseHelper.getHelper(this);
    private BroadcastReceiver mLReciever = null;
    private final IntentFilter mLIntentFilter = new IntentFilter();

    // Variables for UI
    private ViewPager mViewPager;
    private ActionBar mActionBar;
    private TabsPagerAdaptor mAdaptor;

    // Variables for background services
    protected SocketService mSocketService;
    private boolean mSocketServiceBound = false;
    private DatabaseListener dbcallback = new DatabaseListener() {

        @Override
        public void addClient(String mac, String ip)
        {
            ContentValues cv = new ContentValues(2);
            cv.put(DatabaseHelper.DATABASE_MAC, mac);
            cv.put(DatabaseHelper.DATABASE_IP, ip);
            SQLiteDatabase db = mDatabaseH.getWritableDatabase();
            try {
                db.replaceOrThrow(DatabaseHelper.DATABASE_TABLE, null, cv);
            } catch (SQLException e) {
                Log.e(AperiMainActivity.TAG, "Could not add/replace [" + mac
                        + "]:\n" + e.getMessage());
            }
        }

        @Override
        public void delClient(String mac)
        {
            mDatabaseH.getWritableDatabase().delete(
                    DatabaseHelper.DATABASE_TABLE,
                    DatabaseHelper.DATABASE_MAC + " = ?", new String[] {
                        mac
                    });
        }

        @Override
        public int size()
        {
            SQLiteDatabase db = mDatabaseH.getReadableDatabase();
            return (int) DatabaseUtils.queryNumEntries(db,
                    DatabaseHelper.DATABASE_TABLE);
        }

        @Override
        public void clearClients()
        {
            mDatabaseH.getWritableDatabase().delete(DatabaseHelper.DATABASE_TABLE, null, null);
        }

        @Override
        public ArrayList<String[]> getClients()
        {
            ArrayList<String[]> array = new ArrayList<String[]>(5);
            SQLiteDatabase db = mDatabaseH.getReadableDatabase();
            Cursor cr = db.query(DatabaseHelper.DATABASE_TABLE,
                    DatabaseHelper.DATABASE_FIELDS, null, null, null, null,
                    null);

            if (cr.moveToFirst()) {
                do {
                    String[] g = {
                            cr.getString(0), cr.getString(1)
                    };
                    array.add(g);
                } while (cr.moveToNext());
            }

            return array;
        }
    };
    private ServiceConnection mSocketServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mSocketServiceBound = false;
            Toast.makeText(AperiMainActivity.this,
                    "SocketService unexpectently was disconnected!",
                    Toast.LENGTH_SHORT).show();
            Log.w(AperiMainActivity.TAG,
                    "SocketService unexpectently was disconnected!");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            LocalSocketBinder binder = (LocalSocketBinder) service;
            mSocketService = binder.getService();
            try {
                mSocketService.setDatabaseListener(dbcallback);
            } catch (Throwable t) {
                Log.e(TAG, "Failed to register callback in SocketService", t);
            }
            mSocketServiceBound = true;
        }
    };

    /* END OF VARIABLES */

    /**
     * Method to remove a single device from the database
     * 
     * @param mac
     *            The MAC address of the device to be removed
     */
    protected void delClient(String mac)
    {
        mDatabaseH.getWritableDatabase().delete(DatabaseHelper.DATABASE_TABLE,
                DatabaseHelper.DATABASE_MAC + "= ?", new String[] {
                    mac
                });
    }

    /**
     * Method to remove all entries from the MACIP SQLite Database
     */
    protected void delClients()
    {
        mDatabaseH.getWritableDatabase().delete(DatabaseHelper.DATABASE_TABLE,
                "1", null);
    }

    /**
     * Retrieve an IP address for a specific device from the database
     * 
     * @param mac
     *            The MAC address of the device being searched for
     * @return Either the IP of a found device, or null if not found
     */
    protected String getDeviceIP(String mac)
    {
        String addr = null;
        SQLiteDatabase db = mDatabaseH.getReadableDatabase();
        Cursor data = db.query(DatabaseHelper.DATABASE_TABLE,
                DatabaseHelper.DATABASE_FIELDS, DatabaseHelper.DATABASE_MAC
                        + "= ?", new String[] {
                    mac
                }, null, null, null);
        if (data.moveToFirst()) {
            addr = data.getString(1);
        }
        return addr;
    }

    private void bindSocketService()
    {
        if (!mSocketServiceBound) {
            Intent intent = new Intent(getApplicationContext(),
                    SocketService.class);
            getApplicationContext().bindService(intent,
                    mSocketServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindSocketService()
    {
        if (mSocketServiceBound) {
            getApplicationContext().unbindService(mSocketServiceConnection);
            mSocketServiceBound = false;
        }
    }

    /**
     * @param state
     *            the state of the WiFi
     */
    protected void setIsWifiP2pEnabled(boolean state)
    {
        mIsWifiP2pEnabled = state;
    }

    /**
     * Get the Fragment at index
     * 
     * @param index
     *            index of the fragment within the ViewPager
     * @return the fragment at index, or null if not found
     * @see TabsPagerAdaptor#getFragmentByPosition(int)
     * @see TabsPagerAdaptor#makeFragmentName(int, int)
     */
    protected Fragment getFragment(int index)
    {
        /*
         * Safe(st) way to get Fragment. Makes no assumption on tagging
         * convention.
         */
        return mAdaptor.getFragmentByPosition(index);

        /*
         * Safe(r) way to get Fragment. This might change in future as it is
         * based upon how the Adaptor tags the fragments itself.
         */
        // String tag = TabsPagerAdaptor.makeFragmentName(mViewPager.getId(),
        // index);
        // return getFragmentManager().findFragmentByTag(tag);
    }

    /**
     * Callback for SelfFragment to prevent race-conditions
     */
    protected void loaded()
    {
        mIsReady = true;
    }

    /**
     * Callback to register the AperiBroadcastReceiver to the Activity
     * <p>
     * <em>
     * This is a bit of a hack. As far as I can tell there is much of a problem with doing something
     * like this, just that it might be more convenient to associate the AperiBroadcastReceiver
     * with the SelfFragment considering that it is the default starting point for the user. Also it
     * might be an idea to think about using a background Service instead of a BroadcastReciever so
     * that this application could then function (partially) in the background as well.
     * </em>
     * </p>
     */
    protected void register()
    {
        if (mIsReady) {
            // Register the BroadcastReciever
            mReceiver = new AperiBroadcastReceiver(mManager,
                    mChannel,
                    this);
            registerReceiver(mReceiver, mIntentFilter);
        }
    }

    /**
     * Get a human understandable status message
     * 
     * @param deviceStatus
     *            as specified by WifiP2pDevice
     * @return String with human-readable status
     * @see WifiP2pDevice
     */
    protected static String getDeviceStatus(int deviceStatus)
    {
        String status;
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                status = "Available";
                break;
            case WifiP2pDevice.INVITED:
                status = "Invited";
                break;
            case WifiP2pDevice.CONNECTED:
                status = "Connected";
                break;
            case WifiP2pDevice.FAILED:
                status = "Failed";
                break;
            case WifiP2pDevice.UNAVAILABLE:
                status = "Unavailable";
                break;
            default:
                status = "Unknown";
                break;
        }
        Log.d(AperiMainActivity.TAG, "Peer status: " + status);
        return status;
    }

    /**
     * Remove all peers.
     * <p>
     * This is called on {@link AperiBroadcastReceiver} receiving at state
     * change event of {@link WifiP2pManager#WIFI_P2P_STATE_CHANGED_ACTION
     * WIFI_P2P_STATE_CHANGED_ACTION}.
     * 
     * @see DeviceListFragment#clearPeers()
     */
    protected void clearPeers()
    {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragment(TabsPagerAdaptor.LIST_FRAG);
        if (fragmentList != null) {
            fragmentList.clearPeers();
            Log.i(TAG, "Peer list reset");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // add necessary intent values to be matched.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter
                .addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter
                .addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Initialise LocalServiceReciever
        mLIntentFilter.addAction(SocketService.UPDATE_LIST);
        mLReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                updateMap();
            }
        };

        // Initialise
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        // Initialise
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mActionBar = getActionBar();
        mAdaptor = new TabsPagerAdaptor(getFragmentManager());

        // Set the Tabs Pager
        mViewPager.setAdapter(mAdaptor);
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Add tabs
        for (int i = 0; i < mAdaptor.getCount(); i++) {
            mActionBar.addTab(mActionBar.newTab()
                    .setText(mAdaptor.getPageTitle(i))
                    .setTabListener(this));
        }

        // Bind to SocketService
        bindSocketService();

        // ensure that tabs work
        mViewPager
                .setOnPageChangeListener(new ViewPager.OnPageChangeListener()
                {
                    @Override
                    public void onPageSelected(int position)
                    {
                        mActionBar.setSelectedNavigationItem(position);
                    }

                    @Override
                    public void onPageScrolled(int position, float arg1,
                            int arg2)
                    {
                    }

                    @Override
                    public void onPageScrollStateChanged(int index)
                    {
                    }
                });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        register();
        LocalBroadcastManager.getInstance(this).registerReceiver(mLReciever,
                mLIntentFilter);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLReciever);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unbindSocketService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (mManager != null && mChannel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.

                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } else {
                    Log.e(TAG, "mChannel or manager is null");
                }
                return true;

            case R.id.atn_direct_discover:
                if (!mIsWifiP2pEnabled) {
                    Toast.makeText(AperiMainActivity.this,
                            R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceListFragment list = (DeviceListFragment) getFragment(TabsPagerAdaptor.LIST_FRAG);
                list.onInitiateDiscovery();
                mManager.discoverPeers(mChannel,
                        new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess()
                            {
                                Toast.makeText(AperiMainActivity.this,
                                        "Discovery Initiated",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int reasonCode)
                            {
                                String reason = reasonCode == WifiP2pManager.P2P_UNSUPPORTED ? "P2P not supported"
                                        : (reasonCode == WifiP2pManager.ERROR ? "Error"
                                                : (reasonCode == WifiP2pManager.BUSY) ? "System Busy, try again later"
                                                        : "Unknown :'(");
                                Toast.makeText(AperiMainActivity.this,
                                        "Discovery Failed : " + reason,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                return true;
            case R.id.macip_refresh:
                updateMap();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void updateMap()
    {
        SelfFragment self = (SelfFragment) getFragment(TabsPagerAdaptor.SELF_FRAG);
        self.getData();
    }

    @Override
    public void connect(WifiP2pConfig config)
    {
        mManager.connect(mChannel, config, new ActionListener() {

            @Override
            public void onSuccess()
            {
                // AperiBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason)
            {
                Toast.makeText(AperiMainActivity.this,
                        "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect()
    {
        mSocketService.sendHandShakeDisconnect();
        mManager.removeGroup(mChannel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode)
            {
                Log.e(TAG, "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess()
            {
            }
        });
    }

    @Override
    public void onChannelDisconnected()
    {
        // we will try once more
        if (mManager != null && !mRetryChannel) {
            Toast.makeText(this, "Channel lost. Trying again",
                    Toast.LENGTH_LONG).show();
            clearPeers();
            mRetryChannel = true;
            mManager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(
                    this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect()
    {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (mManager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragment(TabsPagerAdaptor.LIST_FRAG);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                mManager.cancelConnect(mChannel, new ActionListener() {

                    @Override
                    public void onSuccess()
                    {
                        Toast.makeText(AperiMainActivity.this,
                                "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode)
                    {
                        Toast.makeText(
                                AperiMainActivity.this,
                                "Connect abort request failed. Reason Code: "
                                        + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    /* THIS RELATES TO ACTIONBAR TABS, NOT PART OF MAIN PROGRAM LOGIC */

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft)
    {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft)
    {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft)
    {
    }
}
