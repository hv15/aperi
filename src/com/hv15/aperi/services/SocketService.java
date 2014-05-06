
package com.hv15.aperi.services;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.hv15.aperi.AperiMainActivity;
import com.hv15.aperi.interfaces.DatabaseListener;
import com.hv15.aperi.network.NetPackage;

public class SocketService extends Service
{
    public final static String UPDATE_LIST = "com.hv15.aperi.services.SocketService.UPDATE_LIST";
    private final IBinder mBinder = new LocalSocketBinder(SocketService.this);
    private final LocalBroadcastManager mBroadcast = LocalBroadcastManager
            .getInstance(SocketService.this);
    private final int mPort = 6445;

    private Thread mServer;
    private Thread mClient;
    private WifiP2pDevice mSelf;
    private Handler mToasty;
    private DatabaseListener mDatabase;

    @Override
    public void onCreate()
    {
        super.onCreate();
        startServer();
        mToasty = new Handler();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopServer();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    public boolean status()
    {
        return mServer != null ? mServer.isAlive() : false;
    }

    private void startServer()
    {
        mServer = new Thread(execServer());
        mServer.start();
    }

    private void stopServer()
    {
        if (mServer != null) {
            mServer.interrupt();
        }
    }

    public void setDatabaseListener(DatabaseListener callback)
    {
        mDatabase = callback;
    }

    public void sendHandShakeConnect(WifiP2pInfo info, WifiP2pDevice self)
    {
        mSelf = self;
        mClient = new Thread(execClient(
                info.groupOwnerAddress.getHostAddress(), new NetPackage(
                        self.deviceName, self.deviceAddress, null,
                        NetPackage.CONNECT)));
        mClient.start();
    }

    public void sendHandShakeDisconnect()
    {
        NetPackage packet = new NetPackage(mSelf.deviceName,
                mSelf.deviceAddress, null, NetPackage.DISCONNECT);
        ArrayList<Thread> threads = new ArrayList<Thread>(5);
        for (String[] device : mDatabase.getClients()) {
            Thread thread = new Thread(execClient(device[1], packet));
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Log.e(AperiMainActivity.TAG,
                        "Something went wrong with sending disconnect packets: ");
                e.printStackTrace();
                break;
            }
        }
    }

