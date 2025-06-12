package jp.jaxa.iss.kibo.rpc.SENSUAY_TEAM;

import android.graphics.Bitmap;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectDetector {

    private static final String TAG = "ObjectDetector";
    private static final int INPUT_SIZE = 640; // TFLite model input image size
    private static final float CONFIDENCE_THRESHOLD = 0.4f;
    // Model output is [1, 15, 8400]
    private static final int OUTPUT_BATCH = 1;
    private static final int OUTPUT_PREDICTIONS = 15;
    private static final int VALUES_PER_DETECTION = 8400;
    private final Interpreter interpreter;
    private static final String[] LABELS = new String[]{"coin",
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

    public ObjectDetector(Context context) {
        try {
            MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, "SensuayModelV3.tflite");
            Interpreter.Options interpretOptions = new Interpreter.Options();
            interpretOptions.setNumThreads(2);
            this.interpreter = new Interpreter(modelBuffer, interpretOptions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load TFLite model", e);
        }
    }

    public ArrayList<Map<String, Object>> processImage(Mat image) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);;
            Utils.matToBitmap(image,bitmap);

            if (bitmap == null) {
                Log.e(TAG, "Failed to convert image to bitmap");
                throw new RuntimeException("Failed to convert image to bitmap");
            }

            Bitmap scaleFitImage = bitmap;

            TensorImage tensorImage = new TensorImage();
            tensorImage.load(scaleFitImage);

            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new NormalizeOp(0f, 255f)) // Normalize to [0,1]
                    .build();
            tensorImage = imageProcessor.process(tensorImage);

            float[][][] output = new float[OUTPUT_BATCH][OUTPUT_PREDICTIONS][VALUES_PER_DETECTION];
            interpreter.run(tensorImage.getBuffer(), output);

            Log.i(TAG, "Success Interpreter");

            return parseDetections(output, 640, 640);

        } catch (Exception e) {
            Log.e(TAG, "Error processing image: " + e.getMessage(), e);
            throw new RuntimeException("Error processing image: " + e.getMessage(), e);
        }
    }

    private ArrayList<Map<String, Object>> parseDetections(float[][][] output, int originalWidth, int originalHeight) {
        Log.i("parseDetections", "Method started.");
        Log.i("parseDetections", "Input - originalWidth: " + originalWidth + ", originalHeight: " + originalHeight);

        ArrayList<Map<String, Object>> detections = new ArrayList<>();
        Log.i("parseDetections", "Initialized detections list.");

        // Calculate scale and padding offsets using the helper class
        int targetSize = INPUT_SIZE;
        Log.i("parseDetections", "targetSize (INPUT_SIZE): " + targetSize);

        float scale = Math.min((float) (targetSize / originalWidth), (float) (targetSize / originalHeight));
        Log.i("parseDetections", "scale: " + scale);

        int scaledWidth = Math.round(originalWidth * scale);
        Log.i("parseDetections", "scaledWidth: " + scaledWidth);

        int scaledHeight = Math.round(originalHeight * scale);
        Log.i("parseDetections", "scaledHeight: " + scaledHeight);

        int dx = (targetSize - scaledWidth) / 2 ;
        Log.i("parseDetections", "dx (padding x-offset): " + dx);

        int dy = (targetSize - scaledHeight) / 2 ;
        Log.i("parseDetections", "dy (padding y-offset): " + dy);

        for (float[] prediction : output[0]) {
            Log.i("parseDetections", "Processing a new prediction.");

            float confidence = prediction[4];
            Log.i("parseDetections", "confidence: " + confidence);
            for (float num : prediction) {
                Log.i("parseDetections", String.valueOf(num));
            }

            if (confidence < CONFIDENCE_THRESHOLD) {
                Log.i("parseDetections", "Confidence (" + confidence + ") is below threshold (" + CONFIDENCE_THRESHOLD + "). Skipping this prediction.");
                continue;
            }

            // Get normalized coordinates relative to the INPUT_SIZE x INPUT_SIZE image
            float nx1 = clamp(prediction[0], 0f, 1f); // x1
            Log.i("parseDetections", "nx1 (normalized x1): " + nx1);

            float ny1 = clamp(prediction[1], 0f, 1f); // y1
            Log.i("parseDetections", "ny1 (normalized y1): " + ny1);

            float nx2 = clamp(prediction[2], 0f, 1f); // x2
            Log.i("parseDetections", "nx2 (normalized x2): " + nx2);

            float ny2 = clamp(prediction[3], 0f, 1f); // y2
            Log.i("parseDetections", "ny2 (normalized y2): " + ny2);

            // Convert normalized coordinates to pixel coordinates in the padded image (INPUT_SIZE x INPUT_SIZE)
            float paddedX1 = nx1 * targetSize;
            Log.i("parseDetections", "paddedX1: " + paddedX1);

            float paddedY1 = ny1 * targetSize;
            Log.i("parseDetections", "paddedY1: " + paddedY1);

            float paddedX2 = nx2 * targetSize;
            Log.i("parseDetections", "paddedX2: " + paddedX2);

            float paddedY2 = ny2 * targetSize;
            Log.i("parseDetections", "paddedY2: " + paddedY2);

            // Remove padding offsets to get coordinates relative to the scaled image
            float scaledX1 = paddedX1 - dx;
            Log.i("parseDetections", "scaledX1: " + scaledX1);

            float scaledY1 = paddedY1 - dy;
            Log.i("parseDetections", "scaledY1: " + scaledY1);

            float scaledX2 = paddedX2 - dx;
            Log.i("parseDetections", "scaledX2: " + scaledX2);

            float scaledY2 = paddedY2 - dy;
            Log.i("parseDetections", "scaledY2: " + scaledY2);

            // Scale back to original image dimensions
            double originalX1 = scaledX1 / scale;
            Log.i("parseDetections", "originalX1 (before clamp): " + originalX1);

            double originalY1 = scaledY1 / scale;
            Log.i("parseDetections", "originalY1 (before clamp): " + originalY1);

            double originalX2 = scaledX2 / scale;
            Log.i("parseDetections", "originalX2 (before clamp): " + originalX2);

            double originalY2 = scaledY2 / scale;
            Log.i("parseDetections", "originalY2 (before clamp): " + originalY2);

            // Clamp coordinates to original image bounds
            originalX1 = Math.max(0, Math.min(originalWidth, originalX1));
            Log.i("parseDetections", "originalX1 (after clamp): " + originalX1);

            originalY1 = Math.max(0, Math.min(originalHeight, originalY1));
            Log.i("parseDetections", "originalY1 (after clamp): " + originalY1);

            originalX2 = Math.max(0, Math.min(originalWidth, originalX2));
            Log.i("parseDetections", "originalX2 (after clamp): " + originalX2);

            originalY2 = Math.max(0, Math.min(originalHeight, originalY2));
            Log.i("parseDetections", "originalY2 (after clamp): " + originalY2);

            // Calculate width and height in original image pixels
            double originalBoxWidth = originalX2 - originalX1;
            Log.i("parseDetections", "originalBoxWidth: " + originalBoxWidth);

            double originalBoxHeight = originalY2 - originalY1;
            Log.i("parseDetections", "originalBoxHeight: " + originalBoxHeight);

            int classId = (int) prediction[5];
            Log.i("parseDetections", "classId: " + classId);

            Map<String, Object> result = new HashMap<>();
            String label = (classId >= 0 && classId < LABELS.length) ? LABELS[classId] : "unknown";
            result.put("label", label);
            Log.i("parseDetections", "Detection label: " + label);

            result.put("confidence", (double) confidence);
            Log.i("parseDetections", "Detection confidence: " + confidence);

            Map<String, Object> origin = new HashMap<>();
            origin.put("x", originalX1);
            origin.put("y", originalY1);
            result.put("origin", origin);
            Log.i("parseDetections", "Detection origin: x=" + originalX1 + ", y=" + originalY1);

            Map<String, Object> size = new HashMap<>();
            size.put("width", originalBoxWidth);
            size.put("height", originalBoxHeight);
            result.put("size", size);
            Log.i("parseDetections", "Detection size: width=" + originalBoxWidth + ", height=" + originalBoxHeight);

            detections.add(result);
            Log.i("parseDetections", "Added detection to list.");
        }

        Log.i("parseDetections", "Method finished. Total detections: " + detections.size());
        return detections;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

}
