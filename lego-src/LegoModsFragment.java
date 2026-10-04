package net.kdt.pojavlaunch.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LruCache;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LegoLauncher: Modrinth API (v2) orqali mod qidirish, o'rnatish (kerakli bog'liqliklar bilan)
 * va o'rnatilgan modlarni boshqarish. Modlar joriy profilning "mods" papkasiga yuklanadi.
 */
public class LegoModsFragment extends Fragment {
    public static final String TAG = "LegoModsFragment";

    private static final String API = "https://api.modrinth.com/v2";
    private static final String UA = "LegoLauncher/1.0 (Android Minecraft launcher)";
    private static final String[] LOADER_IDS = {"fabric", "forge", "neoforge", "quilt"};
    private static final String[] LOADER_NAMES = {"Fabric", "Forge", "NeoForge", "Quilt"};
    private static final int PAGE = 20;

    private static final ExecutorService NET = Executors.newFixedThreadPool(3);
    private static final ExecutorService IMG = Executors.newFixedThreadPool(2);
    private static final LruCache<String, Bitmap> ICONS = new LruCache<>(80);

    private final Handler mUi = new Handler(Looper.getMainLooper());
    private final List<Row> mRows = new ArrayList<>();
    private final Set<String> mBusy = new HashSet<>();

    private RecyclerView mList;
    private RowAdapter mAdapter;
    private EditText mQuery, mMcVer;
    private Spinner mLoader;
    private View mFilters;
    private Button mTabSearch, mTabInstalled;
    private TextView mStatus, mTarget;
    private ProgressBar mProgress;

    private File mModsDir;
    private boolean mInstalledMode = false;
    private boolean mLoading = false, mEnd = false;
    private int mOffset = 0, mToken = 0;

    private final Runnable mDebounced = this::refresh;

    private static class Row {
        String projectId, title, desc, icon, meta;
        File file;
    }

    public LegoModsFragment() {
        super(R.layout.fragment_lego_mods);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mList = view.findViewById(R.id.lego_mods_list);
        mQuery = view.findViewById(R.id.lego_mods_query);
        mMcVer = view.findViewById(R.id.lego_mods_mcver);
        mLoader = view.findViewById(R.id.lego_mods_loader);
        mFilters = view.findViewById(R.id.lego_mods_filters);
        mTabSearch = view.findViewById(R.id.lego_tab_search);
        mTabInstalled = view.findViewById(R.id.lego_tab_installed);
        mStatus = view.findViewById(R.id.lego_mods_status);
        mTarget = view.findViewById(R.id.lego_mods_target);
        mProgress = view.findViewById(R.id.lego_mods_progress);

        // Joriy profil -> mods papkasi, MC versiya va loader
        String profileName = LauncherPreferences.DEFAULT_PREF
                .getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        MinecraftProfile profile = null;
        if (Tools.isValidString(profileName)) {
            LauncherProfiles.load();
            profile = LauncherProfiles.mainProfileJson.profiles.get(profileName);
        }
        File gameDir = profile == null ? new File(Tools.DIR_GAME_NEW) : Tools.getGameDirPath(profile);
        mModsDir = new File(gameDir, "mods");
        mTarget.setText(((profileName == null ? "-" : profileName)) + "  ›  " + mModsDir.getAbsolutePath());

        String versionId = profile == null || profile.lastVersionId == null ? "" : profile.lastVersionId;
        mMcVer.setText(detectMcVersion(versionId));

        ArrayAdapter<String> loaderAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, LOADER_NAMES);
        loaderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLoader.setAdapter(loaderAdapter);
        mLoader.setSelection(detectLoader(versionId));

