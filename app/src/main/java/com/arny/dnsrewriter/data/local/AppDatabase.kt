package com.arny.dnsrewriter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DnsRuleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dnsRuleDao(): DnsRuleDao
}