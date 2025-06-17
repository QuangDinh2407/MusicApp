package com.ck.music_app.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.Album;
import com.ck.music_app.Model.ArtistWithAlbums;
import com.ck.music_app.R;

import java.util.List;

public class ArtistAlbumAdapter extends ArrayAdapter<ArtistWithAlbums> {
    private Context context;
    private List<ArtistWithAlbums> data;
    private OnAlbumClickListener onAlbumClickListener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album, com.ck.music_app.Model.Artist artist);
    }

    public void setOnAlbumClickListener(OnAlbumClickListener listener) {
        this.onAlbumClickListener = listener;
    }

    public ArtistAlbumAdapter(Context context, List<ArtistWithAlbums> data) {
        super(context, 0, data);
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_artist_albums, parent, false);
        }
        TextView tvArtistName = convertView.findViewById(R.id.tvArtistName);
        LinearLayout layoutAlbums = convertView.findViewById(R.id.layoutAlbums);

        ArtistWithAlbums artistWithAlbums = data.get(position);
        tvArtistName.setText(artistWithAlbums.getArtist().getName());

        // Xóa các view cũ
        layoutAlbums.removeAllViews();

        // Thêm các album vào layoutAlbums
        for (Album album : artistWithAlbums.getAlbums()) {
            View albumView = LayoutInflater.from(context).inflate(R.layout.item_album, layoutAlbums, false);
            ImageView imgAlbumCover = albumView.findViewById(R.id.imgAlbumCover);
            TextView tvAlbumTitle = albumView.findViewById(R.id.tvAlbumTitle);

            tvAlbumTitle.setText(album.getTitle());

            Glide.with(convertView.getContext())
                    .load(album.getCoverUrl())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(imgAlbumCover);

            albumView.setOnClickListener(v -> {
                if (onAlbumClickListener != null) {
                    onAlbumClickListener.onAlbumClick(album, artistWithAlbums.getArtist());
                }
            });

            layoutAlbums.addView(albumView);
        }

        return convertView;
    }
}