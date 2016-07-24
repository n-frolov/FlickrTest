package com.example.flickrtest;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.flickrapi.FlickrApi;
import com.example.flickrobjects.Photo;
import com.example.flickrtest.flickrtest.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PhotosAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnLastItemBoundListener {
        void onLastItemBound();
    }

    private List<Photo> items = new ArrayList<>();
    private OnLastItemBoundListener listener;

    public void reset() {
        this.items.clear();
        notifyDataSetChanged();
    }

    public void setItems(@NonNull List<Photo> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setLastItemBoundListener(OnLastItemBoundListener listener) {
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Photo item = items.get(position);
        String url = FlickrApi.urlForPhoto(item);
        ImageView imageView = ((PhotoViewHolder)holder).imageView;
        Picasso.with(imageView.getContext()).load(url).into(imageView);

        if (position == items.size()-1 && listener != null)
            listener.onLastItemBound();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;

        public PhotoViewHolder(View view) {
            super(view);
            imageView = (ImageView) view.findViewById(R.id.imageView);
        }
    }

}
