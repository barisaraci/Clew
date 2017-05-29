package com.clew.android.climage;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.clew.android.R;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class ClimageAdapter extends RecyclerView.Adapter<ClimageAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Climage> climageList;

    private final String BLOB_DIRECTORY = "https://clew.blob.core.windows.net";
    private final String CONTAINER_REFERENCE_IMAGES = "images";
    private final String CONTAINER_REFERENCE_PROFILE = "profilephotos";

    public ClimageAdapter(Context context, ArrayList<Climage> climageList) {
        this.context = context;
        this.climageList = new ArrayList<>(climageList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_climage, parent, false);
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

    public void animateTo(ArrayList<Climage> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }

    private void applyAndAnimateRemovals(ArrayList<Climage> newModels) {
        for (int i = climageList.size() - 1; i >= 0; i--) {
            final Climage model = climageList.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(ArrayList<Climage> newModels) {
        for (int i = 0, count = newModels.size(); i < count; i++) {
            final Climage model = newModels.get(i);
            if (!climageList.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(ArrayList<Climage> newModels) {
        for (int toPosition = newModels.size() - 1; toPosition >= 0; toPosition--) {
            final Climage model = newModels.get(toPosition);
            final int fromPosition = climageList.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private Climage removeItem(int position) {
        final Climage model = climageList.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    public void addItem(int position, Climage model) {
        climageList.add(position, model);
        notifyItemInserted(position);
    }

    private void moveItem(int fromPosition, int toPosition) {
        final Climage model = climageList.remove(fromPosition);
        climageList.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvTimePassed, tvTrace, tvLikes, tvType;
        private Button buttonLike;
        private ImageView imageView;
        private CircularImageView avatar;

        public ViewHolder(View itemView) {
            super(itemView);

            avatar = (CircularImageView) itemView.findViewById(R.id.civAvatar);
            imageView = (ImageView) itemView.findViewById(R.id.image);

            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvTimePassed = (TextView) itemView.findViewById(R.id.tvTimePassed);
            tvTrace = (TextView) itemView.findViewById(R.id.tvTrace);
            tvLikes = (TextView) itemView.findViewById(R.id.tvLikes);
            tvType = (TextView) itemView.findViewById(R.id.tvType);

            buttonLike = (Button) itemView.findViewById(R.id.buttonLike);
        }

        public void bind(final Climage climage) {
            tvName.setText(climage.getFullName());
            tvTrace.setText(climage.getTrace());
            tvLikes.setText(Integer.toString(climage.getLikes()) + " likes");
            tvType.setText("#" + climage.getType());

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

            long date = (calendar.getTimeInMillis() - climage.getCreatedAt().getTime()) / (60 * 1000);

            if (date > 7 * 24 * 60) { tvTimePassed.setText(date / (7 * 24 * 60) + context.getResources().getString(R.string.timeWeek)); }
            else if (date > 24 * 60) { tvTimePassed.setText(date / (24 * 60) + context.getResources().getString(R.string.timeDay)); }
            else if (date > 60) { tvTimePassed.setText(date / 60 + context.getResources().getString(R.string.timeHour)); }
            else if (date >= 0){ tvTimePassed.setText(date + context.getResources().getString(R.string.timeMinute)); }
            else { tvTimePassed.setText("now"); }

            Picasso.with(context)
                    .load(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE_PROFILE + "/" + climage.getUserId() + "_thumbnail")
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(avatar);

            Picasso.with(context)
                    .load(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE_IMAGES + "/" + climage.getUserId() + "/" + climage.getId())
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(imageView);

            avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }
}