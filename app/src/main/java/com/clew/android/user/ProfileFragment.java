package com.clew.android.user;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.clew.android.R;
import com.clew.android.activities.FriendsActivity;
import com.clew.android.activities.HomeActivity;
import com.clew.android.activities.EditUserActivity;
import com.clew.android.climage.ClimAdapter;
import com.clew.android.climage.Climage;
import com.clew.android.climage.ClimageAdapter;
import com.clew.android.trace.Trace;
import com.clew.android.trace.TraceAdapter;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ProfileFragment extends Fragment {

    private Context context;
    private User user;

    private ClimAdapter climAdapter;

    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;
    private SwipeRefreshLayout swipeLayout;
    private View layoutMain;
    private TextView tvUsername, tvFullname, tvPosts, tvLikes, tvFollowers, tvFollowing;
    private ImageView profileImage;

    private ArrayList<Climage> climageList;
    private boolean isLoading = true, noMoreClimage;

    private final String BLOB_DIRECTORY = "https://clew.blob.core.windows.net";
    private final String CONTAINER_REFERENCE = "profilephotos"; // bunları aktivitiye al hatta direk main aktivitiye koy hepsinde ordan al

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        user = ((HomeActivity) getActivity()).getUser();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        initLayout(rootView);

        return rootView;
    }

    private void initLayout(View rootView) {
        tvUsername = (TextView) rootView.findViewById(R.id.tvUsername);
        tvFullname = (TextView) rootView.findViewById(R.id.tvFullname);
        tvPosts = (TextView) rootView.findViewById(R.id.tvPosts);
        tvLikes = (TextView) rootView.findViewById(R.id.tvLikes);
        tvFollowers = (TextView) rootView.findViewById(R.id.tvFollowers);
        tvFollowing = (TextView) rootView.findViewById(R.id.tvFollowing);

        tvUsername.setText(user.getUserName());
        tvFullname.setText(user.getFullName());
        tvPosts.setText(Integer.toString(user.getPosts()));
        tvLikes.setText(Integer.toString(user.getLikes()));
        tvFollowers.setText(Integer.toString(user.getFollowers()));
        tvFollowing.setText(Integer.toString(user.getFollowing()));

        profileImage = (ImageView) rootView.findViewById(R.id.imageProfile);

        Picasso.with(context)
                .load(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE + "/" + user.getId() + "_thumbnail")
                .error(android.R.drawable.ic_dialog_alert)
                .into(profileImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        Picasso.with(context)
                                .load(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE + "/" + user.getId() + "_main")
                                .error(android.R.drawable.ic_dialog_alert)
                                .into(profileImage);
                    }
                    @Override
                    public void onError() {

                    }
                });

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        layoutManager = new GridLayoutManager(context, 3);
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

                    if ((visibleItemCount + pastVisiblesItems) >= totalItemCount && !isLoading && !noMoreClimage && totalItemCount >= 15) { // bunu static final yap
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
        inflater.inflate(R.menu.menu_profile, menu);
        super.onCreateOptionsMenu(menu, inflater);

        final MenuItem itemEditUser = menu.findItem(R.id.action_edit_profile);
        itemEditUser.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(context, EditUserActivity.class); // burda userı aktar edituserda get user yapmasın tekrar
                startActivity(intent);
                return false;
            }
        });

        final MenuItem itemUsers = menu.findItem(R.id.action_friends);
        itemUsers.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(context, FriendsActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
                return false;
            }
        });
    }

    private void loadFeed(int type) { // type 0 = opening, type 1 = refresh, type 2 = old
        /*List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();

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
            }
        });*/
        /*String userID = preferences.getString("userid", null);
        String refreshDate = "0";

        if(refType == 0) {
            refreshDate = "0";
        } else if(refType == 1) {
            refreshDate = preferences.getString("refreshdate", null);
        } else if(refType == 2) {
            refreshDate = preferences.getString("lasteventdate", null);
        }

        JSONObject postObj = new JSONObject();

        postObj.put("userID", userID);
        postObj.put("refreshDate", refreshDate);
        postObj.put("refreshType", refType);*/

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

                climAdapter = new ClimAdapter(context, climageList);
                recyclerView.setAdapter(climAdapter);

                isLoading = false;
                swipeLayout.setRefreshing(false);
            }
        }.execute();

        /*JsonObjectRequest sr = new JsonObjectRequest(Request.Method.POST, "http://www.berkugudur.com/narr3/query.php?action=newsfeed", postObj, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject result) {
                SharedPreferences.Editor editor = preferences.edit();

                try {
                    if(!result.getJSONObject("generalInfo").getBoolean("newEvent")) {
                        Snackbar.make(getView(), context.getResources().getString(R.string.noNewEvent), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        if(refType == 2) { noMoreQuestion = true; }
                    } else {
                        String refreshDate = result.getJSONObject("generalInfo").getString("refreshDate");
                        String lastEventDate = result.getJSONObject("generalInfo").getString("lastEventDate");

                        long refreshDateNumber = result.getJSONObject("generalInfo").getLong("refreshDateNumber");

                        if(refType == 0) {
                            editor.putString("refreshdate", refreshDate);
                            editor.putString("lasteventdate", lastEventDate);
                            editor.apply();
                        } else if(refType == 1) {
                            editor.putString("refreshdate", refreshDate);
                            editor.apply();
                        } else if(refType == 2) {
                            editor.putString("lasteventdate", lastEventDate);
                            editor.apply();
                        }

                        for(int i = 1; i <= result.getJSONObject("generalInfo").getInt("eventNumber"); i++) {
                            Event event = new Event();
                            event.setId(result.getJSONObject("event" + Integer.toString(i)).getInt("event_id"));
                            event.setType(result.getJSONObject("event" + Integer.toString(i)).getInt("user_action"));
                            event.setName(result.getJSONObject("event" + Integer.toString(i)).getString("action_username"));
                            event.setTopic(result.getJSONObject("event" + Integer.toString(i)).getString("event_name"));
                            event.setImageCount(result.getJSONObject("event" + Integer.toString(i)).getInt("event_image_count"));
                            event.setLocation(result.getJSONObject("event" + Integer.toString(i)).getString("event_location"));
                            event.setContent(result.getJSONObject("event" + Integer.toString(i)).getString("event_content"));
                            event.setMaxAtt(result.getJSONObject("event" + Integer.toString(i)).getInt("event_max_att"));
                            event.setCurAtt(result.getJSONObject("event" + Integer.toString(i)).getInt("event_cur_att"));
                            event.setEventDate(result.getJSONObject("event" + Integer.toString(i)).getLong("event_start_date"));
                            event.setActionTime(refreshDateNumber - result.getJSONObject("event" + Integer.toString(i)).getLong("user_action_time"));
                            event.setInterests(result.getJSONObject("event" + Integer.toString(i)).getString("event_interests"));
                            event.setEventVersion(result.getJSONObject("event" + Integer.toString(i)).getInt("event_image_version"));
                            event.setTimeLeft(result.getJSONObject("event" + Integer.toString(i)).getLong("event_start_date") - refreshDateNumber);
                            event.setActionTime(event.getActionTime() / 60);
                            event.setTimeLeft(event.getTimeLeft() / 60);

                            if(refType == 0) {
                                eventList.add(event);
                            }else if(refType == 1) {
                                eventAdapter.addItem(0, event);
                                layoutManager.scrollToPositionWithOffset(0, 0);
                            }else if(refType == 2) {
                                eventAdapter.addItem(eventAdapter.getItemCount() - 1, event);
                            }
                        }

                        if(refType==0){
                            eventAdapter = new EventAdapter(context, eventList);
                            recyclerView.setAdapter((new SlideInLeftAnimationAdapter(eventAdapter)));
                        }


                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                isLoading = false;
                swipeLayout.setRefreshing(false);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                swipeLayout.setRefreshing(false);
                Snackbar.make(getView(), context.getResources().getString(R.string.siresult3), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse != null) {
                    Log.e("Volley", "Error. HTTP Status Code:" + networkResponse.statusCode);
                }

                if (error instanceof TimeoutError) {
                    Log.e("Volley", "TimeoutError: " + error.toString());
                }else if(error instanceof NoConnectionError){
                    Log.e("Volley", "NoConnectionError: " + error.toString());
                } else if (error instanceof AuthFailureError) {
                    Log.e("Volley", "AuthFailureError: " + error.toString());
                } else if (error instanceof ServerError) {
                    Log.e("Volley", "ServerError: " + error.toString());
                } else if (error instanceof NetworkError) {
                    Log.e("Volley", "NetworkError: " + error.toString());
                } else if (error instanceof ParseError) {
                    Log.e("Volley", "ParseError: " + error.toString());
                }
                +
            }
        });

        VolleySingleton.getInstance(context).addToRequestQueue(sr);*/
    }
}
