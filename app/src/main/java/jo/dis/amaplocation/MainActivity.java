package jo.dis.amaplocation;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView locationInfoTv;

    private AMapLBSTask lbsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationInfoTv = findViewById(R.id.location_info_tv);

        lbsTask = AMapLBSTask.getInstance(this);

        lbsTask.addOnLocationReceiveListener(locationReceiveListener);

//        lbsTask.setIfUseCacheResult(false);

        lbsTask.requestLocationInfo();
    }

    /**
     * 定位相关
     */
    LocationTask.OnLocationReceiveListener locationReceiveListener = new LocationTask.OnLocationReceiveListener() {

        @Override
        public void onReceive(LocationTask.LocationInfo info, int result) {
            String resultInfo = "定位成功" +
                    "\n经度：" + info.longitude +
                    "\n纬度：" + info.latitude +
                    "\n省份：" + info.province +
                    "\n城市code：" + info.cityCode +
                    "\n城市：" + info.city +
                    "\n县(区): " + info.district +
                    "\n街道：" + info.street +
                    "\n街道号码：" + info.streetNumber +
                    "\n详细地址：" + info.address +
                    "\n省份id: " + info.adCode;

            locationInfoTv.setText(resultInfo);
        }

        @Override
        public void onError(int resultCode) {
            locationInfoTv.setTextColor(Color.RED);
            locationInfoTv.setText("定位失败，code = " + resultCode);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lbsTask != null) {
            lbsTask.removeOnLocationReceiveListener(locationReceiveListener);
        }
    }
}