        mAdapter = new RowAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        mList.setLayoutManager(lm);
        mList.setAdapter(mAdapter);
        mList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!mInstalledMode && !mLoading && !mEnd
                        && lm.findLastVisibleItemPosition() >= mRows.size() - 4) loadMore();
            }
        });

        mTabSearch.setOnClickListener(v -> setMode(false));
        mTabInstalled.setOnClickListener(v -> setMode(true));

        TextView.OnEditorActionListener go = (v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (ev != null && ev.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                mUi.removeCallbacks(mDebounced);
                refresh();
                return true;
            }
            return false;
        };
        mQuery.setOnEditorActionListener(go);
        mMcVer.setOnEditorActionListener(go);
        mQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                mUi.removeCallbacks(mDebounced);
                mUi.postDelayed(mDebounced, 450);
            }
        });
        mLoader.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean first = true;
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (first) { first = false; return; }
                refresh();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        updateTabs();
        refresh();
    }

    @Override
    public void onDestroyView() {
        mUi.removeCallbacksAndMessages(null);
        mToken++; // eski so'rovlar natijasini e'tiborsiz qoldirish
        super.onDestroyView();
    }

    // ------------------------------------------------------------------ rejim / yangilash

    private void setMode(boolean installed) {
        if (mInstalledMode == installed) return;
        mInstalledMode = installed;
        updateTabs();
        refresh();
    }

    private void updateTabs() {
        mTabSearch.setAlpha(mInstalledMode ? 0.55f : 1f);
        mTabInstalled.setAlpha(mInstalledMode ? 1f : 0.55f);
        mFilters.setVisibility(mInstalledMode ? View.GONE : View.VISIBLE);
    }

    private void refresh() {
        if (!isAdded() || getView() == null) return;
        mToken++;
        mOffset = 0;
        mEnd = false;
        mLoading = false;
        mRows.clear();
        mAdapter.notifyDataSetChanged();
        mStatus.setVisibility(View.GONE);
        loadMore();
    }

    private void loadMore() {
        if (mInstalledMode) listInstalled(); else search();
    }

    // ------------------------------------------------------------------ o'rnatilgan modlar

    private void listInstalled() {
        String q = mQuery.getText().toString().trim().toLowerCase(Locale.ROOT);
        File[] files = mModsDir.listFiles();
        mRows.clear();
        if (files != null) {
            Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File f : files) {
                String n = f.getName();
                if (!(n.endsWith(".jar") || n.endsWith(".jar.disabled"))) continue;
                if (!q.isEmpty() && !n.toLowerCase(Locale.ROOT).contains(q)) continue;
                Row r = new Row();
                r.file = f;
                r.title = n;
                r.desc = "";
                r.meta = (f.length() / 1024) + " KB";
                mRows.add(r);
            }
        }
        mEnd = true;
        mAdapter.notifyDataSetChanged();
        showEmptyIfNeeded("Hozircha mod o'rnatilmagan");
    }

    // ------------------------------------------------------------------ qidirish

    private void search() {
        if (mLoading || mEnd) return;
        mLoading = true;
        mProgress.setVisibility(View.VISIBLE);
        final int token = mToken;
        final String q = mQuery.getText().toString().trim();
        final String mc = mMcVer.getText().toString().trim();
        final String loader = LOADER_IDS[Math.max(0, mLoader.getSelectedItemPosition())];
        final int off = mOffset;

        NET.execute(() -> {
            try {
                StringBuilder facets = new StringBuilder("[[\"project_type:mod\"]");
                if (!mc.isEmpty()) facets.append(",[\"versions:").append(mc).append("\"]");
                facets.append(",[\"categories:").append(loader).append("\"]]");
                String url = API + "/search?query=" + enc(q) + "&facets=" + enc(facets.toString())
                        + "&limit=" + PAGE + "&offset=" + off
                        + "&index=" + (q.isEmpty() ? "downloads" : "relevance");
                JsonObject o = getJson(url).getAsJsonObject();
                JsonArray hits = o.getAsJsonArray("hits");
                int total = o.has("total_hits") ? o.get("total_hits").getAsInt() : 0;
                List<Row> got = new ArrayList<>();
                for (JsonElement el : hits) {
                    JsonObject h = el.getAsJsonObject();
                    Row r = new Row();
                    r.projectId = str(h, "project_id");
                    r.title = str(h, "title");
                    r.desc = str(h, "description");
                    r.icon = str(h, "icon_url");
                    r.meta = str(h, "author") + "  •  " + fmt(h.has("downloads") ? h.get("downloads").getAsLong() : 0) + " yuklash";
                    got.add(r);
                }
                mUi.post(() -> {
                    if (token != mToken || !isAdded()) return;
                    int start = mRows.size();
                    mRows.addAll(got);
                    mAdapter.notifyItemRangeInserted(start, got.size());
                    mOffset = off + got.size();
                    mEnd = got.isEmpty() || mOffset >= total;
                    mLoading = false;
                    mProgress.setVisibility(View.GONE);
                    showEmptyIfNeeded("Hech narsa topilmadi");
                });
            } catch (Exception e) {
                mUi.post(() -> {
                    if (token != mToken || !isAdded()) return;
                    mLoading = false;
                    mProgress.setVisibility(View.GONE);
                    if (mRows.isEmpty()) {
                        mStatus.setText("Internet/Modrinth xatosi:\n" + e.getMessage());
                        mStatus.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void showEmptyIfNeeded(String msg) {
        mProgress.setVisibility(View.GONE);
        if (mRows.isEmpty()) {
            mStatus.setText(msg);
            mStatus.setVisibility(View.VISIBLE);
        } else mStatus.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------ o'rnatish

    private void install(Row row) {
        if (!mBusy.add(row.projectId)) return;
        mAdapter.notifyDataSetChanged();
        final String mc = mMcVer.getText().toString().trim();
        final String loader = LOADER_IDS[Math.max(0, mLoader.getSelectedItemPosition())];
        final File dir = mModsDir;

        NET.execute(() -> {
            List<String> done = new ArrayList<>();
            String error = null;
            try {
                installProject(row.projectId, null, mc, loader, new HashSet<>(), 0, dir, done);
            } catch (Exception e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            final String err = error;
            mUi.post(() -> {
                mBusy.remove(row.projectId);
                if (!isAdded()) return;
                mAdapter.notifyDataSetChanged();
                String msg = err != null ? ("Xato: " + err)
                        : ("O'rnatildi: " + android.text.TextUtils.join(", ", done));
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void installProject(String projectId, String versionId, String mc, String loader,
                                Set<String> visited, int depth, File dir, List<String> done) throws Exception {
        if (!visited.add(projectId)) return;

        JsonObject ver;
        if (versionId != null && !versionId.isEmpty()) {
            ver = getJson(API + "/version/" + enc(versionId)).getAsJsonObject();
        } else {
            String url = API + "/project/" + enc(projectId) + "/version?loaders=" + enc("[\"" + loader + "\"]");
            if (!mc.isEmpty()) url += "&game_versions=" + enc("[\"" + mc + "\"]");
            JsonArray arr = getJson(url).getAsJsonArray();
            if (arr.size() == 0) throw new IOException("Bu versiya/loader uchun mos fayl yo'q (" + loader + " " + mc + ")");
            ver = arr.get(0).getAsJsonObject();
            for (JsonElement e : arr) { // avval barqaror "release"
                JsonObject v = e.getAsJsonObject();
                if ("release".equals(str(v, "version_type"))) { ver = v; break; }
            }
        }

        JsonArray files = ver.getAsJsonArray("files");
        if (files == null || files.size() == 0) throw new IOException("Faylsiz versiya");
        JsonObject file = files.get(0).getAsJsonObject();
        for (JsonElement e : files) {
            JsonObject f = e.getAsJsonObject();
            if (f.has("primary") && f.get("primary").getAsBoolean()) { file = f; break; }
        }
        String name = new File(str(file, "filename")).getName();
        if (!name.endsWith(".jar")) throw new IOException("Kutilmagan fayl turi: " + name);
        String sha1 = null;
        if (file.has("hashes") && file.getAsJsonObject("hashes").has("sha1"))
            sha1 = file.getAsJsonObject("hashes").get("sha1").getAsString();

        download(str(file, "url"), new File(dir, name), sha1);
        done.add(name);

        // Majburiy bog'liqliklar (masalan Fabric API)
        if (depth < 3 && ver.has("dependencies") && ver.get("dependencies").isJsonArray()) {
            for (JsonElement e : ver.getAsJsonArray("dependencies")) {
                JsonObject d = e.getAsJsonObject();
                if (!"required".equals(str(d, "dependency_type"))) continue;
                String pid = str(d, "project_id");
                if (pid.isEmpty()) continue;
                try {
                    installProject(pid, str(d, "version_id"), mc, loader, visited, depth + 1, dir, done);
                } catch (Exception ex) {
                    done.add("(bog'liqlik o'tkazib yuborildi: " + pid + ")");
                }
            }
        }
    }

    private static void download(String urlStr, File dest, String sha1) throws Exception {
        File parent = dest.getParentFile();
        if (parent != null) parent.mkdirs();
        File part = new File(dest.getPath() + ".part");
        HttpURLConnection c = open(urlStr);
        try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(part)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) { out.write(buf, 0, n); md.update(buf, 0, n); }
            if (sha1 != null) {
                StringBuilder hex = new StringBuilder();
                for (byte b : md.digest()) hex.append(String.format("%02x", b));
                if (!hex.toString().equalsIgnoreCase(sha1)) {
                    part.delete();
                    throw new IOException("SHA-1 mos kelmadi: " + dest.getName());
                }
            }
        } catch (Exception e) {
            part.delete();
            throw e;
        } finally {
            c.disconnect();
        }
        if (dest.exists()) dest.delete();
        if (!part.renameTo(dest)) throw new IOException("Faylni saqlab bo'lmadi: " + dest.getName());
    }

    // ------------------------------------------------------------------ yordamchilar

    private static HttpURLConnection open(String urlStr) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        int code = c.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code);
        return c;
    }

    private static JsonElement getJson(String url) throws IOException {
        HttpURLConnection c = open(url);
        try (InputStream in = c.getInputStream()) {
            return Tools.GLOBAL_GSON.fromJson(Tools.read(in), JsonElement.class);
        } finally {
            c.disconnect();
        }
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return (e == null || e.isJsonNull()) ? "" : e.getAsString();
    }

    private static String fmt(long n) {
        if (n >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", n / 1_000_000f);
        if (n >= 1_000) return String.format(Locale.ROOT, "%.1fK", n / 1_000f);
        return String.valueOf(n);
    }

    private static String detectMcVersion(String versionId) {
        Matcher m = Pattern.compile("\\b1\\.(\\d{1,2})(?:\\.(\\d+))?\\b").matcher(versionId);
        return m.find() ? m.group() : "";
    }

    private static int detectLoader(String versionId) {
        String v = versionId.toLowerCase(Locale.ROOT);
        if (v.contains("neoforge")) return 2;
        if (v.contains("forge")) return 1;
        if (v.contains("quilt")) return 3;
        return 0; // fabric (standart)
    }

    private void loadIcon(ImageView iv, String url) {
        iv.setTag(url);
        iv.setImageResource(R.drawable.lego_brick_yellow);
        if (url == null || url.isEmpty()) return;
        Bitmap cached = ICONS.get(url);
        if (cached != null) { iv.setImageBitmap(cached); return; }
        IMG.execute(() -> {
            try {
                HttpURLConnection c = open(url);
                Bitmap bmp;
                try (InputStream in = c.getInputStream()) { bmp = BitmapFactory.decodeStream(in); }
                finally { c.disconnect(); }
                if (bmp == null) return;
                if (bmp.getWidth() > 128) bmp = Bitmap.createScaledBitmap(bmp, 128, 128, true);
                final Bitmap fb = bmp;
                ICONS.put(url, fb);
                mUi.post(() -> { if (url.equals(iv.getTag())) iv.setImageBitmap(fb); });
            } catch (Exception ignored) { }
        });
    }

    // ------------------------------------------------------------------ adapter

    private class RowAdapter extends RecyclerView.Adapter<RowAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView title, desc, meta;
            final Button action;
            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.lego_mod_icon);
                title = v.findViewById(R.id.lego_mod_title);
                desc = v.findViewById(R.id.lego_mod_desc);
                meta = v.findViewById(R.id.lego_mod_meta);
                action = v.findViewById(R.id.lego_mod_action);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lego_mod, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Row r = mRows.get(position);
            h.title.setText(r.title);
            h.desc.setText(r.desc);
            h.desc.setVisibility(r.desc == null || r.desc.isEmpty() ? View.GONE : View.VISIBLE);
            h.meta.setText(r.meta);
            if (mInstalledMode) {
                h.icon.setTag(null);
                h.icon.setImageResource(R.drawable.lego_brick_green);
                h.action.setEnabled(true);
                h.action.setText(R.string.lego_delete);
                h.action.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                        .setMessage(r.title + " — o'chirilsinmi?")
                        .setPositiveButton(R.string.lego_delete, (d, w) -> {
                            if (r.file != null && r.file.delete()) {
                                int idx = mRows.indexOf(r);
                                if (idx >= 0) { mRows.remove(idx); notifyItemRemoved(idx); }
                                showEmptyIfNeeded("Hozircha mod o'rnatilmagan");
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show());
            } else {
                loadIcon(h.icon, r.icon);
                boolean busy = mBusy.contains(r.projectId);
                h.action.setEnabled(!busy);
                h.action.setText(busy ? "..." : getString(R.string.lego_install));
                h.action.setOnClickListener(v -> install(r));
            }
        }

        @Override
        public int getItemCount() { return mRows.size(); }
    }
}
