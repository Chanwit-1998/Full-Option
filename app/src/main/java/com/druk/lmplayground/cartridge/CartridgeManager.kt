package com.druk.ai_offline_llm.cartridge

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject
import java.io.File

object CartridgeManager {
    private const val PREFS_NAME = "ai_cartridge_prefs"
    private const val KEY_ACTIVE_ID = "active_cartridge_id"
    private const val KEY_FOLDER_URI = "custom_cartridge_folder_uri"
    private const val DIR_NAME = "cartridges"
    private const val FILE_INSTALLED = "installed_cartridges_rag_v1.json"

    val storeCatalog: List<AiCartridge> = listOf(
        AiCartridge(
            id = "rag_termux_linux",
            name = "ตลับฐานข้อมูล Termux & Linux Commands",
            version = "v2.0 RAG",
            category = "เขียนโค้ด & ระบบ",
            colorHex = 0xFF00BFA5,
            description = "คลังคำสั่งลัด Termux, Git, Python, SSH และวิธีแก้ Error พื้นฐานบน Android แบบออฟไลน์",
            knowledge = listOf(
                CartridgeKnowledgeEntry("อัปเดตแพ็กเกจและสิทธิ์พื้นที่เก็บข้อมูล Termux", "ใช้คำสั่ง pkg update && pkg upgrade -y เพื่ออัปเดตคลังโปรแกรม และใช้ termux-setup-storage เพื่อขอสิทธิ์เข้าถึงโฟลเดอร์ /sdcard"),
                CartridgeKnowledgeEntry("คำสั่ง Git พื้นฐานสำหรับอัปโหลดโค้ดขึ้น GitHub", "ตรวจสอบสถานะด้วย git status เพิ่มไฟล์ทั้งหมดด้วย git add . บันทึกด้วย git commit -m \"ข้อความ\" และส่งขึ้นเซิร์ฟเวอร์ด้วย git push origin main"),
                CartridgeKnowledgeEntry("ค้นหาไฟล์ขนาดใหญ่และเช็กพื้นที่ว่าง", "เช็กพื้นที่ดิสก์ด้วย df -h เช็กขนาดโฟลเดอร์ด้วย du -sh * และค้นหาไฟล์ .gguf ทั้งหมดในเครื่องด้วย find /sdcard -name \"*.gguf\" 2>/dev/null"),
                CartridgeKnowledgeEntry("รัน Python HTTP Server แชร์ไฟล์ในวง Wi-Fi", "ใช้คำสั่ง python3 -m http.server 8080 เพื่อเปิดเว็บแชร์ไฟล์ในโฟลเดอร์ปัจจุบันผ่านพอร์ต 8080")
            ),
            bestFor = "• ค้นหาคำสั่ง Termux / Linux / Git โดยไม่ต้องต่อเน็ตเปิดเว็บหา\n• ใช้ร่วมกับบุคลิก 'นักเขียนโค้ด' เพื่อให้ AI ตอบคำสั่งที่ตรงไวยากรณ์เป๊ะ",
            usageTips = "• ถามเจาะจงชื่อคำสั่งหรือสิ่งที่ต้องการทำ เช่น 'คำสั่ง termux แชร์ไฟล์ผ่าน wifi'",
            samplePrompt = "ขอคำสั่ง Termux สำหรับค้นหาไฟล์ .gguf ในเครื่องและเช็กขนาดพื้นที่ว่าง"
        ),
        AiCartridge(
            id = "rag_emulator_bible",
            name = "ตลับคัมภีร์ตั้งค่า Emulator (PSP / PS2 / GC)",
            version = "v2.0 RAG",
            category = "เกม & Emulator",
            colorHex = 0xFFFF1744,
            description = "ฐานข้อมูลค่าตั้งต้น PPSSPP, AetherSX2/NetherSX2 และ Dolphin สำหรับชิป Exynos/Snapdragon บนมือถือ",
            knowledge = listOf(
                CartridgeKnowledgeEntry("การตั้งค่า PPSSPP (PSP) ให้ภาพชัดและลื่น 60FPS", "ตั้งค่า Backend = Vulkan, Rendering Resolution = 2x–3x PSP, เปิด Lazy Texture Caching (speedup), ตั้ง Spline/Bezier curves quality = Low"),
                CartridgeKnowledgeEntry("การตั้งค่า AetherSX2 / NetherSX2 (PS2) ลดอาการหน่วง", "ใช้ GPU Renderer = Vulkan, Upscale Multiplier = 1.5x–2x Native, ตั้ง EE Cycle Rate = 75%, EE Cycle Skip = Normal (0) และเปิด Multi-Threaded VU1"),
                CartridgeKnowledgeEntry("การตั้งค่า Dolphin Emulator (GameCube / Wii)", "เลือก Video Backend = Vulkan, เปิด Compile Shaders Before Starting และ Skip EFB Access from CPU เพื่อเพิ่มเฟรมเรต"),
                CartridgeKnowledgeEntry("ตำแหน่งโฟลเดอร์เซฟเกมและไฟล์ BIOS", "PPSSPP เก็บเซฟไว้ที่ PSP/SAVEDATA และสูตรโกงไว้ที่ PSP/Cheats (เปิด Enable Cheats ก่อน) ส่วน AetherSX2 ต้องนำเข้าไฟล์ BIOS PS2 ก่อนบูตเกม")
            ),
            bestFor = "• ปรับจูนภาพและเฟรมเรตเกม PSP, PS2 และ GameCube บนมือถือ Android\n• แก้ปัญหาภาพแตก เสียงกระตุก และหาตำแหน่งวางไฟล์เซฟ",
            usageTips = "• ระบุชื่อโปรแกรม Emulator (PPSSPP, AetherSX2, Dolphin) ในคำถาม ระบบจะดึงตารางตั้งค่ามาตอบทันที",
            samplePrompt = "ตั้งค่า PPSSPP ยังไงให้เล่นเกมลื่นไม่กระตุกและภาพคมชัด"
        ),
        AiCartridge(
            id = "rag_electric_std",
            name = "ตลับคู่มือมาตรฐานไฟฟ้ากำลัง (วสท.)",
            version = "v2.0 RAG",
            category = "งานช่าง & วิศวกรรม",
            colorHex = 0xFFFFB300,
            description = "ฐานข้อมูลพิกัดกระแสสายไฟ THW ขนาดเบรกเกอร์ กันดูด RCBO และสูตรคำนวณโหลดไฟฟ้าในบ้าน",
            knowledge = listOf(
                CartridgeKnowledgeEntry("สูตรคำนวณกระแสไฟฟ้า 1 เฟส 220V", "กระแสไฟฟ้า I (แอมป์) = กำลังไฟฟ้า P (วัตต์) หารด้วย [แรงดัน V (220V) x PF (0.85–1.0)] เช่น เครื่องทำน้ำอุ่น 4,500W กินกระแสประมาณ 4500 / 220 = 20.45 แอมป์"),
                CartridgeKnowledgeEntry("ตารางขนาดสายไฟทองแดง THW เดินในท่อร้อยสาย", "สาย 1.5 ตร.มม. รับได้ 15A (เบรกเกอร์ 10A–16A), สาย 2.5 ตร.มม. รับได้ 21A (เบรกเกอร์ 16A–20A), สาย 4.0 ตร.มม. รับได้ 28A (เบรกเกอร์ 25A–32A), สาย 6.0 ตร.มม. รับได้ 36A (เบรกเกอร์ 32A–40A)"),
                CartridgeKnowledgeEntry("มาตรฐานเบรกเกอร์เครื่องทำน้ำอุ่น", "เครื่องทำน้ำอุ่น 3,500W ใช้สายไฟ 2.5 ตร.มม. คู่กับเบรกเกอร์กันดูด (RCBO) 20A ส่วน 4,500W ต้องใช้สายไฟ 4.0 ตร.มม. คู่กับ RCBO 32A และต่อสายดินเสมอ"),
                CartridgeKnowledgeEntry("กฎความปลอดภัยก่อนซ่อมระบบไฟฟ้า", "ต้องตัดเมนเบรกเกอร์ลงตำแหน่ง OFF ใช้ไขควงลองไฟวัดยืนยันว่าไม่มีไฟค้าง และห้ามใช้เบรกเกอร์แอมป์สูงกว่าพิกัดสายไฟ")
            ),
            bestFor = "• คำนวณขนาดสายไฟและเลือกขนาดเบรกเกอร์ให้ปลอดภัยตามมาตรฐาน\n• ตรวจสอบโหลดเครื่องทำน้ำอุ่น แอร์ และวงจรปลั๊กไฟในบ้าน",
            usageTips = "• พิมพ์กำลังวัตต์ (W) หรือขนาดสายไฟที่ต้องการทราบ",
            samplePrompt = "เครื่องทำน้ำอุ่น 4,500 วัตต์ ต้องใช้สายไฟกี่ ตร.มม. และเบรกเกอร์กันดูดกี่แอมป์"
        ),
        AiCartridge(
            id = "rag_finance_formula",
            name = "ตลับสูตรคำนวณธุรกิจ การเงิน & ภาษี",
            version = "v2.0 RAG",
            category = "การเงิน & บัญชี",
            colorHex = 0xFF00C853,
            description = "คลังสูตรคิด Margin, Markup, จุดคุ้มทุน (Break-Even), VAT 7% และดอกเบี้ยทบต้นสำหรับค้าขาย",
            knowledge = listOf(
                CartridgeKnowledgeEntry("ความแตกต่างระหว่าง Profit Margin กับ Markup", "Markup (%) = [(ราคาขาย - ต้นทุน) / ต้นทุน] x 100 ส่วน Profit Margin (%) = [(ราคาขาย - ต้นทุน) / ราคาขาย] x 100 หากต้องการกำไร Margin สุทธิ M% จากต้นทุน C ให้ตั้งราคาขาย = C / (1 - M)"),
                CartridgeKnowledgeEntry("สูตรคำนวณจุดคุ้มทุน (Break-Even Point)", "จำนวนชิ้นที่ต้องขายให้คุ้มทุน = ต้นทุนคงที่ทั้งหมด หารด้วย [ราคาขายต่อหน่วย - ต้นทุนผันแปรต่อหน่วย]"),
                CartridgeKnowledgeEntry("วิธีถอดและบวกภาษีมูลค่าเพิ่ม VAT 7%", "บวก VAT 7%: คูณ 1.07 (เช่น 1,000 x 1.07 = 1,070) | ถอด VAT ออกจากราคารวม VAT: หาร 1.07 เพื่อหามูลค่าสินค้า และคูณ 7 หาร 107 เพื่อหายอดภาษี"),
                CartridgeKnowledgeEntry("สูตรดอกเบี้ยทบต้น (Compound Interest)", "เงินรวมในอนาคต A = P x (1 + r)^n โดย P คือเงินต้น, r คืออัตราดอกเบี้ยต่องวด และ n คือจำนวนงวดเวลา")
            ),
            bestFor = "• คำนวณราคาขายสินค้าให้ได้อัตรากำไรตามเป้าหมายจริง\n• ถอดยอด VAT 7% และคำนวณจุดคุ้มทุนของร้านค้า",
            usageTips = "• ใส่ตัวเลขต้นทุนและคำถามต่อท้ายได้เลย",
            samplePrompt = "ต้นทุนสินค้า 140 บาท อยากได้กำไร Margin 30% ต้องตั้งราคาขายกี่บาท และถ้ารวม VAT 7% จะเป็นกี่บาท"
        ),
        AiCartridge(
            id = "rag_jp_en_vocab",
            name = "ตลับคลังศัพท์ & สำนวนเกม/ชีวิตจริง (TH-EN-JP)",
            version = "v2.0 RAG",
            category = "ภาษา & การแปล",
            colorHex = 0xFFFF9100,
            description = "ฐานข้อมูลคำศัพท์เฉพาะทางในเกม RPG คำสุภาพภาษาญี่ปุ่น (Keigo) และสำนวนแชทภาษาอังกฤษ",
            knowledge = listOf(
                CartridgeKnowledgeEntry("คำศัพท์ค่าสถานะในเกม RPG (อังกฤษ - ญี่ปุ่น - ไทย)", "HP = พลังชีวิต, MP = พลังเวท, ATK (攻撃力 - โคเงคิเรียวคุ) = พลังโจมตี, DEF (防御力 - โบเกียวเรียวคุ) = พลังป้องกัน, CRIT (会心 - ไคชิน) = โจมตีติดคริติคอล"),
                CartridgeKnowledgeEntry("ประโยคภาษาญี่ปุ่นที่ใช้บ่อยในชีวิตประจำวัน", "ありがとうございます (Arigatou gozaimasu) = ขอบคุณมาก, すみません (Sumimasen) = ขอโทษ / รบกวนสอบถาม, よろしくお願いします (Yoroshiku onegaishimasu) = ขอฝากตัว / รบกวนด้วย"),
                CartridgeKnowledgeEntry("สำนวนแชทภาษาอังกฤษแบบมืออาชีพและเป็นกันเอง", "FYI = แจ้งเพื่อทราบ, ASAP = เร็วที่สุดเท่าที่เป็นไปได้, No worries = ไม่ต้องกังวล/ไม่เป็นไรเลย, Keep me posted = อัปเดตความคืบหน้าให้รู้ด้วยนะ")
            ),
            bestFor = "• แปลเมนูเกมภาษาญี่ปุ่น/อังกฤษและศัพท์สเตตัสตัวละคร\n• ค้นหาสำนวนตอบแชทภาษาอังกฤษและญี่ปุ่นให้ตรงบริบท",
            usageTips = "• พิมพ์คำศัพท์หรือประโยคที่เจอในเกม/แชท แล้วถามความหมายหรือวิธีใช้",
            samplePrompt = "คำว่า 攻撃力 และ 会心 ในเกมภาษาญี่ปุ่นแปลว่าอะไร และคำว่า Keep me posted ในแชทภาษาอังกฤษใช้ยังไง"
        ),
        AiCartridge(
            id = "rag_cooking_ratio",
            name = "ตลับตารางสัดส่วนเครื่องปรุง & แคลอรีอาหารไทย",
            version = "v2.0 RAG",
            category = "อาหาร & โภชนาการ",
            colorHex = 0xFFFF7043,
            description = "ฐานข้อมูลสูตรซอสผัดอเนกประสงค์ มาตราวัดตวงในครัว และตารางแคลอรี/โปรตีนของวัตถุดิบหลัก",
            knowledge = listOf(
                CartridgeKnowledgeEntry("สูตรซอสผัดกะเพรา / ผัดอเนกประสงค์ (ต่อเนื้อสัตว์ 200 กรัม)", "ซอสหอยนางรม 1.5 ช้อนโต๊ะ + ซีอิ๊วขาว 1 ช้อนโต๊ะ + น้ำปลา 0.5 ช้อนโต๊ะ + ซีอิ๊วดำหวาน 0.5 ช้อนชา + น้ำตาลทราย 0.5–1 ช้อนชา + น้ำเปล่า 2 ช้อนโต๊ะ"),
                CartridgeKnowledgeEntry("มาตราวัดตวงมาตรฐานในครัว", "1 ช้อนโต๊ะ (Tbsp) = 15 มล. (3 ช้อนชา), 1 ช้อนชา (Tsp) = 5 มล., 1 ถ้วยตวง (Cup) = 240 มล. (16 ช้อนโต๊ะ)"),
                CartridgeKnowledgeEntry("ตารางพลังงานและโปรตีนของวัตถุดิบหลัก (ต่อ 100 กรัม)", "อกไก่ลอกหนังดิบ: 110 kcal (โปรตีน 23g), สันในหมู: 143 kcal (โปรตีน 21g), ไข่ไก่ 1 ฟอง: 70–75 kcal (โปรตีน 6g), ข้าวสวย 1 ทัพพี (60g): 80 kcal")
            ),
            bestFor = "• คำนวณแคลอรีและโปรตีนต่อมื้ออาหาร\n• ดูสูตรสัดส่วนเครื่องปรุงอาหารไทยและเทียบมาตราวัดช้อนตวง",
            usageTips = "• บอกชื่อวัตถุดิบและปริมาณที่มี",
            samplePrompt = "อกไก่ 200 กรัม กับไข่ไก่ 2 ฟอง ให้โปรตีนและพลังงานกี่แคลอรี และขอสูตรซอสผัดกะเพราสำหรับปริมาณนี้"
        ),
        AiCartridge(
            id = "rag_first_aid",
            name = "ตลับคู่มือปฐมพยาบาล & เบอร์ฉุกเฉิน",
            version = "v2.0 RAG",
            category = "สุขภาพ & ความปลอดภัย",
            colorHex = 0xFFEC407A,
            description = "ฐานข้อมูลขั้นตอนปฐมพยาบาลเบื้องต้น แผลไฟไหม้ น้ำร้อนลวก ไฟดูด และหมายเลขสายด่วนฉุกเฉินในไทย",
            knowledge = listOf(
                CartridgeKnowledgeEntry("หมายเลขโทรศัพท์แจ้งเหตุฉุกเฉินในประเทศไทย", "1669 = เจ็บป่วยฉุกเฉิน / กู้ชีพทั่วประเทศ, 191 = ตำรวจ / เหตุด่วนเหตุร้าย, 199 = แจ้งเหตุเพลิงไหม้, 1129 = การไฟฟ้าส่วนภูมิภาค (PEA), 1130 = การไฟฟ้านครหลวง (MEA)"),
                CartridgeKnowledgeEntry("การปฐมพยาบาลแผลไฟไหม้และน้ำร้อนลวก", "เปิดน้ำสะอาดอุณหภูมิห้องไหลผ่านแผลเบาๆ นาน 10–20 นาที ห้ามใช้น้ำแข็งประคบโดยตรง ห้ามทายาสีฟัน น้ำปลา หรือน้ำมัน ห้ามเจาะตุ่มน้ำพอง"),
                CartridgeKnowledgeEntry("การช่วยเหลือผู้ถูกไฟฟ้าดูดอย่างปลอดภัย", "ห้ามสัมผัสตัวผู้ถูกไฟดูดด้วยมือเปล่าเด็ดขาด ให้รีบตัดแหล่งจ่ายไฟ หากตัดไฟไม่ได้ให้ใช้ไม้แห้งหรือฉนวนแห้งเขี่ยสายไฟออก แล้วโทร 1669 ทันที")
            ),
            bestFor = "• ค้นหาขั้นตอนปฐมพยาบาลที่ถูกต้องในสถานการณ์ฉุกเฉินแบบออฟไลน์\n• เช็กเบอร์สายด่วนฉุกเฉินในประเทศไทย",
            usageTips = "• ถามอาการหรือเหตุการณ์ตรงๆ เช่น 'วิธีปฐมพยาบาลแผลน้ำร้อนลวก'",
            samplePrompt = "โดนน้ำร้อนลวกที่แขนควรปฐมพยาบาลเบื้องต้นยังไง และมีข้อห้ามอะไรบ้าง"
        ),
        AiCartridge(
            id = "rag_android_kotlin",
            name = "ตลับคู่มือพัฒนาแอป Android & Jetpack Compose",
            version = "v2.0 RAG",
            category = "เขียนโค้ด & ระบบ",
            colorHex = 0xFF2979FF,
            description = "คลังรูปแบบโค้ด Jetpack Compose, Coroutines, Room Database และ Android Service สำหรับนักพัฒนา",
            knowledge = listOf(
                CartridgeKnowledgeEntry("การจัดการ State และ Coroutines ใน Jetpack Compose", "ใช้ remember { mutableStateOf(...) } สำหรับ State ภายใน Composable ใช้ collectAsState() สำหรับ StateFlow และใช้ LaunchedEffect(key) เมื่อต้องการรันงานแบบ Asynchronous"),
                CartridgeKnowledgeEntry("ข้อควรระวังเรื่อง Foreign Key ใน Room Database", "หากตารางลูกตั้งค่า @ForeignKey อ้างอิงไปยังตารางแม่ ห้ามส่งค่า ID ที่ไม่มีอยู่จริงในตารางแม่ มิฉะนั้น SQLite จะโยน SQLiteConstraintException ให้ส่ง null แทนเมื่อไม่ต้องการผูก"),
                CartridgeKnowledgeEntry("การเปิด Activity จาก Service หรือ VoiceInteractionSession", "เมื่อเรียก startActivity(intent) จากภายนอก Activity ต้องเพิ่ม Flag Intent.FLAG_ACTIVITY_NEW_TASK เสมอ และเรียกผ่าน context.startActivity(intent)")
            ),
            bestFor = "• อ้างอิงรูปแบบโค้ด Jetpack Compose, Room และ Android Framework\n• แก้ปัญหา Crash ยอดฮิตบน Android อย่างรวดเร็ว",
            usageTips = "• ถามเรื่องโครงสร้าง Compose, Room หรือ Service ได้ทันที",
            samplePrompt = "วิธีแก้ปัญหา SQLiteConstraintException Foreign Key failed ใน Room Database และวิธีเรียก startActivity จาก Service"
        ),
        AiCartridge(
            id = "rag_summary_framework",
            name = "ตลับแม่แบบสรุปงาน & เอกสารมืออาชีพ",
            version = "v2.0 RAG",
            category = "เครื่องมือทำงาน",
            colorHex = 0xFF26A69A,
            description = "คลังโครงสร้างการสรุปรายงานการประชุม บทความ และการเขียนอีเมลธุรกิจให้กระชับอ่านจบใน 10 วินาที",
            knowledge = listOf(
                CartridgeKnowledgeEntry("โครงสร้างสรุปรายงานการประชุมแบบ 3 ส่วน (Actionable Minutes)", "1. ข้อสรุปและมติสำคัญ (Key Decisions) ไม่เกิน 3 ข้อ | 2. รายการสิ่งที่ต้องทำต่อ (Action Items) ระบุผู้รับผิดชอบและกำหนดส่ง | 3. ประเด็นที่ยังค้างหรือต้องติดตามรอบหน้า"),
                CartridgeKnowledgeEntry("เทคนิคสรุปบทความยาวด้วยหลัก BLUF (Bottom Line Up Front)", "นำข้อสรุปหรือผลลัพธ์สำคัญที่สุดขึ้นบรรทัดแรกสุด ตามด้วยเหตุผลสนับสนุน 3 ข้อ และตัวเลขสถิติสำคัญ โดยตัดคำเกริ่นนำออกทั้งหมด")
            ),
            bestFor = "• ใช้เป็นแม่แบบจัดระเบียบข้อความยาวๆ หรือรายงานการประชุม\n• ทำงานร่วมกับไฟล์เอกสาร RAG ที่แนบในห้องแชท",
            usageTips = "• วางข้อความยาวๆ แล้วสั่งให้สรุปตามโครงสร้าง BLUF",
            samplePrompt = "สรุปข้อความต่อไปนี้ตามหลัก BLUF โดยเอาข้อสรุปสำคัญขึ้นบรรทัดแรกและแยกเหตุผลเป็น 3 ข้อ:"
        ),
        AiCartridge(
            id = "rag_ai_llm_guide",
            name = "ตลับคู่มือระบบ AI Local LLM & การจูนโมเดล",
            version = "v2.0 RAG",
            category = "AI & เทคโนโลยี",
            colorHex = 0xFF7C4DFF,
            description = "ฐานข้อมูลอธิบายค่า Temperature, Top-P, Context Size, Quantization (Q4_K_M / Q8_0) และ KV Cache",
            knowledge = listOf(
                CartridgeKnowledgeEntry("ความหมายของค่า Temperature และ Top-P ใน LLM", "Temperature (0.0–1.5) ควบคุมความสร้างสรรค์: ค่าต่ำ (0.1–0.3) เหมาะกับโค้ดและคำนวณ ค่าสูง (0.7–0.9) เหมาะกับแต่งนิยาย ส่วน Top-P ตัดตัวเลือกคำความน่าจะเป็นต่ำทิ้ง โดยทั่วไปตั้งไว้ที่ 0.85–0.95"),
                CartridgeKnowledgeEntry("รูปแบบไฟล์บีบอัด GGUF Quantization (Q4_K_M vs Q8_0)", "Q4_K_M (4-bit) ใช้แรมน้อยและรันเร็วที่สุดบนมือถือ รักษาความฉลาดได้ >95% ส่วน Q8_0 (8-bit) แม่นยำใกล้เคียงต้นฉบับ 99.9% แต่ใช้แรมมากกว่าเกือบ 2 เท่า"),
                CartridgeKnowledgeEntry("หลักการทำงานของ Context Size และ KV Cache", "Context Size คือความยาวความจำสูงสุดของบทสนทนา ยิ่งตั้งค่าสูงยิ่งกิน RAM มากขึ้นตามขนาด KV Cache มือถือแรม 6GB–8GB แนะนำตั้งไว้ที่ 2048–4096")
            ),
            bestFor = "• ทำความเข้าใจการตั้งค่าโมเดลในแอป ChatLLM (Temp, Top-P, Context Size, GGUF)\n• เลือกดาวน์โหลดไฟล์โมเดล GGUF ให้เหมาะกับสเปกแรมมือถือ",
            usageTips = "• ถามเรื่องการปรับแต่งค่าพารามิเตอร์หรือเลือกขนาดไฟล์โมเดลได้ทันที",
            samplePrompt = "ไฟล์ GGUF แบบ Q4_K_M ต่างจาก Q8_0 ยังไง และมือถือแรม 8GB ควรตั้ง Context Size เท่าไหร่"
        )
    )

