package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.LegoAnim;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** LegoLauncher bosh sahifa: o'rnatilgan versiyalar (overworld rasmlari bilan) va O'YNASH. */
public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private static final int[] WORLDS = {
            R.drawable.lego_world_plains, R.drawable.lego_world_forest, R.drawable.lego_world_desert,
            R.drawable.lego_world_snow, R.drawable.lego_world_mountains};

    private mcVersionSpinner mVersionSpinner; // ko'rinmaydi: Pojav mantig'i (yangi profil tanlovi) uchun
    private final List<String> mKeys = new ArrayList<>();
    private String mSelected = "";
    private ProfileAdapter mAdapter;
    private ImageView mHeroImage;
    private TextView mHeroName, mHeroVersion;
    private Button mPlayButton;

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mNewsButton = view.findViewById(R.id.news_button);
        Button mModsButton = view.findViewById(R.id.mods_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);
        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);
        mHeroImage = view.findViewById(R.id.lego_hero_image);
        mHeroName = view.findViewById(R.id.lego_hero_name);
        mHeroVersion = view.findViewById(R.id.lego_hero_version);

        float d = getResources().getDisplayMetrics().density;
        LegoAnim.round(view.findViewById(R.id.lego_hero), 14 * d);

        RecyclerView list = view.findViewById(R.id.lego_profiles_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        mAdapter = new ProfileAdapter();
        list.setAdapter(mAdapter);

        mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        mNewsButton.setOnLongClickListener(v -> {
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });
        mModsButton.setOnClickListener(v -> Tools.swapFragment(requireActivity(), LegoModsFragment.class, LegoModsFragment.TAG, null));
        mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation(false));
        mInstallJarButton.setOnLongClickListener(v -> {
            runInstallerWithConfirmation(true);
            return true;
        });
        mEditProfileButton.setOnClickListener(v -> editSelected());
        mPlayButton.setOnClickListener(v -> {
            if (mKeys.isEmpty()) { createProfile(); return; }
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        });
        mShareLogsButton.setOnClickListener(v -> shareLog(requireContext()));
        mOpenDirectoryButton.setOnClickListener(v -> {
            Tools.switchDemo(Tools.isDemoProfile(v.getContext())); // avoid switching accounts being able to access
            if (Tools.isDemoProfile(v.getContext())) {
                Toast.makeText(v.getContext(), R.string.toast_not_available_demo, Toast.LENGTH_LONG).show();
                return;
            }
            openPath(v.getContext(), getCurrentProfileDirectory(), false);
        });

        reloadList();
        LegoAnim.start(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        mVersionSpinner.reloadProfiles();
        reloadList();
    }

    // ------------------------------------------------------------------ profillar

    private void createProfile() {
        Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null);
    }

    private void editSelected() {
        if (mKeys.isEmpty()) { createProfile(); return; }
        saveSelection(mSelected);
        Tools.swapFragment(requireActivity(), ProfileEditorFragment.class, ProfileEditorFragment.TAG, null);
    }

    private void saveSelection(String key) {
        mSelected = key;
        LauncherPreferences.DEFAULT_PREF.edit()
                .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).apply();
    }

    private MinecraftProfile profileOf(String key) {
        if (LauncherProfiles.mainProfileJson == null) return null;
        return LauncherProfiles.mainProfileJson.profiles.get(key);
    }

    private String displayName(String key) {
        MinecraftProfile p = profileOf(key);
        if (p != null && Tools.isValidString(p.name)) return p.name;
        if (p != null && Tools.isValidString(p.lastVersionId)) return versionText(p);
        return key;
    }

    private String versionText(MinecraftProfile p) {
        String v = p == null ? null : p.lastVersionId;
        if (!Tools.isValidString(v)) return "-";
        if (MinecraftProfile.LATEST_RELEASE.equals(v)) return getString(R.string.lego_latest_release);
        if (MinecraftProfile.LATEST_SNAPSHOT.equals(v)) return getString(R.string.lego_latest_snapshot);
        return v;
    }

    private static int worldFor(String key, MinecraftProfile p) {
        String v = p != null && p.lastVersionId != null ? p.lastVersionId.toLowerCase(Locale.ROOT) : "";
        if (v.contains("fabric")) return WORLDS[1];
        if (v.contains("forge")) return WORLDS[4];   // neoforge ham
        if (v.contains("quilt")) return WORLDS[3];
        if (v.contains("optifine")) return WORLDS[2];
        int[] pool = {0, 2, 3, 1, 4};
        return WORLDS[pool[Math.abs(key.hashCode()) % pool.length]];
    }

    private void reloadList() {
        LauncherProfiles.load();
        mKeys.clear();
        if (LauncherProfiles.mainProfileJson != null)
            mKeys.addAll(LauncherProfiles.mainProfileJson.profiles.keySet());
        Collections.sort(mKeys, (a, b) -> displayName(a).compareToIgnoreCase(displayName(b)));

        String cur = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
        if (!mKeys.contains(cur)) {
            cur = mKeys.isEmpty() ? "" : mKeys.get(0);
            if (!cur.isEmpty()) saveSelection(cur);
        }
        mSelected = cur;
        mAdapter.notifyDataSetChanged();
        updateHero(false);
    }

    private void updateHero(boolean animate) {
        MinecraftProfile p = profileOf(mSelected);
        if (p == null) {
            mHeroName.setText(R.string.lego_no_profile);
            mHeroVersion.setText(R.string.lego_new_profile);
            mHeroImage.setImageResource(WORLDS[0]);
            return;
        }
        mHeroName.setText(displayName(mSelected));
        mHeroVersion.setText(versionText(p));
        mHeroImage.setImageResource(worldFor(mSelected, p));
        if (animate) {
            mHeroImage.setAlpha(0.3f);
            mHeroImage.animate().alpha(1f).setDuration(280).start();
        }
    }

    private File getCurrentProfileDirectory() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if(!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if(profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        // avoid using custom installers to install a version
        if(Tools.isLocalProfile(requireContext()) || Tools.isDemoProfile(requireContext())){
            Toast.makeText(requireContext(), R.string.toast_not_available_demo, Toast.LENGTH_LONG).show();
            return;
        }
        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    // ------------------------------------------------------------------ ro'yxat adapteri

    private class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final View root;
            final ImageView icon;
            final TextView name, ver;
            VH(View v) {
                super(v);
                root = v.findViewById(R.id.lego_prof_root);
                icon = v.findViewById(R.id.lego_prof_icon);
                name = v.findViewById(R.id.lego_prof_name);
                ver = v.findViewById(R.id.lego_prof_ver);
                LegoAnim.round(icon, 8 * v.getResources().getDisplayMetrics().density);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lego_profile, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            if (position == mKeys.size()) { // oxirgi qator: yangi profil
                h.icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                h.icon.setImageResource(R.drawable.ic_add);
                h.name.setText(R.string.lego_new_profile);
                h.ver.setText("+");
                h.root.setBackgroundResource(R.drawable.lego_mod_item_bg);
                h.root.setOnClickListener(v -> createProfile());
                h.root.setOnLongClickListener(null);
                return;
            }
            final String key = mKeys.get(position);
            MinecraftProfile p = profileOf(key);
            h.icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            h.icon.setImageResource(worldFor(key, p));
            h.name.setText(displayName(key));
            h.ver.setText(versionText(p));
            h.root.setBackgroundResource(key.equals(mSelected) ? R.drawable.lego_profile_sel : R.drawable.lego_mod_item_bg);
            h.root.setOnClickListener(v -> {
                saveSelection(key);
                notifyDataSetChanged();
                updateHero(true);
            });
            h.root.setOnLongClickListener(v -> {
                saveSelection(key);
                editSelected();
                return true;
            });
        }

        @Override
        public int getItemCount() { return mKeys.size() + 1; }
    }
}
