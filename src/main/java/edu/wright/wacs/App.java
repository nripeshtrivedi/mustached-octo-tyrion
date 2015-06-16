package edu.wright.wacs;

import java.awt.BorderLayout;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.GCP;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;
import org.openimaj.feature.local.matcher.LocalFeatureMatcher;
import org.openimaj.feature.local.matcher.MatchingUtilities;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.feature.local.matcher.consistent.LocalConsistentKeypointMatcher;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.engine.ipd.FinderMode;
import org.openimaj.image.feature.local.engine.ipd.IPDSIFTEngine;
import org.openimaj.image.feature.local.interest.HarrisIPD;
import org.openimaj.image.feature.local.interest.IPDSelectionMode;
import org.openimaj.image.feature.local.interest.InterestPointData;
import org.openimaj.image.feature.local.keypoints.InterestPointKeypoint;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.feature.local.keypoints.KeypointVisualizer;
import org.openimaj.image.processing.background.BasicBackgroundSubtract;
import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.model.fit.RANSAC;

import Jama.Matrix;

public class App extends JFrame implements ActionListener {

	BufferedImage image = null;
	JLabel canvas = null;
	JButton load = null;

	static {
		System.out.println("GDAL init...");
		gdal.AllRegister();
		int count = gdal.GetDriverCount();
		System.out.println(count + " available Drivers");
		for (int i = 0; i < count; i++) {
			try {
				Driver driver = gdal.GetDriver(i);
				System.out.println(" " + driver.getShortName() + " : "
						+ driver.getLongName());
			} catch (Exception e) {
				System.err.println("Error loading driver " + i);
			}
		}
	}

	public App() {
		load = new JButton("Load Image");
		load.addActionListener(this);

		// canvas = new JLabel();
		// canvas.setSize(1024, 768);
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(load, BorderLayout.NORTH);
		// this.getContentPane().add(canvas, BorderLayout.SOUTH);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		this.setSize(300, 200);
		this.show();
	}

	// public void setImage(BufferedImage image) {
	// ImageIcon icon = new ImageIcon(image);

	// if(this.canvas != null) {
	// canvas.setIcon(icon);
	// }
	// }

