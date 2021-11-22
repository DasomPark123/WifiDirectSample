package ex.dev.tool.wifidirectsample.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WifiDirectEntity {

    @Expose
    @SerializedName("ssid")
    private String ssid;

    @Expose
    @SerializedName("ip")
    private String ip;

    @Expose
    @SerializedName("port")
    private int port;

    @Expose
    @SerializedName("file_name")
    private String fileName;

    @Expose
    @SerializedName("file_size")
    long fileSize;

    public WifiDirectEntity(String ssid, String ip, int port, String fileName, long fileSize) {
        this.ssid = ssid;
        this.ip = ip;
        this.port = port;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
