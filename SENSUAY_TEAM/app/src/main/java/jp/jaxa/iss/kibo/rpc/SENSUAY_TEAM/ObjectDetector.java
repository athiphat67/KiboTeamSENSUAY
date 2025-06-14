package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;
import android.content.Context;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class ObjectDetector {

    private static final String TAG = "ObjectDetector";
    private static final int INPUT_SIZE = 640; // ขนาดรูปภาพที่ป้อนเข้าโมเดล TFLite (เช่น 640x640)
    private static final float CONFIDENCE_THRESHOLD = 0.5f; // ค่าความเชื่อมั่นขั้นต่ำ (0.0f - 1.0f)
    private static final float IOU_THRESHOLD = 0.45f;   // ค่า IOU สำหรับ Non-Maximum Suppression (0.0f - 1.0f)

    // *** โครงสร้างเอาต์พุตของโมเดล: [Batch, จำนวนฟีเจอร์ต่อการคาดการณ์, จำนวนการคาดการณ์] ***
    // Model output is [1, 15, 8400] <--- ยืนยันตามที่คุณแจ้ง
    private static final int OUTPUT_BATCH = 1;
    private static final int OUTPUT_FEATURES_PER_PROPOSAL = 15;      // จำนวนฟีเจอร์ต่อหนึ่งการคาดการณ์ (x,y,w,h + 11 classes)
    private static final int NUM_TOTAL_PROPOSALS = 8400; // จำนวนการคาดการณ์ทั้งหมด (กล่อง proposals)

    private final Interpreter interpreter;
    private final String[] LABELS; // เปลี่ยนเป็น final เพื่อให้กำหนดค่าใน constructor ได้

    public ObjectDetector(Context context) {
        try {
            // โหลดโมเดล TFLite จากไฟล์ assets
            MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, "SensuayModelV3.tflite");
            Interpreter.Options interpretOptions = new Interpreter.Options();
            interpretOptions.setNumThreads(2); // กำหนดจำนวนเธรดสำหรับ Interpreter
            this.interpreter = new Interpreter(modelBuffer, interpretOptions);
            Log.i(TAG, "TFLite model loaded successfully: SensuayModelV3.tflite");

            // โหลด LABELS จากไฟล์ predefined_classes.txt หรือกำหนดตรงนี้เลย
            // ถ้า LABELS มีค่าคงที่อยู่แล้วตามโค้ดเดิม ไม่จำเป็นต้องโหลดจากไฟล์
            this.LABELS = new String[]{"coin",
                    "compass",
                    "coral",
                    "crystal",
                    "diamond",
                    "emerald",
                    "fossil",
                    "key",
                    "letter",
                    "shell",
                    "treasure_box"
            };
            Log.i(TAG, "Labels loaded. Total labels: " + LABELS.length);

        } catch (Exception e) {
            Log.e(TAG, "Failed to load TFLite model or labels: " + e.getMessage(), e);
            throw new RuntimeException("Failed to load TFLite model or labels", e);
        }
    }

    /**
     * ประมวลผลรูปภาพ (Mat) ด้วยโมเดล Object Detector
     *
     * @param image รูปภาพต้นฉบับในรูปแบบ Mat (OpenCV)
     * @return List ของ Map ที่แต่ละ Map เป็นการตรวจจับที่ผ่าน NMS แล้ว (มี confidence, classId, className)
     */
    public List<Map<String, Object>> processImage(DataPaper image) {
        List<Map<String, Object>> finalDetections = new ArrayList<>();
        Log.i(TAG, "Starting image processing.");
        try {
            Mat img = image.getCaptureImage();
            // แปลง Mat (OpenCV) เป็น Bitmap (Android)
            Bitmap bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bitmap);
            Log.i(TAG, "Converted Mat to Bitmap. Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            if (bitmap == null) {
                Log.e(TAG, "Bitmap conversion failed.");
                throw new RuntimeException("Failed to convert image to bitmap");
            }

            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap.getHeight();
            Log.i(TAG, "Original image dimensions: " + originalWidth + "x" + originalHeight);

            // เตรียม TensorImage สำหรับป้อนเข้าโมเดล
            TensorImage tensorImage = new TensorImage();
            tensorImage.load(bitmap); // โหลด Bitmap ต้นฉบับ (ImageProcessor จะจัดการปรับขนาดในภายหลัง)
            Log.i(TAG, "Bitmap loaded into TensorImage.");

            // สร้าง ImageProcessor เพื่อประมวลผลรูปภาพก่อนเข้าโมเดล
            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new NormalizeOp(0f, 255f)) // Normalize ค่าพิกเซลให้อยู่ในช่วง [0,1]
                    .build();
            tensorImage = imageProcessor.process(tensorImage);
            Log.i(TAG, "Image pre-processed by ImageProcessor. TensorImage shape: " + tensorImage.getTensorBuffer().getShape()[0] + "x" + tensorImage.getTensorBuffer().getShape()[1]);


            // เตรียม Output Array สำหรับรับผลลัพธ์จาก Interpreter
            float[][][] output = new float[OUTPUT_BATCH][OUTPUT_FEATURES_PER_PROPOSAL][NUM_TOTAL_PROPOSALS];
            Log.i(TAG, "Output array initialized with shape: [" + OUTPUT_BATCH + ", " + OUTPUT_FEATURES_PER_PROPOSAL + ", " + NUM_TOTAL_PROPOSALS + "]");

            // รัน Interpreter เพื่ออนุมานผลลัพธ์
            interpreter.run(tensorImage.getBuffer(), output);
            Log.i(TAG, "Model inference completed successfully.");

            // เรียกใช้ filterConfidenceThreshold เพื่อกรองและประมวลผลผลลัพธ์ดิบ
            finalDetections = filterConfidenceThreshold(output, originalWidth, originalHeight);

            if (finalDetections.size() == 0) {
                finalDetections = processBackupImageForDetections(image);
            }

            Log.i(TAG, "Filter and NMS process finished. Found " + finalDetections.size() + " final detections.");

        } catch (Exception e) {
            Log.e(TAG, "Error processing image: " + e.getMessage(), e);
            throw new RuntimeException("Error processing image: " + e.getMessage(), e);
        }
        Log.i(TAG, "Finished image processing.");
        return finalDetections; // คืนค่าผลลัพธ์การตรวจจับสุดท้าย
    }


    /**
     * กรองการคาดการณ์ของโมเดลตาม Confidence Threshold และเตรียมข้อมูลสำหรับ NMS
     *
     * @param output ผลลัพธ์ดิบจากโมเดล TFLite มีรูปร่าง [1, 15, 8400] (ตามที่กำหนดค่าคงที่)
     * @param originalWidth ความกว้างของรูปภาพต้นฉบับ
     * @param originalHeight ความสูงของรูปภาพต้นฉบับ
     * @return List ของ Map ที่แต่ละ Map เป็นการตรวจจับที่ผ่าน NMS แล้ว (มี confidence, classId, className)
     */
    private List<Map<String, Object>> filterConfidenceThreshold(float[][][] output, int originalWidth, int originalHeight) {
        Log.i(TAG, "filterConfidenceThreshold method started for raw output processing.");

        List<RectF> allBoxes = new ArrayList<>();
        List<Float> allConfidences = new ArrayList<>();
        List<Integer> allClasses = new ArrayList<>();

        // เข้าถึงข้อมูลการคาดการณ์จริง (output[0] คือ Batch แรกและ Batch เดียว)
        // รูปร่างของ output[0] คือ [15][8400]
        float[][] rawPredictions = output[0];
        Log.i(TAG, "Raw predictions array shape: [" + rawPredictions.length + "][" + rawPredictions[0].length + "]");


        // กำหนดดัชนีสำหรับพิกัด Bounding Box และ Class Probabilities
        final int X_CENTER_IDX = 0;
        final int Y_CENTER_IDX = 1;
        final int WIDTH_IDX = 2;
        final int HEIGHT_IDX = 3;
        final int CLASS_PROBS_START_IDX = 4; // Class probabilities เริ่มต้นจากดัชนีที่ 4

        // จำนวนคลาสคือ จำนวนฟีเจอร์ทั้งหมด (15) - พิกัด BBox (4)
        final int NUM_CLASSES = OUTPUT_FEATURES_PER_PROPOSAL - CLASS_PROBS_START_IDX; // ควรจะเป็น 15 - 4 = 11

        if (LABELS.length != NUM_CLASSES) {
            Log.e(TAG, "LABELS array size (" + LABELS.length + ") does not match calculated NUM_CLASSES (" + NUM_CLASSES + "). Check LABELS definition.");
            // ควรจะมีการจัดการข้อผิดพลาดที่นี่ เช่น throw Exception
        }
        Log.i(TAG, "Calculated number of classes: " + NUM_CLASSES);
        Log.i(TAG, "Processing " + NUM_TOTAL_PROPOSALS + " detection proposals (raw)."); // เปลี่ยนเป็น NUM_TOTAL_PROPOSALS

        // คำนวณ Scale และ Padding Offsets สำหรับการแปลงพิกัด
        // (ส่วนนี้สำคัญมากในการแปลงพิกัดจาก Normalize ไปสู่พิกเซลจริงบนภาพต้นฉบับ)
        float scale = Math.min((float) INPUT_SIZE / originalWidth, (float) INPUT_SIZE / originalHeight);
        int scaledWidth = Math.round(originalWidth * scale);
        int scaledHeight = Math.round(originalHeight * scale);
        int dx = (INPUT_SIZE - scaledWidth) / 2; // Offset แกน X (padding ซ้าย/ขวา)
        int dy = (INPUT_SIZE - scaledHeight) / 2; // Offset แกน Y (padding บน/ล่าง)
        Log.i(TAG, "Scale: " + scale + ", dx (x-padding): " + dx + ", dy (y-padding): " + dy);


        // วนลูปผ่านการคาดการณ์ทั้งหมด 8400 กล่อง (NUM_TOTAL_PROPOSALS)
        for (int i = 0; i < NUM_TOTAL_PROPOSALS; i++) { // i คือ proposal index
            // Extract bbox coordinates (normalized from 0-1 based on INPUT_SIZE with padding)
            float x_center_normalized = rawPredictions[X_CENTER_IDX][i]; // แถวฟีเจอร์ X, คอลัมน์ proposal i
            float y_center_normalized = rawPredictions[Y_CENTER_IDX][i]; // แถวฟีเจอร์ Y, คอลัมน์ proposal i
            float width_normalized = rawPredictions[WIDTH_IDX][i];       // แถวฟีเจอร์ Width, คอลัมน์ proposal i
            float height_normalized = rawPredictions[HEIGHT_IDX][i];     // แถวฟีเจอร์ Height, คอลัมน์ proposal i

            // ค้นหาคลาสที่มีความน่าจะเป็นสูงสุดและค่าความเชื่อมั่น
            float maxConfidence = -1.0f;
            int classId = -1;

            // วนลูปผ่านค่าความน่าจะเป็นของคลาสสำหรับ proposal ปัจจุบัน
            for (int j = 0; j < NUM_CLASSES; j++) { // j คือ class index
                float classProb = rawPredictions[CLASS_PROBS_START_IDX + j][i]; // แถวฟีเจอร์ Class j, คอลัมน์ proposal i
                if (classProb > maxConfidence) {
                    maxConfidence = classProb;
                    classId = j;
                }
            }

            // ใช้ Confidence Threshold ในการกรอง
            if (maxConfidence >= CONFIDENCE_THRESHOLD) {
                // แปลงพิกัด BBox จาก Normalize (YOLO) ไปเป็นพิกเซลจริงบนรูปภาพต้นฉบับ
                RectF bbox = calculateBoxCoordinates(
                        x_center_normalized, y_center_normalized,
                        width_normalized, height_normalized,
                        INPUT_SIZE, dx, dy, scale,
                        originalWidth, originalHeight
                );

                // เพิ่มลงในลิสต์ทั้งหมดเฉพาะถ้า BBox ที่คำนวณได้ถูกต้อง
                if (bbox != null) {
                    allBoxes.add(bbox);
                    allConfidences.add(maxConfidence);
                    allClasses.add(classId);
                    // Log.d(TAG, String.format("Proposal %d accepted. Class: %s (ID: %d), Conf: %.2f, Box: [%.0f,%.0f,%.0f,%.0f]",
                    //                          i, LABELS[classId], classId, maxConfidence, bbox.left, bbox.top, bbox.right, bbox.bottom));
                }
            }
        }

        Log.i(TAG, "Total detections after confidence threshold: " + allBoxes.size());

        // --- ใช้ Non-Maximum Suppression (NMS) ---
        int[] nmsResultIndices = nonMaxSuppression(allBoxes, allConfidences, IOU_THRESHOLD);
        Log.i(TAG, "Total detections after NMS: " + nmsResultIndices.length);

        // สร้างลิสต์ผลลัพธ์สุดท้ายของการตรวจจับที่ผ่าน NMS แล้ว
        List<Map<String, Object>> finalDetections = new ArrayList<>();
        for (int idx : nmsResultIndices) {
            // RectF bbox = allBoxes.get(idx); // ไม่จำเป็นต้องใช้ bbox ถ้าไม่แสดงผล หรือต้องการแค่ข้อมูลคลาส
            float confidence = allConfidences.get(idx);
            int classId = allClasses.get(idx);
            String className = (classId >= 0 && classId < LABELS.length) ? LABELS[classId] : "Unknown";

            Map<String, Object> detectionMap = new HashMap<>();
            detectionMap.put("confidence", confidence);
            detectionMap.put("classId", classId);
            detectionMap.put("className", className);
            // ไม่ได้ใส่ x1, y1, x2, y2 ลงใน map ถ้าไม่ต้องการ

            finalDetections.add(detectionMap);
            Log.d(TAG, String.format("Final Detection: Class: %s (ID: %d), Conf: %.2f",
                    className, classId, confidence));
        }

        Log.i(TAG, "filterConfidenceThreshold finished. Total final detections: " + finalDetections.size());

        return finalDetections;
    }

    /**
     * คำนวณพิกัด Bounding Box สุดท้ายในหน่วยพิกเซลของรูปภาพต้นฉบับ
     * โดยแปลงจากพิกัด Normalize (center_x, center_y, width, height)
     * และปรับแก้ตาม Scaling และ Padding
     *
     * @param x_center_normalized พิกัด X กลางของกล่อง (0-1, Normalize)
     * @param y_center_normalized พิกัด Y กลางของกล่อง (0-1, Normalize)
     * @param width_normalized ความกว้างของกล่อง (0-1, Normalize)
     * @param height_normalized ความสูงของกล่อง (0-1, Normalize)
     * @param targetSize ขนาดเป้าหมายของรูปภาพที่ป้อนเข้าโมเดล (เช่น 640)
     * @param dx ค่า Padding ทางแกน X
     * @param dy ค่า Padding ทางแกน Y
     * @param scale ค่า Scale ที่ใช้ปรับขนาดรูปภาพ
     * @param originalWidth ความกว้างของรูปภาพต้นฉบับ
     * @param originalHeight ความสูงของรูปภาพต้นฉบับ
     * @return RectF ที่มีพิกัด (x1, y1, x2, y2) ของ Bounding Box ในหน่วยพิกเซลของรูปภาพต้นฉบับ, หรือ null ถ้ากล่องไม่ถูกต้อง
     */
    private RectF calculateBoxCoordinates(
            float x_center_normalized, float y_center_normalized,
            float width_normalized, float height_normalized,
            int targetSize, int dx, int dy, float scale,
            int originalWidth, int originalHeight) {

        float x1_padded_scaled = (x_center_normalized - width_normalized / 2.0f) * targetSize;
        float y1_padded_scaled = (y_center_normalized - height_normalized / 2.0f) * targetSize;
        float x2_padded_scaled = (x_center_normalized + width_normalized / 2.0f) * targetSize;
        float y2_padded_scaled = (y_center_normalized + height_normalized / 2.0f) * targetSize;

        float x1 = (x1_padded_scaled - dx) / scale;
        float y1 = (y1_padded_scaled - dy) / scale;
        float x2 = (x2_padded_scaled - dx) / scale;
        float y2 = (y2_padded_scaled - dy) / scale;

        x1 = clamp(x1, 0, originalWidth - 1);
        y1 = clamp(y1, 0, originalHeight - 1);
        x2 = clamp(x2, 0, originalWidth - 1);
        y2 = clamp(y2, 0, originalHeight - 1);

        if (x2 <= x1 || y2 <= y1) {
            Log.d(TAG, "Invalid box after clipping. Skipping.");
            return null;
        }
        return new RectF(x1, y1, x2, y2);
    }

    /**
     * คำนวณ Intersection over Union (IoU) ของ Bounding Boxes สองกล่อง
     * @param box1 Bounding Box แรก (RectF)
     * @param box2 Bounding Box ที่สอง (RectF)
     * @return ค่า IoU
     */
    private float calculateIoU(RectF box1, RectF box2) {
        float intersectionX1 = Math.max(box1.left, box2.left);
        float intersectionY1 = Math.max(box1.top, box2.top);
        float intersectionX2 = Math.min(box1.right, box2.right);
        float intersectionY2 = Math.min(box1.bottom, box2.bottom);

        float intersectionWidth = Math.max(0, intersectionX2 - intersectionX1);
        float intersectionHeight = Math.max(0, intersectionY2 - intersectionY1);

        float intersectionArea = intersectionWidth * intersectionHeight;

        float box1Area = (box1.right - box1.left) * (box1.bottom - box1.top);
        float box2Area = (box2.right - box2.left) * (box2.bottom - box2.top);

        float unionArea = box1Area + box2Area - intersectionArea;

        if (unionArea == 0) {
            return 0.0f;
        }
        return intersectionArea / unionArea;
    }

    /**
     * ใช้ Non-Maximum Suppression (NMS) เพื่อกรอง Bounding Boxes ที่ทับซ้อนกัน
     * @param boxes List ของ Bounding Boxes (RectF)
     * @param confidences List ของ Confidence Scores ที่สอดคล้องกัน
     * @param iouThreshold ค่า IoU Threshold สำหรับการกำจัดกล่องที่ทับซ้อนกัน
     * @return อาร์เรย์ของดัชนีของการตรวจจับที่จะเก็บไว้หลัง NMS
     */
    private int[] nonMaxSuppression(List<RectF> boxes, final List<Float> confidences, float iouThreshold) {
        Log.i(TAG, "Starting NMS with " + boxes.size() + " boxes.");
        if (boxes.isEmpty() || confidences.isEmpty() || boxes.size() != confidences.size()) {
            Log.w(TAG, "NMS input is invalid: boxes or confidences list is empty or sizes don't match.");
            return new int[0];
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < confidences.size(); i++) {
            indices.add(i);
        }

        // เรียงลำดับดัชนีตาม Confidence Score จากมากไปน้อย
        Collections.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) {
                return Float.compare(confidences.get(idx2), confidences.get(idx1)); // เรียงแบบ Descending Order (มากไปน้อย)
            }
        });

        List<Integer> picked = new ArrayList<>(); // ดัชนีของกล่องที่ถูกเลือก (ที่ผ่าน NMS)
        boolean[] suppressed = new boolean[confidences.size()]; // สถานะว่ากล่องถูกกำจัดไปแล้วหรือไม่

        for (int i = 0; i < indices.size(); i++) {
            int currentIdx = indices.get(i); // ดึงดัชนีของกล่องที่มี Confidence สูงสุดที่ยังไม่ถูกกำจัด
            if (suppressed[currentIdx]) {
                continue; // ถ้ากล่องนี้ถูกกำจัดไปแล้ว ก็ข้ามไป
            }
            picked.add(currentIdx); // เพิ่มกล่องนี้เข้าไปในลิสต์ที่เลือก

            RectF currentBox = boxes.get(currentIdx); // Bounding Box ปัจจุบันที่ถูกเลือก

            // เปรียบเทียบกล่องปัจจุบันกับกล่องที่เหลือทั้งหมด
            for (int j = i + 1; j < indices.size(); j++) {
                int nextIdx = indices.get(j);
                if (suppressed[nextIdx]) {
                    continue; // ถ้ากล่องถัดไปถูกกำจัดไปแล้ว ก็ข้ามไป
                }

                RectF nextBox = boxes.get(nextIdx); // Bounding Box ถัดไป
                float iou = calculateIoU(currentBox, nextBox); // คำนวณ IoU ระหว่างสองกล่อง

                if (iou > iouThreshold) {
                    suppressed[nextIdx] = true; // ถ้า IoU สูงกว่า Threshold แสดงว่าทับซ้อนกันมาก ก็กำจัดกล่องที่สอง
                }
            }
        }
        // แปลง List ของดัชนีที่เลือกให้เป็น Array ของ int
        int[] result = new int[picked.size()];
        for (int i = 0; i < picked.size(); i++) {
            result[i] = picked.get(i);
        }
        Log.i(TAG, "NMS finished. Kept " + result.length + " boxes.");
        return result;
    }

    /**
     * ฟังก์ชัน Clamp (ตัดขอบ) เพื่อจำกัดค่าให้อยู่ในช่วงที่กำหนด
     * @param value ค่าที่ต้องการ Clamp
     * @param min ค่าต่ำสุดที่อนุญาต
     * @param max ค่าสูงสุดที่อนุญาต
     * @return ค่าที่ถูก Clamp แล้ว
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<Map<String, Object>> processBackupImageForDetections(DataPaper image) {

        Log.i("StartBackup", "Start Use BackupImg");

        List<Map<String, Object>> BackupfinalDetections = new ArrayList<>();

        try {
            Mat img = image.getBackupImage();
            // แปลง Mat (OpenCV) เป็น Bitmap (Android)
            Bitmap bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bitmap);
            Log.i(TAG, "Converted Mat to Bitmap. Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            if (bitmap == null) {
                Log.e(TAG, "Bitmap conversion failed.");
                throw new RuntimeException("Failed to convert image to bitmap");
            }

            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap.getHeight();
            Log.i(TAG, "Original image dimensions: " + originalWidth + "x" + originalHeight);

            // เตรียม TensorImage สำหรับป้อนเข้าโมเดล
            TensorImage tensorImage = new TensorImage();
            tensorImage.load(bitmap); // โหลด Bitmap ต้นฉบับ (ImageProcessor จะจัดการปรับขนาดในภายหลัง)
            Log.i(TAG, "Bitmap loaded into TensorImage.");

            // สร้าง ImageProcessor เพื่อประมวลผลรูปภาพก่อนเข้าโมเดล
            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new NormalizeOp(0f, 255f)) // Normalize ค่าพิกเซลให้อยู่ในช่วง [0,1]
                    .build();
            tensorImage = imageProcessor.process(tensorImage);
            Log.i(TAG, "Image pre-processed by ImageProcessor. TensorImage shape: " + tensorImage.getTensorBuffer().getShape()[0] + "x" + tensorImage.getTensorBuffer().getShape()[1]);


            // เตรียม Output Array สำหรับรับผลลัพธ์จาก Interpreter
            float[][][] output = new float[OUTPUT_BATCH][OUTPUT_FEATURES_PER_PROPOSAL][NUM_TOTAL_PROPOSALS];
            Log.i(TAG, "Output array initialized with shape: [" + OUTPUT_BATCH + ", " + OUTPUT_FEATURES_PER_PROPOSAL + ", " + NUM_TOTAL_PROPOSALS + "]");

            // รัน Interpreter เพื่ออนุมานผลลัพธ์
            interpreter.run(tensorImage.getBuffer(), output);
            Log.i(TAG, "Model inference completed successfully.");

            BackupfinalDetections = filterConfidenceThreshold(output, originalWidth, originalHeight);

        } catch (Exception e) {
            Log.e(TAG, "Error processing " + ": " + e.getMessage(), e);
        }


        return BackupfinalDetections;
    }

}