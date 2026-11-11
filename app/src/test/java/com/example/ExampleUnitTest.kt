package com.example

import com.example.kiraengine.ExcelCell
import com.example.kiraengine.ExcelFormulaEvaluator
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testExcelFormulaEvaluation() {
        val cells = mapOf(
            "0,0" to ExcelCell(0, 0, rawValue = "100"),
            "1,0" to ExcelCell(1, 0, rawValue = "250"),
            "2,0" to ExcelCell(2, 0, rawValue = "50")
        )
        val sumResult = ExcelFormulaEvaluator.evaluate("=SUM(A1:A3)", cells)
        assertEquals("400", sumResult)

        val avgResult = ExcelFormulaEvaluator.evaluate("=AVERAGE(A1:A3)", cells)
        assertEquals("133.33", avgResult)

        val minResult = ExcelFormulaEvaluator.evaluate("=MIN(A1:A3)", cells)
        assertEquals("50", minResult)

        val maxResult = ExcelFormulaEvaluator.evaluate("=MAX(A1:A3)", cells)
        assertEquals("250", maxResult)

        val countResult = ExcelFormulaEvaluator.evaluate("=COUNT(A1:A3)", cells)
        assertEquals("3", countResult)
    }

    @Test
    fun testCoordParsing() {
        val (col, row) = ExcelFormulaEvaluator.parseCoord("B5")
        assertEquals(1, col)
        assertEquals(4, row)

        val colName = ExcelFormulaEvaluator.formatColName(1)
        assertEquals("B", colName)
    }
}