    private Runnable execServer()
    {
        return new Runnable() {
            private Socket mIncomingSocket;
            private ServerSocket mServer;
            private ObjectInputStream mOIS;
            private NetPackage mPackage;

            @Override
            public void run()
            {
                Log.i(AperiMainActivity.TAG,
                        "Starting HandshakeServer, waiting for handshakes");
                try {
                    // Initialisation of the Server
                    mServer = new ServerSocket(mPort, 10);
                    mServer.setReuseAddress(true);
                    Log.i(AperiMainActivity.TAG, "Waiting for connection...");
                    // Run this until ordered to kill, muhaha...
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(100);
                            mIncomingSocket = mServer.accept();
                            mOIS = new ObjectInputStream(
                                    mIncomingSocket.getInputStream());
                            String out = mOIS.readUTF();
                            mPackage = new Gson().fromJson(out,
                                    NetPackage.class);
                            processPacket(mIncomingSocket.getInetAddress()
                                    .getHostAddress(),
                                    mPackage);
                            // This could be problematic...
                            sendUpdateUIRequest();
                        } catch (IOException e) {
                            Log.e(AperiMainActivity.TAG, e.toString());
                        } catch (JsonSyntaxException e) {
                            Log.e(AperiMainActivity.TAG,
                                    "Object is not JSON!\n" + e.toString());
                        } catch (InterruptedException e) {
                            Log.e(AperiMainActivity.TAG,
                                    "Server failed to wait\n" + e.toString());
                        }
                    }
                } catch (IOException e) {
                    Log.e(AperiMainActivity.TAG,
                            "SocketServer failed:\n" + e.toString());
                    mToasty.post(toasty(
                            "Background Network Listener failed to start!",
                            Color.RED));
                } finally {
                    try {
                        if (mOIS != null) {
                            mOIS.close();
                        }
                        if (mIncomingSocket != null) {
                            mIncomingSocket.close();
                        }
                        if (mServer != null) {
                            mServer.close();
                        }
                    } catch (IOException e) {
                        Log.e(AperiMainActivity.TAG,
                                "SocketServer failed, unrecoverable!");
                        e.printStackTrace();
                    }
                }
                Log.d(AperiMainActivity.TAG, "HandshakeServer finished...");
            }
        };
    }

    private Runnable execClient(final String goal, final NetPackage packet)
    {
        return new Runnable()
        {
            private String mGoal = goal;
            private String mSerSelf = new Gson().toJson(packet);
            private Socket mSocket;
            private ObjectOutputStream mOOS;

            @Override
            public void run()
            {
                Log.i(AperiMainActivity.TAG,
                        "Starting HandshakeClient, sending handshake type => "
                                + NetPackage.type[packet.packetType]);
                try {
                    mSocket = new Socket();
                    mSocket.setReuseAddress(true);
                    mSocket.connect(new InetSocketAddress(mGoal, mPort), 5000);
                    mOOS = new ObjectOutputStream(mSocket.getOutputStream());
                    mOOS.writeUTF(mSerSelf);
                    Log.i(AperiMainActivity.TAG, "HandShakeClient packet sent");
                } catch (SocketException e) {
                    Log.e(AperiMainActivity.TAG,
                            "Client Socket failed =\n" + e.toString());
                } catch (IOException e) {
                    Log.e(AperiMainActivity.TAG,
                            "Client could not send handshake =\n"
                                    + e.toString());
                } finally {
                    try {
                        if (mOOS != null) {
                            mOOS.close();
                        }
                        if (mSocket != null) {
                            mSocket.close();
                        }
                    } catch (IOException e) {
                        Log.e(AperiMainActivity.TAG,
                                "Client Socket failed, unrecoverable!");
                        e.printStackTrace();
                    }
                }
                Log.d(AperiMainActivity.TAG, "HandshakeClient finished...");
            }
        };
    }

    private Runnable toasty(final String message, final int color)
    {
        return new Runnable() {
            @Override
            public void run()
            {
                Toast toast = Toast.makeText(SocketService.this, message,
                        Toast.LENGTH_SHORT);
                toast.getView().setBackgroundColor(color);
                toast.show();
            }
        };
    }

    private void sendUpdateUIRequest()
    {
        Intent update = new Intent(UPDATE_LIST);
        mBroadcast.sendBroadcast(update);
    }

    private void processPacket(String sender, NetPackage packet)
    {
        String message = "Paket processed => ";
        switch (packet.packetType) {
            case NetPackage.CONNECT:
                for (String[] device : mDatabase.getClients()) {
                    new Thread(
                            execClient(device[1], new NetPackage(
                                    packet.deviceName, packet.deviceAddress,
                                    sender, NetPackage.RESPOND)))
                            .start();
                }
                mDatabase
                        .addClient(packet.deviceAddress, sender);
                message += "Added " + packet.deviceName + " ["
                        + packet.deviceAddress
                        + "]: " + sender;
                break;
            case NetPackage.RESPOND:
                mDatabase.addClient(packet.deviceAddress, packet.deviceIP);
                new Thread(
                        execClient(packet.deviceIP, new NetPackage(
                                mSelf.deviceName, mSelf.deviceAddress, null,
                                NetPackage.HELLO))).start();
                message += "Added " + packet.deviceName + " ["
                        + packet.deviceAddress
                        + "]: " + packet.deviceIP;
                break;
            case NetPackage.HELLO:
                mDatabase.addClient(packet.deviceAddress,
                        sender);
                message += "Added " + packet.deviceName + " ["
                        + packet.deviceAddress
                        + "]: " + sender;
                break;
            case NetPackage.DISCONNECT:
                mDatabase.delClient(packet.deviceAddress);
                message += "Removed " + packet.deviceName + " ["
                        + packet.deviceAddress
                        + "]: " + sender;
                break;
            default:
                message += "ERROR, unknown packet recieved -> " + packet;
                break;
        }
        Log.i(AperiMainActivity.TAG, message);
        mToasty.post(toasty(message, Color.GREEN));
    }
}
