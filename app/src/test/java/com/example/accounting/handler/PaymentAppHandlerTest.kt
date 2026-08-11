package com.example.accounting.handler

import com.example.accounting.model.InspectionResult
import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import com.example.accounting.model.ViewNodeInfo
import com.example.accounting.util.GsonProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

data class TestCase(
    val path: String,
    val name: String
) {
    override fun toString(): String = "$path:$name"
}

@RunWith(Parameterized::class)
class PaymentAppHandlerTest(
    private val testCase: TestCase
) {
    private val gson = GsonProvider.gson

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun findTestCases(): Collection<TestCase> {
            val location = "private/inspections"

            val testDir: File = location.let { loc ->
                val currentDir = File(System.getProperty("user.dir", ".")!!).canonicalFile
                var projRoot = currentDir
                while (!projRoot.resolve("settings.gradle.kts").exists()) {
                    projRoot = projRoot.parentFile!!
                }
                projRoot.resolve(loc)
            }

            val cases = mutableListOf<TestCase>()
            testDir.walkTopDown().forEach { file ->
                if (file.isFile && file.name.endsWith(".expected")) {
                    val name = file.nameWithoutExtension
                    cases.add(TestCase(file.parent!!, name))
                }
            }
            return cases
        }
    }

    @Test
    fun testOnDumpedInspections() {
        val dumpedFile = File(testCase.path, "${testCase.name}.json")
        val expectedFile = File(testCase.path, "${testCase.name}.expected")

        // Prepare testcase
        var dumpedInspection =
            gson.fromJson(dumpedFile.readText(), InspectionResult::class.java)
        assertNotNull(dumpedInspection.packageName)
        assertNotNull(dumpedInspection.rootNode)
        if (dumpedInspection.rootNode.size == 0)
            dumpedInspection = dumpedInspection.copy(
                rootNode = dumpedViewNode_v0_to_v1(dumpedInspection.rootNode)
            )

        val expectedTransaction =
            gson.fromJson(expectedFile.readText(), TransactionInfo::class.java)
        assertNotNull(expectedTransaction.amount)

        // Test PaymentAppHandler
        val handler = PaymentAppHandlerFactory.getHandler(dumpedInspection.packageName)
        assertNotNull("No handler found for package: ${dumpedInspection.packageName}", handler)
        handler!!

        val viewType = handler.detectPaymentView(dumpedInspection.rootNode)
        assertTrue("View type should not be None", viewType != PaymentViewType.None)
        assertEquals(
            "ViewType mismatch",
            expectedTransaction.viewType, viewType
        )

        val transaction = handler.extractTransaction(dumpedInspection.rootNode, viewType)
        assertNotNull("extractTransaction returned null", transaction)
        transaction!!

        assertTrue(
            "Amount mismatch. Expected: ${expectedTransaction.amount}, Actual: ${transaction.amount}",
            transaction.amount.compareTo(expectedTransaction.amount) == 0,
        )
    }

    /**
     * Fill height and size properties, which v0 didn't have.
     */
    fun dumpedViewNode_v0_to_v1(node: ViewNodeInfo): ViewNodeInfo {
        val children = mutableListOf<ViewNodeInfo>()
        for (child in node.children) {
            children.add(dumpedViewNode_v0_to_v1(child))
        }

        val height = if (children.isEmpty()) {
            0
        } else {
            children.maxOf { it.height } + 1
        }

        val size = if (children.isEmpty()) {
            1
        } else {
            children.sumOf { it.size } + 1
        }

        return node.copy(
            height = height,
            size = size,
            children = children
        )
    }
}