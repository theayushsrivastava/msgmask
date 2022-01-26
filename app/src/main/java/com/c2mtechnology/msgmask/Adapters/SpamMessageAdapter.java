package com.c2mtechnology.msgmask.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.c2mtechnology.msgmask.ChatActivity;
import com.c2mtechnology.msgmask.Models.Sms;
import com.c2mtechnology.msgmask.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class SpamMessageAdapter extends RecyclerView.Adapter<SpamMessageAdapter.ViewHolder> {

    ArrayList<Sms> addresses;
    Context mcontext;
    Drawable drawable;

    public SpamMessageAdapter(ArrayList<Sms> addresses, Context mcontext) {
        this.addresses = addresses;
        this.mcontext = mcontext;
        drawable    = mcontext.getResources().getDrawable(R.drawable.ic_exclamation);
    }

    @NonNull
    @NotNull
    @Override
    public SpamMessageAdapter.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_msg_card,parent,false);
        return new SpamMessageAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull SpamMessageAdapter.ViewHolder holder, int position) {
        Sms sms = addresses.get(position);
        holder.bind(sms);
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView address_tv,body_tv,alert_tv,date_tv;
        RelativeLayout relativeLayout;
        ImageView imageView;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            address_tv      = itemView.findViewById(R.id.address_tv);

            body_tv         = itemView.findViewById(R.id.body_tv);
            alert_tv        = itemView.findViewById(R.id.alert_tv);
            date_tv         = itemView.findViewById(R.id.date_tv);
            relativeLayout  = itemView.findViewById(R.id.customer_rl);
            imageView       = itemView.findViewById(R.id.firstChar_cv);
            imageView.setImageDrawable(drawable);
        }

        public void bind(Sms sms)
        {

            address_tv.setText(sms.getAddress());
            String body = sms.getLastMessageFromDB(mcontext);
            if(body == null)
            {
                body_tv.setText("");
                date_tv.setText("");
            }else{
                body_tv.setText(body);
                date_tv.setText(sms.getRelativeTime());
            }
            relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Long threadID = sms.get_threadId();
                    if(threadID == null)
                    {
                        Toast.makeText(mcontext,"No messages found",Toast.LENGTH_SHORT).show();
                    }else{
                        Intent intent = new Intent(mcontext, ChatActivity.class);
                        intent.putExtra("threadID",sms.get_threadId());
                        intent.putExtra("address",sms.getAddress());
                        mcontext.startActivity(intent);
                    }

                }
            });
        }


    }
}
