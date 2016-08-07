package org.rm3l.ddwrt.common.resources;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by rm3l on 07/08/16.
 */
public class RouterInfo implements Parcelable {

    /**
     * the internal id (in DB)
     */
    private int id = -1;

    @NonNull
    private String uuid;

    /**
     * the router name
     */
    @Nullable
    private String name;

    /**
     * the connection protocol
     */
    @NonNull
    private String routerConnectionProtocol;

    /**
     * the router IP or DNS
     */
    @NonNull
    private String remoteIpAddress;

    /**
     * the port to connect on
     */
    private int remotePort = -1;

    private String routerFirmware;

    @Nullable
    private String routerModel;

    @NonNull
    private boolean isDemoRouter;

    @NonNull
    public String getUuid() {
        return uuid;
    }

    public RouterInfo setUuid(@NonNull String uuid) {
        this.uuid = uuid;
        return this;
    }

    public int getId() {
        return id;
    }

    public RouterInfo setId(int id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public RouterInfo setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @NonNull
    public String getRouterConnectionProtocol() {
        return routerConnectionProtocol;
    }

    public RouterInfo setRouterConnectionProtocol(@NonNull String routerConnectionProtocol) {
        this.routerConnectionProtocol = routerConnectionProtocol;
        return this;
    }

    @NonNull
    public String getRemoteIpAddress() {
        return remoteIpAddress;
    }

    public RouterInfo setRemoteIpAddress(@NonNull String remoteIpAddress) {
        this.remoteIpAddress = remoteIpAddress;
        return this;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public RouterInfo setRemotePort(int remotePort) {
        this.remotePort = remotePort;
        return this;
    }

    public String getRouterFirmware() {
        return routerFirmware;
    }

    public RouterInfo setRouterFirmware(String routerFirmware) {
        this.routerFirmware = routerFirmware;
        return this;
    }

    @Nullable
    public String getRouterModel() {
        return routerModel;
    }

    public RouterInfo setRouterModel(@Nullable String routerModel) {
        this.routerModel = routerModel;
        return this;
    }

    @NonNull
    public boolean isDemoRouter() {
        return isDemoRouter;
    }

    public RouterInfo setDemoRouter(@NonNull boolean demoRouter) {
        isDemoRouter = demoRouter;
        return this;
    }

    public RouterInfo() {}

    protected RouterInfo(Parcel in) {
        id = in.readInt();
        uuid = in.readString();
        name = in.readString();
        routerConnectionProtocol = in.readString();
        remoteIpAddress = in.readString();
        remotePort = in.readInt();
        routerFirmware = in.readString();
        routerModel = in.readString();
        final boolean[] booleanArr = new boolean[1];
        in.readBooleanArray(booleanArr);
        isDemoRouter = booleanArr[0];
    }

    public static final Creator<RouterInfo> CREATOR = new Creator<RouterInfo>() {
        @Override
        public RouterInfo createFromParcel(Parcel in) {
            return new RouterInfo(in);
        }

        @Override
        public RouterInfo[] newArray(int size) {
            return new RouterInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(uuid);
        parcel.writeString(name);
        parcel.writeString(routerConnectionProtocol);
        parcel.writeString(remoteIpAddress);
        parcel.writeInt(remotePort);
        parcel.writeString(routerFirmware);
        parcel.writeString(routerModel);
        parcel.writeBooleanArray(new boolean[] {isDemoRouter});
    }

    @Override
    public String toString() {
        return "RouterInfo{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", routerConnectionProtocol='" + routerConnectionProtocol + '\'' +
                ", remoteIpAddress='" + remoteIpAddress + '\'' +
                ", remotePort=" + remotePort +
                ", routerFirmware='" + routerFirmware + '\'' +
                ", routerModel='" + routerModel + '\'' +
                ", isDemoRouter=" + isDemoRouter +
                '}';
    }
}
