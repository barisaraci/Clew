package com.clew.android.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.clew.android.MainActivity;
import com.clew.android.R;
import com.clew.android.trace.Trace;
import com.clew.android.user.User;
import com.google.gson.JsonObject;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceJsonTable;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

public class AdminActivity extends AppCompatActivity {

    private Context context;
    private MobileServiceClient client;
    private User user;

    private View layoutMain;
    private EditText etName, etPopulation, etPosts, etTime, etFullname, etUsername, etFollowerUser, etFollowingUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        context = this;

        initLayout();
        initClient();
        authenticate();
    }

    private void initLayout() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);

        layoutMain = findViewById(R.id.layoutMain);
        etName = (EditText) findViewById(R.id.etName);
        etPopulation = (EditText) findViewById(R.id.etPopulation);
        etPosts = (EditText) findViewById(R.id.etPosts);
        etTime = (EditText) findViewById(R.id.etTime);
        etFullname = (EditText) findViewById(R.id.etFullname);
        etUsername = (EditText) findViewById(R.id.etUsername);
        etFollowerUser = (EditText) findViewById(R.id.etFollowerUsername);
        etFollowingUser = (EditText) findViewById(R.id.etFollowingUsername);

        Button buttonCreateTrace = (Button) findViewById(R.id.buttonCreateTrace);
        buttonCreateTrace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createTrace();
            }
        });

        Button buttonCreateUser = (Button) findViewById(R.id.buttonCreateUser);
        buttonCreateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createUser();
            }
        });

        Button buttonMakeFriend = (Button) findViewById(R.id.buttonMakeFriend);
        buttonMakeFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeFriend();
            }
        });

    }

    private void createTrace() {
        final Trace trace = new Trace();
        trace.setName(etName.getText().toString());
        trace.setPopulation(Integer.parseInt(etPopulation.getText().toString()));
        trace.setPosts(Integer.parseInt(etPosts.getText().toString()));
        trace.setTime(Long.parseLong(etTime.getText().toString()) * 60 * 60 * 1000);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                MobileServiceTable<Trace> traceTable = client.getTable(Trace.class);

                try {
                    traceTable.insert(trace).get();
                } catch (InterruptedException | ExecutionException e) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                finish();
            }
        }.execute();
    }

    private void createUser() {
        final User user = new User();
        user.setFullName(etFullname.getText().toString());
        user.setUserName(etUsername.getText().toString());

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                MobileServiceTable<User> userTable = client.getTable(User.class);

                try {
                    userTable.insert(user).get();
                } catch (InterruptedException | ExecutionException e) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                finish();
            }
        }.execute();
    }

    private void makeFriend() {
        final JsonObject follower = new JsonObject();
        follower.addProperty("followerId", etFollowerUser.getText().toString());
        follower.addProperty("followingId", etFollowingUser.getText().toString());

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                MobileServiceJsonTable followerTable = client.getTable("Follower");

                try {
                    followerTable.insert(follower).get();
                } catch (InterruptedException | ExecutionException e) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                finish();
            }
        }.execute();
    }

    private void initClient() {
        try {
            client = new MobileServiceClient(MainActivity.CLIENT_ADRESS, this);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void authenticate() {
        if (loadUserTokenCache(client)) {
            user = getIntent().getParcelableExtra("user");
        } else {
            Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    private boolean loadUserTokenCache(MobileServiceClient client) {
        SharedPreferences prefs = getSharedPreferences(MainActivity.SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(MainActivity.USERIDPREF, null);
        if (userId == null)
            return false;
        String token = prefs.getString(MainActivity.TOKENPREF, null);
        if (token == null)
            return false;

        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_newsfeed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        /*if (id == R.id.action_logout) {
            SharedPreferences.Editor editor = preferences.edit();

            editor.putBoolean("isRegistered", false);
            editor.putString("username", "");
            editor.putString("password", "");
            editor.apply();

            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
            ((Activity) context).finish();
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

}
