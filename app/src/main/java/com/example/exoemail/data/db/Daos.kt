package com.example.exoemail.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY email")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun get(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE email = :email LIMIT 1")
    suspend fun byAddress(email: String): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY email")
    suspend fun all(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Query("SELECT * FROM accounts ORDER BY email LIMIT 1")
    suspend fun first(): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Query("UPDATE accounts SET lastSyncAt = :at, lastUid = :uid WHERE id = :id")
    suspend fun updateSyncState(id: String, at: Long, uid: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE accountId = :accountId ORDER BY unreadCount DESC")
    fun observe(accountId: String): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(folders: List<FolderEntity>)

    @Query("DELETE FROM folders WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE accountId = :accountId AND folder = :folder ORDER BY date DESC")
    fun pagingFolder(accountId: String, folder: String): PagingSource<Int, MessageEntity>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId " +
            "AND (:q = '' OR subject LIKE '%' || :q || '%' OR senderName LIKE '%' || :q || '%' OR sender LIKE '%' || :q || '%' OR snippet LIKE '%' || :q || '%') " +
            "ORDER BY date DESC"
    )
    fun searchAll(accountId: String, q: String): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun get(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = :read WHERE id = :id")
    suspend fun setRead(id: String, read: Boolean)

    @Query("UPDATE messages SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)

    @Query("UPDATE messages SET bodyPath = :path WHERE id = :id")
    suspend fun setBodyPath(id: String, path: String)

    @Query("UPDATE messages SET body = :body WHERE id = :id")
    suspend fun setBody(id: String, body: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT MAX(uid) FROM messages WHERE accountId = :accountId AND folder = 'INBOX'")
    suspend fun maxUid(accountId: String): Long?

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND isStarred = 1 " +
            "ORDER BY date DESC"
    )
    fun pagingStarred(accountId: String): PagingSource<Int, MessageEntity>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND folder = 'Sent' " +
            "ORDER BY date DESC"
    )
    fun pagingSent(accountId: String): PagingSource<Int, MessageEntity>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND folder = 'Drafts' " +
            "ORDER BY date DESC"
    )
    fun pagingDrafts(accountId: String): PagingSource<Int, MessageEntity>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND folder = 'INBOX' AND isRead = 0 " +
            "ORDER BY date DESC"
    )
    fun pagingUnread(accountId: String): PagingSource<Int, MessageEntity>

    @Query("UPDATE messages SET folder = :folder WHERE id = :id")
    suspend fun moveTo(id: String, folder: String)

    @Query("DELETE FROM messages WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AttachmentEntity>)

    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    suspend fun forMessage(messageId: String): List<AttachmentEntity>

    @Query("DELETE FROM attachments WHERE messageId LIKE :accountId || ':%'")
    suspend fun deleteForAccount(accountId: String)
}
