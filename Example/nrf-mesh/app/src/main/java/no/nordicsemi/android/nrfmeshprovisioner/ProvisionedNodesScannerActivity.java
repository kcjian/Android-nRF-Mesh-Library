/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrfmeshprovisioner;

import android.Manifest;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import no.nordicsemi.android.nrfmeshprovisioner.adapter.DevicesAdapter;
import no.nordicsemi.android.nrfmeshprovisioner.adapter.ExtendedBluetoothDevice;
import no.nordicsemi.android.nrfmeshprovisioner.ble.BleMeshManager;
import no.nordicsemi.android.nrfmeshprovisioner.di.Injectable;
import no.nordicsemi.android.nrfmeshprovisioner.utils.Utils;
import no.nordicsemi.android.nrfmeshprovisioner.viewmodels.ProvisionedNodesScannerViewModel;
import no.nordicsemi.android.nrfmeshprovisioner.viewmodels.ScannerLiveData;

public class ProvisionedNodesScannerActivity extends AppCompatActivity implements Injectable, DevicesAdapter.OnItemClickListener {
	private static final int REQUEST_ACCESS_COARSE_LOCATION = 1022; // random number
	public static int CONNECT_TO_NETWORK = 1023; //random number
	public static final String SELECTED_NODE = "SELECTED_NODE";
	public static final String NETWORK_ID = "NETWORK_ID";

	@Inject
	ViewModelProvider.Factory mViewModelFactory;

	@BindView(R.id.state_scanning) View mScanningView;
	@BindView(R.id.no_devices)View mEmptyView;
	@BindView(R.id.no_location_permission) View mNoLocationPermissionView;
	@BindView(R.id.action_grant_location_permission) Button mGrantPermissionButton;
	@BindView(R.id.action_permission_settings) Button mPermissionSettingsButton;
	@BindView(R.id.no_location)	View mNoLocationView;
	@BindView(R.id.bluetooth_off) View mNoBluetoothView;

	private ProvisionedNodesScannerViewModel mViewModel;

	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scanner);
		ButterKnife.bind(this);

		final Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(R.string.app_name);

		// Create view model containing utility methods for scanning
		mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(ProvisionedNodesScannerViewModel.class);
		mViewModel.getScannerState().startScanning();
		mViewModel.getScannerState().observe(this, this::startScan);

		// Configure the recycler view
		final RecyclerView recyclerViewDevices = findViewById(R.id.recycler_view_ble_devices);
		recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
		final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerViewDevices.getContext(), DividerItemDecoration.VERTICAL);
		recyclerViewDevices.addItemDecoration(dividerItemDecoration);
		final DevicesAdapter adapter = new DevicesAdapter(this, mViewModel.getScannerState());
		adapter.setOnItemClickListener(this);
		recyclerViewDevices.setAdapter(adapter);

		/*mViewModel.isDeviceReady().observe(this, isDeviceReady -> {
			if(isDeviceReady){
				finish();
			}
		});*/

	}

	@Override
	protected void onStop() {
		super.onStop();
		stopScan();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == ReconnectActivity.REQUEST_DEVICE_READY){
			if(resultCode == RESULT_OK){
				final boolean isDeviceReady = data.getBooleanExtra(Utils.ACTIVITY_RESULT, false);
				if(isDeviceReady){
					finish();
				}
			}
		}
	}

	@Override
	public void onItemClick(final ExtendedBluetoothDevice device) {
		stopScan();
		final Intent meshProvisionerIntent = new Intent(this, ReconnectActivity.class);
		meshProvisionerIntent.putExtra(Utils.EXTRA_DEVICE, device);
		startActivityForResult(meshProvisionerIntent, ReconnectActivity.REQUEST_DEVICE_READY);
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case REQUEST_ACCESS_COARSE_LOCATION:
				mViewModel.refresh();
				break;
		}
	}

	@OnClick(R.id.action_enable_location)
	public void onEnableLocationClicked() {
		final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	@OnClick(R.id.action_enable_bluetooth)
	public void onEnableBluetoothClicked() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivity(enableIntent);
	}

	@OnClick(R.id.action_grant_location_permission)
	public void onGrantLocationPermissionClicked() {
		Utils.markLocationPermissionRequested(this);
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
	}

	@OnClick(R.id.action_permission_settings)
	public void onPermissionSettingsClicked() {
		final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.fromParts("package", getPackageName(), null));
		startActivity(intent);
	}


	/**
	 * Start scanning for Bluetooth devices or displays a message based on the scanner state.
	 */
	private void startScan(final ScannerLiveData state) {
		// First, check the Location permission. This is required on Marshmallow onwards in order to scan for Bluetooth LE devices.
		if (Utils.isLocationPermissionsGranted(this)) {
			mNoLocationPermissionView.setVisibility(View.GONE);

			// Bluetooth must be enabled
			if (state.isBluetoothEnabled()) {
				mNoBluetoothView.setVisibility(View.GONE);

				// We are now OK to start scanning
				mViewModel.startScan(BleMeshManager.MESH_PROXY_UUID);
				mScanningView.setVisibility(View.VISIBLE);

				if (state.isEmpty()) {
					mEmptyView.setVisibility(View.VISIBLE);

					if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
						mNoLocationView.setVisibility(View.INVISIBLE);
					} else {
						mNoLocationView.setVisibility(View.VISIBLE);
					}
				} else {
					mEmptyView.setVisibility(View.GONE);
				}
			} else {
				mNoBluetoothView.setVisibility(View.VISIBLE);
				mScanningView.setVisibility(View.INVISIBLE);
				mEmptyView.setVisibility(View.GONE);
			}
		} else {
			mNoLocationPermissionView.setVisibility(View.VISIBLE);
			mNoBluetoothView.setVisibility(View.GONE);
			mScanningView.setVisibility(View.INVISIBLE);
			mEmptyView.setVisibility(View.GONE);

			final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
			mGrantPermissionButton.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
			mPermissionSettingsButton.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
		}
	}


	/**
	 * stop scanning for bluetooth devices.
	 */
	private void stopScan() {
		mViewModel.stopScan();
	}
}
