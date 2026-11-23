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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cw1trafficapp.MainActivity;
import com.example.cw1trafficapp.R;
import com.example.cw1trafficapp.adapter.TrafficAdapter;
import com.example.cw1trafficapp.viewmodel.TrafficViewModel;

import java.util.Locale;

public class IncidentsFragment extends Fragment {

    private TrafficViewModel viewModel;
    private TrafficAdapter adapter;

    private EditText editSearchIncidents;
    private Spinner spinnerDistanceIncidents;
    private TextView txtIncidentsCountHeader;
    private RecyclerView recyclerIncidents;
    private TextView txtEmptyIncidents;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_incidents,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(
                requireActivity()
        ).get(TrafficViewModel.class);

        editSearchIncidents =
                view.findViewById(R.id.editSearchIncidents);

        spinnerDistanceIncidents =
                view.findViewById(R.id.spinnerDistanceIncidents);

        txtIncidentsCountHeader =
                view.findViewById(R.id.txtIncidentsCountHeader);

        recyclerIncidents =
                view.findViewById(R.id.recyclerIncidents);

        txtEmptyIncidents =
                view.findViewById(R.id.txtEmptyIncidents);

        view.findViewById(
                R.id.btnShowFilteredIncidentsMap
        ).setOnClickListener(v -> {

            viewModel.setMapMode(
                    TrafficViewModel.MapMode.FILTERED
            );

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity())
                        .selectTab(R.id.nav_map);
            }
        });

        setupRecyclerView();
        setupDistanceSpinner();
        setupSearchInput();
        observeViewModel();
    }

    private void setupRecyclerView() {

        adapter = new TrafficAdapter();

        recyclerIncidents.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerIncidents.setAdapter(adapter);

        adapter.setOnItemClickListener(situation -> {

            viewModel.setSelectedSituation(situation);

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity())
                        .navigateToFragment(
                                new DetailsFragment()
                        );
            }
        });
    }

    private void setupDistanceSpinner() {

        String[] distanceOptions = {
                "All Scotland",
                "Within 5 km",
                "Within 10 km",
                "Within 25 km",
                "Within 50 km",
                "Within 100 km"
        };

        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        R.layout.spinner_item,
                        distanceOptions
                );

        spinnerAdapter.setDropDownViewResource(
                R.layout.spinner_dropdown_item
        );

        spinnerDistanceIncidents.setAdapter(spinnerAdapter);

        Double radius =
                viewModel.getDistanceFilterRadiusKm().getValue();

        if (radius != null) {

            if (radius == 5.0) {
                spinnerDistanceIncidents.setSelection(1);
            } else if (radius == 10.0) {
                spinnerDistanceIncidents.setSelection(2);
            } else if (radius == 25.0) {
                spinnerDistanceIncidents.setSelection(3);
            } else if (radius == 50.0) {
                spinnerDistanceIncidents.setSelection(4);
            } else if (radius == 100.0) {
                spinnerDistanceIncidents.setSelection(5);
            } else {
                spinnerDistanceIncidents.setSelection(0);
            }
        }

        spinnerDistanceIncidents.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        double newRadius = 0.0;

                        switch (position) {
                            case 1: newRadius = 5.0; break;
                            case 2: newRadius = 10.0; break;
                            case 3: newRadius = 25.0; break;
                            case 4: newRadius = 50.0; break;
                            case 5: newRadius = 100.0; break;
                        }

                        viewModel.setDistanceFilterRadiusKm(
                                newRadius
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }

    private void setupSearchInput() {

        String query =
                viewModel.getSearchQueryLiveData().getValue();

        if (query != null && !query.isEmpty()) {
            editSearchIncidents.setText(query);
        }

        editSearchIncidents.addTextChangedListener(
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

        editSearchIncidents.setOnTouchListener(
                (v, event) -> {

                    if (event.getAction()
                            == MotionEvent.ACTION_UP) {

                        if (editSearchIncidents
                                .getCompoundDrawables()[2] != null) {

                            if (event.getRawX()
                                    >= editSearchIncidents.getRight()
                                    - editSearchIncidents
                                    .getCompoundDrawables()[2]
                                    .getBounds().width()
                                    - 48) {

                                editSearchIncidents.setText("");
                                v.performClick();
                                return true;
                            }
                        }
                    }

                    return false;
                }
        );
    }

    private void observeViewModel() {

        viewModel.getFilteredIncidentsLiveData().observe(
                getViewLifecycleOwner(),
                incidents -> {

                    if (incidents != null
                            && !incidents.isEmpty()) {

                        adapter.setSituations(incidents);

                        txtIncidentsCountHeader.setText(
                                String.format(
                                        Locale.UK,
                                        "Showing %d unplanned traffic incidents",
                                        incidents.size()
                                )
                        );

                        recyclerIncidents.setVisibility(
                                View.VISIBLE
                        );

                        txtEmptyIncidents.setVisibility(
                                View.GONE
                        );

                    } else {

                        adapter.setSituations(null);

                        txtIncidentsCountHeader.setText(
                                "Showing 0 unplanned traffic incidents"
                        );

                        recyclerIncidents.setVisibility(
                                View.GONE
                        );

                        txtEmptyIncidents.setVisibility(
                                View.VISIBLE
                        );
                    }
                }
        );
    }
}