	public BufferedImage openFile(File f) {
		Dataset poDataset = null;
		try {
			poDataset = (Dataset) gdal.Open(f.getAbsolutePath(),
					gdalconst.GA_ReadOnly);
			if (poDataset == null) {
				System.out.println("The image could not be read.");
				printLastError();
				return null;
			}
		} catch (Exception e) {
			System.err.println("Exception caught.");
			System.err.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
		double[] adfGeoTransform = new double[6];

		System.out.println("Driver: " + poDataset.GetDriver().GetDescription());

		System.out.println("Size is: " + poDataset.getRasterXSize() + "x"
				+ poDataset.getRasterYSize() + "  bands:"
				+ poDataset.getRasterCount());

		if (poDataset.GetProjectionRef() != null)
			System.out.println("Projection is `" + poDataset.GetProjectionRef()
					+ "'");

		Hashtable dict = poDataset.GetMetadata_Dict("");
		Enumeration keys = dict.keys();
		System.out.println(dict.size()
				+ " items of metadata found (via Hashtable dict):");
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			System.out.println(" :" + key + ":==:" + dict.get(key) + ":");
		}

		Vector list = poDataset.GetMetadata_List("");
		Enumeration enumerate = list.elements();
		System.out.println(list.size()
				+ " items of metadata found (via Vector list):");
		while (enumerate.hasMoreElements()) {
			String s = (String) enumerate.nextElement();
			System.out.println(" " + s);
		}

		Vector GCPs = new Vector();
		poDataset.GetGCPs(GCPs);
		System.out.println("Got " + GCPs.size() + " GCPs");
		Enumeration e = GCPs.elements();
		while (e.hasMoreElements()) {
			GCP gcp = (GCP) e.nextElement();
			System.out.println(" x:" + gcp.getGCPX() + " y:" + gcp.getGCPY()
					+ " z:" + gcp.getGCPZ() + " pixel:" + gcp.getGCPPixel()
					+ " line:" + gcp.getGCPLine() + " line:" + gcp.getInfo());
		}

		poDataset.GetGeoTransform(adfGeoTransform);
		{
			System.out.println("Origin = (" + adfGeoTransform[0] + ", "
					+ adfGeoTransform[3] + ")");

			System.out.println("Pixel Size = (" + adfGeoTransform[1] + ", "
					+ adfGeoTransform[5] + ")");
		}

		Band poBand = null;
		double[] adfMinMax = new double[2];
		Double[] max = new Double[1];
		Double[] min = new Double[1];

		int bandCount = poDataset.getRasterCount();
		ByteBuffer[] bands = new ByteBuffer[bandCount];
		int[] banks = new int[bandCount];
		int[] offsets = new int[bandCount];

		int xsize = 2000;// 6297;//poDataset.getRasterXSize();
		int ysize = 2000;// 5529;//poDataset.getRasterYSize();
		int pixels = xsize * ysize;
		int buf_type = 0, buf_size = 0;

		for (int band = 0; band < bandCount; band++) {
			/* Bands are not 0-base indexed, so we must add 1 */
			poBand = poDataset.GetRasterBand(band + 1);

			buf_type = poBand.getDataType();
			buf_size = pixels * gdal.GetDataTypeSize(buf_type) / 8;

			System.out.println(" Data Type = "
					+ gdal.GetDataTypeName(poBand.getDataType()));
			System.out.println(" ColorInterp = "
					+ gdal.GetColorInterpretationName(poBand
							.GetRasterColorInterpretation()));

			System.out.println("Band size is: " + poBand.getXSize() + "x"
					+ poBand.getYSize());

			poBand.GetMinimum(min);
			poBand.GetMaximum(max);
			if (min[0] != null || max[0] != null) {
				System.out.println("  Min=" + min[0] + " Max=" + max[0]);
			} else {
				System.out.println("  No Min/Max values stored in raster.");
			}

			if (poBand.GetOverviewCount() > 0) {
				System.out.println("Band has " + poBand.GetOverviewCount()
						+ " overviews.");
			}

			if (poBand.GetRasterColorTable() != null) {
				System.out
						.println("Band has a color table with "
								+ poBand.GetRasterColorTable().GetCount()
								+ " entries.");
				for (int i = 0; i < poBand.GetRasterColorTable().GetCount(); i++) {
					System.out.println(" " + i + ": "
							+ poBand.GetRasterColorTable().GetColorEntry(i));
				}
			}

			System.out.println("Allocating ByteBuffer of size: " + buf_size);

			ByteBuffer data = ByteBuffer.allocateDirect(buf_size);
			data.order(ByteOrder.nativeOrder());

			int returnVal = 0;
			try {
				int thexsize = poBand.getXSize();
				int theysize = poBand.getYSize();
				returnVal = poBand.ReadRaster_Direct(0, 0, thexsize, theysize,
						xsize, ysize, buf_type, data);
			} catch (Exception ex) {
				System.err.println("Could not read raster data.");
				System.err.println(ex.getMessage());
				ex.printStackTrace();
				return null;
			}
			if (returnVal == gdalconstConstants.CE_None) {
				bands[band] = data;
			} else {
				printLastError();
			}
			banks[band] = band;
			offsets[band] = 0;
		}

		DataBuffer imgBuffer = null;
		SampleModel sampleModel = null;
		int data_type = 0, buffer_type = 0;

		if (buf_type == gdalconstConstants.GDT_Byte) {
			byte[][] bytes = new byte[bandCount][];
			for (int i = 0; i < bandCount; i++) {
				bytes[i] = new byte[pixels];
				bands[i].get(bytes[i]);
			}
			imgBuffer = new DataBufferByte(bytes, pixels);
			buffer_type = DataBuffer.TYPE_BYTE;
			sampleModel = new BandedSampleModel(buffer_type, xsize, ysize,
					xsize, banks, offsets);
			data_type = (poBand.GetRasterColorInterpretation() == gdalconstConstants.GCI_PaletteIndex) ? BufferedImage.TYPE_BYTE_INDEXED
					: BufferedImage.TYPE_BYTE_GRAY;
		} else if (buf_type == gdalconstConstants.GDT_Int16) {
			short[][] shorts = new short[bandCount][];
			for (int i = 0; i < bandCount; i++) {
				shorts[i] = new short[pixels];
				bands[i].asShortBuffer().get(shorts[i]);
			}
			imgBuffer = new DataBufferShort(shorts, pixels);
			buffer_type = DataBuffer.TYPE_USHORT;
			sampleModel = new BandedSampleModel(buffer_type, xsize, ysize,
					xsize, banks, offsets);
			data_type = BufferedImage.TYPE_USHORT_GRAY;
		} else if (buf_type == gdalconstConstants.GDT_Int32) {
			int[][] ints = new int[bandCount][];
			for (int i = 0; i < bandCount; i++) {
				ints[i] = new int[pixels];
				bands[i].asIntBuffer().get(ints[i]);
			}
			imgBuffer = new DataBufferInt(ints, pixels);
			buffer_type = DataBuffer.TYPE_INT;
			sampleModel = new BandedSampleModel(buffer_type, xsize, ysize,
					xsize, banks, offsets);
			data_type = BufferedImage.TYPE_CUSTOM;
		}

		WritableRaster raster = Raster.createWritableRaster(sampleModel,
				imgBuffer, null);
		BufferedImage img = null;
		ColorModel cm = null;

		if (poBand.GetRasterColorInterpretation() == gdalconstConstants.GCI_PaletteIndex) {
			data_type = BufferedImage.TYPE_BYTE_INDEXED;
			cm = poBand.GetRasterColorTable().getIndexColorModel(
					gdal.GetDataTypeSize(buf_type));
			img = new BufferedImage(cm, raster, false, null);
		} else {
			ColorSpace cs = null;
			if (bandCount > 2) {
				cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
				cm = new ComponentColorModel(cs, false, false,
						ColorModel.OPAQUE, buffer_type);
				img = new BufferedImage(cm, raster, true, null);
			} else {
				img = new BufferedImage(xsize, ysize, data_type);
				img.setData(raster);
			}
		}
		return img;
	}

