package jo.dis.amaplocation;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class LocationTask {

    public static final int GPS_RESULT = 61;

    public static final int NETWORK_RESULT = 161;

    public abstract void getLocationInfo();

    /**
     * 最近一次定位的信息
     */
    public static LocationInfo info = null;

    public abstract void setOnLocationReceiveListener(
            OnLocationReceiveListener mOnLocationReceiveListener);

    public static class LocationInfo implements Parcelable {
        public double longitude;    // 经度
        public double latitude;     // 纬度
        public String province;     // 省份
        public String cityCode;     // 城市code
        public String city;         // 城市
        public String district;     // 县(区)
        public String street;       // 街道
        public String streetNumber; // 街道号码
        public String address;      // 详细地址
        public String adCode;       // 省份id

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeDouble(longitude);
            dest.writeDouble(latitude);
            dest.writeString(province);
            dest.writeString(cityCode);
            dest.writeString(city);
            dest.writeString(district);
            dest.writeString(street);
            dest.writeString(streetNumber);
            dest.writeString(address);
            dest.writeString(adCode);
        }

        public static final Parcelable.Creator<LocationInfo> CREATOR = new Parcelable.Creator<LocationInfo>() {
            public LocationInfo createFromParcel(Parcel in) {
                LocationInfo info = new LocationInfo();
                info.longitude = in.readDouble();
                info.latitude = in.readDouble();
                info.cityCode = in.readString();
                info.city = in.readString();
                info.district = in.readString();
                info.street = in.readString();
                info.streetNumber = in.readString();
                info.address = in.readString();
                info.adCode = in.readString();
                return info;
            }

            @Override
            public LocationInfo[] newArray(int size) {
                return new LocationInfo[size];
            }
        };
    }

    public interface OnLocationReceiveListener {
        void onReceive(LocationInfo info, int result);

        void onError(int resultCode);
    }
}