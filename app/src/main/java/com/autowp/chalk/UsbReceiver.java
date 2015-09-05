package com.autowp.chalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.autowp.chalk.driver.ChalkDriverAndroidAPI;

/**
 * Created by Dmitry on 28.07.2015.
 */
public class UsbReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        System.out.println("USBReceiver: " + action);

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (ChalkDriverAndroidAPI.isCompatibleDevice(device)) {
                Intent serviceIntent = new Intent(context, ChalkService.class);
                serviceIntent.setAction("usbAttached");
                serviceIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
                context.startService(serviceIntent);
            }

        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (ChalkDriverAndroidAPI.isCompatibleDevice(device)) {
                Intent serviceIntent = new Intent(context, ChalkService.class);
                serviceIntent.setAction("usbDetached");
                serviceIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
                context.startService(serviceIntent);
            }

        } else if (ChalkService.ACTION_USB_PERMISSION.equals(action)) {

            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            Intent serviceIntent = new Intent(context, ChalkService.class);
            serviceIntent.setAction("usbPermission");
            serviceIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
            context.startService(serviceIntent);

        }
    }
}
