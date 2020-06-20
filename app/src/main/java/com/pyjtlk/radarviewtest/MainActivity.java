package com.pyjtlk.radarviewtest;

import android.animation.ObjectAnimator;
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

        //布局文件上设置attributeCount=6，则需要6个参数
        //attributeLevel=6，则各参数的取值范围为0-6
        int attrs[] = {6,1,2,3,4,5};
        radarChart.setAttributes(attrs);
        radarChart.setAttribute(0,1);
    }

    public void onClicked(View view) {
        radarChart.setAttribute(0,1);
        radarChart.setAttribute(1,4);
        radarChart.setAttribute(2,2);
    }

    public void onClicked2(View view) {
        radarChart.setAttribute(0,6);
        radarChart.setAttribute(1,2);
        radarChart.setAttribute(2,3);
    }
}
