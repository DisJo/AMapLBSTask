package jo.dis.amaplocation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 高德定位
 * Created by diszhou on 2017/2/14.
 */
public class AMapLBSTask {

    private final static String TAG = "AMapLocation";

    public static final int INVALID_LONGITUDE = -99999;
    public static final int INVALID_LATITUDE = -99999;
    private final static String CACHE_LBS_HISTORY = "amap_lbs_history";
    private final static String CACHE_LBS_HISTORY_CODE = "amap_lbs_his";
    private final static String CACHE_LBS_HISTORY_LONGITUDE = "amap_lbs_history_longitude";
    private final static String CACHE_LBS_HISTORY_LATITUDE = "amap_lbs_history_latitude";
    private final static String CACHE_LBS_HISTORY_PROVINCE = "amap_lbs_history_province";
    private final static String CACHE_LBS_HISTORY_CITY = "amap_lbs_history_city";
    private final static String CACHE_LBS_HISTORY_CITY_CODE = "amap_lbs_history_city_code";
    private final static String CACHE_LBS_HISTORY_ADD_STR = "amap_lbs_history_add_str";
    private final static String CACHE_LBS_HISTORY_DISTRICT = "amap_lbs_history_district";
    private final static String CACHE_LBS_HISTORY_STREET = "amap_lbs_history_street";
    private final static String CACHE_LBS_HISTORY_STREET_NUMBER = "amap_lbs_history_street_number";
    private final static String CACHE_LBS_HISTORY_AD_CODE = "amap_lbs_history_ad_code";

    private SharedPreferences mCache;

    private int mResultCode;
    private double latitude = INVALID_LATITUDE;
    private double longitude = INVALID_LONGITUDE;
    private String province = "";
    private String city = "";
    private String citycode = "";
    // 详细地址
    private String addStr = "";
    // 县区
    private String district = "";
    // 街道
    private String street = "";
    // 街道号
    private String streetNumber = "";
    // 省份id
    private String adCode = "";

    // 定位失败时是否启用历史定位
    private boolean ifUseCacheResult = true;
    // 是否定位中
    private boolean isRequesting;
    // 一次定位有效时长
    private final static long VALID_LOCATION_DURATION = 1 * 60 * 1000L;
    // 最后一次定位成功时间
    private long lastLocateSuccessTime;

    private boolean locationSuccess = false;

    private AMapLocationClient mLocationClient;

    private Context mContext;

    private List<LocationTask.OnLocationReceiveListener> mOnLocationReceiveListeners = new ArrayList<>();
    private List<LocationTask.OnLocationReceiveListener> mPendingRemoveLocationReceiveListeners = new ArrayList<>();

    private static AMapLBSTask mInstance;

    public static AMapLBSTask getInstance(Context context) {
        synchronized(AMapLBSTask.class) {
            if (mInstance == null) {
                mInstance = new AMapLBSTask(context.getApplicationContext());
            }
            return mInstance;
        }
    }

    private AMapLBSTask(Context context) {
        this.mContext = context;
        mCache = context.getSharedPreferences(CACHE_LBS_HISTORY, Context.MODE_PRIVATE);
        mResultCode = mCache.getInt(CACHE_LBS_HISTORY_CODE, 0);
        // 获取默认地理位置
        double lat = Double.longBitsToDouble(mCache.getLong(CACHE_LBS_HISTORY_LATITUDE, INVALID_LATITUDE));
        double lon = Double.longBitsToDouble(mCache.getLong(CACHE_LBS_HISTORY_LONGITUDE, INVALID_LONGITUDE));
        if (!ifUseCacheResult || lat == INVALID_LATITUDE || lon == INVALID_LONGITUDE) {
            return;
        }

        latitude = lat;
        longitude = lon;
        province = mCache.getString(CACHE_LBS_HISTORY_PROVINCE, "");
        city = mCache.getString(CACHE_LBS_HISTORY_CITY, "");
        citycode = mCache.getString(CACHE_LBS_HISTORY_CITY_CODE, "");
        street = mCache.getString(CACHE_LBS_HISTORY_STREET, "");
        streetNumber = mCache.getString(CACHE_LBS_HISTORY_STREET_NUMBER, "");
        addStr = mCache.getString(CACHE_LBS_HISTORY_ADD_STR, "");
        district = mCache.getString(CACHE_LBS_HISTORY_DISTRICT, "");
        district = mCache.getString(CACHE_LBS_HISTORY_AD_CODE, "");
    }

