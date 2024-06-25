package com.darkprograms.speech.util;

import java.io.*;
import java.util.*;

public class ChunkedOutputStream extends BufferedOutputStream {

	private static final byte[] crlf = { 13, 10 };
	private byte[] lenBytes = new byte[20]; // big enough for any number in hex
	private List<String> footerNames = new ArrayList<String>();
	private List<String> footerValues = new ArrayList<String>();

	public ChunkedOutputStream(OutputStream out) {
		super(out);
	}

	public ChunkedOutputStream(OutputStream out, int size) {
		super(out, size);
	}

	public synchronized void flush() throws IOException {
		if (count != 0) {
			writeBuf(buf, 0, count);
			count = 0;
		}
	}

	public void setFooter(String name, String value) {
		footerNames.add(name);
		footerValues.add(value);
	}

	public void done() throws IOException {
		flush();
		PrintStream pout = new PrintStream(out);
		pout.println("0");
		if (footerNames.size() > 0) {
			// Send footers.
			for (int i = 0; i < footerNames.size(); ++i) {
				String name = footerNames.get(i);
				String value = footerValues.get(i);
				pout.println(name + ": " + value);
			}
		}
		footerNames = null;
		footerValues = null;
		pout.println("");
		pout.flush();
	}

	/// Make sure that calling close() terminates the chunked stream.
	public void close() throws IOException {
		if (footerNames != null)
			done();
		super.close();
	}

	public synchronized void write(byte b[], int off, int len) throws IOException {
		int avail = buf.length - count;

		if (len <= avail) {
			System.arraycopy(b, off, buf, count, len);
			count += len;
			return;
		}
		flush();
		writeBuf(b, off, len);
	}

	@SuppressWarnings("deprecation")
	private void writeBuf(byte b[], int off, int len) throws IOException {
		// Write the chunk length as a hex number.
		String lenStr = Integer.toString(len, 16);
		lenStr.getBytes(0, lenStr.length(), lenBytes, 0);
		out.write(lenBytes);
		// Write a CRLF.
		out.write(crlf);
		// Write the data.
		if (len != 0)
			out.write(b, off, len);
		// Write a CRLF.
		out.write(crlf);
		// And flush the real stream.
		out.flush();
	}

}
