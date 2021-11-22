package ex.dev.tool.wifidirectsample.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class QRDataEntity {

    @Expose
    @SerializedName("wifi_direct")
    private WifiDirectEntity wifiDirectEntity;

    public QRDataEntity(WifiDirectEntity wifiDirectEntity) {
        this.wifiDirectEntity = wifiDirectEntity;
    }

    public WifiDirectEntity getWifiDirectEntity() {
        return wifiDirectEntity;
    }
    public void setWifiDirectEntity(WifiDirectEntity wifiDirectEntity) {
        this.wifiDirectEntity = wifiDirectEntity;
    }
}
