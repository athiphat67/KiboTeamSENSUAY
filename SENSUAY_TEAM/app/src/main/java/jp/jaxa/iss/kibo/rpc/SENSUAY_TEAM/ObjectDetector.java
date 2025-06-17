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
    private static final int INPUT_SIZE = 640;
    private static final float CONFIDENCE_THRESHOLD = 0.5f;
    private static final float IOU_THRESHOLD = 0.45f;

    // Model output is [1, 15, 8400]
    private static final int OUTPUT_BATCH = 1;
    private static final int OUTPUT_FEATURES_PER_PROPOSAL = 15;
    private static final int NUM_TOTAL_PROPOSALS = 8400;

    private final Interpreter interpreter;
    private final String[] LABELS;

    public ObjectDetector(Context context) {
        try {
            MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, "SensuayModelV3.tflite");
            Interpreter.Options interpretOptions = new Interpreter.Options();
            interpretOptions.setNumThreads(2);
            this.interpreter = new Interpreter(modelBuffer, interpretOptions);
            Log.i(TAG, "TFLite model loaded successfully: SensuayModelV3.tflite");

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
     * Processes an image (Mat) using the Object Detector model.
     *
     * @param image The original image in Mat (OpenCV) format.
     * @return A list of Maps, where each Map represents a detection after NMS (containing confidence, classId, className).
     */
    public List<Map<String, Object>> processImage(DataPaper image) {
        List<Map<String, Object>> finalDetections = new ArrayList<>();
        Log.i(TAG, "Starting image processing.");
        try {
            Mat img = image.getCaptureImage();
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

            TensorImage tensorImage = new TensorImage();
            tensorImage.load(bitmap);
            Log.i(TAG, "Bitmap loaded into TensorImage.");

            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new NormalizeOp(0f, 255f))
                    .build();
            tensorImage = imageProcessor.process(tensorImage);
            Log.i(TAG, "Image pre-processed by ImageProcessor. TensorImage shape: " + tensorImage.getTensorBuffer().getShape()[0] + "x" + tensorImage.getTensorBuffer().getShape()[1]);

            float[][][] output = new float[OUTPUT_BATCH][OUTPUT_FEATURES_PER_PROPOSAL][NUM_TOTAL_PROPOSALS];
            Log.i(TAG, "Output array initialized with shape: [" + OUTPUT_BATCH + ", " + OUTPUT_FEATURES_PER_PROPOSAL + ", " + NUM_TOTAL_PROPOSALS + "]");

            interpreter.run(tensorImage.getBuffer(), output);
            Log.i(TAG, "Model inference completed successfully.");

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
        return finalDetections;
    }

    private List<Map<String, Object>> filterConfidenceThreshold(float[][][] output, int originalWidth, int originalHeight) {
        Log.i(TAG, "filterConfidenceThreshold method started for raw output processing.");

        List<RectF> allBoxes = new ArrayList<>();
        List<Float> allConfidences = new ArrayList<>();
        List<Integer> allClasses = new ArrayList<>();

        float[][] rawPredictions = output[0];
        Log.i(TAG, "Raw predictions array shape: [" + rawPredictions.length + "][" + rawPredictions[0].length + "]");

        final int X_CENTER_IDX = 0;
        final int Y_CENTER_IDX = 1;
        final int WIDTH_IDX = 2;
        final int HEIGHT_IDX = 3;
        final int CLASS_PROBS_START_IDX = 4;

        final int NUM_CLASSES = OUTPUT_FEATURES_PER_PROPOSAL - CLASS_PROBS_START_IDX;

        if (LABELS.length != NUM_CLASSES) {
            Log.e(TAG, "LABELS array size (" + LABELS.length + ") does not match calculated NUM_CLASSES (" + NUM_CLASSES + "). Check LABELS definition.");
        }
        Log.i(TAG, "Calculated number of classes: " + NUM_CLASSES);
        Log.i(TAG, "Processing " + NUM_TOTAL_PROPOSALS + " detection proposals (raw).");

        float scale = Math.min((float) INPUT_SIZE / originalWidth, (float) INPUT_SIZE / originalHeight);
        int scaledWidth = Math.round(originalWidth * scale);
        int scaledHeight = Math.round(originalHeight * scale);
        int dx = (INPUT_SIZE - scaledWidth) / 2;
        int dy = (INPUT_SIZE - scaledHeight) / 2;
        Log.i(TAG, "Scale: " + scale + ", dx (x-padding): " + dx + ", dy (y-padding): " + dy);

        for (int i = 0; i < NUM_TOTAL_PROPOSALS; i++) {
            float x_center_normalized = rawPredictions[X_CENTER_IDX][i];
            float y_center_normalized = rawPredictions[Y_CENTER_IDX][i];
            float width_normalized = rawPredictions[WIDTH_IDX][i];
            float height_normalized = rawPredictions[HEIGHT_IDX][i];

            float maxConfidence = -1.0f;
            int classId = -1;

            for (int j = 0; j < NUM_CLASSES; j++) {
                float classProb = rawPredictions[CLASS_PROBS_START_IDX + j][i];
                if (classProb > maxConfidence) {
                    maxConfidence = classProb;
                    classId = j;
                }
            }

            if (maxConfidence >= CONFIDENCE_THRESHOLD) {
                RectF bbox = calculateBoxCoordinates(
                        x_center_normalized, y_center_normalized,
                        width_normalized, height_normalized,
                        INPUT_SIZE, dx, dy, scale,
                        originalWidth, originalHeight
                );

                if (bbox != null) {
                    allBoxes.add(bbox);
                    allConfidences.add(maxConfidence);
                    allClasses.add(classId);
                }
            }
        }

        Log.i(TAG, "Total detections after confidence threshold: " + allBoxes.size());

        int[] nmsResultIndices = nonMaxSuppression(allBoxes, allConfidences, IOU_THRESHOLD);
        Log.i(TAG, "Total detections after NMS: " + nmsResultIndices.length);

        List<Map<String, Object>> finalDetections = new ArrayList<>();
        for (int idx : nmsResultIndices) {
            float confidence = allConfidences.get(idx);
            int classId = allClasses.get(idx);
            String className = (classId >= 0 && classId < LABELS.length) ? LABELS[classId] : "Unknown";

            Map<String, Object> detectionMap = new HashMap<>();
            detectionMap.put("confidence", confidence);
            detectionMap.put("classId", classId);
            detectionMap.put("className", className);

            finalDetections.add(detectionMap);
            Log.d(TAG, String.format("Final Detection: Class: %s (ID: %d), Conf: %.2f",
                    className, classId, confidence));
        }

        Log.i(TAG, "filterConfidenceThreshold finished. Total final detections: " + finalDetections.size());

        return finalDetections;
    }

    /**
     * Calculates the final Bounding Box coordinates in pixels relative to the original image.
     * This converts from normalized coordinates (center_x, center_y, width, height)
     * and adjusts for scaling and padding.
     *
     * @param x_center_normalized X-center coordinate of the box (0-1, normalized)
     * @param y_center_normalized Y-center coordinate of the box (0-1, normalized)
     * @param width_normalized Width of the box (0-1, normalized)
     * @param height_normalized Height of the box (0-1, normalized)
     * @param targetSize Target input size of the model (e.g., 640)
     * @param dx X-axis padding value
     * @param dy Y-axis padding value
     * @param scale Scaling factor used for image resizing
     * @param originalWidth Original image width
     * @param originalHeight Original image height
     * @return RectF with (x1, y1, x2, y2) coordinates of the Bounding Box in original image pixels, or null if the box is invalid.
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
     * Calculates the Intersection over Union (IoU) of two Bounding Boxes.
     * @param box1 The first Bounding Box (RectF).
     * @param box2 The second Bounding Box (RectF).
     * @return The IoU value.
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
     * Applies Non-Maximum Suppression (NMS) to filter overlapping Bounding Boxes.
     * @param boxes List of Bounding Boxes (RectF).
     * @param confidences List of corresponding Confidence Scores.
     * @param iouThreshold IoU Threshold for suppressing overlapping boxes.
     * @return An array of indices of the detections to keep after NMS.
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

        Collections.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) {
                return Float.compare(confidences.get(idx2), confidences.get(idx1));
            }
        });

        List<Integer> picked = new ArrayList<>();
        boolean[] suppressed = new boolean[confidences.size()];

        for (int i = 0; i < indices.size(); i++) {
            int currentIdx = indices.get(i);
            if (suppressed[currentIdx]) {
                continue;
            }
            picked.add(currentIdx);

            RectF currentBox = boxes.get(currentIdx);

            for (int j = i + 1; j < indices.size(); j++) {
                int nextIdx = indices.get(j);
                if (suppressed[nextIdx]) {
                    continue;
                }

                RectF nextBox = boxes.get(nextIdx);
                float iou = calculateIoU(currentBox, nextBox);

                if (iou > iouThreshold) {
                    suppressed[nextIdx] = true;
                }
            }
        }
        int[] result = new int[picked.size()];
        for (int i = 0; i < picked.size(); i++) {
            result[i] = picked.get(i);
        }
        Log.i(TAG, "NMS finished. Kept " + result.length + " boxes.");
        return result;
    }

    /**
     * Clamps a value within a specified range.
     * @param value The value to clamp.
     * @param min The minimum allowed value.
     * @param max The maximum allowed value.
     * @return The clamped value.
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<Map<String, Object>> processBackupImageForDetections(DataPaper image) {

        Log.i("StartBackup", "Start Use BackupImg");

        List<Map<String, Object>> BackupfinalDetections = new ArrayList<>();

        try {
            Mat img = image.getBackupImage();
            Bitmap bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bitmap);
            Log.i(TAG, "Converted Mat to Bitmap. Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            if (bitmap == null) {
                Log.i(TAG, "Bitmap conversion failed.");
                throw new RuntimeException("Failed to convert image to bitmap");
            }

            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap.getHeight();
            Log.i(TAG, "Original image dimensions: " + originalWidth + "x" + originalHeight);

            TensorImage tensorImage = new TensorImage();
            tensorImage.load(bitmap);
            Log.i(TAG, "Bitmap loaded into TensorImage.");

            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new NormalizeOp(0f, 255f))
                    .build();
            tensorImage = imageProcessor.process(tensorImage);
            Log.i(TAG, "Image pre-processed by ImageProcessor. TensorImage shape: " + tensorImage.getTensorBuffer().getShape()[0] + "x" + tensorImage.getTensorBuffer().getShape()[1]);

            float[][][] output = new float[OUTPUT_BATCH][OUTPUT_FEATURES_PER_PROPOSAL][NUM_TOTAL_PROPOSALS];
            Log.i(TAG, "Output array initialized with shape: [" + OUTPUT_BATCH + ", " + OUTPUT_FEATURES_PER_PROPOSAL + ", " + NUM_TOTAL_PROPOSALS + "]");

            interpreter.run(tensorImage.getBuffer(), output);
            Log.i(TAG, "Model inference completed successfully.");

            BackupfinalDetections = filterConfidenceThreshold(output, originalWidth, originalHeight);

        } catch (Exception e) {
            Log.i(TAG, "Error processing " + ": " + e.getMessage(), e);
        }

        return BackupfinalDetections;
    }
}