    private fun getInstalledFile(context: Context): File =
        File(File(context.filesDir, DIR_NAME).apply { mkdirs() }, FILE_INSTALLED)

    fun getCartridgeFolderPath(context: Context): String {
        val rawUri = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_FOLDER_URI, null)
        if (rawUri.isNullOrBlank()) return "โฟลเดอร์แอป/cartridges"
        val decoded = Uri.decode(rawUri)
        val segment = decoded.substringAfterLast("primary:", decoded.substringAfterLast("/"))
        return if (segment.isNotBlank()) "Internal Storage/$segment" else decoded
    }

    fun setCustomCartridgeFolderUri(context: Context, uri: Uri): Int {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return 0
        var importedCount = 0
        tree.listFiles().forEach { doc ->
            if (doc.isFile && (doc.name?.endsWith(".json", ignoreCase = true) == true)) {
                if (importFromUri(context, doc.uri).isSuccess) importedCount++
            }
        }
        return importedCount
    }

    fun getInstalledCartridges(context: Context): List<AiCartridge> {
        val file = getInstalledFile(context)
        if (!file.exists()) {
            val defaults = storeCatalog.take(2)
            saveInstalledCartridges(context, defaults)
            return defaults
        }
        return AiCartridge.listFromJsonString(file.readText(Charsets.UTF_8))
    }

    fun saveInstalledCartridges(context: Context, list: List<AiCartridge>) {
        getInstalledFile(context).writeText(AiCartridge.listToJsonString(list), Charsets.UTF_8)
    }

    fun installCartridge(context: Context, cartridge: AiCartridge): List<AiCartridge> {
        val current = getInstalledCartridges(context).toMutableList()
        val idx = current.indexOfFirst { it.id == cartridge.id }
        if (idx >= 0) current[idx] = cartridge else current.add(0, cartridge)
        saveInstalledCartridges(context, current)
        return current
    }

    fun removeInstalledCartridge(context: Context, cartridgeId: String): List<AiCartridge> {
        val updated = getInstalledCartridges(context).filterNot { it.id == cartridgeId }
        saveInstalledCartridges(context, updated)
        if (getActiveCartridgeId(context) == cartridgeId) setActiveCartridgeId(context, null)
        return updated
    }

    fun getAllAvailableCartridges(context: Context): List<AiCartridge> {
        val map = LinkedHashMap<String, AiCartridge>()
        getInstalledCartridges(context).forEach { map[it.id] = it }
        storeCatalog.forEach { if (!map.containsKey(it.id)) map[it.id] = it }
        return map.values.toList()
    }

    fun getActiveCartridgeId(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ACTIVE_ID, null)

    fun setActiveCartridgeId(context: Context, cartridgeId: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ACTIVE_ID, cartridgeId).apply()
    }

    fun getActiveCartridge(context: Context): AiCartridge? =
        getActiveCartridgeId(context)?.let { id -> getAllAvailableCartridges(context).find { it.id == id } }

    private val greetingOnlySet = setOf(
        "สวัสดี", "สวัสดีครับ", "สวัสดีค่ะ", "สวัสดีจ้า", "หวัดดี", "ดีครับ", "ดีค่ะ",
        "hi", "hello", "hey", "yo", "ขอบคุณ", "ขอบคุณครับ", "ขอบคุณค่ะ", "ok", "โอเค", "เค"
    )

    fun retrieveKnowledge(context: Context, query: String, maxChars: Int = 3000): List<Pair<String, CartridgeKnowledgeEntry>> {
        val cartridge = getActiveCartridge(context) ?: return emptyList()
        if (cartridge.knowledge.isEmpty() || maxChars <= 0) return emptyList()

        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty() || trimmed in greetingOnlySet) return emptyList()

        val qTokens = extractSearchTokens(query)
        if (qTokens.isEmpty()) return emptyList()

        val minScore = 6
        val scored = cartridge.knowledge.mapIndexed { index, entry ->
            val topicTokens = extractSearchTokens(entry.topic)
            val bodyTokens = extractSearchTokens(entry.content)
            var score = 0
            for (qt in qTokens) {
                if (topicTokens.contains(qt)) score += 4
                if (bodyTokens.contains(qt)) score += 2
            }
            Triple(score, -index, entry)
        }.filter { it.first >= minScore }
         .sortedWith(compareByDescending<Triple<Int, Int, CartridgeKnowledgeEntry>> { it.first }.thenByDescending { it.second })

        if (scored.isEmpty()) return emptyList()

        val results = mutableListOf<Pair<String, CartridgeKnowledgeEntry>>()
        var usedChars = 0
        for ((_, _, entry) in scored) {
            val blockLen = cartridge.name.length + entry.topic.length + entry.content.length + 40
            if (usedChars + blockLen > maxChars && results.isNotEmpty()) break
            results.add(cartridge.name to entry)
            usedChars += blockLen
            if (results.size >= 3) break
        }
        return results
    }

    private fun extractSearchTokens(text: String): Set<String> {
        val clean = text.lowercase().replace(Regex("[^a-z0-9ก-๙\\s]"), " ")
        val tokens = HashSet<String>()
        for (word in clean.split(Regex("\\s+"))) {
            if (word.length >= 2) {
                tokens.add(word)
                if (word.length > 4) {
                    for (i in 0..(word.length - 3)) tokens.add(word.substring(i, i + 3))
                }
            }
        }
        return tokens
    }

    fun importFromUri(context: Context, uri: Uri): Result<AiCartridge> = runCatching {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("ไม่สามารถอ่านไฟล์ตลับได้")
        val json = JSONObject(raw)
        val imported = AiCartridge.fromJson(json).copy(
            id = json.optString("id").ifBlank { "custom_${System.currentTimeMillis()}" },
            isCustom = true
        )
        installCartridge(context, imported)
        imported
    }
}
