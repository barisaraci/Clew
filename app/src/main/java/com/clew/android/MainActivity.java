package com.clew.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.clew.android.activities.HomeActivity;
import com.clew.android.activities.EditUserActivity;
import com.clew.android.activities.SnapActivity;
import com.clew.android.user.User;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private MobileServiceClient client;

    private View layoutMain;
    private ImageButton loginButtonFb, loginButtonG;
    private ProgressBar progressBar;

    public static final String CLIENT_ADRESS = "https://clew.azurewebsites.net/";

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";

    public static final String STORAGE_CONNECTION_STRING =
                    "DefaultEndpointsProtocol=https;" +
                    "AccountName=clew;" +
                    "AccountKey=GgoUsQ0UqioKdcZB7vDh9PZImWegSY0mIY62qrSf17HjExJZAwTrmJZcEk6FCYp7d3+mxTwV7dH1zkPFh0564Q==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        initLayout();
        initClient();
        authenticate();
    }

    private void initLayout() {
        layoutMain = findViewById(R.id.layoutMain);

        loginButtonFb = (ImageButton) findViewById(R.id.buttonLoginFb);
        loginButtonG = (ImageButton) findViewById(R.id.buttonLoginG);

        loginButtonFb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginWithFacebook();
            }
        });


        loginButtonG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginWithGoogle();
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void initClient() {
        try {
            client = new MobileServiceClient(CLIENT_ADRESS, this);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void authenticate() {
        if (loadUserTokenCache(client)) {
            showProgress();
            userAuthenticated();
        }
    }

    private void userAuthenticated() {
        new AsyncTask<Void, Void, User>() {
            @Override
            protected User doInBackground(Void... params) {
                MobileServiceTable<User> userTable = client.getTable(User.class);

                User user = null;

                try {
                    user = userTable.lookUp(client.getCurrentUser().getUserId()).get();
                } catch (InterruptedException | ExecutionException e) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG)
                            .setAction("RETRY", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    showProgress();
                                    userAuthenticated();
                                }
                            }).show();
                    e.printStackTrace();
                }

                return user;
            }

            @Override
            protected void onPostExecute(User user) {
                if (user == null) {
                    closeProgress();
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG)
                            .setAction("RETRY", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    showProgress();
                                    userAuthenticated();
                                }
                            }).show();
                } else {
                    Intent intent = new Intent(context, HomeActivity.class);
                    intent.putExtra("user", user);
                    startActivity(intent);
                    finish();
                }
            }
        }.execute();
    }

    private void loginWithFacebook() {
        ListenableFuture<MobileServiceUser> login = client.login(MobileServiceAuthenticationProvider.Facebook);

        Futures.addCallback(login, new FutureCallback<MobileServiceUser>() {
            @Override
            public void onFailure(Throwable exc) {
                Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                Log.e("Clew: ", exc.toString());
            }

            @Override
            public void onSuccess(MobileServiceUser user) {
                cacheUserToken(client.getCurrentUser());
                checkIfUserExist();
            }
        });
    }

    private void loginWithGoogle() {
        ListenableFuture<MobileServiceUser> login = client.login(MobileServiceAuthenticationProvider.Google);

        Futures.addCallback(login, new FutureCallback<MobileServiceUser>() {
            @Override
            public void onFailure(Throwable exc) {
                Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                Log.e("Clew: ", exc.toString());
            }

            @Override
            public void onSuccess(MobileServiceUser user) {
                cacheUserToken(client.getCurrentUser());
                checkIfUserExist();
            }
        });
    }

    private void checkIfUserExist() {
        new AsyncTask<Void, Void, User>() {
            @Override
            protected User doInBackground(Void... params) {
                MobileServiceTable<User> userTable = client.getTable(User.class);
                User user = null;

                try {
                    user = userTable.lookUp(client.getCurrentUser().getUserId()).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                return user;
            }

            @Override
            protected void onPostExecute(User user) {
                if (user == null) {
                    createNewUser();
                } else {
                    userAuthenticated();
                }
            }
        }.execute();
    }

    private void createNewUser() {
        final User user = new User();
        user.setId(client.getCurrentUser().getUserId());

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                MobileServiceTable<User> userTable = client.getTable(User.class);

                try {
                    userTable.insert(user).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                userAuthenticatedFirstTime();
            }
        }.execute();
    }

    private void userAuthenticatedFirstTime() {
        Intent intent = new Intent(this, EditUserActivity.class);
        startActivity(intent);
        finish();
    }

    private void cacheUserToken(MobileServiceUser user) {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.apply();
    }

    private boolean loadUserTokenCache(MobileServiceClient client) {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, null);
        if (userId == null)
            return false;
        String token = prefs.getString(TOKENPREF, null);
        if (token == null)
            return false;

        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);

        return true;
    }

    private void showProgress() {
        loginButtonFb.setVisibility(View.GONE);
        loginButtonG.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void closeProgress() {
        loginButtonFb.setVisibility(View.VISIBLE);
        loginButtonG.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

}
