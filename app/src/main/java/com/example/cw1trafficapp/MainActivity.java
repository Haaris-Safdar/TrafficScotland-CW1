
package com.example.cw1trafficapp;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.cw1trafficapp.ui.DetailsFragment;
import com.example.cw1trafficapp.ui.HomeFragment;
import com.example.cw1trafficapp.ui.IncidentsFragment;
import com.example.cw1trafficapp.ui.MapFragment;
import com.example.cw1trafficapp.ui.RoadworksFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigationrail.NavigationRailView;

/**
 * Main Activity for Traffic Scotland.
 *
 * Uses one Activity with Fragments and MVVM.
 *
 * Portrait: bottom navigation.
 * Landscape: navigation rail.
 *
 * Handles system insets, tab navigation,
 * details back navigation and configuration changes.
 */
public class MainActivity extends AppCompatActivity {

    private static final String KEY_ACTIVE_TAB =
            "active_tab_id";

    private Toolbar toolbar;

    private BottomNavigationView bottomNavigation;
    private NavigationRailView navigationRail;

    private int currentTabId = R.id.nav_home;

    private boolean isProgrammaticSelection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Explicit edge-to-edge handling on supported Android versions.
        WindowCompat.setDecorFitsSystemWindows(
                getWindow(),
                false
        );

        setContentView(R.layout.activity_main);

        initialiseViews();
        setupWindowInsets();
        setupNavigation();

        if (savedInstanceState != null) {

            currentTabId =
                    savedInstanceState.getInt(
                            KEY_ACTIVE_TAB,
                            R.id.nav_home
                    );
        }

        getSupportFragmentManager()
                .addOnBackStackChangedListener(
                        this::updateNavigationOnBackStackChange
                );

        if (savedInstanceState == null) {

            // Open Home on first launch.
            selectTab(R.id.nav_home);

        } else {

            // Keep the selected tab after rotation.
            syncNavigationSelection(currentTabId);

            Fragment existing =
                    getSupportFragmentManager()
                            .findFragmentById(
                                    R.id.fragment_container
                            );

            if (existing == null) {
                selectTab(currentTabId);
            }
        }

