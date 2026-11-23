
package com.example.cw1trafficapp.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cw1trafficapp.MainActivity;
import com.example.cw1trafficapp.R;
import com.example.cw1trafficapp.adapter.TrafficAdapter;
import com.example.cw1trafficapp.model.TrafficSituation;
import com.example.cw1trafficapp.viewmodel.TrafficViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main Home screen for Traffic Scotland.
 *
 * Displays live traffic statistics, data source status,
 * road-name search, distance filtering and incident previews.
 */
public class HomeFragment extends Fragment {

    private static final int PREVIEW_LIMIT = 5;

    private static final double[] DISTANCE_OPTIONS = {
            0.0, 5.0, 10.0, 25.0, 50.0, 100.0
    };

    private TrafficViewModel viewModel;
    private TrafficAdapter incidentsAdapter;

    private SwipeRefreshLayout swipeRefresh;

    private TextView txtIncidentsCount;
    private TextView txtRoadworksCount;
    private TextView txtClosuresCount;
    private TextView txtLongDelaysCount;
    private TextView txtDataSourceLabel;
    private TextView txtLastUpdatedTime;

    private LinearLayout layoutUnavailable;
    private TextView txtUnavailableError;

    private Button btnRefreshData;
    private Button btnRetryDownload;

    private EditText editSearchQuery;
    private Spinner spinnerDistanceFilter;

    private RecyclerView recyclerIncidentsPreview;
    private TextView txtEmptyState;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_home,
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

        setupRecyclerView();
        setupDistanceSpinner();
        setupSearchInput();
        setupClickListeners(view);
        observeViewModel();

