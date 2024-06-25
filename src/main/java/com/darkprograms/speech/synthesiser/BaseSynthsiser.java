package com.darkprograms.speech.synthesiser;

import com.darkprograms.speech.translator.GoogleTranslate;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class BaseSynthsiser {

	public abstract InputStream getMP3Data(String synthText) throws IOException;

	public InputStream getMP3Data(List<String> synthText) throws IOException {
		// Uses an executor service pool for concurrency. Limit to 1000 threads max.
		ExecutorService pool = Executors.newFixedThreadPool(1000);
		// Stores the Future (Data that will be returned in the future)
		Set<Future<InputStream>> set = new LinkedHashSet<Future<InputStream>>(synthText.size());
		for (String part : synthText) { // Iterates through the list
			Callable<InputStream> callable = new MP3DataFetcher(part);// Creates Callable
			Future<InputStream> future = pool.submit(callable);// Begins to run Callable
			set.add(future);// Adds the response that will be returned to a set.
		}
		List<InputStream> inputStreams = new ArrayList<InputStream>(set.size());
		for (Future<InputStream> future : set) {
			try {
				inputStreams.add(future.get());// Gets the returned data from the future.
			} catch (ExecutionException e) {// Thrown if the MP3DataFetcher encountered an error.
				Throwable ex = e.getCause();
				if (ex instanceof IOException) {
					throw (IOException) ex;// Downcasts and rethrows it.
				}
			} catch (InterruptedException e) {// Will probably never be called, but just in case...
				Thread.currentThread().interrupt();// Interrupts the thread since something went wrong.
			}
		}
		return new SequenceInputStream(Collections.enumeration(inputStreams));// Sequences the stream.
	}

	protected List<String> parseString(String input) {
		return parseString(input, new ArrayList<String>());
	}

	private List<String> parseString(String input, List<String> fragments) {
		if (input.length() <= 100) {// Base Case
			fragments.add(input);
			return fragments;
		} else {
			int lastWord = findLastWord(input);// Checks if a space exists
			if (lastWord <= 0) {
				fragments.add(input.substring(0, 100));// In case you sent gibberish to Google.
				return parseString(input.substring(100), fragments);
			} else {
				fragments.add(input.substring(0, lastWord));
				// Otherwise, adds the last word to the list for recursion.
				return parseString(input.substring(lastWord), fragments);
			}
		}
	}

	private int findLastWord(String input) {
		if (input.length() < 100)
			return input.length();
		int space = -1;
		for (int i = 99; i > 0; i--) {
			char tmp = input.charAt(i);
			if (isEndingPunctuation(tmp)) {
				return i + 1;
			}
			if (space == -1 && tmp == ' ') {
				space = i;
			}
		}
		if (space > 0) {
			return space;
		}
		return -1;
	}

	private boolean isEndingPunctuation(char input) {
		return input == '.' || input == '!' || input == '?' || input == ';' || input == ':' || input == '|';
	}

	public String detectLanguage(String text) throws IOException {
		return GoogleTranslate.detectLanguage(text);
	}

	private class MP3DataFetcher implements Callable<InputStream> {
		private String synthText;

		public MP3DataFetcher(String synthText) {
			this.synthText = synthText;
		}

		public InputStream call() throws IOException {
			return getMP3Data(synthText);
		}
	}
}
