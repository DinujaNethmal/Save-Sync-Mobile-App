package com.example.personalfinancetrackerapp

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

enum class TransactionType {
    INCOME,
    EXPENSE
}

data class Transaction(
    val id: Long,
    val amount: Double,
    val category: String,
    val date: Date,
    val type: TransactionType
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readDouble(),
        parcel.readString() ?: "",
        Date(parcel.readLong()),
        TransactionType.valueOf(parcel.readString() ?: "EXPENSE")
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeDouble(amount)
        parcel.writeString(category)
        parcel.writeLong(date.time)
        parcel.writeString(type.name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Transaction> {
        override fun createFromParcel(parcel: Parcel): Transaction {
            return Transaction(parcel)
        }

        override fun newArray(size: Int): Array<Transaction?> {
            return arrayOfNulls(size)
        }
    }
} 