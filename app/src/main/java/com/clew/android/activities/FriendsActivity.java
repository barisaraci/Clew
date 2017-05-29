package com.clew.android.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.clew.android.MainActivity;
import com.clew.android.R;
import com.clew.android.user.User;
import com.clew.android.user.UsersFragment;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

import java.net.MalformedURLException;

public class FriendsActivity extends AppCompatActivity {

    private Context context;
    private MobileServiceClient client;
    private User user;

    private View layoutMain;
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        context = this;

        initLayout();
        initClient();
        authenticate();
    }

    private void initLayout() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("People");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        layoutMain = findViewById(R.id.layoutMain);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(2);

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
            System.out.println(user);
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
        }

        if (id == R.id.action_new_event) {
            Intent intent = new Intent(this, com.pomegro.android.event.NewEventActivity.class);
            startActivity(intent);
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                UsersFragment fragment = new UsersFragment();
                Bundle bundle = new Bundle();
                bundle.putString("userType", "followers");
                fragment.setArguments(bundle);
                return fragment;
            } else  if(position == 1) {
                UsersFragment fragment = new UsersFragment();
                Bundle bundle = new Bundle();
                bundle.putString("userType", "following");
                fragment.setArguments(bundle);
                return fragment;
            } else {
                UsersFragment fragment = new UsersFragment();
                Bundle bundle = new Bundle();
                bundle.putString("userType", "search");
                fragment.setArguments(bundle);
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
                    return "Followers";
                case 1:
                    return "Following";
                case 2:
                    return "Search";
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
