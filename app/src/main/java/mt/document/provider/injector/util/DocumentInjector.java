
/*
 * MT-Document-Provider-Injector 
 * Copyright 2024, developer-krushna
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of developer-krushna nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


 *     Please contact Krushna by email mt.modder.hub@gmail.com if you need
 *     additional information or have any questions
 */
 
package mt.document.provider.injector.util;

import android.content.*;
import bin.zip.*;
import java.io.*;
import java.util.*;

import pxb.android.axml.*;

import bin.zip.ZipEntry;
import bin.zip.ZipFile;
import bin.zip.ZipOutputStream;
import org.jetbrains.annotations.*;
import android.widget.*;

/* 
 Author @developer-krushna
 Comments by ChatGPT
 */
 
public class DocumentInjector {
	// Paths for APK files and temporary directory
	private String outApk; // Path for the output APK
	private String srcApk; // Path for the source APK
	private String tempApk; // Path for the temporary directory
	private DocumentInjectorCallBack mCallBack; // Callback interface for progress and messages
	private String AndroidManifest = "AndroidManifest.xml"; // Name of the AndroidManifest file

	// Constructor to initialize the callback
	public DocumentInjector(DocumentInjectorCallBack callback) {
		mCallBack = callback; 
	}

	// Method to set paths for source APK, output APK, and temporary directory
	public void setPath(String input) {
		srcApk = input;
		outApk = input.replace(".apk", "_dp.apk"); // Output APK with "_dp" suffix
		tempApk = new File(srcApk).getParentFile().toString() + "/.temp"; // Temporary directory path
	}

	// Main method to process the APK
	public void ProcessApk() throws Exception {
		// Delete existing output APK file
		new File(outApk).delete();

		try (ZipFile zipFile = new ZipFile(srcApk)) { // Open source APK as a zip file
			// Retrieve AndroidManifest entry from the APK
			ZipEntry manifestEntry = zipFile.getEntry(AndroidManifest);

			// Modify the AndroidManifest binary XML data
			byte[] manifestData = getModifiedBinXmlData(zipFile.getInputStream(manifestEntry));

			// Prepare the temporary zip output stream
			ZipOutputStream zipOutputStream = new ZipOutputStream(new File(tempApk));
			zipOutputStream.setLevel(1); // Set compression level
			zipOutputStream.setMethod(ZipOutputStream.STORED); // Set storage method

			// Lists to track dex files and all files in the zip
			Enumeration<ZipEntry> entries = zipFile.getEntries();
			ArrayList<String> dexList = new ArrayList<>();
			ArrayList<String> totalFilesInZip = new ArrayList<>();

			// Copy specific entries to the temporary zip
			while (entries.hasMoreElements()) {
				ZipEntry nextElement = entries.nextElement();
				String name = nextElement.getName();
				totalFilesInZip.add(name);

				// Copy dex files and ignore other files
				if ((name.startsWith("classes") && name.endsWith("dex")) || name.startsWith("./")) {
					zipOutputStream.copyZipEntry(nextElement, zipFile);
					dexList.add(name);
				}
			}
			zipOutputStream.close();

			// Create the final output APK
			try (ZipOutputStream zos = new ZipOutputStream(new File(outApk))) {
				zos.setMethod(ZipOutputStream.DEFLATED);

				// Add the modified AndroidManifest to the output APK
				zos.putNextEntry(AndroidManifest);
				zos.write(manifestData);
				zos.closeEntry();

				// Process and add new dex file to the output APK
				byte[] dexData = processDex();
				zos.putNextEntry(new ZipEntry("classes" + (dexList.size() + 1) + ".dex"));

				// Write dex data with progress tracking
				ByteArrayInputStream bis = new ByteArrayInputStream(dexData);
				byte[] buffer = new byte[2048];
				int length;
				int totalBytes = dexData.length;
				int copiedBytes = 0;
				while ((length = bis.read(buffer)) > 0) {
					zos.write(buffer, 0, length);
					copiedBytes += length;
					mCallBack.onProgress(copiedBytes, totalBytes); // Update progress
				}
				zos.closeEntry();

				// Copy other files from the temporary zip to the final output APK
				Enumeration<ZipEntry> entry = zipFile.getEntries();
				int copiedFiles = 0;
				while (entry.hasMoreElements()) {
					ZipEntry newEntry = entry.nextElement();
					// Avoid duplicating the AndroidManifest and new dex file
					if (!newEntry.getName().equals(AndroidManifest) &&
						!newEntry.getName().equals(new ZipEntry("classes" + (dexList.size() + 1) + ".dex"))) {
						zos.copyZipEntry(newEntry, zipFile);
						copiedFiles++;
						mCallBack.onProgress(copiedFiles, totalFilesInZip.size()); // Update progress
					}
				}

				// Clean up the temporary directory
				new File(tempApk).delete();
				zipFile.close();
			} 
		}
	}

	// Method to process and return new dex file data
	private byte[] processDex() throws Exception {
		byte[] dexBytes = readAllBytes(DocumentInjector.class.getResourceAsStream("/assets/provider"));
		return dexBytes;
	}

	// Utility method to read all bytes from an InputStream
	public static byte[] readAllBytes(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];
		int len;
		while ((len = is.read(buffer)) > 0)
			bos.write(buffer, 0, len);
		is.close();
		return bos.toByteArray();
	}

	// Method to modify AndroidManifest binary XML data
	public static byte[] getModifiedBinXmlData(InputStream input) throws IOException {
		Axml axml = new Axml();
		byte[] bytes = readAllBytes(input);
		new AxmlReader(bytes).accept(ManifestEditor.getNodeVisitor(axml));
		ManifestEditor.addDocumentProvider(axml); // Add custom document provider
		AxmlWriter aw = new AxmlWriter();
		axml.accept(aw);
		return aw.toByteArray();
	}

	// Callback interface for progress and messages
	public interface DocumentInjectorCallBack {
		void onProgress(int progress, int total); // Progress update method
		void onMessage(String name); // Message notification method
	}
}

