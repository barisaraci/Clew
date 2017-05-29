package com.clew.android.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.clew.android.MainActivity;
import com.clew.android.R;
import com.clew.android.climage.Climage;
import com.clew.android.snap.DrawFragment;
import com.clew.android.snap.PhotoFragment;
import com.clew.android.snap.PreviewFragment;
import com.clew.android.user.User;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SnapActivity extends AppCompatActivity {

    private Context context;
    private MobileServiceClient client;
    private CloudBlobContainer container;
    private User user;

    private PreviewFragment previewFragment;
    private View layoutMain;
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    public int displayType; // 1 = 4:3, 2 = square
    private float aspectRatio;
    private String photoPath, trace;
    public String imageType;

    private final String BLOB_DIRECTORY = "https://clew.blob.core.windows.net";
    private final String CONTAINER_REFERENCE = "images";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snap);
        context = this;

        initLayout();
        initClient();
        authenticate();
        initStorage();
    }

    private void initLayout() {
        layoutMain = findViewById(R.id.layoutMain);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        trace = getIntent().getExtras().getString("trace");
        calculateRatio();
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

    private void initStorage() {
        try {
            CloudStorageAccount account = CloudStorageAccount.parse(MainActivity.STORAGE_CONNECTION_STRING);
            CloudBlobClient blobClient = account.createCloudBlobClient();
            container = blobClient.getContainerReference(CONTAINER_REFERENCE);
        } catch (URISyntaxException | StorageException | InvalidKeyException e) {
            Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
            e.printStackTrace();
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

    private void calculateRatio() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        aspectRatio = (float) metrics.heightPixels / metrics.widthPixels;

        if (aspectRatio > 1.55) {
            displayType = 1; // 4:3
        } else {
            displayType = 2; // square
        }
    }

    public void post() {
        String uniqueID = UUID.randomUUID().toString();

        final Climage climage = new Climage();
        climage.setId(uniqueID);
        climage.setUserId(user.getId());
        climage.setUserName(user.getUserName());
        climage.setFullName(user.getFullName());
        climage.setTrace(trace);
        climage.setLikes(0);
        climage.setType(imageType);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                MobileServiceTable<Climage> climageTable = client.getTable(Climage.class);

                try {
                    climageTable.insert(climage).get();

                    CloudBlockBlob blobImage = container.getBlockBlobReference(user.getId() + "/" + climage.getId());
                    File source = new File(photoPath);
                    blobImage.upload(new FileInputStream(source), source.length());
                } catch (InterruptedException | ExecutionException | StorageException | URISyntaxException | IOException e) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    previewFragment.closeProgress();
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                previewFragment.closeProgress();
                finish();
            }
        }.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_snap, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                PhotoFragment fragment = new PhotoFragment();
                return fragment;
            } else {
                DrawFragment fragment = new DrawFragment();
                return fragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SNAP"; // burasi resourcesda olsun
                case 1:
                    return "DRAW"; // burasi resourcesda olsun
            }
            return null;
        }
    }

    public void saveImage(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 768, 1024, true);
        try {
            FileOutputStream fos = new FileOutputStream(getOutputMediaFile(1));
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    public File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Clew");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == 1) {
            photoPath = mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg";
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    public void setPreviewFragment(PreviewFragment fragment) {
        previewFragment = fragment;
    }

    public String getPhotoPath() {
        return photoPath;
    }

}
