package com.c2mtechnology.msgmask.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.c2mtechnology.msgmask.Models.Sms;
import com.c2mtechnology.msgmask.R;
import com.developer.messageview.MessageView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    public interface ShowSmsCheckDialog
    {
        void showSmsCheckerDialog(Sms sms);
    }
    ArrayList<Sms> smsArrayList;
    Context context;
    ShowSmsCheckDialog callback;
    int color1,color1_pressed,color2,color2_pressed,color_white,color_grey,color_black;

    public ChatAdapter(ArrayList<Sms> smsArrayList, Context context,ShowSmsCheckDialog callback) {
        this.smsArrayList = smsArrayList;
        this.context = context;
        this.callback = callback;

        color1 = context.getResources().getColor(R.color.color2);
        color1_pressed = context.getResources().getColor(R.color.color1);
        color2          = context.getResources().getColor(R.color.color3);
        color2_pressed  = context.getResources().getColor(R.color.color4);
        color_white     = context.getResources().getColor(R.color.white);
        color_grey      = context.getResources().getColor(R.color.white_five);
        color_black     = context.getResources().getColor(R.color.black);
    }


    @NonNull
    @NotNull
    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_layout,parent,false);
        return new ChatAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ChatAdapter.ViewHolder holder, int position) {
        Sms sms = smsArrayList.get(position);
        holder.bind(sms);
    }

    @Override
    public int getItemCount() {
        return smsArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView msg_tv,date_tv,alert_tv;
        MessageView messageView;
        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            msg_tv      = itemView.findViewById(R.id.text);
            messageView = itemView.findViewById(R.id.message_view);
            alert_tv    = itemView.findViewById(R.id.alert_tv2);
            date_tv     = itemView.findViewById(R.id.date_tv);
        }

        @SuppressLint("ResourceAsColor")
        public void bind(Sms sms)
        {
            msg_tv.setText(sms.getMsg());
            if(sms.getMessageType() == Telephony.Sms.MESSAGE_TYPE_SENT)
            {
                messageView.setArrowPosition(MessageView.ArrowPosition.RIGHT);
                messageView.setBackgroundColor(color1,color1_pressed);
                msg_tv.setTextColor(color_white);
                date_tv.setTextColor(color_grey);
            }else{
                messageView.setArrowPosition(MessageView.ArrowPosition.LEFT);
                messageView.setBackgroundColor(color2,color2_pressed);
                msg_tv.setTextColor(color_black);
                date_tv.setTextColor(color_black);
            }

            int is_spam = sms.getIsSpam(context);
            if(is_spam == 1)
            {
                alert_tv.setVisibility(View.VISIBLE);
            }else{
                alert_tv.setVisibility(View.GONE);
            }
            date_tv.setText(sms.getRelativeTime());

            messageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    callback.showSmsCheckerDialog(sms);
                    return true;
                }
            });
        }
    }
}
