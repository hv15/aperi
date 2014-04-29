package com.hv15.aperi.services;

import android.os.Binder;

public class LocalSocketBinder extends Binder
{
    private SocketService mService;

    public LocalSocketBinder(SocketService service) {
        mService = service;
    }

    public SocketService getService()
    {
        return mService;
    }
}
