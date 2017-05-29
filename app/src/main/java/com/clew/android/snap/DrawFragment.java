package com.clew.android.snap;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.clew.android.R;
import com.clew.android.activities.SnapActivity;

public class DrawFragment extends Fragment {

    private Context context;


    private int displayType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        displayType = ((SnapActivity) getActivity()).displayType;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_draw, container, false);

        initLayout(rootView);

        return rootView;
    }

    private void initLayout(View rootView) {
        final DrawingView drawingView = new DrawingView(context, displayType);
        FrameLayout drawBoard = (FrameLayout) rootView.findViewById(R.id.drawBoard);
        drawBoard.addView(drawingView);

        ImageButton saveButton = (ImageButton) rootView.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SnapActivity) context).saveImage(drawingView.getCanvasBitmap());
                ((SnapActivity) context).imageType = "drawing";
                PreviewFragment fragmentPreview = new PreviewFragment();
                ((SnapActivity) context).setPreviewFragment(fragmentPreview);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.layoutMain, fragmentPreview);
                transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }

}
