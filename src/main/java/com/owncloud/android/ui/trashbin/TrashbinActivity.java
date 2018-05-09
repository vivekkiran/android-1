package com.owncloud.android.ui.trashbin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteTrashbinFolderOperation;
import com.owncloud.android.lib.resources.files.RemoveTrashbinFileOperation;
import com.owncloud.android.lib.resources.files.TrashbinFile;
import com.owncloud.android.operations.EmptyTrashbinFileOperation;
import com.owncloud.android.operations.RestoreTrashbinFileOperation;
import com.owncloud.android.ui.EmptyRecyclerView;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.TrashbinListAdapter;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.interfaces.TrashbinActivityInterface;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.owncloud.android.db.PreferenceManager.getSortOrder;

public class TrashbinActivity extends FileActivity implements TrashbinActivityInterface,
        SortingOrderDialogFragment.OnSortingOrderListener {// implements TrashbinInterface, TrashbinContract.View {

    @BindView(R.id.empty_list_view_text)
    public TextView emptyContentMessage;

    @BindView(R.id.empty_list_view_headline)
    public TextView emptyContentHeadline;

    @BindView(R.id.empty_list_icon)
    public ImageView emptyContentIcon;

    @BindView(android.R.id.list)
    public EmptyRecyclerView recyclerView;

    @BindView(R.id.swipe_containing_list)
    public SwipeRefreshLayout swipeListRefreshLayout;

    @BindString(R.string.trashbin_empty_headline)
    public String noResultsHeadline;

    @BindString(R.string.trashbin_empty_message)
    public String noResultsMessage;

    private Unbinder unbinder;
    private TrashbinListAdapter trashbinListAdapter;
    private String userId;
    private OwnCloudClient ownCloudClient;

    private String currentPath = "/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AccountManager accountManager = AccountManager.get(this);
        Account account = AccountUtils.getCurrentOwnCloudAccount(this);

        userId = accountManager.getUserData(account,
                com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

        try {
            OwnCloudAccount ocAccount = new OwnCloudAccount(account, this);
            ownCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, this);
        } catch (Exception e) {
            Log_OC.e(TAG, "Could not create an owncloudClient: " + e.getMessage());
        }

        setContentView(R.layout.trashbin_activity);
        unbinder = ButterKnife.bind(this);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_trashbin);

        ThemeUtils.setColoredTitle(getSupportActionBar(), R.string.trashbin_activity_title, this);

        setupContent();
    }

    private void setupContent() {
        recyclerView = findViewById(android.R.id.list);
        recyclerView.setEmptyView(findViewById(R.id.empty_list_view));
        findViewById(R.id.empty_list_progress).setVisibility(View.GONE);
        emptyContentIcon.setImageResource(R.drawable.ic_delete);
        emptyContentIcon.setVisibility(View.VISIBLE);
        emptyContentHeadline.setText(noResultsHeadline);
        emptyContentMessage.setText(noResultsMessage);
        emptyContentMessage.setVisibility(View.VISIBLE);

        trashbinListAdapter = new TrashbinListAdapter(this, getStorageManager(), this);
        recyclerView.setAdapter(trashbinListAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setHasFooter(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeListRefreshLayout.setOnRefreshListener(this::refresh);

        loadItems();
    }

    private void loadItems() {
        new Thread(() -> {
            ReadRemoteTrashbinFolderOperation readRemoteTrashbinFolderOperation =
                    new ReadRemoteTrashbinFolderOperation(currentPath, userId);

            try {
                RemoteOperationResult result = readRemoteTrashbinFolderOperation.execute(ownCloudClient);

                if (result.isSuccess()) {
                    runOnUiThread(() -> trashbinListAdapter.setTrashbinFiles(result.getData(), true));
                }
            } catch (Exception e) {
                Log_OC.e(TAG, e.getMessage());
            }
        }).start();

        swipeListRefreshLayout.setVisibility(View.VISIBLE);
        swipeListRefreshLayout.setRefreshing(false);
    }

    private void refresh() {
        loadItems();
        swipeListRefreshLayout.setRefreshing(false);
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent i = new Intent(getApplicationContext(), FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else if (!"/".equals(currentPath)) {
                    onBackPressed();
                } else {
                    openDrawer();
                }
                break;
            case R.id.action_sort: {
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.addToBackStack(null);

                SortingOrderDialogFragment mSortingOrderDialogFragment = SortingOrderDialogFragment.newInstance(
                        getSortOrder(this, null));
                mSortingOrderDialogFragment.show(ft, SortingOrderDialogFragment.SORTING_ORDER_FRAGMENT);

                break;
            }
            case R.id.action_empty_trashbin:
                new Thread(() -> {
                    EmptyTrashbinFileOperation emptyTrashbinFileOperation = new EmptyTrashbinFileOperation(userId);
                    RemoteOperationResult result = emptyTrashbinFileOperation.execute(ownCloudClient);

                    if (result.isSuccess()) {
                        loadItems();
                    } else {
                        // TODO notify user
                    }
                }).start();
            default:
                retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public void onOverflowIconClicked(TrashbinFile file, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.trashbin_actions_menu);

        popup.setOnMenuItemClickListener(item -> {
            new Thread(() -> {
                RemoveTrashbinFileOperation removeTrashbinFileOperation = new RemoveTrashbinFileOperation(file.getRemotePath());
                RemoteOperationResult result = removeTrashbinFileOperation.execute(ownCloudClient);

                if (result.isSuccess()) {
                    loadItems();
                } else {
                    // TODO notify user
                }
            }).start();

            Toast.makeText(this, "Delete: " + file.getFileName(), Toast.LENGTH_LONG).show();
            return true;
        });
        popup.show();
    }

    @Override
    public void onItemClicked(TrashbinFile file) {
        if (file.isFolder()) {
            currentPath = file.getRemotePath();
            loadItems();
            Toast.makeText(this, "Item clicked: " + file.getFileName(), Toast.LENGTH_LONG).show();

            mDrawerToggle.setDrawerIndicatorEnabled("/".equals(currentPath));

            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null && toolbar.getNavigationIcon() != null) {
                ThemeUtils.tintDrawable(toolbar.getNavigationIcon(), ThemeUtils.fontColor(this));
            }
        }
    }

    @Override
    public void onRestoreIconClicked(TrashbinFile file, View view) {
        new Thread(() -> {
            RestoreTrashbinFileOperation restoreTrashbinFileOperation = new RestoreTrashbinFileOperation(
                    file.getFullRemotePath(), file.getFileName(), userId);

            RemoteOperationResult result = restoreTrashbinFileOperation.execute(ownCloudClient);

            if (result.isSuccess()) {
                loadItems();
            }
        }).start();

        Toast.makeText(this, "Restore: " + file.getFileName(), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trashbin_options_menu, menu);

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        trashbinListAdapter.cancelAllPendingTasks();
    }

    @Override
    public void onBackPressed() {
        if ("/".equals(currentPath)) {
            openDrawer();
        } else {
            currentPath = new File(currentPath).getParent();
            loadItems();
        }

        mDrawerToggle.setDrawerIndicatorEnabled("/".equals(currentPath));
    }

    @Override
    public void onSortingOrderChosen(FileSortOrder sortOrder) {
        trashbinListAdapter.setSortOrder(sortOrder);
    }
}