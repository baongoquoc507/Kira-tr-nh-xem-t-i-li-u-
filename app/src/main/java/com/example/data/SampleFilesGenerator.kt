package com.example.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.kiraengine.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleFilesGenerator {
    suspend fun generateSamples(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        val samplesDir = File(context.filesDir, "sample_docs").apply { mkdirs() }

        // 1. Excel Sample
        val excelFile = File(samplesDir, "Bao_cao_tai_chinh_Q4.xlsx")
        if (!excelFile.exists()) {
            createSampleExcel(excelFile)
        }
        val excelUri = getUri(context, excelFile)
        database.documentDao().insertOrUpdate(
            RecentDocumentEntity(
                uri = excelUri.toString(),
                name = "Báo cáo tài chính Q4.xlsx",
                type = DocumentType.EXCEL.name,
                sizeBytes = excelFile.length(),
                isFavorite = true
            )
        )

        // 2. Word Sample
        val docxFile = File(samplesDir, "Hop_dong_dich_vu_Kira.docx")
        if (!docxFile.exists()) {
            createSampleDocx(docxFile)
        }
        val docxUri = getUri(context, docxFile)
        database.documentDao().insertOrUpdate(
            RecentDocumentEntity(
                uri = docxUri.toString(),
                name = "Hợp đồng dịch vụ Kira.docx",
                type = DocumentType.WORD.name,
                sizeBytes = docxFile.length(),
                isFavorite = true
            )
        )

        // 3. PPT Sample
        val pptFile = File(samplesDir, "Ke_hoach_ra_mat_Kira_2026.pptx")
        if (!pptFile.exists()) {
            createSamplePpt(pptFile)
        }
        val pptUri = getUri(context, pptFile)
        database.documentDao().insertOrUpdate(
            RecentDocumentEntity(
                uri = pptUri.toString(),
                name = "Kế hoạch ra mắt Kira 2026.pptx",
                type = DocumentType.POWERPOINT.name,
                sizeBytes = pptFile.length(),
                isFavorite = false
            )
        )

        // 4. PDF Sample
        val pdfFile = File(samplesDir, "Huong_dan_su_dung_Kira.pdf")
        if (!pdfFile.exists()) {
            createSamplePdf(pdfFile)
        }
        val pdfUri = getUri(context, pdfFile)
        database.documentDao().insertOrUpdate(
            RecentDocumentEntity(
                uri = pdfUri.toString(),
                name = "Hướng dẫn sử dụng Kira.pdf",
                type = DocumentType.PDF.name,
                sizeBytes = pdfFile.length(),
                isFavorite = true
            )
        )

        // 5. CSV Sample
        val csvFile = File(samplesDir, "Danh_sach_khach_hang.csv")
        if (!csvFile.exists()) {
            createSampleCsv(csvFile)
        }
        val csvUri = getUri(context, csvFile)
        database.documentDao().insertOrUpdate(
            RecentDocumentEntity(
                uri = csvUri.toString(),
                name = "Danh sách khách hàng VIP.csv",
                type = DocumentType.EXCEL.name,
                sizeBytes = csvFile.length(),
                isFavorite = false
            )
        )
    }

    private fun getUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun createSamplePdf(file: File) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1080, 1920, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        // Header accent
        val accentPaint = Paint().apply {
            color = Color.parseColor("#007AFF")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 1080f, 220f, accentPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 56f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Trình xem tài liệu Kira", 80f, 110f, headerTextPaint)

        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F2FE")
            textSize = 32f
        }
        canvas.drawText("Tài liệu hướng dẫn & Giới thiệu tính năng", 80f, 165f, subHeaderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 46f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("1. Định dạng hỗ trợ", 80f, 320f, titlePaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 34f
            typeface = Typeface.DEFAULT
        }
        val tips = listOf(
            "• Word (DOCX, DOC): Đọc, sửa văn bản, đổi font, căn lề, chèn bảng",
            "• Excel (XLSX, XLS, CSV): Lưới tính toán, hỗ trợ hàm SUM, AVERAGE, MIN, MAX",
            "• PowerPoint (PPTX, PPT): Xem slide, trình chiếu toàn màn hình chuyên nghiệp",
            "• PDF: Tốc độ cao, vẽ tay (Ink), tô sáng (Highlight), ghi chú ghim",
            "• Văn bản thuần (TXT): Mở nhanh tức thì, tìm kiếm và thay thế",
            "",
            "2. Hoàn toàn Offline & Bảo mật",
            "• Ứng dụng không yêu cầu quyền INTERNET",
            "• Không SDK quảng cáo, không theo dõi, không gửi dữ liệu ra bên ngoài",
            "• Dữ liệu và tài liệu của bạn được bảo mật tuyệt đối trên thiết bị",
            "",
            "3. Trải nghiệm phong cách Liquid Glass",
            "• Tùy chỉnh hình nền: Màu trơn, Gradient, Trừu tượng hoặc ảnh từ máy",
            "• Điều chỉnh độ mờ kính mờ, độ sáng và chế độ giảm trong suốt",
            "• Chế độ đọc ban đêm và chế độ bảo vệ mắt (Sepia)"
        )

        var y = 390f
        for (tip in tips) {
            if (tip.startsWith("2.") || tip.startsWith("3.")) {
                canvas.drawText(tip, 80f, y + 20f, titlePaint)
                y += 75f
            } else {
                canvas.drawText(tip, 80f, y, bodyPaint)
                y += 58f
            }
        }

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private fun createSampleDocx(file: File) {
        val zos = ZipOutputStream(FileOutputStream(file))

        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("word/document.xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>
  <w:p><w:pPr><w:rPr><w:b/></w:rPr></w:pPr><w:r><w:t>HỢP ĐỒNG CUNG CẤP DỊCH VỤ CÔNG NGHỆ</w:t></w:r></w:p>
  <w:p><w:r><w:t>Số: 2026/HĐ-KIRA</w:t></w:r></w:p>
  <w:p><w:r><w:t>Hôm nay, ngày 10 tháng 11 năm 2026, chúng tôi gồm các bên:</w:t></w:r></w:p>
  <w:p><w:pPr><w:rPr><w:b/></w:rPr></w:pPr><w:r><w:t>Bên A: Công ty Cổ phần Công nghệ Kira Việt Nam</w:t></w:r></w:p>
  <w:p><w:r><w:t>Đại diện: Ông Nguyễn Văn A - Tổng Giám đốc</w:t></w:r></w:p>
  <w:p><w:pPr><w:rPr><w:b/></w:rPr></w:pPr><w:r><w:t>Bên B: Khách hàng Doanh nghiệp Toàn Cầu</w:t></w:r></w:p>
  <w:p><w:r><w:t>Đại diện: Bà Trần Thị B - Giám đốc Công nghệ</w:t></w:r></w:p>
  <w:p><w:pPr><w:rPr><w:b/></w:rPr></w:pPr><w:r><w:t>Điều 1: Phạm vi và nội dung công việc</w:t></w:r></w:p>
  <w:p><w:r><w:t>Bên A cam kết cung cấp giải pháp xử lý tài liệu văn phòng hoàn toàn ngoại tuyến (Offline Document Engine) bảo mật cao theo đúng tiêu chuẩn công nghệ 2026.</w:t></w:r></w:p>
  <w:tbl>
    <w:tr>
      <w:tc><w:p><w:r><w:t>Giai đoạn</w:t></w:r></w:p></w:tc>
      <w:tc><w:p><w:r><w:t>Hạng mục</w:t></w:r></w:p></w:tc>
      <w:tc><w:p><w:r><w:t>Thời gian</w:t></w:r></w:p></w:tc>
    </w:tr>
    <w:tr>
      <w:tc><w:p><w:r><w:t>GĐ 1</w:t></w:r></w:p></w:tc>
      <w:tc><w:p><w:r><w:t>Tích hợp Kira Engine</w:t></w:r></w:p></w:tc>
      <w:tc><w:p><w:r><w:t>30 ngày</w:t></w:r></w:p></w:tc>
    </w:tr>
    <w:tr>
      <w:tc><w:p><w:r><w:t>GĐ 2</w:t></w:r></w:p></w:tc>
      <w:tc><w:p><w:r><w:t>Kiểm thử hiệu năng cao</w:t></w:r></w:p></w:tc>
      <w:tc><w:p><w:r><w:t>15 ngày</w:t></w:r></w:p></w:tc>
    </w:tr>
  </w:tbl>
  <w:p><w:r><w:t>Điều 2: Điều khoản bảo mật</w:t></w:r></w:p>
  <w:p><w:r><w:t>Toàn bộ tài liệu không gửi qua mạng internet nhằm tuân thủ quy chuẩn an toàn thông tin tối đa.</w:t></w:r></w:p>
</w:body></w:document>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.close()
    }

    private fun createSampleExcel(file: File) {
        val zos = ZipOutputStream(FileOutputStream(file))

        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheets><sheet name="Tài chính Q4" sheetId="1" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/></sheets>
</workbook>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1"><v>Mục chi tiêu</v></c>
      <c r="B1"><v>Tháng 10</v></c>
      <c r="C1"><v>Tháng 11</v></c>
      <c r="D1"><v>Tháng 12</v></c>
      <c r="E1"><v>Tổng cộng</v></c>
    </row>
    <row r="2">
      <c r="A2"><v>Doanh thu phần mềm</v></c>
      <c r="B2"><v>450000000</v></c>
      <c r="C2"><v>520000000</v></c>
      <c r="D2"><v>610000000</v></c>
      <c r="E2"><f>=SUM(B2:D2)</f><v>1580000000</v></c>
    </row>
    <row r="3">
      <c r="A3"><v>Dịch vụ hỗ trợ</v></c>
      <c r="B3"><v>120000000</v></c>
      <c r="C3"><v>140000000</v></c>
      <c r="D3"><v>165000000</v></c>
      <c r="E3"><f>=SUM(B3:D3)</f><v>425000000</v></c>
    </row>
    <row r="4">
      <c r="A4"><v>Chi phí vận hành</v></c>
      <c r="B4"><v>180000000</v></c>
      <c r="C4"><v>195000000</v></c>
      <c r="D4"><v>210000000</v></c>
      <c r="E4"><f>=SUM(B4:D4)</f><v>585000000</v></c>
    </row>
    <row r="5">
      <c r="A5"><v>Lợi nhuận ròng</v></c>
      <c r="B5"><v>390000000</v></c>
      <c r="C5"><v>465000000</v></c>
      <c r="D5"><v>565000000</v></c>
      <c r="E5"><f>=SUM(B5:D5)</f><v>1420000000</v></c>
    </row>
  </sheetData>
</worksheet>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.close()
    }

    private fun createSamplePpt(file: File) {
        val zos = ZipOutputStream(FileOutputStream(file))

        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slides/slide1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
  <Override PartName="/ppt/slides/slide2.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
  <Override PartName="/ppt/slides/slide3.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
</Types>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("ppt/presentation.xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldIdLst>
    <p:sldId id="256" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
    <p:sldId id="257" r:id="rId2" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
    <p:sldId id="258" r:id="rId3" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
  </p:sldIdLst>
</p:presentation>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // Slide 1
        zos.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:sp><p:txBody><a:p><a:r><a:t>Chiến lược ra mắt Kira Document Viewer 2026</a:t></a:r></a:p></p:txBody></p:sp>
      <p:sp><p:txBody>
        <a:p><a:r><a:t>Trình xem tài liệu di động tốc độ vượt trội</a:t></a:r></a:p>
        <a:p><a:r><a:t>100% Offline và bảo vệ quyền riêng tư người dùng</a:t></a:r></a:p>
        <a:p><a:r><a:t>Giao diện phong cách iOS Liquid Glass thời thượng</a:t></a:r></a:p>
      </p:txBody></p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // Slide 2
        zos.putNextEntry(ZipEntry("ppt/slides/slide2.xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:sp><p:txBody><a:p><a:r><a:t>Lộ trình phát triển & Cột mốc chính</a:t></a:r></a:p></p:txBody></p:sp>
      <p:sp><p:txBody>
        <a:p><a:r><a:t>Tháng 11/2026: Phát hành phiên bản chính thức Kira Engine</a:t></a:r></a:p>
        <a:p><a:r><a:t>Tháng 12/2026: Nâng cấp tính năng chú thích PDF nâng cao</a:t></a:r></a:p>
        <a:p><a:r><a:t>Quý 1/2027: Tối ưu bộ nhớ cho tệp văn phòng trên 100MB</a:t></a:r></a:p>
      </p:txBody></p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // Slide 3
        zos.putNextEntry(ZipEntry("ppt/slides/slide3.xml"))
        zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:sp><p:txBody><a:p><a:r><a:t>Cam kết chất lượng & Trải nghiệm người dùng</a:t></a:r></a:p></p:txBody></p:sp>
      <p:sp><p:txBody>
        <a:p><a:r><a:t>Mở file ngay trong tích tắc (dưới 500ms)</a:t></a:r></a:p>
        <a:p><a:r><a:t>Không quảng cáo làm phiền, không thu thập dữ liệu cá nhân</a:t></a:r></a:p>
        <a:p><a:r><a:t>Tiết kiệm pin tối đa khi đọc tài liệu dài</a:t></a:r></a:p>
      </p:txBody></p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>""".toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.close()
    }

    private fun createSampleCsv(file: File) {
        val content = """STT,Họ và tên,Số điện thoại,Email,Hạng hội viên,Điểm tích lũy,Tổng chi tiêu (VNĐ)
1,Nguyễn Văn An,0901234567,an.nguyen@email.vn,Diamond,12500,250000000
2,Trần Thị Mai,0912345678,mai.tran@email.vn,Platinum,8400,168000000
3,Lê Hoàng Nam,0983456789,nam.le@email.vn,Gold,5200,104000000
4,Phạm Minh Tuấn,0974567890,tuan.pham@email.vn,Diamond,14200,284000000
5,Võ Thanh Hương,0935678901,huong.vo@email.vn,Gold,4800,96000000
6,Đỗ Quang Huy,0946789012,huy.do@email.vn,Silver,2100,42000000
7,Bùi Thùy Dung,0927890123,dung.bui@email.vn,Platinum,9100,182000000
"""
        file.writeText(content, Charsets.UTF_8)
    }
}
