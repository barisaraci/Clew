package com.clew.android.user;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.clew.android.R;
import com.clew.android.activities.FriendsActivity;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.microsoft.windowsazure.mobileservices.ApiJsonOperationCallback;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class UsersFragment extends Fragment {

    private Context context;
    private UserAdapter userAdapter;
    private MobileServiceClient client;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private User user;
    private View layoutMain;

    private ArrayList<User> userList;
    private int currentIndex = 0;
    private String type;
    private boolean isLoading = true, noMoreUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        user = ((FriendsActivity) context).getUser();
        client = ((FriendsActivity) context).getClient();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_users, container, false);
        type = getArguments().getString("userType");
        initLayout(rootView);

        return rootView;
    }

    private void initLayout(View rootView) {
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setScrollBarStyle(RecyclerView.SCROLLBARS_OUTSIDE_OVERLAY);

        userList = new ArrayList<>();

        layoutMain = ((FriendsActivity) context).getLayoutMain();
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

                    if ((visibleItemCount + pastVisiblesItems) >= totalItemCount && !isLoading && !noMoreUser && totalItemCount >= 10) { // bunu static final yap
                        isLoading = true;
                        loadUsers(currentIndex, 10);
                    }
                }
            }
        });

        if (!type.equals("search"))
            loadUsers(currentIndex, 10);
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
                if (type.equals("search"))
                    loadUsers(0, 10, query);

                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (!type.equals("search")) {
                    final ArrayList<User> filteredModelList = filter(userList, query);
                    userAdapter.animateTo(filteredModelList);
                    recyclerView.scrollToPosition(0);
                }
                return true;
            }
        });
    }

    private ArrayList<User> filter(ArrayList<User> models, String query) {
        query = query.toLowerCase();
        final ArrayList<User> filteredModelList = new ArrayList<>();
        for (User model : models) {
            final String text = model.getUserName().toLowerCase() + model.getFullName().toLowerCase();
            if (text.contains(query)) {
                filteredModelList.add(model);
            }
        }

        return filteredModelList;
    }

    private void loadUsers(int skip, int top) {
        List<Pair<String, String>> params = new ArrayList<>();

        params.add(new Pair("id", user.getId()));
        /*params.add(new Pair("skip", Integer.toString(skip)));
        params.add(new Pair("top", Integer.toString(top)));*/

        client.invokeApi(type, "POST", params, new ApiJsonOperationCallback() {
            @Override
            public void onCompleted(JsonElement jsonElement, Exception exception, ServiceFilterResponse response) {
                GsonBuilder gsonb = new GsonBuilder();
                Gson gson = gsonb.create();

                JsonArray array = jsonElement.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    userList.add(gson.fromJson(array.get(i).getAsJsonObject().toString(), User.class));
                }

                userAdapter = new UserAdapter(context, userList);
                recyclerView.setAdapter(userAdapter);

                isLoading = false;
            }
        });

        /*ListenableFuture<JsonElement> result = client.invokeApi(type, "POST", params, JsonElement.class);

        Futures.addCallback(result, new FutureCallback<JsonElement>() {

            @Override
            public void onFailure(Throwable exc) {
                Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();

                Log.e("Clew: ", exc.toString());
            }

            @Override
            public void onSuccess(JsonElement result) {
                GsonBuilder gsonb = new GsonBuilder();
                Gson gson = gsonb.create();

                JsonArray array = result.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    userList.add(gson.fromJson(array.get(i).getAsJsonObject().toString(), User.class));
                }

                userAdapter = new UserAdapter(context, userList);
                recyclerView.setAdapter(userAdapter);

                isLoading = false;
            }

        });*/
    }

    private void loadUsers(final int skip, final int top, final String query) {
        new AsyncTask<Void, Void, List>() {
            @Override
            protected List doInBackground(Void... params) {
                MobileServiceTable<User> userTable = ((FriendsActivity) context).getClient().getTable(User.class);
                List<User> results = null;

                try {
                    results = userTable.where().field("userName").eq(query).or().field("fullName").eq(query).skip(skip).top(top).orderBy("fullName", QueryOrder.Descending).execute().get();
                } catch (ExecutionException | InterruptedException e) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    e.printStackTrace();
                }

                return results;
            }

            @Override
            protected void onPostExecute(List result) {
                userList = (ArrayList<User>) result;

                userAdapter = new UserAdapter(context, userList);
                recyclerView.setAdapter(userAdapter);

                isLoading = false;
            }
        }.execute();
    }

}
