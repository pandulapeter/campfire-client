package com.pandulapeter.campfire.data.source.localImpl.implementation.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = SongEntity.TABLE_NAME)
internal data class SongEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "artist") val artist: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "isExplicit") val isExplicit: Boolean,
    @ColumnInfo(name = "hasChords") val hasChords: Boolean,
    @ColumnInfo(name = "isPublic") val isPublic: Boolean,
    @ColumnInfo(name = SHEET_URL) val sheetUrl: String
) {

    companion object {
        const val TABLE_NAME = "songs"
        const val SHEET_URL = "sheetUrl"
    }
}