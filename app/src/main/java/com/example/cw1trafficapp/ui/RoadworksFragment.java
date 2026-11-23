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

public class RoadworksFragment extends Fragment {

    private TrafficViewModel viewModel;
    private TrafficAdapter adapter;

    private EditText editSearchRoadworks;
    private Spinner spinnerDistanceRoadworks;
    private TextView txtRoadworksCountHeader;
    private RecyclerView recyclerRoadworks;
    private TextView txtEmptyRoadworks;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_roadworks,
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

        editSearchRoadworks =
                view.findViewById(R.id.editSearchRoadworks);

        spinnerDistanceRoadworks =
                view.findViewById(R.id.spinnerDistanceRoadworks);

        txtRoadworksCountHeader =
                view.findViewById(R.id.txtRoadworksCountHeader);

        recyclerRoadworks =
                view.findViewById(R.id.recyclerRoadworks);

        txtEmptyRoadworks =
                view.findViewById(R.id.txtEmptyRoadworks);

        view.findViewById(
                R.id.btnShowFilteredRoadworksMap
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

        recyclerRoadworks.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerRoadworks.setAdapter(adapter);

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

        String[] options = {
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
                        options
                );

        spinnerAdapter.setDropDownViewResource(
                R.layout.spinner_dropdown_item
        );

        spinnerDistanceRoadworks.setAdapter(
                spinnerAdapter
        );

        Double radius =
                viewModel.getDistanceFilterRadiusKm().getValue();

        if (radius != null) {

            if (radius == 5.0) {
                spinnerDistanceRoadworks.setSelection(1);
            } else if (radius == 10.0) {
                spinnerDistanceRoadworks.setSelection(2);
            } else if (radius == 25.0) {
                spinnerDistanceRoadworks.setSelection(3);
            } else if (radius == 50.0) {
                spinnerDistanceRoadworks.setSelection(4);
            } else if (radius == 100.0) {
                spinnerDistanceRoadworks.setSelection(5);
            } else {
                spinnerDistanceRoadworks.setSelection(0);
            }
        }

        spinnerDistanceRoadworks.setOnItemSelectedListener(
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
            editSearchRoadworks.setText(query);
        }

        editSearchRoadworks.addTextChangedListener(
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

        editSearchRoadworks.setOnTouchListener(
                (v, event) -> {

                    if (event.getAction()
                            == MotionEvent.ACTION_UP) {

                        if (editSearchRoadworks
                                .getCompoundDrawables()[2] != null) {

                            if (event.getRawX()
                                    >= editSearchRoadworks.getRight()
                                    - editSearchRoadworks
                                    .getCompoundDrawables()[2]
                                    .getBounds().width()
                                    - 48) {

                                editSearchRoadworks.setText("");
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

        viewModel.getFilteredRoadworksLiveData().observe(
                getViewLifecycleOwner(),
                roadworks -> {

                    if (roadworks != null
                            && !roadworks.isEmpty()) {

                        adapter.setSituations(roadworks);

                        txtRoadworksCountHeader.setText(
                                String.format(
                                        Locale.UK,
                                        "Showing %d active roadworks",
                                        roadworks.size()
                                )
                        );

                        recyclerRoadworks.setVisibility(
                                View.VISIBLE
                        );

                        txtEmptyRoadworks.setVisibility(
                                View.GONE
                        );

                    } else {

                        adapter.setSituations(null);

                        txtRoadworksCountHeader.setText(
                                "Showing 0 active roadworks"
                        );

                        recyclerRoadworks.setVisibility(
                                View.GONE
                        );

                        txtEmptyRoadworks.setVisibility(
                                View.VISIBLE
                        );
                    }
                }
        );
    }
}