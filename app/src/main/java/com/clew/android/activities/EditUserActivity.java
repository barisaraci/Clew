package com.clew.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.clew.android.MainActivity;
import com.clew.android.R;
import com.clew.android.activities.HomeActivity;
import com.clew.android.user.User;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EditUserActivity extends AppCompatActivity {

    private Context context;
    private MobileServiceClient client;
    private CloudBlobContainer container;
    private User user;

    private View layoutMain;
    private ImageButton buttonProfile;
    private EditText etUsername, etFullname;
    private Button buttonSave;
    private ProgressBar progressBar;
    private File photoFile, thumbnailFile;

    private final String BLOB_DIRECTORY = "https://clew.blob.core.windows.net";
    private final String CONTAINER_REFERENCE = "profilephotos";

    private String userID;
    private int PICK_IMAGE_REQUEST = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);
        context = this;

        initLayout();
        initClient();
        authenticate();
        initStorage();
    }

    private void initLayout() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Profile");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        layoutMain = findViewById(R.id.layoutMain);

        etUsername = (EditText) findViewById(R.id.etUsername);
        etFullname = (EditText) findViewById(R.id.etFullname);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        buttonProfile = (ImageButton) findViewById(R.id.buttonProfile);
        buttonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });

        buttonSave = (Button) findViewById(R.id.buttonSave);
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonSave.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                save();
            }
        });
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
            getUser();
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

    private void getUser() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                MobileServiceTable<User> userTable = client.getTable(User.class);

                try {
                    user = userTable.lookUp(client.getCurrentUser().getUserId()).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (user == null) {
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                } else {
                    userID = client.getCurrentUser().getUserId();
                    etUsername.setText(user.getUserName());
                    etFullname.setText(user.getFullName());
                    Picasso.with(context)
                            .load(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE + "/" + userID + "_thumbnail")
                            .error(android.R.drawable.ic_dialog_alert)
                            .into(buttonProfile, new Callback() {
                                @Override
                                public void onSuccess() {
                                    Picasso.with(context)
                                            .load(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE + "/" + userID + "_main")
                                            .error(android.R.drawable.ic_dialog_alert)
                                            .into(buttonProfile);
                                }
                                @Override
                                public void onError() {

                                }
                            });
                }
            }
        }.execute();
    }

    private void save() {
        final String userName = etUsername.getText().toString();
        final String fullName = etFullname.getText().toString();
        if (userName.length() < 6 || userName.length() > 16) {
            Snackbar.make(layoutMain, context.getResources().getString(R.string.error1), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        } else if (fullName.length() < 6 || fullName.length() > 30) {
            Snackbar.make(layoutMain, context.getResources().getString(R.string.error2), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        } else if (!userName.matches("[a-zA-Z0-9.? ]*")) {
            Snackbar.make(layoutMain, context.getResources().getString(R.string.error3), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        } else if (!fullName.matches("[a-zA-Z0-9.? ]*")) {
            Snackbar.make(layoutMain, context.getResources().getString(R.string.error4), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        } else {
            new AsyncTask<Void, Void, List>() {
                @Override
                protected List doInBackground(Void... params) {
                    MobileServiceTable<User> userTable = client.getTable(User.class);

                    List<User> result = null;

                    try {
                        result = userTable.where().field("userName").eq(userName).execute().get();
                    } catch (InterruptedException | ExecutionException e) {
                        //closeProgress();
                        Snackbar.make(layoutMain, context.getResources().getString(R.string.error5), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        e.printStackTrace();
                    }

                    return result;
                }

                @Override
                protected void onPostExecute(List result) {
                    if (result.isEmpty()) {
                        saveUser(userName, fullName);
                    } else {
                        User user = (User) result.get(0);
                        if (userID.equals(user.getId())) {
                            saveUser(userName, fullName);
                        } else {
                            Snackbar.make(layoutMain, context.getResources().getString(R.string.error5), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                            //closeProgress();
                        }
                    }
                }
            }.execute();
        }
    }

    private void saveUser(String userName, String fullName) {
        user.setUserName(userName);
        user.setFullName(fullName);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    MobileServiceTable<User> userTable = client.getTable(User.class);
                    userTable.update(user);

                    if (photoFile != null && thumbnailFile != null) {
                        CloudBlockBlob blobPhoto = container.getBlockBlobReference(userID + "_main");
                        CloudBlockBlob blobPhotoThumbnail = container.getBlockBlobReference(userID + "_thumbnail");
                        blobPhoto.upload(new FileInputStream(photoFile), photoFile.length());
                        blobPhotoThumbnail.upload(new FileInputStream(thumbnailFile), thumbnailFile.length());
                    }
                } catch (IOException | URISyntaxException | StorageException e) {
                    //closeProgress();
                    Snackbar.make(layoutMain, context.getResources().getString(R.string.error), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Snackbar.make(layoutMain, context.getResources().getString(R.string.success), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                Picasso.with(context).invalidate(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE + "/" + user.getId() + "_main");
                Picasso.with(context).invalidate(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE + "/" + user.getId() + "_thumbnail");
                Intent intent = new Intent(context, HomeActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
                finish();
            }
        }.execute();
    }

    /*private void closeProgress() {
        progressBar.setVisibility(View.GONE);
        buttonSave.setVisibility(View.VISIBLE);
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            CropImage.activity(uri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setMinCropResultSize(128, 128)
                    .setFixAspectRatio(true)
                    .setAspectRatio(1, 1)
                    .start(this);

        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri uri = result != null ? result.getUri() : null;
                Bitmap bitmap = null, newBitmap = null, thumbnailBitmap;
                int imageW = 0, imageH = 0;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                    bitmapOptions.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(inputStream, null, bitmapOptions);
                    imageW = bitmapOptions.outWidth;
                    imageH = bitmapOptions.outHeight;
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (imageW > 512 || imageH > 512) {
                    newBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                } else {
                    newBitmap = bitmap;
                }

                thumbnailBitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, true);
                saveImage(newBitmap, "main");
                saveImage(thumbnailBitmap, "thumbnail");
                buttonProfile.setImageBitmap(newBitmap);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.e("Clew: ", result.toString());
            }
        }
    }

    private void saveImage(Bitmap bitmap, String tag) {
        try {
            File file = getOutputMediaFile(tag);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            if (tag.equals("main")) {
                photoFile = file;
            } else if (tag.equals("thumbnail")) {
                thumbnailFile = file;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getOutputMediaFile(String tag) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Clew");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + tag + "_" + timeStamp + ".jpg");
    }


}
