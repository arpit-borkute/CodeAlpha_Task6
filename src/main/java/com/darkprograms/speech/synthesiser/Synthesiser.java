package com.darkprograms.speech.synthesiser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.util.List;

public class Synthesiser extends BaseSynthsiser {

	private final static String GOOGLE_SYNTHESISER_URL = "http://translate.google.com/translate_tts";

	private String languageCode;

	public static final String LANG_AU_ENGLISH = "en-AU";
	public static final String LANG_US_ENGLISH = "en-US";
	public static final String LANG_UK_ENGLISH = "en-GB";
	public static final String LANG_ES_SPANISH = "es";
	public static final String LANG_FR_FRENCH = "fr";
	public static final String LANG_DE_GERMAN = "de";
	public static final String LANG_PT_PORTUGUESE = "pt-pt";
	public static final String LANG_PT_BRAZILIAN = "pt-br";

	public Synthesiser() {
		languageCode = "auto";
	}

	public Synthesiser(String languageCode) {
		this.languageCode = languageCode;
	}

	public String getLanguage() {
		return languageCode;
	}

	public void setLanguage(String languageCode) {
		this.languageCode = languageCode;
	}

	@Override
	public InputStream getMP3Data(String synthText) throws IOException {

		String languageCode = this.languageCode;// Ensures retention of language settings if set to auto

		if (languageCode == null || languageCode.equals("") || languageCode.equalsIgnoreCase("auto")) {
			languageCode = detectLanguage(synthText);// Detects language

			if (languageCode == null) {
				languageCode = "en-us";// Reverts to Default Language if it can't detect it.
				// Throw an error message here eventually
			}
		}

		if (synthText.length() > 100) {
			List<String> fragments = parseString(synthText);// parses String if too long
			String tmp = getLanguage();
			setLanguage(languageCode);// Keeps it from autodetecting each fragment.
			InputStream out = getMP3Data(fragments);
			setLanguage(tmp);// Reverts it to it's previous Language such as auto.
			return out;
		}

		String encoded = URLEncoder.encode(synthText, "UTF-8"); // Encode

		StringBuilder sb = new StringBuilder();
		sb.append(GOOGLE_SYNTHESISER_URL); // The base URL prefixed by the query parameter.
		sb.append("?tl=");
		sb.append(languageCode); // The query parameter to specify the language code.
		sb.append("&q=");
		sb.append(encoded); // We encode the String using URL Encoder
		sb.append("&ie=UTF-8&total=1&idx=0"); // Some unknown parameters needed to make the URL work
		sb.append("&textlen=");
		sb.append(synthText.length()); // We need some String length now.
		sb.append("&client=tw-ob"); // Once again, a weird parameter.
		// Client=t no longer works as it requires a token, but client=tw-ob seems to
		// work just fine.

		URL url = new URL(sb.toString());
		// Open New URL connection channel.
		URLConnection urlConn = url.openConnection(); // Open connection

		// Adding header for user agent is required
		urlConn.addRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:2.0) " + "Gecko/20100101 Firefox/4.0");

		return urlConn.getInputStream();
	}
}
