package com.example.btl.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.example.btl.Domain.Model.Artifact;
import com.example.btl.R;
import java.util.List;

public class CollectedArtifactAdapter extends BaseAdapter {
    private Context context;
    private List<Artifact> artifactList;

    public CollectedArtifactAdapter(Context context, List<Artifact> artifactList) {
        this.context = context;
        this.artifactList = artifactList;
    }

    @Override
    public int getCount() {
        return artifactList.size();
    }

    @Override
    public Object getItem(int position) {
        return artifactList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_collected_artifact, parent, false);
        }

        ImageView artifactImage = (ImageView) convertView;
        Artifact artifact = artifactList.get(position);

        if (artifact.getImageUrl() != null && !artifact.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(artifact.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(artifactImage);
        } else {
            artifactImage.setImageResource(R.drawable.placeholder_image);
        }

        return convertView;
    }
}
