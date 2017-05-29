package com.clew.android.snap;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.clew.android.R;
import com.clew.android.activities.SnapActivity;

import java.io.File;

public class PreviewFragment extends Fragment {

    private Context context;

    private ImageView imagePreview;
    private RelativeLayout layoutButtons;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_preview, container, false);

        initLayout(rootView);

        return rootView;
    }

    private void initLayout(View rootView) {
        imagePreview = (ImageView) rootView.findViewById(R.id.imagePreview);
        imagePreview.setImageURI(Uri.fromFile(new File(((SnapActivity) context).getPhotoPath())));

        layoutButtons = (RelativeLayout) rootView.findViewById(R.id.layoutButtons);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        Button buttonPost = (Button) rootView.findViewById(R.id.buttonPost);
        buttonPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutButtons.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                ((SnapActivity) context).post();
            }
        });
    }

    public void closeProgress() {
        progressBar.setVisibility(View.GONE);
        layoutButtons.setVisibility(View.VISIBLE);
    }

}
