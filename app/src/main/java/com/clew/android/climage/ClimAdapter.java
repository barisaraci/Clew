package com.clew.android.climage;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.clew.android.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ClimAdapter extends RecyclerView.Adapter<ClimAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Climage> climageList;

    private final String BLOB_DIRECTORY = "https://clew.blob.core.windows.net";
    private final String CONTAINER_REFERENCE_IMAGES = "images";

    public ClimAdapter(Context context, ArrayList<Climage> climageList) {
        this.context = context;
        this.climageList = new ArrayList<>(climageList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clim, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Climage climage = climageList.get(position);
        holder.bind(climage);
    }

    @Override
    public int getItemCount() {
        return climageList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.image);
        }

        public void bind(final Climage climage) {

            Picasso.with(context)
                    .load(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE_IMAGES + "/" + climage.getUserId() + "/" + climage.getId())
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(imageView);

        }
    }

}