        swipeRefresh.setOnRefreshListener(
                () -> viewModel.refreshData()
        );
    }

    /**
     * Find all views in fragment_home.xml.
     */
    private void initialiseViews(View view) {

        swipeRefresh =
                view.findViewById(R.id.swipeRefresh);

        txtIncidentsCount =
                view.findViewById(R.id.txtIncidentsCount);

        txtRoadworksCount =
                view.findViewById(R.id.txtRoadworksCount);

        txtClosuresCount =
                view.findViewById(R.id.txtClosuresCount);

        txtLongDelaysCount =
                view.findViewById(R.id.txtLongDelaysCount);

        txtDataSourceLabel =
                view.findViewById(R.id.txtDataSourceLabel);

        txtLastUpdatedTime =
                view.findViewById(R.id.txtLastUpdatedTime);

        layoutUnavailable =
                view.findViewById(R.id.layoutUnavailable);

        txtUnavailableError =
                view.findViewById(R.id.txtUnavailableError);

        btnRefreshData =
                view.findViewById(R.id.btnRefreshData);

        btnRetryDownload =
                view.findViewById(R.id.btnRetryDownload);

        editSearchQuery =
                view.findViewById(R.id.editSearchQuery);

        spinnerDistanceFilter =
                view.findViewById(R.id.spinnerDistanceFilter);

        recyclerIncidentsPreview =
                view.findViewById(R.id.recyclerIncidentsPreview);

        txtEmptyState =
                view.findViewById(R.id.txtEmptyState);
    }

    /**
     * Displays a limited incident preview on Home.
     *
     * The full incident list remains available
     * from the Incidents navigation tab.
     */
    private void setupRecyclerView() {

        incidentsAdapter = new TrafficAdapter();

        recyclerIncidentsPreview.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerIncidentsPreview.setNestedScrollingEnabled(
                false
        );

        recyclerIncidentsPreview.setAdapter(
                incidentsAdapter
        );

        incidentsAdapter.setOnItemClickListener(
                situation -> {

                    viewModel.setSelectedSituation(
                            situation
                    );

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity())
                                .navigateToFragment(
                                        new DetailsFragment()
                                );
                    }
                }
        );
    }

    /**
     * Sets up distance filtering from central Glasgow.
     */
    private void setupDistanceSpinner() {

        String[] options = {
                "All Scotland",
                "Within 5 km",
                "Within 10 km",
                "Within 25 km",
                "Within 50 km",
                "Within 100 km"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        R.layout.spinner_item,
                        options
                );

        adapter.setDropDownViewResource(
                R.layout.spinner_dropdown_item
        );

        spinnerDistanceFilter.setAdapter(adapter);

        Double currentRadius =
                viewModel.getDistanceFilterRadiusKm()
                        .getValue();

        double radius = currentRadius != null
                ? currentRadius
                : 0.0;

        int selectedIndex = 0;

        for (int i = 0; i < DISTANCE_OPTIONS.length; i++) {

            if (Double.compare(
                    DISTANCE_OPTIONS[i],
                    radius
            ) == 0) {

                selectedIndex = i;
                break;
            }
        }

        spinnerDistanceFilter.setSelection(
                selectedIndex
        );

        spinnerDistanceFilter.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id) {

                        if (position >= 0
                                && position < DISTANCE_OPTIONS.length) {

                            viewModel.setDistanceFilterRadiusKm(
                                    DISTANCE_OPTIONS[position]
                            );
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }

    /**
     * Shares the road-name search with other Fragments
     * through the activity-scoped ViewModel.
     */
    private void setupSearchInput() {

        String existingQuery =
                viewModel.getSearchQueryLiveData()
                        .getValue();

        if (existingQuery != null) {
            editSearchQuery.setText(existingQuery);
        }

        editSearchQuery.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        viewModel.setSearchQuery(
                                s != null ? s.toString() : ""
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );

        // Clear the search when tapping the trailing X.
        editSearchQuery.setOnTouchListener((v, event) -> {

            if (event.getAction()
                    != MotionEvent.ACTION_UP) {
                return false;
            }

            android.graphics.drawable.Drawable clearIcon =
                    editSearchQuery.getCompoundDrawables()[2];

            if (clearIcon == null) {
                return false;
            }

            float touchArea = clearIcon.getBounds().width()
                    + dpToPx(20);

            float startX = editSearchQuery.getWidth()
                    - editSearchQuery.getPaddingRight()
                    - touchArea;

            if (event.getX() >= startX) {

                editSearchQuery.setText("");
                v.performClick();

                return true;
            }

            return false;
        });
    }

    private float dpToPx(float dp) {
        return dp * getResources()
                .getDisplayMetrics().density;
    }

    /**
     * Navigation buttons and manual refresh.
     */
    private void setupClickListeners(View view) {

        btnRefreshData.setOnClickListener(
                v -> viewModel.refreshData()
        );

        btnRetryDownload.setOnClickListener(
                v -> viewModel.refreshData()
        );

        view.findViewById(
                R.id.cardIncidentsCount
        ).setOnClickListener(
                v -> openTab(R.id.nav_incidents)
        );

        view.findViewById(
                R.id.cardRoadworksCount
        ).setOnClickListener(
                v -> openTab(R.id.nav_roadworks)
        );

        view.findViewById(
                R.id.btnViewAllIncidents
        ).setOnClickListener(
                v -> openTab(R.id.nav_incidents)
        );

        view.findViewById(
                R.id.btnViewMatchingMap
        ).setOnClickListener(v -> {

            viewModel.setMapMode(
                    TrafficViewModel.MapMode.FILTERED
            );

            openTab(R.id.nav_map);
        });
    }

    private void openTab(int tabId) {

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity())
                    .selectTab(tabId);
        }
    }

    /**
     * Observes traffic data, status and filtered results.
     */
    private void observeViewModel() {

        // Loading indicator
        viewModel.getIsLoadingLiveData().observe(
                getViewLifecycleOwner(),
                isLoading -> {

                    boolean loading =
                            Boolean.TRUE.equals(isLoading);

                    swipeRefresh.setRefreshing(loading);

                    btnRefreshData.setEnabled(!loading);
                    btnRetryDownload.setEnabled(!loading);
                }
        );

        // Live or cached source
        viewModel.getDataSourceLabelLiveData().observe(
                getViewLifecycleOwner(),
                label -> {

                    txtDataSourceLabel.setText(
                            label != null
                                    ? label
                                    : "Traffic status unavailable"
                    );
                }
        );

        // Last successful update
        viewModel.getLastUpdatedTimeLiveData().observe(
                getViewLifecycleOwner(),
                updated -> {

                    txtLastUpdatedTime.setText(
                            String.format(
                                    Locale.UK,
                                    "Last updated: %s",
                                    updated != null
                                            ? updated
                                            : "Unknown"
                            )
                    );
                }
        );

        // Network errors
        viewModel.getErrorLiveData().observe(
                getViewLifecycleOwner(),
                error -> {

                    boolean hasError =
                            error != null
                                    && !error.trim().isEmpty();

                    layoutUnavailable.setVisibility(
                            hasError
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    if (hasError) {
                        txtUnavailableError.setText(error);
                    }
                }
        );

        // Keep all summary counts updated
        // whenever either traffic feed changes.
        viewModel.getIncidentsLiveData().observe(
                getViewLifecycleOwner(),
                incidents -> updateSummaryCounters()
        );

        viewModel.getRoadworksLiveData().observe(
                getViewLifecycleOwner(),
                roadworks -> updateSummaryCounters()
        );

        // Preview of matching incidents
        viewModel.getFilteredIncidentsLiveData().observe(
                getViewLifecycleOwner(),
                incidents -> updateIncidentPreview(incidents)
        );
    }

    /**
     * Updates nationwide situation counts.
     *
     * Road closures are counted from BOTH the
     * incidents and roadworks feeds.
     */
    private void updateSummaryCounters() {

        List<TrafficSituation> incidents =
                viewModel.getIncidentsLiveData().getValue();

        List<TrafficSituation> roadworks =
                viewModel.getRoadworksLiveData().getValue();

        int incidentCount =
                incidents == null ? 0 : incidents.size();

        int roadworksCount =
                roadworks == null ? 0 : roadworks.size();

        int closureCount = 0;
        int longDelayCount = 0;

        if (incidents != null) {

            for (TrafficSituation situation : incidents) {

                if (isRoadClosure(situation)) {
                    closureCount++;
                }
            }
        }

        if (roadworks != null) {

            for (TrafficSituation situation : roadworks) {

                if (isRoadClosure(situation)) {
                    closureCount++;
                }

                if (isLongDelay(situation)) {
                    longDelayCount++;
                }
            }
        }

        txtIncidentsCount.setText(
                String.valueOf(incidentCount)
        );

        txtRoadworksCount.setText(
                String.valueOf(roadworksCount)
        );

        txtClosuresCount.setText(
                String.valueOf(closureCount)
        );

        txtLongDelaysCount.setText(
                String.valueOf(longDelayCount)
        );
    }

    /**
     * Identifies records describing a full road
     * or carriageway closure.
     *
     * Excludes obvious lane-only restrictions.
     */
    private boolean isRoadClosure(
            TrafficSituation situation) {

        if (situation == null) {
            return false;
        }

        String subtype = safeLower(
                situation.getSubtype()
        );

        String comment = safeLower(
                situation.getPublicComment()
        );

        // Explicit DATEX II closure classifications
        if (subtype.contains("roadclosed")
                || subtype.contains("carriagewayclosed")
                || subtype.contains("alllanesclosed")
                || subtype.contains("roadclosure")) {

            return true;
        }

        // Exclude lane-only restrictions
        if (comment.contains("lane closure")
                || comment.contains("lane closures")
                || comment.contains("lane closed")
                || comment.contains("lanes closed")) {

            return false;
        }

        return comment.contains("road closed")
                || comment.contains("road closure")
                || comment.contains("carriageway closed")
                || comment.contains("carriageway closure")
                || comment.contains("full closure")
                || comment.contains("total closure")
                || comment.contains(" - closure");
    }

    /**
     * Counts long and very long delays reported
     * by the roadworks feed.
     */
    private boolean isLongDelay(
            TrafficSituation situation) {

        if (situation == null) {
            return false;
        }

        String delay = situation.getDelaysType();

        return "longDelays".equalsIgnoreCase(delay)
                || "veryLongDelays".equalsIgnoreCase(delay);
    }

    private String safeLower(String text) {

        return text == null
                ? ""
                : text.toLowerCase(Locale.UK);
    }

    /**
     * Display only the first few matching incidents
     * to keep Home concise and responsive.
     */
    private void updateIncidentPreview(
            List<TrafficSituation> incidents) {

        if (incidents == null || incidents.isEmpty()) {

            incidentsAdapter.setSituations(
                    new ArrayList<>()
            );

            recyclerIncidentsPreview.setVisibility(
                    View.GONE
            );

            txtEmptyState.setVisibility(
                    View.VISIBLE
            );

            return;
        }

        int limit = Math.min(
                PREVIEW_LIMIT,
                incidents.size()
        );

        List<TrafficSituation> preview =
                new ArrayList<>(
                        incidents.subList(0, limit)
                );

        incidentsAdapter.setSituations(preview);

        recyclerIncidentsPreview.setVisibility(
                View.VISIBLE
        );

        txtEmptyState.setVisibility(
                View.GONE
        );
    }
}
