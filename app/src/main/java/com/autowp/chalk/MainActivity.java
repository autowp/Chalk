package com.autowp.chalk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.autowp.chalk.driver.ChalkException;

public class MainActivity extends AppCompatActivity implements ServiceConnection, ChalkService.OnDataChangedListener, ChalkService.OnStatusChangedListener {

    //private static final String TAG = "MainActivity";

    ChalkService mService = null;
    private ToggleButton mBtnBacklightToggle;
    private ToggleButton mBtnAutoBacklightToggle;
    private SeekBar mSeekBar;
    private ProgressBar mProgressBar;
    private ToggleButton mBtnFlipH;
    private ToggleButton mBtnFlipV;
    private Button mBtnDec;
    private Button mBtnInc;
    private Button mBtnMin;
    private Button mBtnMax;
    private TextView mTextStatus;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnBacklightToggle = (ToggleButton)findViewById(R.id.btnBacklightToggle);
        mBtnAutoBacklightToggle = (ToggleButton)findViewById(R.id.btnAutoBacklightToggle);
        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);

        mBtnDec = (Button)findViewById(R.id.btnDec);
        mBtnInc = (Button)findViewById(R.id.btnInc);
        mBtnMin = (Button)findViewById(R.id.btnMin);
        mBtnMax = (Button)findViewById(R.id.btnMax);

        mBtnFlipH = (ToggleButton)findViewById(R.id.btnFlipH);
        mBtnFlipV = (ToggleButton)findViewById(R.id.btnFlipV);

        mTextStatus = (TextView)findViewById(R.id.textStatus);

        mTextStatus.setText(getString(R.string.status_service_started));

        //startService(new Intent(this, ChalkService.class));

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                if (fromUser) {
                    try {
                        mService.setBacklightLevel((byte) progress);
                    } catch (ChalkException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onDestroy() {

        if (mService != null) {
            mService.removeEventListener((ChalkService.OnDataChangedListener) this);
            mService.removeEventListener((ChalkService.OnStatusChangedListener) this);
            unbindService(this);
            mService = null;
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

     @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_connect:
                if (mService != null) {
                    mService.findCompatibleDevice();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mService == null) {
            Intent intent = new Intent(this, ChalkService.class);
            bindService(intent, this, Context.BIND_AUTO_CREATE);
        }

    }

    @Override
    public void onPause() {
        if (mService != null) {
            mService.removeEventListener((ChalkService.OnDataChangedListener) this);
            mService.removeEventListener((ChalkService.OnStatusChangedListener) this);
            unbindService(this);
            mService = null;
        }

        super.onPause();
    }

    @Override
    public void onServiceConnected(final ComponentName componentName, final IBinder service) {
        ChalkService.LocalBinder binder = (ChalkService.LocalBinder) service;
        mService = binder.getService();
        mService.addEventListener((ChalkService.OnDataChangedListener) this);
        mService.addEventListener((ChalkService.OnStatusChangedListener) this);

        handleIsConnectedChanged(mService.isConnected());
        handleHasPermissionChanged(mService.hasPermission());
        handleDeviceFoundChanged(mService.getCompatibleDevice());

        mBtnBacklightToggle.setChecked(mService.isBacklightEnabled());
        mBtnAutoBacklightToggle.setChecked(mService.isAutoBacklightEnabled());
        mSeekBar.setProgress(mService.getCurrentBacklightLevel());
        mProgressBar.setProgress(mService.getAmbientLightLevel());

        mBtnFlipH.setChecked(mService.isFlipH());
        mBtnFlipV.setChecked(mService.isFlipV());

        mTextStatus.setText(mService.getStatusString());
    }

    @Override
    public void onServiceDisconnected(final ComponentName componentName) {
        mService.removeEventListener((ChalkService.OnDataChangedListener) this);
        mService.removeEventListener((ChalkService.OnStatusChangedListener) this);
        mService = null;
        mTextStatus.setText("Disconnected");
    }

    public void minClick(final View view) throws ChalkException {
        mService.setBrightnessToMin();
    }

    public void maxClick(final View view) throws ChalkException {
        mService.setBrightnessToMax();
    }

    public void decClick(final View view) throws ChalkException {
        mService.brightnessDec();
    }

    public void incClick(final View view) throws ChalkException {
        mService.brightnessInc();
    }

    public void backlightToggleClick(final View view) throws ChalkException {
        mService.setBacklightEnabled(mBtnBacklightToggle.isChecked());
    }

    public void autoBacklightToggleClick(final View view) throws ChalkException {
        mService.setAutoBacklightEnabled(mBtnAutoBacklightToggle.isChecked());
    }

    @Override
    public void handleBacklightLevelChanged(final byte level) {
        SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setProgress(level);
    }

    @Override
    public void handleAmbientLightLevelChanged(final byte level) {
        mProgressBar.setProgress(level);
    }

    @Override
    public void handleBacklightEnabledChanged(final boolean enabled) {
        mBtnBacklightToggle.setChecked(enabled);
    }

    @Override
    public void handleAutoBrightnessEnabledChanged(final boolean enabled) {
        mBtnAutoBacklightToggle.setChecked(enabled);
    }

    @Override
    public void handleFlipHChanged(boolean value) {
        mBtnFlipH.setChecked(value);
    }

    @Override
    public void handleFlipVChanged(boolean value) {
        mBtnFlipV.setChecked(value);
    }

    public void flipHClick(final View view) throws ChalkException {
        mService.setFlipH(mBtnFlipH.isChecked());
    }

    public void flipVClick(final View view) throws ChalkException {
        mService.setFlipV(mBtnFlipV.isChecked());
    }

    @Override
    public void handleStatusChanged() {
        mTextStatus.setText(mService.getStatusString());
    }

    @Override
    public void handleDeviceFoundChanged(UsbDevice usbDevice) {
        if (usbDevice != null) {
            System.out.println("Device: found " + usbDevice.getDeviceName());
        } else {
            System.out.println("Device: waiting for device ...");
        }
    }

    @Override
    public void handleIsConnectedChanged(final boolean isConnected) {

        mTextStatus.setText(mService.getStatusString());

        mBtnAutoBacklightToggle.setEnabled(isConnected);
        mSeekBar.setEnabled(isConnected);
        mBtnBacklightToggle.setEnabled(isConnected);
        mBtnDec.setEnabled(isConnected);
        mBtnInc.setEnabled(isConnected);
        mBtnMin.setEnabled(isConnected);
        mBtnMax.setEnabled(isConnected);
        mBtnFlipH.setEnabled(isConnected);
        mBtnFlipV.setEnabled(isConnected);
    }

    @Override
    public void handleHasPermissionChanged(boolean hasPermission) {
        System.out.println("hasPermission " + hasPermission);
    }
}
