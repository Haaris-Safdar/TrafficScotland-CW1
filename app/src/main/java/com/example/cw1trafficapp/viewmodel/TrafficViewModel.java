package com.example.cw1trafficapp.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cw1trafficapp.model.TrafficSituation;
import com.example.cw1trafficapp.repository.TrafficRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrafficViewModel extends AndroidViewModel {

    private static final String TAG = "TRAFFIC_API";

    public enum MapMode {
        ALL,
        FILTERED,
        SELECTED
    }

    private final TrafficRepository repository;

    private final MutableLiveData<List<TrafficSituation>>
            incidentsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<TrafficSituation>>
            roadworksLiveData =
            new MutableLiveData<>(new ArrayList<>());

    private final MediatorLiveData<List<TrafficSituation>>
            filteredIncidentsLiveData =
            new MediatorLiveData<>();

    private final MediatorLiveData<List<TrafficSituation>>
            filteredRoadworksLiveData =
            new MediatorLiveData<>();

    private final MutableLiveData<Boolean> isLoadingLiveData =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> errorLiveData =
            new MutableLiveData<>(null);

    private final MutableLiveData<String> searchQueryLiveData =
            new MutableLiveData<>("");

    private final MutableLiveData<Double> distanceFilterRadiusKm =
            new MutableLiveData<>(0.0);

    private final MutableLiveData<TrafficSituation>
            selectedSituationLiveData =
            new MutableLiveData<>(null);

    private final MutableLiveData<MapMode> mapModeLiveData =
            new MutableLiveData<>(MapMode.ALL);

    private final MutableLiveData<TrafficRepository.TrafficResult>
            trafficResultLiveData =
            new MutableLiveData<>(null);

    private final MutableLiveData<String> dataSourceLabelLiveData =
            new MutableLiveData<>("Waiting for traffic data");

    private final MutableLiveData<String> lastUpdatedTimeLiveData =
            new MutableLiveData<>("Updating...");

    public TrafficViewModel(@NonNull Application application) {
        super(application);

        repository = new TrafficRepository();

        filteredIncidentsLiveData.addSource(
                incidentsLiveData, list -> applyFilters());

        filteredIncidentsLiveData.addSource(
                searchQueryLiveData, query -> applyFilters());

        filteredIncidentsLiveData.addSource(
                distanceFilterRadiusKm, radius -> applyFilters());

        filteredRoadworksLiveData.addSource(
                roadworksLiveData, list -> applyFilters());

        filteredRoadworksLiveData.addSource(
                searchQueryLiveData, query -> applyFilters());

        filteredRoadworksLiveData.addSource(
                distanceFilterRadiusKm, radius -> applyFilters());

        loadData();
        startAutoRefresh();
    }

    public LiveData<List<TrafficSituation>>
    getIncidentsLiveData() {
        return incidentsLiveData;
    }

    public LiveData<List<TrafficSituation>>
    getRoadworksLiveData() {
        return roadworksLiveData;
    }

    public LiveData<List<TrafficSituation>>
    getFilteredIncidentsLiveData() {
        return filteredIncidentsLiveData;
    }

    public LiveData<List<TrafficSituation>>
    getFilteredRoadworksLiveData() {
        return filteredRoadworksLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<String> getSearchQueryLiveData() {
        return searchQueryLiveData;
    }

    public LiveData<Double> getDistanceFilterRadiusKm() {
        return distanceFilterRadiusKm;
    }

    public LiveData<TrafficSituation>
    getSelectedSituationLiveData() {
        return selectedSituationLiveData;
    }

    public LiveData<MapMode> getMapModeLiveData() {
        return mapModeLiveData;
    }

    public LiveData<TrafficRepository.TrafficResult>
    getTrafficResultLiveData() {
        return trafficResultLiveData;
    }

    public LiveData<String> getDataSourceLabelLiveData() {
        return dataSourceLabelLiveData;
    }

    public LiveData<String> getLastUpdatedTimeLiveData() {
        return lastUpdatedTimeLiveData;
    }

    public void setSearchQuery(String query) {
        String next = query != null ? query.trim() : "";

        if (!next.equals(searchQueryLiveData.getValue())) {
            searchQueryLiveData.setValue(next);
        }
    }

    public void setDistanceFilterRadiusKm(double radiusKm) {
        if (!Double.isFinite(radiusKm) || radiusKm < 0) {
            radiusKm = 0.0;
        }

        Double current = distanceFilterRadiusKm.getValue();

        if (current == null
                || Double.compare(current, radiusKm) != 0) {
            distanceFilterRadiusKm.setValue(radiusKm);
        }
    }

    public void setMapMode(MapMode mode) {
        mapModeLiveData.setValue(
                mode == null ? MapMode.ALL : mode
        );
    }

    public void setSelectedSituation(
            TrafficSituation situation) {
        selectedSituationLiveData.setValue(situation);
    }

    public void refreshData() {
        if (Boolean.TRUE.equals(
                isLoadingLiveData.getValue())) {
            return;
        }

        loadData();
    }

    public void loadData() {
        if (Boolean.TRUE.equals(
                isLoadingLiveData.getValue())) {
            return;
        }

        isLoadingLiveData.setValue(true);
        errorLiveData.setValue(null);

        Log.d(TAG, "ViewModel loadData requested");

        repository.loadTrafficData(
                getApplication(),
                createCallback("Startup/manual")
        );
    }

    public void startAutoRefresh() {
        repository.startHourlyAutoRefresh(
                getApplication(),
                createCallback("Hourly")
        );

        Log.d(TAG, "Hourly auto-refresh scheduled");
    }

    private TrafficRepository.DataCallback<
            TrafficRepository.TrafficResult>
    createCallback(String origin) {

        return new TrafficRepository.DataCallback<
                TrafficRepository.TrafficResult>() {

            @Override
            public void onSuccess(
                    TrafficRepository.TrafficResult result) {

                isLoadingLiveData.setValue(false);

                if (result == null) {
                    errorLiveData.setValue(
                            "Traffic data could not be processed."
                    );
                    return;
                }

                trafficResultLiveData.setValue(result);

                incidentsLiveData.setValue(
                        new ArrayList<>(result.getIncidents())
                );

                roadworksLiveData.setValue(
                        new ArrayList<>(result.getRoadworks())
                );

                dataSourceLabelLiveData.setValue(
                        result.getOverallStatusLabel()
                );

                lastUpdatedTimeLiveData.setValue(
                        result.getFormattedLastUpdatedTime()
                );

                boolean neitherAvailable =
                        result.getUnplannedStatus()
                                == TrafficRepository.FeedStatus.UNAVAILABLE
                                && result.getRoadworksStatus()
                                == TrafficRepository.FeedStatus.UNAVAILABLE;

                errorLiveData.setValue(
                        neitherAvailable
                                ? "Traffic feeds unavailable. Connect to the internet and retry."
                                : null
                );

                Log.d(TAG,
                        origin + " update applied: "
                                + result.getIncidents().size()
                                + " incidents, "
                                + result.getRoadworks().size()
                                + " roadworks");
            }

            @Override
            public void onError(String message) {
                isLoadingLiveData.setValue(false);

                errorLiveData.setValue(
                        message != null && !message.trim().isEmpty()
                                ? message
                                : "Unable to refresh traffic data."
                );

                Log.e(
                        TAG,
                        origin + " refresh failed: " + message
                );
            }
        };
    }

    private void applyFilters() {
        List<TrafficSituation> allIncidents =
                incidentsLiveData.getValue();

        List<TrafficSituation> allRoadworks =
                roadworksLiveData.getValue();

        String query = searchQueryLiveData.getValue() != null
                ? searchQueryLiveData.getValue()
                .toLowerCase(Locale.UK)
                : "";

        Double radius = distanceFilterRadiusKm.getValue() != null
                ? distanceFilterRadiusKm.getValue()
                : 0.0;

        filteredIncidentsLiveData.setValue(
                filterList(allIncidents, query, radius)
        );

        filteredRoadworksLiveData.setValue(
                filterList(allRoadworks, query, radius)
        );
    }

    private List<TrafficSituation> filterList(
            List<TrafficSituation> originalList,
            String query,
            Double radiusKm) {

        if (originalList == null) {
            return new ArrayList<>();
        }

        List<TrafficSituation> result = new ArrayList<>();

        for (TrafficSituation situation : originalList) {

            boolean matchesSearch = true;

            if (!query.isEmpty()) {
                String comment =
                        situation.getPublicComment()
                                .toLowerCase(Locale.UK);

                String road =
                        situation.getRoadName()
                                .toLowerCase(Locale.UK);

                String fromLoc =
                        situation.getFromLocation()
                                .getDisplayName()
                                .toLowerCase(Locale.UK);

                String toLoc =
                        situation.getToLocation()
                                .getDisplayName()
                                .toLowerCase(Locale.UK);

                matchesSearch =
                        comment.contains(query)
                                || road.contains(query)
                                || fromLoc.contains(query)
                                || toLoc.contains(query);
            }

            boolean matchesDistance = true;

            if (radiusKm != null && radiusKm > 0.0) {
                double dist =
                        situation.getDistanceFromReference(
                                TrafficSituation.GLASGOW_LAT,
                                TrafficSituation.GLASGOW_LNG
                        );

                matchesDistance =
                        dist >= 0.0 && dist <= radiusKm;
            }

            if (matchesSearch && matchesDistance) {
                result.add(situation);
            }
        }

        return result;
    }
}