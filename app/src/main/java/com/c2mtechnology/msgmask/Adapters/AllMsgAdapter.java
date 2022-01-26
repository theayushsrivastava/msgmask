package com.c2mtechnology.msgmask.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.c2mtechnology.msgmask.ChatActivity;
import com.c2mtechnology.msgmask.Models.Sms;
import com.c2mtechnology.msgmask.R;

import java.util.ArrayList;

public class AllMsgAdapter extends RecyclerView.Adapter<AllMsgAdapter.ViewHolder> {

    ArrayList<Sms> smsArrayList;
    Context mcontext;

    public AllMsgAdapter(ArrayList<Sms> smsArrayList,Context context) {
        this.smsArrayList = smsArrayList;
        this.mcontext = context;
    }

    @NonNull
    @org.jetbrains.annotations.NotNull
    @Override
    public AllMsgAdapter.ViewHolder onCreateViewHolder(@NonNull @org.jetbrains.annotations.NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_msg_card,parent,false);
        return new AllMsgAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @org.jetbrains.annotations.NotNull AllMsgAdapter.ViewHolder holder, int position) {
        Sms sms = smsArrayList.get(position);
        holder.bind(sms);
    }

    @Override
    public int getItemCount() {
        return smsArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView address_tv,body_tv,alert_tv,date_tv;
        RelativeLayout relativeLayout;

        public ViewHolder(@NonNull @org.jetbrains.annotations.NotNull View itemView) {
            super(itemView);
            address_tv      = itemView.findViewById(R.id.address_tv);
            body_tv         = itemView.findViewById(R.id.body_tv);
            alert_tv        = itemView.findViewById(R.id.alert_tv);
            date_tv         = itemView.findViewById(R.id.date_tv);
            relativeLayout  = itemView.findViewById(R.id.customer_rl);
        }

        public void bind(Sms sms)
        {
            address_tv.setText(sms.getAddress());
            body_tv.setText(sms.getMsg());
            date_tv.setText(sms.getRelativeTime());
            int isSpam = sms.getIsSpam(mcontext);
            if(isSpam == 1)
            {
                alert_tv.setVisibility(View.VISIBLE);
            }else{
                alert_tv.setVisibility(View.GONE);
            }

            relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mcontext, ChatActivity.class);
                    intent.putExtra("threadID",sms.get_threadId());
                    intent.putExtra("address",sms.getAddress());
                    mcontext.startActivity(intent);
                }
            });
        }
    }
}
