package com.clew.android.trace;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.clew.android.R;
import com.clew.android.activities.HomeActivity;
import com.clew.android.trace.Trace;
import com.clew.android.trace.TraceAdapter;
import com.clew.android.user.User;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TraceFragment extends Fragment {

    private Context context;
    private TraceAdapter traceAdapter;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private SwipeRefreshLayout swipeLayout;
    private User user;
    private View layoutMain;

    private ArrayList<Trace> traceList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        user = ((HomeActivity) getActivity()).getUser();

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_trace, container, false);

        initLayout(rootView);

        return rootView;
    }

    private void initLayout(View rootView) {
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setScrollBarStyle(RecyclerView.SCROLLBARS_OUTSIDE_OVERLAY);

        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);

        traceList = new ArrayList<>();

        layoutMain = ((HomeActivity) context).getLayoutMain();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadTraces(0);
            }
        });

        loadTraces(0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_snap, menu);
        super.onCreateOptionsMenu(menu, inflater);

        /*final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                final ArrayList<Event> filteredModelList = filter(eventList, query);
                eventAdapter.animateTo(filteredModelList);
                recyclerView.scrollToPosition(0);
                return true;
            }
        });*/
    }

    private void loadTraces(int type) { // type 0 = trend, type 1 = most post, type 2 = new
        new AsyncTask<Void, Void, List>() {
            @Override
            protected List doInBackground(Void... params) {
                /*MobileServiceJsonTable actionTable = ((HomeActivity) context).getClient().getTable("Action");
                JsonElement result = null;
                JsonArray results = null;
                try {
                    result = actionTable.execute().get();
                    results = result.getAsJsonArray();
                } catch (InterruptedException | ExecutionException | MobileServiceException e) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    e.printStackTrace();
                }*/

                MobileServiceTable<Trace> traceTable = ((HomeActivity) context).getClient().getTable(Trace.class);
                List<Trace> results = null;

                try {
                    results = traceTable.orderBy("createdAt", QueryOrder.Descending).execute().get();
                } catch (ExecutionException | InterruptedException e) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    e.printStackTrace();
                }

                return results;
            }

            @Override
            protected void onPostExecute(List result) {
                traceList = (ArrayList<Trace>) result;

                traceAdapter = new TraceAdapter(context, traceList);
                recyclerView.setAdapter(traceAdapter);

                swipeLayout.setRefreshing(false);
            }
        }.execute();
    }
}
