package com.clew.android.trace;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.clew.android.R;
import com.clew.android.activities.HomeActivity;
import com.clew.android.activities.SnapActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class TraceAdapter extends RecyclerView.Adapter<TraceAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Trace> traceList;

    public TraceAdapter(Context context, ArrayList<Trace> traceList) {
        this.context = context;
        this.traceList = new ArrayList<>(traceList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trace, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Trace trace = traceList.get(position);
        holder.bind(trace);
    }

    @Override
    public int getItemCount() {
        return traceList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvInfo, tvTime;
        private Button buttonSnap;
        private boolean isActive;

        public ViewHolder(View itemView) {
            super(itemView);

            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvInfo = (TextView) itemView.findViewById(R.id.tvInfo);
            tvTime = (TextView) itemView.findViewById(R.id.tvTime);

            isActive = true;
            buttonSnap = (Button) itemView.findViewById(R.id.buttonSnap);
        }

        public void bind(final Trace trace) {
            tvName.setText(trace.getName());
            String info = "<font color=#ad1010>" + trace.getPosts() + "</font>" + " posts by " + "<font color=#ad1010>" + trace.getPopulation() + "</font>" + " people";
            tvInfo.setText(Html.fromHtml(info), TextView.BufferType.SPANNABLE);

            String timePassed, timeLeft;

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            long datePassed = (calendar.getTimeInMillis() - trace.getCreatedAt().getTime()) / (60 * 1000);
            long dateLeft = (trace.getCreatedAt().getTime() + trace.getTime() - calendar.getTimeInMillis()) / (60 * 1000);

            if (datePassed > 7 * 24 * 60) { timePassed = "(created <font color=#d90d0e>" + datePassed / (7 * 24 * 60) + context.getResources().getString(R.string.timeWeek) + "</font> ago)"; }
            else if (datePassed > 24 * 60) { timePassed = "(created <font color=#d90d0e>" + datePassed / (24 * 60) + context.getResources().getString(R.string.timeDay) + "</font> ago)"; }
            else if (datePassed > 60) { timePassed = "(created <font color=#d90d0e>" + datePassed / 60 + context.getResources().getString(R.string.timeHour) + "</font> ago)"; }
            else if (datePassed >= 0){ timePassed = "(created <font color=#d90d0e>" + datePassed + context.getResources().getString(R.string.timeMinute) + "</font> ago)"; }
            else { timePassed = "<font color=#d90d0e>(now)</font>"; }

            timeLeft = "<font color=#d90d0e><b>";
            if (dateLeft > 7 * 24 * 60) { timeLeft += dateLeft / (7 * 24 * 60) + context.getResources().getString(R.string.timeWeek) + "</b></font> left"; }
            else if (dateLeft > 24 * 60) { timeLeft += dateLeft / (24 * 60) + context.getResources().getString(R.string.timeDay) + "</b></font> left"; }
            else if (dateLeft > 60) { timeLeft += dateLeft / 60 + context.getResources().getString(R.string.timeHour) + "</b></font> left"; }
            else if (dateLeft >= 0){ timeLeft += dateLeft + context.getResources().getString(R.string.timeMinute) + "</b></font> left"; }
            else { timeLeft += "-</b></font>"; timePassed = ""; isActive = false;}

            String time = timeLeft + " " + timePassed;
            tvTime.setText(Html.fromHtml(time), TextView.BufferType.SPANNABLE);

            buttonSnap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isActive) {
                        Intent intent = new Intent(context, SnapActivity.class);
                        intent.putExtra("user", ((HomeActivity) context).getUser());
                        intent.putExtra("trace", trace.getName());
                        context.startActivity(intent);
                    }
                }
            });
        }
    }
}