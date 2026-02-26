package com.example.accounting.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.accounting.database.dao.TransactionDao
import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetTransaction() = runBlocking {
        val transaction = TransactionInfo(
            id = 0,
            amount = BigDecimal("99.99"),
            merchant = "Test Merchant",
            paymentMethod = "WeChat Pay",
            timestamp = System.currentTimeMillis(),
            viewType = PaymentViewType.Success
        )

        // Insert transaction
        val id = transactionDao.insertTransaction(transaction)

        // Retrieve transaction
        val retrievedTransaction = transactionDao.getTransactionById(id)

        // Verify
        assert(retrievedTransaction != null)
        assert(retrievedTransaction!!.amount == transaction.amount)
        assert(retrievedTransaction.merchant == transaction.merchant)
        assert(retrievedTransaction.paymentMethod == transaction.paymentMethod)
        assert(retrievedTransaction.viewType is PaymentViewType.Success)
    }
}