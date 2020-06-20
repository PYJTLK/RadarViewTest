package com.pyjtlk.radarviewtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.pyjtlk.radarview.RadarChart;

public class MainActivity extends AppCompatActivity {

    private RadarChart radarChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radarChart = findViewById(R.id.radarChart);
        int attrs[] = {6,1,2,3,4,5};
        radarChart.setAttributes(attrs);
        radarChart.setAttribute(0,1);
    }

    public void onClicked(View view) {
        int attrs[] = {4,5,6,7,1,2};
        radarChart.setAttributes(attrs);
    }
}
