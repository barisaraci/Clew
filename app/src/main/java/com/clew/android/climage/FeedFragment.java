package com.clew.android.climage;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.clew.android.R;
import com.clew.android.activities.HomeActivity;
import com.clew.android.climage.Climage;
import com.clew.android.climage.ClimageAdapter;
import com.clew.android.user.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.microsoft.windowsazure.mobileservices.ApiJsonOperationCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

public class FeedFragment extends Fragment {

    private Context context;
    private ClimageAdapter climageAdapter;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private SwipeRefreshLayout swipeLayout;
    private User user;
    private View layoutMain;

    private ArrayList<Climage> climageList;
    private boolean isLoading = true, noMoreClimage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        user = ((HomeActivity) getActivity()).getUser();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_feed, container, false);

        initLayout(rootView);

        return rootView;
    }

    private void initLayout(View rootView) {
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setScrollBarStyle(RecyclerView.SCROLLBARS_OUTSIDE_OVERLAY);

        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);

        climageList = new ArrayList<>();

        layoutMain = ((HomeActivity) context).getLayoutMain();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int pastVisiblesItems, visibleItemCount, totalItemCount;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    visibleItemCount = layoutManager.getChildCount();
                    totalItemCount = layoutManager.getItemCount();
                    pastVisiblesItems = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisiblesItems) >= totalItemCount && !isLoading && !noMoreClimage && totalItemCount >= 10) { // bunu static final yap
                        isLoading = true;
                        loadFeed(2);
                    }
                }
            }
        });


        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadFeed(1);
            }
        });

        loadFeed(0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_feed, menu);
        super.onCreateOptionsMenu(menu, inflater);

        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                final ArrayList<Climage> filteredModelList = filter(climageList, query);
                climageAdapter.animateTo(filteredModelList);
                recyclerView.scrollToPosition(0);
                return true;
            }
        });
    }

    private ArrayList<Climage> filter(ArrayList<Climage> models, String query) {
        query = query.toLowerCase();
        final ArrayList<Climage> filteredModelList = new ArrayList<>();
        for (Climage model : models) {
            final String text = model.getFullName().toLowerCase() + model.getTrace().toLowerCase() + model.getType();
            if (text.contains(query)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    private void loadFeed(int type) { // type 0 = opening, type 1 = refresh, type 2 = old
        /*List<Pair<String, String>> params = new ArrayList<>();

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        params.add(new Pair("id", user.getId()));
        params.add(new Pair("refreshDate", calendar.getTime().toString()));
        params.add(new Pair("refreshType", Integer.toString(type)));

        ((HomeActivity) context).getClient().invokeApi("feed", "POST", params, new ApiJsonOperationCallback() {
            @Override
            public void onCompleted(JsonElement jsonElement, Exception exception, ServiceFilterResponse response) {
                GsonBuilder gsonb = new GsonBuilder();
                Gson gson = gsonb.create();

                JsonArray array = jsonElement.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    climageList.add(gson.fromJson(array.get(i).getAsJsonObject().toString(), Climage.class));
                }

                climageAdapter = new ClimageAdapter(context, climageList);
                recyclerView.setAdapter(climageAdapter);

                isLoading = false;
                swipeLayout.setRefreshing(false);
            }
        });*/

        new AsyncTask<Void, Void, List>() {
            @Override
            protected List doInBackground(Void... params) {

                MobileServiceTable<Climage> climageTable = ((HomeActivity) context).getClient().getTable(Climage.class);
                List<Climage> results = null;

                try {
                    results = climageTable.orderBy("createdAt", QueryOrder.Descending).execute().get();
                } catch (ExecutionException | InterruptedException e) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    e.printStackTrace();
                }

                return results;
            }

            @Override
            protected void onPostExecute(List result) {
                climageList = (ArrayList<Climage>) result;

                climageAdapter = new ClimageAdapter(context, climageList);
                recyclerView.setAdapter(climageAdapter);

                isLoading = false;
                swipeLayout.setRefreshing(false);
            }
        }.execute();
    }
}
