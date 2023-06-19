package com.example.buddy_prototype_v1.menus;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.buddy_prototype_v1.MainActivity;
import com.example.buddy_prototype_v1.R;

public class SubAppMenuAdapter extends RecyclerView.Adapter<SubAppMenuAdapter.SubAppMenuViewHolder> {
    String[] appNames;
    private static ClickListener clickListener;

    public SubAppMenuAdapter(String[] names) {
        Log.d("SubAppMenuAdapter", "constructor");
        appNames = names;
    }

    @NonNull
    @Override
    public SubAppMenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_main_app_item, parent, false);
        return new SubAppMenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubAppMenuAdapter.SubAppMenuViewHolder holder, int position) {
        holder.Bind(appNames[position]);
        holder.setEnabled(appNames[position]);
    }

    @Override
    public int getItemCount() {
        return appNames.length;
    }

    public class SubAppMenuViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        Button appButton;

        public SubAppMenuViewHolder(@NonNull View itemView) {
            super(itemView);
            appButton = itemView.findViewById(R.id.subAppButton);
            appButton.setOnClickListener(this);
        }

        public void Bind(String appName) {
            appButton.setText(appName);
        }

        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition());
        }

        public void setEnabled(String appName) {
            if (MainActivity.gazeEnabled == false) {
                if (appName == MainActivity.GENERIC_SCRATCH_ACTIVITY_NAME) {
                    appButton.setEnabled(false);
                }
                if (appName == SetupActivity.CALIBRATION_ACTIVITY_NAME) {
                    appButton.setEnabled(false);
                }
                if (appName == DevToolsActivity.DIAGNOSTIC_2_ACTIVITY_NAME) {
                    appButton.setEnabled(false);
                }
            }
        }

    }

    public interface ClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }
}
