package com.example.cw1trafficapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cw1trafficapp.R;
import com.example.cw1trafficapp.model.TrafficSituation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrafficAdapter
        extends RecyclerView.Adapter<TrafficAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(TrafficSituation situation);
    }

    private List<TrafficSituation> situations =
            new ArrayList<>();

    private OnItemClickListener listener;

    public TrafficAdapter() {
    }

    public void setSituations(
            List<TrafficSituation> newSituations) {

        situations = newSituations != null
                ? newSituations
                : new ArrayList<>();

        notifyDataSetChanged();
    }

    public void setOnItemClickListener(
            OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.item_traffic_situation,
                parent,
                false
        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        holder.bind(
                situations.get(position),
                listener
        );
    }

    @Override
    public int getItemCount() {
        return situations.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        private final ImageView imgCategoryIcon;
        private final TextView txtRoadName;
        private final TextView txtBadgeType;
        private final TextView txtDescription;
        private final TextView txtLocation;
        private final TextView txtDistance;
        private final TextView txtTimeInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgCategoryIcon =
                    itemView.findViewById(R.id.imgCategoryIcon);

            txtRoadName =
                    itemView.findViewById(R.id.txtRoadName);

            txtBadgeType =
                    itemView.findViewById(R.id.txtBadgeType);

            txtDescription =
                    itemView.findViewById(R.id.txtDescription);

            txtLocation =
                    itemView.findViewById(R.id.txtLocation);

            txtDistance =
                    itemView.findViewById(R.id.txtDistance);

            txtTimeInfo =
                    itemView.findViewById(R.id.txtTimeInfo);
        }

        public void bind(
                TrafficSituation situation,
                OnItemClickListener listener) {

            Context context = itemView.getContext();

            txtRoadName.setText(
                    situation.getRoadName()
            );

            txtDescription.setText(
                    situation.getPublicComment()
            );

            String fromLoc = situation
                    .getFromLocation().getDisplayName();

            String toLoc = situation
                    .getToLocation().getDisplayName();

            if (!fromLoc.isEmpty()
                    && !toLoc.isEmpty()
                    && !fromLoc.equalsIgnoreCase(toLoc)) {

                txtLocation.setText(
                        fromLoc + " to " + toLoc
                );

            } else if (!fromLoc.isEmpty()) {

                txtLocation.setText(fromLoc);

            } else if (!toLoc.isEmpty()) {

                txtLocation.setText(toLoc);

            } else {

                txtLocation.setText(
                        "Location details in description"
                );
            }

            double distance =
                    situation.getDistanceFromReference(
                            TrafficSituation.GLASGOW_LAT,
                            TrafficSituation.GLASGOW_LNG
                    );

            if (distance >= 0) {

                txtDistance.setText(
                        String.format(
                                Locale.UK,
                                "%.1f km from Glasgow",
                                distance
                        )
                );

            } else {

                txtDistance.setText("Location N/A");
            }

            txtTimeInfo.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.text_secondary
                    )
            );

            if (situation.isUnplanned()) {

                txtTimeInfo.setText(
                        situation.getFormattedStartTime()
                );

            } else {

                long hours = situation.getDurationHours();
                String duration =
                        situation.getCalculatedDuration();

                if (hours >= 24) {

                    txtTimeInfo.setText(
                            "Long-duration works • " + duration
                    );

                    txtTimeInfo.setTextColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.severity_long_delays
                            )
                    );

                } else if (hours >= 0) {

                    txtTimeInfo.setText(
                            "Short-duration works • " + duration
                    );

                    txtTimeInfo.setTextColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.secondary_blue
                            )
                    );

                } else {

                    txtTimeInfo.setText(duration);
                }
            }

            switch (situation.getIconCategory()) {

                case ROAD_CLOSED:
                    imgCategoryIcon.setImageResource(
                            R.drawable.ic_road_closed
                    );
                    break;

                case LANE_RESTRICTION:
                    imgCategoryIcon.setImageResource(
                            R.drawable.ic_lane_restriction
                    );
                    break;

                case QUEUING_TRAFFIC:
                    imgCategoryIcon.setImageResource(
                            R.drawable.ic_queuing_traffic
                    );
                    break;

                case ABNORMAL_TRAFFIC:
                    imgCategoryIcon.setImageResource(
                            R.drawable.ic_abnormal_traffic
                    );
                    break;

                case MAINTENANCE_ROADWORKS:
                    imgCategoryIcon.setImageResource(
                            R.drawable.ic_roadworks
                    );
                    break;

                default:
                    imgCategoryIcon.setImageResource(
                            R.drawable.ic_incident_other
                    );
                    break;
            }

            if (situation.isUnplanned()) {

                txtBadgeType.setText(
                        "Unplanned Incident"
                );

                txtBadgeType.setBackgroundResource(
                        R.drawable.bg_badge_unplanned
                );

                txtBadgeType.setTextColor(
                        ContextCompat.getColor(
                                context,
                                R.color.severity_road_closed
                        )
                );

            } else {

                String delay = situation.getDelaysType();

                if ("veryLongDelays".equalsIgnoreCase(delay)) {

                    txtBadgeType.setText("Very Long Delays");

                    txtBadgeType.setBackgroundResource(
                            R.drawable.bg_badge_very_long_delays
                    );

                    txtBadgeType.setTextColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.severity_very_long_delays
                            )
                    );

                } else if ("longDelays".equalsIgnoreCase(delay)) {

                    txtBadgeType.setText("Long Delays");

                    txtBadgeType.setBackgroundResource(
                            R.drawable.bg_badge_roadworks
                    );

                    txtBadgeType.setTextColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.severity_long_delays
                            )
                    );

                } else if ("delays".equalsIgnoreCase(delay)) {

                    txtBadgeType.setText("Delays");

                    txtBadgeType.setBackgroundResource(
                            R.drawable.bg_badge_delays
                    );

                    txtBadgeType.setTextColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.severity_delays
                            )
                    );

                } else {

                    txtBadgeType.setText("Roadworks");

                    txtBadgeType.setBackgroundResource(
                            R.drawable.bg_badge_delays
                    );

                    txtBadgeType.setTextColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.primary_navy
                            )
                    );
                }
            }

            itemView.setOnClickListener(v -> {

                if (listener != null) {
                    listener.onItemClick(situation);
                }
            });
        }
    }
}