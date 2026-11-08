package com.example.email_client.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun teardown() = db.close()

    private fun account(id: String = "a1") = AccountEntity(
        id = id, email = "u@test.com", providerId = "custom", displayName = "Test",
        imapHost = "h", imapPort = 993, imapSsl = true,
        smtpHost = "s", smtpPort = 465, smtpSsl = true, authType = "PASSWORD",
    )

    private fun msg(id: String, uid: Long, date: Long, read: Boolean = false) = MessageEntity(
        id = id, accountId = "a1", folder = "INBOX", uid = uid, threadId = "t$uid",
        subject = "s$uid", sender = "x@y.z", senderName = "X", recipients = "u@test.com",
        date = date, snippet = "snip", isRead = read, isStarred = false,
        hasAttachments = false, bodyPath = null,
    )

    @Test fun upsertAndObserveAccount() = runTest {
        db.accountDao().upsert(account())
        assertEquals(1, db.accountDao().observeAll().first().size)
    }

    @Test fun inboxOrdersByDateDesc() = runTest {
        db.accountDao().upsert(account())
        db.messageDao().upsertAll(listOf(msg("m1", 1, 100), msg("m2", 2, 300), msg("m3", 3, 200)))
        val src = db.messageDao().pagingInbox("a1")
        val page = src.load(androidx.paging.PagingSource.LoadParams.Refresh(null, 50, false))
        val dates = (page as androidx.paging.PagingSource.LoadResult.Page).data.map { it.date }
        assertEquals(listOf(300L, 200L, 100L), dates)
    }

    @Test fun markReadFlipsFlag() = runTest {
        db.accountDao().upsert(account())
        db.messageDao().upsertAll(listOf(msg("m1", 1, 100)))
        db.messageDao().setRead("m1", true)
        assertTrue(db.messageDao().get("m1")!!.isRead)
    }

    @Test fun maxUidTracksHighest() = runTest {
        db.accountDao().upsert(account())
        db.messageDao().upsertAll(listOf(msg("m1", 4, 1), msg("m2", 9, 2)))
        assertEquals(9L, db.messageDao().maxUid("a1"))
    }
}