        updateToolbarNavigation();
    }

    /**
     * Obtain views used in both orientations.
     */
    private void initialiseViews() {

        toolbar = findViewById(R.id.toolbar);

        bottomNavigation =
                findViewById(R.id.bottom_navigation);

        navigationRail =
                findViewById(R.id.navigation_rail);
    }

    /**
     * Prevents the toolbar title from being clipped
     * beneath the Android status bar.
     *
     * Also keeps bottom navigation clear of
     * gesture navigation/system bars.
     */
    private void setupWindowInsets() {

        if (toolbar != null) {

            ViewGroup.LayoutParams initialParams =
                    toolbar.getLayoutParams();

            final int normalHeight =
                    initialParams.height > 0
                            ? initialParams.height
                            : dpToPx(56);

            final int initialTopPadding =
                    toolbar.getPaddingTop();

            final int initialBottomPadding =
                    toolbar.getPaddingBottom();

            final int initialLeftPadding =
                    toolbar.getPaddingLeft();

            final int initialRightPadding =
                    toolbar.getPaddingRight();

            ViewCompat.setOnApplyWindowInsetsListener(
                    toolbar,
                    (view, windowInsets) -> {

                        Insets statusBars =
                                windowInsets.getInsets(
                                        WindowInsetsCompat.Type.statusBars()
                                                | WindowInsetsCompat.Type.displayCutout()
                                );

                        int topInset = statusBars.top;

                        int requiredHeight =
                                normalHeight + topInset;

                        ViewGroup.LayoutParams params =
                                view.getLayoutParams();

                        if (params.height != requiredHeight) {

                            params.height = requiredHeight;
                            view.setLayoutParams(params);
                        }

                        view.setPadding(
                                initialLeftPadding,
                                initialTopPadding + topInset,
                                initialRightPadding,
                                initialBottomPadding
                        );

                        return windowInsets;
                    }
            );

            ViewCompat.requestApplyInsets(toolbar);
        }

        // Portrait bottom navigation.
        if (bottomNavigation != null) {

            final int originalLeft =
                    bottomNavigation.getPaddingLeft();

            final int originalTop =
                    bottomNavigation.getPaddingTop();

            final int originalRight =
                    bottomNavigation.getPaddingRight();

            final int originalBottom =
                    bottomNavigation.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(
                    bottomNavigation,
                    (view, windowInsets) -> {

                        Insets navigationBars =
                                windowInsets.getInsets(
                                        WindowInsetsCompat.Type.navigationBars()
                                );

                        view.setPadding(
                                originalLeft,
                                originalTop,
                                originalRight,
                                originalBottom + navigationBars.bottom
                        );

                        return windowInsets;
                    }
            );

            ViewCompat.requestApplyInsets(bottomNavigation);
        }

        // Landscape navigation rail.
        if (navigationRail != null) {

            final int originalLeft =
                    navigationRail.getPaddingLeft();

            final int originalTop =
                    navigationRail.getPaddingTop();

            final int originalRight =
                    navigationRail.getPaddingRight();

            final int originalBottom =
                    navigationRail.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(
                    navigationRail,
                    (view, windowInsets) -> {

                        Insets systemBars =
                                windowInsets.getInsets(
                                        WindowInsetsCompat.Type.systemBars()
                                );

                        view.setPadding(
                                originalLeft,
                                originalTop + systemBars.top,
                                originalRight,
                                originalBottom + systemBars.bottom
                        );

                        return windowInsets;
                    }
            );

            ViewCompat.requestApplyInsets(navigationRail);
        }
    }

    private int dpToPx(float dp) {

        return Math.round(
                dp * getResources()
                        .getDisplayMetrics().density
        );
    }

    /**
     * Configure portrait and landscape navigation.
     */
    private void setupNavigation() {

        if (bottomNavigation != null) {

            bottomNavigation.setOnItemSelectedListener(
                    item -> {

                        if (isProgrammaticSelection) {
                            return true;
                        }

                        loadTabFragment(
                                item.getItemId()
                        );

                        return true;
                    }
            );
        }

        if (navigationRail != null) {

            navigationRail.setOnItemSelectedListener(
                    item -> {

                        if (isProgrammaticSelection) {
                            return true;
                        }

                        loadTabFragment(
                                item.getItemId()
                        );

                        return true;
                    }
            );
        }
    }

    /**
     * Public method used by Fragments to switch tabs.
     */
    public void selectTab(int menuId) {

        loadTabFragment(menuId);
    }

    /**
     * Synchronise the currently selected navigation item.
     */
    private void syncNavigationSelection(int menuId) {

        isProgrammaticSelection = true;

        try {

            if (bottomNavigation != null) {

                if (bottomNavigation.getSelectedItemId()
                        != menuId) {

                    bottomNavigation.setSelectedItemId(
                            menuId
                    );
                }
            }

            if (navigationRail != null) {

                if (navigationRail.getSelectedItemId()
                        != menuId) {

                    navigationRail.setSelectedItemId(
                            menuId
                    );
                }
            }

        } finally {

            isProgrammaticSelection = false;
        }
    }

    /**
     * Opens a main navigation destination.
     */
    private void loadTabFragment(int menuId) {

        FragmentManager fragmentManager =
                getSupportFragmentManager();

        currentTabId = menuId;

        Fragment currentFragment =
                fragmentManager.findFragmentById(
                        R.id.fragment_container
                );

        // Avoid unnecessarily recreating the current tab.
        if (currentFragment != null
                && isSameTabFragment(
                currentFragment,
                menuId
        )) {

            syncNavigationSelection(menuId);
            updateToolbarNavigation();

            return;
        }

        Fragment newFragment = createTabFragment(
                menuId
        );

        // Remove details/back-stack entries on tab changes.
        fragmentManager.popBackStackImmediate(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
        );

        replaceFragment(
                newFragment,
                false
        );

        syncNavigationSelection(menuId);
        updateToolbarNavigation();
    }

    /**
     * Create a Fragment for the requested tab.
     */
    private Fragment createTabFragment(int menuId) {

        if (menuId == R.id.nav_incidents) {
            return new IncidentsFragment();
        }

        if (menuId == R.id.nav_roadworks) {
            return new RoadworksFragment();
        }

        if (menuId == R.id.nav_map) {
            return new MapFragment();
        }

        return new HomeFragment();
    }

    /**
     * Check whether the requested tab is already open.
     */
    private boolean isSameTabFragment(
            Fragment fragment,
            int menuId) {

        if (menuId == R.id.nav_home) {
            return fragment instanceof HomeFragment;
        }

        if (menuId == R.id.nav_incidents) {
            return fragment instanceof IncidentsFragment;
        }

        if (menuId == R.id.nav_roadworks) {
            return fragment instanceof RoadworksFragment;
        }

        if (menuId == R.id.nav_map) {
            return fragment instanceof MapFragment;
        }

        return false;
    }

    /**
     * Open a secondary screen such as Details.
     *
     * The originating tab is preserved and Android's
     * back stack returns the user to that screen.
     */
    public void navigateToFragment(Fragment fragment) {

        if (fragment == null) {
            return;
        }

        replaceFragment(
                fragment,
                true
        );
    }

    /**
     * Perform the Fragment transaction.
     */
    private void replaceFragment(
            Fragment fragment,
            boolean addToBackStack) {

        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

        transaction.replace(
                R.id.fragment_container,
                fragment
        );

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    /**
     * Called when the back stack changes.
     */
    private void updateNavigationOnBackStackChange() {

        Fragment currentFragment =
                getSupportFragmentManager()
                        .findFragmentById(
                                R.id.fragment_container
                        );

        if (currentFragment instanceof HomeFragment) {

            currentTabId = R.id.nav_home;

        } else if (currentFragment instanceof IncidentsFragment) {

            currentTabId = R.id.nav_incidents;

        } else if (currentFragment instanceof RoadworksFragment) {

            currentTabId = R.id.nav_roadworks;

        } else if (currentFragment instanceof MapFragment) {

            currentTabId = R.id.nav_map;
        }

        // For DetailsFragment, keep the originating tab.
        syncNavigationSelection(currentTabId);

        updateToolbarNavigation();
    }

    /**
     * Show a back arrow when displaying a secondary screen.
     */
    private void updateToolbarNavigation() {

        if (toolbar == null) {
            return;
        }

        boolean hasBackStack =
                getSupportFragmentManager()
                        .getBackStackEntryCount() > 0;

        if (hasBackStack) {

            toolbar.setTitle("Situation Details");

            Drawable backIcon =
                    AppCompatResources.getDrawable(
                            this,
                            androidx.appcompat.R.drawable
                                    .abc_ic_ab_back_material
                    );

            if (backIcon != null) {

                backIcon = DrawableCompat.wrap(
                        backIcon.mutate()
                );

                DrawableCompat.setTint(
                        backIcon,
                        ContextCompat.getColor(
                                this,
                                R.color.white
                        )
                );

                toolbar.setNavigationIcon(backIcon);
            }

            toolbar.setNavigationContentDescription(
                    "Back to previous screen"
            );

            toolbar.setNavigationOnClickListener(
                    view -> getOnBackPressedDispatcher()
                            .onBackPressed()
            );

        } else {

            toolbar.setTitle("Traffic Scotland");

            toolbar.setNavigationIcon(
                    (Drawable) null
            );

            toolbar.setNavigationOnClickListener(null);
        }
    }

    /**
     * Preserve the selected tab during rotation.
     */
    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putInt(
                KEY_ACTIVE_TAB,
                currentTabId
        );
    }
}
