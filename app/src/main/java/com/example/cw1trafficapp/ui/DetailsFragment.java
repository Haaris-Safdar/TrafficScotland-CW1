
package com.example.cw1trafficapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cw1trafficapp.MainActivity;
import com.example.cw1trafficapp.R;
import com.example.cw1trafficapp.model.TrafficCoordinates;
import com.example.cw1trafficapp.model.TrafficSituation;
import com.example.cw1trafficapp.viewmodel.TrafficViewModel;

import java.util.Locale;

/**
 * Displays detailed information about a selected traffic situation.
 *
 * Uses data extracted from Traffic Scotland DATEX II XML.
 * Shows dates, locations, coordinates, validity, traffic impact
 * and provides map and sharing actions.
 */
public class DetailsFragment extends Fragment {

    private TrafficViewModel viewModel;

    private ImageView imgDetailsIcon;

    private TextView txtDetailsRoadName;
    private TextView txtDetailsBadge;
    private TextView txtDetailsReference;
    private TextView txtDetailsPublicComment;

    private TextView txtDetailsStartTime;
    private TextView txtDetailsEndTime;
    private TextView txtDetailsDuration;

    private TextView txtDetailsFromLocation;
    private TextView txtDetailsToLocation;
    private TextView txtDetailsCoordinates;
    private TextView txtDetailsGlasgowDistance;
    private TextView txtDetailsImpact;

