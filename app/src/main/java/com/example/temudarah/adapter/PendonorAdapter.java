package com.example.temudarah.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.temudarah.R;
import com.example.temudarah.model.Pendonor;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PendonorAdapter extends RecyclerView.Adapter<PendonorAdapter.PendonorViewHolder> {

    private List<Pendonor> pendonorList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Pendonor pendonor);
    }

    public PendonorAdapter(Context context, List<Pendonor> pendonorList, OnItemClickListener listener) {
        this.context = context;
        this.pendonorList = pendonorList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PendonorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pendonor, parent, false);
        return new PendonorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendonorViewHolder holder, int position) {
        Pendonor pendonor = pendonorList.get(position);

        holder.tvName.setText(pendonor.getNama());
        holder.tvBloodType.setText(pendonor.getGolonganDarah());
        holder.tvGender.setText(pendonor.getJenisKelamin());
        holder.tvLocation.setText(pendonor.getLokasi());
        holder.ivProfilePhoto.setImageResource(pendonor.getFotoResource());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pendonor);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pendonorList.size();
    }

    public static class PendonorViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivProfilePhoto;
        TextView tvName, tvBloodType, tvGender, tvLocation;

        public PendonorViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePhoto = itemView.findViewById(R.id.iv_profile_photo);
            tvName = itemView.findViewById(R.id.tv_name);
            tvBloodType = itemView.findViewById(R.id.tv_blood_type);
            tvGender = itemView.findViewById(R.id.tv_gender);
            tvLocation = itemView.findViewById(R.id.tv_address);
        }
    }
}
