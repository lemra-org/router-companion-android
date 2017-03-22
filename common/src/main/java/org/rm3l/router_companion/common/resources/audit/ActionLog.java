package org.rm3l.router_companion.common.resources.audit;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Created by rm3l on 06/09/16.
 */
public class ActionLog implements Parcelable {

  public static final Creator<ActionLog> CREATOR = new Creator<ActionLog>() {
    @Override public ActionLog createFromParcel(Parcel in) {
      return new ActionLog(in);
    }

    @Override public ActionLog[] newArray(int size) {
      return new ActionLog[size];
    }
  };
  /**
   * the internal id (in DB)
   */
  private long id = -1l;
  private String uuid;
  //Package name
  //UNKNOWN, if unknown
  private String originPackageName;
  @NonNull private String router;
  @NonNull
  //YYYY-MM-dd
  private String date;
  private String actionName;
  private String actionData;
  //0 => success
  private int status;

  public ActionLog() {
  }

  protected ActionLog(Parcel in) {
    id = in.readLong();
    uuid = in.readString();
    originPackageName = in.readString();
    router = in.readString();
    date = in.readString();
    actionName = in.readString();
    actionData = in.readString();
    status = in.readInt();
  }

  public long getId() {
    return id;
  }

  public ActionLog setId(long id) {
    this.id = id;
    return this;
  }

  public String getOriginPackageName() {
    return originPackageName;
  }

  public ActionLog setOriginPackageName(String originPackageName) {
    this.originPackageName = originPackageName;
    return this;
  }

  @NonNull public String getRouter() {
    return router;
  }

  public ActionLog setRouter(@NonNull String router) {
    this.router = router;
    return this;
  }

  @NonNull public String getDate() {
    return date;
  }

  public ActionLog setDate(@NonNull String date) {
    this.date = date;
    return this;
  }

  public String getActionName() {
    return actionName;
  }

  public ActionLog setActionName(String actionName) {
    this.actionName = actionName;
    return this;
  }

  public String getActionData() {
    return actionData;
  }

  public ActionLog setActionData(String actionData) {
    this.actionData = actionData;
    return this;
  }

  public int getStatus() {
    return status;
  }

  public ActionLog setStatus(int status) {
    this.status = status;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public ActionLog setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel parcel, int i) {
    parcel.writeLong(id);
    parcel.writeString(uuid);
    parcel.writeString(originPackageName);
    parcel.writeString(router);
    parcel.writeString(date);
    parcel.writeString(actionName);
    parcel.writeString(actionData);
    parcel.writeInt(status);
  }

  @Override public String toString() {
    return "ActionLog{"
        + "id="
        + id
        + ", uuid='"
        + uuid
        + '\''
        + ", originPackageName='"
        + originPackageName
        + '\''
        + ", router='"
        + router
        + '\''
        + ", date='"
        + date
        + '\''
        + ", actionName='"
        + actionName
        + '\''
        + ", actionData='"
        + actionData
        + '\''
        + ", status="
        + status
        + '}';
  }
}
