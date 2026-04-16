package com.chessroguelike.roguelike

import android.os.Parcel
import android.os.Parcelable

data class Upgrade(
    val id: Int,
    val name: String,
    val description: String,
    val upgradeType: UpgradeType,
    val icon: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        name = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        upgradeType = parcel.readParcelable(UpgradeType::class.java.classLoader)!!,
        icon = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeParcelable(upgradeType as Parcelable, flags)
        parcel.writeString(icon)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Upgrade> {
        override fun createFromParcel(parcel: Parcel) = Upgrade(parcel)
        override fun newArray(size: Int) = arrayOfNulls<Upgrade>(size)
    }
}