    private Button btnViewOnMap;
    private Button btnShare;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_details,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity())
                .get(TrafficViewModel.class);

        initialiseViews(view);
        observeSelectedSituation();
    }

    /**
     * Connect UI components to their XML views.
     */
    private void initialiseViews(View view) {

        imgDetailsIcon =
                view.findViewById(R.id.imgDetailsIcon);

        txtDetailsRoadName =
                view.findViewById(R.id.txtDetailsRoadName);

        txtDetailsBadge =
                view.findViewById(R.id.txtDetailsBadge);

        txtDetailsReference =
                view.findViewById(R.id.txtDetailsReference);

        txtDetailsPublicComment =
                view.findViewById(R.id.txtDetailsPublicComment);

        txtDetailsStartTime =
                view.findViewById(R.id.txtDetailsStartTime);

        txtDetailsEndTime =
                view.findViewById(R.id.txtDetailsEndTime);

        txtDetailsDuration =
                view.findViewById(R.id.txtDetailsDuration);

        txtDetailsFromLocation =
                view.findViewById(R.id.txtDetailsFromLocation);

        txtDetailsToLocation =
                view.findViewById(R.id.txtDetailsToLocation);

        txtDetailsCoordinates =
                view.findViewById(R.id.txtDetailsCoordinates);

        txtDetailsGlasgowDistance =
                view.findViewById(R.id.txtDetailsGlasgowDistance);

        txtDetailsImpact =
                view.findViewById(R.id.txtDetailsImpact);

        btnViewOnMap =
                view.findViewById(R.id.btnViewOnMap);

        btnShare =
                view.findViewById(R.id.btnShare);
    }

    /**
     * Updates the details whenever the selected situation changes.
     */
    private void observeSelectedSituation() {

        viewModel.getSelectedSituationLiveData().observe(
                getViewLifecycleOwner(),
                this::bindSituation
        );
    }

    /**
     * Displays the selected situation.
     */
    private void bindSituation(TrafficSituation situation) {

        if (situation == null) {

            txtDetailsRoadName.setText(
                    "No situation selected"
            );

            txtDetailsPublicComment.setText(
                    "Select an incident or roadworks record "
                            + "to view its details."
            );

            btnViewOnMap.setEnabled(false);
            btnShare.setEnabled(false);

            return;
        }

        btnViewOnMap.setEnabled(true);
        btnShare.setEnabled(true);

      
        // BASIC INFORMATION
      

        txtDetailsRoadName.setText(
                situation.getRoadName()
        );

        txtDetailsReference.setText(
                String.format(
                        Locale.UK,
                        "Record Ref: %s",
                        displayValue(
                                situation.getSituationRecordId()
                        )
                )
        );

        txtDetailsPublicComment.setText(
                displayValue(situation.getPublicComment())
        );

      
        // SCHEDULE AND DURATION
      

        txtDetailsStartTime.setText(
                situation.getFormattedStartTime()
        );

        txtDetailsEndTime.setText(
                situation.getFormattedEndTime()
        );

        txtDetailsDuration.setText(
                situation.getCalculatedDuration()
        );

      
        // LOCATION INFORMATION
      

        String fromLocation = situation
                .getFromLocation()
                .getDisplayName();

        String toLocation = situation
                .getToLocation()
                .getDisplayName();

        txtDetailsFromLocation.setText(
                "From: " + displayValue(fromLocation)
        );

        txtDetailsToLocation.setText(
                "To: " + displayValue(toLocation)
        );

      
        // GPS COORDINATES
      

        TrafficCoordinates coordinates =
                situation.getPrimaryCoordinates();

        if (coordinates != null && coordinates.isValid()) {

            txtDetailsCoordinates.setText(
                    String.format(
                            Locale.UK,
                            "Coordinates: %.5f, %.5f",
                            coordinates.getLatitude(),
                            coordinates.getLongitude()
                    )
            );

        } else {

            txtDetailsCoordinates.setText(
                    "Coordinates: Not available"
            );
        }

      
        // DISTANCE FROM GLASGOW
      

        double distance =
                situation.getDistanceFromReference(
                        TrafficSituation.GLASGOW_LAT,
                        TrafficSituation.GLASGOW_LNG
                );

        if (distance >= 0
                && Double.isFinite(distance)) {

            txtDetailsGlasgowDistance.setText(
                    String.format(
                            Locale.UK,
                            "%.1f km from Glasgow City Centre",
                            distance
                    )
            );

        } else {

            txtDetailsGlasgowDistance.setText(
                    "Distance from Glasgow unavailable"
            );
        }

      
        // TRAFFIC IMPACT AND VALIDITY
      

        displayTrafficImpact(situation);

      
        // ICON AND BADGE
      

        displaySituationIcon(situation);
        displaySituationBadge(situation);

      
        // ACTION BUTTONS
      

        setupActionButtons(situation);
    }

    /**
     * Builds a readable summary using fields parsed
     * from the DATEX II XML.
     */
    private void displayTrafficImpact(
            TrafficSituation situation) {

        StringBuilder impact = new StringBuilder();

        appendInfo(
                impact,
                "Situation category",
                situation.isUnplanned()
                        ? "Unplanned incident"
                        : "Roadworks"
        );

        appendInfo(
                impact,
                "Record type",
                situation.getSituationRecordType()
        );

        appendInfo(
                impact,
                "Subtype",
                situation.getSubtype()
        );

        appendInfo(
                impact,
                "Validity",
                situation.getValidityStatus()
        );

        appendInfo(
                impact,
                "Traffic delay category",
                situation.getDelaysType()
        );

        appendInfo(
                impact,
                "Direction",
                situation.getDirection()
        );

        if (situation.getNumberOfOperationalLanes() >= 0) {

            appendInfo(
                    impact,
                    "Operational lanes",
                    String.valueOf(
                            situation.getNumberOfOperationalLanes()
                    )
            );
        }

        if (situation.getNumberOfLanesRestricted() >= 0) {

            appendInfo(
                    impact,
                    "Restricted lanes",
                    String.valueOf(
                            situation.getNumberOfLanesRestricted()
                    )
            );
        }

        appendInfo(
                impact,
                "Record version",
                situation.getVersion()
        );

        if (impact.length() == 0) {

            txtDetailsImpact.setText(
                    "Additional traffic impact details "
                            + "were not provided by the feed."
            );

        } else {

            txtDetailsImpact.setText(
                    impact.toString().trim()
            );
        }
    }

    /**
     * Adds a field only when its value is available.
     */
    private void appendInfo(
            StringBuilder result,
            String label,
            String value) {

        if (value == null || value.trim().isEmpty()) {
            return;
        }

        result.append(label)
                .append(": ")
                .append(formatDatexValue(value))
                .append("\n");
    }

    /**
     * Translates technical DATEX II values into
     * labels understandable by general users.
     *
     * The underlying XML/model data is unchanged.
     */
    private String formatDatexValue(String rawValue) {

        if (rawValue == null) {
            return "Not available";
        }

        String value = rawValue.trim();

        switch (value) {

            case "MaintenanceWorks":
                return "Maintenance works";

            case "ConstructionWorks":
                return "Construction works";

            case "RoadOrCarriagewayOrLaneManagement":
                return "Road or lane management";

            case "AbnormalTraffic":
                return "Abnormal traffic";

            case "roadworks":
                return "Roadworks";

            case "roadClosed":
                return "Road closed";

            case "carriagewayClosed":
                return "Carriageway closed";

            case "laneClosed":
                return "Lane closed";

            case "definedByValidityTimeSpec":
                return "Defined by scheduled dates";

            case "active":
                return "Active";

            case "suspended":
                return "Suspended";

            case "longDelays":
                return "Long delays";

            case "veryLongDelays":
                return "Very long delays";

            case "delays":
                return "Delays reported";

            case "noDelays":
                return "No reported delays";

            case "westBound":
                return "Westbound";

            case "eastBound":
                return "Eastbound";

            case "northBound":
                return "Northbound";

            case "southBound":
                return "Southbound";

            case "bothDirections":
                return "Both directions";

            case "queuingTraffic":
                return "Queuing traffic";

            case "slowTraffic":
                return "Slow-moving traffic";

            default:
                return value;
        }
    }

    /**
     * Provides a fallback for missing text fields.
     */
    private String displayValue(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Not available";
        }

        return value.trim();
    }

    /**
     * Displays the appropriate traffic situation icon.
     */
    private void displaySituationIcon(
            TrafficSituation situation) {

        switch (situation.getIconCategory()) {

            case ROAD_CLOSED:

                imgDetailsIcon.setImageResource(
                        R.drawable.ic_road_closed
                );

                break;

            case LANE_RESTRICTION:

                imgDetailsIcon.setImageResource(
                        R.drawable.ic_lane_restriction
                );

                break;

            case QUEUING_TRAFFIC:

                imgDetailsIcon.setImageResource(
                        R.drawable.ic_queuing_traffic
                );

                break;

            case ABNORMAL_TRAFFIC:

                imgDetailsIcon.setImageResource(
                        R.drawable.ic_abnormal_traffic
                );

                break;

            case MAINTENANCE_ROADWORKS:

                imgDetailsIcon.setImageResource(
                        R.drawable.ic_roadworks
                );

                break;

            default:

                imgDetailsIcon.setImageResource(
                        R.drawable.ic_incident_other
                );

                break;
        }
    }

    /**
     * Shows colour-coded traffic delay information.
     */
    private void displaySituationBadge(
            TrafficSituation situation) {

        if (situation.isUnplanned()) {

            txtDetailsBadge.setText(
                    "Unplanned Incident"
            );

            txtDetailsBadge.setBackgroundResource(
                    R.drawable.bg_badge_unplanned
            );

            txtDetailsBadge.setTextColor(
                    ContextCompat.getColor(
                            requireContext(),
                            R.color.severity_road_closed
                    )
            );

            return;
        }

        String delayType = situation.getDelaysType();

        if ("veryLongDelays".equalsIgnoreCase(delayType)) {

            setBadge(
                    "Very Long Delays",
                    R.drawable.bg_badge_very_long_delays,
                    R.color.severity_very_long_delays
            );

        } else if ("longDelays".equalsIgnoreCase(delayType)) {

            setBadge(
                    "Long Delays",
                    R.drawable.bg_badge_roadworks,
                    R.color.severity_long_delays
            );

        } else if ("delays".equalsIgnoreCase(delayType)) {

            setBadge(
                    "Delays",
                    R.drawable.bg_badge_delays,
                    R.color.severity_delays
            );

        } else {

            setBadge(
                    "Roadworks",
                    R.drawable.bg_badge_delays,
                    R.color.primary_navy
            );
        }
    }

    /**
     * Common badge styling method.
     */
    private void setBadge(
            String text,
            int backgroundResource,
            int textColourResource) {

        txtDetailsBadge.setText(text);

        txtDetailsBadge.setBackgroundResource(
                backgroundResource
        );

        txtDetailsBadge.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        textColourResource
                )
        );
    }

    /**
     * Handles navigation to Google Maps and sharing.
     */
    private void setupActionButtons(
            TrafficSituation situation) {

        btnViewOnMap.setOnClickListener(v -> {

            viewModel.setSelectedSituation(situation);

            viewModel.setMapMode(
                    TrafficViewModel.MapMode.SELECTED
            );

            if (requireActivity() instanceof MainActivity) {

                ((MainActivity) requireActivity())
                        .selectTab(R.id.nav_map);
            }
        });

        btnShare.setOnClickListener(
                v -> shareSituation(situation)
        );
    }

    /**
     * Opens the Android share sheet.
     */
    private void shareSituation(
            TrafficSituation situation) {

        String shareBody = String.format(
                Locale.UK,
                "Traffic Scotland Update\n\n"
                        + "Road: %s\n"
                        + "Type: %s\n"
                        + "Details: %s\n"
                        + "Start: %s\n"
                        + "End: %s\n"
                        + "Duration: %s\n"
                        + "Reference: %s",
                situation.getRoadName(),
                situation.isUnplanned()
                        ? "Unplanned incident"
                        : "Roadworks",
                situation.getPublicComment(),
                situation.getFormattedStartTime(),
                situation.getFormattedEndTime(),
                situation.getCalculatedDuration(),
                situation.getSituationRecordId()
        );

        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");

        intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Traffic Scotland - "
                        + situation.getRoadName()
        );

        intent.putExtra(
                Intent.EXTRA_TEXT,
                shareBody
        );

        startActivity(
                Intent.createChooser(
                        intent,
                        "Share Traffic Update"
                )
        );
    }
}