	public void printLastError() {
		System.out.println("Last error: " + gdal.GetLastErrorMsg());
		System.out.println("Last error no: " + gdal.GetLastErrorNo());
		System.out.println("Last error type: " + gdal.GetLastErrorType());
	}

	public IPDSIFTEngine engine() {
		HarrisIPD hIPD = new HarrisIPD(1.4f);
		hIPD.setImageBlurred(false);
		// AffineAdaption affineIPD = new AffineAdaption(harrisIPD,new
		// IPDSelectionMode.Threshold(10000f));
		IPDSIFTEngine engine = new IPDSIFTEngine(hIPD);
		engine.setSelectionMode(new IPDSelectionMode.Threshold(1000f));
		engine.setAcrossScales(true);
		engine.setFinderMode(new FinderMode.Characteristic<InterestPointData>());
		return engine;
	}

	public BufferedImage subtract(File files[] ) {
		MBFImage query = ImageUtilities.createMBFImage(openFile(files[0]), false);
		MBFImage target = ImageUtilities.createMBFImage(openFile(files[1]), false);
		MBFImage sub = ImageUtilities.createMBFImage(openFile(files[2]), false);
		
		DisplayUtilities.display("query", query);
		DisplayUtilities.display("target", target);

		/*
		 * BasicBackgroundSubtract<MBFImage> test = new
		 * BasicBackgroundSubtract<MBFImage>(target); test.processImage(query);
		 * DisplayUtilities.display(query);
		 */

		//
		// IPDSIFTEngine engine = engine();
		// LocalFeatureList<InterestPointKeypoint<InterestPointData>>
		// queryKeypoints = engine.findFeatures(query.flatten());
		// LocalFeatureList<InterestPointKeypoint<InterestPointData>>
		// targetKeypoints = engine.findFeatures(target.flatten());
		//

		DoGSIFTEngine engine = new DoGSIFTEngine();
		LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(query
				.flatten());
		LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(target
				.flatten());
		LocalFeatureList<Keypoint> subKeypoints = engine.findFeatures(sub
				.flatten());

		// System.out.println(queryKeypoints);

		// MatchingUtilities.drawMatches(query, queryKeypoints, RGBColour.RED);

		/*
		 * 
		 * RobustAffineTransformEstimator modelFitter = new
		 * RobustAffineTransformEstimator(1.0, 1500,new
		 * RANSAC.PercentageInliersStoppingCondition(0.5));
		 * LocalConsistentKeypointMatcher
		 * <InterestPointKeypoint<InterestPointData>> matcher = new
		 * LocalConsistentKeypointMatcher
		 * <InterestPointKeypoint<InterestPointData>>(8);
		 * 
		 * matcher.setFittingModel(modelFitter);
		 */
		RobustAffineTransformEstimator modelFitter = new RobustAffineTransformEstimator(
				5.0, 200, new RANSAC.PercentageInliersStoppingCondition(0.5));
		LocalFeatureMatcher<Keypoint> matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(
				new FastBasicKeypointMatcher<Keypoint>(8), modelFitter);

		matcher.setModelFeatures(queryKeypoints);
		matcher.findMatches(targetKeypoints);
		
		RobustAffineTransformEstimator submodelFitter = new RobustAffineTransformEstimator(
				5.0, 200, new RANSAC.PercentageInliersStoppingCondition(0.5));
		LocalFeatureMatcher<Keypoint> submatcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(
				new FastBasicKeypointMatcher<Keypoint>(8), modelFitter);

		submatcher.setModelFeatures(subKeypoints);
		submatcher.findMatches(targetKeypoints);

		MBFImage consistentMatches = MatchingUtilities.drawMatches(query,
				target, matcher.getMatches(), RGBColour.RED);

		MBFImage query2 = query.transform(modelFitter.getModel().getTransform()
				.inverse());
		MBFImage subimg = sub.transform(submodelFitter.getModel().getTransform().inverse());
		
		
		

		int hei = Math.max(target.getHeight(), query2.getHeight());
		int wth = Math.max(target.getWidth(), query2.getWidth());
		System.out.println(hei + " " + wth);

		DisplayUtilities.display("transformed query", query2);

		System.out.println("subtracting...");
		System.out.println(target.getHeight() + " " + target.getWidth());

		MBFImage target2 = target.paddingSymmetric(0,
				Math.abs(wth - target.getWidth()), 0,
				Math.abs(hei - target.getHeight()));

		System.out.println(target2.getHeight() + " " + target2.getWidth());
		DisplayUtilities.display(target2);
		System.out.println(query2.getHeight() + " " + query2.getWidth());

		query2 = query2.paddingSymmetric(0, Math.abs(wth - query2.getWidth()),
				0, Math.abs(hei - query2.getHeight()));
		DisplayUtilities.display(query2);
		System.out.println(query2.getHeight() + " " + query2.getWidth());

		System.out.println("display subtraction");

		DisplayUtilities.display("Keypoint Matches", consistentMatches);

		DisplayUtilities.display(ImageUtilities
				.createBufferedImageForDisplay(target2.subtract(query2).abs()));

		// */
		return null;// (ImageUtilities.createBufferedImageForDisplay(target));

	}

	public void actionPerformed(ActionEvent arg0) {
		System.out.println("Loading file chooser...");
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			/* open the image! */
			File files[] = chooser.getSelectedFiles();
			if (files.length >= 2) {
				subtract(files);
				// DisplayUtilities.display();
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		App test = new App();
		if (args.length >= 1) {
			BufferedImage tmpImage = test.openFile(new File(args[0]));
			DisplayUtilities.display(tmpImage);
		}
	}

}
