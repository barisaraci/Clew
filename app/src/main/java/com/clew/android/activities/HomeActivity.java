package com.clew.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.clew.android.MainActivity;
import com.clew.android.R;
import com.clew.android.climage.FeedFragment;
import com.clew.android.trace.TraceFragment;
import com.clew.android.user.ProfileFragment;
import com.clew.android.user.User;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

import java.net.MalformedURLException;

public class HomeActivity extends AppCompatActivity {

    private Context context;
    private MobileServiceClient client;
    private User user;

    private View layoutMain;
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        context = this;

        initLayout();
        initClient();
        authenticate();
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

        if (id == R.id.action_admin) {
            Intent intent = new Intent(this, AdminActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initLayout() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setLogo(R.mipmap.ic_launcher);

        layoutMain = findViewById(R.id.layoutMain);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setCurrentItem(1);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
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

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                ProfileFragment fragment = new ProfileFragment();
                return fragment;
            } else if (position == 1) {
                FeedFragment fragment = new FeedFragment();
                return fragment;
            } else {
                TraceFragment fragment = new TraceFragment();
                return fragment;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getApplicationContext().getResources().getString(R.string.homeProfile);
                case 1:
                    return getApplicationContext().getResources().getString(R.string.homeFeed);
                case 2:
                    return getApplicationContext().getResources().getString(R.string.homeTrace);
            }
            return null;
        }
    }

    public MobileServiceClient getClient() {
        return client;
    }

    public User getUser() {
        return user;
    }

    public View getLayoutMain() {
        return layoutMain;
    }

}
