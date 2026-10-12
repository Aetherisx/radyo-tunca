package com.example.radyotunca;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RadioAdapter extends RecyclerView.Adapter<RadioAdapter.RadioViewHolder> {


    private List<Radio> radioList = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onPlayClick(Radio radio);
        void onFavoriteClick(Radio radio);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setRadios(List<Radio> radios) {
        this.radioList = radios;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RadioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_radio, parent, false);
        return new RadioViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RadioViewHolder holder, int position) {
        Radio currentRadio = radioList.get(position);

        holder.textRadioName.setText(currentRadio.name);
        holder.textCategory.setText(currentRadio.category);

        if (currentRadio.name != null && !currentRadio.name.isEmpty()) {
            holder.textRadioInitial.setText(currentRadio.name.substring(0, 1).toUpperCase());
        }

        if (currentRadio.isFavorite) {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_filled);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_empty);
        }

        holder.btnPlayStop.setImageResource(R.drawable.ic_play);

        holder.btnPlayStop.setOnClickListener(v -> {
            if (listener != null) listener.onPlayClick(currentRadio);
        });

        holder.btnFavorite.setOnClickListener(v -> {
            if (listener != null) listener.onFavoriteClick(currentRadio);
        });
    }

    @Override
    public int getItemCount() {
        return radioList.size();
    }

    class RadioViewHolder extends RecyclerView.ViewHolder {
        private TextView textRadioName;
        private TextView textCategory;
        private TextView textRadioInitial;
        private ImageButton btnPlayStop;
        private ImageButton btnFavorite;

        public RadioViewHolder(View itemView) {
            super(itemView);
            textRadioName = itemView.findViewById(R.id.textRadioName);
            textCategory = itemView.findViewById(R.id.textCategory);
            textRadioInitial = itemView.findViewById(R.id.textRadioInitial);
            btnPlayStop = itemView.findViewById(R.id.btnPlayStop);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}