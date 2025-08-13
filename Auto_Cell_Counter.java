
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.*;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.Field;
import javax.swing.UIManager;

public class Auto_Cell_Counter implements PlugIn, ActionListener,
        TextListener {

    String imageFolder;
    Dimension screen;
    //microscope images
    ImageProcessor ipChannelBlue = null;
    ImageProcessor ipChannelGreen = null;
    ImageProcessor ipChannelRed = null;
    ImageProcessor ipChannelWhite = null;

    // displayed images
    ImagePlus impBlue0 = null;
    ImagePlus impGreen0 = null;
    ImagePlus impRed0 = null;
    ImagePlus impWhite0 = null;
    ImageProcessor ipBlue0 = null;
    ImageProcessor ipGreen0 = null;
    ImageProcessor ipRed0 = null;
    ImageProcessor ipWhite0 = null;
    // images for calculation
    ImagePlus impBlue = null;
    ImagePlus impGreen = null;
    ImagePlus impRed = null;
    ImagePlus impWhite = null;
    ImageProcessor ipBlue = null;
    ImageProcessor ipGreen = null;
    ImageProcessor ipRed = null;
    ImageProcessor ipWhite = null;

    int blueRadius;
    int blueProm;
    int blueThresh;
    int greenRadius;
    int greenProm;
    int greenThresh;
    int redRadius;
    int redProm;
    int redThresh;
    int whiteRadius;
    int whiteProm;
    int whiteThresh;

    int width;
    int height;

    ImageProcessor ipBlueSegmented;
    ByteProcessor ipBluePoints;
    ByteProcessor ipGreenPoints;
    ByteProcessor ipRedPoints;
    ByteProcessor ipWhitePoints;

    // displyed masks
    ImagePlus impBlueMask0;
    ImagePlus impGreenMask0;
    ImagePlus impRedMask0;
    ImagePlus impWhiteMask0;

    // calculated masks 
    ImagePlus impBlueMask;
    ByteProcessor ipBlueMask;
    ImagePlus impGreenMask;
    ByteProcessor ipGreenMask;
    ImagePlus impRedMask;
    ByteProcessor ipRedMask;
    ImagePlus impWhiteMask;
    ByteProcessor ipWhiteMask;

    Roi[] roiArray;
    int[][] roiIndexArray;
    boolean[][] roiChannelArray;

    int numBlue;
    int numGreen;
    int numRed;
    int numWhite;
    int numGreenRed;
    int numRedWhite;
    int numGreenWhite;
    int numGreenRedWhite;

    Frame frame = null;
    Label folderLabel = null;

    TextField blueRadiusField = null;
    TextField bluePromField = null;
    TextField blueThreshField = null;
    TextField greenRadiusField = null;
    TextField greenThreshField = null;
    TextField greenPromField = null;
    TextField redRadiusField = null;
    TextField redThreshField = null;
    TextField redPromField = null;
    TextField whiteRadiusField = null;
    TextField whitePromField = null;
    TextField whiteThreshField = null;

    Button peakButton;

    Checkbox batchCheckbox;
    Checkbox holdCheckbox;

    BatchHandler batchHandler;
    Boolean boolBatchHandler;

    MaximumFinder mf;

    @Override
    public void run(String arg) {

        imageFolder = IJ.getDirectory("Select image folder...");
        if (imageFolder == null) {
            return;
        }
        // read blue, green, red and white images
        if (readImages() < 4) {
            IJ.log("Does not have blue channel");
            return;
        }

        screen = Toolkit.getDefaultToolkit().getScreenSize();

        mf = new MaximumFinder();

        impBlue = new ImagePlus();
        impGreen = new ImagePlus();
        impRed = new ImagePlus();
        impWhite = new ImagePlus();
        impBlue0 = new ImagePlus();
        impGreen0 = new ImagePlus();
        impRed0 = new ImagePlus();
        impWhite0 = new ImagePlus();
        impBlueMask0 = new ImagePlus();
        impGreenMask0 = new ImagePlus();
        impRedMask0 = new ImagePlus();
        impWhiteMask0 = new ImagePlus();

        impBlueMask = NewImage.createByteImage("Blue mask", width, height, 1, NewImage.FILL_BLACK);
        ipBlueMask = (ByteProcessor) impBlueMask.getProcessor();
        impGreenMask = NewImage.createByteImage("Green mask", width, height, 1, NewImage.FILL_BLACK);
        ipGreenMask = (ByteProcessor) impGreenMask.getProcessor();
        impRedMask = NewImage.createByteImage("Red mask", width, height, 1, NewImage.FILL_BLACK);
        ipRedMask = (ByteProcessor) impRedMask.getProcessor();
        impWhiteMask = NewImage.createByteImage("White mask", width, height, 1, NewImage.FILL_BLACK);
        ipWhiteMask = (ByteProcessor) impWhiteMask.getProcessor();

        blueRadius = 3;
        greenRadius = 3;
        redRadius = 3;
        whiteRadius = 3;

        blueProm = 6;
        greenProm = 6;
        redProm = 6;
        whiteProm = 6;

        // filter blue, green, red and white images
        filterImages();
        // get thresholds of blue, green, red and white images 
        getThresholds();
        displayImages();
        getMaxima();
        getBlueMask();
        getRoiIndexArray();
        getMasks();
        doCrossChannels();
        displayMasks();
        showResult();
        showFrame();
    }

    public int readImages() {
        // Read 4 images: blue, green, red and white
        ImagePlus imp;
        int numChannels = 0;
        File pathFile = new File(File.separator + imageFolder);
        String[] fileList = pathFile.list();
        int listLength = fileList.length;
        ipChannelBlue = null;
        ipChannelGreen = null;
        ipChannelRed = null;
        ipChannelWhite = null;
        for (int index = 0; index < listLength; index++) {
            //IJ.log(fileList[index]);
            if (fileList[index].contains("C001.tif")) {
                imp = new ImagePlus(imageFolder + fileList[index]);
                if (readRGB(imp)) {
                    numChannels++;
                }
            } else if (fileList[index].contains("C002.tif")) {
                imp = new ImagePlus(imageFolder + fileList[index]);
                if (readRGB(imp)) {
                    numChannels++;
                }
            } else if (fileList[index].contains("C003.tif")) {
                imp = new ImagePlus(imageFolder + fileList[index]);
                if (readRGB(imp)) {
                    numChannels++;
                }
            } else if (fileList[index].contains("C004.tif")) {
                imp = new ImagePlus(imageFolder + fileList[index]);
                if (readRGB(imp)) {
                    numChannels++;
                }
            }
        }

        if (ipChannelBlue == null) {
            return 0;
        }

        if (ipChannelGreen == null) {
            ipChannelGreen = ipChannelBlue.duplicate();
            numChannels++;
        }
        if (ipChannelRed == null) {
            ipChannelRed = ipChannelBlue.duplicate();
            numChannels++;
        }
        if (ipChannelWhite == null) {
            ipChannelWhite = ipChannelBlue.duplicate();
            numChannels++;
        }
        /*
        ImagePlus[] stack = new ImagePlus[7];
        // 0-red, 1-green, 2-blue, 3-gray,4-cyan, 5-magenta, 6-yellow
        stack[0] = impRed;
        stack[1] = impGreen;
        stack[2] = impBlue;
        stack[3] = impWhite;
        stack[4] = null;
        stack[5] = null;
        stack[6] = null;
        ImagePlus impComposite = RGBStackMerge.mergeChannels(stack, false);
        IJ.run(impComposite, "RGB Color", "");
        impComposite.setTitle("Composite");
         */
        return numChannels;
    }

    public boolean readRGB(ImagePlus imp) {
        int[] rgbArray;
        int sumR = 0;
        int sumG = 0;
        int sumB = 0;
        width = imp.getWidth();
        height = imp.getHeight();
        ByteProcessor ipR = new ByteProcessor(width, height);
        ByteProcessor ipG = new ByteProcessor(width, height);
        ByteProcessor ipB = new ByteProcessor(width, height);
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                rgbArray = imp.getPixel(i, j);
                ipR.set(i, j, rgbArray[0]);
                ipG.set(i, j, rgbArray[1]);
                ipB.set(i, j, rgbArray[2]);
                sumR = sumR + rgbArray[0];
                sumG = sumG + rgbArray[1];
                sumB = sumB + rgbArray[2];
            }
        }
        if ((sumR > 0) && (sumG == 0) && (sumB == 0)) {
            ipChannelRed = ipR;
            return true;
        } else if ((sumR == 0) && (sumG > 0) && (sumB == 0)) {
            ipChannelGreen = ipG;
            return true;
        } else if ((sumR == 0) && (sumG == 0) && (sumB > 0)) {
            ipChannelBlue = ipB;
            // Apply CLAHE (contrast limmited adaptive histogram equalization) 
            // to blue channel to correct illumination unevenness
            //IJ.showStatus("Correcting blue channel...");
            //ImagePlus img = new ImagePlus("img", ipChannelBlue);
            //CLAHE.run(img, (width + height) / 8, 256, 3);
            //ipChannelBlue = img.getProcessor();
            return true;
        } else if ((sumR > 0) && (sumG > 0) && (sumB > 0)) {
            ipChannelWhite = ipR;
            return true;
        }
        return false;
    }

    public void filterImages() {

        ipBlue = ipChannelBlue.duplicate();
        ipGreen = ipChannelGreen.duplicate();
        ipRed = ipChannelRed.duplicate();
        ipWhite = ipChannelWhite.duplicate();

        impBlue.setProcessor("Blue", ipBlue);
        impGreen.setProcessor("Green", ipGreen);
        impRed.setProcessor("Red", ipRed);
        impWhite.setProcessor("White", ipWhite);

        IJ.run(impBlue, "Gaussian Blur...", "sigma=" + blueRadius);
        IJ.run(impGreen, "Gaussian Blur...", "sigma=" + greenRadius);
        IJ.run(impRed, "Gaussian Blur...", "sigma=" + redRadius);
        IJ.run(impWhite, "Gaussian Blur...", "sigma=" + whiteRadius);

        ipBlue0 = impBlue.getProcessor();
        ipGreen0 = impGreen.getProcessor();
        ipRed0 = impRed.getProcessor();
        ipWhite0 = impWhite.getProcessor();
    }

    public void zoomExact(ImagePlus img, double mag) {
        ImageWindow win = img.getWindow();
        if (win == null) {
            return;
        }
        ImageCanvas c = win.getCanvas();
        if (c == null) {
            return;
        }
        c.setMagnification(mag);
        // see if it fits
        double w = img.getWidth() * mag;
        double h = img.getHeight() * mag;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (w > screen.width - 10) {
            w = screen.width - 10;
        }
        if (h > screen.height - 30) {
            h = screen.height - 30;
        }
        try {
            Field f_srcRect = c.getClass().getDeclaredField("srcRect");
            f_srcRect.setAccessible(true);
            f_srcRect.set(c, new Rectangle(0, 0, (int) (w / mag), (int) (h / mag)));
            c.setDrawingSize((int) w, (int) h);
            win.pack();
            c.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getMaxima() {

        ipBlueSegmented = mf.findMaxima(ipBlue0, blueProm, MaximumFinder.SEGMENTED, false);
        ipBluePoints = mf.findMaxima(ipBlue0, blueProm, MaximumFinder.SINGLE_POINTS, false);

        ipGreenPoints = mf.findMaxima(ipGreen0, greenProm, MaximumFinder.SINGLE_POINTS, false);
        ipRedPoints = mf.findMaxima(ipRed0, redProm, MaximumFinder.SINGLE_POINTS, false);
        ipWhitePoints = mf.findMaxima(ipWhite0, whiteProm, MaximumFinder.SINGLE_POINTS, false);

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (ipBlue0.get(i, j) < blueThresh) {
                    ipBluePoints.set(i, j, 0);
                }
            }
        }

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (ipGreen0.get(i, j) < greenThresh) {
                    ipGreenPoints.set(i, j, 0);
                }
            }
        }

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (ipRed0.get(i, j) < redThresh) {
                    ipRedPoints.set(i, j, 0);
                }
            }
        }
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (ipWhite0.get(i, j) < whiteThresh) {
                    ipWhitePoints.set(i, j, 0);
                }
            }
        }
    }

    public void getThresholds() {
        // get the threshold for the images
        int[] histBlue = new int[256];
        int[] histGreen = new int[256];
        int[] histRed = new int[256];
        int[] histWhite = new int[256];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                histBlue[ipBlue0.get(i, j)]++;
                histGreen[ipGreen0.get(i, j)]++;
                histRed[ipRed0.get(i, j)]++;
                histWhite[ipWhite0.get(i, j)]++;
            }
        }
        blueThresh = getInflection(histBlue);
        greenThresh = getInflection(histGreen);
        redThresh = getInflection(histRed);
        whiteThresh = getInflection(histWhite);

    }

    public int getInflection(int[] histArray) {
        //where histArray1's going down gets slower.   
        int length = histArray.length;
        int[] histArray1 = new int[length];
        int[] histArray2 = new int[length];

        for (int index = 1; index < (length - 1); index++) {
            histArray1[index] = histArray[index + 1] - histArray[index - 1];
        }
        for (int index = 2; index < (length - 2); index++) {
            histArray2[index] = histArray1[index + 1] - histArray1[index - 1];
        }
        int maxIndex = 0;
        int max = 0;
        for (int index = 0; index < length; index++) {
            if (histArray[index] > max) {
                maxIndex = index;
                max = histArray[index];
            }
        }

        if (maxIndex < 1) {
            maxIndex = 1;
        }
        int index1 = maxIndex;
        while (histArray1[index1] > 0) {
            index1++;
        }
        if (index1 < 2) {
            index1 = 2;
        }
        int index2 = index1;
        while (histArray2[index2] < 0) {
            index2++;
        }
        int startIndex = index2;
        while (!(histArray2[index2] < 0)) {
            index2++;
        }
        int endIndex = index2;

        // take the middle of positive 2nd derivative as relflection point
        return (int) Math.round((startIndex + endIndex) / 2.0);

    }

    public void getBlueMask() {
        //======================================================================
        // get all the cells from blue image
        //double corr = 3;
        double corr = 0;
        double thresh;
        double diag = Math.sqrt(width * width + height * height);
        double projection;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                ipBlueMask.set(i, j, 0);
            }
        }
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                /*
                projection = Math.sqrt(i * i + j * j)
                        * Math.cos(Math.atan2(j, i) - Math.atan2(height, width));
                if (projection > diag / 2.0) {
                    thresh = blueThresh - 2.0 * corr / diag * projection;
                } else {
                    thresh = blueThresh + corr;
                }
                 */
                thresh = blueThresh;
                if (ipBlue0.get(i, j) > thresh) {
                    ipBlueMask.set(i, j, 255);
                }
                // use the FindMaxima to further do the segmentation 
                if (ipBlueSegmented.get(i, j) == 0) {
                    ipBlueMask.set(i, j, 0);
                }
            }
        }
        //IJ.run(impBlueMask, "Watershed", "");
        ResultsTable rt = new ResultsTable();
        rt.reset();
        ipBlueMask.setThreshold(255, 255);
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);
        pa.analyze(impBlueMask);
        roiArray = impBlueMask.getOverlay().toArray();

        // Get rid of the areas that do not contain a maximum point
        ipBlueMask.setColor(Color.black);
        Roi roi;
        ImageProcessor ipRoi;
        Rectangle rect;
        int sum;
        for (int index = 0; index < roiArray.length; index++) {
            roi = roiArray[index];
            ipRoi = roi.getMask();
            rect = roi.getBounds();
            sum = 0;
            for (int j = rect.y; j < (rect.y + rect.height); j++) {
                for (int i = rect.x; i < (rect.x + rect.width); i++) {
                    if (ipRoi.get(i - rect.x, j - rect.y) > 0) {
                        sum = sum + ipBluePoints.get(i, j);
                    }
                }
            }
            if (sum == 0) {
                ipBlueMask.setRoi(roi);
                ipBlueMask.fill(roi);
            }
        }

        rt.reset();
        ipBlueMask.setThreshold(255, 255);
        pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);
        pa.analyze(impBlueMask);
        roiArray = impBlueMask.getOverlay().toArray();
        numBlue = roiArray.length;

        IJ.run(impBlueMask, "Blue", "");
        impBlueMask.setOverlay(null);
    }

    public void getRoiIndexArray() {
        roiIndexArray = new int[width][height];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                roiIndexArray[i][j] = -1;
            }
        }
        Rectangle rect;
        Roi roi;
        ImageProcessor ipRoi;
        int bX, bY, bW, bH;

        int length = roiArray.length;
        if (length > 0) {
            for (int index = 0; index < length; index++) {
                roi = roiArray[index];
                rect = roi.getBounds();
                ipRoi = roi.getMask();
                bX = rect.x;
                bY = rect.y;
                bW = rect.width;
                bH = rect.height;

                for (int jB = bY; jB < bY + bH; jB++) {
                    for (int iB = bX; iB < bX + bW; iB++) {
                        if (ipRoi.get(iB - bX, jB - bY) > 0) {
                            roiIndexArray[iB][jB] = index;
                        }
                    }
                }
            }
        }
    }

    public void getMasks() {

        roiChannelArray = new boolean[numBlue][3];
        for (int index = 0; index < numBlue; index++) {
            roiChannelArray[index][0] = false;
            roiChannelArray[index][1] = false;
            roiChannelArray[index][2] = false;
        }

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                ipGreenMask.set(i, j, 0);
                ipRedMask.set(i, j, 0);
                ipWhiteMask.set(i, j, 0);
            }
        }

        Roi roi;
        // first analyze the counted cells
        numGreen = 0;
        numRed = 0;
        numWhite = 0;
        ipGreenMask.setColor(Color.white);
        ipRedMask.setColor(Color.white);
        ipWhiteMask.setColor(Color.white);
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if ((ipGreenPoints.get(i, j) > 0) && (roiIndexArray[i][j] > -1)) {
                    numGreen++;
                    roi = roiArray[roiIndexArray[i][j]];
                    ipGreenMask.setRoi(roi);
                    ipGreenMask.fill(roi);
                    roiChannelArray[roiIndexArray[i][j]][0] = true;
                }
                if ((ipRedPoints.get(i, j) > 0) && (roiIndexArray[i][j] > -1)) {
                    numRed++;
                    roi = roiArray[roiIndexArray[i][j]];
                    ipRedMask.setRoi(roi);
                    ipRedMask.fill(roi);
                    roiChannelArray[roiIndexArray[i][j]][1] = true;
                }
                if ((ipWhitePoints.get(i, j) > 0) && (roiIndexArray[i][j] > -1)) {
                    numWhite++;
                    roi = roiArray[roiIndexArray[i][j]];
                    ipWhiteMask.setRoi(roi);
                    ipWhiteMask.fill(roi);
                    roiChannelArray[roiIndexArray[i][j]][2] = true;
                }
            }
        }

        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);
        ipGreenMask.setThreshold(255, 255);
        pa.analyze(impGreenMask);
        numGreen = rt.size();
        impGreenMask.setOverlay(null);

        rt.reset();
        pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);
        ipRedMask.setThreshold(255, 255);
        pa.analyze(impRedMask);
        numRed = rt.size();
        impRedMask.setOverlay(null);

        rt.reset();
        pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);
        ipWhiteMask.setThreshold(255, 255);
        pa.analyze(impWhiteMask);
        numWhite = rt.size();
        impWhiteMask.setOverlay(null);

        IJ.run(impGreenMask, "Green", "");
        IJ.run(impRedMask, "Red", "");
        IJ.run(impWhiteMask, "Grays", "");
    }

    public void doCrossChannels() {
        numGreenRed = 0;
        numRedWhite = 0;
        numGreenWhite = 0;
        numGreenRedWhite = 0;
        ipGreenMask.setColor(new Color(0, 127, 0));
        ipRedMask.setColor(new Color(127, 0, 0));
        ipWhiteMask.setColor(new Color(127, 127, 127));
        Roi roi;
        for (int index = 0; index < numBlue; index++) {
            if (roiChannelArray[index][0] && roiChannelArray[index][1] && (!roiChannelArray[index][2])) {
                numGreenRed++;
                roi = roiArray[index];
                ipGreenMask.setRoi(roi);
                ipGreenMask.fill(roi);
                ipRedMask.setRoi(roi);
                ipRedMask.fill(roi);
            } else if ((!roiChannelArray[index][0]) && roiChannelArray[index][1] && roiChannelArray[index][2]) {
                numRedWhite++;
                roi = roiArray[index];
                ipRedMask.setRoi(roi);
                ipRedMask.fill(roi);
                ipWhiteMask.setRoi(roi);
                ipWhiteMask.fill(roi);
            } else if (roiChannelArray[index][0] && (!roiChannelArray[index][1]) && roiChannelArray[index][2]) {
                numGreenWhite++;
                roi = roiArray[index];
                ipGreenMask.setRoi(roi);
                ipGreenMask.fill(roi);
                ipWhiteMask.setRoi(roi);
                ipWhiteMask.fill(roi);
            } else if (roiChannelArray[index][0] && (roiChannelArray[index][1]) && roiChannelArray[index][2]) {
                numGreenRedWhite++;
                roi = roiArray[index];
                ipGreenMask.setRoi(roi);
                ipGreenMask.fill(roi);
                ipRedMask.setRoi(roi);
                ipRedMask.fill(roi);
                ipWhiteMask.setRoi(roi);
                ipWhiteMask.fill(roi);
            }
        }
    }

    public void updateResult() {

        filterImages();
        displayImages();
        getMaxima();
        getBlueMask();
        getRoiIndexArray();
        getMasks();
        doCrossChannels();
        displayMasks();
    }

    public void displayImages() {
        if (!impBlue0.isVisible()) {
            impBlue0 = new ImagePlus("Blue filtered", ipBlue0);
            impBlue0.show();
        } else {
            impBlue0.setProcessor("Blue filtered", ipBlue0);
            impBlue0.updateAndDraw();
        }
        IJ.run(impBlue0, "Blue", "");
        Window window = impBlue0.getWindow();
        zoomExact(impBlue0, 0.333);
        window.setLocation(0, screen.height / 8);
        Rectangle rect = window.getBounds();

        if (!impGreen0.isVisible()) {
            impGreen0 = new ImagePlus("Green filtered", ipGreen0);

            impGreen0.show();
        } else {
            impGreen0.setProcessor("Green filtered", ipGreen0);
            impGreen0.updateAndDraw();
        }
        IJ.run(impGreen0, "Green", "");
        window = impGreen0.getWindow();
        zoomExact(impGreen0, 0.333);
        window.setLocation(rect.x + rect.width, rect.y);
        rect = window.getBounds();

        if (!impRed0.isVisible()) {
            impRed0 = new ImagePlus("Red filtered", ipRed0);

            impRed0.show();
        } else {
            impRed0.setProcessor("Red filtered", ipRed0);
            impRed0.updateAndDraw();
        }
        IJ.run(impRed0, "Red", "");
        window = impRed0.getWindow();
        zoomExact(impRed0, 0.333);
        window.setLocation(rect.x + rect.width, rect.y);
        rect = window.getBounds();

        if (!impWhite0.isVisible()) {
            impWhite0 = new ImagePlus("White filtered", ipWhite0);
            impWhite0.show();
        } else {
            impWhite0.setProcessor("White filtered", ipWhite0);
            impWhite0.updateAndDraw();
        }
        IJ.run(impWhite0, "Grays", "");
        window = impWhite0.getWindow();
        zoomExact(impWhite0, 0.333);
        window.setLocation(rect.x + rect.width, rect.y);
    }

    public void displayMasks() {
        Window window = impBlue0.getWindow();
        Rectangle rect = window.getBounds();

        if (!impBlueMask0.isVisible()) {
            impBlueMask0 = new ImagePlus("Blue mask", ipBlueMask);
            impBlueMask0.show();
        } else {
            impBlueMask0.setProcessor("Blue mask", ipBlueMask);
            impBlueMask0.updateAndDraw();
        }
        window = impBlueMask0.getWindow();
        zoomExact(impBlueMask0, 0.333);
        window.setLocation(rect.x, rect.y + rect.height);
        rect = window.getBounds();

        if (!impGreenMask0.isVisible()) {
            impGreenMask0 = new ImagePlus("Green mask", ipGreenMask);
            impGreenMask0.show();
        } else {
            impGreenMask0.setProcessor("Green mask", ipGreenMask);
            impGreenMask0.updateAndDraw();
        }
        window = impGreenMask0.getWindow();
        zoomExact(impGreenMask0, 0.333);
        window.setLocation(rect.x + rect.width, rect.y);
        rect = window.getBounds();

        if (!impRedMask0.isVisible()) {
            impRedMask0 = new ImagePlus("Red mask", ipRedMask);
            impRedMask0.show();
        } else {
            impRedMask0.setProcessor("Red mask", ipRedMask);
            impRedMask0.updateAndDraw();
        }
        window = impRedMask0.getWindow();
        zoomExact(impRedMask0, 0.333);
        window.setLocation(rect.x + rect.width, rect.y);
        rect = window.getBounds();

        if (!impWhiteMask0.isVisible()) {
            impWhiteMask0 = new ImagePlus("White mask", ipWhiteMask);
            impWhiteMask0.show();
        } else {
            impWhiteMask0.setProcessor("White mask", ipWhiteMask);
            impWhiteMask0.updateAndDraw();
        }
        window = impWhiteMask0.getWindow();
        zoomExact(impWhiteMask0, 0.333);
        window.setLocation(rect.x + rect.width, rect.y);
    }

    public void loadImageFolder() {

        imageFolder = IJ.getDirectory("Select image folder...");
        if (imageFolder == null) {
            return;
        }
        int index = imageFolder.lastIndexOf(File.separator);
        String folderName = imageFolder.substring(0, index);
        index = folderName.lastIndexOf(File.separator);
        folderName = folderName.substring(index + 1, folderName.length());
        folderLabel.setText(folderName);

        // Read the 4 images
        if (readImages() < 4) {
            IJ.log("Does not have (all) images");
            return;
        }

        mf = new MaximumFinder();

        filterImages();
        getThresholds();
        updateFrame();
        displayImages();
        getMaxima();
        getBlueMask();
        getRoiIndexArray();
        getMasks();
        doCrossChannels();
        displayMasks();
        showResult();
    }

    public void doBatchCount() {
        int index = imageFolder.lastIndexOf(File.separator);
        String parentFolder = imageFolder.substring(0, index);
        index = parentFolder.lastIndexOf(File.separator);
        parentFolder = parentFolder.substring(0, index + 1);
        String[] parentList = (new File(parentFolder)).list();
        String folderName;

        if (holdCheckbox.getState()) {
            IJ.log(String.format("%-60s\t%-12s\t%-12s\t%-12s\t%-12s\t",
                    "Channel",
                    "Blue",
                    "Green",
                    "Red",
                    "White"));
            IJ.log(String.format("%-60s\t%-12d\t%-12d\t%-12d\t%-12d\t",
                    "Gaussian radius",
                    blueRadius,
                    greenRadius,
                    redRadius,
                    whiteRadius));
            IJ.log(String.format("%-60s\t%-12d\t%-12d\t%-12d\t%-12d\t",
                    "Prominance",
                    blueProm,
                    greenProm,
                    redProm,
                    whiteProm));
            IJ.log(String.format("%-60s\t%-12d\t%-12d\t%-12d\t%-12d\t",
                    "Threshold",
                    blueThresh,
                    greenThresh,
                    redThresh,
                    whiteThresh));
            IJ.log(String.format("%-60s\t%-12s\t%-12s\t%-12s\t%-12s\t%-12s\t%-12s\t%-12s\t%-12s\t",
                    "Channel(s)",
                    "Blue",
                    "Green",
                    "Red",
                    "White",
                    "GR",
                    "RW",
                    "GW",
                    "GRW"));
        }
        for (int i = 0; i < parentList.length; i++) {
            if (!boolBatchHandler) {
                frame.dispose();
                impBlue0.changes = false;
                impGreen0.changes = false;
                impRed0.changes = false;
                impWhite0.changes = false;
                impBlueMask0.changes = false;
                impGreenMask0.changes = false;
                impRedMask0.changes = false;
                impWhiteMask0.changes = false;
                impBlue0.close();
                impGreen0.close();
                impRed0.close();
                impWhite0.close();
                impBlueMask0.close();
                impGreenMask0.close();
                impRedMask0.close();
                impWhiteMask0.close();
                break;
            }
            folderName = parentList[i];
            if ((new File(parentFolder + folderName)).isDirectory()) {
                imageFolder = parentFolder + folderName + File.separator;
                if (readImages() > 3) {
                    folderLabel.setText(folderName);
                    filterImages();
                    if (!holdCheckbox.getState()) {
                        getThresholds();
                        updateFrame();
                    }
                    updateResult();
                    if (holdCheckbox.getState()) {
                        IJ.log(String.format("%-50s\t%-12d\t%-12d\t%-12d\t%-12d\t%-12d\t%-12d\t%-12d\t%-12d\t",
                                folderName,
                                numBlue,
                                numGreen,
                                numRed,
                                numWhite,
                                numGreenRed,
                                numRedWhite,
                                numGreenWhite,
                                numGreenRedWhite));
                    } else {
                        showResult();
                    }
                }

            }
        }

        if (holdCheckbox.getState()) {
            IJ.log("");
        }
    }

    public void showFrame() {

        frame = new Frame("Auto Cell Counter");
        frame.setSize(screen.width / 4, screen.height / 3);
        frame.setLayout(new BorderLayout());

        Color color = UIManager.getColor("Panel.background");
        Panel folderPanel = new Panel(new BorderLayout());
        folderPanel.setBackground(color);
        Button folderButton = new Button("ImageSet");
        folderButton.addActionListener(this);
        int index = imageFolder.lastIndexOf(File.separator);
        String folderName = imageFolder.substring(0, index);
        index = folderName.lastIndexOf(File.separator);
        folderName = folderName.substring(index + 1, folderName.length());
        folderLabel = new Label(folderName);
        folderPanel.add(new Label(), "North");
        folderPanel.add(folderButton, "West");
        folderPanel.add(folderLabel, "Center");
        folderPanel.add(new Label(), "South");

        blueRadiusField = new TextField(Integer.toString(blueRadius));
        greenRadiusField = new TextField(Integer.toString(greenRadius));
        redRadiusField = new TextField(Integer.toString(redRadius));
        whiteRadiusField = new TextField(Integer.toString(whiteRadius));

        bluePromField = new TextField(Integer.toString(blueProm));
        greenPromField = new TextField(Integer.toString(greenProm));
        redPromField = new TextField(Integer.toString(redProm));
        whitePromField = new TextField(Integer.toString(whiteProm));
        bluePromField.addTextListener(this);
        greenPromField.addTextListener(this);
        redPromField.addTextListener(this);
        whitePromField.addTextListener(this);

        blueThreshField = new TextField(Integer.toString(blueThresh));
        greenThreshField = new TextField(Integer.toString(greenThresh));
        redThreshField = new TextField(Integer.toString(redThresh));
        whiteThreshField = new TextField(Integer.toString(whiteThresh));

        Panel paramPanel = new Panel(new GridLayout(6, 4));
        paramPanel.add(new Label("Blue"));
        paramPanel.add(new Label("Green"));
        paramPanel.add(new Label("Red"));
        paramPanel.add(new Label("White"));
        paramPanel.add(blueRadiusField);
        paramPanel.add(greenRadiusField);
        paramPanel.add(redRadiusField);
        paramPanel.add(whiteRadiusField);

        paramPanel.add(new Label());
        paramPanel.add(new Label());
        paramPanel.add(new Label());
        paramPanel.add(new Label());
        paramPanel.add(bluePromField);
        paramPanel.add(greenPromField);
        paramPanel.add(redPromField);
        paramPanel.add(whitePromField);
        paramPanel.add(new Label());
        paramPanel.add(new Label());
        paramPanel.add(new Label());
        paramPanel.add(new Label());
        paramPanel.add(blueThreshField);
        paramPanel.add(greenThreshField);
        paramPanel.add(redThreshField);
        paramPanel.add(whiteThreshField);

        Panel labelPanel = new Panel(new GridLayout(6, 1));
        labelPanel.add(new Label());
        labelPanel.add(new Label("Gaussian radius:"));
        labelPanel.add(new Label());
        labelPanel.add(new Label("Prominence:"));
        labelPanel.add(new Label());
        labelPanel.add(new Label("Threshold:"));

        Panel buttonPanel = new Panel(new GridLayout(6, 1));
        buttonPanel.add(new Label());
        Button gaussianButton = new Button("Smooth");
        gaussianButton.addActionListener(this);
        buttonPanel.add(gaussianButton);
        buttonPanel.add(new Label());
        peakButton = new Button("View Peaks");
        peakButton.addActionListener(this);
        buttonPanel.add(peakButton);
        buttonPanel.add(new Label());
        Button resetButton = new Button("Reset");
        resetButton.addActionListener(this);
        buttonPanel.add(resetButton);

        Button updateButton = new Button("Update");
        updateButton.addActionListener(this);

        Panel panel = new Panel(new BorderLayout());
        panel.setBackground(color);
        panel.add(paramPanel, "Center");
        panel.add(labelPanel, "West");
        panel.add(buttonPanel, "East");
        panel.add(updateButton, "South");

        Panel allPanel = new Panel(new GridLayout(4, 3));
        allPanel.setBackground(color);
        allPanel.add(new Label());
        allPanel.add(new Label());
        allPanel.add(new Label());
        batchCheckbox = new Checkbox("Batch");
        allPanel.add(batchCheckbox);
        allPanel.add(new Label());
        allPanel.add(new Label());
        holdCheckbox = new Checkbox("Hold");
        allPanel.add(holdCheckbox);
        allPanel.add(new Label());
        allPanel.add(new Label());
        allPanel.add(new Label());
        Button okButton = new Button("OK");
        okButton.addActionListener(this);
        allPanel.add(okButton);
        Button cancelButton = new Button("Cancel");
        cancelButton.addActionListener(this);
        allPanel.add(cancelButton);
        frame.add(folderPanel, "North");
        frame.add(panel, "Center");

        frame.add(allPanel, "South");

        //frame.pack();
        frame.setVisible(true);
        Rectangle rect = frame.getBounds();
        frame.setLocation((screen.width - 1) - rect.width, screen.height / 6 - 1);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                frame.dispose();
                impBlue0.changes = false;
                impGreen0.changes = false;
                impRed0.changes = false;
                impWhite0.changes = false;
                impBlueMask0.changes = false;
                impGreenMask0.changes = false;
                impRedMask0.changes = false;
                impWhiteMask0.changes = false;
                impBlueMask0.close();
                impBlue0.close();
                impGreen0.close();
                impRed0.close();
                impWhite0.close();
                impBlueMask0.close();
                impGreenMask0.close();
                impRedMask0.close();
                impWhiteMask0.close();
            }
        });
    }

    public void updateFrame() {
        blueThreshField.setText(Integer.toString(blueThresh));
        greenThreshField.setText(Integer.toString(greenThresh));
        redThreshField.setText(Integer.toString(redThresh));
        whiteThreshField.setText(Integer.toString(whiteThresh));
    }

    public void showResult() {
        String str = imageFolder.substring(0, imageFolder.lastIndexOf(File.separator));
        String folderName = imageFolder.substring(str.lastIndexOf(File.separator) + 1, imageFolder.lastIndexOf(File.separator));
        IJ.log(String.format("%-60s\t%-10s\t%-10s\t%-10s\t%-10s\t",
                "Channel",
                "Blue",
                "Green",
                "Red",
                "White"));
        IJ.log(String.format("%-60s\t%-10d\t%-10d\t%-10d\t%-10d\t",
                "Gaussian radius",
                blueRadius,
                greenRadius,
                redRadius,
                whiteRadius));
        IJ.log(String.format("%-60s\t%-10d\t%-10d\t%-10d\t%-10d\t",
                "Prominence",
                blueProm,
                greenProm,
                redProm,
                whiteProm));
        IJ.log(String.format("%-60s\t%-10d\t%-10d\t%-10d\t%-10d\t",
                "Threshold",
                blueThresh,
                greenThresh,
                redThresh,
                whiteThresh));

        IJ.log(String.format("%-60s\t%-10s\t%-10s\t%-10s\t%-10s\t%-10s\t%-10s\t%-10s\t%-10s\t",
                "Channel(s)",
                "Blue",
                "Green",
                "Red",
                "White",
                "GR",
                "RW",
                "GW",
                "GRW"));
        IJ.log(String.format("%-50s\t%-10d\t%-10d\t%-10d\t%-10d\t%-10d\t%-10d\t%-10d\t%-10d\t",
                folderName,
                numBlue,
                numGreen,
                numRed,
                numWhite,
                numGreenRed,
                numRedWhite,
                numGreenWhite,
                numGreenRedWhite));
        IJ.log("");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if (label.equals("ImageSet")) {
            loadImageFolder();
        } else if (label.equals("Smooth")) {
            blueRadius = Math.round(Float.parseFloat(blueRadiusField.getText()));
            greenRadius = Math.round(Float.parseFloat(greenRadiusField.getText()));
            redRadius = Math.round(Float.parseFloat(redRadiusField.getText()));
            whiteRadius = Math.round(Float.parseFloat(whiteRadiusField.getText()));
            filterImages();
            displayImages();
            IJ.run(impBlue0, "Select None", "");
            IJ.run(impGreen0, "Select None", "");
            IJ.run(impRed0, "Select None", "");
            IJ.run(impWhite0, "Select None", "");
            peakButton.setLabel("View Peaks");
        } else if (label.equals("View Peaks")) {
            blueRadius = Math.round(Float.parseFloat(blueRadiusField.getText()));
            greenRadius = Math.round(Float.parseFloat(greenRadiusField.getText()));
            redRadius = Math.round(Float.parseFloat(redRadiusField.getText()));
            whiteRadius = Math.round(Float.parseFloat(whiteRadiusField.getText()));
            filterImages();
            displayImages();
            blueProm = Math.round(Float.parseFloat(bluePromField.getText()));
            greenProm = Math.round(Float.parseFloat(greenPromField.getText()));
            redProm = Math.round(Float.parseFloat(redPromField.getText()));
            whiteProm = Math.round(Float.parseFloat(whitePromField.getText()));
            IJ.run(impBlue0, "Find Maxima...", "prominence=" + blueProm + " output=[Point Selection]");
            IJ.run(impGreen0, "Find Maxima...", "prominence=" + greenProm + " output=[Point Selection]");
            IJ.run(impRed0, "Find Maxima...", "prominence=" + redProm + " output=[Point Selection]");
            IJ.run(impWhite0, "Find Maxima...", "prominence=" + whiteProm + " output=[Point Selection]");
            peakButton.setLabel("Hide Peaks");
        } else if (label.equals("Hide Peaks")) {
            blueRadius = Math.round(Float.parseFloat(blueRadiusField.getText()));
            greenRadius = Math.round(Float.parseFloat(greenRadiusField.getText()));
            redRadius = Math.round(Float.parseFloat(redRadiusField.getText()));
            whiteRadius = Math.round(Float.parseFloat(whiteRadiusField.getText()));
            filterImages();
            displayImages();
            IJ.run(impBlue0, "Select None", "");
            IJ.run(impGreen0, "Select None", "");
            IJ.run(impRed0, "Select None", "");
            IJ.run(impWhite0, "Select None", "");
            peakButton.setLabel("View Peaks");
        } else if (label.equals("Reset")) {
            filterImages();
            displayImages();
            getThresholds();
            blueThreshField.setText(Integer.toString(blueThresh));
            greenThreshField.setText(Integer.toString(greenThresh));
            redThreshField.setText(Integer.toString(redThresh));
            whiteThreshField.setText(Integer.toString(whiteThresh));
        } else if (label.equals("Update")) {
            blueRadius = Math.round(Float.parseFloat(blueRadiusField.getText()));
            blueProm = Math.round(Float.parseFloat(bluePromField.getText()));
            blueThresh = Math.round(Float.parseFloat(blueThreshField.getText()));
            greenRadius = Math.round(Float.parseFloat(greenRadiusField.getText()));
            greenProm = Math.round(Float.parseFloat(greenPromField.getText()));
            greenThresh = Math.round(Float.parseFloat(greenThreshField.getText()));
            redRadius = Math.round(Float.parseFloat(redRadiusField.getText()));
            redProm = Math.round(Float.parseFloat(redPromField.getText()));
            redThresh = Math.round(Float.parseFloat(redThreshField.getText()));
            whiteRadius = Math.round(Float.parseFloat(whiteRadiusField.getText()));
            whiteProm = Math.round(Float.parseFloat(whitePromField.getText()));
            whiteThresh = Math.round(Float.parseFloat(whiteThreshField.getText()));
            updateResult();
            showResult();
        } else if (label.equals("OK")) {
            blueRadius = Math.round(Float.parseFloat(blueRadiusField.getText()));
            blueProm = Math.round(Float.parseFloat(bluePromField.getText()));
            blueThresh = Math.round(Float.parseFloat(blueThreshField.getText()));
            greenRadius = Math.round(Float.parseFloat(greenRadiusField.getText()));
            greenProm = Math.round(Float.parseFloat(greenPromField.getText()));
            greenThresh = Math.round(Float.parseFloat(greenThreshField.getText()));
            redRadius = Math.round(Float.parseFloat(redRadiusField.getText()));
            redProm = Math.round(Float.parseFloat(redPromField.getText()));
            redThresh = Math.round(Float.parseFloat(redThreshField.getText()));
            whiteRadius = Math.round(Float.parseFloat(whiteRadiusField.getText()));
            whiteProm = Math.round(Float.parseFloat(whitePromField.getText()));
            whiteThresh = Math.round(Float.parseFloat(whiteThreshField.getText()));
            if (batchCheckbox.getState()) {
                batchHandler = new BatchHandler();
                batchHandler.start();
            } else {
                frame.dispose();
                impBlue0.changes = false;
                impGreen0.changes = false;
                impRed0.changes = false;
                impWhite0.changes = false;
                impBlueMask0.changes = false;
                impGreenMask0.changes = false;
                impRedMask0.changes = false;
                impWhiteMask0.changes = false;
                impBlue0.close();
                impGreen0.close();
                impRed0.close();
                impWhite0.close();
                impBlueMask0.close();
                impGreenMask0.close();
                impRedMask0.close();
                impWhiteMask0.close();
            }
        } else if (label.equals("Cancel")) {
            if (batchHandler != null) {
                boolBatchHandler = false;
            } else {
                frame.dispose();
                impBlue0.changes = false;
                impGreen0.changes = false;
                impRed0.changes = false;
                impWhite0.changes = false;
                impBlueMask0.changes = false;
                impGreenMask0.changes = false;
                impRedMask0.changes = false;
                impWhiteMask0.changes = false;
                impBlue0.close();
                impGreen0.close();
                impRed0.close();
                impWhite0.close();
                impBlueMask0.close();
                impGreenMask0.close();
                impRedMask0.close();
                impWhiteMask0.close();
            }
        }
    }

    @Override
    public void textValueChanged(TextEvent e) {
        blueRadius = Math.round(Float.parseFloat(blueRadiusField.getText()));
        greenRadius = Math.round(Float.parseFloat(greenRadiusField.getText()));
        redRadius = Math.round(Float.parseFloat(redRadiusField.getText()));
        whiteRadius = Math.round(Float.parseFloat(whiteRadiusField.getText()));
        IJ.run(impBlue0, "Select None", "");
        IJ.run(impGreen0, "Select None", "");
        IJ.run(impRed0, "Select None", "");
        IJ.run(impWhite0, "Select None", "");
        peakButton.setLabel("View Peaks");
    }

    class BatchHandler extends Thread {

        @Override
        public void run() {
            boolBatchHandler = true;
            doBatchCount();
        }
    }

}
