package com.example.bluetoothtutorial2_chat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class SlaveView extends AppCompatActivity {

    TextView tv_slave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slave_view);

        tv_slave = findViewById(R.id.tv_slave);
    }


}
