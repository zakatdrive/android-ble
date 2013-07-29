package com.example.blehw1;

import java.util.ArrayList;
import java.util.Date;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

/**
 * Activity for scanning and displaying available BLE devices.
 */
public class DeviceScanActivity extends ListActivity {
	ArrayList<String> listItems=new ArrayList<String>();
	ArrayAdapter<String> adapter;

	private static final String TAG=DeviceScanActivity.class.getSimpleName();
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler = new Handler();
	private int scanCount;
	private static final int REQUEST_ENABLE_BT=123456;

	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,listItems);
		setListAdapter(adapter);
		
		checkBLE();
		init();
		boolean ret = enableBLE();
		if(ret){
			startScan(false);
		}else{
			Log.d(TAG,getCtx()+" onCreate Waiting for on onActivityResult");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void init(){
		// Initializes Bluetooth adapter.
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
	}
	private void startScan(boolean success){
		if(mBluetoothAdapter == null){
			init();
		}
		if(success){
			mScanning=true;
			scanLeDevice(mScanning);
			return;
		}
		if(enableBLE()){
			mScanning=true;
			scanLeDevice(mScanning);
		}else{
			Log.d(TAG,getCtx()+" startScan Waiting for on onActivityResult success:"+success);
		}
	}
	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					Log.d(TAG,getCtx() + "run stopLeScan");
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
				}
			}, SCAN_PERIOD);
			Log.d(TAG,getCtx()+" scanLeDevice startLeScan:"+enable);
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			Log.d(TAG,getCtx()+ " scanLeDevice stopLeScan:"+enable);
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}
	private static String getCtx(){
		Date dt = new Date();
		return dt+ " thread:"+Thread.currentThread().getName();
	}
	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
			new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				final byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					scanCount++;
					
					String msg=getCtx()+
							"\nLeScanCallback.onLeScan stopLeScan run " +scanCount+
							"\nDevice:" +device+
							"\nRssi:" + rssi+
							"\nScanRecord:" + HexBin.encode(scanRecord);
					Log.d(TAG,msg);
					addItems(msg);
				}
			});
		}
	};
	private void addItems(String msg) {
		synchronized(listItems){
			listItems.add(msg);
			adapter.notifyDataSetChanged();
		}
	}
	public void startScan(View v) {
		startScan(false);
	}
	public void stopScan(View v) {
		mScanning=false;
		scanLeDevice(mScanning);
	}
	public void clear(View v) {
		Log.d(TAG,getCtx()+" called clear");
		synchronized(listItems){
			listItems.clear();
			adapter.notifyDataSetChanged();
		}
	}
	private  void checkBLE(){
		// Use this check to determine whether BLE is supported on the device. Then
		// you can selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	private boolean enableBLE(){
		boolean ret=true;
		// Ensures Bluetooth is available on the device and it is enabled. If not,
		// displays a dialog requesting user permission to enable Bluetooth.
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Log.d(TAG,getCtx()+" enableBLE either mBluetoothAdapter == null or disabled:"+mBluetoothAdapter);
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
			ret=false;
		}
		return ret;
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG,getCtx()+" onActivityResult requestCode="+requestCode+
				", resultCode="+resultCode+", Intent:"+data
				);
		if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
			startScan(true);
		}
	}
}
