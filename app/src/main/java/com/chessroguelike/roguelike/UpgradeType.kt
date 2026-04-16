package com.chessroguelike.roguelike

import android.os.Parcel
import android.os.Parcelable
import com.chessroguelike.engine.Ability
import com.chessroguelike.engine.PieceType

sealed class UpgradeType : Parcelable {
    data class AddPiece(val pieceType: PieceType) : UpgradeType() {
        constructor(parcel: Parcel) : this(PieceType.valueOf(parcel.readString()!!))
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(TYPE_ADD_PIECE)
            parcel.writeString(pieceType.name)
        }
        override fun describeContents() = 0
        companion object CREATOR : Parcelable.Creator<AddPiece> {
            override fun createFromParcel(parcel: Parcel) = AddPiece(parcel)
            override fun newArray(size: Int) = arrayOfNulls<AddPiece>(size)
        }
    }

    data class AddAbility(val ability: Ability) : UpgradeType() {
        constructor(parcel: Parcel) : this(Ability.valueOf(parcel.readString()!!))
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(TYPE_ADD_ABILITY)
            parcel.writeString(ability.name)
        }
        override fun describeContents() = 0
        companion object CREATOR : Parcelable.Creator<AddAbility> {
            override fun createFromParcel(parcel: Parcel) = AddAbility(parcel)
            override fun newArray(size: Int) = arrayOfNulls<AddAbility>(size)
        }
    }

    object Heal : UpgradeType() {
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(TYPE_HEAL)
        }
        override fun describeContents() = 0
        @JvmField
        val CREATOR = object : Parcelable.Creator<Heal> {
            override fun createFromParcel(parcel: Parcel) = Heal
            override fun newArray(size: Int) = arrayOfNulls<Heal>(size)
        }
    }

    companion object {
        const val TYPE_ADD_PIECE = 0
        const val TYPE_ADD_ABILITY = 1
        const val TYPE_HEAL = 2

        @JvmField
        val CREATOR = object : Parcelable.Creator<UpgradeType> {
            override fun createFromParcel(parcel: Parcel): UpgradeType {
                return when (parcel.readInt()) {
                    TYPE_ADD_PIECE -> AddPiece(PieceType.valueOf(parcel.readString()!!))
                    TYPE_ADD_ABILITY -> AddAbility(Ability.valueOf(parcel.readString()!!))
                    else -> Heal
                }
            }
            override fun newArray(size: Int) = arrayOfNulls<UpgradeType>(size)
        }
    }
}
