package com.arny.dnsrewriter.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsRuleDao {
    @Query("SELECT * FROM dns_rules ORDER BY domain ASC")
    fun getRules(): Flow<List<DnsRuleEntity>>

    @Query("SELECT * FROM dns_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<DnsRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: DnsRuleEntity)

    @Delete
    suspend fun delete(rule: DnsRuleEntity)

    @Update
    suspend fun update(rule: DnsRuleEntity)

    @Transaction
    suspend fun replaceAll(rules: List<DnsRuleEntity>) {
        deleteAll() // Сначала удаляем все старые
        insertAll(rules) // Затем вставляем новые
    }

    @Query("DELETE FROM dns_rules")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<DnsRuleEntity>)
}