package com.clew.android.user;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.clew.android.R;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private Context context;
    private ArrayList<User> userList;

    private final String BLOB_DIRECTORY = "https://clew.blob.core.windows.net";
    private final String CONTAINER_REFERENCE_PROFILE = "profilephotos";

    public UserAdapter(Context context, ArrayList<User> userList) {
        this.context = context;

        if (!userList.isEmpty())
            this.userList = new ArrayList<User>(userList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void animateTo(ArrayList<User> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }

    private void applyAndAnimateRemovals(ArrayList<User> newModels) {
        for (int i = userList.size() - 1; i >= 0; i--) {
            final User model = userList.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(ArrayList<User> newModels) {
        for (int i = 0, count = newModels.size(); i < count; i++) {
            final User model = newModels.get(i);
            if (!userList.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(ArrayList<User> newModels) {
        for (int toPosition = newModels.size() - 1; toPosition >= 0; toPosition--) {
            final User model = newModels.get(toPosition);
            final int fromPosition = userList.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private User removeItem(int position) {
        final User model = userList.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    public void addItem(int position, User model) {
        userList.add(position, model);
        notifyItemInserted(position);
    }

    private void moveItem(int fromPosition, int toPosition) {
        final User model = userList.remove(fromPosition);
        userList.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFullName, tvUserName;
        private Button buttonFollow;
        private CircularImageView avatar;

        private boolean isFollowing; // burayı tamamla

        public ViewHolder(View itemView) {
            super(itemView);

            avatar = (CircularImageView) itemView.findViewById(R.id.civAvatar);

            tvFullName = (TextView) itemView.findViewById(R.id.tvFullName);
            tvUserName = (TextView) itemView.findViewById(R.id.tvUserName);

            buttonFollow = (Button) itemView.findViewById(R.id.buttonFollow);
        }

        public void bind(final User user) {
            tvFullName.setText(user.getFullName());
            tvUserName.setText(user.getUserName());

            buttonFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            Picasso.with(context)
                    .load(BLOB_DIRECTORY + "/" + CONTAINER_REFERENCE_PROFILE + "/" + user.getId() + "_thumbnail")
                    .error(R.drawable.profile0)
                    .into(avatar);

            avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }
}