package com.example.btl.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.btl.Domain.Model.Message;
import com.example.btl.Domain.Model.User;
import com.example.btl.R;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messageList;
    private String currentUserId;
    private User otherUser;

    public MessageAdapter(List<Message> messageList, String currentUserId, User otherUser) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.otherUser = otherUser;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (message.getSenderId().equals(currentUserId)) {
            // Tin nhắn của người dùng hiện tại
            holder.myMessageLayout.setVisibility(View.VISIBLE);
            holder.otherMessageLayout.setVisibility(View.GONE);
            holder.myMessageText.setText(message.getContent());
        } else {
            // Tin nhắn của người khác
            holder.otherMessageLayout.setVisibility(View.VISIBLE);
            holder.myMessageLayout.setVisibility(View.GONE);
            holder.otherMessageText.setText(message.getContent());
            if (otherUser.getAvatar() != null && !otherUser.getAvatar().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(otherUser.getAvatar())
                        .placeholder(R.drawable.default_avatar)
                        .into(holder.otherUserAvatar);
            } else {
                holder.otherUserAvatar.setImageResource(R.drawable.default_avatar);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout myMessageLayout;
        TextView myMessageText;
        LinearLayout otherMessageLayout;
        TextView otherMessageText;
        ImageView otherUserAvatar;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            myMessageLayout = itemView.findViewById(R.id.myMessageLayout);
            myMessageText = itemView.findViewById(R.id.myMessageText);
            otherMessageLayout = itemView.findViewById(R.id.otherMessageLayout);
            otherMessageText = itemView.findViewById(R.id.otherMessageText);
            otherUserAvatar = itemView.findViewById(R.id.otherUserAvatar);
        }
    }
}
