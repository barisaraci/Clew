package com.clew.android.snap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.clew.android.R;
import com.clew.android.activities.SnapActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PhotoFragment extends Fragment {

    private Context context;
    private Camera camera;
    private CameraPreview preview;
    private FrameLayout cameraBoard;

    private boolean isFlashActive;
    private int currentCameraId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_photo, container, false);

        cameraBoard = (FrameLayout) rootView.findViewById(R.id.cameraBoard);
        initializeCamera();

        ImageButton saveButton = (ImageButton) rootView.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.takePicture(null, null, picture);
            }
        });

        ImageButton flipButton = (ImageButton) rootView.findViewById(R.id.flipButton);
        flipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*camera.stopPreview();
                camera.release();*/

                preview.getHolder().removeCallback(preview);
                camera.stopPreview();
                camera.release();
                camera = null;
                cameraBoard.removeAllViews();

                if(currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                    currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }
                else {
                    currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                camera = Camera.open(currentCameraId);

                setCameraProperties(currentCameraId, camera);
                try {
                    camera.setPreviewDisplay(preview.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                preview = new CameraPreview(context, camera, ((SnapActivity) context).displayType);
                cameraBoard.addView(preview);
            }
        });

        ImageButton flashButton = (ImageButton) rootView.findViewById(R.id.flashButton);
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Camera.Parameters params = camera.getParameters();

                String flashMode = (isFlashActive) ? Camera.Parameters.FLASH_MODE_OFF : Camera.Parameters.FLASH_MODE_ON;
                isFlashActive = !isFlashActive;
                params.setFlashMode(flashMode);
                camera.setParameters(params);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

    }

    private void initializeCamera() {
        currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        camera = Camera.open(currentCameraId);
        setCameraProperties(currentCameraId, camera);

        preview = new CameraPreview(context, camera, ((SnapActivity) context).displayType);
        cameraBoard.addView(preview);
    }

    private Camera.PictureCallback picture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = ((SnapActivity) context).getOutputMediaFile(1);

            if (pictureFile == null) {
                Log.d("Clew: ", "Error creating media file, check storage permissions.");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close(); // burada jpeg compress olacak
            } catch (FileNotFoundException e) {
                Log.d("Clew: ", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("Clew: ", "Error accessing file: " + e.getMessage());
            }

            try {
                rotateImage(pictureFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            ((SnapActivity) context).imageType = "photo";
            PreviewFragment fragmentPreview = new PreviewFragment();
            ((SnapActivity) context).setPreviewFragment(fragmentPreview);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.layoutMain, fragmentPreview);
            transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            transaction.addToBackStack(null);
            transaction.commit();

            camera.startPreview();
        }
    };

    private Camera.Size getOptimalPictureSize(List<Camera.Size> sizes, int w, int h) {
        if (sizes == null) return null;

        if (sizes.get(0).width > sizes.get(0).height) {
            int temp = w;
            w = h;
            h = temp;
        }

        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    private void setCameraProperties(int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        Camera.Parameters params = camera.getParameters();

        // orientation parameter
        int degrees = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int rotation, displayRotation;

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (360 + info.orientation + degrees) % 360;
        } else {
            rotation = (360 + info.orientation - degrees) % 360;
        }
        params.setRotation(rotation);

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayRotation = (info.orientation + degrees) % 360;
            displayRotation = (360 - displayRotation) % 360;
        } else {
            displayRotation = (360 + info.orientation - degrees) % 360;
        }
        camera.setDisplayOrientation(displayRotation);

        // aspect ratio parameter
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size optimalSize = getOptimalPictureSize(sizes, 768, 1024);
        params.setPictureSize(optimalSize.width, optimalSize.height);
        params.setPreviewSize(optimalSize.width, optimalSize.height);

        // auto focus parameter
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        camera.setParameters(params);
    }

    public void rotateImage(String file) throws IOException{

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, bounds);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = BitmapFactory.decodeFile(file, opts);

        File file1 = new File(((SnapActivity) context).getPhotoPath());

        int rotationAngle = getCameraPhotoOrientation(getActivity(), Uri.fromFile(file1), file1.toString());

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
        FileOutputStream fos=new FileOutputStream(file);
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();
    }

    public static int getCameraPhotoOrientation(Context context, Uri imageUri, String imagePath){
        int rotate = 0;
        try {
            context.getContentResolver().notifyChange(imageUri, null);
            File imageFile = new File(imagePath);
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    rotate = 0;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }

    @Override
    public void onPause() {
        super.onPause();

        preview.getHolder().removeCallback(preview);
        camera.stopPreview();
        camera.release();
        camera = null;
        cameraBoard.removeView(preview);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (camera == null)
            initializeCamera();
    }

}
