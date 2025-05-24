# KiboTeamSENSUAY
ภารกิจการแข่งขัน Kibo-RPC — ทำอะไรบ้าง?

1. เริ่มต้นภารกิจ (Start)
ควบคุมหุ่น Astrobee ให้ออกจาก Docking Station เพื่อเริ่มต้นบินในโมดูล Kibo
2. บินสำรวจ (Patrol)
บินสำรวจในพื้นที่ที่กำหนดไว้ ซึ่งแบ่งเป็น 4 พื้นที่หลัก (Area 1-4)
ตรวจจับและ อ่านภาพวัตถุ (Items) ที่วางอยู่ในแต่ละพื้นที่อย่างแม่นยำ
วัตถุที่สำรวจแบ่งเป็น
Landmark Items: วัตถุที่ใช้เป็นจุดอ้างอิง เช่น เหรียญ, ฟอสซิล, กุญแจ
Treasure Items: สมบัติจริง เช่น คริสตัล, เพชร, มรกต
บินผ่าน Oasis Zones เพื่อเก็บคะแนนโบนัส (ถ้าเลือกเส้นทางผ่านโซนนี้)
3. รายงานข้อมูล (Report)
หลังจากสำรวจครบทุกพื้นที่
บินไปหานักบินอวกาศ (Astronaut)
รายงานข้อมูลภาพและจำนวนของ Landmark Items ที่ตรวจจับได้ในแต่ละพื้นที่
4. รับข้อมูลสมบัติเป้าหมาย (Read Target Item)
นักบินอวกาศจะโชว์ภาพ Target Item ซึ่งเป็นสมบัติจริงที่ต้องหา
Astrobee ต้องอ่านและรับรู้ภาพนี้ เพื่อเปรียบเทียบกับข้อมูลที่เก็บมา
5. ค้นหาสมบัติจริง (Find Treasure)
บินไปยังพื้นที่ที่พบ Target Item ตามข้อมูลที่เก็บมา
ถ่ายภาพสมบัติจริง (Snapshot) ในมุมและระยะที่กำหนด (มุมกล้องไม่เกิน 30 องศา, ระยะไม่เกิน 0.9 เมตร) เพื่อยืนยันการพบสมบัติ
6. แจ้งภารกิจสำเร็จ (Mission Completion)
เปิดไฟสัญญาณ (Signal Lights) บน Astrobee เพื่อแจ้งให้นักบินอวกาศทราบตำแหน่งสมบัติ
การเปิดไฟสัญญาณถือเป็นการสิ้นสุดภารกิจ
7. เงื่อนไขเวลาทำภารกิจ
ภารกิจทั้งหมดต้องเสร็จภายในเวลาประมาณ 5 นาที
ยิ่งทำเสร็จเร็ว ยิ่งได้รับคะแนนโบนัสเพิ่ม

# Installing Android Studio
Please download Android Studio 3.6.3 from the Android Studio download archives page
( https://developer.android.com/studio/archive) 
and extract it to your home directory.




## Downloading additional components

To build the Guest Science APK, you need to download additional components as
follows.
1. Launch Android Studio.
2. Select [Tools] -> [SDK Manager].
On the SDK Platforms tab, check “Show Package Details” and select “Android SDK
Platform 25”
, “Android SDK Platform 26”, and “Android SDK Platform 28”
.

3. On the SDK Tools Tab, check “Show Package Details” and select “25.0.3”, “26.0.2”,
“28.0.3” under Android SDK Build-Tools and select “20.0.5594570” under NDK (Side
by side).
”

5. Click the [Apply] button to install these components.



# Page KIBO RPC
- https://jaxa.krpc.jp/#subsection_entry_guidance_movie
  
# Entry and Progamming guide 
- https://jaxa.krpc.jp/download

# Programming tool guide
- https://humans-in-space.jaxa.jp/krpc/1st/download/files/Kibo-RPC_PGManual.pdf

# Simulation Test 
- link: https://jaxa.krpc.jp/user-auth/index.html
- Account ID: w1l-qfzqap
- Password: )76/8[h')8vc'6|



