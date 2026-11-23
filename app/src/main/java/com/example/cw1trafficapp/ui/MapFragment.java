
package com.example.cw1trafficapp.ui;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cw1trafficapp.MainActivity;
import com.example.cw1trafficapp.R;
import com.example.cw1trafficapp.model.TrafficCoordinates;
import com.example.cw1trafficapp.model.TrafficSituation;
import com.example.cw1trafficapp.viewmodel.TrafficViewModel;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Traffic Scotland interactive map.
 *
 * Supports:
 * - All incidents and roadworks
 * - Multiple matching filtered situations
 * - Single selected situation
 * - Six different situation category icons
 * - Marker interaction and details navigation
 * - Road segment lines
 * - Automatic camera positioning
 *
 * Shares state through the activity-scoped TrafficViewModel.
 */
public class MapFragment extends Fragment
        implements OnMapReadyCallback {

    private static final String TAG = "TRAFFIC_MAP";

    private TrafficViewModel viewModel;

    private MapView mapView;
    private GoogleMap googleMap;

    private TextView txtMapStatus;
    private LinearLayout layoutMapFallback;
    private TextView txtFallbackDetails;

    private Button btnMapAll;
    private Button btnMapFiltered;
    private Button btnMapSelected;

    /**
     * Cache the six marker bitmaps instead of creating
     * a new bitmap for every traffic situation.
     */
    private final Map<TrafficSituation.IconCategory, BitmapDescriptor>
            markerIconCache = new EnumMap<>(
            TrafficSituation.IconCategory.class
    );

    /**
     * Prevent delayed camera operations from applying
     * after the displayed map data has changed.
     */
    private int cameraGeneration = 0;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_map,
                container,
                false
        );

        mapView = view.findViewById(R.id.mapView);

        txtMapStatus =
                view.findViewById(R.id.txtMapStatus);

        layoutMapFallback =
                view.findViewById(R.id.layoutMapFallback);

        txtFallbackDetails =
                view.findViewById(R.id.txtFallbackDetails);

        btnMapAll =
                view.findViewById(R.id.btnMapAll);

        btnMapFiltered =
                view.findViewById(R.id.btnMapFiltered);

        btnMapSelected =
                view.findViewById(R.id.btnMapSelected);

        mapView.onCreate(savedInstanceState);

        return view;
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(
                requireActivity()
        ).get(TrafficViewModel.class);

        setupMapModeButtons();

        try {

            mapView.getMapAsync(this);

        } catch (Exception exception) {

            Log.e(
                    TAG,
                    "Unable to initialise Google Maps",
                    exception
            );

            showFallback(
                    "The map could not be loaded. "
                            + "Please check your connection."
            );
        }
    }

    /**
     * Set up the three map display modes.
     */
    private void setupMapModeButtons() {

        btnMapAll.setOnClickListener(v -> {

            viewModel.setMapMode(
                    TrafficViewModel.MapMode.ALL
            );
        });

        btnMapFiltered.setOnClickListener(v -> {

            viewModel.setMapMode(
                    TrafficViewModel.MapMode.FILTERED
            );
        });

        btnMapSelected.setOnClickListener(v -> {

            if (viewModel.getSelectedSituationLiveData()
                    .getValue() != null) {

                viewModel.setMapMode(
                        TrafficViewModel.MapMode.SELECTED
                );
            }
        });
    }

    /**
     * Called when the Google Map is ready.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        if (!isAdded() || mapView == null
                || viewModel == null) {
            return;
        }

        googleMap = map;

        googleMap.getUiSettings()
                .setZoomControlsEnabled(true);

        googleMap.getUiSettings()
                .setCompassEnabled(true);

        googleMap.getUiSettings()
                .setMapToolbarEnabled(false);

        googleMap.setMapType(
                GoogleMap.MAP_TYPE_NORMAL
        );

        // Show a marker's information window.
        googleMap.setOnMarkerClickListener(marker -> {

            marker.showInfoWindow();

            return true;
        });

        // Open the selected situation's details.
        googleMap.setOnInfoWindowClickListener(marker -> {

            Object tag = marker.getTag();

            if (tag instanceof TrafficSituation) {

                TrafficSituation situation =
                        (TrafficSituation) tag;

                viewModel.setSelectedSituation(situation);

                if (getActivity() instanceof MainActivity) {

                    ((MainActivity) requireActivity())
                            .navigateToFragment(
                                    new DetailsFragment()
                            );
                }
            }
        });

        // Default location: central Glasgow.
        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        getGlasgowPosition(),
                        9f
                )
        );

        Log.d(TAG, "Google Maps initialised");

        observeViewModel();
    }

    /**
     * Observe live traffic, filtered records,
     * selection and map mode changes.
     */
    private void observeViewModel() {

        viewModel.getIncidentsLiveData().observe(
                getViewLifecycleOwner(),
                incidents -> updateMarkers()
        );

        viewModel.getRoadworksLiveData().observe(
                getViewLifecycleOwner(),
                roadworks -> updateMarkers()
        );

        viewModel.getFilteredIncidentsLiveData().observe(
                getViewLifecycleOwner(),
                incidents -> updateMarkers()
        );

        viewModel.getFilteredRoadworksLiveData().observe(
                getViewLifecycleOwner(),
                roadworks -> updateMarkers()
        );

        viewModel.getSelectedSituationLiveData().observe(
                getViewLifecycleOwner(),
                situation -> updateMarkers()
        );

        viewModel.getMapModeLiveData().observe(
                getViewLifecycleOwner(),
                mode -> updateMarkers()
        );
    }

    /**
     * Refresh the map for the selected display mode.
     */
    private void updateMarkers() {

        if (googleMap == null
                || viewModel == null
                || mapView == null) {
            return;
        }

        cameraGeneration++;

        int currentGeneration = cameraGeneration;

        googleMap.clear();

        TrafficViewModel.MapMode mode =
                viewModel.getMapModeLiveData().getValue();

        if (mode == null) {
            mode = TrafficViewModel.MapMode.ALL;
        }

        TrafficSituation selected =
                viewModel.getSelectedSituationLiveData()
                        .getValue();

        btnMapSelected.setEnabled(selected != null);

        updateScopeButtons(mode);

        List<TrafficSituation> situations =
                getSituationsForMode(mode, selected);

        java.util.Map<TrafficSituation.IconCategory, Integer> counts =
                new java.util.EnumMap<>(
                        TrafficSituation.IconCategory.class
                );

        for (TrafficSituation situation : situations) {
            if (situation != null) {
                TrafficSituation.IconCategory category =
                        situation.getIconCategory();

                counts.merge(category, 1, Integer::sum);
            }
        }

        Log.d(TAG, "LIVE MAP ICON CATEGORIES: " + counts);

        LatLngBounds.Builder bounds =
                new LatLngBounds.Builder();

        LatLng firstPosition = null;

        int mappedCount = 0;
        int skippedCount = 0;

        for (TrafficSituation situation : situations) {

            if (situation == null) {
                continue;
            }

            TrafficCoordinates coordinates =
                    situation.getPrimaryCoordinates();

            if (coordinates == null
                    || !coordinates.isValid()) {

                skippedCount++;
                continue;
            }

            LatLng position = new LatLng(
                    coordinates.getLatitude(),
                    coordinates.getLongitude()
            );

            // Use the actual situation icon rather
            // than a generic coloured Google Maps pin.
            BitmapDescriptor markerIcon =
                    getSituationMarkerIcon(situation);

            String roadName = situation.getRoadName();

            if (roadName == null
                    || roadName.trim().isEmpty()) {
                roadName = "Unknown road";
            }

            String categoryLabel = situation.isUnplanned()
                    ? "Incident"
                    : "Roadworks";

            String description =
                    situation.getPublicComment();

            if (description == null
                    || description.trim().isEmpty()) {
                description = "Tap for more details";
            }

            MarkerOptions options =
                    new MarkerOptions()
                            .position(position)
                            .title(
                                    roadName + " — " + categoryLabel
                            )
                            .snippet(description)
                            .icon(markerIcon)
                            .anchor(0.5f, 0.5f);

            Marker marker =
                    googleMap.addMarker(options);

            if (marker != null) {
                marker.setTag(situation);
            }

            bounds.include(position);

            if (firstPosition == null) {
                firstPosition = position;
            }

            mappedCount++;

            drawSegmentLine(situation);
        }

        updateMapStatus(
                mode,
                mappedCount,
                skippedCount
        );

        updateCamera(
                bounds,
                firstPosition,
                mappedCount,
                currentGeneration
        );

        Log.d(
                TAG,
                "Map updated. Mode=" + mode
                        + ", markers=" + mappedCount
                        + ", skipped=" + skippedCount
        );
    }

    /**
     * Choose which situations should appear on the map.
     */
    private List<TrafficSituation> getSituationsForMode(
            TrafficViewModel.MapMode mode,
            TrafficSituation selected) {

        List<TrafficSituation> result =
                new ArrayList<>();

        // Display only one selected record.
        if (mode == TrafficViewModel.MapMode.SELECTED) {

            if (selected != null) {
                result.add(selected);
            }

            return result;
        }

        List<TrafficSituation> incidents;
        List<TrafficSituation> roadworks;

        if (mode == TrafficViewModel.MapMode.FILTERED) {

            incidents = viewModel
                    .getFilteredIncidentsLiveData()
                    .getValue();

            roadworks = viewModel
                    .getFilteredRoadworksLiveData()
                    .getValue();

        } else {

            incidents = viewModel
                    .getIncidentsLiveData()
                    .getValue();

            roadworks = viewModel
                    .getRoadworksLiveData()
                    .getValue();
        }

        if (incidents != null) {
            result.addAll(incidents);
        }

        if (roadworks != null) {
            result.addAll(roadworks);
        }

        return result;
    }

    /**
     * Convert a vector drawable into a Google Maps icon.
     *
     * Each marker receives a white circular background
     * containing its situation-specific vector graphic.
     */
    private BitmapDescriptor getSituationMarkerIcon(
            TrafficSituation situation) {

        TrafficSituation.IconCategory category =
                situation.getIconCategory();

        BitmapDescriptor cached =
                markerIconCache.get(category);

        if (cached != null) {
            return cached;
        }

        int drawableResource =
                getDrawableForCategory(category);

        Drawable drawable =
                AppCompatResources.getDrawable(
                        requireContext(),
                        drawableResource
                );

        if (drawable == null) {

            Log.w(
                    TAG,
                    "Unable to load marker drawable: "
                            + category
            );

            return BitmapDescriptorFactory.defaultMarker();
        }

        float density = getResources()
                .getDisplayMetrics().density;

        int size = Math.max(
                1,
                Math.round(34f * density)
        );

        int padding = Math.round(7f * density);



        Bitmap bitmap = Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);

        float centre = size / 2f;
        float radius = centre - density;

        // Marker background.
        Paint background =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        background.setColor(Color.WHITE);
        background.setStyle(Paint.Style.FILL);

        canvas.drawCircle(
                centre,
                centre,
                radius,
                background
        );

        // Subtle outline for contrast against map tiles.
        Paint outline =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        outline.setColor(
                Color.rgb(190, 201, 213)
        );

        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(1.5f * density);

        canvas.drawCircle(
                centre,
                centre,
                radius,
                outline
        );

        drawable.setBounds(
                padding,
                padding,
                size - padding,
                size - padding
        );

        drawable.draw(canvas);

        BitmapDescriptor descriptor =
                BitmapDescriptorFactory.fromBitmap(
                        bitmap
                );

        markerIconCache.put(
                category,
                descriptor
        );

        return descriptor;
    }

    /**
     * Match every category to its corresponding
     * icon in res/drawable.
     */
    private int getDrawableForCategory(
            TrafficSituation.IconCategory category) {

        switch (category) {

            case ROAD_CLOSED:
                return R.drawable.ic_road_closed;

            case LANE_RESTRICTION:
                return R.drawable.ic_lane_restriction;

            case QUEUING_TRAFFIC:
                return R.drawable.ic_queuing_traffic;

            case ABNORMAL_TRAFFIC:
                return R.drawable.ic_abnormal_traffic;

            case MAINTENANCE_ROADWORKS:
                return R.drawable.ic_roadworks;

            case OTHER_INCIDENT:
            default:
                return R.drawable.ic_incident_other;
        }
    }

    /**
     * Add a line representing the available
     * from/to coordinates of a situation.
     *
     * This is a straight-line approximation,
     * not an exact road geometry.
     */
    private void drawSegmentLine(
            TrafficSituation situation) {

        if (googleMap == null || situation == null) {
            return;
        }

        if (situation.getFromLocation() == null
                || situation.getToLocation() == null) {
            return;
        }

        TrafficCoordinates fromCoords =
                situation.getFromLocation()
                        .getCoordinates();

        TrafficCoordinates toCoords =
                situation.getToLocation()
                        .getCoordinates();

        if (fromCoords == null || toCoords == null
                || !fromCoords.isValid()
                || !toCoords.isValid()) {
            return;
        }

        LatLng from = new LatLng(
                fromCoords.getLatitude(),
                fromCoords.getLongitude()
        );

        LatLng to = new LatLng(
                toCoords.getLatitude(),
                toCoords.getLongitude()
        );

        int lineColour = situation.isUnplanned()
                ? Color.rgb(220, 38, 38)
                : Color.rgb(0, 94, 184);

        googleMap.addPolyline(
                new PolylineOptions()
                        .add(from, to)
                        .width(4f)
                        .color(lineColour)
                        .geodesic(true)
        );
    }

    /**
     * Set the map camera to show the plotted positions.
     */
    private void updateCamera(
            LatLngBounds.Builder bounds,
            LatLng firstPosition,
            int count,
            int generation) {

        if (googleMap == null || mapView == null) {
            return;
        }

        if (count == 0) {

            // No markers: return to the Glasgow overview.
            googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            getGlasgowPosition(),
                            9f
                    )
            );

            return;
        }

        if (count == 1 && firstPosition != null) {

            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            firstPosition,
                            12f
                    )
            );

            return;
        }

        // Multiple markers: fit all positions in view.
        final LatLngBounds targetBounds;

        try {
            targetBounds = bounds.build();

        } catch (IllegalStateException exception) {
            return;
        }

        final MapView activeMapView = mapView;

        activeMapView.post(() -> {

            // Ignore outdated camera updates and
            // operations after the view is destroyed.
            if (generation != cameraGeneration
                    || googleMap == null
                    || mapView != activeMapView
                    || !isAdded()) {
                return;
            }

            int width = activeMapView.getWidth();
            int height = activeMapView.getHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            int padding = Math.min(
                    Math.round(
                            64f * getResources()
                                    .getDisplayMetrics().density
                    ),
                    Math.min(width, height) / 4
            );

            try {

                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                                targetBounds,
                                width,
                                height,
                                padding
                        )
                );

            } catch (IllegalStateException exception) {

                Log.w(
                        TAG,
                        "Map camera could not update",
                        exception
                );
            }
        });
    }

    private LatLng getGlasgowPosition() {

        return new LatLng(
                TrafficSituation.GLASGOW_LAT,
                TrafficSituation.GLASGOW_LNG
        );
    }

    /**
     * Show the current map scope and marker count.
     */
    private void updateMapStatus(
            TrafficViewModel.MapMode mode,
            int mappedCount,
            int skippedCount) {

        if (mappedCount == 0) {

            txtMapStatus.setText(
                    "No mapped situations match this selection"
            );

            return;
        }

        String title;

        if (mode == TrafficViewModel.MapMode.SELECTED) {

            title = "Selected situation";

        } else if (mode == TrafficViewModel.MapMode.FILTERED) {

            title = "Matching situations";

        } else {

            title = "All traffic situations";
        }

        if (mode == TrafficViewModel.MapMode.FILTERED) {

            String query = viewModel
                    .getSearchQueryLiveData()
                    .getValue();

            if (query != null && !query.trim().isEmpty()) {
                title += " (" + query.trim() + ")";
            }
        }

        String status = String.format(
                Locale.UK,
                "%s: %d mapped",
                title,
                mappedCount
        );

        if (skippedCount > 0) {

            status += String.format(
                    Locale.UK,
                    " • %d without coordinates",
                    skippedCount
            );
        }

        txtMapStatus.setText(status);
    }

    /**
     * Visually distinguish the selected map mode.
     */
    private void updateScopeButtons(
            TrafficViewModel.MapMode mode) {

        Button[] buttons = {
                btnMapAll,
                btnMapFiltered,
                btnMapSelected
        };

        TrafficViewModel.MapMode[] modes = {
                TrafficViewModel.MapMode.ALL,
                TrafficViewModel.MapMode.FILTERED,
                TrafficViewModel.MapMode.SELECTED
        };

        int activeBackground =
                ContextCompat.getColor(
                        requireContext(),
                        R.color.primary_navy
                );

        int inactiveBackground =
                ContextCompat.getColor(
                        requireContext(),
                        R.color.accent_blue_light
                );

        int activeText =
                ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                );

        int inactiveText =
                ContextCompat.getColor(
                        requireContext(),
                        R.color.primary_navy
                );

        for (int i = 0; i < buttons.length; i++) {

            boolean active = mode == modes[i];

            buttons[i].setSelected(active);

            buttons[i].setBackgroundTintList(
                    ColorStateList.valueOf(
                            active
                                    ? activeBackground
                                    : inactiveBackground
                    )
            );

            buttons[i].setTextColor(
                    active ? activeText : inactiveText
            );
        }
    }

    /**
     * Display a message if the map cannot initialise.
     */
    private void showFallback(String message) {

        if (mapView != null) {
            mapView.setVisibility(View.GONE);
        }

        if (layoutMapFallback != null) {

            layoutMapFallback.setVisibility(View.VISIBLE);
        }

        if (txtFallbackDetails != null) {

            txtFallbackDetails.setText(message);
        }

        if (btnMapAll != null) {
            btnMapAll.setEnabled(false);
        }

        if (btnMapFiltered != null) {
            btnMapFiltered.setEnabled(false);
        }

        if (btnMapSelected != null) {
            btnMapSelected.setEnabled(false);
        }
    }

    // ==========================================
    // MAPVIEW LIFECYCLE
    // ==========================================

    @Override
    public void onStart() {

        super.onStart();

        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onResume() {

        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {

        if (mapView != null) {
            mapView.onPause();
        }

        super.onPause();
    }

    @Override
    public void onStop() {

        if (mapView != null) {
            mapView.onStop();
        }

        super.onStop();
    }

    @Override
    public void onDestroyView() {

        cameraGeneration++;

        if (mapView != null) {
            mapView.onDestroy();
        }

        googleMap = null;
        mapView = null;

        markerIconCache.clear();

        btnMapAll = null;
        btnMapFiltered = null;
        btnMapSelected = null;

        txtMapStatus = null;
        layoutMapFallback = null;
        txtFallbackDetails = null;

        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {

        super.onLowMemory();

        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(
            @NonNull Bundle outState) {

        super.onSaveInstanceState(outState);

        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }
}
