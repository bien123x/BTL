package com.example.btl.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.btl.Domain.Model.User;
import com.example.btl.R;
import java.util.List;

public class MessageUserAdapter extends RecyclerView.Adapter<MessageUserAdapter.MessageUserViewHolder> {
    private List<User> userList;
    private OnUserClickListener onUserClickListener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public MessageUserAdapter(List<User> userList, OnUserClickListener onUserClickListener) {
        this.userList = userList;
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public MessageUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_user, parent, false);
        return new MessageUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageUserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.userName.setText(user.getName());

        // Tải avatar
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getAvatar())
                    .placeholder(R.drawable.default_avatar)
                    .into(holder.userAvatar);
        } else {
            holder.userAvatar.setImageResource(R.drawable.default_avatar);
        }

        // Hiển thị trạng thái online
        holder.onlineStatus.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);

        // Xử lý sự kiện nhấn vào người dùng
        holder.itemView.setOnClickListener(v -> onUserClickListener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class MessageUserViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar;
        TextView userName;
        View onlineStatus;

        public MessageUserViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            onlineStatus = itemView.findViewById(R.id.onlineStatus);
        }
    }
}
