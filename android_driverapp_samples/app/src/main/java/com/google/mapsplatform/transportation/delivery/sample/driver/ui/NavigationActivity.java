/* Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mapsplatform.transportation.delivery.sample.driver.ui;

import static com.google.android.libraries.navigation.SpeedAlertSeverity.MAJOR;
import static com.google.android.libraries.navigation.SpeedAlertSeverity.MINOR;
import static com.google.android.libraries.navigation.SpeedAlertSeverity.NONE;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.android.libraries.mapsplatform.transportation.driver.api.delivery.DeliveryDriverApi;
import com.google.android.libraries.mapsplatform.transportation.driver.api.delivery.vehiclereporter.DeliveryVehicleReporter;
import com.google.android.libraries.navigation.ListenableResultFuture;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.Navigator.RouteStatus;
import com.google.android.libraries.navigation.SimulationOptions;
import com.google.android.libraries.navigation.SpeedAlertOptions;
import com.google.android.libraries.navigation.SpeedAlertSeverity;
import com.google.android.libraries.navigation.SpeedometerUiOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.android.libraries.navigation.Waypoint;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.mapsplatform.transportation.delivery.sample.driver.MainActivity;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.auth.DeliveryDriverTokenFactory;
import com.google.mapsplatform.transportation.delivery.sample.driver.backend.SampleBackend;
import com.google.mapsplatform.transportation.delivery.sample.driver.delivery.DeliveryManager;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.VehicleStopState;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.ItineraryListUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NavigationActivity extends AppCompatActivity {

    private static final String TAG = NavigationActivity.class.getName();

    private static final Logger logger = Logger.getLogger(TAG);

    private SupportNavigationFragment navigationFragment;
    private TextView addressTextView;
    private TextView detailTextView;
    private ImageButton exitButton;

    // Extra fields to be passed in via the intent.
    public static final String EXTRA_END_LOCATION = "end_location";
    public static final String EXTRA_END_LOCATION_ADDRESS = "end_location_address";
    public static final String EXTRA_START_LOCATION = "start_location";
    public static final String EXTRA_DETAIL = "detail";
    public static final String EXTRA_PROVIDER_ID = "provider_id";
    public static final String EXTRA_VEHICLE_ID = "vehicle_id";
    public static final String EXTRA_LOCATION_TRACKING = "location_tracking";
    public static final String EXTRA_SIMULATION = "simulation";

    private final DeliveryManager manager = DeliveryManager.getInstance();

    private Navigator navigator;
    private GoogleMap googleMap;
    private LatLng location;
    private String endLocationAddress;
    private LatLng startLocation;
    private ActivityResultLauncher<String[]> locationLauncher;
    private boolean simulation;
    private boolean locationTracking;

    private boolean arrived;

    private DeliveryVehicleReporter vehicleReporter;
    private Navigator.ArrivalListener arrivalListener;

    private SpeedAlertSeverity lastSeverity;
    private boolean manualExit;

    private int stopIndex;

    private String vehicleId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        arrived = false;
        lastSeverity = NONE;
        manualExit = false;
        stopIndex = 0;

        addressTextView = findViewById(R.id.address_text_view);
        detailTextView = findViewById(R.id.detail_text_view);
        exitButton = findViewById(R.id.exit_button);

        navigationFragment =
                (SupportNavigationFragment)
                        getSupportFragmentManager().findFragmentById(R.id.navigation_fragment);
        navigationFragment.getMapAsync((googleMap) -> {
            googleMap.setTrafficEnabled(true);
            googleMap.setMapStyle(new MapStyleOptions(NavigationActivity.this.getString(R.string.map_options)));
            NavigationActivity.this.googleMap = googleMap;
        });
        SpeedometerUiOptions speedometerUiOptions =
                new SpeedometerUiOptions.Builder()
                        .setBackgroundColorDayMode(MINOR, ContextCompat.getColor(NavigationActivity.this, R.color.minorSpeedAlertBackgroundColorDayMode))
                        .setBackgroundColorNightMode(MINOR, ContextCompat.getColor(NavigationActivity.this, R.color.minorSpeedAlertBackgroundColorNightMode))
                        .setTextColorDayMode(MINOR, ContextCompat.getColor(NavigationActivity.this, R.color.minorSpeedAlertTextColorDayMode))
                        .setTextColorNightMode(MINOR, ContextCompat.getColor(NavigationActivity.this, R.color.minorSpeedAlertTextColorNightMode))
                        .setBackgroundColorDayMode(MAJOR, ContextCompat.getColor(NavigationActivity.this, R.color.majorSpeedAlertBackgroundColorDayMode))
                        .setBackgroundColorNightMode(MAJOR, ContextCompat.getColor(NavigationActivity.this, R.color.majorSpeedAlertBackgroundColorNightMode))
                        .setTextColorDayMode(MAJOR, ContextCompat.getColor(NavigationActivity.this, R.color.majorSpeedAlertTextColorDayMode))
                        .setTextColorNightMode(MAJOR, ContextCompat.getColor(NavigationActivity.this, R.color.majorSpeedAlertTextColorNightMode))
                        .build();
        navigationFragment.setSpeedometerUiOptions(speedometerUiOptions);
        navigationFragment.setSpeedometerEnabled(true);
        navigationFragment.setSpeedLimitIconEnabled(true);
        navigationFragment.setTripProgressBarEnabled(true);

        vehicleId = getIntent().getStringExtra(EXTRA_VEHICLE_ID);
        location = getIntent().getParcelableExtra(EXTRA_END_LOCATION);
        endLocationAddress = getIntent().getStringExtra(EXTRA_END_LOCATION_ADDRESS);
        startLocation = getIntent().getParcelableExtra(EXTRA_START_LOCATION);
        addressTextView.setText(endLocationAddress);
        String details = getIntent().getStringExtra(EXTRA_DETAIL);
        simulation = getIntent().getBooleanExtra(EXTRA_SIMULATION, true);
        locationTracking = getIntent().getBooleanExtra(EXTRA_LOCATION_TRACKING, true);
        detailTextView.setText(details);
        exitButton.setOnClickListener(
                view -> {
                    manualExit = true;
                    exit();
                });

        locationLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        this::onLocationPermissionGrant);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!permissionsAreGranted()) {
            requestLocationPermission();
        } else {
            initNavigator();
        }
    }

    private void exit() {
        setResult(arrived ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
        finish();
    }

    private void setupDeliveryVehicleReporter(Navigator navigator) {
        // getInstance() should always be null because it is cleared in onDestroy(); check here just
        // to be sure.
        if (DeliveryDriverApi.getInstance() == null) {
            Application application = getApplication();
            DriverContext driverContext =
                    DriverContext.builder(application)
                            .setProviderId(getIntent().getStringExtra(EXTRA_PROVIDER_ID))
                            .setVehicleId(NavigationActivity.this.vehicleId)
                            .setAuthTokenFactory(DeliveryDriverTokenFactory.getInstance(manager.getBackend()))
                            .setNavigator(navigator)
                            .setRoadSnappedLocationProvider(
                                    NavigationApi.getRoadSnappedLocationProvider(application))
                            .setStatusListener(
                                    (statusLevel, statusCode, statusMsg) -> {
                                        Log.d(
                                                TAG,
                                                String.format(
                                                        "VehicleReporter: %s | %s | %s",
                                                        statusLevel.name(), statusCode.name(), statusMsg));
                                    })
                            .build();
            DeliveryDriverApi.createInstance(driverContext);
        }
        vehicleReporter = DeliveryDriverApi.getInstance().getDeliveryVehicleReporter();
        if (locationTracking) {
            vehicleReporter.enableLocationTracking();
            Log.i(TAG, "Started location tracking");
        } else {
            Log.i(TAG, "Location tracking was turned off in settings");
        }
    }

    /**
     * Validates if the permissions are granted after user took action. In case permissions are
     * granted, continue with initialization flow. Otherwise, display an informative dialogue and
     * request permission again.
     */
    private void onLocationPermissionGrant(Map<String, Boolean> result) {
        if (!permissionsAreGranted()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_warning_title)
                    .setMessage(R.string.permission_warning_message)
                    .setNeutralButton(
                            R.string.permission_warning_button,
                            (dialogInterface, n) -> {
                                requestLocationPermission();
                            })
                    .create()
                    .show();
        } else {
            initNavigator();
        }
    }

    /**
     * Displays the location permission request dialog.
     */
    private void requestLocationPermission() {
        locationLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
    }

    /**
     * Verifies if location permissions are granted to the app.
     */
    private boolean permissionsAreGranted() {
        int permission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void initNavigator() {
        NavigationApi.getNavigator(
                this,
                new NavigationApi.NavigatorListener() {
                    @Override
                    public void onNavigatorReady(Navigator navigator) {
                        NavigationActivity.this.navigator = navigator;
                        setupDeliveryVehicleReporter(navigator);

                        startGuidance();

                        arrivalListener = arrivalEvent -> onArrive();
                        navigator.addArrivalListener(arrivalListener);

                        float minorSpeedAlertThresholdPercentage = 25;
                        float majorSpeedAlertThresholdPercentage = 50;
                        double severityUpgradeDurationSeconds = 30;

                        SpeedAlertOptions speedAlertOptions = new SpeedAlertOptions.Builder().setSpeedAlertThresholdPercentage(
                                        MINOR, minorSpeedAlertThresholdPercentage)
                                .setSpeedAlertThresholdPercentage(
                                        MAJOR, majorSpeedAlertThresholdPercentage)
                                .setSeverityUpgradeDurationSeconds(severityUpgradeDurationSeconds).build();

                        navigator.setSpeedAlertOptions(speedAlertOptions);

                        navigator.setSpeedingListener((percentageAboveLimit, speedAlertSeverity) -> {
                            if (speedAlertSeverity != lastSeverity) {
                                lastSeverity = speedAlertSeverity;
                                String speedingMessage;
                                if (speedAlertSeverity.equals(MAJOR)) {
                                    speedingMessage = getResources().getString(R.string.major_speeding_message);
                                } else if (speedAlertSeverity.equals(NONE)) {
                                    speedingMessage = getResources().getString(R.string.not_speeding_message);
                                } else {
                                    speedingMessage = getResources().getString(R.string.minor_speeding_message);
                                }

                                Snackbar.make(
                                                findViewById(android.R.id.content),
                                                speedingMessage,
                                                Snackbar.LENGTH_LONG)
                                        .show();

                                SampleBackend backend = new SampleBackend(NavigationActivity.this);
                                ListenableFuture<String> future = backend.postSpeeding(NavigationActivity.this.vehicleId, percentageAboveLimit, speedAlertSeverity);

                                Futures.addCallback(
                                        future,
                                        new FutureCallback<String>() {
                                            @Override
                                            public void onSuccess(String result) {
                                                logger.log(Level.INFO, result);
                                            }

                                            @Override
                                            public void onFailure(Throwable t) {
                                                String msg = "Could not update remote speeding status.";
                                                logger.log(Level.SEVERE, msg, t);
                                                throw new IllegalStateException(msg);
                                            }
                                        },
                                        MoreExecutors.directExecutor());
                            }
                        });
                    }

                    @Override
                    public void onError(int errorCode) {
                        Log.e(TAG, String.format("Error loading Navigator instance: %s", errorCode));
                    }
                });
    }

    private void startGuidance() {
        ListenableResultFuture<Navigator.RouteStatus> pendingRoute =
                NavigationActivity.this.navigator.setDestination(
                        Waypoint.builder()
                                .setLatLng(location.latitude, location.longitude)
                                .setTitle(endLocationAddress)
                                .build());
        pendingRoute.setOnResultListener(
                routeStatus -> {
                    if (routeStatus != RouteStatus.OK) {
                        String msg = String.format("RouteStatus is %s, not starting nav.", routeStatus);
                        Log.e(TAG, msg);
                        showToast(msg);
                        exit();
                    }
                    if (simulation) {
                        if (startLocation != null) {
                            Log.i(
                                    TAG,
                                    String.format(
                                            "Nav start location is %f, %f",
                                            startLocation.latitude, startLocation.longitude));
                            NavigationActivity.this.navigator.getSimulator().setUserLocation(startLocation);
                        }
                        NavigationActivity.this.navigator
                                .getSimulator()
                                .simulateLocationsAlongExistingRoute(
                                        new SimulationOptions().speedMultiplier(1.5f));
                    }
                    NavigationActivity.this.navigator.startGuidance();
                });
    }

    private void onArrive() {
        if (!arrived) {
            arrived = true;

            Snackbar.make(
                            findViewById(android.R.id.content),
                            getResources().getString(R.string.arrival_message),
                            Snackbar.LENGTH_LONG)
                    .show();

            new android.os.Handler(Looper.getMainLooper()).postDelayed(
                    () -> {
                        if (!manualExit) {
                            new MaterialAlertDialogBuilder(NavigationActivity.this)
                                    .setTitle(NavigationActivity.this.getString(R.string.on_arrive_popup_title))
                                    .setMessage(NavigationActivity.this.getString(R.string.on_arrive_popup_body))
                                    .setPositiveButton(NavigationActivity.this.getString(R.string.successful_and_continue), (dialogInterface, i) -> completeAndContinue(true))
                                    .setNegativeButton(NavigationActivity.this.getString(R.string.unsuccessful_and_continue), (dialogInterface, i) -> completeAndContinue(false))
                                    .setNeutralButton(NavigationActivity.this.getString(R.string.stop_nav), (dialogInterface, i) -> exit())
                                    .show();
                        }
                    },
                    1500);
        }
    }

    private void completeAndContinue(boolean successful) {
        DeliveryManager deliveryManager = DeliveryManager.getInstance();
        ImmutableList<AppVehicleStop> vehicleStops = deliveryManager.getStops().getValue();
        AppVehicleStop vehicleStop = null;
        for (AppVehicleStop vs : vehicleStops) {
            if (vs.getWaypoint().getPosition().equals(location)) {
                vehicleStop = vs;
                break;
            }
        }

        deliveryManager.arrivedAtStop();

        if (vehicleStop != null) {
            final AppVehicleStop lastVehicleStop = vehicleStop;
            List<Task> tasks = vehicleStop.getTasks();
            deliveryManager.updateTaskOutcome(tasks, successful, (failedTaskIds, throwableX) -> {
                // Just report the number of failed task outcomes for now.
                if (failedTaskIds != null && failedTaskIds.isEmpty()) {
                    String msg = NavigationActivity.this.getString(R.string.task_outcomes_update_done);
                    logger.log(Level.INFO, msg);
                    showToast(msg);
                } else {
                    String msg = NavigationActivity.this.getResources().getQuantityString(R.plurals.task_outcome_update_failed, failedTaskIds.size(),
                            failedTaskIds.size());
                    logger.log(Level.SEVERE, msg, throwableX);
                    showToast(msg);
                }

                deliveryManager.refreshStopsList(throwable -> {
                    if (throwable == null) {
                        showToast(NavigationActivity.this.getString(R.string.stops_list_update_done));
                    } else {
                        logger.log(Level.SEVERE, throwable.getMessage(), throwable);
                        showToast(NavigationActivity.this.getString(R.string.stops_list_update_fail));
                    }
                });
            });
            showToast(getResources()
                    .getQuantityString(R.plurals.task_outcome_updates_sent, tasks.size(), tasks.size()));

            try {
                stopIndex += 1;
                if (stopIndex < vehicleStops.size()) {
                    AppVehicleStop nextStop = vehicleStops.get(stopIndex);
                    location = nextStop.getWaypoint().getPosition();

                    endLocationAddress = nextStop.getWaypoint().getTitle();
                    addressTextView.setText(endLocationAddress);

                    String details = ItineraryListUtils.getTasksCountString(nextStop.getTasks(), NavigationActivity.this);
                    detailTextView.setText(details);

                    // NavigationActivity.this.navigator.getSimulator().unsetUserLocation();
                    startLocation = lastVehicleStop.getWaypoint().getPosition();
                    arrived = false;
                    lastSeverity = NONE;

                    startGuidance();
                    deliveryManager.enrouteToStop();
                } else {
                    showToast(NavigationActivity.this.getString(R.string.final_destination));
                    new android.os.Handler(Looper.getMainLooper()).postDelayed(
                            () -> {
                                if (!manualExit) {
                                    exit();
                                }
                            },
                            5000);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                showToast(NavigationActivity.this.getString(R.string.final_destination));
                exit();
            }
        }
    }

    public void showToast(String message) {
        Snackbar.make(findViewById(android.R.id.content),
                        message,
                        Snackbar.LENGTH_LONG)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        googleMap.clear();
        vehicleReporter.disableLocationTracking();
        DeliveryDriverApi.clearInstance();
        navigator.stopGuidance();
        navigator.getSimulator().unsetUserLocation();
        navigator.clearDestinations();
        navigator.removeArrivalListener(arrivalListener);
        navigator.cleanup();
    }
}