    /**
     * 请求定位信息
     */
    public void requestLocationInfo() {
        if (isRequesting) {
            return;
        }
        if (lastLocateSuccessTime != 0
                && System.currentTimeMillis() - lastLocateSuccessTime <= VALID_LOCATION_DURATION) {
            // 没过定位信息有效时长无需再定位
            LocationTask.LocationInfo info = new LocationTask.LocationInfo();
            info.longitude = longitude;
            info.latitude = latitude;
            info.province = province;
            info.cityCode = citycode;
            info.city = city;
            info.district = district;
            info.street = street;
            info.streetNumber = streetNumber;
            info.address = addStr;
            info.adCode = adCode;
            onReceiveSuccess(info, mResultCode);
            return;
        }
        isRequesting = true;
        if (BuildConfig.DEBUG) {
            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Request location from " + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + ":" + stackTraceElement.getLineNumber());
            }
        }
        // 初始化client
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(mContext);
        }
        // 设置定位参数
        mLocationClient.setLocationOption(getDefaultOption());
        // 设置定位监听
        mLocationClient.setLocationListener(mLocationListener);
        mLocationClient.startLocation();
        if (!mLocationClient.isStarted()) {
            Log.d("location", "locClient is null or not started");
        }
    }

    /**
     * 默认的定位参数
     */
    private AMapLocationClientOption getDefaultOption(){
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);//可选，设置定位模式，Hight_Accuracy高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(2000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
        mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setLocationCacheEnable(false); //可选，设置是否使用缓存定位，默认为true
        return mOption;
    }

    /**
     * 定位监听
     */
    private AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            isRequesting = false;
            if (location != null) {
                mResultCode = location.getErrorCode();
                if (mResultCode == AMapLocation.LOCATION_SUCCESS) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    province = location.getProvince();
                    city = location.getCity();
                    citycode = location.getCityCode();
                    street = location.getStreet();
                    streetNumber = location.getStreetNum();
                    district = location.getDistrict();
                    addStr = location.getAddress();
                    adCode = location.getAdCode();

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "高德定位成功：" +
                                "\n经度：" + longitude +
                                "\n纬度：" + latitude +
                                "\n省份：" + province +
                                "\n城市code：" + citycode +
                                "\n城市：" + city +
                                "\n县(区): " + district +
                                "\n街道：" + street +
                                "\n街道号码：" + streetNumber +
                                "\n详细地址：" + addStr +
                                "\n省份id: " + adCode);
                    }

                    LocationTask.LocationInfo info = new LocationTask.LocationInfo();
                    info.longitude = longitude;
                    info.latitude = latitude;
                    info.province = province;
                    info.cityCode = citycode;
                    info.city = city;
                    info.district = district;
                    info.street = street;
                    info.streetNumber = streetNumber;
                    info.address = addStr;
                    info.adCode = adCode;

                    updateLocationCache(location, mResultCode);

                    onReceiveSuccess(info, mResultCode);
                    lastLocateSuccessTime = System.currentTimeMillis();
                } else {
                    onReceiveError(mResultCode);
                    Log.d(TAG, "高德定位失败, ErrCode:"
                            + location.getErrorCode() + ", errInfo:"
                            + location.getErrorInfo());
                }
            }
            mLocationClient.unRegisterLocationListener(this);
            mLocationClient.stopLocation();
        }
    };

    private void updateLocationCache(AMapLocation location, int resultCode) {
        SharedPreferences.Editor editor = mCache.edit();
        editor.putLong(CACHE_LBS_HISTORY_LONGITUDE, Double.doubleToRawLongBits(location.getLongitude()));
        editor.putLong(CACHE_LBS_HISTORY_LATITUDE, Double.doubleToRawLongBits(location.getLatitude()));
        editor.putString(CACHE_LBS_HISTORY_PROVINCE, location.getProvince());
        editor.putString(CACHE_LBS_HISTORY_CITY, location.getCity());
        editor.putString(CACHE_LBS_HISTORY_CITY_CODE, location.getCityCode());
        editor.putString(CACHE_LBS_HISTORY_STREET, location.getStreet());
        editor.putString(CACHE_LBS_HISTORY_STREET_NUMBER, location.getStreetNum());
        editor.putString(CACHE_LBS_HISTORY_ADD_STR, location.getAddress());
        editor.putString(CACHE_LBS_HISTORY_DISTRICT, location.getDistrict());
        editor.putString(CACHE_LBS_HISTORY_AD_CODE, location.getAdCode());
        editor.putInt(CACHE_LBS_HISTORY_CODE, resultCode);
        editor.apply();
    }

    private void onReceiveError(int resultCode) {
        double la = Double.longBitsToDouble(mCache.getLong(CACHE_LBS_HISTORY_LATITUDE, INVALID_LATITUDE));
        double lo = Double.longBitsToDouble(mCache.getLong(CACHE_LBS_HISTORY_LONGITUDE, INVALID_LONGITUDE));
        int code = mCache.getInt(CACHE_LBS_HISTORY_CODE, -99999);
        if (!ifUseCacheResult || la == INVALID_LATITUDE || lo == INVALID_LONGITUDE || Double.isNaN(la) || Double.isNaN(lo)) {
            notifyOnError(resultCode);
            return;
        }

        latitude = la;
        longitude = lo;
        province = mCache.getString(CACHE_LBS_HISTORY_PROVINCE, "");
        city = mCache.getString(CACHE_LBS_HISTORY_CITY, "");
        citycode = mCache.getString(CACHE_LBS_HISTORY_CITY_CODE, "");
        street = mCache.getString(CACHE_LBS_HISTORY_STREET, "");
        streetNumber = mCache.getString(CACHE_LBS_HISTORY_STREET_NUMBER, "");
        addStr = mCache.getString(CACHE_LBS_HISTORY_ADD_STR, "");
        district = mCache.getString(CACHE_LBS_HISTORY_DISTRICT, "");
        adCode = mCache.getString(CACHE_LBS_HISTORY_AD_CODE, "");

        LocationTask.LocationInfo info = new LocationTask.LocationInfo();
        info.longitude = longitude;
        info.latitude = latitude;
        info.province = province;
        info.cityCode = citycode;
        info.city = city;
        info.district = district;
        info.street = street;
        info.streetNumber = streetNumber;
        info.address = addStr;
        info.adCode = adCode;

        onReceiveSuccess(info, code);
    }

    private void onReceiveSuccess(LocationTask.LocationInfo location, int resultCode) {
        locationSuccess = true;
        notifyOnReceive(location, resultCode);
    }

    public void removeOnLocationReceiveListener(LocationTask.OnLocationReceiveListener listener) {
        if (null != listener && !mPendingRemoveLocationReceiveListeners.contains(listener)) {
            mPendingRemoveLocationReceiveListeners.add(listener);
        }
    }

    public void addOnLocationReceiveListener(LocationTask.OnLocationReceiveListener listener) {
        if (listener != null && !mOnLocationReceiveListeners.contains(listener)) {
            mOnLocationReceiveListeners.add(listener);
        }
    }

    private void notifyOnReceive(LocationTask.LocationInfo location, int resultCode) {
        for (int i = 0, size = mOnLocationReceiveListeners.size(); i < size; i++) {
            LocationTask.OnLocationReceiveListener listener = mOnLocationReceiveListeners.get(i);
            listener.onReceive(location, resultCode);
        }
        mOnLocationReceiveListeners.removeAll(mPendingRemoveLocationReceiveListeners);
        mPendingRemoveLocationReceiveListeners.clear();
    }

    private void notifyOnError(int resultCode) {
        String errMsg = resultCode + ": 未知错误";
        if (resultCode == AMapLocation.ERROR_CODE_INVALID_PARAMETER) {
            errMsg = "ERROR_CODE_INVALID_PARAMETER: 一些重要参数为空，如context";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_WIFI_INFO) {
            errMsg = "ERROR_CODE_FAILURE_WIFI_INFO: 定位失败，由于仅扫描到单个wifi，且没有基站信息";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_LOCATION_PARAMETER) {
            errMsg = "ERROR_CODE_FAILURE_LOCATION_PARAMETER: 获取到的请求参数为空，可能获取过程中出现异常";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_CONNECTION) {
            errMsg = "ERROR_CODE_FAILURE_CONNECTION: 请求服务器过程中的异常，多为网络情况差，链路不通导致";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_PARSER) {
            errMsg = "ERROR_CODE_FAILURE_PARSER: 请求被恶意劫持，定位结果解析失败";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_LOCATION) {
            errMsg = "ERROR_CODE_FAILURE_LOCATION: 定位服务返回定位失败";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_AUTH) {
            errMsg = "ERROR_CODE_FAILURE_AUTH: KEY鉴权失败";
        } else if (resultCode == AMapLocation.ERROR_CODE_UNKNOWN) {
            errMsg = "ERROR_CODE_UNKNOWN: Android exception常规错误";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_INIT) {
            errMsg = "ERROR_CODE_FAILURE_INIT: 定位初始化时出现异常";
        } else if (resultCode == AMapLocation.ERROR_CODE_SERVICE_FAIL) {
            errMsg = "ERROR_CODE_SERVICE_FAIL: 定位客户端启动失败";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_CELL) {
            errMsg = "ERROR_CODE_FAILURE_CELL: 定位时的基站信息错误";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_LOCATION_PERMISSION) {
            errMsg = "ERROR_CODE_FAILURE_LOCATION_PERMISSION: 缺少定位权限";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_NOWIFIANDAP) {
            errMsg = "ERROR_CODE_FAILURE_NOWIFIANDAP: 定位失败，由于设备未开启WIFI模块或未插入SIM卡，且GPS当前不可用";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_NOENOUGHSATELLITES) {
            errMsg = "ERROR_CODE_FAILURE_NOENOUGHSATELLITES: GPS 定位失败，由于设备当前 GPS 状态差";
        } else if (resultCode == AMapLocation.ERROR_CODE_FAILURE_SIMULATION_LOCATION) {
            errMsg = "ERROR_CODE_FAILURE_SIMULATION_LOCATION: 定位结果被模拟导致定位失败";
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, errMsg);
        }

        for (int i = 0, size = mOnLocationReceiveListeners.size(); i < size; i++) {
            LocationTask.OnLocationReceiveListener listener = mOnLocationReceiveListeners.get(i);
            listener.onError(resultCode);
        }
        mOnLocationReceiveListeners.removeAll(mPendingRemoveLocationReceiveListeners);
        mPendingRemoveLocationReceiveListeners.clear();
    }

    public boolean isLocationSuccess() {
        return locationSuccess;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCityCode() {
        return citycode;
    }

    public String getCityName() {
        return city;
    }

    public String getProvince() {
        return province;
    }

    public String getDistrict() {
        return district;
    }

    public String getStreet() {
        return street;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public String getAddStr() {
        return addStr;
    }

    public String getAdCode() {
        return adCode;
    }

    public LocationTask.LocationInfo getLocationInfo() {
        if (latitude == INVALID_LATITUDE || longitude == INVALID_LONGITUDE || Double.isNaN(latitude) || Double.isNaN(longitude)) {
            return null;
        }
        LocationTask.LocationInfo info = new LocationTask.LocationInfo();
        info.longitude = longitude;
        info.latitude = latitude;
        info.province = province;
        info.cityCode = citycode;
        info.city = city;
        info.district = district;
        info.street = street;
        info.streetNumber = streetNumber;
        info.address = addStr;
        info.adCode = adCode;
        return info;
    }

    public int getResultCode() {
        return mResultCode;
    }

    /**
     * 当高德定位失败时是否使用缓存的历史结果
     *
     * @param ifUseCacheResult 默认为true
     */
    public void setIfUseCacheResult(boolean ifUseCacheResult) {
        this.ifUseCacheResult = ifUseCacheResult;
    }
}
