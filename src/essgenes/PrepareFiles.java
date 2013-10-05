package essgenes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import GUI.MainFrame;

import com.google.code.externalsorting.ExternalSort;

public class PrepareFiles {

	private static Logger logger = Logger.getLogger(PrepareFiles.class.getName());

	public static void processSelectedScaffold(String imgFilePath, String scaffoldName, ProjectInfo info) throws IOException{
		
		File genesFile = new File(info.getPath() + scaffoldName + ".genes");	
		BufferedWriter bw = new BufferedWriter(new FileWriter(genesFile));
		
		bw.write("\t\t\t\t\t\t\t\n");
		bw.write("\t\t\t\t\t\t\t\n");
		bw.write("\t\t\t\t\t\t\t\n");
		bw.write("\t\t\t\t\t\t\t\n");
		
		String start_coord = "start_coord";
		String end_coord = "end_coord";
		String strand = "strand";
		String length = "length";
		String locus_type = "locus_type";
		String locus_tag = "locus_tag";
		String gene_symbol = "gene_symbol";
		String description = "description";
		
		String toBeWritten = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", start_coord, end_coord, strand, length, locus_type, locus_tag, gene_symbol, description);
		bw.write(toBeWritten);
		
		List<List<ImgFileInfo>> wholeImg = extractScaffolds(imgFilePath, info, null);
		
		for (List<ImgFileInfo> tempArray : wholeImg){
			for (ImgFileInfo t : tempArray){
				
				if(t.source.compareTo("Locus_type") == 0){
					locus_type = t.gene_information;
				}
				
				if(t.source.compareTo("Gene_symbol") == 0){
					gene_symbol = t.gene_information;
				}
					
				if(t.source.compareTo("Coordinates") == 0){
					String temp = t.gene_information;
					start_coord = temp.substring(0, temp.indexOf(".."));
					temp = temp.substring(temp.indexOf("..") + 2);
					
					end_coord = temp.substring(0, temp.indexOf("("));
					
					strand = temp.charAt(temp.indexOf("(") + 1) + "";
					
					length = (Integer.parseInt(end_coord) - Integer.parseInt(start_coord) + 1) + "";
				}
				
				if(t.source.compareTo("Product_name") == 0){
					description = t.gene_information;
				}
				
				locus_tag = "";
			}
			toBeWritten = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", start_coord, end_coord, strand, length, locus_type, locus_tag, gene_symbol, description);
			bw.write(toBeWritten);
			bw.flush();
		}
		
		if (Integer.parseInt(end_coord) > info.getSequenceLen()){

			JOptionPane.showMessageDialog(null, ""
					+ "ERROR: insertions were found at positions above the provided sequence length. The library\n"
					+ "was not created.\n"
					+ "\n"
					+ "Possible reasons and solutions:\n"
					+ "- The sequence length is incorrect. Verify the sequence length and if necessary correct it in the\n"
					+ "'Main' tab.\n"
					+ "- You have used a wrong DNA sequence when running the Barrows-WheelerAligner to locate\n"
					+ "the insertions and generate the .sam file. Run BWA with the correct sequence.\n"
					+ "- Some of the input files are not formatted properly. Verify that all input files, including the\n"
					+ "DNA sequence used for BWA, have the correct format.", "ERROR", JOptionPane.ERROR_MESSAGE);

			bw.close();
			
			deleteAllOtherGenesFiles("", info);
			info.setGeneFile(null);
			return;
		}
		
		bw.close();
		
		deleteAllOtherGenesFiles(genesFile.getName(), info);
		
		info.setGeneFile(genesFile);
	}
	
