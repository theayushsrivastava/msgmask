package com.c2mtechnology.msgmask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.c2mtechnology.msgmask.Adapters.SpamMessageAdapter;
import com.c2mtechnology.msgmask.Classes.DataBaseHelper;
import com.c2mtechnology.msgmask.Models.Sms;

import java.util.ArrayList;

public class SpamActivity extends AppCompatActivity {

    ArrayList<Sms> addresses;
    RecyclerView recyclerView;
    SpamMessageAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spam);

        addresses = new ArrayList<>();
        recyclerView    = findViewById(R.id.rl1);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        findViewById(R.id.imageButton6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        getData();
    }

    private void getData()
    {
        addresses   = DataBaseHelper.getInstance(this).getAllSpamAddresses();
        adapter     = new SpamMessageAdapter(addresses,this);
        recyclerView.setAdapter(adapter);
    }
}