	public static void deleteAllOtherGenesFiles(String notToBeDeleted, ProjectInfo info){
		File projectDir = new File(info.getPath());
		File[] match = projectDir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(".genes") || name.endsWith(".Genes"));
			}
		});
		
		for (File t : match){
			if (t.getName().compareTo(notToBeDeleted) != 0){
				t.delete();
			}
		}
		
	}
	
	public static List<List<ImgFileInfo>> extractScaffolds(String path, ProjectInfo info, JComboBox<String> enableAfterDone) throws IOException{
		File imgFile = new File(path);
		
		BufferedReader br = new BufferedReader(new FileReader(imgFile));
		br.readLine();
		
		List<List<ImgFileInfo>> wholeImgFile = new ArrayList<>();
		List<ImgFileInfo> imgLines = new ArrayList<>();
		String line = br.readLine();
		while(line != null){
			ImgFileInfo tempImgLine = new ImgFileInfo();
			
			if(line.compareTo("\t\t\t\t\t") == 0){
				wholeImgFile.add(imgLines);
				imgLines = new ArrayList<>();
				line = br.readLine();
				continue;
			}
			
			String temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);
			tempImgLine.gene_oid = temp;

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);
			tempImgLine.locus_tag = temp;

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);
			tempImgLine.source = temp;

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);
			tempImgLine.cluster_information = temp;

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);
			tempImgLine.gene_information = temp;

			tempImgLine.e_value = line;

			imgLines.add(tempImgLine);
			line = br.readLine();
		}
		
		br.close();
		
		List<String> scaffolds = new ArrayList<>();
		for (List<ImgFileInfo> arrayTemp : wholeImgFile){
			for (ImgFileInfo t : arrayTemp){
				if (t.source.compareTo("Scaffold") == 0){
					if (!scaffolds.contains(t.gene_information)){
						scaffolds.add(t.gene_information);
					}
				}
			}
		}
		
		if(enableAfterDone != null){
			enableAfterDone.removeAllItems();
			for (String t : scaffolds){
				enableAfterDone.addItem(t);
			}
			enableAfterDone.setEnabled(true);
		}

		return wholeImgFile;
	}
	
	public static String createGeneFile(String pttFilePath, String rntFilePath, String projectPath, ProjectInfo info) {

		String geneFileName = prepareFileName(pttFilePath, ".genes");

		File genesFile = new File(projectPath + geneFileName);
		File pttFile = new File(pttFilePath);

		BufferedWriter bw = null;
		BufferedReader br = null;
		try {
			if(!genesFile.exists()){
				genesFile.createNewFile();
			}
			bw = new BufferedWriter(new FileWriter(genesFile));
			br = new BufferedReader(new FileReader(pttFile));
			writeToGenesFile(bw, br, true);

			if(rntFilePath != null && rntFilePath.compareTo("") != 0){
				File rntFile = new File(rntFilePath);
				br = new BufferedReader(new FileReader(rntFile));
				writeToGenesFile(bw, br, false);
			}
			
			bw.close();
			br.close();
			
			//First Sort it
			File sorted = new File("sorted.genes");
			Comparator<String> comparator = new Comparator<String>() {

				@Override
				public int compare(String arg0, String arg1) {
					String start0 = arg0.substring(0, arg0.indexOf("\t"));
					String start1 = arg1.substring(0, arg1.indexOf("\t"));
					
					Integer int0 = Integer.parseInt(start0);
					Integer int1 = Integer.parseInt(start1);
					
					return int0.compareTo(int1);
				}
			};
			sortNotDistinct(genesFile.getAbsolutePath(), sorted.getAbsolutePath(), comparator);
			
			if(genesFile.delete()){
				if(!sorted.renameTo(genesFile)){
					logger.fatal("Not renamed!!!");
				}
			}
			
			//Then Write the Header to the File
			writeGenesFileHeader(genesFile);
			
			ArrayList<String> lines = MyFileUtil.tailNLines(genesFile, 2);
			String temp = lines.get(1);
			temp = temp.substring(temp.indexOf("\t") + 1);
			temp = temp.substring(0, temp.indexOf("\t"));

			if(Integer.parseInt(temp) > info.getSequenceLen()){
				JOptionPane.showMessageDialog(null, ""
						+ "ERROR: insertions were found at positions above the provided sequence length. The library\n"
						+ "was not created.\n"
						+ "\n"
						+ "Possible reasons and solutions:\n"
						+ "- The sequence length is incorrect. Verify the sequence length and if necessary correct it in the\n"
						+ "'Main' tab.\n"
						+ "- You have used a wrong DNA sequence when running the Barrows-WheelerAligner to locate\n"
						+ "the insertions and generate the .sam file. Run BWA with the correct sequence.\n"
						+ "- Some of the input files are not formatted properly. Verify that all input files, including the\n"
						+ "DNA sequence used for BWA, have the correct format.", "ERROR", JOptionPane.ERROR_MESSAGE);
				
				if(!genesFile.delete()){
					logger.warn("Could not delete the incompatible gene file!");
				}
				
				return Messages.failMsg;
			}
			
		} catch (IOException e) {
			logger.error(e.getMessage());
			return Messages.failMsg;
		} finally {
			try {
				bw.close();
				br.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				return Messages.failMsg;
			}
		}

		return genesFile.getAbsolutePath();
	}

	private static void writeGenesFileHeader(File genesFile){

		BufferedReader br = null;
		BufferedWriter bw = null;
		
		File tempFile = new File("temp.genes");
		try {
			br = new BufferedReader(new FileReader(genesFile));
			bw = new BufferedWriter(new FileWriter(tempFile));

			//Leaving 4 Lines Blank For Later Added Details
			bw.write("\t\t\t\t\t\t\t\n");
			bw.write("\t\t\t\t\t\t\t\n");
			bw.write("\t\t\t\t\t\t\t\n");
			bw.write("\t\t\t\t\t\t\t\n");

			//Table Columns
			String start_coord = "start_coord";
			String end_coord = "end_coord";
			String strand = "strand";
			String length = "length";
			String locus_type = "locus_type";
			String locus_tag = "locus_tag";
			String gene_symbol = "gene_symbol";
			String description = "description";
			String temp = "";

			temp = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", start_coord, end_coord, strand, 
					length, locus_type, locus_tag, gene_symbol, description);
			bw.write(temp);
			bw.flush();
			
			temp = br.readLine();
			while(temp != null){
				bw.write(temp + "\n");
				bw.flush();
				
				temp = br.readLine();
			}
			
			br.close();
			bw.close();
			
			genesFile.delete();
			tempFile.renameTo(genesFile);
			
		} catch (IOException e) {
			logger.error(e.getMessage());
			return;
		} finally {
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				return;
			}
		}
		
	}

	private static void writeToGenesFile(BufferedWriter bw, BufferedReader br, boolean ptt) throws IOException{

		//Skip File Headers
		br.readLine();
		br.readLine();
		String line = br.readLine();

		//Table Columns
		String start_coord = "start_coord";
		String end_coord = "end_coord";
		String strand = "strand";
		String length = "length";
		String locus_type = "locus_type";
		String locus_tag = "locus_tag";
		String gene_symbol = "gene_symbol";
		String description = "description";
		String temp = "";

		while(line != null){
			line = br.readLine();
			if(line == null)
				break;

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);

			start_coord = temp.substring(0, temp.indexOf(".."));
			end_coord = temp.substring(temp.indexOf("..") + 2);

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);

			strand = temp;

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);

			length = temp;

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);

			gene_symbol = temp;

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);

			locus_tag = temp;

			if(ptt){
				locus_type = "CDS";
			}else{
				locus_type = "RNA";
			}

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);

			temp = line.substring(0, line.indexOf("\t"));
			line = line.substring(line.indexOf("\t") + 1);

			description = line;

			temp = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", start_coord, end_coord, strand, 
					length, locus_type, locus_tag, gene_symbol, description);
			bw.write(temp);
			bw.flush();
		}

	}

	public static String countUniqueLocationsSortedByNumbers(String samFilePath, String outPath){

		String inputPath = prepareOutputFilePath(samFilePath, outPath, ".inspou");
		String outputPath = prepareOutputFilePath(samFilePath, outPath, ".inspous");

		Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String r1, String r2){
				r1 = r1.substring(r1.indexOf("\t") + 3);
				r2 = r2.substring(r2.indexOf("\t") + 3);

				Integer num1 = Integer.parseInt(r1);
				Integer num2 = Integer.parseInt(r2);

				return num2.compareTo(num1);
			}
		};

		try {
			sortNotDistinct(inputPath, outputPath, comparator);
		} catch (IOException e) {
			logger.error(e.getMessage());
			return e.getMessage();
		} catch(Exception e){
			logger.error(e.getMessage());
			return e.getMessage();
		}

		return Messages.successMsg;
	}

	public static String countUniqueLocations(String samFilePath, String outPath){

		String sortedPath = prepareOutputFilePath(samFilePath, outPath, ".inspos");
		String uniquePath = prepareOutputFilePath(samFilePath, outPath, ".inspou");
		String tempPath = prepareOutputFilePath(samFilePath, outPath, ".tmp");

		File input = new File(sortedPath);
		File tempFile = new File(tempPath);

		BufferedReader br = null;
		BufferedWriter bw = null;

		try{
			br = new BufferedReader(new FileReader(input));
			bw = new BufferedWriter(new FileWriter(tempPath));

			String line = br.readLine();
			int currentUniqueNumber = Integer.parseInt(line);
			int count = 0;

			while(line != null){
				int current = Integer.parseInt(line);

				if(current == currentUniqueNumber){
					count++;
				}else{
					String sign = "+";
					if(currentUniqueNumber < 0){
						sign = "-";
						currentUniqueNumber *= -1;
					}

					bw.write(currentUniqueNumber + "\t" + sign + "\t" + count + "\n");

					currentUniqueNumber = current;
					count = 1;
				}

				bw.flush();
				line = br.readLine();
			}

			String sign = "+";
			if(currentUniqueNumber < 0){
				sign = "-";
				currentUniqueNumber *= -1;
			}

			bw.write(currentUniqueNumber + "\t" + sign + "\t" + count + "\n");

		}catch(IOException e){
			logger.error(e.getMessage());
			return e.getMessage();
		} catch(Exception e){
			logger.error(e.getMessage());
			return e.getMessage();
		}finally{
			try{
				br.close();
				bw.close();
			}catch(IOException e){
				logger.error(e.getMessage());
				return e.getMessage();
			}
		}

		Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String r1, String r2){
				r1 = r1.substring(0, r1.indexOf("\t"));
				r2 = r2.substring(0, r2.indexOf("\t"));

				Integer num1 = Integer.parseInt(r1);
				Integer num2 = Integer.parseInt(r2);

				return num1.compareTo(num2);
			}
		};

		try {
			sortNotDistinct(tempPath, uniquePath, comparator);
		} catch (IOException e) {
			logger.error(e.getMessage());
			return e.getMessage();
		} catch(Exception e){
			logger.error(e.getMessage());
			return e.getMessage();
		}

		tempFile.delete();

		return Messages.successMsg;
	}

	public static String sortTheLocationsFile(String samFilePath, String outPath) {

		String unsortedPath = prepareOutputFilePath(samFilePath, outPath, ".inspo");
		String sortedPath = prepareOutputFilePath(samFilePath, outPath, ".inspos");

		try{
			Comparator<String> comparator = new Comparator<String>() {
				@Override
				public int compare(String r1, String r2){
					Integer num1 = Integer.parseInt(r1);
					Integer num2 = Integer.parseInt(r2);

					return num1.compareTo(num2);
				}
			};

			sortNotDistinct(unsortedPath, sortedPath, comparator);
		}catch(IOException e){
			logger.error(e.getMessage());
			return e.getMessage();
		} catch(Exception e){
			logger.error(e.getMessage());
			return e.getMessage();
		}

		return Messages.successMsg;
	}

	private static void sortNotDistinct(String inputFile, String outputFile, Comparator<String> comparator) throws IOException{	
		boolean verbose = true;
		boolean distinct = false;
		int maxTempFiles = 2048;
		Charset cs = Charset.forName("US-ASCII");
		File tempFileStore = null;
		int headersize = 0;
		boolean useGZip = false;

		List<File> filesList = ExternalSort.sortInBatch(new File(inputFile), comparator, maxTempFiles, cs, tempFileStore, distinct, headersize, useGZip);
		if(verbose) {
			logger.info("created " + filesList.size() + " tmp files");
		}

		ExternalSort.mergeSortedFiles(filesList, new File(outputFile), comparator, cs, distinct, false, useGZip);
	}

	public static String prepareFileName(String inPath, String extension){
		int tempIndex = 0;
		boolean windows = true;
		if (inPath.contains("/")){
			windows = false;
		}

		String fileName = new String(inPath);
		if(fileName.endsWith(".inspo")){
			fileName = fileName.substring(0, fileName.length() - 6);
		}else{
			fileName = fileName.substring(0, fileName.length() - 4);			
		}
		if (windows){
			while(fileName.contains("\\")){
				tempIndex = fileName.indexOf("\\");
				fileName = fileName.substring(tempIndex + 1);
			}
		}else{
			while(fileName.contains("/")){
				tempIndex = fileName.indexOf("/");
				fileName = fileName.substring(tempIndex + 1);
			}
		}

		return fileName + extension;
	}

	public static String prepareOutputFilePath(String inPath, String outPath, String extension){
		int tempIndex = 0;
		boolean windows = true;
		if (inPath.contains("/")){
			windows = false;
		}

		String fileName = new String(inPath);
		
		if(fileName.endsWith(".inspo")){
			fileName = fileName.substring(0, fileName.length() - 6);
		}else{
			fileName = fileName.substring(0, fileName.length() - 4);			
		}
		if (windows){
			while(fileName.contains("\\")){
				tempIndex = fileName.indexOf("\\");
				fileName = fileName.substring(tempIndex + 1);
			}
		}else{
			while(fileName.contains("/")){
				tempIndex = fileName.indexOf("/");
				fileName = fileName.substring(tempIndex + 1);
			}
		}

		return outPath + fileName + extension;
	}

	public static String findTheLocationsInTheSamFile(String inFilePath, String outPath, int CHRLEN){

		String outFilePath = prepareOutputFilePath(inFilePath, outPath, ".inspo");

		File input = new File(inFilePath);
		File output = new File(outFilePath);

		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			br = new BufferedReader(new FileReader(input));	        
			bw = new BufferedWriter(new FileWriter(output));

			//long totalSize = input.length();
			//long readSize = 0;

			String line = br.readLine();
			line = br.readLine();

			while(line != null){
				line = line.substring(line.indexOf("\t") + 1);
				int direction = Integer.parseInt(line.substring(0, line.indexOf("\t")));

				if (direction == 0){
					direction = 1;
				}else if(direction == 16){
					direction = -1;
				}else{
					logger.warn("There must have been a mistake. The direction code is --> " + direction);
				}	

				line = line.substring(line.indexOf("\t") + 1);
				line = line.substring(line.indexOf("\t") + 1);

				int location = 0;
				if (direction == 1){
					location = Integer.parseInt(line.substring(0, line.indexOf("\t")));
				}else{
					location = (Integer.parseInt(line.substring(0, line.indexOf("\t"))) * -1) - 50;
					if (location < -CHRLEN){
						location += CHRLEN;
					}
				}

				if(direction == 1 || direction == -1){
					bw.write(location + "\n");
					bw.flush();
				}

				line = br.readLine();
			}

		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
			return e.getMessage();
		} catch (IOException e) {
			logger.error(e.getMessage());
			return e.getMessage();
		} catch(Exception e){
			logger.error(e.getMessage());
			return e.getMessage();
		}finally {
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				return e.getMessage();
			}
		}

		return Messages.successMsg;

	}

}
